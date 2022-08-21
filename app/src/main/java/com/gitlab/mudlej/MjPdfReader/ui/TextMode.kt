package com.gitlab.mudlej.MjPdfReader.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.data.Preferences
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityTextModeBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TextMode : AppCompatActivity() {
    private val TAG = "TextMode"
    private lateinit var binding: ActivityTextModeBinding

    private lateinit var pdfText: Map<Int, String>
    private var pageNum = 0
    private var pdfLength = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTextModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // convert pdf from Json to a Kotlin object using Gson
        // val pdfType = object : TypeToken<PDF>() {}.type
        // pdf = Gson().fromJson(intent.getStringExtra("pdfJson"), pdfType)

        // convert PDF::PagesText Map from Json to a Kotlin object using Gson
        val pdfTextType = object : TypeToken<Map<Int, String>>() {}.type
        pdfText = Gson().fromJson(intent.getStringExtra(Preferences.pdfTextKey), pdfTextType)

        pdfLength = intent.getIntExtra(Preferences.pdfLengthKey, Preferences.pdfLengthDefault)

        // setPdfPagesMode()
        setContinuousMode()
    }

    private fun setContinuousMode() {
        binding.apply {
            buttonsLayout.visibility = View.GONE
            pageTextView.text = pdfText.values
                .reduce { acc, s -> "$acc $s" }
                .replace("\n+", "\n")   // replace one or more newline
        }
    }

    private fun setPdfPagesMode() {
        binding.apply {
            buttonsLayout.visibility = View.VISIBLE
            pageTextView.text = textOrEmpty(pageNum)
            updatePageCounterView()
            prevButton.setOnClickListener {
                if (pageNum > 0) {
                    pageTextView.text = textOrEmpty(--pageNum)
                    updatePageCounterView()
                }
            }
            nextButton.setOnClickListener {
                if (pageNum < pdfLength) {
                    pageTextView.text = textOrEmpty(++pageNum)
                    updatePageCounterView()
                }
            }
        }
    }

    private fun updatePageCounterView() {
        binding.pageCounter.text = "Page: ${pageNum + 1}/${pdfLength}"
    }

    // if null or empty return Empty page, otherwise return it as it is
    private fun textOrEmpty(i: Int): String
        = (pdfText[i] ?: "").ifEmpty { "Empty page" }
}