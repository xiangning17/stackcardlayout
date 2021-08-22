package me.xiangning.stackcard.library

import android.graphics.PointF
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 堆叠卡片布局
 * Created by xiangning on 2021/8/21.
 */
class StackCardLayoutManager: RecyclerView.LayoutManager(),
    RecyclerView.SmoothScroller.ScrollVectorProvider {

    private val TAG = "StackCardLayoutManager"

    val SHOW_COUNT = 3
    val GAP = 30f
    val DEG = 30f

    private var recyclerView: RecyclerView? = null

    private var offset = 0

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(-1, -1)
    }

    override fun onAttachedToWindow(view: RecyclerView?) {
        super.onAttachedToWindow(view)
        recyclerView = view
        Snap().attachToRecyclerView(view)
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State?) {
        detachAndScrapAttachedViews(recycler)
        val offsetCnt = offset / width
        offset = offsetCnt * width

        val itemCnt = itemCount
        for (i in 0..SHOW_COUNT) {
            val pos = i + offsetCnt
            if (pos >= itemCnt) {
                break
            }

            val child = recycler.getViewForPosition(pos)
            addView(child, 0)
            layoutView(child)
            transformStackChildren(child, i)
        }

        if (offsetCnt > 0) {
            val child = recycler.getViewForPosition(offsetCnt - 1)
            addDisappearingView(child)
            layoutView(child)
            transformPastChildren(child, width)
        }

        Log.e(TAG, "onLayoutChildren: " + (0 until childCount).map { getChildAt(it) })

    }

    private fun layoutView(view: View) {
        measureChildWithMargins(view, 0, 0)

        val widthSpace = width - getDecoratedMeasuredWidth(view)
        val heightSpace = height - getDecoratedMeasuredHeight(view)

        layoutDecoratedWithMargins(
            view, widthSpace / 2, heightSpace / 2,
            widthSpace / 2 + getDecoratedMeasuredWidth(view),
            heightSpace / 2 + getDecoratedMeasuredHeight(view)
        )
    }

    private fun transformStackChildren(view: View, pos: Int, offset: Int = 0) {
        val mapPos = min(SHOW_COUNT - 1, pos)
        val step = if (mapPos == pos) 1f else 0f
        val fraction =  step * min(1f, 2f * offset / width)
        val scale = 0.85f - (mapPos - fraction) * 0.1f
        val transY = - (mapPos - fraction) / (SHOW_COUNT - 1f) * (height * 0.15f)
        view.scaleX = scale
        view.scaleY = scale
        view.translationY = transY
        view.translationX = 0f
    }

    private fun transformPastChildren(view: View, offset: Int = 0) {
        val fraction = min(1f, 1f * offset / width)
        val scale = 0.85f + 0.15f * fraction
        view.scaleX = scale
        view.scaleY = scale
        view.translationX = -width * fraction
        view.translationY = 0f
    }


    override fun canScrollHorizontally(): Boolean {
        return true
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State?
    ): Int {
        // 大于0正向移动，判断是否超过最后一个
        val realDx = if (dx > 0) {
            min((itemCount - 1) * width, offset + dx) - offset
        } else {
            max(0, offset + dx) - offset
        }

        val offsetCnt = offset / width
        val newOffset = offset + realDx
        val newOffsetCnt = newOffset / width

        val transform = newOffset % width
        when {
            newOffsetCnt == offsetCnt -> {
                val start = childCount - 1 - if (offsetCnt > 0) 1 else 0

                transformPastChildren(getChildAt(start)!!, transform)

                (start - 1 downTo 0).withIndex()
                    .forEach { (index, childIndex) ->
                        transformStackChildren(getChildAt(childIndex)!!, index + 1, transform)
                    }
            }
            newOffsetCnt > offsetCnt -> {
                val deltaCnt = newOffsetCnt - offsetCnt
                for (i in max(1, offsetCnt) until newOffsetCnt) {
                    detachAndScrapViewAt(childCount - 1, recycler)
                }
                repeat(deltaCnt) {
                    val position =
                        getChildAt(0)?.let { recyclerView?.getChildAdapterPosition(it) }
                            ?: RecyclerView.NO_POSITION
                    if (position != RecyclerView.NO_POSITION && position + 1 < itemCount) {
                        val child = recycler.getViewForPosition(position + 1)
                        addView(child, 0)
                        layoutView(child)
                    }
                }

                var start = childCount - 1

                transformPastChildren(getChildAt(start--)!!, width)
                transformPastChildren(getChildAt(start--)!!, transform)

                (start downTo 0).withIndex()
                    .forEach { (index, childIndex) ->
                        transformStackChildren(getChildAt(childIndex)!!, index + 1, transform)
                    }

            }
            else -> {
                val deltaCnt = offsetCnt - newOffsetCnt
                for (i in newOffsetCnt until min(itemCount - 1 - SHOW_COUNT, offsetCnt)) {
                    detachAndScrapViewAt(0, recycler)
                }
                repeat(deltaCnt) {
                    val position = getChildAt(childCount - 1)?.let { recyclerView?.getChildAdapterPosition(it) } ?: RecyclerView.NO_POSITION
                    if (position != RecyclerView.NO_POSITION && position - 1 >= 0) {
                        val child = recycler.getViewForPosition(position - 1)
                        addDisappearingView(child)
                        layoutView(child)
                    }
                }

                var start = childCount - 1
                if (newOffsetCnt > 0) {
                    transformPastChildren(getChildAt(start)!!, width)
                    start -= 1
                }

                transformPastChildren(getChildAt(start--)!!, transform)

                (start downTo 0).withIndex()
                    .forEach { (index, childIndex) ->
                        transformStackChildren(getChildAt(childIndex)!!, index + 1, transform)
                    }
            }
        }

        offset += realDx
        return dx
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF {
        return PointF(width.toFloat() * max(0, targetPosition) - offset, 0f)
    }

    private inner class Snap: PagerSnapHelper() {

        override fun findSnapView(layoutManager: RecyclerView.LayoutManager?): View? {
            if (width == 0) {
                return null
            }

            val pos = (offset / width.toFloat()).roundToInt()
            for (i in childCount - 1 downTo 0) {
                if (pos == recyclerView?.getChildAdapterPosition(getChildAt(i)!!)) {
                    return getChildAt(i)
                }
            }

            return null
        }

        override fun calculateDistanceToFinalSnap(
            layoutManager: RecyclerView.LayoutManager,
            targetView: View
        ): IntArray? {
            return recyclerView?.getChildAdapterPosition(targetView)?.let { pos ->
                intArrayOf(computeScrollVectorForPosition(pos).x.toInt(), 0)
            }
        }

        // fling时计算目标position
        override fun findTargetSnapPosition(
            layoutManager: RecyclerView.LayoutManager?,
            velocityX: Int,
            velocityY: Int
        ): Int {
            return (offset / width.toFloat()).roundToInt()
        }
    }
}