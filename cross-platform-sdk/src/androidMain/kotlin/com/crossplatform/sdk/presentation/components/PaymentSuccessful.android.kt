package com.crossplatform.sdk.presentation.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.crossplatform.sdk.domain.model.ReceiptData
import java.io.OutputStream
import androidx.core.graphics.scale

private const val PAGE_WIDTH  = 595f
private const val PAGE_HEIGHT = 842f
private const val MARGIN      = 50f

internal actual object PdfReceiptGenerator {

    actual fun generateAndDownload(
        receiptData: ReceiptData,
        context : Any?,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val ctx = context as? Context ?: run {
            onResult(false, "Context unavailable — cannot generate PDF.")
            return
        }
        runCatching {
            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1,
            ).create()
            val page = pdf.startPage(pageInfo)
            drawReceiptPage(page.canvas, receiptData, ctx)
            pdf.finishPage(page)

            val fileName = "receipt_${receiptData.transactionId}.pdf"
            val out: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                ctx.contentResolver
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                    ?.let { ctx.contentResolver.openOutputStream(it) }
            } else {
                @Suppress("DEPRECATION")
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                java.io.FileOutputStream(java.io.File(dir, fileName))
            }

            if (out != null) {
                out.use { pdf.writeTo(it) }
                pdf.close()
                onResult(true, "Receipt saved to Downloads/$fileName")
            } else {
                pdf.close()
                onResult(false, "Could not open output stream — check storage permissions.")
            }
        }.onFailure { e ->
            onResult(false, e.message ?: "Unknown error generating PDF")
        }
    }

    // -------------------------------------------------------------------------
    // Drawing helpers
    // -------------------------------------------------------------------------

    private fun drawReceiptPage(canvas: Canvas, data: ReceiptData, ctx: Context? = null) {
        val green    = Color.parseColor("#019939")
        val black    = Color.BLACK
        val darkGray = Color.DKGRAY
        val ltGray   = Color.LTGRAY

        val titlePaint      = paint(green, 22f, bold = true)
        val labelPaint      = paint(darkGray, 14f)
        val valuePaint      = paint(black, 14f, bold = true)
        val dividerPaint    = Paint().apply {
            color       = ltGray
            strokeWidth = 1f
            isAntiAlias = true
        }
        val totalLabelPaint = paint(black, 16f, bold = true)
        val totalValuePaint = paint(black, 16f, bold = true)

        val right = PAGE_WIDTH - MARGIN
        var y = 70f

        canvas.drawText("Payment Receipt", MARGIN, y, titlePaint)
        y += 10f

        divider(canvas, y, dividerPaint)
        y += 28f

        val rows = listOf(
            "Transaction ID" to data.transactionId,
            "Date"           to data.date,
            "Time"           to data.time,
            "Payment Method" to data.paymentMethod,
        )
        for ((label, value) in rows) {
            canvas.drawText(label, MARGIN, y, labelPaint)
            canvas.drawText(value, right - valuePaint.measureText(value), y, valuePaint)
            y += 34f
        }

        y += 6f
        divider(canvas, y, dividerPaint)
        y += 30f

        canvas.drawText("Total", MARGIN, y, totalLabelPaint)
        val totalText = "${data.currencyCode} ${data.formattedAmount}"
        canvas.drawText(totalText, right - totalValuePaint.measureText(totalText), y, totalValuePaint)
        y += 12f

        divider(canvas, y, dividerPaint)

        // ── "Secured by BoxPay" footer ─────────────────────────────────────────
        val logo = ctx?.let { loadSplashIcon(it) }
        drawBoxPayFooter(canvas, logo)
    }

    /**
     * Tries several asset paths that Compose Multiplatform uses across versions
     * to locate splash_icon.png, logging each attempt so you can check Logcat
     * (filter tag: BoxPayPDF) to see exactly which path works in your build.
     */
    private fun loadSplashIcon(ctx: Context): Bitmap? {
        val candidates = listOf(
            "composeResources/crossplatformsdk.cross_platform_sdk.generated.resources/drawable/splash-icon.png",
        )
        for (path in candidates) {
            runCatching {
                val bmp = ctx.assets.open(path).use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    return bmp
                }
            }.onFailure {

            }
        }
        return null
    }


    private fun drawBoxPayFooter(canvas: Canvas, logo: Bitmap?) {
        val footerY   = PAGE_HEIGHT - 44f

        val iconSize = 100f
        val iconGap  = 2f

        val logoW    = if (logo != null) iconSize + iconGap else 0f

        var x = (PAGE_WIDTH - logoW) / 2f

        if (logo != null) {
            val scaled = logo.scale(iconSize.toInt(), iconSize.toInt())
            val top    = footerY - iconSize + 2f
            canvas.drawBitmap(scaled, x, top, Paint().apply { isAntiAlias = true })
            x += iconSize + iconGap
        }
    }

    private fun divider(canvas: Canvas, y: Float, paint: Paint) =
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint().apply {
        this.color       = color
        this.textSize    = size
        this.typeface    = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        this.isAntiAlias = true
    }
}