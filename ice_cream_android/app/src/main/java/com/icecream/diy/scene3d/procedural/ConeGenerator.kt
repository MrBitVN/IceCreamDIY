package com.icecream.diy.scene3d.procedural

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

class ConeMesh(
    val vertexBuffer: FloatBuffer,
    val indexBuffer: ShortBuffer,
    val indexCount: Int
)

object ConeGenerator {
    fun generateWaffleCone(
        topRadius: Float = 0.52f,
        bottomRadius: Float = 0.28f,
        height: Float = 0.95f,
        radialSegments: Int = 36
    ): ConeMesh {
        val halfHeight = height / 2.0f
        val vertexCount = (radialSegments + 1) * 2
        // Format: pos(x,y,z), normal(nx,ny,nz), uv(u,v) -> 8 floats per vertex
        val floatsPerVertex = 8
        val vertices = FloatArray(vertexCount * floatsPerVertex)
        val indices = ShortArray(radialSegments * 6)

        var vIdx = 0
        for (i in 0..radialSegments) {
            val u = i.toFloat() / radialSegments.toFloat()
            val angle = u * 2.0f * Math.PI.toFloat()
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top ring vertex
            vertices[vIdx++] = topRadius * cosA
            vertices[vIdx++] = halfHeight
            vertices[vIdx++] = topRadius * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = 0.15f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u * 6.0f
            vertices[vIdx++] = 0.0f

            // Bottom ring vertex
            vertices[vIdx++] = bottomRadius * cosA
            vertices[vIdx++] = -halfHeight
            vertices[vIdx++] = bottomRadius * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = -0.15f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u * 6.0f
            vertices[vIdx++] = 4.0f
        }

        var iIdx = 0
        for (i in 0 until radialSegments) {
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

    // Generate cylinder mesh for cone rim or dispenser beams
    fun generateCylinder(
        radius: Float,
        height: Float,
        segments: Int = 24
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
            val cosA = cos(angle)
            val sinA = sin(angle)

            // Top
            vertices[vIdx++] = radius * cosA
            vertices[vIdx++] = halfHeight
            vertices[vIdx++] = radius * sinA
            vertices[vIdx++] = cosA
            vertices[vIdx++] = 0.0f
            vertices[vIdx++] = sinA
            vertices[vIdx++] = u
            vertices[vIdx++] = 0.0f

            // Bottom
            vertices[vIdx++] = radius * cosA
            vertices[vIdx++] = -halfHeight
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
