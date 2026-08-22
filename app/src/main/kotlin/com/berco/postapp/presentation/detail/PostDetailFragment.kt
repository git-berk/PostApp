package com.berco.postapp.presentation.detail

import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.berco.postapp.R
import com.berco.postapp.databinding.FragmentPostDetailBinding
import com.berco.postapp.domain.model.Post
import com.berco.postapp.presentation.posts.heroUrl
import com.berco.postapp.shared.ui.applyBottomInsetPadding
import com.berco.postapp.shared.ui.applyTopSystemBarMargin
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val SAVED_PILL_DURATION_MS = 2_200L

@AndroidEntryPoint
class PostDetailFragment : Fragment(R.layout.fragment_post_detail) {

    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostDetailViewModel by viewModels()
    private var isBindingDraft = false
    private var boundImageIndex: Int? = null
    private var currentPost: Post? = null
    private var savedPillJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostDetailBinding.bind(view)

        boundImageIndex = null
        binding.backButton.applyTopSystemBarMargin()
        binding.badge.applyTopSystemBarMargin()
        binding.scrollView.applyBottomInsetPadding()

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
            currentPost?.let { viewModel.startEditing(EditorTarget.TITLE, it.title) }
        }
        editBodyButton.setOnClickListener {
            currentPost?.let { viewModel.startEditing(EditorTarget.BODY, it.body) }
        }
        val save = View.OnClickListener {
            if (viewModel.commitEdit()) showSavedPill()
        }
        saveTitleButton.setOnClickListener(save)
        saveBodyButton.setOnClickListener(save)

        cancelTitleButton.setOnClickListener { viewModel.cancelEditing() }
        cancelBodyButton.setOnClickListener { viewModel.cancelEditing() }

        titleInput.doAfterTextChanged { onDraftTyped(it?.toString()) }
        bodyInput.doAfterTextChanged { onDraftTyped(it?.toString()) }
    }

    private fun onDraftTyped(value: String?) {
        if (!isBindingDraft) viewModel.onDraftChanged(value.orEmpty())
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: PostDetailUiState) {
        when (state) {
            PostDetailUiState.Loading -> Unit
            PostDetailUiState.Deleted -> findNavController().popBackStack()
            is PostDetailUiState.Content -> renderContent(state.post, state.edit)
        }
    }

    private fun renderContent(post: Post, editState: EditUiState) = with(binding) {
        currentPost = post
        title.text = post.title
        body.text = post.body
        badge.text = getString(R.string.post_badge, post.id)

        // Keyed on the image index rather than the post, so typing in a draft does
        // not re-issue an image request on every character.
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
        if (editState.target == EditorTarget.NONE) hideKeyboard()
    }

    private fun hideKeyboard() {
        val view = view ?: return
        WindowCompat.getInsetsController(requireActivity().window, view)
            .hide(WindowInsetsCompat.Type.ime())
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
}
