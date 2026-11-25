import XCTest
@testable import ice_cream_ios

final class IceCreamMatcherTests: XCTestCase {

    func testCase1_PerfectMatch() {
        let target = [
            TargetLayer(flavors: [.vanilla], proportion: 0.5),
            TargetLayer(flavors: [.strawberry], proportion: 0.5)
        ]

        let userSegments = [
            UserPouredSegment(
                flavors: [.vanilla],
                heightStart: 0.0,
                heightEnd: 0.5,
                durationSec: 1.0
            ),
            UserPouredSegment(
                flavors: [.strawberry],
                heightStart: 0.5,
                heightEnd: 1.0,
                durationSec: 1.0
            )
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: userSegments,
            targetLayers: target,
            finalPourProgress: 1.0
        )

        XCTAssertGreaterThanOrEqual(result.totalScore, 90)
        XCTAssertEqual(result.stars, 3)
        XCTAssertGreaterThanOrEqual(result.sequenceScore, 99.0)
        XCTAssertGreaterThanOrEqual(result.proportionScore, 99.0)
        XCTAssertGreaterThanOrEqual(result.heightScore, 99.0)
    }

    func testCase2_MissingLayer() {
        let target = [
            TargetLayer(flavors: [.vanilla], proportion: 0.5),
            TargetLayer(flavors: [.strawberry], proportion: 0.5)
        ]

        let userSegments = [
            UserPouredSegment(
                flavors: [.vanilla],
                heightStart: 0.0,
                heightEnd: 1.0,
                durationSec: 2.0
            )
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: userSegments,
            targetLayers: target,
            finalPourProgress: 1.0
        )

        XCTAssertLessThan(result.totalScore, 85)
        XCTAssertLessThanOrEqual(result.stars, 2)
    }

    func testCase3_ExtraLayers() {
        let target = [
            TargetLayer(flavors: [.vanilla], proportion: 1.0)
        ]

        let userSegments = [
            UserPouredSegment(
                flavors: [.vanilla],
                heightStart: 0.0,
                heightEnd: 0.35,
                durationSec: 0.7
            ),
            UserPouredSegment(
                flavors: [.strawberry],
                heightStart: 0.35,
                heightEnd: 0.70,
                durationSec: 0.7
            ),
            UserPouredSegment(
                flavors: [.chocolate],
                heightStart: 0.70,
                heightEnd: 1.0,
                durationSec: 0.6
            )
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: userSegments,
            targetLayers: target,
            finalPourProgress: 1.0
        )

        XCTAssertLessThan(result.totalScore, 60)
        XCTAssertLessThanOrEqual(result.stars, 1)
    }

    func testCase4_WrongSequence() {
        let target = [
            TargetLayer(flavors: [.chocolate], proportion: 0.5),
            TargetLayer(flavors: [.vanilla], proportion: 0.5)
        ]

        let userSegments = [
            UserPouredSegment(
                flavors: [.vanilla],
                heightStart: 0.0,
                heightEnd: 0.5,
                durationSec: 1.0
            ),
            UserPouredSegment(
                flavors: [.chocolate],
                heightStart: 0.5,
                heightEnd: 1.0,
                durationSec: 1.0
            )
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: userSegments,
            targetLayers: target,
            finalPourProgress: 1.0
        )

        XCTAssertEqual(result.sequenceScore, 0.0, accuracy: 0.01)
        XCTAssertLessThan(result.totalScore, 60)
        XCTAssertLessThanOrEqual(result.stars, 1)
    }

    func testCase5_ShortHeight() {
        let target = [
            TargetLayer(flavors: [.vanilla], proportion: 1.0)
        ]

        let userSegments = [
            UserPouredSegment(
                flavors: [.vanilla],
                heightStart: 0.0,
                heightEnd: 0.40,
                durationSec: 0.8
            )
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: userSegments,
            targetLayers: target,
            finalPourProgress: 0.40
        )

        XCTAssertEqual(result.heightScore, 0.0, accuracy: 0.01)
        XCTAssertLessThan(result.totalScore, 85)
        XCTAssertLessThanOrEqual(result.stars, 2)
    }

    func testCase6_EmptySegments() {
        let target = [
            TargetLayer(flavors: [.vanilla], proportion: 1.0)
        ]

        let result = IceCreamMatcher.evaluate(
            userSegments: [],
            targetLayers: target,
            finalPourProgress: 0.0
        )

        XCTAssertEqual(result.totalScore, 0)
        XCTAssertEqual(result.stars, 0)
        XCTAssertEqual(result.sequenceScore, 0.0, accuracy: 0.01)
        XCTAssertEqual(result.proportionScore, 0.0, accuracy: 0.01)
        XCTAssertEqual(result.heightScore, 0.0, accuracy: 0.01)
    }
}
