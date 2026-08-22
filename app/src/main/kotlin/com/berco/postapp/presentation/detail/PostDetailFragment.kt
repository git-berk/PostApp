package com.berco.postapp.presentation.detail

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.berco.postapp.R
import com.berco.postapp.databinding.FragmentPostDetailBinding
import com.berco.postapp.domain.model.Post
import com.berco.postapp.presentation.posts.PostListUiState
import com.berco.postapp.presentation.posts.PostsViewModel
import com.berco.postapp.presentation.posts.heroUrl
import com.berco.postapp.shared.ui.applyBottomSystemBarPadding
import com.berco.postapp.shared.ui.applyTopSystemBarMargin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val SAVED_PILL_DURATION_MS = 2_200L

@AndroidEntryPoint
class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val postsViewModel: PostsViewModel by hiltNavGraphViewModels(R.id.nav_graph)
    private val detailViewModel: PostDetailViewModel by viewModels()

    // Guards against the text watcher echoing programmatic setText back into the
    // draft, which would fight the user's cursor on every state emission.
    private var isBindingDraft = false
    private var boundImageIndex: Int? = null
    private var savedPillJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        boundImageIndex = null
        binding.backButton.applyTopSystemBarMargin()
        binding.badge.applyTopSystemBarMargin()
        binding.scrollView.applyBottomSystemBarPadding()

        setUpActions()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpActions() = with(binding) {
        backButton.setOnClickListener { findNavController().popBackStack() }

        editTitleButton.setOnClickListener {
            currentPost()?.let { detailViewModel.startEditing(EditorTarget.TITLE, it.title) }
        }
        editBodyButton.setOnClickListener {
            currentPost()?.let { detailViewModel.startEditing(EditorTarget.BODY, it.body) }
        }

        saveTitleButton.setOnClickListener {
            val draft = detailViewModel.commitEdit() ?: return@setOnClickListener
            if (postsViewModel.updateTitle(detailViewModel.postId, draft)) showSavedPill()
        }
        saveBodyButton.setOnClickListener {
            val draft = detailViewModel.commitEdit() ?: return@setOnClickListener
            if (postsViewModel.updateBody(detailViewModel.postId, draft)) showSavedPill()
        }

        cancelTitleButton.setOnClickListener { detailViewModel.cancelEditing() }
        cancelBodyButton.setOnClickListener { detailViewModel.cancelEditing() }

        titleInput.doAfterTextChanged { onDraftTyped(it?.toString()) }
        bodyInput.doAfterTextChanged { onDraftTyped(it?.toString()) }
    }

    private fun onDraftTyped(value: String?) {
        if (!isBindingDraft) detailViewModel.onDraftChanged(value.orEmpty())
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    postsViewModel.post(detailViewModel.postId),
                    detailViewModel.editState,
                    ::Pair,
                ).collect { (post, editState) -> render(post, editState) }
            }
        }
    }

    private fun render(post: Post?, editState: EditUiState) {
        if (post == null) {
            if (postsViewModel.uiState.value !is PostListUiState.Loading) {
                findNavController().popBackStack()
            }
            return
        }

        with(binding) {
            title.text = post.title
            body.text = post.body
            badge.text = getString(R.string.post_badge, post.id)

            // Keyed on the image index rather than the post, so typing in a draft
            // does not re-issue an image request on every character.
            if (boundImageIndex != post.imageIndex) {
                boundImageIndex = post.imageIndex
                heroImage.load(post.heroUrl()) {
                    crossfade(false)
                    placeholder(R.drawable.post_placeholder)
                    error(R.drawable.post_placeholder)
                }
            }

            titleEditor.isVisible = editState.isEditingTitle
            editTitleButton.isVisible = !editState.isEditingTitle

            bodyEditor.isVisible = editState.isEditingBody
            editBodyButton.isVisible = !editState.isEditingBody
            body.isVisible = !editState.isEditingBody

            bindDraft(editState)
        }
    }

    private fun bindDraft(editState: EditUiState) = with(binding) {
        val input = when (editState.target) {
            EditorTarget.TITLE -> titleInput
            EditorTarget.BODY -> bodyInput
            EditorTarget.NONE -> return
        }
        if (input.text.toString() == editState.draft) return

        isBindingDraft = true
        input.setText(editState.draft)
        input.setSelection(input.text.length)
        isBindingDraft = false
    }

    private fun showSavedPill() {
        savedPillJob?.cancel()
        savedPillJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.savedPill.isVisible = true
            delay(SAVED_PILL_DURATION_MS.milliseconds)
            binding.savedPill.isVisible = false
        }
    }

    private fun currentPost(): Post? = postsViewModel.currentPost(detailViewModel.postId)
}
