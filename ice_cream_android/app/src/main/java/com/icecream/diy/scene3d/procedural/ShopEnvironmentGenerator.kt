package com.icecream.diy.scene3d.procedural

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

object ShopEnvironmentGenerator {

    // Quad plane mesh for back wall, poster, banners
    fun createPlaneMesh(width: Float, height: Float): ConeMesh {
        val hw = width / 2f
        val hh = height / 2f

        val vertices = floatArrayOf(
            -hw, -hh, 0f,  0f, 0f, 1f,
             hw, -hh, 0f,  0f, 0f, 1f,
             hw,  hh, 0f,  0f, 0f, 1f,
            -hw,  hh, 0f,  0f, 0f, 1f
        )

        val indices = shortArrayOf(0, 1, 2, 0, 2, 3)

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

    // Slatted wall lines
    fun createWallPanelLines(): FloatArray {
        // Vertical lines / horizontal grooves on the pink back wall
        return floatArrayOf(
            -2.0f,  0.8f, -1.8f,   2.0f,  0.8f, -1.8f,
            -2.0f,  0.4f, -1.8f,   2.0f,  0.4f, -1.8f,
            -2.0f,  0.0f, -1.8f,   2.0f,  0.0f, -1.8f,
            -2.0f, -0.4f, -1.8f,   2.0f, -0.4f, -1.8f
        )
    }
}
