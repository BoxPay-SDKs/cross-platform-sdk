package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.domain.model.ReceiptData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSMutableData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.writeToURL
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginPDFContextToData
import platform.UIKit.UIGraphicsBeginPDFPage
import platform.UIKit.UIGraphicsEndPDFContext
import platform.UIKit.UIImage
import platform.UIKit.drawAtPoint
import platform.UIKit.sizeWithAttributes

@OptIn(ExperimentalForeignApi::class)
internal actual object PdfReceiptGenerator {

    actual fun generateAndDownload(
        receiptData: ReceiptData,
        context : Any?,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val pdfData = NSMutableData()

        // A4 page
        UIGraphicsBeginPDFContextToData(pdfData, CGRectMake(0.0, 0.0, 595.0, 842.0), null)
        UIGraphicsBeginPDFPage()

        drawReceiptPage(receiptData)

        UIGraphicsEndPDFContext()

        // Write to temp directory
        val fileName = "receipt_${receiptData.transactionId}.pdf"
        val filePath = "${NSTemporaryDirectory()}$fileName"
        val fileURL  = NSURL.fileURLWithPath(filePath)

        if (!pdfData.writeToURL(fileURL, atomically = true)) {
            onResult(false, "Failed to write receipt PDF to temp directory.")
            return
        }

        // Present share/save sheet
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootVC == null) {
            onResult(false, "Could not obtain root view controller.")
            return
        }

        val activityVC = UIActivityViewController(
            activityItems = listOf(fileURL),
            applicationActivities = null,
        )
        rootVC.presentViewController(activityVC, animated = true, completion = null)
        onResult(true, "Receipt ready to save or share.")
    }

    // -------------------------------------------------------------------------
    // Drawing helpers (uses UIKit string-drawing extensions available in K/N)
    // -------------------------------------------------------------------------

    private val green  = UIColor(red = 0.004, green = 0.600, blue = 0.224, alpha = 1.0)
    private val navy   = UIColor(red = 0.102, green = 0.137, blue = 0.494, alpha = 1.0)
    private val margin = 50.0

    private fun drawReceiptPage(data: ReceiptData) {
        val titleAttrs: Map<Any?, *> = mapOf(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(22.0),
            NSForegroundColorAttributeName to green,
        )
        val labelAttrs: Map<Any?, *> = mapOf(
            NSFontAttributeName to UIFont.systemFontOfSize(14.0),
            NSForegroundColorAttributeName to UIColor.darkGrayColor,
        )
        val valueAttrs: Map<Any?, *> = mapOf(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(14.0),
            NSForegroundColorAttributeName to UIColor.blackColor,
        )
        val totalAttrs: Map<Any?, *> = mapOf(
            NSFontAttributeName to UIFont.boldSystemFontOfSize(16.0),
            NSForegroundColorAttributeName to UIColor.blackColor,
        )

        var y = 60.0
        val valueX = 300.0

        // Title
        drawText("Payment Receipt", x = margin, y = y, attrs = titleAttrs)
        y += 36.0

        // Info rows
        val rows = listOf(
            "Transaction ID" to data.transactionId,
            "Date"           to data.date,
            "Time"           to data.time,
            "Payment Method" to data.paymentMethod,
        )
        for ((label, value) in rows) {
            drawText(label, x = margin, y = y, attrs = labelAttrs)
            drawText(value, x = valueX, y = y, attrs = valueAttrs)
            y += 34.0
        }

        y += 12.0

        // Total
        drawText("Total", x = margin, y = y, attrs = totalAttrs)
        drawText("${data.currencyCode} ${data.formattedAmount}", x = valueX, y = y, attrs = totalAttrs)

        // ── "Secured by BoxPay" footer ─────────────────────────────────────────
        drawBoxPayFooter()
    }

    /**
     * Draws "Secured by [splash-icon] BoxPay" centred at the bottom of the page.
     * The icon is loaded from the app bundle as splash-icon.png.
     */
    private fun drawBoxPayFooter() {
        val pageWidth = 595.0
        val footerY   = 800.0          // top-left Y of the footer row
        val iconSize  = 100.0


        val totalW      = iconSize + 2.0
        var x           = (pageWidth - totalW) / 2.0

        // splash-icon.png from the app bundle
        val logo = UIImage.imageNamed("splash-icon")
        if (logo != null) {
            val iconRect = CGRectMake(x, footerY, iconSize, iconSize)
            logo.drawInRect(iconRect)
            x += iconSize + 2.0
        }
    }

    /**
     * Returns the rendered width of [text] with [attrs] using UIKit sizing.
     */
    private fun measureText(text: String, attrs: Map<Any?, *>): Double {
        val nsStr = text as platform.Foundation.NSString
        val size  = nsStr.sizeWithAttributes(attrs)
        return size.useContents { width }
    }

    /**
     * Draws [text] at the given point using UIKit's NSString drawing extensions.
     * The rect height is intentionally large so text is never clipped.
     */
    private fun drawText(text: String, x: Double, y: Double, attrs: Map<Any?, *>) {
        (text as platform.Foundation.NSString).drawAtPoint(
            platform.CoreGraphics.CGPointMake(x, y),
            withAttributes = attrs,
        )
    }
}