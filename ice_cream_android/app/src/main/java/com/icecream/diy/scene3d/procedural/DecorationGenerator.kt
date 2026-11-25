package com.icecream.diy.scene3d.procedural

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

object DecorationGenerator {

    // Procedural Cherry Sphere
    fun generateCherry(
        radius: Float = 0.12f,
        stacks: Int = 16,
        slices: Int = 16
    ): ConeMesh {
        val vertexCount = (stacks + 1) * (slices + 1)
        val vertices = FloatArray(vertexCount * 8)
        val indices = ShortArray(stacks * slices * 6)

        var vIdx = 0
        for (i in 0..stacks) {
            val v = i.toFloat() / stacks.toFloat()
            val phi = v * Math.PI.toFloat()
            val sinPhi = sin(phi)
            val cosPhi = cos(phi)

            for (j in 0..slices) {
                val u = j.toFloat() / slices.toFloat()
                val theta = u * 2.0f * Math.PI.toFloat()
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                val nx = cosTheta * sinPhi
                val ny = cosPhi
                val nz = sinTheta * sinPhi

                vertices[vIdx++] = radius * nx
                vertices[vIdx++] = radius * ny
                vertices[vIdx++] = radius * nz
                vertices[vIdx++] = nx
                vertices[vIdx++] = ny
                vertices[vIdx++] = nz
                vertices[vIdx++] = u
                vertices[vIdx++] = v
            }
        }

        var iIdx = 0
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val first = (i * (slices + 1) + j).toShort()
                val second = (first + slices + 1).toShort()

                indices[iIdx++] = first
                indices[iIdx++] = second
                indices[iIdx++] = (first + 1).toShort()

                indices[iIdx++] = second
                indices[iIdx++] = (second + 1).toShort()
                indices[iIdx++] = (first + 1).toShort()
            }
        }

        val vBuf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(vertices)
                position(0)
            }

        val iBuf = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(indices)
                position(0)
            }

        return ConeMesh(vBuf, iBuf, indices.size)
    }

    /**
     * Procedural curved low-poly cherry stem using a swept tube along a quadratic Bezier curve.
     */
    fun generateCherryStem(
        segments: Int = 8,
        radialSegments: Int = 6,
        radius: Float = 0.015f
    ): ConeMesh {
        val verticesList = mutableListOf<Float>()
        val indicesList = mutableListOf<Short>()

        for (i in 0..segments) {
            val t = i.toFloat() / segments.toFloat()
            // Quadratic Bezier from (0, 0.08, 0) -> control (0.01, 0.22, 0) -> end (0.11, 0.29, 0.02)
            val oneMinusT = 1f - t
            val cx = oneMinusT * oneMinusT * 0f + 2f * oneMinusT * t * 0.01f + t * t * 0.11f
            val cy = oneMinusT * oneMinusT * 0.08f + 2f * oneMinusT * t * 0.22f + t * t * 0.29f
            val cz = oneMinusT * oneMinusT * 0f + 2f * oneMinusT * t * 0f + t * t * 0.02f

            for (j in 0..radialSegments) {
                val u = j.toFloat() / radialSegments.toFloat()
                val angle = u * 2.0f * Math.PI.toFloat()
                val nx = cos(angle)
                val nz = sin(angle)

                verticesList.add(cx + radius * nx)
                verticesList.add(cy)
                verticesList.add(cz + radius * nz)
                verticesList.add(nx)
                verticesList.add(0.2f)
                verticesList.add(nz)
                verticesList.add(u)
                verticesList.add(t)
            }
        }

        val ringStride = radialSegments + 1
        for (i in 0 until segments) {
            for (j in 0 until radialSegments) {
                val first = (i * ringStride + j).toShort()
                val second = (first + ringStride).toShort()
                indicesList.add(first)
                indicesList.add(second)
                indicesList.add((first + 1).toShort())

                indicesList.add(second)
                indicesList.add((second + 1).toShort())
                indicesList.add((first + 1).toShort())
            }
        }

        val vertices = verticesList.toFloatArray()
        val indices = indicesList.toShortArray()

        val vBuf = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(vertices); position(0) }

        val iBuf = ByteBuffer.allocateDirect(indices.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply { put(indices); position(0) }

        return ConeMesh(vBuf, iBuf, indices.size)
    }

    // Procedural Sprinkles Batch (Capsules with various positions, rotations and pastel colors)
    data class SprinkleItem(
        val x: Float,
        val y: Float,
        val z: Float,
        val rotX: Float,
        val rotY: Float,
        val rotZ: Float,
        val color: FloatArray
    )

    fun createRandomSprinkles(count: Int, totalProgress: Float): List<SprinkleItem> {
        val list = mutableListOf<SprinkleItem>()
        val colors = listOf(
            floatArrayOf(1.0f, 0.45f, 0.70f, 1.0f), // Pastel pink
            floatArrayOf(0.40f, 0.85f, 0.95f, 1.0f), // Pastel cyan
            floatArrayOf(1.0f, 0.90f, 0.35f, 1.0f), // Pastel yellow
            floatArrayOf(0.55f, 0.32f, 0.18f, 1.0f), // Chocolate brown
            floatArrayOf(0.98f, 0.98f, 0.98f, 1.0f)  // White
        )

        val totalTurns = 4.2f
        for (i in 0 until count) {
            // Pick a height t between 0.15 and current pourProgress
            val t = 0.12f + (i.toFloat() / count.toFloat()) * (totalProgress * 0.82f)
            val angle = t * totalTurns * 2.0f * Math.PI.toFloat() + ((i * 137.5f) * Math.PI.toFloat() / 180f)

            val helixRadius = (0.38f * (1.0f - 0.76f * t)) + 0.15f
            val px = helixRadius * cos(angle)
            val pz = helixRadius * sin(angle)
            val py = t * 1.55f + 0.05f

            val rotX = (i * 45f) % 360f
            val rotY = (i * 73f) % 360f
            val rotZ = (i * 31f) % 360f
            val col = colors[i % colors.size]

            list.add(SprinkleItem(px, py, pz, rotX, rotY, rotZ, col))
        }
        return list
    }
}
