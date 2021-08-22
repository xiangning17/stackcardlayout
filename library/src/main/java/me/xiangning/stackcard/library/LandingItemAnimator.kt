package me.xiangning.stackcard.library

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * 入场下落动画
 * Created by xiangning on 2021/8/22.
 */
class LandingItemAnimator: DefaultItemAnimator() {

    class LandingHolderInfo: ItemHolderInfo() {
        var targetTransY: Float = 0f
    }

    private val addInfoList = mutableMapOf<RecyclerView.ViewHolder, LandingHolderInfo>()

    override fun obtainHolderInfo(): ItemHolderInfo {
        return LandingHolderInfo()
    }

    override fun animateAppearance(
        viewHolder: RecyclerView.ViewHolder,
        preLayoutInfo: ItemHolderInfo?,
        postLayoutInfo: ItemHolderInfo
    ): Boolean {
        if (preLayoutInfo == null) {
            viewHolder.itemView.alpha = 0f
            addInfoList[viewHolder] = postLayoutInfo as LandingHolderInfo
            return true
        }

        dispatchAnimationFinished(viewHolder)
        return false
    }

    override fun runPendingAnimations() {
        var delay = 500L
        for (holder in addInfoList.keys.sortedBy { it.adapterPosition }) {
            val itemView = holder.itemView
            val info = addInfoList[holder]!!
            info.targetTransY = itemView.translationY
            itemView.translationY = info.targetTransY - (info.bottom - info.top).toFloat()
            itemView.animate()
                .setDuration(500L)
                .setStartDelay(delay)
                .translationY(info.targetTransY)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        itemView.alpha = 1f
                    }

                    override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                        dispatchAnimationFinished(holder)
                    }
                })
                .start()
            delay += 500
        }
    }

    override fun endAnimation(item: RecyclerView.ViewHolder) {
        addInfoList.remove(item)?.let {
            resetAnim(item, it)
        } ?: super.endAnimation(item)

    }

    override fun endAnimations() {
        super.endAnimations()
        val itr = addInfoList.iterator()
        while (itr.hasNext()) {
            itr.next().let { resetAnim(it.key, it.value) }
            itr.remove()
        }
    }

    private fun resetAnim(item: RecyclerView.ViewHolder, info: LandingHolderInfo) {
        val view = item.itemView
        view.animate().cancel()
        view.alpha = 1f
        view.translationY = info.targetTransY
        dispatchAnimationFinished(item)
    }

    override fun isRunning(): Boolean {
        return super.isRunning() || addInfoList.isNotEmpty()
    }

}