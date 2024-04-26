package com.example.mhg.`object`

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission


object TedPermissionWrapper {
    val TAG = TedPermissionWrapper::class.java.getSimpleName()
    @SuppressLint("StaticFieldLeak")
    const val AL_TITLE = "퍼미션 권한 알림"
    const val AL_NPA = "권한 허가를 동의 하지 않으셨습니다.\n" +
            "일부 기능 사용에 제한이 있을 수 있습니다.\n" +
            "[설정] > [권한]에서 거부한 권한을 활성화 해주세요"
    const val AL_NQA = "안전한 앱 사용을 위해 보안 상태 확인 권한 동의를 해주세요."
    const val AL_OK = "확인"
    const val AL_SET = "설정"
    const val AL_NO = "취소"
    var mContext: Context? = null
    @SuppressLint("StaticFieldLeak")
    fun checkPermission(context: Context?) {
        mContext = context
        try {
            Log.i(TAG, "checkPermission()===============================")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) // Target 31
            {
                TedPermission.create()
                    .setPermissionListener(permissionListener)
                    .setPermissions(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                    .check()
            } else  // Taget 31 미만
            {
                TedPermission.create()
                    .setPermissionListener(permissionListener)
                    .setPermissions(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    .check()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val permissionListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            Log.e(TAG, "==========onPermissionGranted==========")
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {
            Log.e(TAG, "==========onPermissionDenied==========")
        }
    }
}

