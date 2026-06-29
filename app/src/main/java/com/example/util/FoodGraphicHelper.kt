package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object FoodGraphicHelper {

    fun generateFoodBitmap(foodType: String): Bitmap {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Draw background
        val bgPaint = Paint().apply {
            color = when (foodType) {
                "shake" -> Color.parseColor("#E1BEE7") // lavender for shake
                "chicken" -> Color.parseColor("#FFF9C4") // yellow for chicken plate
                "yogurt" -> Color.parseColor("#E0F2F1") // teal for yogurt
                else -> Color.parseColor("#FFCCBC") // peach for steak
            }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 512f, 512f, bgPaint)

        // Draw border
        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 10f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(5f, 5f, 507f, 507f, borderPaint)

        // Draw specific food elements
        val paint = Paint().apply {
            isAntiAlias = true
        }

        when (foodType) {
            "shake" -> {
                // Draw shaker bottle
                paint.color = Color.parseColor("#7B1FA2")
                paint.style = Paint.Style.FILL
                val bottlePath = android.graphics.Path().apply {
                    moveTo(180f, 420f)
                    lineTo(150f, 180f)
                    lineTo(362f, 180f)
                    lineTo(332f, 420f)
                    close()
                }
                canvas.drawPath(bottlePath, paint)

                // Shaker liquid level
                paint.color = Color.parseColor("#D81B60")
                val liquidPath = android.graphics.Path().apply {
                    moveTo(180f, 420f)
                    lineTo(165f, 280f)
                    lineTo(347f, 280f)
                    lineTo(332f, 420f)
                    close()
                }
                canvas.drawPath(liquidPath, paint)

                // Cap
                paint.color = Color.BLACK
                canvas.drawRect(140f, 140f, 372f, 180f, paint)
                canvas.drawCircle(256f, 130f, 20f, paint)

                // Text Label
                paint.color = Color.WHITE
                paint.textSize = 36f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("WHEY PROTEIN", 256f, 360f, paint)
            }
            "chicken" -> {
                // Plate
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                canvas.drawCircle(256f, 256f, 220f, paint)
                paint.color = Color.LTGRAY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 8f
                canvas.drawCircle(256f, 256f, 180f, paint)

                // Chicken Breast (golden brown)
                paint.color = Color.parseColor("#D84315")
                paint.style = Paint.Style.FILL
                val chickenRect = RectF(140f, 180f, 340f, 320f)
                canvas.drawOval(chickenRect, paint)

                // Rice grains (white dots)
                paint.color = Color.parseColor("#CFD8DC")
                for (i in 0..15) {
                    val angle = i * (360f / 16f)
                    val r = 130f
                    val x = 256f + r * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                    val y = 256f + r * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                    canvas.drawCircle(x, y, 10f, paint)
                }

                // Title Label
                paint.color = Color.BLACK
                paint.textSize = 32f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("CHICKEN BREAST & RICE", 256f, 420f, paint)
            }
            "yogurt" -> {
                // Bowl
                paint.color = Color.parseColor("#00796B")
                paint.style = Paint.Style.FILL
                val bowlRect = RectF(100f, 150f, 412f, 380f)
                canvas.drawArc(bowlRect, 0f, 180f, true, paint)

                // Yogurt layer (white)
                paint.color = Color.parseColor("#ECEFF1")
                canvas.drawRect(110f, 240f, 402f, 265f, paint)

                // Berries (red/blue circles)
                paint.color = Color.RED
                canvas.drawCircle(200f, 220f, 18f, paint)
                canvas.drawCircle(220f, 230f, 15f, paint)
                paint.color = Color.BLUE
                canvas.drawCircle(280f, 225f, 16f, paint)
                canvas.drawCircle(300f, 218f, 14f, paint)

                // Label
                paint.color = Color.parseColor("#004D40")
                paint.textSize = 32f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("GREEK YOGURT BOWL", 256f, 440f, paint)
            }
            else -> { // steak
                // Plate
                paint.color = Color.parseColor("#90A4AE")
                paint.style = Paint.Style.FILL
                val plateRect = RectF(80f, 120f, 432f, 392f)
                canvas.drawOval(plateRect, paint)

                // Steak (juicy brown)
                paint.color = Color.parseColor("#3E2723")
                val steakRect = RectF(120f, 160f, 392f, 340f)
                canvas.drawRoundRect(steakRect, 30f, 30f, paint)

                // Grill Marks
                paint.color = Color.BLACK
                paint.strokeWidth = 6f
                for (i in 0..4) {
                    val offset = i * 40f
                    canvas.drawLine(150f + offset, 180f, 220f + offset, 320f, paint)
                }

                // Label
                paint.color = Color.WHITE
                paint.textSize = 34f
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("RIBEYE STEAK", 256f, 440f, paint)
            }
        }

        return bitmap
    }
}
