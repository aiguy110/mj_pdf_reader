package com.gitlab.mudlej.MjPdfReader.ui.bookmark

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.ActivityBookmarksBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BookmarksActivity : AppCompatActivity(), BookmarkFunctions {
    private lateinit var binding: ActivityBookmarksBinding
    private val bookmarkAdapter = BookmarkAdapter(this, this)
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
        if (bookmarks.isNotEmpty()) {
            binding.noTableOfContentsText.visibility = View.GONE
        }
    }

    private fun initUi() {
        title = getString(R.string.table_of_contents)
        bookmarkAdapter.submitList(bookmarks)
        binding.bookmarksRecyclerView.apply {
            adapter = bookmarkAdapter
            layoutManager = LinearLayoutManager(this@BookmarksActivity)
        }
    }

    companion object {
        const val TAG = "BookmarksActivity"
    }

    override fun onBookmarkClicked(bookmark: Bookmark) {
        val resultIntent = Intent()
        resultIntent.putExtra(PDF.chosenBookmarkKey, bookmark.pageIdx.toInt())
        setResult(PDF.BOOKMARK_RESULT_OK, resultIntent)
        finish()
    }
}