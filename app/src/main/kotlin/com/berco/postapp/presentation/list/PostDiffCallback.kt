package com.berco.postapp.presentation.list

import androidx.recyclerview.widget.DiffUtil
import com.berco.postapp.domain.model.Post

enum class PostField { TITLE, BODY }

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
        val changed = buildSet {
            if (oldItem.title != newItem.title) add(PostField.TITLE)
            if (oldItem.body != newItem.body) add(PostField.BODY)
        }
        return changed.ifEmpty { null }
    }
}
