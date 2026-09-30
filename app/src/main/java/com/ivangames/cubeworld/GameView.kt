package com.ivangames.cubeworld

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var screenW = 0f
    private var screenH = 0f

    // ===== Камера (от первого лица) =====
    private var camX = 0f
    private var camY = 60f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    // ===== Персонаж =====
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 0f
    private val eyeHeight = 60f

    // ===== Джойстик =====
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private var joyRadius = 200f
    private var joyKnobX = 0f
    private var joyKnobY = 0f
    private var joyActive = false
    private var joyDeltaX = 0f
    private var joyDeltaY = 0f

    // ===== Свайп камеры =====
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isRotating = false

    // ===== Мир =====
    private val cubes = mutableListOf<Cube>()

    class Cube(val x: Float, val y: Float, val z: Float, val size: Float, val color: Int)

    // ===== Краски =====
    private val joyBasePaint = Paint().apply {
        color = Color.parseColor("#66FFFFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val joyKnobPaint = Paint().apply {
        color = Color.parseColor("#CCFFFFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val joyBorderPaint = Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    init {
        setupWorld()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()

        joyRadius = minOf(screenW, screenH) * 0.16f
        joyCenterX = joyRadius + 80f
        joyCenterY = screenH - joyRadius - 80f
        joyKnobX = joyCenterX
        joyKnobY = joyCenterY
    }

    private fun setupWorld() {
        cubes.clear()

        val colors = intArrayOf(
            Color.parseColor("#E94560"),
            Color.parseColor("#F39C12"),
            Color.parseColor("#9B59B6"),
            Color.parseColor("#3498DB"),
            Color.parseColor("#1ABC9C"),
            Color.parseColor("#E74C3C"),
            Color.parseColor("#F1C40F"),
            Color.parseColor("#2ECC71")
        )

        // Кубы вокруг игрока
        for (i in 0 until 30) {
            val angle = i * (360f / 30f) * Math.PI.toFloat() / 180f
            val dist = 300f + (i % 5) * 180f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val y = 40f + (i % 3) * 30f
            val size = 80f + (i % 3) * 40f
            val color = colors[i % colors.size]
            cubes.add(Cube(x, y, z, size, color))
        }
    }
// ============ 3D-ПРОЕКЦИЯ ============

private fun project(x: Float, y: Float, z: Float): FloatArray? {
    val dx = x - camX
    val dy = y - camY
    val dz = z - camZ

    val yawRad = Math.toRadians(camYaw.toDouble())
    val cosYaw = cos(yawRad).toFloat()
    val sinYaw = sin(yawRad).toFloat()

    val rx = dx * cosYaw - dz * sinYaw
    val rz = dx * sinYaw + dz * cosYaw

    val pitchRad = Math.toRadians(camPitch.toDouble())
    val cosPitch = cos(pitchRad).toFloat()
    val sinPitch = sin(pitchRad).toFloat()

    val ry = dy * cosPitch - rz * sinPitch
    val rz2 = dy * sinPitch + rz * cosPitch

    if (rz2 <= 1f) return null

    val fov = 500f
    val screenX = screenW / 2f + (rx * fov / rz2)
    val screenY = screenH / 2f - (ry * fov / rz2)

    return floatArrayOf(screenX, screenY, rz2)
}

private fun drawCube(canvas: Canvas, cube: Cube) {
    val s = cube.size / 2f

    val vertices = arrayOf(
        floatArrayOf(cube.x - s, cube.y - s, cube.z - s),
        floatArrayOf(cube.x + s, cube.y - s, cube.z - s),
        floatArrayOf(cube.x + s, cube.y - s, cube.z + s),
        floatArrayOf(cube.x - s, cube.y - s, cube.z + s),
        floatArrayOf(cube.x - s, cube.y + s, cube.z - s),
        floatArrayOf(cube.x + s, cube.y + s, cube.z - s),
        floatArrayOf(cube.x + s, cube.y + s, cube.z + s),
        floatArrayOf(cube.x - s, cube.y + s, cube.z + s)
    )

    val projected = arrayOfNulls<FloatArray>(8)
    for (i in 0 until 8) {
        projected[i] = project(vertices[i][0], vertices[i][1], vertices[i][2])
    }

    if (projected.any { it == null }) return

    val paint = Paint().apply {
        color = cube.color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Верхняя грань
    drawFace(canvas, projected[4]!!, projected[5]!!, projected[6]!!, projected[7]!!, paint)
    // Передняя
    drawFace(canvas, projected[0]!!, projected[1]!!, projected[5]!!, projected[4]!!, paint)
    // Задняя
    drawFace(canvas, projected[3]!!, projected[2]!!, projected[6]!!, projected[7]!!, paint)
    // Левая
    drawFace(canvas, projected[0]!!, projected[3]!!, projected[7]!!, projected[4]!!, paint)
    // Правая
    drawFace(canvas, projected[1]!!, projected[2]!!, projected[6]!!, projected[5]!!, paint)
    // Нижняя
    drawFace(canvas, projected[0]!!, projected[1]!!, projected[2]!!, projected[3]!!, paint)
}

private fun drawFace(canvas: Canvas, p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, paint: Paint) {
    val path = Path()
    path.moveTo(p1[0], p1[1])
    path.lineTo(p2[0], p2[1])
    path.lineTo(p3[0], p3[1])
    path.lineTo(p4[0], p4[1])
    path.close()
    canvas.drawPath(path, paint)
}

// ============ КАМЕРА ============

private fun updateCamera() {
    camX = playerX
    camY = playerY + eyeHeight
    camZ = playerZ
}

// ============ РИСОВАНИЕ ============

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    canvas.drawColor(Color.parseColor("#87CEEB"))

    updateCamera()

    // Сортируем кубы — дальние раньше
    val sortedCubes = cubes.sortedByDescending { cube ->
        val dx = cube.x - camX
        val dz = cube.z - camZ
        dx * dx + dz * dz
    }

    drawGround(canvas)

    for (cube in sortedCubes) {
        drawCube(canvas, cube)
    }

    drawJoystick(canvas)

    update()
    invalidate()
}

private fun drawGround(canvas: Canvas) {
    val gridSize = 400f
    val range = 3

    val groundPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    val borderPaint = Paint().apply {
        color = Color.parseColor("#4A9E4A")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    for (ix in -range..range) {
        for (iz in -range..range) {
            val x = ix * gridSize
            val z = iz * gridSize
            val half = gridSize / 2f

            val p1 = project(x - half, 0f, z - half) ?: continue
            val p2 = project(x + half, 0f, z - half) ?: continue
            val p3 = project(x + half, 0f, z + half) ?: continue
            val p4 = project(x - half, 0f, z + half) ?: continue

            val isDark = (ix + iz) % 2 == 0
            groundPaint.color = if (isDark) Color.parseColor("#5DAE5D") else Color.parseColor("#6DBE6D")

            val path = Path()
            path.moveTo(p1[0], p1[1])
            path.lineTo(p2[0], p2[1])
            path.lineTo(p3[0], p3[1])
            path.lineTo(p4[0], p4[1])
            path.close()
            canvas.drawPath(path, groundPaint)
            canvas.drawPath(path, borderPaint)
        }
    }
}

private fun drawJoystick(canvas: Canvas) {
    canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBasePaint)
    canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBorderPaint)
    canvas.drawCircle(joyKnobX, joyKnobY, joyRadius * 0.4f, joyKnobPaint)
}
    // ============ ФИЗИКА ============

    private fun update() {
        if (joyActive) {
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            // ВАЖНО: инвертируем Y (вверх = вперёд)
            val moveX = joyDeltaX
            val moveZ = -joyDeltaY

            val worldX = moveX * cosYaw - moveZ * sinYaw
            val worldZ = moveX * sinYaw + moveZ * cosYaw

            val speed = 8f
            playerX += worldX * speed
            playerZ += worldZ * speed
        }
    }

    // ============ ТАП И ЖЕСТЫ ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dx = x - joyCenterX
                val dy = y - joyCenterY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= joyRadius * 1.5f) {
                    joyActive = true
                    updateJoystick(x, y)
                } else {
                    isRotating = true
                    lastTouchX = x
                    lastTouchY = y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (joyActive) {
                    updateJoystick(x, y)
                } else if (isRotating) {
                    val dx = x - lastTouchX
                    val dy = y - lastTouchY

                    camYaw += dx * 0.3f
                    camPitch -= dy * 0.3f
                    camPitch = camPitch.coerceIn(-60f, 60f)

                    lastTouchX = x
                    lastTouchY = y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (joyActive) {
                    joyActive = false
                    joyDeltaX = 0f
                    joyDeltaY = 0f
                    joyKnobX = joyCenterX
                    joyKnobY = joyCenterY
                }
                if (isRotating) {
                    isRotating = false
                }
            }
        }
        return true
    }

    private fun updateJoystick(touchX: Float, touchY: Float) {
        var dx = touchX - joyCenterX
        var dy = touchY - joyCenterY

        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist > joyRadius) {
            val k = joyRadius / dist
            dx *= k
            dy *= k
        }

        joyKnobX = joyCenterX + dx
        joyKnobY = joyCenterY + dy

        joyDeltaX = dx / joyRadius
        joyDeltaY = dy / joyRadius
    }
}
