package com.gitlab.mudlej.MjPdfReader.ui.reference

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.llm.ReferenceItem
import com.gitlab.mudlej.MjPdfReader.manager.extractor.PdfExtractor

object ReferencePopup {

    private const val RENDER_WIDTH = 1024
    private const val CROP_PADDING_PX = 12

    fun show(context: Context, anchor: View, extractor: PdfExtractor, item: ReferenceItem, anchorX: Float, anchorY: Float) {
        val pageNumber = item.pageNumber + 1 // PdfExtractor page-taking methods are 1-indexed
        val pageBitmap = extractor.renderPageBitmap(pageNumber, RENDER_WIDTH) ?: return
        val cropped = cropToBoundingBox(pageBitmap, item) ?: return

        val view = LayoutInflater.from(context).inflate(R.layout.popup_reference_view, null)
        view.findViewById<ImageView>(R.id.referencePopupImage).setImageBitmap(cropped)

        val popupWindow = PopupWindow(
            view,
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupHeight = view.measuredHeight
        val showY = if (anchorY - popupHeight > 0) anchorY - popupHeight else anchorY

        popupWindow.showAtLocation(anchor, android.view.Gravity.NO_GRAVITY, anchorX.toInt(), showY.toInt())
    }

    private fun cropToBoundingBox(pageBitmap: Bitmap, item: ReferenceItem): Bitmap? {
        val width = pageBitmap.width
        val height = pageBitmap.height
        val left = (item.bbox.left * width - CROP_PADDING_PX).toInt().coerceIn(0, width - 1)
        val top = (item.bbox.top * height - CROP_PADDING_PX).toInt().coerceIn(0, height - 1)
        val right = (item.bbox.right * width + CROP_PADDING_PX).toInt().coerceIn(left + 1, width)
        val bottom = (item.bbox.bottom * height + CROP_PADDING_PX).toInt().coerceIn(top + 1, height)
        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth <= 0 || cropHeight <= 0) return null
        return Bitmap.createBitmap(pageBitmap, left, top, cropWidth, cropHeight)
    }
}
