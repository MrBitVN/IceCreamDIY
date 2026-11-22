package com.icecream.diy.domain.scoring

import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.TargetLayer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class UserPouredSegment(
    val flavors: Set<Flavor>,
    val durationSec: Float,
    val progressFraction: Float
)

data class MatchResult(
    val totalScore: Int,      // 0 - 100
    val stars: Int,           // 1, 2, or 3 (0 if failed)
    val sequenceScore: Float, // 0.0 - 100.0
    val proportionScore: Float,// 0.0 - 100.0
    val heightScore: Float    // 0.0 - 100.0
)

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
object IceCreamMatcher {

    fun evaluate(
        userSegments: List<UserPouredSegment>,
        targetLayers: List<TargetLayer>,
        finalPourProgress: Float
    ): MatchResult {
        if (targetLayers.isEmpty()) {
            return MatchResult(totalScore = 100, stars = 3, sequenceScore = 100f, proportionScore = 100f, heightScore = 100f)
        }

        // Aggregate consecutive segments with identical flavor sets into distinct layers
        val userLayers = consolidateUserLayers(userSegments)

        // 1. Calculate S_seq (Sequence & Flavor Accuracy)
        val numUser = userLayers.size
        val numTarget = targetLayers.size

        val layerPenalty = if (max(numUser, numTarget) > 0) {
            abs(numUser - numTarget).toFloat() / max(numUser, numTarget).toFloat()
        } else 0f

        val compareCount = min(numUser, numTarget)
        var jaccardSum = 0f

        for (i in 0 until compareCount) {
            val uFlavors = userLayers[i].flavors
            val tFlavors = targetLayers[i].flavors.toSet()

            val intersection = uFlavors.intersect(tFlavors).size
            val union = uFlavors.union(tFlavors).size
            val jaccard = if (union > 0) intersection.toFloat() / union.toFloat() else 0f
            jaccardSum += jaccard
        }

        val avgJaccard = if (compareCount > 0) jaccardSum / compareCount.toFloat() else 0f
        val sequenceScore = (100f * max(0f, 1f - layerPenalty) * avgJaccard).coerceIn(0f, 100f)

        // 2. Calculate S_prop (Proportion Accuracy)
        val totalUserProgress = userLayers.sumOf { it.progressFraction.toDouble() }.toFloat().coerceAtLeast(0.001f)
        var propDiff = 0f

        for (i in 0 until compareCount) {
            val userNormalizedProp = userLayers[i].progressFraction / totalUserProgress
            val targetProp = targetLayers[i].proportion
            propDiff += abs(userNormalizedProp - targetProp)
        }
        val proportionScore = (100f * max(0f, 1f - propDiff)).coerceIn(0f, 100f)

        // 3. Calculate S_height (Height / Fill Completeness)
        val heightDiff = abs(finalPourProgress - 1.0f)
        val heightScore = (100f * max(0f, 1f - (2.0f * heightDiff))).coerceIn(0f, 100f)

        // Total weighted score
        val total = (0.45f * sequenceScore + 0.35f * proportionScore + 0.20f * heightScore).toInt().coerceIn(0, 100)

        val stars = when {
            total >= 85 -> 3
            total >= 60 -> 2
            total >= 40 -> 1
            else -> 0
        }

        return MatchResult(
            totalScore = total,
            stars = stars,
            sequenceScore = sequenceScore,
            proportionScore = proportionScore,
            heightScore = heightScore
        )
    }

    private fun consolidateUserLayers(segments: List<UserPouredSegment>): List<UserPouredSegment> {
        if (segments.isEmpty()) return emptyList()

        val consolidated = mutableListOf<UserPouredSegment>()
        var currentFlavors = segments[0].flavors
        var currentDuration = segments[0].durationSec
        var currentProgress = segments[0].progressFraction

        for (i in 1 until segments.size) {
            val seg = segments[i]
            if (seg.flavors == currentFlavors) {
                currentDuration += seg.durationSec
                currentProgress += seg.progressFraction
            } else {
                consolidated.add(UserPouredSegment(currentFlavors, currentDuration, currentProgress))
                currentFlavors = seg.flavors
                currentDuration = seg.durationSec
                currentProgress = seg.progressFraction
            }
        }
        consolidated.add(UserPouredSegment(currentFlavors, currentDuration, currentProgress))
        return consolidated
    }
}
