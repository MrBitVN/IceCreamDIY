package com.icecream.diy.scene3d

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.icecream.diy.scene3d.camera.SceneLighting
import com.icecream.diy.scene3d.procedural.ConeGenerator
import com.icecream.diy.scene3d.procedural.ConeMesh
import com.icecream.diy.scene3d.procedural.CustomerGenerator
import com.icecream.diy.scene3d.procedural.SoftServeGenerator
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

@Composable
fun HomeCustomerSceneView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val glView = remember { HomeCustomerGLSurfaceView(context) }

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

class HomeCustomerGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer = HomeCustomerRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(true)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}

class HomeCustomerRenderer : GLSurfaceView.Renderer {
    private var programId = 0
    private var uMVPMatrixHandle = 0
    private var uColorHandle = 0
    private var uLightPosHandle = 0
    private var aPositionHandle = 0
    private var aNormalHandle = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private lateinit var boxMesh: ConeMesh
    private lateinit var shadowMesh: ConeMesh

    private var startTime = System.currentTimeMillis()
    private val sunglassesColor = floatArrayOf(0.12f, 0.12f, 0.14f, 1.0f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initShader()

        boxMesh = CustomerGenerator.createBoxMesh(1f, 1f, 1f)
        shadowMesh = SoftServeGenerator.generateShadowPlane(radius = 0.55f, segments = 24)
        startTime = System.currentTimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, 42.0f, aspect, 0.1f, 50.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val elapsed = (System.currentTimeMillis() - startTime) / 1000f
        val waveAngle = sin(elapsed * 4.0f) * 18.0f // Hand waving animation

        // Camera
        Matrix.setLookAtM(
            viewMatrix, 0,
            0.0f, 0.25f, 3.2f,
            0.0f, 0.10f, 0.0f,
            0.0f, 1.0f, 0.0f
        )

        // 1. Soft Shadow
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.0f, -0.65f, 0.0f)
        drawMesh(shadowMesh, SceneLighting.shadowColor, 8)

        // 2. Character Variant (Cool Girl with Mint Top)
        val variant = CustomerGenerator.VARIANTS[0]
        val cx = 0.0f
        val cy = 0.0f
        val cz = 0.0f

        // Torso
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy, cz)
        Matrix.scaleM(modelMatrix, 0, 0.58f, 0.65f, 0.28f)
        drawMesh(boxMesh, variant.shirtColor, 6)

        // Neck
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.38f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.16f, 0.16f, 0.16f)
        drawMesh(boxMesh, variant.skinColor, 6)

        // Head
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.62f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.36f, 0.40f, 0.32f)
        drawMesh(boxMesh, variant.skinColor, 6)

        // Hair Top
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.78f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.39f, 0.14f, 0.35f)
        drawMesh(boxMesh, variant.hairColor, 6)

        // Long hair strands
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx - 0.21f, cy + 0.52f, cz + 0.02f)
        Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
        drawMesh(boxMesh, variant.hairColor, 6)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx + 0.21f, cy + 0.52f, cz + 0.02f)
        Matrix.scaleM(modelMatrix, 0, 0.08f, 0.48f, 0.24f)
        drawMesh(boxMesh, variant.hairColor, 6)

        // Sunglasses
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx, cy + 0.64f, cz + 0.18f)
        Matrix.scaleM(modelMatrix, 0, 0.34f, 0.10f, 0.04f)
        drawMesh(boxMesh, sunglassesColor, 6)

        // Left Arm (resting)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx - 0.34f, cy - 0.10f, cz)
        Matrix.scaleM(modelMatrix, 0, 0.12f, 0.38f, 0.12f)
        drawMesh(boxMesh, variant.skinColor, 6)

        // Right Arm (WAVING hand!)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, cx + 0.34f, cy + 0.15f, cz + 0.05f)
        Matrix.rotateM(modelMatrix, 0, 65f + waveAngle, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, 20f, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.12f, 0.38f, 0.12f)
        drawMesh(boxMesh, variant.skinColor, 6)
    }

    private fun drawMesh(mesh: ConeMesh, color: FloatArray, strideFloats: Int) {
        GLES20.glUseProgram(programId)
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(uColorHandle, 1, color, 0)
        GLES20.glUniform3fv(uLightPosHandle, 1, SceneLighting.keyLightDirection, 0)

        mesh.vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false, strideFloats * 4, mesh.vertexBuffer)

        mesh.vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormalHandle)
        GLES20.glVertexAttribPointer(aNormalHandle, 3, GLES20.GL_FLOAT, false, strideFloats * 4, mesh.vertexBuffer)

        mesh.indexBuffer.position(0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)

        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aNormalHandle)
    }

    private fun initShader() {
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
                float diffuse = 0.40 + 0.60 * diff;
                gl_FragColor = vec4(uColor.rgb * diffuse, uColor.a);
            }
        """.trimIndent()

        val vShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER).apply {
            GLES20.glShaderSource(this, vShaderCode)
            GLES20.glCompileShader(this)
        }
        val fShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER).apply {
            GLES20.glShaderSource(this, fShaderCode)
            GLES20.glCompileShader(this)
        }
        programId = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vShader)
            GLES20.glAttachShader(this, fShader)
            GLES20.glLinkProgram(this)
        }

        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uColorHandle = GLES20.glGetUniformLocation(programId, "uColor")
        uLightPosHandle = GLES20.glGetUniformLocation(programId, "uLightPos")
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(programId, "aNormal")
    }
}
