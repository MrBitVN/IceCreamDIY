package com.icecream.diy.scene3d.camera

import android.opengl.Matrix

object SceneLighting {
    val keyLightDirection = floatArrayOf(0.5f, 0.8f, 0.6f) // Normalized directional light
    val ambientColor = floatArrayOf(0.75f, 0.68f, 0.72f, 1.0f) // Soft pastel pink ambient
    val shadowColor = floatArrayOf(0.6f, 0.25f, 0.35f, 0.25f) // Soft shadow disk under cone

    fun createProjectionMatrix(width: Int, height: Int): FloatArray {
        val proj = FloatArray(16)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(proj, 0, 48.0f, aspect, 0.1f, 50.0f)
        return proj
    }

    fun createViewMatrix(): FloatArray {
        val view = FloatArray(16)
        // Camera positioned at (0, 0.45, 3.65) looking at (0, -0.15, 0)
        Matrix.setLookAtM(
            view, 0,
            0.0f, 0.45f, 3.65f,
            0.0f, -0.15f, 0.0f,
            0.0f, 1.0f, 0.0f
        )
        return view
    }
}
