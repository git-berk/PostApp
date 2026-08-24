package com.berco.postapp.presentation.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.berco.postapp.R
import com.berco.postapp.databinding.ItemPostBinding
import com.berco.postapp.domain.model.Post

private const val SNIPPET_MAX_CHARS = 78

class PostListAdapter(
    private val onClick: (Post) -> Unit,
) : ListAdapter<Post, PostListAdapter.PostViewHolder>(PostDiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val fields = payloads.filterIsInstance<Set<*>>().flatten().filterIsInstance<PostField>()
        if (fields.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindFields(getItem(position), fields)
        }
    }

    class PostViewHolder(
        private val binding: ItemPostBinding,
        onClick: (Post) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var current: Post? = null

        init {
            binding.root.setOnClickListener { current?.let(onClick) }
        }

        fun bind(post: Post) {
            current = post
            binding.title.text = post.title
            binding.snippet.text = post.body.toSnippet()
            binding.thumbnail.load(post.thumbnailUrl()) {
                crossfade(false)
                placeholder(R.drawable.bg_skeleton_circle)
                error(R.drawable.bg_skeleton_circle)
            }
        }

        fun bindFields(post: Post, fields: List<PostField>) {
            current = post
            fields.forEach { field ->
                when (field) {
                    PostField.TITLE -> binding.title.text = post.title
                    PostField.BODY -> binding.snippet.text = post.body.toSnippet()
                }
            }
        }
    }
}

private fun String.toSnippet(): String =
    if (length > SNIPPET_MAX_CHARS) take(SNIPPET_MAX_CHARS).trimEnd() + "…" else this
