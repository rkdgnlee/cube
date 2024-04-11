package com.example.mhg

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.BLEListAdapter
import com.example.mhg.databinding.FragmentReportGoalBinding
import java.lang.Exception
import java.util.UUID


class ReportGoalFragment : Fragment() {
    lateinit var binding: FragmentReportGoalBinding
    val REQUEST_CODE = 8080
    val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    // BLE 주변 장치 찾기 위한 클래스 + 스캐너
    private lateinit var bluetooth : BluetoothManager

    private val scanner: BluetoothLeScanner
        get() = bluetooth.adapter.bluetoothLeScanner

    private var selectedDevice : BluetoothDevice? = null

    private val devices = mutableListOf<BluetoothDevice>()
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
            if (device != null && device.name == "MyDevice" && device !in devices) {
                devices.add(device)
            }
        }
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.w("BLEScan", "BLE Scan Failed")
        }
    }
    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun startScanning() {
        scanner.startScan(scanCallback)
    }

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun connect() {
        gatt = selectedDevice?.connectGatt(requireContext(), true, callback)
        discoverServices()
    }

    private var gatt: BluetoothGatt? = null
    private val callback = object: BluetoothGattCallback() {

        // 기기 연결 상태가 변경될 때 호출 newState는 새로운 연결 상태.
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                // TODO: handle error
                return
            }
            if (newState == BluetoothGatt.STATE_CONNECTED) { // 장치가 연결 됨.
                // TODO: handle the fact that we've just connected
                discoverServices()
            }
        }
        // 기기 연결 후 기기에서 가져올 수 있는 서비스 찾았을 때 호출
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { // readCharacteristic() 메서드를 호출한 후, 특성 읽기 작업이 완료되면 호출
            super.onServicesDiscovered(gatt, status)
            services = gatt?.services!!
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (characteristic.uuid == myCharacteristicUUID) {
                Log.w("bluetooth", String(characteristic.value))
                readCharacterisitic(myCharacteristicUUID ,characteristic.uuid)
            }
        }
    }
    // 특성에서 데이터를 추출하는 곳.
    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun readCharacterisitic(serviceUUID: UUID, characteristicUUID: UUID) {
        val service = gatt?.getService(serviceUUID)
        val characteristic = service?.getCharacteristic(characteristicUUID)

        if (characteristic != null) {
            val success = gatt?.readCharacteristic(characteristic)
            Log.v("bluetooth", "Read status: $success")
        }
    }



    private var services: List<BluetoothGattService> = emptyList()

    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
    fun discoverServices() {
        gatt?.discoverServices()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bluetooth = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportGoalBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.schReportBluetooth.setOnClickListener {
            if (binding.schReportBluetooth.isChecked) {
                if (haveAllPermissions(requireContext())) {
                    bluetooth.adapter.isEnabled
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), ALL_BLE_PERMISSIONS, REQUEST_CODE)
                }
            } else {
                bluetooth.adapter?.disable()
            }
        }
        val adapter = BLEListAdapter(devices) {device ->
            selectedDevice = device
            connect()
        }
        adapter.devices = devices
        val linearLayoutManager2 =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvBLE.layoutManager = linearLayoutManager2
        binding.rvBLE.adapter = adapter
        adapter.notifyDataSetChanged()

    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 8080) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionGranted()
            } else {
                Toast.makeText(requireContext(), "블루투스 권한을 설정해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @SuppressLint("MissingPermission")
    fun onPermissionGranted() {
        startScanning()
    }
    fun haveAllPermissions(context: Context) =
        ALL_BLE_PERMISSIONS
            .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
}