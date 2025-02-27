package com.tangoplus.tangoq.fragment

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils

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
//                Log.v("clickBadge", "badgekey : $badgeKey, clickBadge: ${sharedPref.getBoolean(badgeKey, false)}")
            }
        }
        return null
    }

    fun dialogFragmentResize(context: Context, df: DialogFragment, width: Float = 0.8f, height: Float = 0.7f) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()

            display.getSize(size)

            val window = df.dialog?.window

            val x = (size.x * width).toInt()
            val y = (size.y * height).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val window = df.dialog?.window

            val x = (rect.width() * width).toInt()
            val y = (rect.height() * height).toInt()

            window?.setLayout(x, y)
        }
    }

    fun isKorean(str: String): Boolean {
        val koreanRegex = Regex("[ㄱ-ㅎㅏ-ㅣ가-힣]+")
        return koreanRegex.containsMatchIn(str)
    }
}
