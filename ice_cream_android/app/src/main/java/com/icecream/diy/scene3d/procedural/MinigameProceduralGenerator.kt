package com.icecream.diy.scene3d.procedural

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MinigameProceduralGenerator {

    private fun buildConeMesh(vertices: FloatArray, indices: ShortArray): ConeMesh {
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
     * Generates a 3D rounded-corner cutting board slab centered at (0, 0, 0) in XZ plane,
     * spanning Y in [-thickness/2, +thickness/2].
     */
    fun generateRoundedBoardMesh(
        width: Float = 2.50f,
        depth: Float = 1.32f,
        thickness: Float = 0.12f,
        cornerRadius: Float = 0.24f,
        cornerSegments: Int = 6
    ): ConeMesh {
        val perimeterCount = 4 * (cornerSegments + 1)
        val px = FloatArray(perimeterCount)
        val pz = FloatArray(perimeterCount)
        val nx = FloatArray(perimeterCount)
        val nz = FloatArray(perimeterCount)

        val hx = width * 0.5f - cornerRadius
        val hz = depth * 0.5f - cornerRadius

        val centersX = floatArrayOf(hx, -hx, -hx, hx)
        val centersZ = floatArrayOf(hz, hz, -hz, -hz)
        val startAngles = floatArrayOf(0f, (PI * 0.5).toFloat(), PI.toFloat(), (PI * 1.5).toFloat())

        var pIdx = 0
        for (c in 0 until 4) {
            for (s in 0..cornerSegments) {
                val frac = s.toFloat() / cornerSegments.toFloat()
                val angle = startAngles[c] + frac * (PI * 0.5).toFloat()
                val ca = cos(angle)
                val sa = sin(angle)
                px[pIdx] = centersX[c] + cornerRadius * ca
                pz[pIdx] = centersZ[c] + cornerRadius * sa
                nx[pIdx] = ca
                nz[pIdx] = sa
                pIdx++
            }
        }

        val halfY = thickness * 0.5f
        // Top center (1) + Top ring (P) + Bottom center (1) + Bottom ring (P) + Side top ring (P) + Side bottom ring (P)
        val totalVertices = 2 + perimeterCount * 4
        val floats = FloatArray(totalVertices * 8)
        val totalTriangles = perimeterCount * 4
        val indices = ShortArray(totalTriangles * 3)

        var vOut = 0
        fun putVertex(x: Float, y: Float, z: Float, vnx: Float, vny: Float, vnz: Float, u: Float, v: Float) {
            floats[vOut++] = x
            floats[vOut++] = y
            floats[vOut++] = z
            floats[vOut++] = vnx
            floats[vOut++] = vny
            floats[vOut++] = vnz
            floats[vOut++] = u
            floats[vOut++] = v
        }

        // 0: Top center
        putVertex(0f, halfY, 0f, 0f, 1f, 0f, 0.5f, 0.5f)
        // 1..P: Top perimeter
        for (i in 0 until perimeterCount) {
            putVertex(px[i], halfY, pz[i], 0f, 1f, 0f, (px[i] / width) + 0.5f, (pz[i] / depth) + 0.5f)
        }

        // 1 + P: Bottom center
        val bottomCenterIdx = 1 + perimeterCount
        putVertex(0f, -halfY, 0f, 0f, -1f, 0f, 0.5f, 0.5f)
        // 2 + P .. 1 + 2P: Bottom perimeter
        for (i in 0 until perimeterCount) {
            putVertex(px[i], -halfY, pz[i], 0f, -1f, 0f, (px[i] / width) + 0.5f, (pz[i] / depth) + 0.5f)
        }

        // Side wall rings (2 + 2P .. 1 + 4P)
        val sideBaseIdx = 2 + perimeterCount * 2
        for (i in 0 until perimeterCount) {
            val u = i.toFloat() / perimeterCount.toFloat()
            putVertex(px[i], halfY, pz[i], nx[i], 0.15f, nz[i], u, 0f)
            putVertex(px[i], -halfY, pz[i], nx[i], -0.15f, nz[i], u, 1f)
        }

        var iOut = 0
        // Top fan
        for (i in 0 until perimeterCount) {
            val next = (i + 1) % perimeterCount
            indices[iOut++] = 0
            indices[iOut++] = (1 + next).toShort()
            indices[iOut++] = (1 + i).toShort()
        }
        // Bottom fan
        for (i in 0 until perimeterCount) {
            val next = (i + 1) % perimeterCount
            indices[iOut++] = bottomCenterIdx.toShort()
            indices[iOut++] = (bottomCenterIdx + 1 + i).toShort()
            indices[iOut++] = (bottomCenterIdx + 1 + next).toShort()
        }
        // Side quads
        for (i in 0 until perimeterCount) {
            val next = (i + 1) % perimeterCount
            val t0 = (sideBaseIdx + i * 2).toShort()
            val b0 = (sideBaseIdx + i * 2 + 1).toShort()
            val t1 = (sideBaseIdx + next * 2).toShort()
            val b1 = (sideBaseIdx + next * 2 + 1).toShort()

            indices[iOut++] = t0
            indices[iOut++] = b0
            indices[iOut++] = t1

            indices[iOut++] = t1
            indices[iOut++] = b0
            indices[iOut++] = b1
        }

        return buildConeMesh(floats, indices)
    }

    /**
     * Low-poly 3D ellipsoid/sphere for Blueberries and Mango fruit.
     */
    fun generateEllipsoidMesh(
        radiusX: Float = 0.13f,
        radiusY: Float = 0.12f,
        radiusZ: Float = 0.13f,
        latSegments: Int = 10,
        lonSegments: Int = 14
    ): ConeMesh {
        val vertexCount = (latSegments + 1) * (lonSegments + 1)
        val floats = FloatArray(vertexCount * 8)
        val indices = ShortArray(latSegments * lonSegments * 6)

        var vIdx = 0
        for (lat in 0..latSegments) {
            val v = lat.toFloat() / latSegments.toFloat()
            val theta = v * PI.toFloat()
            val sinT = sin(theta)
            val cosT = cos(theta)

            for (lon in 0..lonSegments) {
                val u = lon.toFloat() / lonSegments.toFloat()
                val phi = u * 2f * PI.toFloat()
                val cosP = cos(phi)
                val sinP = sin(phi)

                val ux = sinT * cosP
                val uy = cosT
                val uz = sinT * sinP

                floats[vIdx++] = radiusX * ux
                floats[vIdx++] = radiusY * uy
                floats[vIdx++] = radiusZ * uz

                val nx = ux / radiusX.coerceAtLeast(0.01f)
                val ny = uy / radiusY.coerceAtLeast(0.01f)
                val nz = uz / radiusZ.coerceAtLeast(0.01f)
                val nLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)

                floats[vIdx++] = nx / nLen
                floats[vIdx++] = ny / nLen
                floats[vIdx++] = nz / nLen
                floats[vIdx++] = u
                floats[vIdx++] = v
            }
        }

        var iIdx = 0
        val stride = lonSegments + 1
        for (lat in 0 until latSegments) {
            for (lon in 0 until lonSegments) {
                val p1 = (lat * stride + lon).toShort()
                val p2 = ((lat + 1) * stride + lon).toShort()
                val p3 = (lat * stride + lon + 1).toShort()
                val p4 = ((lat + 1) * stride + lon + 1).toShort()

                indices[iIdx++] = p1
                indices[iIdx++] = p2
                indices[iIdx++] = p3

                indices[iIdx++] = p3
                indices[iIdx++] = p2
                indices[iIdx++] = p4
            }
        }

        return buildConeMesh(floats, indices)
    }

    /**
     * Low-poly 3D Mint Leaf (double-sided arched diamond/ovate leaf with raised central ridge).
     */
    fun generateMintLeafMesh(
        length: Float = 0.34f,
        width: Float = 0.22f,
        archHeight: Float = 0.05f
    ): ConeMesh {
        val halfL = length * 0.5f
        val halfW = width * 0.5f

        // 8 vertices top + 8 vertices bottom for crisp double-sided lighting
        val positions = arrayOf(
            floatArrayOf(0f, 0.01f, -halfL),                   // 0: base stem end
            floatArrayOf(-halfW * 0.75f, archHeight * 0.6f, -halfL * 0.35f), // 1: lower-left
            floatArrayOf(0f, archHeight, -halfL * 0.25f),      // 2: lower midrib
            floatArrayOf(halfW * 0.75f, archHeight * 0.6f, -halfL * 0.35f),  // 3: lower-right
            floatArrayOf(-halfW, archHeight * 0.7f, halfL * 0.20f),          // 4: upper-left
            floatArrayOf(0f, archHeight * 1.1f, halfL * 0.20f),              // 5: upper midrib
            floatArrayOf(halfW, archHeight * 0.7f, halfL * 0.20f),           // 6: upper-right
            floatArrayOf(0f, 0.02f, halfL)                     // 7: pointed tip
        )

        val floats = FloatArray(16 * 8)
        var vOut = 0
        // Top side vertices (0..7)
        for (i in 0 until 8) {
            val p = positions[i]
            val sideNx = when {
                p[0] < -0.01f -> -0.25f
                p[0] > 0.01f -> 0.25f
                else -> 0f
            }
            floats[vOut++] = p[0]
            floats[vOut++] = p[1]
            floats[vOut++] = p[2]
            floats[vOut++] = sideNx
            floats[vOut++] = 0.96f
            floats[vOut++] = 0f
            floats[vOut++] = (p[0] / width) + 0.5f
            floats[vOut++] = (p[2] / length) + 0.5f
        }
        // Bottom side vertices (8..15)
        for (i in 0 until 8) {
            val p = positions[i]
            floats[vOut++] = p[0]
            floats[vOut++] = p[1] - 0.015f
            floats[vOut++] = p[2]
            floats[vOut++] = 0f
            floats[vOut++] = -1f
            floats[vOut++] = 0f
            floats[vOut++] = (p[0] / width) + 0.5f
            floats[vOut++] = (p[2] / length) + 0.5f
        }

        val topTris = shortArrayOf(
            0, 2, 1,
            0, 3, 2,
            1, 2, 4,
            2, 5, 4,
            2, 3, 6,
            2, 6, 5,
            4, 5, 7,
            5, 6, 7
        )
        val indices = ShortArray(topTris.size * 2)
        var iOut = 0
        for (i in topTris.indices step 3) {
            indices[iOut++] = topTris[i]
            indices[iOut++] = topTris[i + 1]
            indices[iOut++] = topTris[i + 2]
        }
        for (i in topTris.indices step 3) {
            indices[iOut++] = (topTris[i] + 8).toShort()
            indices[iOut++] = (topTris[i + 2] + 8).toShort()
            indices[iOut++] = (topTris[i + 1] + 8).toShort()
        }

        return buildConeMesh(floats, indices)
    }

    /**
     * 3D Chef's Knife Blade (wedge cross-section along Z axis, spine on +Y, sharp edge on -Y).
     */
    fun generateKnifeBladeMesh(
        length: Float = 0.68f,
        height: Float = 0.16f,
        spineThickness: Float = 0.026f
    ): ConeMesh {
        val halfT = spineThickness * 0.5f
        val halfH = height * 0.5f
        // Left & right wedge faces + spine top face
        // Z = 0 is bolster base, Z = -length is tip
        val verts = arrayOf(
            // Left face (normal -X)
            floatArrayOf(-halfT, halfH, 0f, -0.98f, 0.15f, 0f),
            floatArrayOf(0f, -halfH, 0f, -0.98f, -0.15f, 0f),
            floatArrayOf(-halfT * 0.4f, halfH * 0.85f, -length * 0.72f, -0.98f, 0.15f, 0f),
            floatArrayOf(0f, -halfH * 0.65f, -length * 0.72f, -0.98f, -0.15f, 0f),
            floatArrayOf(0f, halfH * 0.35f, -length, -0.95f, 0f, -0.3f),

            // Right face (normal +X)
            floatArrayOf(halfT, halfH, 0f, 0.98f, 0.15f, 0f),
            floatArrayOf(0f, -halfH, 0f, 0.98f, -0.15f, 0f),
            floatArrayOf(halfT * 0.4f, halfH * 0.85f, -length * 0.72f, 0.98f, 0.15f, 0f),
            floatArrayOf(0f, -halfH * 0.65f, -length * 0.72f, 0.98f, -0.15f, 0f),
            floatArrayOf(0f, halfH * 0.35f, -length, 0.95f, 0f, -0.3f),

            // Top spine face (normal +Y)
            floatArrayOf(-halfT, halfH, 0f, 0f, 1f, 0f),
            floatArrayOf(halfT, halfH, 0f, 0f, 1f, 0f),
            floatArrayOf(-halfT * 0.4f, halfH * 0.85f, -length * 0.72f, 0f, 1f, 0f),
            floatArrayOf(halfT * 0.4f, halfH * 0.85f, -length * 0.72f, 0f, 1f, 0f),
            floatArrayOf(0f, halfH * 0.35f, -length, 0f, 1f, 0f)
        )

        val floats = FloatArray(verts.size * 8)
        var vOut = 0
        for (v in verts) {
            floats[vOut++] = v[0]
            floats[vOut++] = v[1]
            floats[vOut++] = v[2]
            floats[vOut++] = v[3]
            floats[vOut++] = v[4]
            floats[vOut++] = v[5]
            floats[vOut++] = 0.5f
            floats[vOut++] = 0.5f
        }

        val indices = shortArrayOf(
            // Left side
            0, 2, 1,
            1, 2, 3,
            2, 4, 3,
            // Right side
            5, 6, 7,
            6, 8, 7,
            7, 8, 9,
            // Top spine
            10, 11, 12,
            11, 13, 12,
            12, 13, 14
        )

        return buildConeMesh(floats, indices)
    }

    /**
     * Tapered cup / liquid column mesh spanning Y in [0, height] (bottom at Y=0, top at Y=height)
     * so scaling Y directly animates liquid filling from the bottom of the cup!
     */
    fun generateBottomAnchoredCupMesh(
        bottomRadius: Float = 0.21f,
        topRadius: Float = 0.30f,
        height: Float = 0.88f,
        segments: Int = 24,
        includeCaps: Boolean = true
    ): ConeMesh {
        val sideVerts = (segments + 1) * 2
        val capVerts = if (includeCaps) (segments + 2) * 2 else 0
        val floats = FloatArray((sideVerts + capVerts) * 8)
        val sideIndices = segments * 6
        val capIndices = if (includeCaps) segments * 6 else 0
        val indices = ShortArray(sideIndices + capIndices)

        var vOut = 0
        for (i in 0..segments) {
            val u = i.toFloat() / segments.toFloat()
            val angle = u * 2f * PI.toFloat()
            val ca = cos(angle)
            val sa = sin(angle)

            // Top ring at Y = height
            floats[vOut++] = topRadius * ca
            floats[vOut++] = height
            floats[vOut++] = topRadius * sa
            floats[vOut++] = ca
            floats[vOut++] = 0.12f
            floats[vOut++] = sa
            floats[vOut++] = u
            floats[vOut++] = 0f

            // Bottom ring at Y = 0
            floats[vOut++] = bottomRadius * ca
            floats[vOut++] = 0f
            floats[vOut++] = bottomRadius * sa
            floats[vOut++] = ca
            floats[vOut++] = -0.12f
            floats[vOut++] = sa
            floats[vOut++] = u
            floats[vOut++] = 1f
        }

        var iOut = 0
        for (i in 0 until segments) {
            val p1 = (i * 2).toShort()
            val p2 = (i * 2 + 1).toShort()
            val p3 = ((i + 1) * 2).toShort()
            val p4 = ((i + 1) * 2 + 1).toShort()

            indices[iOut++] = p1
            indices[iOut++] = p2
            indices[iOut++] = p3

            indices[iOut++] = p3
            indices[iOut++] = p2
            indices[iOut++] = p4
        }

        if (includeCaps) {
            val topCenterIdx = sideVerts
            floats[vOut++] = 0f; floats[vOut++] = height; floats[vOut++] = 0f
            floats[vOut++] = 0f; floats[vOut++] = 1f; floats[vOut++] = 0f
            floats[vOut++] = 0.5f; floats[vOut++] = 0.5f

            for (i in 0..segments) {
                val angle = (i.toFloat() / segments.toFloat()) * 2f * PI.toFloat()
                val ca = cos(angle)
                val sa = sin(angle)
                floats[vOut++] = topRadius * ca
                floats[vOut++] = height
                floats[vOut++] = topRadius * sa
                floats[vOut++] = 0f; floats[vOut++] = 1f; floats[vOut++] = 0f
                floats[vOut++] = 0.5f + 0.5f * ca; floats[vOut++] = 0.5f + 0.5f * sa
            }

            for (i in 1..segments) {
                indices[iOut++] = topCenterIdx.toShort()
                indices[iOut++] = (topCenterIdx + i + 1).toShort()
                indices[iOut++] = (topCenterIdx + i).toShort()
            }

            val botCenterIdx = topCenterIdx + segments + 2
            floats[vOut++] = 0f; floats[vOut++] = 0f; floats[vOut++] = 0f
            floats[vOut++] = 0f; floats[vOut++] = -1f; floats[vOut++] = 0f
            floats[vOut++] = 0.5f; floats[vOut++] = 0.5f

            for (i in 0..segments) {
                val angle = (i.toFloat() / segments.toFloat()) * 2f * PI.toFloat()
                val ca = cos(angle)
                val sa = sin(angle)
                floats[vOut++] = bottomRadius * ca
                floats[vOut++] = 0f
                floats[vOut++] = bottomRadius * sa
                floats[vOut++] = 0f; floats[vOut++] = -1f; floats[vOut++] = 0f
                floats[vOut++] = 0.5f + 0.5f * ca; floats[vOut++] = 0.5f + 0.5f * sa
            }

            for (i in 1..segments) {
                indices[iOut++] = botCenterIdx.toShort()
                indices[iOut++] = (botCenterIdx + i).toShort()
                indices[iOut++] = (botCenterIdx + i + 1).toShort()
            }
        }

        return buildConeMesh(floats, indices)
    }
}
