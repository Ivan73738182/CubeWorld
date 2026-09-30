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
    private var camY = 150f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    // Персонаж
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 0f
    private var velocityY = 0f
    private var onGround = true
    private val eyeHeight = 150f

    // Физика
    private val gravity = 1.5f
    private val jumpPower = 20f

    // Размеры блоков
    private val groundBlockSize = 100f
    private val cubeSize = 100f

    // Джойстик (слева)
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private var joyRadius = 200f
    private var joyKnobX = 0f
    private var joyKnobY = 0f
    private var joyActive = false
    private var joyTouchId = -1
    private var joyDeltaX = 0f
    private var joyDeltaY = 0f

    // Кнопка прыжка (справа снизу)
    private var jumpBtnX = 0f
    private var jumpBtnY = 0f
    private var jumpBtnRadius = 100f
    private var jumpBtnPressed = false
    private var jumpTouchId = -1

    // Поворот (правая половина)
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
    private val jumpBtnPaint = Paint().apply {
        color = Color.parseColor("#66FFC107")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val jumpBtnBorderPaint = Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val jumpBtnTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        isFakeBoldText = true
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

        // Джойстик — слева снизу
        joyRadius = minOf(screenW, screenH) * 0.14f
        joyCenterX = joyRadius + 80f
        joyCenterY = screenH - joyRadius - 80f
        joyKnobX = joyCenterX
        joyKnobY = joyCenterY

        // Кнопка прыжка — справа снизу
        jumpBtnRadius = minOf(screenW, screenH) * 0.09f
        jumpBtnX = screenW - jumpBtnRadius - 100f
        jumpBtnY = screenH - jumpBtnRadius - 100f
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

        for (i in 0 until 40) {
            val angle = i * (360f / 40f) * Math.PI.toFloat() / 180f
            val dist = 300f + (i % 6) * 200f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val y = cubeSize / 2f
            val color = colors[i % colors.size]
            cubes.add(Cube(x, y, z, cubeSize, color))
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

    private fun drawBox(canvas: Canvas,
                        x: Float, y: Float, z: Float,
                        w: Float, h: Float, d: Float,
                        color: Int) {

        val hw = w / 2f
        val hh = h / 2f
        val hd = d / 2f

        val vertices = arrayOf(
            floatArrayOf(x - hw, y - hh, z - hd),
            floatArrayOf(x + hw, y - hh, z - hd),
            floatArrayOf(x + hw, y - hh, z + hd),
            floatArrayOf(x - hw, y - hh, z + hd),
            floatArrayOf(x - hw, y + hh, z - hd),
            floatArrayOf(x + hw, y + hh, z - hd),
            floatArrayOf(x + hw, y + hh, z + hd),
            floatArrayOf(x - hw, y + hh, z + hd)
        )

        val projected = arrayOfNulls<FloatArray>(8)
        for (i in 0 until 8) {
            projected[i] = project(vertices[i][0], vertices[i][1], vertices[i][2])
        }

        if (projected.all { it == null }) return

        val faces = mutableListOf<Array<Any>>()

        if (projected[0] != null && projected[1] != null && projected[5] != null && projected[4] != null) {
            faces.add(arrayOf(projected[0]!!, projected[1]!!, projected[5]!!, projected[4]!!,
                (projected[0]!![2] + projected[1]!![2] + projected[5]!![2] + projected[4]!![2]) / 4f))
        }
        if (projected[1] != null && projected[2] != null && projected[6] != null && projected[5] != null) {
            faces.add(arrayOf(projected[1]!!, projected[2]!!, projected[6]!!, projected[5]!!,
                (projected[1]!![2] + projected[2]!![2] + projected[6]!![2] + projected[5]!![2]) / 4f))
        }
        if (projected[3] != null && projected[2] != null && projected[6] != null && projected[7] != null) {
            faces.add(arrayOf(projected[3]!!, projected[2]!!, projected[6]!!, projected[7]!!,
                (projected[3]!![2] + projected[2]!![2] + projected[6]!![2] + projected[7]!![2]) / 4f))
        }
        if (projected[0] != null && projected[3] != null && projected[7] != null && projected[4] != null) {
            faces.add(arrayOf(projected[0]!!, projected[3]!!, projected[7]!!, projected[4]!!,
                (projected[0]!![2] + projected[3]!![2] + projected[7]!![2] + projected[4]!![2]) / 4f))
        }
        if (projected[4] != null && projected[5] != null && projected[6] != null && projected[7] != null) {
            faces.add(arrayOf(projected[4]!!, projected[5]!!, projected[6]!!, projected[7]!!,
                (projected[4]!![2] + projected[5]!![2] + projected[6]!![2] + projected[7]!![2]) / 4f))
        }
        if (projected[0] != null && projected[1] != null && projected[2] != null && projected[3] != null) {
            faces.add(arrayOf(projected[0]!!, projected[1]!!, projected[2]!!, projected[3]!!,
                (projected[0]!![2] + projected[1]!![2] + projected[2]!![2] + projected[3]!![2]) / 4f))
        }

        val sorted = faces.sortedByDescending { it[4] as Float }

        val paint = Paint().apply {
            this.color = color
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

        // Земля
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
            drawBox(canvas, cube.x, cube.y, cube.z, cube.size, cube.size, cube.size, cube.color)
        }

        // Джойстик
        drawJoystick(canvas)

        // Кнопка прыжка
        drawJumpButton(canvas)

        // Прицел
        drawCrosshair(canvas)

        update()
        invalidate()
    }

    // Земля — маленькие блоки 100x100, отсортированные
    private fun drawGround(canvas: Canvas) {
        val range = 12

        val groundPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val centerX = (playerX / groundBlockSize).toInt() * groundBlockSize
        val centerZ = (playerZ / groundBlockSize).toInt() * groundBlockSize

        // Список блоков с глубиной
        data class GBlock(val x: Float, val z: Float, val isDark: Boolean, val depth: Float)
        val blocks = mutableListOf<GBlock>()

        for (ix in -range..range) {
            for (iz in -range..range) {
                val x = centerX + ix * groundBlockSize
                val z = centerZ + iz * groundBlockSize
                val dx = x - camX
                val dz = z - camZ
                val depth = dx * dx + dz * dz
                val isDark = (ix + iz) % 2 == 0
                blocks.add(GBlock(x, z, isDark, depth))
            }
        }

        // Сортируем — дальние раньше
        val sorted = blocks.sortedByDescending { it.depth }

        for (block in sorted) {
            val x = block.x
            val z = block.z
            val half = groundBlockSize / 2f

            val p1 = project(x - half, 0f, z - half) ?: continue
            val p2 = project(x + half, 0f, z - half) ?: continue
            val p3 = project(x + half, 0f, z + half) ?: continue
            val p4 = project(x - half, 0f, z + half) ?: continue

            groundPaint.color = Color.parseColor("#4CAF50")

            val path = Path()
            path.moveTo(p1[0], p1[1])
            path.lineTo(p2[0], p2[1])
            path.lineTo(p3[0], p3[1])
            path.lineTo(p4[0], p4[1])
            path.close()
            canvas.drawPath(path, groundPaint)
        }
    }

    private fun drawJoystick(canvas: Canvas) {
        canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBasePaint)
        canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBorderPaint)
        canvas.drawCircle(joyKnobX, joyKnobY, joyRadius * 0.4f, joyKnobPaint)
    }

    private fun drawJumpButton(canvas: Canvas) {
        // Кнопка прыжка
        val paint = if (jumpBtnPressed) {
            Paint().apply {
                color = Color.parseColor("#AAFFC107")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
        } else jumpBtnPaint

        canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, paint)
        canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, jumpBtnBorderPaint)

        // Текст "↑"
        val textY = jumpBtnY - (jumpBtnTextPaint.descent() + jumpBtnTextPaint.ascent()) / 2f
        canvas.drawText("↑", jumpBtnX, textY, jumpBtnTextPaint)
    }

    private fun drawCrosshair(canvas: Canvas) {
        val cx = screenW / 2f
        val cy = screenH / 2f
        canvas.drawCircle(cx, cy, 20f, crosshairPaint)
        canvas.drawCircle(cx, cy, 4f, crosshairDotPaint)
    }

    // ============ ФИЗИКА ============

    private fun update() {
        // Горизонтальное движение
        if (joyActive) {
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            val moveX = joyDeltaX
            val moveZ = -joyDeltaY

            val worldX = moveX * cosYaw + moveZ * sinYaw
            val worldZ = -moveX * sinYaw + moveZ * cosYaw

            val speed = 12f
            playerX += worldX * speed
            playerZ += worldZ * speed
        }
// Гравитация (вертикаль) — тянет вниз
velocityY -= gravity
playerY += velocityY

// Пол — не проваливаемся ниже 0
if (playerY <= 0f) {
    playerY = 0f
    velocityY = 0f
    onGround = true
} else {
    onGround = false
}
    }

    // ============ МУЛЬТИТАЧ ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Проверяем: попали ли в кнопку прыжка?
                val jumpDx = x - jumpBtnX
                val jumpDy = y - jumpBtnY
                val jumpDist = Math.sqrt((jumpDx * jumpDx + jumpDy * jumpDy).toDouble()).toFloat()

                if (jumpDist <= jumpBtnRadius * 1.3f) {
                    jumpBtnPressed = true
                    jumpTouchId = event.getPointerId(0)
                    doJump()
                    return true
                }

                // Проверяем: попали ли в джойстик?
                val joyDx = x - joyCenterX
                val joyDy = y - joyCenterY
                val joyDist = Math.sqrt((joyDx * joyDx + joyDy * joyDy).toDouble()).toFloat()

                if (joyDist <= joyRadius * 1.5f) {
                    joyActive = true
                    joyTouchId = event.getPointerId(0)
                    updateJoystick(x, y)
                    return true
                }

                // Иначе — поворот
                rotateActive = true
                rotateTouchId = event.getPointerId(0)
                lastRotateX = x
                lastRotateY = y
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val x = event.getX(index)
                val y = event.getY(index)
                val id = event.getPointerId(index)

                val jumpDx = x - jumpBtnX
                val jumpDy = y - jumpBtnY
                val jumpDist = Math.sqrt((jumpDx * jumpDx + jumpDy * jumpDy).toDouble()).toFloat()

                if (jumpDist <= jumpBtnRadius * 1.3f && !jumpBtnPressed) {
                    jumpBtnPressed = true
                    jumpTouchId = id
                    doJump()
                    return true
                }

                val joyDx = x - joyCenterX
                val joyDy = y - joyCenterY
                val joyDist = Math.sqrt((joyDx * joyDx + joyDy * joyDy).toDouble()).toFloat()

                if (joyDist <= joyRadius * 1.5f && !joyActive) {
                    joyActive = true
                    joyTouchId = id
                    updateJoystick(x, y)
                    return true
                }

                if (!rotateActive) {
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
                if (id == jumpTouchId) {
                    jumpBtnPressed = false
                    jumpTouchId = -1
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                joyActive = false
                rotateActive = false
                jumpBtnPressed = false
                joyTouchId = -1
                rotateTouchId = -1
                jumpTouchId = -1
                joyDeltaX = 0f
                joyDeltaY = 0f
                joyKnobX = joyCenterX
                joyKnobY = joyCenterY
            }
        }
        return true
    }

    private fun doJump() {
        if (onGround) {
            velocityY = jumpPower
            onGround = false
        }
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
