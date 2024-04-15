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
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.registerReceiver
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.BLEListAdapter
import com.example.mhg.Dialog.AgreementDialogFragment
import com.example.mhg.VO.BLEViewModel
import com.example.mhg.databinding.FragmentDeviceConnectDialogBinding
import com.example.mhg.service.BluetoothLeService
import kotlinx.coroutines.flow.callbackFlow
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.UUID

class DeviceConnectDialogFragment : DialogFragment() {
    lateinit var binding : FragmentDeviceConnectDialogBinding
    private val viewModel: BLEViewModel by activityViewModels()
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var selectedDevice : BluetoothDevice? = null
    private lateinit var bluetooth : BluetoothManager


    var scanning = false
    val SCAN_PERIOD : Long = 3000
    private val handler = Handler()

    private var bluetoothService : BluetoothLeService? = null
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

        val gattServiceIntent = Intent(requireContext(), BluetoothLeService::class.java)
        requireContext().startService(gattServiceIntent)
        requireContext().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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
            bluetoothService?.let { bluetooth ->
                if (bluetooth.initialize()) {
                    Log.e("GattServSuccess", "Enable to initialize Bluetooth")
                    bluetooth.connect(selectedDevice?.address.toString())
                } else {
                    Log.e("GattServError", "Unable to initialize Bluetooth")
                    dismiss()
                }
            }
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


    private val serviceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            if (!bluetoothService?.initialize()!!) {
                Log.e("GattServError", "Unable to initialize Bluetooth")
                dismiss()
            } else {
                Log.e("GattServSuccess", "Enable to initialize Bluetooth")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }
    }


    private val gattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {

                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {

                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {

                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val servicesToCharacteristics = mutableMapOf<UUID, MutableList<ByteArray>>()
                    for (key in intent.extras!!.keySet()) {
                        val uuid = UUID.fromString(key)
                        val encodedDataList = intent.getStringArrayListExtra(key)!!
                        val decodedDataList = encodedDataList.map { Base64.decode(it, Base64.DEFAULT) }.toMutableList()
                        servicesToCharacteristics[uuid] = decodedDataList
                    }
                    viewModel.characteristicValues.value = servicesToCharacteristics
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter())
        if (bluetoothService != null) {
            val result = bluetoothService!!.connect(selectedDevice?.address.toString())
            Log.d("GATT상태 Receiver", "Connect request result=$result")
        }
    }
    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(gattUpdateReceiver)
    }
    private fun makeGattUpdateIntentFilter(): IntentFilter? {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        }
    }
    // -------------!! scanner !!--------------

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
            if (result.device.name != null) {
                viewModel.addDevice(result.device)
                activity?.runOnUiThread {
                    (binding.rvBLE.adapter as? BLEListAdapter)?.notifyDataSetChanged()
                }
            }
        }
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