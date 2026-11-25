import Foundation

struct UserPouredSegment: Equatable {
    let flavors: Set<Flavor>
    let heightStart: Float
    let heightEnd: Float
    let durationSec: Float
    var progressFraction: Float { max(0, heightEnd - heightStart) }

    init(flavors: Set<Flavor>, heightStart: Float, heightEnd: Float, durationSec: Float = 0) {
        self.flavors = flavors
        self.heightStart = heightStart
        self.heightEnd = heightEnd
        self.durationSec = durationSec
    }
}

struct MatchResult {
    let totalScore: Int       // 0 - 100
    let stars: Int            // 1, 2, or 3 (0 if failed)
    let sequenceScore: Float  // 0.0 - 100.0
    let proportionScore: Float// 0.0 - 100.0
    let heightScore: Float    // 0.0 - 100.0
}

/**
 * THUẬT TOÁN CHẤM ĐIỂM KEM (ICE CREAM MATCHING ALGORITHM)
 *
 * Tổng điểm: TotalScore = (0.45 * S_seq) + (0.35 * S_prop) + (0.20 * S_height)
 *
 * Trong đó:
 * 1. S_seq (Trọng số 45% - Độ chính xác thứ tự vị/màu):
 *    - So khớp từng tầng từ đáy lên đỉnh giữa các tầng người chơi rót và kem mẫu (TargetLayers).
 *    - Sử dụng chỉ số tương đồng Jaccard giữa tập hợp vị người chơi bấm và vị yêu cầu:
 *      Jaccard = |UserFlavors ∩ TargetFlavors| / |UserFlavors ∪ TargetFlavors|
 *    - Trừ điểm nếu số lượng tầng rót thiếu hoặc thừa so với mẫu:
 *      Penalty = |NumUserLayers - NumTargetLayers| / max(NumUserLayers, NumTargetLayers)
 *      S_seq = 100 * max(0, 1 - Penalty) * AverageJaccard
 *
 * 2. S_prop (Trọng số 35% - Độ dài/Tỷ lệ độ cao từng lớp):
 *    - Tính độ lệch tuyệt đối giữa tỷ lệ độ dày thực tế của từng tầng so với mẫu:
 *      Diff_prop = Sum( |p_user[i] - p_target[i]| )
 *      S_prop = 100 * max(0, 1 - Diff_prop)
 *
 * 3. S_height (Trọng số 20% - Độ cao tổng thể / Độ đầy cây kem):
 *    - Kiểm tra xem người chơi có rót kem đầy ngọn (pourProgress gần 1.0) hay dừng quá sớm/tràn:
 *      S_height = 100 * max(0, 1 - 2.0 * |pourProgress - 1.0|)
 *
 * Quy đổi ra số Sao:
 * - TotalScore >= 85 điểm  -> 3 SAO (Hoàn hảo)
 * - 60 <= TotalScore < 85 -> 2 SAO (Tốt)
 * - 40 <= TotalScore < 60 -> 1 SAO (Đạt)
 * - TotalScore < 40        -> 0 SAO (Không đạt - Phải làm lại)
 */
enum IceCreamMatcher {

    static func evaluate(
        userSegments: [UserPouredSegment],
        targetLayers: [TargetLayer],
        finalPourProgress: Float
    ) -> MatchResult {
        if targetLayers.isEmpty {
            return MatchResult(totalScore = 100, stars: 3, sequenceScore: 100, proportionScore: 100, heightScore: 100)
        }

        let userLayers = consolidateUserLayers(userSegments)

        // 1. Calculate S_seq (Sequence & Flavor Accuracy)
        let numUser = userLayers.count
        let numTarget = targetLayers.count

        let layerPenalty: Float = max(numUser, numTarget) > 0 ?
            Float(abs(numUser - numTarget)) / Float(max(numUser, numTarget)) : 0.0

        let compareCount = min(numUser, numTarget)
        var jaccardSum: Float = 0.0

        for i in 0..<compareCount {
            let uFlavors = userLayers[i].flavors
            let tFlavors = Set(targetLayers[i].flavors)

            let intersection = uFlavors.intersection(tFlavors).count
            let union = uFlavors.union(tFlavors).count
            let jaccard = union > 0 ? Float(intersection) / Float(union) : 0.0
            jaccardSum += jaccard
        }

        let avgJaccard: Float = compareCount > 0 ? (jaccardSum / Float(compareCount)) : 0.0
        let sequenceScore = min(max(100.0 * max(0.0, 1.0 - layerPenalty) * avgJaccard, 0.0), 100.0)

        // 2. Calculate S_prop (Proportion Accuracy)
        let totalUserProgress = max(userLayers.reduce(0.0) { $0 + $1.progressFraction }, 0.001)
        var propDiff: Float = 0.0

        for i in 0..<compareCount {
            let userNormalizedProp = userLayers[i].progressFraction / totalUserProgress
            let targetProp = targetLayers[i].proportion
            propDiff += abs(userNormalizedProp - targetProp)
        }
        let proportionScore = min(max(100.0 * max(0.0, 1.0 - propDiff), 0.0), 100.0)

        // 3. Calculate S_height (Height / Fill Completeness)
        let heightDiff = abs(finalPourProgress - 1.0)
        let heightScore = min(max(100.0 * max(0.0, 1.0 - (2.0 * heightDiff)), 0.0), 100.0)

        // Total weighted score
        let total = Int(min(max(0.45 * sequenceScore + 0.35 * proportionScore + 0.20 * heightScore, 0.0), 100.0))

        let stars: Int
        switch total {
        case 85...100: stars = 3
        case 60..<85:  stars = 2
        case 40..<60:  stars = 1
        default:       stars = 0
        }

        return MatchResult(
            totalScore: total,
            stars: stars,
            sequenceScore: sequenceScore,
            proportionScore: proportionScore,
            heightScore: heightScore
        )
    }

    private static func consolidateUserLayers(_ segments: [UserPouredSegment]) -> [UserPouredSegment] {
        guard !segments.isEmpty else { return [] }

        var consolidated: [UserPouredSegment] = []
        var currentFlavors = segments[0].flavors
        var currentStart = segments[0].heightStart
        var currentEnd = segments[0].heightEnd
        var currentDuration = segments[0].durationSec

        for i in 1..<segments.count {
            let seg = segments[i]
            if seg.flavors == currentFlavors {
                currentDuration += seg.durationSec
                currentEnd = seg.heightEnd
            } else {
                consolidated.append(
                    UserPouredSegment(
                        flavors = currentFlavors,
                        heightStart = currentStart,
                        heightEnd = currentEnd,
                        durationSec = currentDuration
                    )
                )
                currentFlavors = seg.flavors
                currentStart = seg.heightStart
                currentEnd = seg.heightEnd
                currentDuration = seg.durationSec
            }
        }
        consolidated.append(
            UserPouredSegment(
                flavors = currentFlavors,
                heightStart = currentStart,
                heightEnd = currentEnd,
                durationSec = currentDuration
            )
        )
        return consolidated
    }
}
