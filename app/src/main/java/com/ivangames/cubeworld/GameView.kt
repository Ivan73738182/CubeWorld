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
import kotlin.random.Random

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

    // Прогресс
    private var playerXp = 0
    private var playerLevel = 1
    private var playerCoins = 0
    private var playerKills = 0
    private val xpPerLevel = 100

    // Физика
    private val gravity = 1.5f
    private val jumpPower = 20f

    // Размеры
    private val groundBlockSize = 100f
    private val cubeSize = 100f
    private val playerRadius = 30f

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

    // Прыжок
    private var jumpBtnX = 0f
    private var jumpBtnY = 0f
    private var jumpBtnRadius = 100f
    private var jumpBtnPressed = false
    private var jumpTouchId = -1

    // Атака
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

    // Частицы
    private val particles = mutableListOf<Particle>()
    class Particle(
        var x: Float, var y: Float, var z: Float,
        var vx: Float, var vy: Float, var vz: Float,
        var life: Int
    )

    // Мир
    private val walls = mutableListOf<Wall>()
    private val enemies = mutableListOf<Enemy>()

    class Wall(val x: Float, val z: Float, val size: Float, val height: Float, val color: Int)
    class Enemy(
        var x: Float,
        var y: Float,
        var z: Float,
        var size: Float,
        var hp: Int,
        val maxHp: Int,
        var attackCooldown: Int = 0
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
    private val statsTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 26f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.RIGHT
    }
    private val xpBarBgPaint = Paint().apply {
        color = Color.parseColor("#55000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val xpBarFillPaint = Paint().apply {
        color = Color.parseColor("#FF2196F3")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val xpBarBorderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
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
    private val particlePaint = Paint().apply {
        color = Color.parseColor("#E94560")
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
        particles.clear()

        playerXp = 0
        playerLevel = 1
        playerCoins = 0
        playerKills = 0
        playerHp = playerMaxHp

        val house1X = -600f
        val house1Z = -400f
        buildHouse(house1X, house1Z, Color.parseColor("#8B4513"))

        val house2X = 600f
        val house2Z = -400f
        buildHouse(house2X, house2Z, Color.parseColor("#A0522D"))

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

    private fun buildHouse(centerX: Float, centerZ: Float, color: Int) {
        val halfSize = 250f
        val wallThickness = 80f
        val wallHeight = 200f

        walls.add(Wall(centerX, centerZ - halfSize, halfSize * 2 + wallThickness, wallHeight, color))
        walls.add(Wall(centerX - halfSize / 2 - 30f, centerZ + halfSize, halfSize, wallHeight, color))
        walls.add(Wall(centerX + halfSize / 2 + 30f, centerZ + halfSize, halfSize, wallHeight, color))
        walls.add(Wall(centerX - halfSize, centerZ, halfSize * 2 + wallThickness, wallHeight, color))
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

    canvas.drawColor(Color.parseColor("#87CEEB"))

    updateCamera()

    drawGround(canvas)

    data class DrawItem(val depth: Float, val draw: () -> Unit)
    val items = mutableListOf<DrawItem>()

    for (wall in walls) {
        val dx = wall.x - camX
        val dz = wall.z - camZ
        val depth = dx * dx + dz * dz
        items.add(DrawItem(depth) {
            drawBox(canvas, wall.x, wall.height / 2f, wall.z,
                wall.size, wall.height, wall.size, wall.color)
        })
    }

    for (e in enemies) {
        if (e.hp <= 0) continue
        val dx = e.x - camX
        val dz = e.z - camZ
        val depth = dx * dx + dz * dz
        items.add(DrawItem(depth) {
            drawBox(canvas, e.x, e.y, e.z, e.size, e.size, e.size, Color.parseColor("#E94560"))
            drawEnemyHpBar(canvas, e)
        })
    }

    for (item in items.sortedByDescending { it.depth }) {
        item.draw()
    }

    drawParticles(canvas)

    drawJoystick(canvas)
    drawJumpButton(canvas)
    drawAttackButton(canvas)
    drawCrosshair(canvas)
    drawSword(canvas)
    drawHpBars(canvas)
    drawStats(canvas)

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

    for (block in blocks.sortedByDescending { it.depth }) {
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

private fun drawEnemyHpBar(canvas: Canvas, enemy: Enemy) {
    val headY = enemy.y + enemy.size / 2f + 30f
    val pos = project(enemy.x, headY, enemy.z) ?: return

    val screenX = pos[0]
    val screenY = pos[1]

    val depth = pos[2]
    val scale = (400f / depth).coerceIn(0.4f, 1.2f)

    val barWidth = 80f * scale
    val barHeight = 10f * scale

    val left = screenX - barWidth / 2f
    val top = screenY - barHeight / 2f

    val bgPaint = Paint().apply {
        color = Color.parseColor("#CC550000")
        style = Paint.Style.FILL
    }
    canvas.drawRect(left, top, left + barWidth, top + barHeight, bgPaint)

    val hpPercent = enemy.hp.toFloat() / enemy.maxHp
    val fillPaint = Paint().apply {
        color = Color.parseColor("#FF4CAF50")
        style = Paint.Style.FILL
    }
    canvas.drawRect(left, top, left + barWidth * hpPercent, top + barHeight, fillPaint)

    val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * scale
    }
    canvas.drawRect(left, top, left + barWidth, top + barHeight, borderPaint)
}

// ===== СТАТИСТИКА (СДВИНУТА НИЖЕ) =====
private fun drawStats(canvas: Canvas) {
    val margin = 60f
    val x = screenW - margin

    // Уровень
    canvas.drawText("Уровень: $playerLevel", x, margin + 40f, statsTextPaint)
    // XP
    canvas.drawText("XP: $playerXp / $xpPerLevel", x, margin + 80f, statsTextPaint)

    // Полоска XP
    val xpBarWidth = 300f
    val xpBarHeight = 12f
    val xpBarX = screenW - margin - xpBarWidth
    val xpBarY = margin + 95f

    canvas.drawRect(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, xpBarBgPaint)
    val fill = playerXp.toFloat() / xpPerLevel * xpBarWidth
    canvas.drawRect(xpBarX, xpBarY, xpBarX + fill, xpBarY + xpBarHeight, xpBarFillPaint)
    canvas.drawRect(xpBarX, xpBarY, xpBarX + xpBarWidth, xpBarY + xpBarHeight, xpBarBorderPaint)

    // Монеты и убийства — НИЖЕ (с отступом)
    canvas.drawText("💰 $playerCoins", x, margin + 170f, statsTextPaint)
    canvas.drawText("⚔ $playerKills", x, margin + 210f, statsTextPaint)
}

// ===== ЧАСТИЦЫ =====
private fun spawnParticles(x: Float, y: Float, z: Float) {
    for (i in 0 until 10) {
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val speed = Random.nextFloat() * 4f + 2f
        particles.add(
            Particle(
                x = x,
                y = y,
                z = z,
                vx = cos(angle) * speed,
                vy = Random.nextFloat() * 4f + 2f,
                vz = sin(angle) * speed,
                life = 30
            )
        )
    }
}

private fun drawParticles(canvas: Canvas) {
    for (p in particles) {
        val pos = project(p.x, p.y, p.z) ?: continue
        val size = (200f / pos[2]).coerceIn(1f, 8f)
        canvas.drawCircle(pos[0], pos[1], size, particlePaint)
    }
}

private fun gainXp(amount: Int) {
    playerXp += amount
    while (playerXp >= xpPerLevel) {
        playerXp -= xpPerLevel
        playerLevel++
        playerHp = playerMaxHp
    }
}

private fun onEnemyKilled(enemy: Enemy) {
    playerKills++
    gainXp(20)
    playerCoins += 5
    spawnParticles(enemy.x, enemy.y, enemy.z)
}
    // ============ ФИЗИКА ============

    private fun update() {
        if (joyActive) {
            val yawRad = Math.toRadians(camYaw.toDouble())
            val cosYaw = cos(yawRad).toFloat()
            val sinYaw = sin(yawRad).toFloat()

            val moveX = joyDeltaX
            val moveZ = -joyDeltaY

            val worldX = moveX * cosYaw + moveZ * sinYaw
            val worldZ = -moveX * sinYaw + moveZ * cosYaw

            val speed = 12f

            val newX = playerX + worldX * speed
            if (!collidesWithWall(newX, playerZ) && !collidesWithEnemy(newX, playerZ)) {
                playerX = newX
            }

            val newZ = playerZ + worldZ * speed
            if (!collidesWithWall(playerX, newZ) && !collidesWithEnemy(playerX, newZ)) {
                playerZ = newZ
            }
        }

        velocityY -= gravity
        playerY += velocityY

        if (playerY <= 0f) {
            playerY = 0f
            velocityY = 0f
            onGround = true
        } else {
            onGround = false
        }

        for (enemy in enemies) {
            if (enemy.hp <= 0) continue

            val dx = playerX - enemy.x
            val dz = playerZ - enemy.z
            val dist = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()

            if (dist > 150f) {
                val speed = 3f
                val nx = dx / dist
                val nz = dz / dist

                val newX = enemy.x + nx * speed
                if (!collidesWithWall(newX, enemy.z)) {
                    enemy.x = newX
                }
                val newZ = enemy.z + nz * speed
                if (!collidesWithWall(enemy.x, newZ)) {
                    enemy.z = newZ
                }
            } else {
                if (enemy.attackCooldown <= 0) {
                    playerHp -= 10
                    if (playerHp < 0) playerHp = 0
                    enemy.attackCooldown = 60
                }
            }

            if (enemy.attackCooldown > 0) enemy.attackCooldown--
        }

        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.z += p.vz
            p.vy -= 0.3f
            p.life--
            if (p.life <= 0) iter.remove()
        }

        if (attackAnimTimer > 0) attackAnimTimer--
    }

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

    // ТВЁРДЫЕ ВРАГИ
    private fun collidesWithEnemy(x: Float, z: Float): Boolean {
        for (enemy in enemies) {
            if (enemy.hp <= 0) continue
            val halfSize = enemy.size / 2f
            val dx = Math.abs(x - enemy.x)
            val dz = Math.abs(z - enemy.z)
            if (dx < halfSize + playerRadius && dz < halfSize + playerRadius) {
                return true
            }
        }
        return false
    }

    // ============ АТАКА (ТОЧНО ПО ЦЕНТРУ) ============

    private fun doAttack() {
        attackAnimTimer = 15

        val yawRad = Math.toRadians(camYaw.toDouble())
        val pitchRad = Math.toRadians(camPitch.toDouble())

        val cosYaw = cos(yawRad).toFloat()
        val sinYaw = sin(yawRad).toFloat()
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        val dirX = -sinYaw * cosPitch
        val dirY = sinPitch
        val dirZ = cosYaw * cosPitch

        for (enemy in enemies) {
            if (enemy.hp <= 0) continue

            val toX = enemy.x - playerX
            val toY = enemy.y - camY
            val toZ = enemy.z - playerZ

            val dist = Math.sqrt((toX * toX + toY * toY + toZ * toZ).toDouble()).toFloat()
            if (dist > 250f) continue

            val nx = toX / dist
            val ny = toY / dist
            val nz = toZ / dist

            val dot = nx * dirX + ny * dirY + nz * dirZ

            // Очень узкий конус — точно по прицелу
            if (dot > 0.97f) {
                enemy.hp -= 25
                if (enemy.hp < 0) enemy.hp = 0

                spawnParticles(enemy.x, enemy.y + enemy.size / 4, enemy.z)

                val dx = enemy.x - playerX
                val dz = enemy.z - playerZ
                val d = Math.sqrt((dx * dx + dz * dz).toDouble()).toFloat()
                if (d > 0.01f) {
                    enemy.x += dx / d * 50f
                    enemy.z += dz / d * 50f
                }

                if (enemy.hp <= 0) {
                    onEnemyKilled(enemy)
                }

                break
            }
        }
    }

    // ============ ТАП ============

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                val jumpDx = x - jumpBtnX
                val jumpDy = y - jumpBtnY
                val jumpDist = Math.sqrt((jumpDx * jumpDx + jumpDy * jumpDy).toDouble()).toFloat()

                if (jumpDist <= jumpBtnRadius * 1.3f) {
                    jumpBtnPressed = true
                    jumpTouchId = event.getPointerId(0)
                    doJump()
                    return true
                }

                val attackDx = x - attackBtnX
                val attackDy = y - attackBtnY
                val attackDist = Math.sqrt((attackDx * attackDx + attackDy * attackDy).toDouble()).toFloat()

                if (attackDist <= attackBtnRadius * 1.3f) {
                    attackBtnPressed = true
                    attackTouchId = event.getPointerId(0)
                    doAttack()
                    return true
                }

                val joyDx = x - joyCenterX
                val joyDy = y - joyCenterY
                val joyDist = Math.sqrt((joyDx * joyDx + joyDy * joyDy).toDouble()).toFloat()

                if (joyDist <= joyRadius * 1.5f) {
                    joyActive = true
                    joyTouchId = event.getPointerId(0)
                    updateJoystick(x, y)
                    return true
                }

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
