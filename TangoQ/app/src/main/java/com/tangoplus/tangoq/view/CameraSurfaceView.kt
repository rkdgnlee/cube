package com.tangoplus.tangoq.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.hardware.Camera
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.ByteArrayOutputStream
import java.io.IOException

class CameraSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback {
    private val TAG = "CameraSurfaceView"

    private var mSurfaceHolder: SurfaceHolder
    private var mCamera: Camera? = null
    private var mBitmap: Bitmap? = null
    private var mContext: Context
    private var mParameters: Camera.Parameters? = null
    private var byteArray: ByteArray? = null
    private var mSupportedPreviewSizes: List<Camera.Size>? = null
    private var mPreviewSize: Camera.Size? = null

    init {
        mContext = context
        mSurfaceHolder = holder
        mSurfaceHolder.addCallback(this)
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        if (mCamera == null) {
            try {
                mCamera = Camera.open()
            } catch (ignored: RuntimeException) {
            }
        }

        try {
            if (mCamera != null) {
                mCamera!!.setPreviewDisplay(mSurfaceHolder)
            }
        } catch (e: Exception) {
            if (mCamera != null)
                mCamera!!.release()
            mCamera = null
        }
    }

    override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int, height: Int) {
        try {
            mParameters = mCamera!!.parameters

            val cameraSize = mParameters!!.supportedPreviewSizes
            mPreviewSize = cameraSize[0]

            for (s in cameraSize) {
                if (s.width * s.height > mPreviewSize!!.width * mPreviewSize!!.height) {
                    mPreviewSize = s
                }
            }

            mParameters!!.setPreviewSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mCamera!!.parameters = mParameters
            mCamera!!.startPreview()

            mCamera!!.setPreviewCallback { bytes, camera ->
                byteArray = bytes // 프리뷰 프레임을 캡처하기 위해 byteArray에 데이터를 할당합니다.
            }
        } catch (e: Exception) {
            if (mCamera != null) {
                mCamera!!.release()
                mCamera = null
            }
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        if (mCamera != null) {
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }

    fun getBitmap(): Bitmap? {
        try {
            if (mParameters == null)
                return null

            if (mPreviewSize == null || byteArray == null)
                return null

            val format = mParameters!!.previewFormat
            val yuvImage = android.graphics.YuvImage(byteArray, format, mPreviewSize!!.width, mPreviewSize!!.height, null)
            val byteArrayOutputStream = ByteArrayOutputStream()

            val rect = Rect(0, 0, mPreviewSize!!.width, mPreviewSize!!.height)

            yuvImage.compressToJpeg(rect, 75, byteArrayOutputStream)
            val options = BitmapFactory.Options()
            options.inPurgeable = true
            options.inInputShareable = true
            mBitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size(), options)

            byteArrayOutputStream.flush()
            byteArrayOutputStream.close()
        } catch (ioe: IOException) {
            ioe.printStackTrace()
        }

        return mBitmap
    }

    fun getCamera(): Camera? {
        return mCamera
    }
}
