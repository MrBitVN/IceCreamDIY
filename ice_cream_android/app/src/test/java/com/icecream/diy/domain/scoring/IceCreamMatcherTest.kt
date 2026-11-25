package com.icecream.diy.domain.scoring

import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.TargetLayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for IceCreamMatcher (Phase 4 Scoring Engine)
 * Validates the 6 fundamental test cases:
 * 1. Hoàn hảo (Perfect Match) -> 3 stars
 * 2. Thiếu tầng (Missing Layer) -> reduced score
 * 3. Thừa tầng (Extra Unwanted Layer) -> penalty applied
 * 4. Sai thứ tự (Wrong sequence / Inverted flavors) -> sequence score penalized
 * 5. Thấp (Underfilled height) -> height score penalized
 * 6. Trống (Empty pour) -> 0 score, 0 stars
 */
class IceCreamMatcherTest {

    @Test
    fun testCase1_PerfectMatch() {
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 0.5f),
            TargetLayer(flavors = listOf(Flavor.STRAWBERRY), proportion = 0.5f)
        )

        val userSegments = listOf(
            UserPouredSegment(
                flavors = setOf(Flavor.VANILLA),
                heightStart = 0.0f,
                heightEnd = 0.5f,
                durationSec = 1.0f
            ),
            UserPouredSegment(
                flavors = setOf(Flavor.STRAWBERRY),
                heightStart = 0.5f,
                heightEnd = 1.0f,
                durationSec = 1.0f
            )
        )

        val result = IceCreamMatcher.evaluate(
            userSegments = userSegments,
            targetLayers = target,
            finalPourProgress = 1.0f
        )

        assertTrue("Expected score >= 90 for perfect match, was ${result.totalScore}", result.totalScore >= 90)
        assertEquals(3, result.stars)
        assertTrue("Sequence score should be 100", result.sequenceScore >= 99f)
        assertTrue("Proportion score should be 100", result.proportionScore >= 99f)
        assertTrue("Height score should be 100", result.heightScore >= 99f)
    }

    @Test
    fun testCase2_MissingLayer() {
        // Target requires 2 layers (Vanilla then Strawberry)
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 0.5f),
            TargetLayer(flavors = listOf(Flavor.STRAWBERRY), proportion = 0.5f)
        )

        // User pours ONLY Vanilla up to 1.0 (Strawberry missing)
        val userSegments = listOf(
            UserPouredSegment(
                flavors = setOf(Flavor.VANILLA),
                heightStart = 0.0f,
                heightEnd = 1.0f,
                durationSec = 2.0f
            )
        )

        val result = IceCreamMatcher.evaluate(
            userSegments = userSegments,
            targetLayers = target,
            finalPourProgress = 1.0f
        )

        assertTrue("Score must be lower than 85 when a layer is missing, was ${result.totalScore}", result.totalScore < 85)
        assertTrue("Stars should be <= 2", result.stars <= 2)
    }

    @Test
    fun testCase3_ExtraLayers() {
        // Target requires only 1 layer of Vanilla (1.0)
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 1.0f)
        )

        // User pours 3 different layers (Vanilla, Strawberry, Chocolate)
        val userSegments = listOf(
            UserPouredSegment(
                flavors = setOf(Flavor.VANILLA),
                heightStart = 0.0f,
                heightEnd = 0.35f,
                durationSec = 0.7f
            ),
            UserPouredSegment(
                flavors = setOf(Flavor.STRAWBERRY),
                heightStart = 0.35f,
                heightEnd = 0.70f,
                durationSec = 0.7f
            ),
            UserPouredSegment(
                flavors = setOf(Flavor.CHOCOLATE),
                heightStart = 0.70f,
                heightEnd = 1.0f,
                durationSec = 0.6f
            )
        )

        val result = IceCreamMatcher.evaluate(
            userSegments = userSegments,
            targetLayers = target,
            finalPourProgress = 1.0f
        )

        assertTrue("Extra layers penalty should reduce score below 60, was ${result.totalScore}", result.totalScore < 60)
        assertTrue("Stars should be <= 1", result.stars <= 1)
    }

    @Test
    fun testCase4_WrongSequence() {
        // Target: Chocolate on bottom (0.5), Vanilla on top (0.5)
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.CHOCOLATE), proportion = 0.5f),
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 0.5f)
        )

        // User inverts the sequence: Vanilla on bottom (0.5), Chocolate on top (0.5)
        val userSegments = listOf(
            UserPouredSegment(
                flavors = setOf(Flavor.VANILLA),
                heightStart = 0.0f,
                heightEnd = 0.5f,
                durationSec = 1.0f
            ),
            UserPouredSegment(
                flavors = setOf(Flavor.CHOCOLATE),
                heightStart = 0.5f,
                heightEnd = 1.0f,
                durationSec = 1.0f
            )
        )

        val result = IceCreamMatcher.evaluate(
            userSegments = userSegments,
            targetLayers = target,
            finalPourProgress = 1.0f
        )

        assertEquals("Sequence score must be 0 for completely inverted flavors", 0f, result.sequenceScore, 0.01f)
        assertTrue("Total score should be significantly lower (< 60), was ${result.totalScore}", result.totalScore < 60)
        assertTrue("Stars should be <= 1", result.stars <= 1)
    }

    @Test
    fun testCase5_ShortHeight() {
        // Target requires full cone (1.0) of Vanilla
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 1.0f)
        )

        // User stops early at 0.40 progress
        val userSegments = listOf(
            UserPouredSegment(
                flavors = setOf(Flavor.VANILLA),
                heightStart = 0.0f,
                heightEnd = 0.40f,
                durationSec = 0.8f
            )
        )

        val result = IceCreamMatcher.evaluate(
            userSegments = userSegments,
            targetLayers = target,
            finalPourProgress = 0.40f
        )

        assertEquals("Height score must be 0 when stopped at 0.40", 0f, result.heightScore, 0.01f)
        assertTrue("Total score must be penalized when height is low (< 85), was ${result.totalScore}", result.totalScore < 85)
        assertTrue("Stars should be <= 2", result.stars <= 2)
    }

    @Test
    fun testCase6_EmptySegments() {
        val target = listOf(
            TargetLayer(flavors = listOf(Flavor.VANILLA), proportion = 1.0f)
        )

        // User poured nothing
        val result = IceCreamMatcher.evaluate(
            userSegments = emptyList(),
            targetLayers = target,
            finalPourProgress = 0.0f
        )

        assertEquals(0, result.totalScore)
        assertEquals(0, result.stars)
        assertEquals(0f, result.sequenceScore, 0.01f)
        assertEquals(0f, result.proportionScore, 0.01f)
        assertEquals(0f, result.heightScore, 0.01f)
    }
}
