package com.example.mhg

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.mhg.Bluetooth.BluetoothLeService
import com.example.mhg.Bluetooth.LeDeviceListAdapter
import com.example.mhg.databinding.FragmentReportGoalBinding


class ReportGoalFragment : Fragment() {
    lateinit var binding: FragmentReportGoalBinding
//    //scan results
//    var scanResults: ArrayList<BluetoothDevice>? = ArrayList()
//
//    //ble adapter
//    private var bleAdapter: BluetoothAdapter? = repository.bleAdapter
//
//    // BLE Gatt
//    private var bleGatt: BluetoothGatt? = null
    val bluetoothManager = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter : BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private var REQUEST_CODE_BLUETOOTH_PERMISSIONS = 8080
    private var scanning = false
    private val handler = Handler()
    val deviceAddress: String = ""
    private val leDeviceListAdapter = LeDeviceListAdapter()
    var bluetoothGatt: BluetoothGatt? = null

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportGoalBinding.inflate(inflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gattCallback = object:BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)

            }
        }


        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_PERMISSIONS)
        }
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            binding.schReportBluetooth.isChecked = false
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH_PERMISSIONS)
        }
        // -----! switch로 블루투스 작동 시작 !-----
        binding.schReportBluetooth.setOnClickListener {
            if (!binding.schReportBluetooth.isChecked) {
                bluetoothAdapter?.isEnabled
            } else {
                if (checkBluetoothPermission()) {
                    bluetoothAdapter?.disable()
                }
            }
        } // -----! switch로 블루투스 작동 끝 !-----

        val pairedDevices: Set<BluetoothDevice>? = if (checkBluetoothPermission()) {
            bluetoothAdapter?.bondedDevices
        } else {
            null
        }
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address
        }

    }
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("NotifyDataSetChanged")
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (checkBluetoothPermission()) {
                leDeviceListAdapter.addDevice(Pair(result.device.name, result.device.address))
                leDeviceListAdapter.notifyDataSetChanged()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun  scanLeDevice() {
        if (!scanning) {
            if (checkBluetoothPermission()) {
                handler.postDelayed({
                    scanning = false
                    bluetoothLeScanner?.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                bluetoothLeScanner?.startScan(leScanCallback)
            } else {
                scanning = false
                bluetoothLeScanner?.stopScan(leScanCallback)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkBluetoothPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_CODE_BLUETOOTH_PERMISSIONS
            )
            false
        } else {
            true
        }
    }

    fun connect(address:String) : Boolean {
        bluetoothGatt.let {
            if (address.equals(deviceAddress)) {
                if (it.connect()) {
                    connectionSt
                }
            }
        }
    }
}