package com.gitlab.mudlej.MjPdfReader.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gitlab.mudlej.MjPdfReader.R
import com.gitlab.mudlej.MjPdfReader.data.Bookmark
import com.gitlab.mudlej.MjPdfReader.data.PDF
import com.gitlab.mudlej.MjPdfReader.databinding.BookmarksListItemBinding


class BookmarksAdapter(
    private val bookmarkFunctions: BookmarkFunctions,
    val activity: BookmarksActivity
) : ListAdapter<Bookmark, BookmarksAdapter.BookmarkViewHolder>(BookmarkComparator()) {

    var shouldExpand = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        return BookmarkViewHolder(
            BookmarksListItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, i: Int) {
        getItem(i)?.let { holder.bind(it) }
    }

    // helping classes
    inner class BookmarkViewHolder(private val binding: BookmarksListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(bookmark: Bookmark) {
            binding.root.removeAllViews()
            binding.root.addView(addSubBookmarkLayout(bookmark))
        }

        private fun addSubBookmarkLayout(subBookmark: Bookmark): ConstraintLayout {

            val subBookmarkLayout = LayoutInflater.from(activity)
                .inflate(R.layout.children_bookmark_layout, null) as ConstraintLayout

            val subToggleButton = subBookmarkLayout[0] as ImageView
            val subText = subBookmarkLayout[1] as TextView
            val subPageNumber = subBookmarkLayout[2] as TextView
            val subChildrenLayout = subBookmarkLayout[3] as LinearLayoutCompat

            subText.text = subBookmark.title
            subText.textSize = PDF.BOOKMARK_TEXT_SIZE - subBookmark.level * PDF.BOOKMARK_TEXT_SIZE_DEC

            subPageNumber.text = (subBookmark.pageIdx + 1).toString()
            subPageNumber.textSize = PDF.BOOKMARK_TEXT_SIZE - subBookmark.level * PDF.BOOKMARK_TEXT_SIZE_DEC

            subText.setOnClickListener { bookmarkFunctions.onBookmarkClicked(subBookmark) }
            subPageNumber.setOnClickListener { bookmarkFunctions.onBookmarkClicked(subBookmark) }
            subBookmarkLayout.setOnClickListener { bookmarkFunctions.onBookmarkClicked(subBookmark) }

//            if (subBookmark.level != 0)
//                subBookmarkLayout.setBackgroundResource(R.drawable.transparent_background)

            if (subBookmark.hasSubBookmarks()) {
                subChildrenLayout.removeAllViews()
                for (child in subBookmark.subBookmarks) {
                    val layout = addSubBookmarkLayout(child)
                    subChildrenLayout.addView(layout)
                }

                subToggleButton.setOnClickListener {
                    if (subChildrenLayout.isVisible) {
                        subChildrenLayout.visibility = View.GONE
                        subToggleButton.setImageResource(R.drawable.ic_small_arrow_right)
                    } else {
                        subChildrenLayout.visibility = View.VISIBLE
                        subToggleButton.setImageResource(R.drawable.ic_small_arrow_down)
                    }
                }
            }
            else {
                subToggleButton.setImageResource(R.drawable.ic_bar)
            }
            return subBookmarkLayout
        }
    }

    class BookmarkComparator : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean
                = oldItem.hashCode() == newItem.hashCode()

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean
                = oldItem.level == newItem.level
                && oldItem.title == newItem.title
                && oldItem.pageIdx == newItem.pageIdx
                && oldItem.children == newItem.children
    }

    interface BookmarkFunctions {
        fun onBookmarkClicked(bookmark: Bookmark)
    }
}