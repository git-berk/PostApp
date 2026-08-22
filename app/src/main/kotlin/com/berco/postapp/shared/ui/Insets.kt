package com.berco.postapp.shared.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun View.applyTopSystemBarPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        view.updatePadding(top = initialTop + top)
        insets
    }
}

fun View.applyTopSystemBarMargin() {
    val params = layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
    val initialTop = params.topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        (view.layoutParams as android.view.ViewGroup.MarginLayoutParams).topMargin = initialTop + top
        view.requestLayout()
        insets
    }
}

fun View.applyBottomSystemBarPadding() {
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
        view.updatePadding(bottom = initialBottom + bottom)
        insets
    }
}
