package com.tangoplus.tangoq.fragment

import android.content.Context
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.runtime.MutableState
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

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
            Log.v("clickBadge", "badgekey : $badgeKey, clickBadge: ${sharedPref.getBoolean(badgeKey, false)}")
        }
    }
    return null
}
