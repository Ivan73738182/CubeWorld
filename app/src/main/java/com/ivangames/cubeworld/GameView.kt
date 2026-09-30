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

    // Герой
    private var playerX = 0f
    private var playerY = 0f
    private var playerZ = 0f
    private var velocityY = 0f
    private var onGround = true
    private val eyeHeight = 150f

    // HP героя
    private var playerHp = 100
    private val playerMaxHp = 100

    // Физика
    private val gravity = 1.5f
    private val jumpPower = 20f

    // Размеры
    private val groundBlockSize = 100f
    private val cubeSize = 100f
    private val playerRadius = 30f   // радиус игрока для столкновений

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

    // Кнопка прыжка
    private var jumpBtnX = 0f
    private var jumpBtnY = 0f
    private var jumpBtnRadius = 100f
    private var jumpBtnPressed = false
    private var jumpTouchId = -1

    // Кнопка атаки
    private var attackBtnX = 0f
    private var attackBtnY = 0f
    private var attackBtnRadius = 100f
    private var attackBtnPressed = false
    private var attackTouchId = -1

    // Поворот
    private var rotateActive = false
    private var rotateTouchId = -1
    private var lastRotateX = 0f
    private var lastRotateY = 0f

    // Анимация удара
    private var attackAnimTimer = 0

    // ===== МИР =====
    private val walls = mutableListOf<Wall>()   // стены (дома)
    private val enemies = mutableListOf<Enemy>()

    class Wall(val x: Float, val z: Float, val size: Float, val height: Float, val color: Int)
    class Enemy(
        var x: Float,
        var y: Float,
        var z: Float,
        var size: Float,
        var hp: Int,
        val maxHp: Int
    )

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
    private val attackBtnPaint = Paint().apply {
        color = Color.parseColor("#66E94560")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val attackBtnBorderPaint = Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val attackBtnTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
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
    private val hpBarBgPaint = Paint().apply {
        color = Color.parseColor("#55000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val hpBarFillPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val hpBarBorderPaint = Paint().apply {
        color = Color.parseColor("#AAFFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    private val hpTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
        isFakeBoldText = true
    }
    private val swordPaint = Paint().apply {
        color = Color.parseColor("#CCCCCC")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }
    private val swordHandlePaint = Paint().apply {
        color = Color.parseColor("#8B4513")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
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

        jumpBtnRadius = minOf(screenW, screenH) * 0.09f
        jumpBtnX = screenW - jumpBtnRadius - 100f
        jumpBtnY = screenH - jumpBtnRadius - 100f

        attackBtnRadius = minOf(screenW, screenH) * 0.09f
        attackBtnX = screenW - attackBtnRadius - 100f
        attackBtnY = jumpBtnY - jumpBtnRadius - attackBtnRadius - 30f
    }

    private fun setupWorld() {
        walls.clear()
        enemies.clear()

        // ===== ДОМ 1 — слева =====
        // Стены дома (квадрат 400x400, стены по краям)
        val house1X = -600f
        val house1Z = -400f
        buildHouse(house1X, house1Z, Color.parseColor("#8B4513"))

        // ===== ДОМ 2 — справа =====
        val house2X = 600f
        val house2Z = -400f
        buildHouse(house2X, house2Z, Color.parseColor("#A0522D"))

        // ===== ВРАГИ =====
        for (i in 0 until 5) {
            val angle = i * (360f / 5f) * Math.PI.toFloat() / 180f
            val dist = 500f + i * 100f
            val x = cos(angle) * dist
            val z = sin(angle) * dist
            val enemySize = 120f
            enemies.add(
                Enemy(
                    x = x,
                    y = enemySize / 2f,
                    z = z,
                    size = enemySize,
                    hp = 50,
                    maxHp = 50
                )
            )
        }
    }

    // Строим дом (стены по периметру)
    private fun buildHouse(centerX: Float, centerZ: Float, color: Int) {
        val halfSize = 250f       // размер дома от центра
        val wallThickness = 80f   // толщина стен
        val wallHeight = 200f     // высота стен

        // Верхняя стена
        walls.add(Wall(centerX, centerZ - halfSize, halfSize * 2 + wallThickness, wallHeight, color))
        // Нижняя стена (с проёмом в центре — вход)
        walls.add(Wall(centerX - halfSize / 2 - 30f, centerZ + halfSize, halfSize, wallHeight, color))
        walls.add(Wall(centerX + halfSize / 2 + 30f, centerZ + halfSize, halfSize, wallHeight, color))
        // Левая стена
        walls.add(Wall(centerX - halfSize, centerZ, halfSize * 2 + wallThickness, wallHeight, color))
        // Правая стена
        walls.add(Wall(centerX + halfSize, centerZ, halfSize * 2 + wallThickness, wallHeight, color))
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

    // Стены домов + враги — все в общий список, сортируем по глубине
    data class DrawItem(val depth: Float, val draw: () -> Unit)
    val items = mutableListOf<DrawItem>()

    // Стены
    for (wall in walls) {
        val dx = wall.x - camX
        val dz = wall.z - camZ
        val depth = dx * dx + dz * dz
        items.add(DrawItem(depth) {
            drawBox(canvas, wall.x, wall.height / 2f, wall.z,
                wall.size, wall.height, wall.size, wall.color)
        })
    }

    // Враги (живые)
    for (e in enemies) {
        if (e.hp <= 0) continue
        val dx = e.x - camX
        val dz = e.z - camZ
        val depth = dx * dx + dz * dz
        items.add(DrawItem(depth) {
            drawBox(canvas, e.x, e.y, e.z, e.size, e.size, e.size, Color.parseColor("#E94560"))
        })
    }

    // Сортируем — дальние раньше
    for (item in items.sortedByDescending { it.depth }) {
        item.draw()
    }

    // Джойстик
    drawJoystick(canvas)

    // Кнопка прыжка
    drawJumpButton(canvas)

    // Кнопка атаки
    drawAttackButton(canvas)

    // Прицел
    drawCrosshair(canvas)

    // Меч
    drawSword(canvas)

    // HP
    drawHpBars(canvas)

    update()
    invalidate()
}

private fun drawGround(canvas: Canvas) {
    val range = 12
    val groundPaint = Paint().apply {
        color = Color.parseColor("#4CAF50")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val centerX = (playerX / groundBlockSize).toInt() * groundBlockSize
    val centerZ = (playerZ / groundBlockSize).toInt() * groundBlockSize

    data class GBlock(val x: Float, val z: Float, val depth: Float)
    val blocks = mutableListOf<GBlock>()

    for (ix in -range..range) {
        for (iz in -range..range) {
            val x = centerX + ix * groundBlockSize
            val z = centerZ + iz * groundBlockSize
            val dx = x - camX
            val dz = z - camZ
            blocks.add(GBlock(x, z, dx * dx + dz * dz))
        }
    }

    val sorted = blocks.sortedByDescending { it.depth }

    for (block in sorted) {
        val half = groundBlockSize / 2f
        val p1 = project(block.x - half, 0f, block.z - half) ?: continue
        val p2 = project(block.x + half, 0f, block.z - half) ?: continue
        val p3 = project(block.x + half, 0f, block.z + half) ?: continue
        val p4 = project(block.x - half, 0f, block.z + half) ?: continue

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
    val paint = if (jumpBtnPressed) {
        Paint().apply {
            color = Color.parseColor("#AAFFC107")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    } else jumpBtnPaint

    canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, paint)
    canvas.drawCircle(jumpBtnX, jumpBtnY, jumpBtnRadius, jumpBtnBorderPaint)

    val textY = jumpBtnY - (jumpBtnTextPaint.descent() + jumpBtnTextPaint.ascent()) / 2f
    canvas.drawText("↑", jumpBtnX, textY, jumpBtnTextPaint)
}

private fun drawAttackButton(canvas: Canvas) {
    val paint = if (attackBtnPressed) {
        Paint().apply {
            color = Color.parseColor("#AAE94560")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    } else attackBtnPaint

    canvas.drawCircle(attackBtnX, attackBtnY, attackBtnRadius, paint)
    canvas.drawCircle(attackBtnX, attackBtnY, attackBtnRadius, attackBtnBorderPaint)

    val textY = attackBtnY - (attackBtnTextPaint.descent() + attackBtnTextPaint.ascent()) / 2f
    canvas.drawText("⚔", attackBtnX, textY, attackBtnTextPaint)
}

private fun drawCrosshair(canvas: Canvas) {
    val cx = screenW / 2f
    val cy = screenH / 2f
    canvas.drawCircle(cx, cy, 20f, crosshairPaint)
    canvas.drawCircle(cx, cy, 4f, crosshairDotPaint)
}

private fun drawSword(canvas: Canvas) {
    val swingOffset = if (attackAnimTimer > 0) {
        val progress = (15 - attackAnimTimer) / 15f
        (1f - Math.abs(progress - 0.5f) * 2f) * 80f
    } else 0f

    val baseX = screenW * 0.85f + swingOffset
    val baseY = screenH * 0.95f - swingOffset

    val handleEndX = baseX - 60f
    val handleEndY = baseY - 60f
    canvas.drawLine(baseX, baseY, handleEndX, handleEndY, swordHandlePaint)

    val bladeEndX = handleEndX - 180f
    val bladeEndY = handleEndY - 180f
    canvas.drawLine(handleEndX, handleEndY, bladeEndX, bladeEndY, swordPaint)

    canvas.drawCircle(bladeEndX, bladeEndY, 6f, swordPaint)
}

private fun drawHpBars(canvas: Canvas) {
    val barWidth = 400f
    val barHeight = 40f
    val margin = 60f

    val heroBarX = margin
    val heroBarY = margin + 40f

    canvas.drawRect(heroBarX, heroBarY, heroBarX + barWidth, heroBarY + barHeight, hpBarBgPaint)

    val heroFill = playerHp.toFloat() / playerMaxHp * barWidth
    if (heroFill > 0) {
        canvas.drawRect(heroBarX, heroBarY, heroBarX + heroFill, heroBarY + barHeight, hpBarFillPaint)
    }

    canvas.drawRect(heroBarX, heroBarY, heroBarX + barWidth, heroBarY + barHeight, hpBarBorderPaint)

    canvas.drawText("HP: $playerHp / $playerMaxHp", heroBarX + 10f, heroBarY + barHeight - 8f, hpTextPaint)
}
    // ============ ФИЗИКА ============

    private fun update() {
        // ===== Горизонтальное движение игрока =====
        if (joyActive) {
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            val moveX = joyDeltaX
            val moveZ = -joyDeltaY

            val worldX = moveX * cosYaw + moveZ * sinYaw
            val worldZ = -moveX * sinYaw + moveZ * cosYaw

            val speed = 12f

            // Пытаемся сдвинуться по X
            val newX = playerX + worldX * speed
            if (!collidesWithWall(newX, playerZ)) {
                playerX = newX
            }

            // Пытаемся сдвинуться по Z
            val newZ = playerZ + worldZ * speed
            if (!collidesWithWall(playerX, newZ)) {
                playerZ = newZ
            }
        }

        // ===== Гравитация =====
        velocityY -= gravity
        playerY += velocityY

        if (playerY <= 0f) {
            playerY = 0f
            velocityY = 0f
            onGround = true
        } else {
            onGround = false
        }

        // ===== ВРАГИ ХОДЯТ ЗА ИГРОКОМ =====
        for (enemy in enemies) {
            if (enemy.hp <= 0) continue

            val dx = playerX - enemy.x
            val dz = playerZ - enemy.z
            val dist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

            if (dist > 150f) {
                val speed = 3f
                val nx = dx / dist
                val nz = dz / dist

                // Враг тоже упирается в стены
                val newX = enemy.x + nx * speed
                if (!collidesWithWall(newX, enemy.z)) {
                    enemy.x = newX
                }
                val newZ = enemy.z + nz * speed
                if (!collidesWithWall(enemy.x, newZ)) {
                    enemy.z = newZ
                }
            }
        }

        // ===== Анимация удара =====
        if (attackAnimTimer > 0) {
            attackAnimTimer--
        }
    }

    // ===== СТОЛКНОВЕНИЯ С ТВЁРДЫМИ ОБЪЕКТАМИ =====
    private fun collidesWithWall(x: Float, z: Float): Boolean {
        for (wall in walls) {
            val halfSize = wall.size / 2f
            val dx = Math.abs(x - wall.x)
            val dz = Math.abs(z - wall.z)

            if (dx < halfSize + playerRadius && dz < halfSize + playerRadius) {
                return true
            }
        }
        return false
    }

    // ============ АТАКА ============

    private fun doAttack() {
        attackAnimTimer = 15

        val yawRad = Math.toRadians(camYaw.toDouble())
        val dirX = -sin(yawRad).toFloat()
        val dirZ = cos(yawRad).toFloat()

        var bestEnemy: Enemy? = null
        var bestDist = Float.MAX_VALUE

        for (enemy in enemies) {
            if (enemy.hp <= 0) continue

            val dx = enemy.x - playerX
            val dz = enemy.z - playerZ
            val dist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

            if (dist > 250f) continue

            val enemyDirX = dx / dist
            val enemyDirZ = dz / dist

            val dot = enemyDirX * dirX + enemyDirZ * dirZ

            if (dot > 0.7f) {
                if (dist < bestDist) {
                    bestDist = dist
                    bestEnemy = enemy
                }
            }
        }

        bestEnemy?.let {
            it.hp -= 25
            if (it.hp < 0) it.hp = 0
        }
    }

    // ============ ТАП ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                // Прыжок
                val jumpDx = x - jumpBtnX
                val jumpDy = y - jumpBtnY
                val jumpDist = Math.sqrt((jumpDx * jumpDx + jumpDy * jumpDy).toDouble()).toFloat()

                if (jumpDist <= jumpBtnRadius * 1.3f) {
                    jumpBtnPressed = true
                    jumpTouchId = event.getPointerId(0)
                    doJump()
                    return true
                }

                // Атака
                val attackDx = x - attackBtnX
                val attackDy = y - attackBtnY
                val attackDist = Math.sqrt((attackDx * attackDx + attackDy * attackDy).toDouble()).toFloat()

                if (attackDist <= attackBtnRadius * 1.3f) {
                    attackBtnPressed = true
                    attackTouchId = event.getPointerId(0)
                    doAttack()
                    return true
                }

                // Джойстик
                val joyDx = x - joyCenterX
                val joyDy = y - joyCenterY
                val joyDist = Math.sqrt((joyDx * joyDx + joyDy * joyDy).toDouble()).toFloat()

                if (joyDist <= joyRadius * 1.5f) {
                    joyActive = true
                    joyTouchId = event.getPointerId(0)
                    updateJoystick(x, y)
                    return true
                }

                // Поворот
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

                val attackDx = x - attackBtnX
                val attackDy = y - attackBtnY
                val attackDist = Math.sqrt((attackDx * attackDx + attackDy * attackDy).toDouble()).toFloat()

                if (attackDist <= attackBtnRadius * 1.3f && !attackBtnPressed) {
                    attackBtnPressed = true
                    attackTouchId = id
                    doAttack()
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
                if (id == attackTouchId) {
                    attackBtnPressed = false
                    attackTouchId = -1
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                joyActive = false
                rotateActive = false
                jumpBtnPressed = false
                attackBtnPressed = false
                joyTouchId = -1
                rotateTouchId = -1
                jumpTouchId = -1
                attackTouchId = -1
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
