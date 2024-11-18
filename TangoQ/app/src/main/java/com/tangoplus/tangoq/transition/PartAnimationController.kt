package com.tangoplus.tangoq.transition

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R

class PartAnimationController(
    private val recyclerView: RecyclerView,
    private val itemCount: Int,
    private val animationDuration: Long = 1000
) {
    private var currentIndex = 0
    private var isAnimating = false
    private var currentAnimation: ValueAnimator? = null
    private val endColor = "#FFE7E4"

    fun startSequentialAnimation() {
        if (isAnimating) return
        isAnimating = true
        currentIndex = 0
        animateNextItem()
    }

    private fun animateNextItem() {
        if (currentIndex >= itemCount) {
            currentIndex = 0
            return
        }

        recyclerView.findViewHolderForAdapterPosition(currentIndex)?.let { holder ->
            val clPI = holder.itemView.findViewById<ConstraintLayout>(R.id.clPI)

            currentAnimation?.cancel()

            // 첫 번째 애니메이션 (deleteContainerColor -> FFFFFF)
            val fadeOutAnimation = ValueAnimator.ofObject(
                ArgbEvaluator(),
                ContextCompat.getColor(recyclerView.context, R.color.deleteContainerColor),
                Color.parseColor(endColor)
            ).apply {
                duration = animationDuration / 2  // 전체 시간의 절반

                addUpdateListener { animator ->
                    val color = animator.animatedValue as Int
                    clPI.backgroundTintList = ColorStateList.valueOf(color)
                }

                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // 두 번째 애니메이션 시작 (FFFFFF -> deleteContainerColor)
                        val fadeInAnimation = ValueAnimator.ofObject(
                            ArgbEvaluator(),
                            Color.parseColor(endColor),
                            ContextCompat.getColor(recyclerView.context, R.color.deleteContainerColor)
                        ).apply {
                            duration = animationDuration / 2  // 전체 시간의 절반

                            addUpdateListener { animator ->
                                val color = animator.animatedValue as Int
                                clPI.backgroundTintList = ColorStateList.valueOf(color)
                            }

                            addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    currentIndex++
                                    if (currentIndex < itemCount) {
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            animateNextItem()
                                        }, 100)
                                    } else {
                                        currentIndex = 0
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            animateNextItem()
                                        }, 100)
                                    }
                                }
                            })
                        }
                        fadeInAnimation.start()
                    }
                })
            }
            fadeOutAnimation.start()
        } ?: run {
            Log.e("Animation", "ViewHolder not found for position $currentIndex")
        }
    }

    fun stopAnimation() {
        isAnimating = false
        currentAnimation?.cancel()
        currentAnimation = null
        currentIndex = 0

        // 모든 아이템의 색상을 원래대로 복구
        for (i in 0 until itemCount) {
            recyclerView.findViewHolderForAdapterPosition(i)?.let { holder ->
                holder.itemView.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(recyclerView.context, R.color.mainColor)
                )
            }
        }
    }
}