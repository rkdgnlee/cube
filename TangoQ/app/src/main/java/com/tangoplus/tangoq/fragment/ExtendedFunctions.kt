package com.tangoplus.tangoq.fragment

import android.content.Context
import androidx.fragment.app.Fragment

fun Fragment.isFirstRun(key: String): Boolean {
    val sharedPref = requireActivity().getSharedPreferences("appFragmentPref", Context.MODE_PRIVATE)
    val isFirstRun = sharedPref.getBoolean(key, true)
    if (isFirstRun) {
        sharedPref.edit().putBoolean(key, false).apply()
    }
    return isFirstRun
}