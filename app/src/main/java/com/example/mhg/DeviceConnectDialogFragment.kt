package com.example.mhg

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.BLEListAdapter
import com.example.mhg.Dialog.AgreementDialogFragment
import com.example.mhg.VO.BLEViewModel
import com.example.mhg.databinding.FragmentDeviceConnectDialogBinding
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception
import java.util.UUID

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


        startScanning()
        binding.btnBLERescan.setOnClickListener {
            binding.pbBLEConnect.isEnabled = true
            binding.pbBLEConnect.visibility = View.VISIBLE
            startScanning()
        }
        binding.btnBLEFinish.setOnClickListener {
            dismiss()
        }

        val adapter = BLEListAdapter {device ->
            selectedDevice = device
            connect()
            Log.w("ConnectDevice", "$device")
        }
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvBLE.layoutManager = linearLayoutManager
        binding.rvBLE.adapter = adapter

        // -----! VM에 장치 목록 중복 없이 추가 시작 !------
        viewModel.devices.observe(viewLifecycleOwner) {newDevices ->
            adapter.updateDevices(newDevices)
        }
        // -----! VM에 장치 목록 중복 없이 추가 끝 !------
    }
    private var selectedDevice : BluetoothDevice? = null
//    private val devices = mutableListOf<BluetoothDevice>()
    private lateinit var bluetooth : BluetoothManager
    private var gatt: BluetoothGatt? = null
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    var scanning = false
    val SCAN_PERIOD : Long = 7000

    private val handler = Handler()
    @SuppressLint("SuspiciousIndentation")
    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun startScanning() {
        if (!scanning) {
            handler.postDelayed({
                scanning = false
                bluetoothLeScanner.stopScan(scanCallback)
                binding.pbBLEConnect.isEnabled = false
                binding.pbBLEConnect.visibility = View.GONE
            }, SCAN_PERIOD)
            scanning = true
            bluetoothLeScanner.startScan(scanCallback)
            Log.i("스캔 시작", "ble장치 스캔이 시작됐습니다.")
            binding.pbBLEConnect.isEnabled = true
            binding.pbBLEConnect.visibility = View.VISIBLE
        } else {
            scanning = false
            bluetoothLeScanner.stopScan(scanCallback)
            binding.pbBLEConnect.isEnabled = false
            binding.pbBLEConnect.visibility = View.GONE
        }
    }

    // 장치 찾기 callback 함수
    private val scanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            Log.i("스캔결과", "Remote device name: " + result!!.device.name)
            Log.v("스캔결과","${result}")
            if (result.device != null) {
                binding.pbBLEConnect.isEnabled = false
                binding.pbBLEConnect.visibility =  View.GONE
                viewModel.addDevice(result.device)
                activity?.runOnUiThread {
//                    (binding.rvBLE.adapter as? BLEListAdapter)?.addDevice(result.device)
                    (binding.rvBLE.adapter as? BLEListAdapter)?.notifyDataSetChanged()
                }
            }
        }
//        override fun onBatchScanResults(results: List<ScanResult>) {
//            for (result in results) {
//                activity?.runOnUiThread {
//                    (binding.rvBLE.adapter as? BLEListAdapter)?.addDevice(result.device)
//                }
//            }
//        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.w("BLEScan", "BLE Scan Failed")
        }
    }
    // 블루투스 스캔 Context에 붙이기
    override fun onStart() {
        super.onStart()
        bluetooth = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.dialog_16))
        requireContext().dialogFragmentResize(0.9f, 0.8f)
    }

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun connect() {
        viewModel.gatt.value = selectedDevice?.connectGatt(requireContext(), true, GattCallback())
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
}