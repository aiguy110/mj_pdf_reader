package com.gitlab.mudlej.MjPdfReader.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityBookmarksBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BookmarksActivity : AppCompatActivity(), BookmarksAdapter.BookmarkFunctions {
    private lateinit var binding: ActivityBookmarksBinding
    private val bookmarksAdapter = BookmarksAdapter(this, this)
    private lateinit var bookmarks: List<Bookmark>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initData()
        initUi()
    }

    private fun initData() {
        val bookmarksJson = intent.getStringExtra(PDF.pdfBookmarksKey)
        val bookmarksType = object : TypeToken<List<Bookmark>>() {}.type
        bookmarks = Gson().fromJson(bookmarksJson, bookmarksType)
    }

    private fun initUi() {
        title = getString(R.string.table_of_contents)
        bookmarksAdapter.submitList(bookmarks)
        binding.bookmarksRecyclerView.apply {
            adapter = bookmarksAdapter
            layoutManager = LinearLayoutManager(this@BookmarksActivity)
        }
    }

    companion object {
        const val TAG = "BookmarksActivity"
    }

    override fun onBookmarkClicked(bookmark: Bookmark) {
        val resultIntent = Intent()
        resultIntent.putExtra(PDF.chosenBookmarkKey, bookmark.pageIdx)
        setResult(PDF.BOOKMARK_RESULT_OK, resultIntent)
        finish()
    }
}