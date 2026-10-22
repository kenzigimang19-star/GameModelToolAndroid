package com.example.gameassettool

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.opengl.GLSurfaceView
import android.opengl.GLES20
import android.opengl.Matrix
import android.graphics.Color
import android.widget.Toast
import com.example.gameassettool.formats.FormatRegistry
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import android.opengl.GLSurfaceView.Renderer
import kotlin.math.min

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var viewer: ModelSurface
    private var loadedName = "Belum ada model"
    private val pickModel = 101
    private val formatRegistry = FormatRegistry()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(18, 22, 30))
        }
        val title = TextView(this).apply {
            text = "GAME ASSET TOOL  •  v0.1"
            textSize = 22f; setTextColor(Color.WHITE)
        }
        status = TextView(this).apply {
            text = "Pilih file OBJ untuk melihat model 3D."
            textSize = 14f; setTextColor(Color.LTGRAY); setPadding(0, 12, 0, 12)
        }
        val open = Button(this).apply {
            text = "Buka model 3D (OBJ)"
            setOnClickListener {
                val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                startActivityForResult(i, pickModel)
            }
        }
        val note = TextView(this).apply {
            text = "MVP: pembuka OBJ dan penampil mesh. YFT/YDR/YDD/YTD serta ekspor DFF/TXD adalah tahap berikutnya; parser format game belum aktif."
            textSize = 13f; setTextColor(Color.rgb(180, 190, 205)); setPadding(0, 8, 0, 12)
        }
        viewer = ModelSurface(this)
        root.addView(title)
        root.addView(status)
        root.addView(open)
        root.addView(note)
        root.addView(viewer, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    @Deprecated("Deprecated in Android API, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickModel && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.openInputStream(uri).use { stream ->
                    if (stream == null) throw IllegalStateException("File tidak dapat dibuka")
                    val text = BufferedReader(InputStreamReader(stream)).readText()
                    val mesh = ObjParser.parse(text)
                    viewer.setMesh(mesh)
                    loadedName = uri.lastPathSegment ?: "model.obj"
                    status.text = "$loadedName\n${mesh.size / 3} segitiga ditampilkan"
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membaca OBJ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

class ModelSurface(context: android.content.Context) : GLSurfaceView(context) {
    private val modelRenderer = ModelRenderer()
    init {
        setEGLContextClientVersion(2)
        setRenderer(modelRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    fun setMesh(mesh: FloatArray) { modelRenderer.setMesh(mesh) }
}

class ModelRenderer : Renderer {
    @Volatile private var mesh = floatArrayOf(
        -0.6f,-0.5f,0f, 0.6f,-0.5f,0f, 0f,0.6f,0f
    )
    private var program = 0
    private var position = 0
    private var mvpHandle = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val mvp = FloatArray(16)
    private var buffer: FloatBuffer? = null
    private var count = 3

    fun setMesh(data: FloatArray) {
        mesh = data
        val bb = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        buffer = bb.asFloatBuffer().apply { put(data); position(0) }
        count = data.size / 3
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.07f, 0.09f, 0.13f, 1f)
        val vs = "attribute vec3 aPos; uniform mat4 uMVP; void main(){ gl_Position=uMVP*vec4(aPos,1.0); gl_PointSize=3.0; }"
        val fs = "precision mediump float; void main(){ gl_FragColor=vec4(0.32,0.78,0.95,1.0); }"
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, shader(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(program, shader(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(program)
        position = GLES20.glGetAttribLocation(program, "aPos")
        mvpHandle = GLES20.glGetUniformLocation(program, "uMVP")
        setMesh(mesh)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / maxOf(height, 1)
        Matrix.frustumM(projection, 0, -ratio, ratio, -1f, 1f, 2f, 20f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setLookAtM(view, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, projection, 0, view, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0)
        val local = buffer ?: return
        local.position(0)
        GLES20.glEnableVertexAttribArray(position)
        GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 12, local)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
        GLES20.glDisableVertexAttribArray(position)
    }

    private fun shader(type: Int, source: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, source)
        GLES20.glCompileShader(s)
        return s
    }
}
