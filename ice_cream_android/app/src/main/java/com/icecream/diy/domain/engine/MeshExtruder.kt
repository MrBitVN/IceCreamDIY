package com.icecream.diy.domain.engine

import com.icecream.diy.data.model.Flavor
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ExtrudedVertex(
    val x: Float,
    val y: Float,
    val z: Float,
    val nx: Float,
    val ny: Float,
    val nz: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float
)

class MeshExtruder(
    val ringSegments: Int = 16,
    val maxRings: Int = 180,
    val lobes: Int = 6
) {
    val totalFloatsPerVertex = 10 // pos(3), norm(3), color(4)
    val maxVertices = maxRings * ringSegments
    val maxIndices = (maxRings - 1) * ringSegments * 6

    // Preallocated native arrays for zero-allocation streaming
    val vertexBufferData = FloatArray(maxVertices * totalFloatsPerVertex)
    val indexBufferData = ShortArray(maxIndices)

    var currentRingCount = 0
        private set

    init {
        // Pre-build index buffer pattern
        var idx = 0
        for (ring in 0 until (maxRings - 1)) {
            val ringStart = ring * ringSegments
            val nextRingStart = (ring + 1) * ringSegments

            for (seg in 0 until ringSegments) {
                val nextSeg = (seg + 1) % ringSegments

                val p1 = (ringStart + seg).toShort()
                val p2 = (ringStart + nextSeg).toShort()
                val p3 = (nextRingStart + seg).toShort()
                val p4 = (nextRingStart + nextSeg).toShort()

                // Triangle 1
                indexBufferData[idx++] = p1
                indexBufferData[idx++] = p3
                indexBufferData[idx++] = p2

                // Triangle 2
                indexBufferData[idx++] = p2
                indexBufferData[idx++] = p3
                indexBufferData[idx++] = p4
            }
        }
    }

    fun reset() {
        currentRingCount = 0
    }

    /**
     * Appends a new ring along the conical helix.
     * @param t normalized or continuous progress in [0, 1]
     * @param activeFlavors list of active flavors for multi-color twisting
     * @return index of newly added ring, or -1 if full
     */
    fun appendRing(
        t: Float,
        activeFlavors: List<Flavor>
    ): Int {
        if (currentRingCount >= maxRings) return -1

        val ringIndex = currentRingCount

        // 1. Centerline along conical helix
        // Total rotations: ~4.5 turns from bottom to top
        val totalTurns = 4.2f
        val angle = t * totalTurns * 2.0f * Math.PI.toFloat()

        // Base helix radius tapers from 0.38f down to 0.08f
        val helixRadius = 0.38f * (1.0f - 0.76f * t)
        val cx = helixRadius * cos(angle)
        val cz = helixRadius * sin(angle)
        val cy = t * 1.55f // Height goes from 0.0 to 1.55

        // Tangent approximation
        val nextT = t + 0.01f
        val nextAngle = nextT * totalTurns * 2.0f * Math.PI.toFloat()
        val nextRadius = 0.38f * (1.0f - 0.76f * nextT)
        val ncx = nextRadius * cos(nextAngle)
        val ncz = nextRadius * sin(nextAngle)
        val ncy = nextT * 1.55f

        val tx = ncx - cx
        val ty = ncy - cy
        val tz = ncz - cz
        val tLen = sqrt(tx * tx + ty * ty + tz * tz).coerceAtLeast(0.0001f)
        val normTx = tx / tLen
        val normTy = ty / tLen
        val normTz = tz / tLen

        // Up vector reference
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f

        // Normal = Tangent x Up
        var nx = normTy * upZ - normTz * upY
        var ny = normTz * upX - normTx * upZ
        var nz = normTx * upY - normTy * upX
        val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        nx /= nLen
        ny /= nLen
        nz /= nLen

        // Binormal = Normal x Tangent
        val bx = ny * normTz - nz * normTy
        val by = nz * normTx - nx * normTz
        val bz = nx * normTy - ny * normTx

        // Cream tube cross-section radius tapers slightly towards tip
        val tubeRadius = 0.16f * (1.0f - 0.45f * t)

        val flavorsToUse = if (activeFlavors.isEmpty()) listOf(Flavor.VANILLA) else activeFlavors
        val numFlavors = flavorsToUse.size

        var offset = ringIndex * ringSegments * totalFloatsPerVertex

        for (seg in 0 until ringSegments) {
            val phi = (seg.toFloat() / ringSegments.toFloat()) * 2.0f * Math.PI.toFloat()

            // Star-shaped nozzle fluting modulation (8 or 6 lobes)
            val flute = 1.0f + 0.15f * cos(lobes * phi)
            val rCurrent = tubeRadius * flute

            val cosPhi = cos(phi)
            val sinPhi = sin(phi)

            // Radial direction vector
            val rx = cosPhi * nx + sinPhi * bx
            val ry = cosPhi * ny + sinPhi * by
            val rz = cosPhi * nz + sinPhi * bz

            // Vertex position
            val px = cx + rCurrent * rx
            val py = cy + rCurrent * ry
            val pz = cz + rCurrent * rz

            // Normal vector
            val normLen = sqrt(rx * rx + ry * ry + rz * rz).coerceAtLeast(0.0001f)
            val vnx = rx / normLen
            val vny = ry / normLen
            val vnz = rz / normLen

            // Multi-flavor coloring around cross-section
            val flavorIndex = ((phi / (2.0f * Math.PI.toFloat())) * numFlavors).toInt().coerceIn(0, numFlavors - 1)
            val flavor = flavorsToUse[flavorIndex]
            val rgb = flavor.rgbFloat

            // Write into native array
            vertexBufferData[offset++] = px
            vertexBufferData[offset++] = py
            vertexBufferData[offset++] = pz
            vertexBufferData[offset++] = vnx
            vertexBufferData[offset++] = vny
            vertexBufferData[offset++] = vnz
            vertexBufferData[offset++] = rgb[0]
            vertexBufferData[offset++] = rgb[1]
            vertexBufferData[offset++] = rgb[2]
            vertexBufferData[offset++] = 1.0f
        }

        currentRingCount++
        return ringIndex
    }

    fun getIndicesCount(): Int {
        if (currentRingCount < 2) return 0
        return (currentRingCount - 1) * ringSegments * 6
    }
}
