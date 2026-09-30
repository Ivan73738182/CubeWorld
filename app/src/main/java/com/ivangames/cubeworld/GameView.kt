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

    // Камера
    private var camX = 0f
    private var camY = 120f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    // Персонаж
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 0f
    private val eyeHeight = 120f

    // Джойстик
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private var joyRadius = 200f
    private var joyKnobX = 0f
    private var joyKnobY = 0f
    private var joyActive = false
    private var joyTouchId = -1
    private var joyDeltaX = 0f
    private var joyDeltaY = 0f

    // Поворот
    private var rotateActive = false
    private var rotateTouchId = -1
    private var lastRotateX = 0f
    private var lastRotateY = 0f

    // Мир
    private val cubes = mutableListOf<Cube>()
    class Cube(val x: Float, val y: Float, val z: Float, val size: Float, val color: Int)

    // Краски
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
    private val crosshairPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val crosshairDotPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        setupWorld()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()

        joyRadius = minOf(screenW, screenH) * 0.14f
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
            val dist = 500f + (i % 5) * 250f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val size = 100f + (i % 3) * 50f
            val y = size / 2f   // центр куба — на высоте половины размера
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

    // ============ РИСОВАНИЕ КУБА ============

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

        val faces = mutableListOf<Array<Any>>()

        // Передняя (0,1,5,4)
        faces.add(arrayOf(projected[0]!!, projected[1]!!, projected[5]!!, projected[4]!!,
            (projected[0]!![2] + projected[1]!![2] + projected[5]!![2] + projected[4]!![2]) / 4f))
        // Правая (1,2,6,5)
        faces.add(arrayOf(projected[1]!!, projected[2]!!, projected[6]!!, projected[5]!!,
            (projected[1]!![2] + projected[2]!![2] + projected[6]!![2] + projected[5]!![2]) / 4f))
        // Задняя (3,2,6,7)
        faces.add(arrayOf(projected[3]!!, projected[2]!!, projected[6]!!, projected[7]!!,
            (projected[3]!![2] + projected[2]!![2] + projected[6]!![2] + projected[7]!![2]) / 4f))
        // Левая (0,3,7,4)
        faces.add(arrayOf(projected[0]!!, projected[3]!!, projected[7]!!, projected[4]!!,
            (projected[0]!![2] + projected[3]!![2] + projected[7]!![2] + projected[4]!![2]) / 4f))
        // Верхняя (4,5,6,7)
        faces.add(arrayOf(projected[4]!!, projected[5]!!, projected[6]!!, projected[7]!!,
            (projected[4]!![2] + projected[5]!![2] + projected[6]!![2] + projected[7]!![2]) / 4f))
        // Нижняя (0,1,2,3)
        faces.add(arrayOf(projected[0]!!, projected[1]!!, projected[2]!!, projected[3]!!,
            (projected[0]!![2] + projected[1]!![2] + projected[2]!![2] + projected[3]!![2]) / 4f))

        // Сортируем по глубине — дальние раньше
        val sorted = faces.sortedByDescending { it[4] as Float }

        val paint = Paint().apply {
            color = cube.color
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        for (face in sorted) {
            drawFace(canvas,
                face[0] as FloatArray,
                face[1] as FloatArray,
                face[2] as FloatArray,
                face[3] as FloatArray,
                paint)
        }
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

    // ============ РИСОВАНИЕ СЦЕНЫ ============

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Небо
        canvas.drawColor(Color.parseColor("#87CEEB"))

        updateCamera()

        // Земля (вокруг игрока)
        drawGround(canvas)

        // Сортируем кубы — дальние раньше
        val sortedCubes = cubes
            .map { cube ->
                val dx = cube.x - camX
                val dz = cube.z - camZ
                cube to (dx * dx + dz * dz)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        for (cube in sortedCubes) {
            drawCube(canvas, cube)
        }

        // Джойстик
        drawJoystick(canvas)

        // Точка прицела
        drawCrosshair(canvas)

        update()
        invalidate()
    }

    // Земля — вокруг игрока (не зависит от позиции, чтобы не было дыр)
    private fun drawGround(canvas: Canvas) {
        val gridSize = 500f
        val range = 6  // 13 x 13 клеток

        val groundPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#4A9E4A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Центр сетки — всегда "под игроком" (но в целых клетках)
        val centerX = (playerX / gridSize).toInt() * gridSize
        val centerZ = (playerZ / gridSize).toInt() * gridSize

        for (ix in -range..range) {
            for (iz in -range..range) {
                val x = centerX + ix * gridSize
                val z = centerZ + iz * gridSize
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

    private fun drawCrosshair(canvas: Canvas) {
        val cx = screenW / 2f
        val cy = screenH / 2f
        canvas.drawCircle(cx, cy, 20f, crosshairPaint)
        canvas.drawCircle(cx, cy, 4f, crosshairDotPaint)
    }

    // ============ ФИЗИКА ============

    private fun update() {
        if (joyActive) {
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            // Вверх джойстика = вперёд
            val moveX = joyDeltaX
            val moveZ = -joyDeltaY

            // Преобразуем в мировые координаты (относительно камеры)
            val worldX = moveX * cosYaw + moveZ * sinYaw
            val worldZ = -moveX * sinYaw + moveZ * cosYaw

            val speed = 10f
            playerX += worldX * speed
            playerZ += worldZ * speed
        }
    }

    // ============ МУЛЬТИТАЧ ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                if (x < screenW / 2f) {
                    joyActive = true
                    joyTouchId = event.getPointerId(0)
                    updateJoystick(x, y)
                } else {
                    rotateActive = true
                    rotateTouchId = event.getPointerId(0)
                    lastRotateX = x
                    lastRotateY = y
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                val id = event.getPointerId(index)

                if (x < screenW / 2f && !joyActive) {
                    joyActive = true
                    joyTouchId = id
                    updateJoystick(x, y)
                } else if (x >= screenW / 2f && !rotateActive) {
                    rotateActive = true
                    rotateTouchId = id
                    lastRotateX = x
                    lastRotateY = y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (id == joyTouchId && joyActive) {
                        updateJoystick(x, y)
                    } else if (id == rotateTouchId && rotateActive) {
                        val dx = x - lastRotateX
                        val dy = y - lastRotateY
                        camYaw += dx * 0.3f
                        camPitch -= dy * 0.3f
                        camPitch = camPitch.coerceIn(-60f, 60f)
                        lastRotateX = x
                        lastRotateY = y
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == joyTouchId) {
                    joyActive = false
                    joyTouchId = -1
                    joyDeltaX = 0f
                    joyDeltaY = 0f
                    joyKnobX = joyCenterX
                    joyKnobY = joyCenterY
                }
                if (id == rotateTouchId) {
                    rotateActive = false
                    rotateTouchId = -1
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                joyActive = false
                rotateActive = false
                joyTouchId = -1
                rotateTouchId = -1
                joyDeltaX = 0f
                joyDeltaY = 0f
                joyKnobX = joyCenterX
                joyKnobY = joyCenterY
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
