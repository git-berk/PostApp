package com.berco.postapp.presentation.list

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.berco.postapp.R
import com.berco.postapp.databinding.FragmentPostListBinding
import com.berco.postapp.domain.model.Post
import com.berco.postapp.domain.result.AppError
import com.berco.postapp.shared.ui.applyBottomInsetPadding
import com.berco.postapp.shared.ui.applyTopSystemBarPadding
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PostListFragment : Fragment(R.layout.fragment_post_list) {

    private var _binding: FragmentPostListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostsViewModel by viewModels()

    private val adapter = PostListAdapter(::openDetail)

    private var shownRefreshError: AppError? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPostListBinding.bind(view)
        binding.header.applyTopSystemBarPadding()
        setUpRecyclerView()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    private fun setUpRecyclerView() = with(binding.recyclerView) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = this@PostListFragment.adapter
        setHasFixedSize(true)

        applyBottomInsetPadding()

        addItemDecoration(
            MaterialDividerItemDecoration(
                requireContext(),
                MaterialDividerItemDecoration.VERTICAL,
            ).apply {
                setDividerColorResource(requireContext(), R.color.divider)
                setDividerThicknessResource(requireContext(), R.dimen.divider_height)
                setDividerInsetStartResource(requireContext(), R.dimen.screen_gutter)
                setDividerInsetEndResource(requireContext(), R.dimen.screen_gutter)
                isLastItemDecorated = false
            },
        )

        ItemTouchHelper(
            SwipeToDeleteCallback(requireContext(), viewModel::delete),
        ).attachToRecyclerView(this)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: PostListUiState) {
        binding.skeleton.isVisible = state is PostListUiState.Loading
        binding.recyclerView.isVisible = state is PostListUiState.Content
        binding.stateView.root.isVisible =
            state is PostListUiState.Empty || state is PostListUiState.Error

        when (state) {
            is PostListUiState.Loading -> binding.countTag.isVisible = false

            is PostListUiState.Content -> {
                binding.countTag.isVisible = true
                binding.countTag.text =
                    resources.getQuantityString(R.plurals.posts_count, state.posts.size, state.posts.size)
                adapter.submitList(state.posts)
                state.refreshError?.let(::showRefreshError)
            }

            is PostListUiState.Empty -> {
                binding.countTag.isVisible = false
                adapter.submitList(emptyList())
                renderEmptyState()
            }

            is PostListUiState.Error -> {
                binding.countTag.isVisible = false
                adapter.submitList(emptyList())
                renderErrorState(state.error)
            }
        }
    }

    private fun renderEmptyState() = with(binding.stateView) {
        stateIconHolder.setBackgroundResource(R.drawable.bg_circle_accent_2_200)
        stateIcon.setImageResource(R.drawable.ic_check_large)
        stateTitle.setText(R.string.empty_title)
        stateMessage.setText(R.string.empty_message)
        stateAction.setText(R.string.reload)
        stateAction.setOnClickListener { viewModel.refresh() }
    }

    private fun renderErrorState(error: AppError) = with(binding.stateView) {
        stateIconHolder.setBackgroundResource(R.drawable.bg_circle_accent_200)
        stateIcon.setImageResource(R.drawable.ic_alert)
        stateTitle.setText(R.string.error_title)
        stateMessage.text = error.toMessage()
        stateAction.setText(R.string.retry)
        stateAction.setOnClickListener { viewModel.refresh() }
    }

    private fun showRefreshError(error: AppError) {
        if (shownRefreshError == error) return
        shownRefreshError = error
        Snackbar.make(binding.root, error.toMessage(), Snackbar.LENGTH_LONG)
            .setAction(R.string.retry) { viewModel.refresh() }
            .show()
    }

    private fun AppError.toMessage(): String = when (this) {
        AppError.Network -> getString(R.string.error_network)
        is AppError.Server -> getString(R.string.error_server, code)
        AppError.Serialization -> getString(R.string.error_serialization)
        is AppError.Unknown -> getString(R.string.error_unknown)
    }

    private fun openDetail(post: Post) {
        findNavController().navigate(
            PostListFragmentDirections.actionListToDetail(post.id),
        )
    }
}
