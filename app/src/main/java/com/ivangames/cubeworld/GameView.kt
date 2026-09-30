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

    // Размеры экрана
    private var screenW = 0f
    private var screenH = 0f

    // ===== Камера =====
    private var camX = 0f          // позиция камеры
    private var camY = 300f        // высота камеры
    private var camZ = -500f
    private var camYaw = 0f        // поворот камеры (влево-вправо)
    private var camPitch = -20f    // наклон камеры (вверх-вниз)
    private val camDistance = 400f // расстояние от камеры до персонажа

    // ===== Персонаж =====
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 0f
    private val playerSize = 80f

    // ===== Джойстик =====
    private var joyCenterX = 0f
    private var joyCenterY = 0f
    private var joyRadius = 200f
    private var joyKnobX = 0f
    private var joyKnobY = 0f
    private var joyActive = false
    private var joyDeltaX = 0f  // -1..1
    private var joyDeltaY = 0f  // -1..1

    // ===== Свайп для поворота камеры =====
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isRotating = false

    // ===== Мир =====
    // Список кубов: x, y, z, size, color
    private val cubes = mutableListOf<Cube>()

    class Cube(val x: Float, val y: Float, val z: Float, val size: Float, val color: Int)

    // ===== Краски =====
    private val bgPaint = Paint().apply { color = Color.parseColor("#87CEEB") }
    private val groundPaint = Paint().apply { color = Color.parseColor("#4CAF50") }
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

        // Джойстик — левый нижний угол
        joyRadius = minOf(screenW, screenH) * 0.16f
        joyCenterX = joyRadius + 60f
        joyCenterY = screenH - joyRadius - 60f
        joyKnobX = joyCenterX
        joyKnobY = joyCenterY
    }

    private fun setupWorld() {
        cubes.clear()

        // Создаём кубы вокруг игрока (мир)
        val colors = intArrayOf(
            Color.parseColor("#E94560"),  // красный
            Color.parseColor("#F39C12"),  // оранжевый
            Color.parseColor("#9B59B6"),  // фиолетовый
            Color.parseColor("#3498DB"),  // синий
            Color.parseColor("#1ABC9C"),  // бирюзовый
            Color.parseColor("#E74C3C")   // тёмно-красный
        )

        // Генерируем 20 кубов в случайных местах
        for (i in 0 until 20) {
            val angle = i * (360f / 20f) * Math.PI.toFloat() / 180f
            val dist = 300f + (i % 5) * 150f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val y = 0f
            val size = 60f + (i % 3) * 30f
            val color = colors[i % colors.size]
            cubes.add(Cube(x, y, z, size, color))
        }
    }
// ============ 3D-ПРОЕКЦИЯ ============

// Проецируем 3D-точку в 2D экран
private fun project(x: Float, y: Float, z: Float): FloatArray? {
    // 1. Смещение относительно камеры
    val dx = x - camX
    val dy = y - camY
    val dz = z - camZ

    // 2. Поворот по Y (yaw)
    val yawRad = Math.toRadians(camYaw.toDouble())
    val cosYaw = cos(yawRad).toFloat()
    val sinYaw = sin(yawRad).toFloat()

    val rx = dx * cosYaw - dz * sinYaw
    val rz = dx * sinYaw + dz * cosYaw

    // 3. Поворот по X (pitch)
    val pitchRad = Math.toRadians(camPitch.toDouble())
    val cosPitch = cos(pitchRad).toFloat()
    val sinPitch = sin(pitchRad).toFloat()

    val ry = dy * cosPitch - rz * sinPitch
    val rz2 = dy * sinPitch + rz * cosPitch

    // 4. Если точка позади камеры — не рисуем
    if (rz2 <= 1f) return null

    // 5. Перспективная проекция
    val fov = 500f
    val screenX = screenW / 2f + (rx * fov / rz2)
    val screenY = screenH / 2f - (ry * fov / rz2)

    return floatArrayOf(screenX, screenY, rz2)
}

// Рисуем куб (псевдо-3D — рисуем только верхнюю грань и видимые стороны)
private fun drawCube(canvas: Canvas, cube: Cube) {
    val s = cube.size / 2f

    // 8 вершин куба
    val vertices = arrayOf(
        floatArrayOf(cube.x - s, cube.y - s, cube.z - s), // 0
        floatArrayOf(cube.x + s, cube.y - s, cube.z - s), // 1
        floatArrayOf(cube.x + s, cube.y - s, cube.z + s), // 2
        floatArrayOf(cube.x - s, cube.y - s, cube.z + s), // 3
        floatArrayOf(cube.x - s, cube.y + s, cube.z - s), // 4
        floatArrayOf(cube.x + s, cube.y + s, cube.z - s), // 5
        floatArrayOf(cube.x + s, cube.y + s, cube.z + s), // 6
        floatArrayOf(cube.x - s, cube.y + s, cube.z + s)  // 7
    )

    // Проецируем все вершины
    val projected = arrayOfNulls<FloatArray>(8)
    for (i in 0 until 8) {
        projected[i] = project(vertices[i][0], vertices[i][1], vertices[i][2])
    }

    // Если хотя бы одна вершина позади — пропускаем куб
    if (projected.any { it == null }) return

    val paint = Paint().apply {
        color = cube.color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Верхняя грань (4, 5, 6, 7)
    drawFace(canvas, projected[4]!!, projected[5]!!, projected[6]!!, projected[7]!!, paint)

    // Передняя грань (0, 1, 5, 4)
    drawFace(canvas, projected[0]!!, projected[1]!!, projected[5]!!, projected[4]!!, paint)

    // Задняя грань (3, 2, 6, 7)
    drawFace(canvas, projected[3]!!, projected[2]!!, projected[6]!!, projected[7]!!, paint)

    // Левая грань (0, 3, 7, 4)
    drawFace(canvas, projected[0]!!, projected[3]!!, projected[7]!!, projected[4]!!, paint)

    // Правая грань (1, 2, 6, 5)
    drawFace(canvas, projected[1]!!, projected[2]!!, projected[6]!!, projected[5]!!, paint)

    // Нижняя грань (0, 1, 2, 3)
    drawFace(canvas, projected[0]!!, projected[1]!!, projected[2]!!, projected[3]!!, paint)
}

// Рисуем одну грань куба
private fun drawFace(canvas: Canvas, p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, paint: Paint) {
    val path = Path()
    path.moveTo(p1[0], p1[1])
    path.lineTo(p2[0], p2[1])
    path.lineTo(p3[0], p3[1])
    path.lineTo(p4[0], p4[1])
    path.close()
    canvas.drawPath(path, paint)
}

// ============ ОБНОВЛЕНИЕ КАМЕРЫ ============

private fun updateCamera() {
    // Камера сзади персонажа, на camDistance
    val yawRad = Math.toRadians(camYaw.toDouble())
    val offsetX = sin(yawRad).toFloat() * camDistance
    val offsetZ = -cos(yawRad).toFloat() * camDistance

    camX = playerX + offsetX
    camY = playerY + 250f
    camZ = playerZ + offsetZ
}

// ============ РИСОВАНИЕ ============

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    // Небо
    canvas.drawColor(Color.parseColor("#87CEEB"))

    // Обновляем камеру перед рисованием
    updateCamera()

    // Сортируем кубы по глубине (дальние — раньше)
    val sortedCubes = cubes.sortedByDescending { cube ->
        val dx = cube.x - camX
        val dz = cube.z - camZ
        dx * dx + dz * dz
    }

    // Рисуем землю (огромный плоский куб)
    drawGround(canvas)

    // Рисуем все кубы
    for (cube in sortedCubes) {
        drawCube(canvas, cube)
    }

    // Рисуем персонажа
    drawPlayer(canvas)

    // Рисуем джойстик
    drawJoystick(canvas)

    // Обновляем физику
    update()
    invalidate()
}

// Земля — большая плоскость
private fun drawGround(canvas: Canvas) {
    // Рисуем "лужайку" как большой квадрат, разделённый на клетки
    val gridSize = 400f
    val range = 3

    val groundPaintLocal = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    for (ix in -range..range) {
        for (iz in -range..range) {
            val x = ix * gridSize
            val z = iz * gridSize
            val halfSize = gridSize / 2f

            // Проецируем 4 угла клетки
            val p1 = project(x - halfSize, 0f, z - halfSize) ?: continue
            val p2 = project(x + halfSize, 0f, z - halfSize) ?: continue
            val p3 = project(x + halfSize, 0f, z + halfSize) ?: continue
            val p4 = project(x - halfSize, 0f, z + halfSize) ?: continue

            // Цвет — шахматный порядок
            val isDark = (ix + iz) % 2 == 0
            groundPaintLocal.color = if (isDark) Color.parseColor("#5DAE5D") else Color.parseColor("#6DBE6D")

            val path = Path()
            path.moveTo(p1[0], p1[1])
            path.lineTo(p2[0], p2[1])
            path.lineTo(p3[0], p3[1])
            path.lineTo(p4[0], p4[1])
            path.close()
            canvas.drawPath(path, groundPaintLocal)

            // Обводка (чтобы клетки выделялись)
            val borderPaint = Paint().apply {
                color = Color.parseColor("#4A9E4A")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawPath(path, borderPaint)
        }
    }
}

// Персонаж — куб
private fun drawPlayer(canvas: Canvas) {
    val playerCube = Cube(playerX, playerY + playerSize / 2f, playerZ, playerSize, Color.parseColor("#FFFFFF"))
    drawCube(canvas, playerCube)
}

// Джойстик
private fun drawJoystick(canvas: Canvas) {
    // База джойстика
    canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBasePaint)
    canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, joyBorderPaint)

    // Ручка джойстика
    canvas.drawCircle(joyKnobX, joyKnobY, joyRadius * 0.4f, joyKnobPaint)
}
    // ============ ФИЗИКА ============

    private fun update() {
        // Движение персонажа относительно джойстика и камеры
        if (joyActive) {
            // Направление движения относительно камеры (yaw)
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            // Джойстик: X — влево/вправо, Y — вперёд/назад
            val moveX = joyDeltaX
            val moveZ = joyDeltaY

            // Преобразуем в мировые координаты с учётом поворота камеры
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
                // Проверяем: попали ли в джойстик?
                val dx = x - joyCenterX
                val dy = y - joyCenterY
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= joyRadius * 1.5f) {
                    // Активируем джойстик
                    joyActive = true
                    updateJoystick(x, y)
                } else {
                    // Начинаем поворот камеры
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

                    // Ограничиваем наклон камеры
                    camPitch = camPitch.coerceIn(-60f, 20f)

                    lastTouchX = x
                    lastTouchY = y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (joyActive) {
                    // Отпустили джойстик — сбрасываем
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

    // Обновляем позицию ручки джойстика
    private fun updateJoystick(touchX: Float, touchY: Float) {
        var dx = touchX - joyCenterX
        var dy = touchY - joyCenterY

        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        // Ограничиваем ручку в пределах радиуса
        if (dist > joyRadius) {
            val k = joyRadius / dist
            dx *= k
            dy *= k
        }

        joyKnobX = joyCenterX + dx
        joyKnobY = joyCenterY + dy

        // Нормализуем -1..1
        joyDeltaX = dx / joyRadius
        joyDeltaY = dy / joyRadius
    }
}
