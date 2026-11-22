package com.icecream.diy.scene3d.procedural

import com.icecream.diy.scene3d.procedural.ConeMesh
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

data class CustomerVariantColors(
    val name: String,
    val skinColor: FloatArray,
    val hairColor: FloatArray,
    val shirtColor: FloatArray,
    val innerShirtColor: FloatArray?,
    val hasSunglasses: Boolean,
    val hasLongHair: Boolean
)

data class DollarBillParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var rotX: Float,
    var rotY: Float,
    var rotZ: Float,
    var rotSpeedX: Float,
    var rotSpeedY: Float,
    var isLanded: Boolean = false
)

object CustomerGenerator {

    val VARIANTS = listOf(
        // Variant 0: Cool Girl with Purple/Blue Hair & Sunglasses (Image 7)
        CustomerVariantColors(
            name = "Cool Girl",
            skinColor = floatArrayOf(0.96f, 0.82f, 0.72f, 1.0f),
            hairColor = floatArrayOf(0.35f, 0.40f, 0.75f, 1.0f),
            shirtColor = floatArrayOf(0.22f, 0.84f, 0.76f, 1.0f),
            innerShirtColor = null,
            hasSunglasses = true,
            hasLongHair = true
        ),
        // Variant 1: Hipster Guy with Burgundy Jacket & Mint Shirt (Image 6)
        CustomerVariantColors(
            name = "Hipster Guy",
            skinColor = floatArrayOf(0.72f, 0.52f, 0.40f, 1.0f),
            hairColor = floatArrayOf(0.24f, 0.20f, 0.18f, 1.0f),
            shirtColor = floatArrayOf(0.65f, 0.22f, 0.24f, 1.0f),
            innerShirtColor = floatArrayOf(0.24f, 0.80f, 0.68f, 1.0f),
            hasSunglasses = false,
            hasLongHair = false
        ),
        // Variant 2: Sunny Blonde with Coral Top
        CustomerVariantColors(
            name = "Sunny Blonde",
            skinColor = floatArrayOf(0.96f, 0.84f, 0.74f, 1.0f),
            hairColor = floatArrayOf(0.94f, 0.82f, 0.38f, 1.0f),
            shirtColor = floatArrayOf(0.95f, 0.45f, 0.52f, 1.0f),
            innerShirtColor = null,
            hasSunglasses = false,
            hasLongHair = true
        ),
        // Variant 3: Trendy Guy with Yellow Polo & Shades
        CustomerVariantColors(
            name = "Trendy Guy",
            skinColor = floatArrayOf(0.82f, 0.64f, 0.52f, 1.0f),
            hairColor = floatArrayOf(0.42f, 0.28f, 0.18f, 1.0f),
            shirtColor = floatArrayOf(0.95f, 0.72f, 0.18f, 1.0f),
            innerShirtColor = null,
            hasSunglasses = true,
            hasLongHair = false
        ),
        // Variant 4: Sporty Teen with Orange Hoodie
        CustomerVariantColors(
            name = "Sporty Teen",
            skinColor = floatArrayOf(0.88f, 0.72f, 0.58f, 1.0f),
            hairColor = floatArrayOf(0.18f, 0.62f, 0.55f, 1.0f),
            shirtColor = floatArrayOf(0.96f, 0.48f, 0.22f, 1.0f),
            innerShirtColor = floatArrayOf(0.98f, 0.98f, 0.98f, 1.0f),
            hasSunglasses = false,
            hasLongHair = false
        )
    )

    fun createBoxMesh(width: Float, height: Float, depth: Float): ConeMesh {
        val hw = width / 2f
        val hh = height / 2f
        val hd = depth / 2f

        val vertices = floatArrayOf(
            // Front face
            -hw, -hh,  hd,  0f, 0f, 1f,
             hw, -hh,  hd,  0f, 0f, 1f,
             hw,  hh,  hd,  0f, 0f, 1f,
            -hw,  hh,  hd,  0f, 0f, 1f,
            // Back face
             hw, -hh, -hd,  0f, 0f, -1f,
            -hw, -hh, -hd,  0f, 0f, -1f,
            -hw,  hh, -hd,  0f, 0f, -1f,
             hw,  hh, -hd,  0f, 0f, -1f,
            // Top face
            -hw,  hh,  hd,  0f, 1f, 0f,
             hw,  hh,  hd,  0f, 1f, 0f,
             hw,  hh, -hd,  0f, 1f, 0f,
            -hw,  hh, -hd,  0f, 1f, 0f,
            // Bottom face
            -hw, -hh, -hd,  0f, -1f, 0f,
             hw, -hh, -hd,  0f, -1f, 0f,
             hw, -hh,  hd,  0f, -1f, 0f,
            -hw, -hh,  hd,  0f, -1f, 0f,
            // Right face
             hw, -hh,  hd,  1f, 0f, 0f,
             hw, -hh, -hd,  1f, 0f, 0f,
             hw,  hh, -hd,  1f, 0f, 0f,
             hw,  hh,  hd,  1f, 0f, 0f,
            // Left face
            -hw, -hh, -hd, -1f, 0f, 0f,
            -hw, -hh,  hd, -1f, 0f, 0f,
            -hw,  hh,  hd, -1f, 0f, 0f,
            -hw,  hh, -hd, -1f, 0f, 0f
        )

        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7,
            8, 9, 10, 8, 10, 11,
            12, 13, 14, 12, 14, 15,
            16, 17, 18, 16, 18, 19,
            20, 21, 22, 20, 22, 23
        )

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

    fun createDollarBillMesh(): ConeMesh {
        val w = 0.16f
        val h = 0.08f
        val hw = w / 2f
        val hh = h / 2f

        val vertices = floatArrayOf(
            -hw, -hh, 0f,  0f, 0f, 1f,
             hw, -hh, 0f,  0f, 0f, 1f,
             hw,  hh, 0f,  0f, 0f, 1f,
            -hw,  hh, 0f,  0f, 0f, 1f,
            // back
             hw, -hh, 0f,  0f, 0f, -1f,
            -hw, -hh, 0f,  0f, 0f, -1f,
            -hw,  hh, 0f,  0f, 0f, -1f,
             hw,  hh, 0f,  0f, 0f, -1f
        )

        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,
            4, 5, 6, 4, 6, 7
        )

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

    fun createInitialDollarParticles(count: Int = 14): List<DollarBillParticle> {
        val list = mutableListOf<DollarBillParticle>()
        for (i in 0 until count) {
            val p = DollarBillParticle(
                x = (Math.random().toFloat() - 0.5f) * 0.9f,
                y = 1.2f + (Math.random().toFloat() * 0.8f),
                z = (Math.random().toFloat() - 0.5f) * 0.5f,
                vx = (Math.random().toFloat() - 0.5f) * 0.25f,
                vy = -0.55f - (Math.random().toFloat() * 0.45f),
                vz = (Math.random().toFloat() - 0.5f) * 0.2f,
                rotX = Math.random().toFloat() * 360f,
                rotY = Math.random().toFloat() * 360f,
                rotZ = Math.random().toFloat() * 360f,
                rotSpeedX = (Math.random().toFloat() - 0.5f) * 180f,
                rotSpeedY = (Math.random().toFloat() - 0.5f) * 220f
            )
            list.add(p)
        }
        return list
    }
}
