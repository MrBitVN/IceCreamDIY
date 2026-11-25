package com.icecream.diy.scene3d

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlin.math.sin

@Composable
fun IceCreamSceneView(
    activeFlavors: Set<Flavor>,
    isRotating: Boolean,
    pourProgress: Float = 0.0f,
    extraCones: Int = 1,
    sprinklesCount: Int = 0,
    hasCherry: Boolean = false,
    isDecorated: Boolean = false,
    level: Int = 1,
    gamePhase: GamePhase = GamePhase.POURING,
    customerVariant: Int = 0,
    stars: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember { IceCreamGLSurfaceView(context) }

    LaunchedEffect(
        activeFlavors,
        pourProgress,
        isRotating,
        extraCones,
        sprinklesCount,
        hasCherry,
        isDecorated,
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
            isDecorated = isDecorated,
            level = level,
            gamePhase = gamePhase,
            customerVariant = customerVariant,
            stars = stars
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glView.onResume()
                Lifecycle.Event.ON_PAUSE -> glView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
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
        isDecorated: Boolean,
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
        renderer.isDecorated = isDecorated
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
    @Volatile var isDecorated: Boolean = false
    @Volatile var level: Int = 1
    @Volatile var gamePhase: GamePhase = GamePhase.POURING
    @Volatile var customerVariant: Int = 0
    @Volatile var stars: Int = 0

    // Extruder and its preallocated native buffers (Zero Allocation in Render Loop)
    private val meshExtruder = MeshExtruder(ringSegments = 16, maxRings = 180, lobes = 6)
    private var lastExtrudedT = 0.0f
    private lateinit var swirlVertexBuffer: FloatBuffer
    private lateinit var swirlIndexBuffer: ShortBuffer

    // Shader programs (OpenGL ES 3.0)
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
    private lateinit var floorPlaneMesh: ConeMesh
    private lateinit var dispenserBeamMesh: ConeMesh
    private lateinit var dispenserNozzleMesh: ConeMesh
    private lateinit var canisterMesh: ConeMesh
    private lateinit var streamMesh: ConeMesh
    private lateinit var sprinkleMesh: ConeMesh
    private lateinit var cherryMesh: ConeMesh
    private lateinit var cherryStemMesh: ConeMesh
    private var customerAnimTime = 0.0f

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

    // Preallocated render-loop buffers & color constants (Zero allocation per frame)
    private val floorColor = floatArrayOf(0.91f, 0.28f, 0.45f, 1.0f)
    private val wallColor = floatArrayOf(1.0f, 0.82f, 0.85f, 1.0f)
    private val wallSlatColor = floatArrayOf(0.94f, 0.72f, 0.76f, 1.0f)
    private val slatYs = floatArrayOf(-0.4f, 0.0f, 0.4f, 0.8f, 1.2f, 1.6f)
    private val posterFrameColor = floatArrayOf(0.98f, 0.98f, 0.98f, 1.0f)
    private val posterMatColor = floatArrayOf(0.99f, 0.90f, 0.92f, 1.0f)
    private val posterConeColor = floatArrayOf(0.92f, 0.71f, 0.44f, 1.0f)
    private val posterScoopColor = floatArrayOf(1.0f, 0.46f, 0.66f, 1.0f)
    private val mintMachineColor = floatArrayOf(0.24f, 0.84f, 0.76f, 1.0f)
    private val leverSilverColor = floatArrayOf(0.88f, 0.90f, 0.92f, 1.0f)
    private val counterPinkColor = floatArrayOf(1.0f, 0.44f, 0.62f, 1.0f)
    private val counterTrimColor = floatArrayOf(1.0f, 0.92f, 0.95f, 1.0f)
    private val sunglassesColor = floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f)
    private val dollarColor = floatArrayOf(0.34f, 0.74f, 0.44f, 1.0f)
    private val extraConeColor = floatArrayOf(0.88f, 0.74f, 0.52f, 1.0f)
    private val mainConeColor = floatArrayOf(0.92f, 0.78f, 0.56f, 1.0f)
    private val coneRimColor = floatArrayOf(0.90f, 0.77f, 0.54f, 1.0f)
    private val cherryRedColor = floatArrayOf(0.92f, 0.08f, 0.16f, 1.0f)
    private val cherryStemColor = floatArrayOf(0.35f, 0.22f, 0.12f, 1.0f)
    private val silverMetallicColor = floatArrayOf(0.86f, 0.88f, 0.90f, 1.0f)
    private val canisterWhiteColor = floatArrayOf(0.98f, 0.98f, 0.96f, 1.0f)
    private val canisterPinkColor = floatArrayOf(1.0f, 0.52f, 0.84f, 1.0f)
    private val canisterBrownColor = floatArrayOf(0.44f, 0.26f, 0.16f, 1.0f)
    private val defaultRgb = floatArrayOf(1f, 1f, 1f)
    private val streamColorBuf = floatArrayOf(1f, 1f, 1f, 1f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

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
        floorPlaneMesh = ShopEnvironmentGenerator.createPlaneMesh(6.0f, 4.0f)
        dispenserBeamMesh = ConeGenerator.generateCylinder(radius = 0.09f, height = 1.25f, segments = 16)
        dispenserNozzleMesh = SoftServeGenerator.generateStarNozzle()
        canisterMesh = ConeGenerator.generateCylinder(radius = 0.09f, height = 0.42f, segments = 16)
        streamMesh = SoftServeGenerator.generateStreamMesh(startY = 0.5f, endY = -0.5f, radius = 0.06f)
        sprinkleMesh = ConeGenerator.generateCylinder(radius = 0.02f, height = 0.08f, segments = 8)
        cherryMesh = DecorationGenerator.generateCherry(radius = 0.12f)
        cherryStemMesh = DecorationGenerator.generateCherryStem()

        // Customer & Environment geometry
        boxMesh = CustomerGenerator.createBoxMesh(1f, 1f, 1f)
        wallPlaneMesh = ShopEnvironmentGenerator.createPlaneMesh(5.0f, 5.0f)
        dollarBillMesh = CustomerGenerator.createDollarBillMesh()

        lastFrameTime = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 48.0f, aspect, 0.1f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.currentTimeMillis()
        val dt = ((now - lastFrameTime) / 1000f).coerceIn(0.001f, 0.05f)
        lastFrameTime = now

        // Continuous gentle idle cone rotation
        if (isRotating && gamePhase != GamePhase.CUSTOMER_REACTION) {
            coneRotationAngle += 36.0f * dt
            if (coneRotationAngle >= 360.0f) coneRotationAngle -= 360.0f
        }

        if (gamePhase == GamePhase.CUSTOMER_REACTION) {
            customerAnimTime += dt
        } else {
            customerAnimTime = 0.0f
        }

        updateSoftServeExtrusion()
        if (gamePhase == GamePhase.CUSTOMER_REACTION && stars == 3) {
            updateDollarParticles(dt)
        }

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        // Camera setup (Perspective viewing from slightly above)
        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 0.45f, 3.65f,
            0.0f, -0.15f, 0.0f,
            0.0f, 1.0f, 0.0f
        )

        val coneY = -0.62f - (pourProgress * 0.70f)

        // 1. Darker Pink Floor Plane (Hidden during DECORATING so swirl background fills view)
        if (gamePhase != GamePhase.DECORATING) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, -1.16f, 0.0f)
            Matrix.rotateM(modelMatrix, 0, -90f, 1.0f, 0.0f, 0.0f)
            drawMesh(floorPlaneMesh, programId, floorColor)
        }

        // 2. Shop Interior (when level >= 5 and not decorating)
        if (level >= 5 && gamePhase != GamePhase.DECORATING) {
            renderShopInterior()
        }

        // 3. Render Floor Soft Shadow under cone
        if (gamePhase != GamePhase.CUSTOMER_REACTION) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, -1.15f, 0.0f)
            drawMesh(shadowMesh, programId, SceneLighting.shadowColor)
        }

        // 4. Render Customer in Reaction Phase (and Dollar Particles only on 3 stars)
        if (gamePhase == GamePhase.CUSTOMER_REACTION) {
            renderCustomer(stars == 3)
            if (stars == 3) {
                renderDollarParticles()
            }
        }

        // 5. Render Cones & Ice Cream (placed in customer's left hand during CUSTOMER_REACTION)
        if (gamePhase == GamePhase.CUSTOMER_REACTION) {
            renderIceCream(coneY = -0.56f, coneX = -0.35f, coneZ = -0.32f, coneScale = 0.78f)
        } else {
            renderIceCream(coneY = coneY)
        }

        // 6. Render Silver Metallic Dispenser & Canisters (only during pouring)
        if (gamePhase == GamePhase.POURING) {
            renderDispenserAndStream()
        }
    }

    private fun renderShopInterior() {
        // Back pink wall
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.6f, -1.8f)
        drawMesh(wallPlaneMesh, programId, wallColor)

        // Horizontal wooden slats (tường kẻ gỗ ngang)
        for (sy in slatYs) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, sy, -1.78f)
            Matrix.scaleM(modelMatrix, 0, 5.0f, 0.03f, 0.02f)
            drawMesh(boxMesh, programId, wallSlatColor)
        }

        // Framed Poster with Ice Cream artwork inside
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.05f, 1.35f, -1.75f)
        Matrix.scaleM(modelMatrix, 0, 0.75f, 0.95f, 0.05f)
        drawMesh(boxMesh, programId, posterFrameColor)

        // Inner pink poster mat
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.05f, 1.35f, -1.72f)
        Matrix.scaleM(modelMatrix, 0, 0.62f, 0.80f, 0.02f)
        drawMesh(boxMesh, programId, posterMatColor)

        // Mini ice cream art inside poster (cone + pink scoop)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.05f, 1.20f, -1.69f)
        Matrix.scaleM(modelMatrix, 0, 0.14f, 0.22f, 0.03f)
        drawMesh(boxMesh, programId, posterConeColor)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.05f, 1.40f, -1.68f)
        Matrix.scaleM(modelMatrix, 0, 0.95f, 0.95f, 0.4f)
        drawMesh(cherryMesh, programId, posterScoopColor)

        // Mint / Teal Soft-serve Machine on the left with silver levers
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -1.25f, 0.35f, -1.3f)
        Matrix.scaleM(modelMatrix, 0, 0.75f, 1.3f, 0.65f)
        drawMesh(boxMesh, programId, mintMachineColor)

        for (i in -1..1) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, -1.25f + i * 0.16f, 0.68f, -0.95f)
            Matrix.scaleM(modelMatrix, 0, 0.05f, 0.22f, 0.06f)
            drawMesh(boxMesh, programId, leverSilverColor)
        }

        // Front Pink Counter Table with white top trim
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, -1.18f, -0.3f)
        Matrix.scaleM(modelMatrix, 0, 3.8f, 0.85f, 0.9f)
        drawMesh(boxMesh, programId, counterPinkColor)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, -0.74f, -0.3f)
        Matrix.scaleM(modelMatrix, 0, 3.85f, 0.07f, 0.94f)
        drawMesh(boxMesh, programId, counterTrimColor)
    }

    private fun renderCustomer(isThumbsUp: Boolean) {
        val variant = CustomerGenerator.VARIANTS[customerVariant % CustomerGenerator.VARIANTS.size]

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

        // Head animation offsets (happy nod for 3 stars, sad droop for < 3)
        val headBobY = if (isThumbsUp) sin(customerAnimTime * 7.0f) * 0.025f else -0.04f
        val headPitch = if (isThumbsUp) sin(customerAnimTime * 7.0f) * 10.0f else 22.0f
        val headYaw = if (isThumbsUp) 0.0f else sin(customerAnimTime * 3.5f) * 8.0f

        // Head
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.62f + headBobY, cz)
        Matrix.rotateM(modelMatrix, 0, headPitch, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, headYaw, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.36f, 0.40f, 0.32f)
        drawMesh(boxMesh, programId, variant.skinColor)

        // Hair Top
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.78f + headBobY, cz)
        Matrix.rotateM(modelMatrix, 0, headPitch, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, headYaw, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.39f, 0.14f, 0.35f)
        drawMesh(boxMesh, programId, variant.hairColor)

        if (variant.hasLongHair) {
            // Left long hair strand
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx - 0.21f, cy + 0.52f + headBobY, cz + 0.02f)
            Matrix.rotateM(modelMatrix, 0, headPitch, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
            drawMesh(boxMesh, programId, variant.hairColor)

            // Right long hair strand
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx + 0.21f, cy + 0.52f + headBobY, cz + 0.02f)
            Matrix.rotateM(modelMatrix, 0, headPitch, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
            drawMesh(boxMesh, programId, variant.hairColor)
        }

        // Sunglasses (if applicable)
        if (variant.hasSunglasses) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, cy + 0.64f + headBobY, cz + 0.18f)
            Matrix.rotateM(modelMatrix, 0, headPitch, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, headYaw, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.34f, 0.10f, 0.04f)
            drawMesh(boxMesh, programId, sunglassesColor)
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

                if (p.y <= -0.72f) {
                    p.y = -0.72f
                    p.isLanded = true
                }
            }
        }
    }

    private fun renderDollarParticles() {
        for (p in dollarParticles) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
            Matrix.rotateM(modelMatrix, 0, p.rotX, 1f, 0f, 0f)
            Matrix.rotateM(modelMatrix, 0, p.rotY, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, p.rotZ, 0f, 0f, 1f)
            drawMesh(dollarBillMesh, programId, dollarColor)
        }
    }

    private fun renderIceCream(
        coneY: Float,
        coneX: Float = 0.0f,
        coneZ: Float = 0.0f,
        coneScale: Float = 1.0f
    ) {
        // Extra Stacked Cones
        if (extraCones >= 2) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, coneX, coneY - 0.45f * coneScale, coneZ)
            Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
            Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
            drawMesh(coneMesh, waffleProgramId, extraConeColor)
        }

        // Main Waffle Cone with Procedural Diamond Grid Shader
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, coneX, coneY, coneZ)
        Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
        Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
        drawMesh(coneMesh, waffleProgramId, mainConeColor)

        // Cone Rim
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, coneX, coneY + 0.47f * coneScale, coneZ)
        Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
        Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
        drawMesh(coneRimMesh, programId, coneRimColor)

        // Swirl Mesh
        if (meshExtruder.currentRingCount >= 2) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, coneX, coneY + 0.48f * coneScale, coneZ)
            Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
            Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
            drawSwirlMesh()

            val showDecorations = isDecorated ||
                gamePhase == GamePhase.CUSTOMER_REACTION ||
                gamePhase == GamePhase.EVALUATING

            if (showDecorations) {
                // Sprinkles
                if (sprinklesCount > 0) {
                    if (sprinklesCount != lastSprinklesCount || cachedSprinkles.isEmpty()) {
                        cachedSprinkles = DecorationGenerator.createRandomSprinkles(sprinklesCount, pourProgress)
                        lastSprinklesCount = sprinklesCount
                    }
                    for (sp in cachedSprinkles) {
                        Matrix.setIdentityM(modelMatrix, 0)
                        Matrix.translateM(modelMatrix, 0, coneX, coneY + 0.48f * coneScale, coneZ)
                        Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
                        Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
                        Matrix.translateM(modelMatrix, 0, sp.x, sp.y, sp.z)
                        Matrix.rotateM(modelMatrix, 0, sp.rotX, 1f, 0f, 0f)
                        Matrix.rotateM(modelMatrix, 0, sp.rotY, 0f, 1f, 0f)
                        drawMesh(sprinkleMesh, programId, sp.color)
                    }
                }

                // Cherry + Curved Stem
                if (hasCherry) {
                    val peakY = coneY + (0.48f + (pourProgress * 1.55f)) * coneScale
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, coneX, peakY + 0.08f * coneScale, coneZ)
                    Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
                    Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
                    drawMesh(cherryMesh, programId, cherryRedColor)

                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, coneX, peakY + 0.08f * coneScale, coneZ)
                    Matrix.rotateM(modelMatrix, 0, coneRotationAngle, 0.0f, 1.0f, 0.0f)
                    Matrix.scaleM(modelMatrix, 0, coneScale, coneScale, coneScale)
                    drawMesh(cherryStemMesh, programId, cherryStemColor)
                }
            }
        }
    }

    private fun renderDispenserAndStream() {
        // Dynamic Pouring Stream connecting nozzle tip (Y=1.02) to current ice cream peak (peakY)
        if (activeFlavors.isNotEmpty() && pourProgress < 1.0f) {
            val coneY = -0.62f - (pourProgress * 0.70f)
            val peakY = coneY + 0.48f + (pourProgress * 1.55f)
            val nozzleTipY = 1.02f
            val streamHeight = (nozzleTipY - peakY).coerceAtLeast(0.04f)
            val streamCenterY = nozzleTipY - (streamHeight / 2.0f)

            val streamColor = activeFlavors.firstOrNull()?.rgbFloat ?: defaultRgb
            streamColorBuf[0] = streamColor[0]
            streamColorBuf[1] = streamColor[1]
            streamColorBuf[2] = streamColor[2]
            streamColorBuf[3] = 1.0f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0.0f, streamCenterY, 0.0f)
            Matrix.scaleM(modelMatrix, 0, 1.0f, streamHeight, 1.0f)
            drawMesh(streamMesh, programId, streamColorBuf)
        }

        // Silver Metallic Dispenser Beam
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 1.35f, 0.0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0.0f, 0.0f, 1.0f)
        drawMesh(dispenserBeamMesh, programId, silverMetallicColor)

        // Star-shaped Nozzle pointing down
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, 1.18f, 0.0f)
        drawMesh(dispenserNozzleMesh, programId, silverMetallicColor)

        // 3 Flavor Canisters (Vanilla, Strawberry, Chocolate)
        drawCanister(-0.42f, canisterWhiteColor)
        drawCanister(0.0f, canisterPinkColor)
        drawCanister(0.42f, canisterBrownColor)
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
        val previousRingCount = meshExtruder.currentRingCount
        var ringsAdded = false
        while (lastExtrudedT + stepT <= pourProgress && meshExtruder.currentRingCount < meshExtruder.maxRings) {
            lastExtrudedT += stepT
            meshExtruder.appendRing(lastExtrudedT, activeFlavors)
            ringsAdded = true
        }

        if (ringsAdded) {
            val floatsPerRing = meshExtruder.ringSegments * meshExtruder.totalFloatsPerVertex
            val startFloat = previousRingCount * floatsPerRing
            val newFloats = (meshExtruder.currentRingCount - previousRingCount) * floatsPerRing
            swirlVertexBuffer.position(startFloat)
            swirlVertexBuffer.put(meshExtruder.vertexBufferData, startFloat, newFloats)
            swirlVertexBuffer.position(0)
        }
    }

    private fun drawCanister(xOffset: Float, color: FloatArray) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, xOffset, 1.62f, 0.0f)
        drawMesh(canisterMesh, programId, color)
    }

    private fun drawSwirlMesh() {
        GLES30.glUseProgram(swirlProgramId)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
        GLES30.glUniformMatrix4fv(sU_MVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform3fv(sU_LightPosHandle, 1, SceneLighting.keyLightDirection, 0)

        val stride = meshExtruder.totalFloatsPerVertex * 4

        swirlVertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(sA_PositionHandle)
        GLES30.glVertexAttribPointer(sA_PositionHandle, 3, GLES30.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlVertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(sA_NormalHandle)
        GLES30.glVertexAttribPointer(sA_NormalHandle, 3, GLES30.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlVertexBuffer.position(6)
        GLES30.glEnableVertexAttribArray(sA_ColorHandle)
        GLES30.glVertexAttribPointer(sA_ColorHandle, 4, GLES30.GL_FLOAT, false, stride, swirlVertexBuffer)

        swirlIndexBuffer.position(0)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, meshExtruder.getIndicesCount(), GLES30.GL_UNSIGNED_SHORT, swirlIndexBuffer)

        GLES30.glDisableVertexAttribArray(sA_PositionHandle)
        GLES30.glDisableVertexAttribArray(sA_NormalHandle)
        GLES30.glDisableVertexAttribArray(sA_ColorHandle)
    }

    private fun drawMesh(mesh: ConeMesh, prg: Int, color: FloatArray) {
        GLES30.glUseProgram(prg)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        if (prg == waffleProgramId) {
            GLES30.glUniformMatrix4fv(wU_MVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES30.glUniform4fv(wU_ColorHandle, 1, color, 0)

            mesh.vertexBuffer.position(0)
            GLES30.glEnableVertexAttribArray(wA_PositionHandle)
            GLES30.glVertexAttribPointer(wA_PositionHandle, 3, GLES30.GL_FLOAT, false, 8 * 4, mesh.vertexBuffer)

            mesh.vertexBuffer.position(3)
            GLES30.glEnableVertexAttribArray(wA_NormalHandle)
            GLES30.glVertexAttribPointer(wA_NormalHandle, 3, GLES30.GL_FLOAT, false, 8 * 4, mesh.vertexBuffer)

            mesh.indexBuffer.position(0)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

            GLES30.glDisableVertexAttribArray(wA_PositionHandle)
            GLES30.glDisableVertexAttribArray(wA_NormalHandle)
        } else {
            GLES30.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
            GLES30.glUniform4fv(uColorHandle, 1, color, 0)
            GLES30.glUniform3fv(uLightPosHandle, 1, SceneLighting.keyLightDirection, 0)

            val floatsPerVertex = if (mesh == boxMesh || mesh == dollarBillMesh || mesh == wallPlaneMesh || mesh == floorPlaneMesh) 6 else 8

            mesh.vertexBuffer.position(0)
            GLES30.glEnableVertexAttribArray(aPositionHandle)
            GLES30.glVertexAttribPointer(aPositionHandle, 3, GLES30.GL_FLOAT, false, floatsPerVertex * 4, mesh.vertexBuffer)

            mesh.vertexBuffer.position(3)
            GLES30.glEnableVertexAttribArray(aNormalHandle)
            GLES30.glVertexAttribPointer(aNormalHandle, 3, GLES30.GL_FLOAT, false, floatsPerVertex * 4, mesh.vertexBuffer)

            mesh.indexBuffer.position(0)
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

            GLES30.glDisableVertexAttribArray(aPositionHandle)
            GLES30.glDisableVertexAttribArray(aNormalHandle)
        }
    }

    private fun initShaders() {
        val vShaderCode = """#version 300 es
            uniform mat4 uMVPMatrix;
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec3 aNormal;
            out vec3 vNormal;
            out vec3 vPosition;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = aNormal;
                vPosition = aPosition.xyz;
            }
        """.trimIndent()

        val fShaderCode = """#version 300 es
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightPos;
            in vec3 vNormal;
            in vec3 vPosition;
            out vec4 fragColor;
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.0);
                float diffuse = 0.38 + 0.62 * diff;
                fragColor = vec4(uColor.rgb * diffuse, uColor.a);
            }
        """.trimIndent()

        val waffleVShader = """#version 300 es
            uniform mat4 uMVPMatrix;
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec3 aNormal;
            out vec3 vPosition;
            out vec3 vNormal;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vPosition = aPosition.xyz;
                vNormal = aNormal;
            }
        """.trimIndent()

        val waffleFShader = """#version 300 es
            precision mediump float;
            uniform vec4 uColor;
            in vec3 vPosition;
            in vec3 vNormal;
            out vec4 fragColor;
            void main() {
                // Procedural Diamond Waffle Grid Pattern
                float u = (vPosition.x + 0.5) * 14.0;
                float v = (vPosition.y + 1.0) * 14.0;
                float pattern1 = abs(fract(u + v) - 0.5);
                float pattern2 = abs(fract(u - v) - 0.5);
                float grid = smoothstep(0.04, 0.12, min(pattern1, pattern2));
                vec3 baseColor = uColor.rgb;
                vec3 grooveColor = baseColor * 0.70;
                vec3 finalColor = mix(grooveColor, baseColor, grid);
                float diff = max(dot(normalize(vNormal), normalize(vec3(1.5, 2.0, 2.5))), 0.0);
                float lighting = 0.40 + 0.60 * diff;
                fragColor = vec4(finalColor * lighting, 1.0);
            }
        """.trimIndent()

        val swirlVShader = """#version 300 es
            uniform mat4 uMVPMatrix;
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec3 aNormal;
            layout(location = 2) in vec4 aColor;
            out vec3 vNormal;
            out vec3 vPosition;
            out vec4 vColor;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vNormal = aNormal;
                vPosition = aPosition.xyz;
                vColor = aColor;
            }
        """.trimIndent()

        val swirlFShader = """#version 300 es
            precision mediump float;
            uniform vec3 uLightPos;
            in vec3 vNormal;
            in vec3 vPosition;
            in vec4 vColor;
            out vec4 fragColor;
            void main() {
                vec3 norm = normalize(vNormal);
                vec3 lightDir = normalize(uLightPos - vPosition);
                float diff = max(dot(norm, lightDir), 0.0);
                float diffuse = 0.45 + 0.55 * diff;
                fragColor = vec4(vColor.rgb * diffuse, vColor.a);
            }
        """.trimIndent()

        programId = createProgram(vShaderCode, fShaderCode)
        uMVPMatrixHandle = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uColorHandle = GLES30.glGetUniformLocation(programId, "uColor")
        uLightPosHandle = GLES30.glGetUniformLocation(programId, "uLightPos")
        aPositionHandle = 0
        aNormalHandle = 1

        waffleProgramId = createProgram(waffleVShader, waffleFShader)
        wU_MVPMatrixHandle = GLES30.glGetUniformLocation(waffleProgramId, "uMVPMatrix")
        wU_ColorHandle = GLES30.glGetUniformLocation(waffleProgramId, "uColor")
        wA_PositionHandle = 0
        wA_NormalHandle = 1

        swirlProgramId = createProgram(swirlVShader, swirlFShader)
        sU_MVPMatrixHandle = GLES30.glGetUniformLocation(swirlProgramId, "uMVPMatrix")
        sU_LightPosHandle = GLES30.glGetUniformLocation(swirlProgramId, "uLightPos")
        sA_PositionHandle = 0
        sA_NormalHandle = 1
        sA_ColorHandle = 2
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, shaderCode)
        GLES30.glCompileShader(shader)
        return shader
    }

    private fun createProgram(vCode: String, fCode: String): Int {
        val vShader = loadShader(GLES30.GL_VERTEX_SHADER, vCode)
        val fShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fCode)
        val prg = GLES30.glCreateProgram()
        GLES30.glAttachShader(prg, vShader)
        GLES30.glAttachShader(prg, fShader)
        GLES30.glLinkProgram(prg)
        return prg
    }
}
