package com.gitlab.mudlej.MjPdfReader.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import androidx.core.view.children
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityMainBinding

class FullScreenOptionsManager(
    private val binding: ActivityMainBinding,
    private val pdf: PDF,
    private val delay: Long
) {
    private val delayHandler = Handler(Looper.getMainLooper())
    private val buttonsList: List<View> = listOf(
        binding.exitFullScreenButton,
        binding.rotateScreenButton,
        binding.brightnessButtonLayout,
        binding.autoScrollLayout,
        binding.screenshotButton,
        binding.toggleHorizontalSwipeButton
    )
    private var currentState: VisibilityState = VisibilityState.INVISIBLE

    init {
        setOnTouchListenerForAll()
    }

    fun isVisible() = currentState == VisibilityState.VISIBLE

    fun showAll() {
        if (pdf.isFullScreenToggled) {
            showFullScreenButtons()
        }
        showPageHandle()
        currentState = VisibilityState.VISIBLE
    }

    fun hideAll() {
        hideFullScreenButtons()
        hidePageHandle()
        currentState = VisibilityState.INVISIBLE
    }

    fun toggleAll() {
        if (isVisible()) hideAll() else showAll()
    }

    fun showAllDelayed() {
        delayAction(::showAll)
    }

    fun hideAllDelayed() {
        delayAction(::hideAll)
    }

    fun toggleAllDelayed() {
        delayAction(::toggleAll)
    }

    fun showAllTemporarily() {
        doTemporarily(::showAll, ::hideAll)
    }

    fun hideAllTemporarily() {
        doTemporarily(::hideAll, ::showAll)
    }

    fun toggleAllTemporarily(): Boolean {
        doTemporarily(::toggleAll, ::toggleAll)
        return true
    }

    fun permanentlyHidePageHandle() {
        binding.pdfView.scrollHandle?.permanentHide()
    }

    // -------------
    private fun delayAction(action: Runnable) {
        delayHandler.reset()
        delayHandler.postDelayed(action, delay)
    }

    private fun doTemporarily(action: Runnable, undoAction: Runnable) {
        delayHandler.reset()
        action.run()
        delayHandler.postDelayed(undoAction, delay)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun getOnTouchListener(): View.OnTouchListener {
        val isEventFullyConsumed = false    // false so clickOnListener will be triggered
        return View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> delayHandler.reset()
                MotionEvent.ACTION_UP -> hideAllDelayed()
            }
            isEventFullyConsumed
        }
    }

    private fun showFullScreenButtons() = changeFullScreenButtonsVisibility(true)

    private fun hideFullScreenButtons() = changeFullScreenButtonsVisibility(false)

    private fun changeFullScreenButtonsVisibility(isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        buttonsList.forEach { it.visibility = visibility }
    }

    private fun showPageHandle() {
        binding.pdfView.scrollHandle?.customShow()
    }

    private fun hidePageHandle() {
        binding.pdfView.scrollHandle?.customHide()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListenerForAll() {
        buttonsList.forEach { it.setOnTouchListener(getOnTouchListener()) }
        binding.brightnessButtonLayout.children.forEach { it.setOnTouchListener(getOnTouchListener()) }
        binding.autoScrollLayout.children.forEach { it.setOnTouchListener(getOnTouchListener()) }
    }

    private fun Handler.reset() {
        this.removeCallbacksAndMessages(null)
    }

}

enum class VisibilityState { VISIBLE, INVISIBLE, NONE }
