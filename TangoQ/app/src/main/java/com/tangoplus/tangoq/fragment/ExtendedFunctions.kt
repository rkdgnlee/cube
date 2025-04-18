package com.tangoplus.tangoq.fragment

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.tangoplus.tangoq.listener.OnSingleClickListener

object ExtendedFunctions {

    fun Fragment.isFirstRun(key: String): Boolean {
        val sharedPref = requireActivity().getSharedPreferences("appFragmentPref", Context.MODE_PRIVATE)
        val isFirstRun = sharedPref.getBoolean(key, true)
        if (isFirstRun) {
            sharedPref.edit().putBoolean(key, false).apply()
        }
        return isFirstRun
    }

    @OptIn(ExperimentalBadgeUtils::class)
    fun Fragment.hideBadgeOnClick(badgeView: View,
                                  containerView: ConstraintLayout,
                                  badgeKey: String,
                                  backgroundColor: Int,
                                  horizontalOffset: Int = 16,
                                  verticalOffset: Int = 12
    ) : (() -> Unit)? {
        val sharedPref = requireActivity().getSharedPreferences("badgePref", Context.MODE_PRIVATE)
        val isBadgeVisible = sharedPref.getBoolean(badgeKey, true)

        if (isBadgeVisible) {
            val badgeDrawable = BadgeDrawable.create(requireContext()).apply {
                badgeGravity = BadgeDrawable.TOP_START
                this.backgroundColor = backgroundColor
                this.horizontalOffset = horizontalOffset  // 원하는 가로 간격 (픽셀 단위)
                this.verticalOffset =verticalOffset  // 원하는 세로 간격 (픽셀 단위)
            }
            containerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                BadgeUtils.attachBadgeDrawable(badgeDrawable, badgeView)
            }
            return {
                BadgeUtils.detachBadgeDrawable(badgeDrawable, badgeView)
                sharedPref.edit().putBoolean(badgeKey, false).apply()
            }
        }
        return null
    }

    fun isKorean(str: String): Boolean {
        val koreanRegex = Regex("[ㄱ-ㅎㅏ-ㅣ가-힣]+")
        return koreanRegex.containsMatchIn(str)
    }

    fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    fun fadeInView(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            duration = 500 // 애니메이션 지속 시간 (ms)
            interpolator = DecelerateInterpolator()
            start()
        }
    }
    fun scrollToView(view: View, nsv: NestedScrollView) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val viewTop = location[1]
        val scrollViewLocation = IntArray(2)

        nsv.getLocationInWindow(scrollViewLocation)
        val scrollViewTop = scrollViewLocation[1]
        val scrollY = nsv.scrollY
        val scrollTo = scrollY + viewTop - scrollViewTop
        nsv.smoothScrollTo(0, scrollTo)
    }
}
