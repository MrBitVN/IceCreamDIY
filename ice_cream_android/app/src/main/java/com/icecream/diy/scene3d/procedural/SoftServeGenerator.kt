package com.icecream.diy.scene3d.procedural

import com.icecream.diy.data.model.Flavor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

object SoftServeGenerator {
    // Generate star-shaped nozzle (fluted cone with 8 star teeth)
    fun generateStarNozzle(
        topRadius: Float = 0.16f,
        bottomRadius: Float = 0.09f,
        height: Float = 0.32f,
        teeth: Int = 8,
        segments: Int = 32
    ): ConeMesh {
        val halfHeight = height / 2.0f
        val vertexCount = (segments + 1) * 2
        val floatsPerVertex = 8
        val vertices = FloatArray(vertexCount * floatsPerVertex)
        val indices = ShortArray(segments * 6)

        var vIdx = 0
        for (i in 0..segments) {
            val u = i.toFloat() / segments.toFloat()
            val angle = u * 2.0f * Math.PI.toFloat()

            // Star tooth fluting modulation
            val starMod = 1.0f + 0.22f * cos(teeth * angle)
            val rTop = topRadius * starMod
            val rBot = bottomRadius * starMod

            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top ring
            vertices[vIdx++] = rTop * cosA
            vertices[vIdx++] = halfHeight
            vertices[vIdx++] = rTop * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = 0.15f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u
            vertices[vIdx++] = 0.0f

            // Bottom ring (nozzle tip)
            vertices[vIdx++] = rBot * cosA
            vertices[vIdx++] = -halfHeight
            vertices[vIdx++] = rBot * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = -0.15f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u
            vertices[vIdx++] = 1.0f
        }

        var iIdx = 0
        for (i in 0 until segments) {
            val p1 = (i * 2).toShort()
            val p2 = (i * 2 + 1).toShort()
            val p3 = ((i + 1) * 2).toShort()
            val p4 = ((i + 1) * 2 + 1).toShort()

            indices[iIdx++] = p1
            indices[iIdx++] = p2
            indices[iIdx++] = p3

            indices[iIdx++] = p3
            indices[iIdx++] = p2
            indices[iIdx++] = p4
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

    // Generate circular disc / ground plane for soft shadow
    fun generateShadowPlane(radius: Float = 0.65f, segments: Int = 32): ConeMesh {
        val vertexCount = segments + 2
        val vertices = FloatArray(vertexCount * 8)
        val indices = ShortArray(segments * 3)

        // Center vertex
        var vIdx = 0
        vertices[vIdx++] = 0.0f
        vertices[vIdx++] = 0.0f
        vertices[vIdx++] = 0.0f
        vertices[vIdx++] = 0.0f
        vertices[vIdx++] = 1.0f
        vertices[vIdx++] = 0.0f
        vertices[vIdx++] = 0.5f
        vertices[vIdx++] = 0.5f

        for (i in 0..segments) {
            val u = i.toFloat() / segments.toFloat()
            val angle = u * 2.0f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            vertices[vIdx++] = radius * cosA
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = radius * sinA
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = 1.0f
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = (cosA + 1f) * 0.5f
            vertices[vIdx++] = (sinA + 1f) * 0.5f
        }

        var iIdx = 0
        for (i in 1..segments) {
            indices[iIdx++] = 0
            indices[iIdx++] = i.toShort()
            indices[iIdx++] = (i + 1).toShort()
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

    // Dynamic vertical stream connecting nozzle to top of current spiral
    fun generateStreamMesh(
        startY: Float,
        endY: Float,
        radius: Float = 0.07f,
        segments: Int = 16
    ): ConeMesh {
        val height = (startY - endY).coerceAtLeast(0.01f)
        val halfH = height / 2.0f
        val vertexCount = (segments + 1) * 2
        val vertices = FloatArray(vertexCount * 8)
        val indices = ShortArray(segments * 6)

        var vIdx = 0
        for (i in 0..segments) {
            val u = i.toFloat() / segments.toFloat()
            val angle = u * 2.0f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top of stream
            vertices[vIdx++] = radius * cosA
            vertices[vIdx++] = halfH
            vertices[vIdx++] = radius * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u
            vertices[vIdx++] = 0.0f

            // Bottom of stream
            vertices[vIdx++] = radius * cosA
            vertices[vIdx++] = -halfH
            vertices[vIdx++] = radius * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u
            vertices[vIdx++] = 1.0f
        }

        var iIdx = 0
        for (i in 0 until segments) {
            val p1 = (i * 2).toShort()
            val p2 = (i * 2 + 1).toShort()
            val p3 = ((i + 1) * 2).toShort()
            val p4 = ((i + 1) * 2 + 1).toShort()

            indices[iIdx++] = p1
            indices[iIdx++] = p2
            indices[iIdx++] = p3

            indices[iIdx++] = p3
            indices[iIdx++] = p2
            indices[iIdx++] = p4
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
}
