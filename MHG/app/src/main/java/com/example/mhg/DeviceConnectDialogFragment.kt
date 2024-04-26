package com.example.mhg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.BLEViewModel
import com.example.mhg.databinding.FragmentDeviceConnectDialogBinding


class DeviceConnectDialogFragment : DialogFragment() {
    lateinit var binding : FragmentDeviceConnectDialogBinding
    private val viewModel: BLEViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDeviceConnectDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        val gattServiceIntent = Intent(requireContext(), BluetoothLeService::class.java)
//        requireContext().startService(gattServiceIntent)
//        requireContext().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
//        startScanning()


    }




//    private fun makeGattUpdateIntentFilter(): IntentFilter {
//        return IntentFilter().apply {
//            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
//            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
//        }
//    }
    // -------------!! scanner !!--------------




    // 블루투스 스캔 Context에 붙이기
//    override fun onStart() {
//        super.onStart()
//        bluetooth = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
//            ?: throw Exception("Bluetooth is not supported by this device")
//    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.dialog_16))
        requireContext().dialogFragmentResize(0.9f, 0.8f)
    }

    private fun Context.dialogFragmentResize(width: Float, height: Float) {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            val window = dialog?.window
            val x = (size.x * width).toInt()
            val y = (size.y * height).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds
            val window = dialog?.window
            val x = (rect.width() * width).toInt()
            val y = (rect.height() * height).toInt()

            window?.setLayout(x, y)
        }
    }
    companion object {
        const val LIST_NAME = "LIST_NAME"
        const val LIST_UUID = "LIST_UUID"
    }

    override fun onDestroy() {
        super.onDestroy()

    }
}