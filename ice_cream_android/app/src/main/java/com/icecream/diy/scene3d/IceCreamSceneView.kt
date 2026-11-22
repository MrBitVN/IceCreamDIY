package com.icecream.diy.scene3d

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.icecream.diy.data.model.Flavor
import com.icecream.diy.data.model.GamePhase
import com.icecream.diy.domain.engine.MeshExtruder
import com.icecream.diy.scene3d.camera.SceneLighting
import com.icecream.diy.scene3d.procedural.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@Composable
fun IceCreamSceneView(
    activeFlavors: Set<Flavor>,
    isRotating: Boolean,
    pourProgress: Float = 0.0f,
    extraCones: Int = 1,
    sprinklesCount: Int = 0,
    hasCherry: Boolean = false,
    level: Int = 1,
    gamePhase: GamePhase = GamePhase.POURING,
    customerVariant: Int = 0,
    stars: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val glView = remember { IceCreamGLSurfaceView(context) }

    LaunchedEffect(
        activeFlavors,
        pourProgress,
        isRotating,
        extraCones,
        sprinklesCount,
        hasCherry,
        level,
        gamePhase,
        customerVariant,
        stars
    ) {
        glView.updateState(
            activeFlavors = activeFlavors.toList(),
            pourProgress = pourProgress,
            isRotating = isRotating,
            extraCones = extraCones,
            sprinklesCount = sprinklesCount,
            hasCherry = hasCherry,
            level = level,
            gamePhase = gamePhase,
            customerVariant = customerVariant,
            stars = stars
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier.fillMaxSize()
    )
}

class IceCreamGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: IceCreamRenderer = IceCreamRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(false)

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateState(
        activeFlavors: List<Flavor>,
        pourProgress: Float,
        isRotating: Boolean,
        extraCones: Int,
        sprinklesCount: Int,
        hasCherry: Boolean,
        level: Int,
        gamePhase: GamePhase,
        customerVariant: Int,
        stars: Int
    ) {
        renderer.activeFlavors = activeFlavors
        renderer.pourProgress = pourProgress
        renderer.isRotating = isRotating
        renderer.extraCones = extraCones
        renderer.sprinklesCount = sprinklesCount
        renderer.hasCherry = hasCherry
        renderer.level = level
        renderer.gamePhase = gamePhase
        renderer.customerVariant = customerVariant
        renderer.stars = stars
    }
}

class IceCreamRenderer : GLSurfaceView.Renderer {
    @Volatile var activeFlavors: List<Flavor> = emptyList()
    @Volatile var pourProgress: Float = 0.0f
    @Volatile var isRotating: Boolean = true
    @Volatile var extraCones: Int = 1
    @Volatile var sprinklesCount: Int = 0
    @Volatile var hasCherry: Boolean = false
    @Volatile var level: Int = 1
    @Volatile var gamePhase: GamePhase = GamePhase.POURING
    @Volatile var customerVariant: Int = 0
    @Volatile var stars: Int = 0

    // Extruder and its preallocated native buffers
    private val meshExtruder = MeshExtruder(ringSegments = 16, maxRings = 180, lobes = 6)
    private var lastExtrudedT = 0.0f
    private lateinit var swirlVertexBuffer: FloatBuffer
    private lateinit var swirlIndexBuffer: ShortBuffer

    // Shader programs
    private var programId = 0
    private var waffleProgramId = 0
    private var swirlProgramId = 0

    // Uniform handles
    private var uMVPMatrixHandle = 0
    private var uColorHandle = 0
    private var uLightPosHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0

    private var wU_MVPMatrixHandle = 0
    private var wU_ColorHandle = 0
    private var wA_PositionHandle = 0
    private var wA_NormalHandle = 0

    private var sU_MVPMatrixHandle = 0
    private var sU_LightPosHandle = 0
    private var sA_PositionHandle = 0
    private var sA_NormalHandle = 0
    private var sA_ColorHandle = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Cone & Dispenser Meshes
    private lateinit var coneMesh: ConeMesh
    private lateinit var coneRimMesh: ConeMesh
    private lateinit var shadowMesh: ConeMesh
    private lateinit var dispenserBeamMesh: ConeMesh
    private lateinit var dispenserNozzleMesh: ConeMesh
    private lateinit var canisterMesh: ConeMesh
    private lateinit var streamMesh: ConeMesh
    private lateinit var sprinkleMesh: ConeMesh
    private lateinit var cherryMesh: ConeMesh

    // Customer & Shop Meshes
    private lateinit var boxMesh: ConeMesh
    private lateinit var wallPlaneMesh: ConeMesh
    private lateinit var dollarBillMesh: ConeMesh

    private var cachedSprinkles: List<DecorationGenerator.SprinkleItem> = emptyList()
    private var lastSprinklesCount = 0

    // Dollar Bill Particles
    private var dollarParticles = CustomerGenerator.createInitialDollarParticles(16)

    // Animation variables
    private var coneRotationAngle = 0.0f
    private var lastFrameTime = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initShaders()

        // Allocate Swirl OpenGL Native Buffers
        swirlVertexBuffer = ByteBuffer.allocateDirect(meshExtruder.vertexBufferData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        swirlIndexBuffer = ByteBuffer.allocateDirect(meshExtruder.indexBufferData.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(meshExtruder.indexBufferData)
                position(0)
            }

        // Generate procedural geometry
        coneMesh = ConeGenerator.generateWaffleCone(topRadius = 0.44f, bottomRadius = 0.28f, height = 0.95f, radialSegments = 24)
        coneRimMesh = ConeGenerator.generateCylinder(radius = 0.47f, height = 0.08f, segments = 24)
        shadowMesh = SoftServeGenerator.generateShadowPlane(radius = 0.52f, segments = 24)
        dispenserBeamMesh = ConeGenerator.generateCylinder(radius = 0.09f, height = 1.25f, segments = 16)
        dispenserNozzleMesh = SoftServeGenerator.generateStarNozzle()
        canisterMesh = ConeGenerator.generateCylinder(radius = 0.09f, height = 0.42f, segments = 16)
        streamMesh = SoftServeGenerator.generateStreamMesh(startY = 0.8f, endY = -0.1f, radius = 0.06f)
        sprinkleMesh = ConeGenerator.generateCylinder(radius = 0.02f, height = 0.08f, segments = 8)
        cherryMesh = DecorationGenerator.generateCherry(radius = 0.12f)

        // Customer & Environment geometry
        boxMesh = CustomerGenerator.createBoxMesh(1f, 1f, 1f)
        wallPlaneMesh = ShopEnvironmentGenerator.createPlaneMesh(5.0f, 5.0f)
        dollarBillMesh = CustomerGenerator.createDollarBillMesh()

        lastFrameTime = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 48.0f, aspect, 0.1f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime) / 1000f).coerceIn(0.001f, 0.05f)
        lastFrameTime = now

        if (isRotating && gamePhase != GamePhase.CUSTOMER_REACTION) {
            coneRotationAngle += 36.0f * dt
            if (coneRotationAngle >= 360.0f) coneRotationAngle -= 360.0f
        }

        updateSoftServeExtrusion()
        updateDollarParticles(dt)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Camera setup
        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 0.45f, 3.65f,
            0.0f, -0.15f, 0.0f,
            0.0f, 1.0f, 0.0f
        )

        val coneY = -0.62f - (pourProgress * 0.70f)

        // 1. Render Shop Interior (when level >= 5)
        if (level >= 5) {
            renderShopInterior()
        }

        // 2. Render Floor Soft Shadow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, -1.15f, 0.0f)
        drawMesh(shadowMesh, programId, SceneLighting.shadowColor)

        // 3. Render Customer in Reaction Phase
        if (gamePhase == GamePhase.CUSTOMER_REACTION) {
            renderCustomer(stars == 3)
            renderDollarParticles()
        }

        // 4. Render Cones & Ice Cream
        renderIceCream(coneY)

        // 5. Render Dispenser Stand & Stream (Only when pouring/preparing)
        if (gamePhase != GamePhase.CUSTOMER_REACTION) {
            renderDispenserAndStream()
        }
    }

    private fun renderShopInterior() {
        // Back pink wall
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.6f, -1.8f)
        drawMesh(wallPlaneMesh, programId, floatArrayOf(1.0f, 0.82f, 0.85f, 1.0f))

        // Framed Poster
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.05f, 1.35f, -1.75f)
        Matrix.scaleM(modelMatrix, 0, 0.75f, 0.95f, 0.05f)
        drawMesh(boxMesh, programId, floatArrayOf(0.98f, 0.98f, 0.98f, 1.0f))

        // Teal Soft-serve Machine on the left
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -1.25f, 0.35f, -1.3f)
        Matrix.scaleM(modelMatrix, 0, 0.75f, 1.3f, 0.65f)
        drawMesh(boxMesh, programId, floatArrayOf(0.20f, 0.76f, 0.78f, 1.0f))

        // Front Pink Counter Table
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, -1.18f, -0.3f)
        Matrix.scaleM(modelMatrix, 0, 3.8f, 0.85f, 0.9f)
        drawMesh(boxMesh, programId, floatArrayOf(1.0f, 0.44f, 0.62f, 1.0f))
    }

    private fun renderCustomer(isThumbsUp: Boolean) {
        val variant = CustomerGenerator.VARIANTS[customerVariant % CustomerGenerator.VARIANTS.size]

        // Customer root position (standing behind counter)
        val cx = 0.0f
        val cy = -0.15f
        val cz = -0.55f

        // Torso
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        Matrix.scaleM(modelMatrix, 0, 0.58f, 0.65f, 0.28f)
        drawMesh(boxMesh, programId, variant.shirtColor)

        // Inner shirt (if applicable)
        if (variant.innerShirtColor != null) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, cy + 0.02f, cz + 0.01f)
            Matrix.scaleM(modelMatrix, 0, 0.26f, 0.55f, 0.29f)
            drawMesh(boxMesh, programId, variant.innerShirtColor)
        }

        // Neck
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.38f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.16f, 0.16f, 0.16f)
        drawMesh(boxMesh, programId, variant.skinColor)

        // Head
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.62f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.36f, 0.40f, 0.32f)
        drawMesh(boxMesh, programId, variant.skinColor)

        // Hair Top
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.78f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.39f, 0.14f, 0.35f)
        drawMesh(boxMesh, programId, variant.hairColor)

        if (variant.hasLongHair) {
            // Left long hair strand
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx - 0.21f, cy + 0.52f, cz + 0.02f)
            Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
            drawMesh(boxMesh, programId, variant.hairColor)

            // Right long hair strand
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx + 0.21f, cy + 0.52f, cz + 0.02f)
            Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
            drawMesh(boxMesh, programId, variant.hairColor)
        }

        // Sunglasses (if applicable)
        if (variant.hasSunglasses) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, cy + 0.64f, cz + 0.18f)
            Matrix.scaleM(modelMatrix, 0, 0.34f, 0.10f, 0.04f)
            drawMesh(boxMesh, programId, floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f))
        }

        // Left Arm (holding cone)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx - 0.32f, cy - 0.08f, cz + 0.18f)
        Matrix.scaleM(modelMatrix, 0, 0.12f, 0.38f, 0.12f)
        drawMesh(boxMesh, programId, variant.skinColor)

        // Right Arm (Thumbs up if 3 stars!)
        Matrix.setIdentityM(modelMatrix, 0)
        if (isThumbsUp) {
            Matrix.translateM(modelMatrix, 0, cx + 0.36f, cy + 0.12f, cz + 0.20f)
            Matrix.rotateM(modelMatrix, 0, 45f, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.12f, 0.36f, 0.12f)
            drawMesh(boxMesh, programId, variant.skinColor)

            // Thumb
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx + 0.36f, cy + 0.32f, cz + 0.24f)
            Matrix.scaleM(modelMatrix, 0, 0.06f, 0.10f, 0.06f)
            drawMesh(boxMesh, programId, variant.skinColor)
        } else {
            Matrix.translateM(modelMatrix, 0, cx + 0.34f, cy - 0.12f, cz + 0.06f)
            Matrix.scaleM(modelMatrix, 0, 0.12f, 0.38f, 0.12f)
            drawMesh(boxMesh, programId, variant.skinColor)
        }
    }

    private fun updateDollarParticles(dt: Float) {
        if (gamePhase != GamePhase.CUSTOMER_REACTION) return
        for (p in dollarParticles) {
            if (!p.isLanded) {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += p.vz * dt
                p.rotX += p.rotSpeedX * dt
                p.rotY += p.rotSpeedY * dt

                // Land on table counter
                if (p.y <= -0.72f) {
                    p.y = -0.72f
                    p.isLanded = true
                }
            }
        }
    }

    private fun renderDollarParticles() {
        val dollarColor = floatArrayOf(0.34f, 0.74f, 0.44f, 1.0f)
        for (p in dollarParticles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.rotateM(modelMatrix, 0, p.rotX, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, p.rotY, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, p.rotZ, 0f, 0f, 1f)
            drawMesh(dollarBillMesh, programId, dollarColor)
        }
    }

    private fun renderIceCream(coneY: Float) {
        // Extra Stacked Cones
        if (extraCones >= 2) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, coneY - 0.45f, 0.0f)
            Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
            drawMesh(coneMesh, waffleProgramId, floatArrayOf(0.88f, 0.74f, 0.52f, 1.0f))
        }

        // Main Waffle Cone
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, coneY, 0.0f)
        Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
        drawMesh(coneMesh, waffleProgramId, floatArrayOf(0.92f, 0.78f, 0.56f, 1.0f))

        // Cone Rim
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, coneY + 0.47f, 0.0f)
        Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
        drawMesh(coneRimMesh, programId, floatArrayOf(0.90f, 0.77f, 0.54f, 1.0f))

        // Swirl Mesh
        if (meshExtruder.currentRingCount >= 2) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, coneY + 0.48f, 0.0f)
            Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
            drawSwirlMesh()

            // Sprinkles
            if (sprinklesCount > 0) {
                if (sprinklesCount != lastSprinklesCount || cachedSprinkles.isEmpty()) {
                    cachedSprinkles = DecorationGenerator.createRandomSprinkles(sprinklesCount, pourProgress)
                    lastSprinklesCount = sprinklesCount
                }
                for (sp in cachedSprinkles) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, 0.0f, coneY + 0.48f, 0.0f)
                    Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
                    Matrix.translateM(modelMatrix, 0, sp.x, sp.y, sp.z)
                    Matrix.rotateM(modelMatrix, 0, sp.rotX, 1f, 0f, 0f)
                    Matrix.rotateM(modelMatrix, 0, sp.rotY, 0f, 1f, 0f)
                    drawMesh(sprinkleMesh, programId, sp.color)
                }
            }

            // Cherry
            if (hasCherry) {
                val peakY = coneY + 0.48f + (pourProgress * 1.55f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, 0.0f, peakY + 0.08f, 0.0f)
                Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
                drawMesh(cherryMesh, programId, floatArrayOf(0.92f, 0.08f, 0.16f, 1.0f))
            }
        }
    }

    private fun renderDispenserAndStream() {
        // Pouring Stream
        if (activeFlavors.isNotEmpty() && pourProgress < 1.0f) {
            val streamColor = activeFlavors.firstOrNull()?.rgbFloat ?: floatArrayOf(1f, 1f, 1f)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, 0.72f, 0.0f)
            drawMesh(streamMesh, programId, floatArrayOf(streamColor[0], streamColor[1], streamColor[2], 1.0f))
        }

        // T-shaped Dispenser Beam
        val woodColor = floatArrayOf(0.82f, 0.66f, 0.46f, 1.0f)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 1.35f, 0.0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0.0f, 0.0f, 1.0f)
        drawMesh(dispenserBeamMesh, programId, woodColor)

        // Star-shaped Nozzle
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 1.18f, 0.0f)
        drawMesh(dispenserNozzleMesh, programId, woodColor)

        // 3 Flavor Canisters
        drawCanister(-0.42f, floatArrayOf(0.98f, 0.98f, 0.96f, 1.0f))
        drawCanister(0.0f, floatArrayOf(1.0f, 0.52f, 0.84f, 1.0f))
        drawCanister(0.42f, floatArrayOf(0.44f, 0.26f, 0.16f, 1.0f))
    }

    private fun updateSoftServeExtrusion() {
        if (pourProgress < 0.001f) {
            if (meshExtruder.currentRingCount > 0) {
                meshExtruder.reset()
                lastExtrudedT = 0.0f
                cachedSprinkles = emptyList()
                dollarParticles = CustomerGenerator.createInitialDollarParticles(16)
            }
            return
        }

        val stepT = 0.008f
        var ringsAdded = false
        while (lastExtrudedT + stepT <= pourProgress && meshExtruder.currentRingCount < meshExtruder.maxRings) {
            lastExtrudedT += stepT
            meshExtruder.appendRing(lastExtrudedT, activeFlavors)
            ringsAdded = true
        }

        if (ringsAdded) {
            val totalFloats = meshExtruder.currentRingCount * meshExtruder.ringSegments * meshExtruder.totalFloatsPerVertex
            swirlVertexBuffer.position(0)
            swirlVertexBuffer.put(meshExtruder.vertexBufferData, 0, totalFloats)
            swirlVertexBuffer.position(0)
        }
    }

    private fun drawCanister(xOffset: Float, color: FloatArray) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, xOffset, 1.62f, 0.0f)
        drawMesh(canisterMesh, programId, color)
    }

    private fun drawSwirlMesh() {
        GLES20.glUseProgram(swirlProgramId)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(sU_MVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform3fv(sU_LightPosHandle, 1, SceneLighting.keyLightDirection, 0)

        val stride = meshExtruder.totalFloatsPerVertex * 4

        swirlVertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(sA_PositionHandle)
        GLES20.glVertexAttribPointer(sA_PositionHandle, 3, GLES20.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlVertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(sA_NormalHandle)
        GLES20.glVertexAttribPointer(sA_NormalHandle, 3, GLES20.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlVertexBuffer.position(6)
        GLES20.glEnableVertexAttribArray(sA_ColorHandle)
        GLES20.glVertexAttribPointer(sA_ColorHandle, 4, GLES20.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlIndexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, meshExtruder.getIndicesCount(), GLES20.GL_UNSIGNED_SHORT, swirlIndexBuffer)

        GLES20.glDisableVertexAttribArray(sA_PositionHandle)
        GLES20.glDisableVertexAttribArray(sA_NormalHandle)
        GLES20.glDisableVertexAttribArray(sA_ColorHandle)
    }

    private fun drawMesh(mesh: ConeMesh, prg: Int, color: FloatArray) {
        GLES20.glUseProgram(prg)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        if (prg == waffleProgramId) {
            GLES20.glUniformMatrix4fv(wU_MVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4fv(wU_ColorHandle, 1, color, 0)

            mesh.vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(wA_PositionHandle)
            GLES20.glVertexAttribPointer(wA_PositionHandle, 3, GLES20.GL_FLOAT, false, 8 * 4, mesh.vertexBuffer)

            mesh.vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(wA_NormalHandle)
            GLES20.glVertexAttribPointer(wA_NormalHandle, 3, GLES20.GL_FLOAT, false, 8 * 4, mesh.vertexBuffer)

            mesh.indexBuffer.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

            GLES20.glDisableVertexAttribArray(wA_PositionHandle)
            GLES20.glDisableVertexAttribArray(wA_NormalHandle)
        } else {
            GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4fv(uColorHandle, 1, color, 0)
            GLES20.glUniform3fv(uLightPosHandle, 1, SceneLighting.keyLightDirection, 0)

            val floatsPerVertex = if (mesh == boxMesh || mesh == dollarBillMesh || mesh == wallPlaneMesh) 6 else 8

            mesh.vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(aPositionHandle)
            GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, floatsPerVertex * 4, mesh.vertexBuffer)

            mesh.vertexBuffer.position(3)
            GLES20.glEnableVertexAttribArray(aNormalHandle)
            GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, floatsPerVertex * 4, mesh.vertexBuffer)

            mesh.indexBuffer.position(0)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

            GLES20.glDisableVertexAttribArray(aPositionHandle)
            GLES20.glDisableVertexAttribArray(aNormalHandle)
        }
    }

    private fun initShaders() {
        val vShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying vec3 vPosition;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = aNormal;
                vPosition = aPosition.xyz;
            }
        """.trimIndent()

        val fShaderCode = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightPos;
            varying vec3 vNormal;
            varying vec3 vPosition;
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.0);
                float diffuse = 0.35 + 0.65 * diff;
                gl_FragColor = vec4(uColor.rgb * diffuse, uColor.a);
            }
        """.trimIndent()

        val waffleVShader = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            varying vec3 vPosition;
            varying vec3 vNormal;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vPosition = aPosition.xyz;
                vNormal = aNormal;
            }
        """.trimIndent()

        val waffleFShader = """
            precision mediump float;
            uniform vec4 uColor;
            varying vec3 vPosition;
            varying vec3 vNormal;
            void main() {
                float u = (vPosition.x + 0.5) * 14.0;
                float v = (vPosition.y + 1.0) * 14.0;
                float pattern1 = abs(fract(u + v) - 0.5);
                float pattern2 = abs(fract(u - v) - 0.5);
                float grid = smoothstep(0.04, 0.12, min(pattern1, pattern2));
                vec3 baseColor = uColor.rgb;
                vec3 grooveColor = baseColor * 0.72;
                vec3 finalColor = mix(grooveColor, baseColor, grid);
                float diff = max(dot(normalize(vNormal), normalize(vec3(1.5, 2.0, 2.5))), 0.0);
                float lighting = 0.40 + 0.60 * diff;
                gl_FragColor = vec4(finalColor * lighting, 1.0);
            }
        """.trimIndent()

        val swirlVShader = """
            uniform mat4 uMVPMatrix;
            attribute vec4 aPosition;
            attribute vec3 aNormal;
            attribute vec4 aColor;
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec4 vColor;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = aNormal;
                vPosition = aPosition.xyz;
                vColor = aColor;
            }
        """.trimIndent()

        val swirlFShader = """
            precision mediump float;
            uniform vec3 uLightPos;
            varying vec3 vNormal;
            varying vec3 vPosition;
            varying vec4 vColor;
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.0);
                float diffuse = 0.45 + 0.55 * diff;
                gl_FragColor = vec4(vColor.rgb * diffuse, vColor.a);
            }
        """.trimIndent()

        programId = createProgram(vShaderCode, fShaderCode)
        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uColorHandle = GLES20.glGetUniformLocation(programId, "uColor")
        uLightPosHandle = GLES20.glGetUniformLocation(programId, "uLightPos")
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(programId, "aNormal")

        waffleProgramId = createProgram(waffleVShader, waffleFShader)
        wU_MVPMatrixHandle = GLES20.glGetUniformLocation(waffleProgramId, "uMVPMatrix")
        wU_ColorHandle = GLES20.glGetUniformLocation(waffleProgramId, "uColor")
        wA_PositionHandle = GLES20.glGetAttribLocation(waffleProgramId, "aPosition")
        wA_NormalHandle = GLES20.glGetAttribLocation(waffleProgramId, "aNormal")

        swirlProgramId = createProgram(swirlVShader, swirlFShader)
        sU_MVPMatrixHandle = GLES20.glGetUniformLocation(swirlProgramId, "uMVPMatrix")
        sU_LightPosHandle = GLES20.glGetUniformLocation(swirlProgramId, "uLightPos")
        sA_PositionHandle = GLES20.glGetAttribLocation(swirlProgramId, "aPosition")
        sA_NormalHandle = GLES20.glGetAttribLocation(swirlProgramId, "aNormal")
        sA_ColorHandle = GLES20.glGetAttribLocation(swirlProgramId, "aColor")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vCode: String, fCode: String): Int {
        val vShader = loadShader(GLES20.GL_VERTEX_SHADER, vCode)
        val fShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fCode)
        val prg = GLES20.glCreateProgram()
        GLES20.glAttachShader(prg, vShader)
        GLES20.glAttachShader(prg, fShader)
        GLES20.glLinkProgram(prg)
        return prg
    }
}
