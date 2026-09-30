package com.ivangames.cubeworld

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class MenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var screenW = 0f
    private var screenH = 0f

    // Данные для фона
    private val trees = mutableListOf<Tree>()
    private val stars = mutableListOf<Star>()
    private var initialized = false

    class Tree(val x: Float, val baseY: Float, val size: Float, val color: Int)
    class Star(val x: Float, val y: Float, val size: Float)

    private val skyPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val mountainPaint = Paint().apply {
        color = Color.parseColor("#2A4A3A")
        style = Paint.Style.FILL
    }
    private val treePaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val trunkPaint = Paint().apply {
        color = Color.parseColor("#4A2A10")
        style = Paint.Style.FILL
    }
    private val groundPaint = Paint().apply {
        color = Color.parseColor("#1B4A2E")
        style = Paint.Style.FILL
    }
    private val starPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val moonPaint = Paint().apply {
        color = Color.parseColor("#FFF8DC")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val glowPaint = Paint().apply {
        color = Color.parseColor("#33FFF8DC")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()
        generateBackground()
    }

    private fun generateBackground() {
        if (screenW == 0f) return

        stars.clear()
        trees.clear()

        // Звёзды
        for (i in 0 until 40) {
            stars.add(
                Star(
                    Random.nextFloat() * screenW,
                    Random.nextFloat() * screenH * 0.5f,
                    Random.nextFloat() * 2f + 1f
                )
            )
        }

        // Деревья (пиксельные) — несколько слоёв
        val treeColors = intArrayOf(
            Color.parseColor("#1B4A2E"),
            Color.parseColor("#236B3F"),
            Color.parseColor("#2E8B57")
        )

        // Дальний слой
        var x = -50f
        while (x < screenW + 50f) {
            trees.add(
                Tree(
                    x,
                    screenH * 0.75f,
                    Random.nextFloat() * 60f + 60f,
                    treeColors[0]
                )
            )
            x += Random.nextFloat() * 40f + 40f
        }

        // Средний слой
        x = -30f
        while (x < screenW + 30f) {
            trees.add(
                Tree(
                    x,
                    screenH * 0.85f,
                    Random.nextFloat() * 80f + 80f,
                    treeColors[1]
                )
            )
            x += Random.nextFloat() * 60f + 60f
        }

        // Ближний слой
        x = -50f
        while (x < screenW + 50f) {
            trees.add(
                Tree(
                    x,
                    screenH * 0.95f,
                    Random.nextFloat() * 100f + 120f,
                    treeColors[2]
                )
            )
            x += Random.nextFloat() * 80f + 80f
        }

        initialized = true
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!initialized) return

        // Ночное небо (градиент тёмный)
        skyPaint.color = Color.parseColor("#0A0E2A")
        canvas.drawRect(0f, 0f, screenW, screenH, skyPaint)

        // Звёзды
        for (star in stars) {
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
        }

        // Луна
        val moonX = screenW * 0.8f
        val moonY = screenH * 0.15f
        val moonR = 60f

        // Свечение вокруг луны
        for (i in 1..5) {
            glowPaint.alpha = 30 / i
            canvas.drawCircle(moonX, moonY, moonR * (1f + i * 0.5f), glowPaint)
        }

        canvas.drawCircle(moonX, moonY, moonR, moonPaint)

        // Далёкие горы (пиксельные)
        val mountainPath = android.graphics.Path()
        mountainPath.moveTo(0f, screenH * 0.75f)
        var mx = 0f
        while (mx < screenW + 100f) {
            mountainPath.lineTo(mx, screenH * 0.55f)
            mountainPath.lineTo(mx + 80f, screenH * 0.75f)
            mx += 160f
        }
        mountainPath.lineTo(screenW, screenH * 0.75f)
        mountainPath.lineTo(screenW, screenH)
        mountainPath.lineTo(0f, screenH)
        mountainPath.close()
        canvas.drawPath(mountainPath, mountainPaint)

        // Деревья — рисуем от дальних к ближним
        val sorted = trees.sortedBy { it.baseY }

        for (tree in sorted) {
            drawTree(canvas, tree)
        }

        // Земля
        canvas.drawRect(0f, screenH * 0.92f, screenW, screenH, groundPaint)
    }

    private fun drawTree(canvas: Canvas, tree: Tree) {
        val size = tree.size
        val x = tree.x
        val baseY = tree.baseY

        // Ствол
        val trunkW = size * 0.15f
        canvas.drawRect(
            x + size * 0.425f, baseY,
            x + size * 0.575f, baseY + size * 0.4f,
            trunkPaint
        )

        // Крона (пиксельная — 3 уровня)
        treePaint.color = tree.color
        canvas.drawRect(
            x, baseY - size * 0.4f,
            x + size, baseY,
            treePaint
        )
        canvas.drawRect(
            x + size * 0.15f, baseY - size * 0.7f,
            x + size * 0.85f, baseY - size * 0.3f,
            treePaint
        )
        canvas.drawRect(
            x + size * 0.3f, baseY - size * 0.9f,
            x + size * 0.7f, baseY - size * 0.6f,
            treePaint
        )
    }
}
