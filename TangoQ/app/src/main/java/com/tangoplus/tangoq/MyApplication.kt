package com.tangoplus.tangoq

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.tangoplus.tangoq.function.PreferencesManager
import java.io.File

class MyApplication : Application() {
    lateinit var preferencesManager: PreferencesManager
    private var startedActivities  = 0
    private var isAppInBackground = false
    val fragmentStack = mutableListOf<Fragment>()
    override fun onCreate() {
        super.onCreate()
        // 전역 Context 초기화
        appContext = this
        preferencesManager = PreferencesManager(appContext)

        // activity들의 시작, 종료 갯수를 카운트해서 앱이 종료되는 시점 가져오기
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                startedActivities++
                if (isAppInBackground) {
                    isAppInBackground = false

                    // 앱이 포그라운드로 돌아왔을 때의 로직
                }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                startedActivities--
                if (startedActivities == 0) {
                    isAppInBackground = true


                    // 앱이 백그라운드로 갔을 때
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (activity.isChangingConfigurations) {
                    return // configuration change로 인한 destroy일 경우 무시 ( 다크 모드 변경 등 )
                }
                if (startedActivities == 0 && isAppInBackground) {
                    // 앱이 완전히 종료되는 시점
                    clearDir()


                }
            }
        })
    }

    fun clearFragmentStack() {
        fragmentStack.clear()
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var appContext: Context
            private set
    }


    private fun clearDir() {
        val cacheDir = cacheDir // 앱의 캐시 디렉토리 가져오기
        val interalDir = filesDir
        if (cacheDir != null && cacheDir.isDirectory) {
            deleteDir(cacheDir)
        }
        interalDir?.let {
            deleteInternalDir(it)
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) {
                    Log.e("FileDelete", "Failed to delete file: ${File(dir, child).absolutePath}")
                    return false
                }
            }
        }
        return dir?.delete() ?: false
    }

    private fun deleteInternalDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) {
                    Log.e("FileDelete", "Failed to delete file: ${File(dir, child).absolutePath}")
                    return false
                }
            }
        }
        val deleted = dir?.delete() ?: false
        if (deleted) {
            Log.d("FileDelete", "Deleted: ${dir?.absolutePath}")
        } else {
            Log.e("FileDelete", "Failed to delete: ${dir?.absolutePath}")
        }
        return deleted
    }
}