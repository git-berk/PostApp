package com.berco.postapp.presentation.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.berco.postapp.R

// Every Paint and Drawable is built here, never in onChildDraw, which runs on
// every frame of the swipe.
class SwipeToDeleteCallback(
    context: Context,
    private val onDelete: (postId: Long) -> Unit,
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.accent)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics,
        )
        typeface = ResourcesCompat.getFont(context, R.font.figtree_bold) ?: Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
    }

    private val trashIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_trash)
    private val label = context.getString(R.string.delete)
    private val edgePadding = 26f * context.resources.displayMetrics.density
    private val iconGap = 8f * context.resources.displayMetrics.density

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean = false

    // Reports the stable id rather than a position, which the caller would have to
    // resolve against a list that may already have moved on.
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        if (viewHolder.itemId != RecyclerView.NO_ID) onDelete(viewHolder.itemId)
    }

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        val view = viewHolder.itemView
        if (dX < 0) {
            canvas.drawRect(
                view.right + dX,
                view.top.toFloat(),
                view.right.toFloat(),
                view.bottom.toFloat(),
                panelPaint,
            )

            val centerY = (view.top + view.bottom) / 2f
            val labelRight = view.right - edgePadding
            val labelBaseline = centerY - (labelPaint.descent() + labelPaint.ascent()) / 2f
            canvas.drawText(label, labelRight, labelBaseline, labelPaint)

            trashIcon?.let { icon ->
                val iconRight = (labelRight - labelPaint.measureText(label) - iconGap).toInt()
                val iconTop = (centerY - icon.intrinsicHeight / 2f).toInt()
                icon.setBounds(
                    iconRight - icon.intrinsicWidth,
                    iconTop,
                    iconRight,
                    iconTop + icon.intrinsicHeight,
                )
                icon.draw(canvas)
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
