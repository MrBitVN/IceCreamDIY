package com.icecream.diy.scene3d

import android.annotation.SuppressLint
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.icecream.diy.domain.engine.MinigameEngine
import com.icecream.diy.domain.engine.MinigameIngredient
import com.icecream.diy.scene3d.procedural.ConeGenerator
import com.icecream.diy.scene3d.procedural.ConeMesh
import com.icecream.diy.scene3d.procedural.CustomerGenerator
import com.icecream.diy.scene3d.procedural.MinigameProceduralGenerator
import com.icecream.diy.scene3d.procedural.ShopEnvironmentGenerator
import com.icecream.diy.scene3d.procedural.SoftServeGenerator
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@SuppressLint("ClickableViewAccessibility")
@Composable
fun SlicingMinigameSceneView(
    onKnifeSlice: (MinigameIngredient, Float, Float) -> Unit,
    onSwipeTrail: (Float, Float) -> Unit,
    onIngredientSorted: (MinigameIngredient, Float, Float) -> Unit,
    onWrongCompartment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember {
        SlicingMinigameRenderer(
            onKnifeSlice = onKnifeSlice,
            onIngredientSorted = onIngredientSorted,
            onWrongCompartment = onWrongCompartment
        )
    }
    var glViewRef: GLSurfaceView? = remember { null }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glViewRef?.onPause()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            onSwipeTrail(event.x, event.y)
                            renderer.handleTouchDown(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            onSwipeTrail(event.x, event.y)
                            renderer.handleTouchMove(event.x, event.y)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            renderer.handleTouchUp(event.x, event.y)
                            true
                        }
                        else -> false
                    }
                }
                glViewRef = this
            }
        }
    )
}

@Composable
fun BlenderMinigameSceneView(
    fillProgress: Float,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { BlenderMinigameRenderer() }
    var glViewRef: GLSurfaceView? = remember { null }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> glViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> glViewRef?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            glViewRef?.onPause()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            GLSurfaceView(ctx).apply {
                setEGLContextClientVersion(3)
                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                preserveEGLContextOnPause = true
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                glViewRef = this
            }
        },
        update = {
            renderer.fillProgress = fillProgress
        }
    )
}

// ============================================================================
// 1. SLICING & SORTING 3D PROCEDURAL RENDERER (ZERO ALLOCATION IN RENDER LOOP)
// ============================================================================
private class SlicingMinigameRenderer(
    private val onKnifeSlice: (MinigameIngredient, Float, Float) -> Unit,
    private val onIngredientSorted: (MinigameIngredient, Float, Float) -> Unit,
    private val onWrongCompartment: () -> Unit
) : GLSurfaceView.Renderer {

    private enum class SlotPhase {
        WHOLE,
        SLICED_ON_BOARD,
        DRAGGING,
        FLYING_TO_BOX,
        SORTED_IN_BOX
    }

    private class SlotState(val ingredient: MinigameIngredient) {
        @Volatile var phase: SlotPhase = SlotPhase.WHOLE
        @Volatile var currX: Float = ingredient.boardX
        @Volatile var currY: Float = 0.14f
        @Volatile var currZ: Float = ingredient.boardZ
        @Volatile var flightStartX: Float = ingredient.boardX
        @Volatile var flightStartZ: Float = ingredient.boardZ
        @Volatile var flightT: Float = 0f
        @Volatile var bounceElapsed: Float = 1.0f
        @Volatile var slicePopElapsed: Float = 1.0f
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val slots = arrayOf(
        SlotState(MinigameIngredient.BLUEBERRY),
        SlotState(MinigameIngredient.MINT),
        SlotState(MinigameIngredient.MANGO)
    )

    // Pre-allocated matrices & scratch buffers (Zero GC in render loop)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val invVPMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private val nearIn = FloatArray(4)
    private val farIn = FloatArray(4)
    private val nearOut = FloatArray(4)
    private val farOut = FloatArray(4)
    private val rayHitXZ = FloatArray(2)
    private val projIn = FloatArray(4)
    private val projOut = FloatArray(4)

    // Pre-allocated colors (Zero GC in render loop)
    private val lightDir = floatArrayOf(0.35f, 0.85f, 0.42f)
    private val tabletopColor = floatArrayOf(0.96f, 0.39f, 0.56f, 1.0f)
    private val shadowColor = floatArrayOf(0.68f, 0.18f, 0.34f, 0.38f)
    private val boardPinkColor = floatArrayOf(0.98f, 0.73f, 0.81f, 1.0f)
    private val boardHoleColor = floatArrayOf(0.92f, 0.38f, 0.55f, 1.0f)
    private val boxOuterWoodColor = floatArrayOf(0.72f, 0.47f, 0.27f, 1.0f)
    private val boxInnerFloorColor = floatArrayOf(0.94f, 0.81f, 0.64f, 1.0f)
    private val boxWrongFlashColor = floatArrayOf(0.96f, 0.32f, 0.36f, 1.0f)

    private val blueberryColor = floatArrayOf(0.26f, 0.36f, 0.78f, 1.0f)
    private val blueberryDarkColor = floatArrayOf(0.16f, 0.22f, 0.54f, 1.0f)
    private val mintLeafColor = floatArrayOf(0.20f, 0.80f, 0.42f, 1.0f)
    private val mintStemColor = floatArrayOf(0.14f, 0.58f, 0.30f, 1.0f)
    private val mangoGoldColor = floatArrayOf(1.00f, 0.70f, 0.10f, 1.0f)
    private val mangoBlushColor = floatArrayOf(1.00f, 0.38f, 0.28f, 1.0f)

    private val knifeSilverColor = floatArrayOf(0.90f, 0.93f, 0.96f, 1.0f)
    private val knifeHandleColor = floatArrayOf(1.00f, 0.46f, 0.66f, 1.0f)

    // Pre-allocated piece offsets for 3 sliced pieces
    private val pieceOffsetsX = floatArrayOf(-0.11f, 0.11f, 0.0f)
    private val pieceOffsetsZ = floatArrayOf(-0.07f, -0.05f, 0.10f)
    private val pieceRotAngles = floatArrayOf(-22f, 28f, 8f)
    private val boxWallXs = floatArrayOf(-1.26f, -0.41f, 0.41f, 1.26f)
    private val mintLeafAngles = floatArrayOf(-38f, 38f, 0f)

    // Compartment shake/flash state (indexed by MinigameIngredient.index)
    private val wrongFlashTimers = FloatArray(3)

    // Knife state
    @Volatile private var knifeX: Float = 1.08f
    @Volatile private var knifeZ: Float = -0.52f
    @Volatile private var knifeChopTimer: Float = 0f
    @Volatile private var activeDragSlot: Int = -1
    private var touchDownScreenX: Float = 0f
    private var touchDownScreenY: Float = 0f

    @Volatile private var viewportW: Int = 1
    @Volatile private var viewportH: Int = 1

    private var programId = 0
    private var uMVPHandle = 0
    private var uColorHandle = 0
    private var uLightDirHandle = 0

    // Meshes
    private lateinit var tabletopMesh: ConeMesh
    private lateinit var boardMesh: ConeMesh
    private lateinit var boxMesh: ConeMesh
    private lateinit var berryMesh: ConeMesh
    private lateinit var mintLeafMesh: ConeMesh
    private lateinit var mangoWholeMesh: ConeMesh
    private lateinit var knifeBladeMesh: ConeMesh
    private lateinit var cylinderMesh: ConeMesh

    private var lastTimeNs: Long = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(1.0f, 0.49f, 0.65f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        programId = MinigameGlShader.createLitProgram()
        uMVPHandle = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uColorHandle = GLES30.glGetUniformLocation(programId, "uColor")
        uLightDirHandle = GLES30.glGetUniformLocation(programId, "uLightDir")

        tabletopMesh = ShopEnvironmentGenerator.createPlaneMesh(8.0f, 8.0f)
        boardMesh = MinigameProceduralGenerator.generateRoundedBoardMesh(
            width = 2.62f,
            depth = 1.28f,
            thickness = 0.12f,
            cornerRadius = 0.24f,
            cornerSegments = 6
        )
        boxMesh = CustomerGenerator.createBoxMesh(1f, 1f, 1f)
        berryMesh = MinigameProceduralGenerator.generateEllipsoidMesh(0.115f, 0.10f, 0.115f, 10, 14)
        mintLeafMesh = MinigameProceduralGenerator.generateMintLeafMesh(0.32f, 0.21f, 0.055f)
        mangoWholeMesh = MinigameProceduralGenerator.generateEllipsoidMesh(0.21f, 0.15f, 0.15f, 12, 16)
        knifeBladeMesh = MinigameProceduralGenerator.generateKnifeBladeMesh(0.68f, 0.16f, 0.026f)
        cylinderMesh = ConeGenerator.generateCylinder(0.055f, 0.28f, 14)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 3.85f, 3.25f,
            0.0f, -0.10f, 0.10f,
            0.0f, 1.0f, 0.0f
        )

        lastTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportW = width.coerceAtLeast(1)
        viewportH = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, viewportW, viewportH)
        val aspect = viewportW.toFloat() / viewportH.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 45.0f, aspect, 0.1f, 30.0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invVPMatrix, 0, vpMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTimeNs = now

        updateAnimations(dt)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(programId)
        GLES30.glUniform3fv(uLightDirHandle, 1, lightDir, 0)

        // 1. Pink Tabletop
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.04f, 0f)
        Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f)
        drawMesh(tabletopMesh, tabletopColor)

        // 2. Cutting Board Soft Shadow & Pink Rounded Cutting Board
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.04f, -0.02f, -0.74f)
        Matrix.scaleM(modelMatrix, 0, 1.03f, 0.15f, 1.04f)
        drawMesh(boardMesh, shadowColor)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 0.05f, -0.78f)
        drawMesh(boardMesh, boardPinkColor)

        // Handle cutout slot on left side of pink board
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -1.08f, 0.112f, -0.78f)
        Matrix.scaleM(modelMatrix, 0, 0.11f, 0.02f, 0.42f)
        drawMesh(boxMesh, boardHoleColor)

        // 3. 3-Compartment Wooden Sorting Box (Z centered at +0.82)
        drawSortingBox()

        // 4. Miniature Hint Pieces permanently inside each compartment (like reference photo 4)
        drawCompartmentSampleHints()

        // 5. Dynamic 3D Procedural Ingredients (Blueberry, Mint, Mango)
        for (i in slots.indices) {
            drawSlotIngredient(slots[i])
        }

        // 6. 3D Procedural Chef Knife
        drawChefKnife()
    }

    private fun updateAnimations(dt: Float) {
        if (knifeChopTimer > 0f) {
            knifeChopTimer = (knifeChopTimer - dt).coerceAtLeast(0f)
        }
        for (i in wrongFlashTimers.indices) {
            if (wrongFlashTimers[i] > 0f) {
                wrongFlashTimers[i] = (wrongFlashTimers[i] - dt).coerceAtLeast(0f)
            }
        }
        for (i in slots.indices) {
            val s = slots[i]
            if (s.slicePopElapsed < 1.0f) {
                s.slicePopElapsed += dt
            }
            when (s.phase) {
                SlotPhase.FLYING_TO_BOX -> {
                    s.flightT += dt * 3.4f
                    if (s.flightT >= 1.0f) {
                        s.flightT = 1.0f
                        s.currX = s.ingredient.targetCenterX
                        s.currZ = s.ingredient.targetCenterZ
                        s.currY = 0.14f
                        s.phase = SlotPhase.SORTED_IN_BOX
                        s.bounceElapsed = 0f

                        val screenPos = projectToScreen(s.currX, s.currY, s.currZ)
                        val sx = screenPos[0]
                        val sy = screenPos[1]
                        val ing = s.ingredient
                        mainHandler.post {
                            onIngredientSorted(ing, sx, sy)
                        }
                    } else {
                        val u = s.flightT
                        s.currX = s.flightStartX + (s.ingredient.targetCenterX - s.flightStartX) * u
                        s.currZ = s.flightStartZ + (s.ingredient.targetCenterZ - s.flightStartZ) * u
                        s.currY = 0.14f + sin(u * PI.toFloat()) * 0.75f
                    }
                }
                SlotPhase.SORTED_IN_BOX -> {
                    if (s.bounceElapsed < 1.0f) {
                        s.bounceElapsed += dt
                    }
                }
                else -> {}
            }
        }
    }

    private fun drawSortingBox() {
        val boxZ = 0.82f
        val boxW = 2.52f
        val boxD = 1.36f
        val wallH = 0.28f
        val wallThick = 0.06f

        // Soft shadow under box
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.04f, -0.02f, boxZ + 0.05f)
        Matrix.scaleM(modelMatrix, 0, boxW + 0.08f, 0.02f, boxD + 0.08f)
        drawMesh(boxMesh, shadowColor)

        // Floor of each of the 3 compartments (flashes coral-red if wrong drop)
        val entries = MinigameIngredient.values()
        for (i in entries.indices) {
            val ing = entries[i]
            val shakeX = if (wrongFlashTimers[i] > 0f) sin(wrongFlashTimers[i] * 48f) * 0.04f else 0f
            val floorCol = if (wrongFlashTimers[i] > 0f) boxWrongFlashColor else boxInnerFloorColor
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, ing.targetCenterX + shakeX, 0.03f, boxZ)
            Matrix.scaleM(modelMatrix, 0, 0.80f, 0.05f, boxD - 0.08f)
            drawMesh(boxMesh, floorCol)
        }

        // Front & Back outer walls
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, wallH * 0.5f, boxZ + boxD * 0.5f)
        Matrix.scaleM(modelMatrix, 0, boxW, wallH, wallThick)
        drawMesh(boxMesh, boxOuterWoodColor)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, wallH * 0.5f, boxZ - boxD * 0.5f)
        Matrix.scaleM(modelMatrix, 0, boxW, wallH, wallThick)
        drawMesh(boxMesh, boxOuterWoodColor)

        // Left & Right outer walls + 2 inner dividers
        for (wx in boxWallXs) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, wx, wallH * 0.5f, boxZ)
            Matrix.scaleM(modelMatrix, 0, wallThick, wallH, boxD)
            drawMesh(boxMesh, boxOuterWoodColor)
        }
    }

    private fun drawCompartmentSampleHints() {
        // Subtle smaller sample inside upper corner of each compartment so player sees target compartment clearly
        // Mint in left compartment (-0.82, 0.52)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.96f, 0.08f, 0.44f)
        Matrix.rotateM(modelMatrix, 0, 25f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.62f, 0.62f, 0.62f)
        drawMesh(mintLeafMesh, mintLeafColor)

        // Mango cube in center compartment (0.0, 0.44)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -0.16f, 0.10f, 0.44f)
        Matrix.rotateM(modelMatrix, 0, 18f, 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.11f, 0.09f, 0.11f)
        drawMesh(boxMesh, mangoGoldColor)

        // Blueberry in right compartment (+0.82, 0.44)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.66f, 0.11f, 0.44f)
        Matrix.scaleM(modelMatrix, 0, 0.72f, 0.72f, 0.72f)
        drawMesh(berryMesh, blueberryColor)
    }

    private fun drawSlotIngredient(slot: SlotState) {
        if (slot.phase == SlotPhase.WHOLE) {
            drawWholeIngredient(slot.ingredient, slot.currX, slot.currY, slot.currZ)
        } else {
            val scale = when (slot.phase) {
                SlotPhase.SORTED_IN_BOX -> MinigameEngine.elasticBounceScale(slot.bounceElapsed)
                SlotPhase.SLICED_ON_BOARD -> MinigameEngine.elasticBounceScale(slot.slicePopElapsed)
                SlotPhase.DRAGGING -> 1.15f
                SlotPhase.FLYING_TO_BOX -> 1.08f
                else -> 1.0f
            }
            drawSlicedPieces(slot.ingredient, slot.currX, slot.currY, slot.currZ, scale)
        }
    }

    private fun drawWholeIngredient(ing: MinigameIngredient, x: Float, y: Float, z: Float) {
        when (ing) {
            MinigameIngredient.BLUEBERRY -> {
                // Cluster of 3 plump 3D blueberries
                for (k in 0 until 3) {
                    val bx = x + pieceOffsetsX[k] * 0.85f
                    val bz = z + pieceOffsetsZ[k] * 0.85f
                    val by = y + if (k == 2) 0.05f else 0.01f
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, bx, by, bz)
                    drawMesh(berryMesh, if (k == 2) blueberryColor else blueberryDarkColor)

                    // Tiny berry crown
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, bx, by + 0.095f, bz)
                    Matrix.scaleM(modelMatrix, 0, 0.04f, 0.02f, 0.04f)
                    drawMesh(boxMesh, blueberryDarkColor)
                }
            }
            MinigameIngredient.MINT -> {
                // 3-leaf fresh 3D Mint sprig
                for (k in 0 until 3) {
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, x, y + k * 0.015f, z)
                    Matrix.rotateM(modelMatrix, 0, mintLeafAngles[k], 0f, 1f, 0f)
                    Matrix.translateM(modelMatrix, 0, 0f, 0f, 0.09f)
                    drawMesh(mintLeafMesh, if (k == 2) mintLeafColor else mintStemColor)
                }
            }
            MinigameIngredient.MANGO -> {
                // Plump 3D Golden-Blush Mango fruit
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x, y + 0.03f, z)
                Matrix.rotateM(modelMatrix, 0, -20f, 0f, 1f, 0f)
                drawMesh(mangoWholeMesh, mangoGoldColor)

                // Blush cap accent
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, x - 0.05f, y + 0.04f, z - 0.02f)
                Matrix.rotateM(modelMatrix, 0, -20f, 0f, 1f, 0f)
                Matrix.scaleM(modelMatrix, 0, 0.75f, 0.96f, 0.92f)
                drawMesh(mangoWholeMesh, mangoBlushColor)
            }
        }
    }

    private fun drawSlicedPieces(ing: MinigameIngredient, x: Float, y: Float, z: Float, bounceScale: Float) {
        for (k in 0 until 3) {
            val px = x + pieceOffsetsX[k] * 1.25f * bounceScale
            val pz = z + pieceOffsetsZ[k] * 1.25f * bounceScale
            val py = y + 0.01f
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, px, py, pz)
            Matrix.rotateM(modelMatrix, 0, pieceRotAngles[k], 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, bounceScale, bounceScale, bounceScale)

            when (ing) {
                MinigameIngredient.BLUEBERRY -> {
                    Matrix.scaleM(modelMatrix, 0, 0.85f, 0.68f, 0.85f)
                    drawMesh(berryMesh, blueberryColor)
                }
                MinigameIngredient.MINT -> {
                    Matrix.scaleM(modelMatrix, 0, 0.72f, 0.80f, 0.72f)
                    drawMesh(mintLeafMesh, mintLeafColor)
                }
                MinigameIngredient.MANGO -> {
                    // Juicy 3D diced Mango cubes
                    Matrix.scaleM(modelMatrix, 0, 0.14f, 0.12f, 0.14f)
                    drawMesh(boxMesh, mangoGoldColor)
                }
            }
        }
    }

    private fun drawChefKnife() {
        val chopProgress = if (knifeChopTimer > 0f) sin((knifeChopTimer / 0.30f) * PI.toFloat()) else 0f
        val ky = 0.36f - chopProgress * 0.22f
        val chopPitch = -12f - chopProgress * 28f

        // Blade
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, knifeX, ky, knifeZ + 0.22f)
        Matrix.rotateM(modelMatrix, 0, -18f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, chopPitch, 1f, 0f, 0f)
        drawMesh(knifeBladeMesh, knifeSilverColor)

        // Pastel Handle
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, knifeX + 0.05f, ky + 0.04f, knifeZ + 0.36f)
        Matrix.rotateM(modelMatrix, 0, -18f, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f + chopPitch, 1f, 0f, 0f)
        drawMesh(cylinderMesh, knifeHandleColor)
    }

    // ========================================================================
    // 3D RAYCAST & TOUCH GESTURE HANDLING (ZERO ALLOCATION)
    // ========================================================================
    fun handleTouchDown(screenX: Float, screenY: Float) {
        touchDownScreenX = screenX
        touchDownScreenY = screenY
        if (!unprojectToPlaneY(screenX, screenY, 0.14f, rayHitXZ)) return
        val wx = rayHitXZ[0]
        val wz = rayHitXZ[1]

        if (wz < 0.15f) {
            knifeX = wx.coerceIn(-1.15f, 1.15f)
            knifeZ = wz.coerceIn(-1.25f, -0.20f)
        }

        for (i in slots.indices) {
            val s = slots[i]
            if (s.phase == SlotPhase.SLICED_ON_BOARD &&
                MinigameEngine.hitTestIngredient(wx, wz, s.currX, s.currZ, 0.38f)
            ) {
                activeDragSlot = i
                s.phase = SlotPhase.DRAGGING
                s.currY = 0.28f
                return
            }
        }
    }

    fun handleTouchMove(screenX: Float, screenY: Float) {
        if (!unprojectToPlaneY(screenX, screenY, 0.14f, rayHitXZ)) return
        val wx = rayHitXZ[0]
        val wz = rayHitXZ[1]

        // Move 3D knife when swiping across the cutting board area
        if (wz < 0.18f) {
            knifeX = wx.coerceIn(-1.15f, 1.15f)
            knifeZ = wz.coerceIn(-1.25f, -0.20f)
        }

        val swipeDist = abs(screenX - touchDownScreenX) + abs(screenY - touchDownScreenY)

        // Check if knife swipe slices any WHOLE ingredient on the pink cutting board
        if (swipeDist > 16f) {
            for (i in slots.indices) {
                val s = slots[i]
                if (s.phase == SlotPhase.WHOLE &&
                    MinigameEngine.hitTestIngredient(wx, wz, s.currX, s.currZ, 0.38f)
                ) {
                    s.phase = SlotPhase.SLICED_ON_BOARD
                    s.slicePopElapsed = 0f
                    knifeChopTimer = 0.30f

                    // Also allow single continuous swipe-slice-and-drag into box!
                    if (activeDragSlot == -1) {
                        activeDragSlot = i
                        s.phase = SlotPhase.DRAGGING
                        s.currY = 0.26f
                    }

                    val ing = s.ingredient
                    mainHandler.post {
                        onKnifeSlice(ing, screenX, screenY)
                    }
                }
            }
        }

        // Update dragged sliced pieces position
        val dragIdx = activeDragSlot
        if (dragIdx in slots.indices) {
            val s = slots[dragIdx]
            if (s.phase == SlotPhase.DRAGGING) {
                s.currX = wx.coerceIn(-1.35f, 1.35f)
                s.currZ = wz.coerceIn(-1.25f, 1.45f)
                s.currY = 0.28f
            }
        }
    }

    fun handleTouchUp(screenX: Float, screenY: Float) {
        val dragIdx = activeDragSlot
        activeDragSlot = -1
        if (dragIdx !in slots.indices) return

        val s = slots[dragIdx]
        if (s.phase != SlotPhase.DRAGGING) return

        val hasHit = unprojectToPlaneY(screenX, screenY, 0.14f, rayHitXZ)
        val wx = if (hasHit) rayHitXZ[0] else s.currX
        val wz = if (hasHit) rayHitXZ[1] else s.currZ

        val hitCompartment = MinigameEngine.hitTestCompartment(wx, wz)
        when {
            hitCompartment == s.ingredient -> {
                // Hit-test matched! Fly along 3D arc & snap with elastic bounce
                s.flightStartX = s.currX
                s.flightStartZ = s.currZ
                s.flightT = 0f
                s.phase = SlotPhase.FLYING_TO_BOX
            }
            hitCompartment != null -> {
                // Wrong compartment! Flash & shake wrong compartment, return pieces to board
                wrongFlashTimers[hitCompartment.index] = 0.45f
                s.currX = s.ingredient.boardX
                s.currZ = s.ingredient.boardZ
                s.currY = 0.14f
                s.phase = SlotPhase.SLICED_ON_BOARD
                s.slicePopElapsed = 0f
                mainHandler.post { onWrongCompartment() }
            }
            else -> {
                // Released above cutting board after slicing: rest neatly as sliced pieces on board
                s.currX = s.ingredient.boardX
                s.currZ = s.ingredient.boardZ
                s.currY = 0.14f
                s.phase = SlotPhase.SLICED_ON_BOARD
            }
        }
    }

    private fun unprojectToPlaneY(screenX: Float, screenY: Float, planeY: Float, outXZ: FloatArray): Boolean {
        val ndcX = (2.0f * screenX / viewportW.toFloat()) - 1.0f
        val ndcY = 1.0f - (2.0f * screenY / viewportH.toFloat())

        nearIn[0] = ndcX; nearIn[1] = ndcY; nearIn[2] = -1.0f; nearIn[3] = 1.0f
        farIn[0] = ndcX; farIn[1] = ndcY; farIn[2] = 1.0f; farIn[3] = 1.0f

        Matrix.multiplyMV(nearOut, 0, invVPMatrix, 0, nearIn, 0)
        Matrix.multiplyMV(farOut, 0, invVPMatrix, 0, farIn, 0)

        if (abs(nearOut[3]) < 0.00001f || abs(farOut[3]) < 0.00001f) return false
        val nx = nearOut[0] / nearOut[3]
        val ny = nearOut[1] / nearOut[3]
        val nz = nearOut[2] / nearOut[3]

        val fx = farOut[0] / farOut[3]
        val fy = farOut[1] / farOut[3]
        val fz = farOut[2] / farOut[3]

        val dy = fy - ny
        if (abs(dy) < 0.0001f) return false
        val t = (planeY - ny) / dy
        if (t < 0f) return false

        outXZ[0] = nx + t * (fx - nx)
        outXZ[1] = nz + t * (fz - nz)
        return true
    }

    private fun projectToScreen(wx: Float, wy: Float, wz: Float): FloatArray {
        projIn[0] = wx; projIn[1] = wy; projIn[2] = wz; projIn[3] = 1.0f
        Matrix.multiplyMV(projOut, 0, vpMatrix, 0, projIn, 0)
        val w = if (abs(projOut[3]) < 0.0001f) 1.0f else projOut[3]
        val ndcX = projOut[0] / w
        val ndcY = projOut[1] / w
        val sx = (ndcX + 1.0f) * 0.5f * viewportW.toFloat()
        val sy = (1.0f - ndcY) * 0.5f * viewportH.toFloat()
        return floatArrayOf(sx, sy)
    }

    private fun drawMesh(mesh: ConeMesh, color: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform4fv(uColorHandle, 1, color, 0)

        val stride = 8 * 4
        mesh.vertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, mesh.vertexBuffer)

        mesh.vertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, mesh.vertexBuffer)

        mesh.indexBuffer.position(0)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }
}

// ============================================================================
// 2. BLENDER 3D PROCEDURAL RENDERER (3 CUPS: YELLOW, PURPLE, GREEN)
// ============================================================================
private class BlenderMinigameRenderer : GLSurfaceView.Renderer {

    @Volatile var fillProgress: Float = 0f

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Pre-allocated colors (Zero GC in render loop)
    private val lightDir = floatArrayOf(0.32f, 0.78f, 0.55f)
    private val counterColor = floatArrayOf(0.92f, 0.29f, 0.46f, 1.0f)
    private val shadowColor = floatArrayOf(0.64f, 0.16f, 0.31f, 0.42f)
    private val silverHeaderColor = floatArrayOf(0.87f, 0.90f, 0.94f, 1.0f)
    private val bladeDarkColor = floatArrayOf(0.35f, 0.38f, 0.44f, 1.0f)
    private val cupGlassRimColor = floatArrayOf(1.0f, 1.0f, 1.0f, 0.34f)

    // 3 Smoothie Colors: Yellow, Purple, Green
    private val cupPositionsX = floatArrayOf(-0.92f, 0.0f, 0.92f)
    private val liquidColors = arrayOf(
        floatArrayOf(1.00f, 0.83f, 0.14f, 1.0f), // Yellow (Mango)
        floatArrayOf(0.61f, 0.32f, 0.88f, 1.0f), // Purple (Blueberry)
        floatArrayOf(0.24f, 0.83f, 0.44f, 1.0f)  // Green (Mint)
    )
    private val foamColors = arrayOf(
        floatArrayOf(1.00f, 0.95f, 0.52f, 1.0f),
        floatArrayOf(0.80f, 0.58f, 0.98f, 1.0f),
        floatArrayOf(0.58f, 0.96f, 0.68f, 1.0f)
    )

    private var programId = 0
    private var uMVPHandle = 0
    private var uColorHandle = 0
    private var uLightDirHandle = 0

    private lateinit var floorMesh: ConeMesh
    private lateinit var shadowMesh: ConeMesh
    private lateinit var outerCupMesh: ConeMesh
    private lateinit var innerLiquidMesh: ConeMesh
    private lateinit var streamCylinderMesh: ConeMesh
    private lateinit var headerBeamMesh: ConeMesh
    private lateinit var boxMesh: ConeMesh

    private var bladeSpinDeg = 0f
    private var lastTimeNs = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(1.0f, 0.50f, 0.66f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        programId = MinigameGlShader.createLitProgram()
        uMVPHandle = GLES30.glGetUniformLocation(programId, "uMVPMatrix")
        uColorHandle = GLES30.glGetUniformLocation(programId, "uColor")
        uLightDirHandle = GLES30.glGetUniformLocation(programId, "uLightDir")

        floorMesh = ShopEnvironmentGenerator.createPlaneMesh(8.0f, 6.0f)
        shadowMesh = SoftServeGenerator.generateShadowPlane(radius = 0.33f, segments = 20)
        outerCupMesh = MinigameProceduralGenerator.generateBottomAnchoredCupMesh(
            bottomRadius = 0.23f,
            topRadius = 0.33f,
            height = 1.05f,
            segments = 24,
            includeCaps = false
        )
        innerLiquidMesh = MinigameProceduralGenerator.generateBottomAnchoredCupMesh(
            bottomRadius = 0.205f,
            topRadius = 0.295f,
            height = 0.92f,
            segments = 24,
            includeCaps = true
        )
        streamCylinderMesh = ConeGenerator.generateCylinder(radius = 0.042f, height = 1.0f, segments = 12)
        headerBeamMesh = ConeGenerator.generateCylinder(radius = 0.14f, height = 2.45f, segments = 16)
        boxMesh = CustomerGenerator.createBoxMesh(1f, 1f, 1f)

        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 0.72f, 3.65f,
            0.0f, 0.05f, 0.0f,
            0.0f, 1.0f, 0.0f
        )

        lastTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        GLES30.glViewport(0, 0, w, h)
        val aspect = w.toFloat() / h.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 46.0f, aspect, 0.1f, 30.0f)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTimeNs = now

        bladeSpinDeg = (bladeSpinDeg + 780f * dt) % 360f
        val fill = fillProgress.coerceIn(0f, 1f)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        GLES30.glUseProgram(programId)
        GLES30.glUniform3fv(uLightDirHandle, 1, lightDir, 0)

        // 1. Darker pink countertop floor
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, -0.66f, 0f)
        Matrix.rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f)
        drawMesh(floorMesh, counterColor)

        // 2. Silver Blender Top Bar & Spouts
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0f, 1.02f, 0f)
        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
        drawMesh(headerBeamMesh, silverHeaderColor)

        val cupBaseY = -0.64f

        // 3. Opaque elements per cup: shadow, spinning blender blade, pouring stream, rising liquid
        for (i in 0 until 3) {
            val cx = cupPositionsX[i]

            // Shadow disc
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, -0.65f, 0f)
            drawMesh(shadowMesh, shadowColor)

            // Spout nozzle above each cup
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, 0.88f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.16f, 0.18f, 0.16f)
            drawMesh(boxMesh, silverHeaderColor)

            // Spinning blender agitator blade near bottom of cup
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cx, cupBaseY + 0.06f, 0f)
            Matrix.rotateM(modelMatrix, 0, bladeSpinDeg + i * 60f, 0f, 1f, 0f)
            Matrix.scaleM(modelMatrix, 0, 0.26f, 0.025f, 0.045f)
            drawMesh(boxMesh, bladeDarkColor)

            // Pouring smoothie stream while filling (0.01 < fill < 0.99)
            if (fill > 0.01f && fill < 0.99f) {
                val liquidSurfaceY = cupBaseY + 0.03f + fill * 0.92f
                val spoutY = 0.84f
                val streamLen = (spoutY - liquidSurfaceY).coerceAtLeast(0.05f)
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, liquidSurfaceY + streamLen * 0.5f, 0f)
                Matrix.scaleM(modelMatrix, 0, 1.0f, streamLen, 1.0f)
                drawMesh(streamCylinderMesh, liquidColors[i])
            }

            // Rising 3D liquid inside cup (Yellow, Purple, Green)
            if (fill > 0.01f) {
                val clampedFill = fill.coerceIn(0.03f, 1.0f)
                val horizScale = 0.86f + 0.14f * clampedFill
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, cupBaseY + 0.02f, 0f)
                Matrix.scaleM(modelMatrix, 0, horizScale, clampedFill, horizScale)
                drawMesh(innerLiquidMesh, liquidColors[i])

                // Top frothy foam layer
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, cx, cupBaseY + 0.02f + clampedFill * 0.91f, 0f)
                Matrix.scaleM(modelMatrix, 0, horizScale * 0.99f, 0.045f, horizScale * 0.99f)
                drawMesh(innerLiquidMesh, foamColors[i])
            }
        }

        // 4. Translucent Glass Cups drawn last with depth-mask disabled for clean transparency
        GLES30.glDepthMask(false)
        for (i in 0 until 3) {
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, cupPositionsX[i], cupBaseY, 0f)
            drawMesh(outerCupMesh, cupGlassRimColor)
        }
        GLES30.glDepthMask(true)
    }

    private fun drawMesh(mesh: ConeMesh, color: FloatArray) {
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES30.glUniformMatrix4fv(uMVPHandle, 1, false, mvpMatrix, 0)
        GLES30.glUniform4fv(uColorHandle, 1, color, 0)

        val stride = 8 * 4
        mesh.vertexBuffer.position(0)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, mesh.vertexBuffer)

        mesh.vertexBuffer.position(3)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, mesh.vertexBuffer)

        mesh.indexBuffer.position(0)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, mesh.indexCount, GLES30.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }
}

private object MinigameGlShader {
    private const val VERTEX_SHADER = """#version 300 es
        layout(location = 0) in vec4 aPosition;
        layout(location = 1) in vec3 aNormal;
        uniform mat4 uMVPMatrix;
        out vec3 vNormal;
        void main() {
            vNormal = aNormal;
            gl_Position = uMVPMatrix * aPosition;
        }
    """

    private const val FRAGMENT_SHADER = """#version 300 es
        precision mediump float;
        in vec3 vNormal;
        uniform vec4 uColor;
        uniform vec3 uLightDir;
        out vec4 fragColor;
        void main() {
            vec3 norm = normalize(vNormal);
            vec3 light = normalize(uLightDir);
            float diff = max(dot(norm, light), 0.0);
            float rim = pow(1.0 - max(dot(norm, vec3(0.0, 0.4, 0.9)), 0.0), 2.5) * 0.14;
            vec3 lit = uColor.rgb * (0.56 + 0.44 * diff) + vec3(rim);
            fragColor = vec4(min(lit, vec3(1.0)), uColor.a);
        }
    """

    fun createLitProgram(): Int {
        val vs = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER).also {
            GLES30.glShaderSource(it, VERTEX_SHADER)
            GLES30.glCompileShader(it)
        }
        val fs = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER).also {
            GLES30.glShaderSource(it, FRAGMENT_SHADER)
            GLES30.glCompileShader(it)
        }
        return GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vs)
            GLES30.glAttachShader(it, fs)
            GLES30.glLinkProgram(it)
        }
    }
}
