package com.example.mhg

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.BLEListAdapter
import com.example.mhg.VO.BLEViewModel
import com.example.mhg.databinding.FragmentReportGoalBinding
import com.example.mhg.`object`.BLEGattAttributes
import com.example.mhg.`object`.CommonDefines
import com.example.mhg.service.BluetoothLeService
import java.text.SimpleDateFormat
import java.util.Date


class ReportGoalFragment : Fragment() {
    lateinit var binding: FragmentReportGoalBinding
    private val viewModel: BLEViewModel by activityViewModels()
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    var encodedData: String = ""
    private var byteArray : ByteArray = byteArrayOf()
    private var isReceiverRegistered = false
    private var selectedDevice : BluetoothDevice? = null
    private lateinit var bluetooth : BluetoothManager
    private var bluetoothService : BluetoothLeService? = null
    var scanning = false
    val SCAN_PERIOD : Long = 3000
    private val handler = Handler()
    var gatt : BluetoothGatt? = null


    private var mPreTime: Long = 0
    private val UART_PROFILE_CONNECTED = 20
    private val UART_PROFILE_DISCONNECTED = 21
    var mState = UART_PROFILE_DISCONNECTED
    private val mRealtimeOrWrite: Byte = 0x00 // 0x01: realtime, 0x00: write


    companion object {
        const val LIST_NAME = "LIST_NAME"
        const val LIST_UUID = "LIST_UUID"
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
    @SuppressLint("MissingPermission", "NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val txValue =
            byteArrayOf(0x00, 0x00, 0x63.toByte(), 0xB0.toByte(), 0xBD.toByte(), 0x4D.toByte())
        //byte[] txValue = new byte[] { (byte)0x82, (byte)0x06, (byte)0x00, (byte) 0x57};
        val time = CommonDefines.convertLittleEndianInt(txValue, 2, 4)
        val bTime = CommonDefines.convertBigEndianInt(txValue, 2, 4)

        val date = Date()
        date.setTime(time * 1000L)
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        //df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        //df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        Log.d("date", "Date little: " + df.format(date))

        date.setTime(bTime * 1000L)

        Log.d("date", "Date big: " + df.format(date))
        // -----! INTENT RECEIVER 설정 시작 !-----
        val gattServiceIntent = Intent(requireContext(), BluetoothLeService::class.java)
        requireContext().startService(gattServiceIntent)
        requireContext().bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        // -----! INTENT RECEIVER 설정 끝 !-----

        binding.pbBLEConnect.visibility = View.GONE
        binding.pbBLEConnect.isEnabled = false
        binding.btnBLERescan.setOnClickListener {
            binding.pbBLEConnect.isEnabled = true
            binding.pbBLEConnect.visibility = View.VISIBLE
            startScanning()
        }
//        binding.btnBLEFinish.setOnClickListener {
//            dismiss()
//        }

        val adapter = BLEListAdapter {device ->
            selectedDevice = device
            bluetoothService?.let { bluetooth ->
                if (bluetooth.initialize()) {
                    gatt = bluetooth.connect(selectedDevice?.address.toString())
                    Log.e("GattServSuccess", "Enable to initialize Bluetooth !, gatt: $gatt")
                } else {
                    Log.e("GattServError", "Unable to initialize Bluetooth")
//                    dismiss()
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


        // -----! 블루투스 켜기 끄기 설정 보존 시작 !-----
        val sharedPref = context?.getSharedPreferences("deviceSettings", Context.MODE_PRIVATE)
        val modeEditor = sharedPref?.edit()
        val bleMode = sharedPref?.getBoolean("bleMode", false)
        if (bleMode == true) {
            binding.schReportBluetooth.isChecked = true
            startScanning()
        } else {
            binding.schReportBluetooth.isChecked = false
        }
        val bleFilter= IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        // -----! 처음 BLE 사용 권한 허용 시작 !-----
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {permissions ->
            if (permissions.all { it.value }) {
                if (bluetooth.adapter.isEnabled) {
                    // -----! 기기 검색 하는 새 창 !-----
                    startScanning()
                }
                Log.w("BLE권한설정", "ALL PERMISSION PERMITTED !")
            } else {
                Toast.makeText(requireContext(), "블루투스 권한을 설정해주세요", Toast.LENGTH_SHORT).show()
            }
        } // -----! 처음 BLE 사용 권한 허용 끝 !-----

        // -----! receiver를 통해 재사용 설정 시작 !-----
        requireActivity().registerReceiver(receiver, bleFilter)
        binding.schReportBluetooth.setOnCheckedChangeListener{_, isChecked ->
            if (isChecked) {
                if (haveAllPermissions(requireContext())) {
                    bluetooth.adapter.enable()
                    modeEditor?.putBoolean("bleMode", isChecked)
                    modeEditor?.apply()
                } else {
                    requestPermissionLauncher.launch(ALL_BLE_PERMISSIONS)
                }
            } else {
                bluetooth.adapter?.disable()
                modeEditor?.putBoolean("bleMode", isChecked)
                modeEditor?.apply()
                viewModel.reset()
            }
        } // -----! receiver를 통해 재사용 설정 끝 !-----

        // -----! notification 설정 시작 !----- 00004 에 알람 박아놓음.


        // -----! notification 설정 끝 !-----

        binding.btn0x82Data.setOnClickListener {
            val curTime = System.currentTimeMillis()
            mPreTime = curTime
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
            val date = Date(curTime)
            var strTemp = "[Request time] " + sdf.format(date)
            //tv_recv_data.append(strTemp + "\r\n");
            //tv_recv_data.append(strTemp + "\r\n");
            Log.d("srtTemp", strTemp)


            val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
            strTemp = ""
            for (i in buf.indices) {
                strTemp += String.format("%02X ", buf[i])
            }
            Log.d(TAG, "[send data - byte] $strTemp")

            binding.tvGattData.append("[send] $strTemp\r\n")
            bluetoothService?.writeRxCharacteristic(buf)
        }
        binding.btnSendData.setOnClickListener {
            val value: ByteArray = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())


            val send_data: String = binding.tbSendData.getText().toString()
            val buf = send_data.toByteArray()

            Log.d("buf", "[send data - str] $send_data")

            var strTemp = ""
            for (i in buf.indices) {
                strTemp += String.format("%02X ", buf[i])
            }
            Log.d("strTemp", "[send data - byte] $strTemp")

            binding.tvGattData.append("[send] $strTemp\r\n")

            bluetoothService?.writeRxCharacteristic(buf)
            binding.tbSendData.text = null
//            val serviceUUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
//            val service = gatt?.getService(UUID.fromString(serviceUUID.toString()))
//            val characteristicUUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
//            val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID.toString()))


//            if (characteristic != null) {
//
////                Log.w("readOn","${bluetoothService?.readCharacteristic(characteristic)}")
//                Log.w("write", "write button")
//            }
//            val notifyUUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
//            val notifyCharacteristic = service?.getCharacteristic(UUID.fromString(notifyUUID.toString()))
//            if (notifyCharacteristic != null) {
//                Log.w("알림설정", "${bluetoothService?.setCharacteristicNotification(notifyCharacteristic,true)}")
//            }
        }

//        binding.btnUpdateData.setOnClickListener {
//            val serviceUUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
//            val service = gatt?.getService(UUID.fromString(serviceUUID.toString()))
//            val characteristicUUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
//            val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID.toString()))
//            val random = Random()
//            val value: ByteArray = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
//            if (characteristic != null) {
//                bluetoothService?.readCharacteristic(characteristic)
////                Log.w("readOn","${bluetoothService?.readCharacteristic(characteristic)}")
//                Log.w("write", "write button")
//            }
//        }

        // -----! 블루투스 기기 연결 후 viewmodel에서 꺼내서 gatt 서비스 쓰기 시작 !-----
        // 기기 이름과 상태
        viewModel.selectedDevice.observe(viewLifecycleOwner) {selectedDevice ->
            if (selectedDevice != null) {
                binding.tvDeviceName.text = selectedDevice.name
                binding.tvDeviceAddress.text = selectedDevice.address
                binding.tvDeviceStatus.text = "연결됨 !"
                binding.tvDeviceStatus.setTextColor(binding.tvDeviceStatus.resources.getColor(R.color.success_green))
                binding.tvDeviceName.setTextColor(binding.tvDeviceStatus.resources.getColor(R.color.mainblack))
            } else {
                binding.tvDeviceStatus.text = "미연결"
                binding.tvDeviceAddress.text = ""
                binding.tvDeviceName.text = "미연결"
                binding.tvDeviceStatus.setTextColor(binding.tvDeviceStatus.resources.getColor(R.color.grey600))
            }
        }
        // write 후 read 해온 값.
        viewModel.byteArrayData.observe(viewLifecycleOwner) { byteArrayData ->
            if (byteArrayData.isEmpty()) {
                binding.tvGattData.text = ""
            } else {
                // 대괄호를 제거합니다.
                val formattedData = byteArrayData.toString().replace("[", "").replace("]", "")
                Log.w("byteArray메인UI", formattedData)
                binding.tvGattData.text = formattedData
                Log.w("byteArray메인UI", "${binding.tvGattData.text}")
            }
        }
    }   // -----! 블루투스 기기 연결 후 viewmodel에서 꺼내서 gatt 서비스 쓰기 끝 !-----


    // -------------------------! 공통 !-------------------------
    private val serviceConnection : ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            if (!bluetoothService?.initialize()!!) {
                Log.e("GattServError", "Unable to initialize Bluetooth")
            } else {
                // -----! 블루투스 Service 연결 성공 !-----
                Log.e("GattServSuccess", "Enable to initialize Bluetooth")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bluetoothService = null
        }
    }

    // -----! intent를 통한 연결 상태 체크 receiver 시작 !-----
    val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {

            // -----! 블루투스 on 연결 시작 !-----
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            startScanning()
                        }
                    }
                }  // -----! 블루투스 on 연결 새창 끝 !-----
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    // -----! 뷰모델에 담기 !-----
//                    viewModel.gattManager = bluetoothService?.getGatt()?.let { GattManager(it) }
                    Log.d(TAG, "UART GATT Connected")
                    viewModel.selectedDevice.value = selectedDevice
                    Log.d(TAG, "UART GATT Connected")

                    mState = UART_PROFILE_CONNECTED


//                    val viewHolder = binding.rvBLE.findViewHolderForAdapterPosition(viewModel.devices.value!!.indexOf(selectedDevice))
//                    viewHolder?.itemView?.findViewById<TextView>(R.id.tvBleConnected)?.visibility = View.VISIBLE
//                    viewHolder?.itemView?.findViewById<TextView>(R.id.tvBleSearched)?.visibility = View.GONE
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {


                    selectedDevice = null
                    mState = UART_PROFILE_DISCONNECTED;
                    Log.d(TAG, "UART Gatt Disconnected")
                    bluetoothService?.deleteDevice();
                    val viewHolder = binding.rvBLE.findViewHolderForAdapterPosition(viewModel.devices.value!!.indexOf(selectedDevice))
                    viewHolder?.itemView?.findViewById<TextView>(R.id.tvBleConnected)?.visibility = View.GONE
                    viewHolder?.itemView?.findViewById<TextView>(R.id.tvBleSearched)?.visibility = View.VISIBLE
                }

                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
//                    displayGattServices(bluetoothService?.getSupportedGattServices())
                    bluetoothService?.enableTxNotification()
                }
                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    val txValue = intent.getByteArrayExtra(bluetoothService?.EXTRA_DATA)

                    requireActivity().runOnUiThread {
                        try {
                            if (binding.tvGattData.getLineCount() > 255) binding.tvGattData.text = ""
                            val recvData = String(txValue!!)
                            val curTime = System.currentTimeMillis()
                            val duringTime: Float = (curTime - mPreTime) / 1000.0f
                            mPreTime = curTime
                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                            val date = Date(curTime)
                            val strTemp = "[ProcessTime] " + sdf.format(date) + "\r\n" +
                                    "\tResult Time: " + duringTime + "\r\n"
                            //tv_recv_data.append(strTemp);
                            Log.d(TAG, strTemp)
                            val hexData = StringBuilder()
                            for (b in txValue) {
                                hexData.append(String.format("%02x ", b.toInt() and 0xFF))
                            }
                            Log.d(
                                TAG,
                                String.format("RX Data(%d): %s", txValue.size, hexData.toString())
                            )
                            binding.tvGattData.append("[recv] $hexData\r\n")
                            recv_process(txValue)
                        } catch (e: java.lang.Exception) {
                            Log.e(TAG, e.toString())
                        }
                    }
//                    // TODO 데이터 알림으로 MAIN UI에서 동작할 것들
//                    if (intent.action.equals(BluetoothLeService.ACTION_DATA_AVAILABLE)) {
//                        encodedData = intent.getStringExtra("encodedData").toString()
//
//                        val byteArray = intent.getByteArrayExtra("byteArray")
//                        Log.w("byteArray", Arrays.toString(byteArray))
//                        if (byteArray != null) {
//                            val hexString = byteArray.joinToString(" ") { String.format("%02X", it) }
//                            Log.w("hexString", hexString)
//                            viewModel.addByteArrayData(hexString)
//                            Log.w("byteArray메인UI", "${viewModel.byteArrayData.value}")
//                        }
//                    }
                }
                BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART -> {
                    Log.d(TAG, "Device doesn't support UART")
                    bluetoothService?.disconnect()
                }
            }
        }
    }
    // ------------------------ ! scan & connect !------------------------
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
    // ------------------- data read & write -------------------------
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?
        val unknownServiceString: String = resources.getString(R.string.unknown_service)
        val unknownCharaString: String = resources.getString(R.string.unknown_characteristic)

        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()
            currentServiceData[LIST_NAME] = BLEGattAttributes.lookup(uuid, unknownServiceString)
            currentServiceData[LIST_UUID] = uuid.toString()
            viewModel.gattServiceData += currentServiceData

            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
            val gattCharacteristics = gattService.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData[LIST_NAME] = BLEGattAttributes.lookup(uuid, unknownCharaString)
                currentCharaData[LIST_UUID] = uuid.toString()
                Log.w("property", "${gattCharacteristic.properties}")
                gattCharacteristicGroupData += currentCharaData
            }
            viewModel.mGattCharacteristics += charas
            viewModel.gattCharacteristicData.value?.plusAssign(gattCharacteristicGroupData)
            Log.w("gattCharacteristicData", "${viewModel.gattCharacteristicData.value}")
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            addAction(BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART)
        }
    }
    private fun recv_process(rxData: ByteArray) {
        val cmd = rxData[0]
        val ack = rxData[1]
        var time: Long = 0
        var index = 0
        when (cmd) {
            CommonDefines.CMD_GET_COUNT -> {
                if (ack != CommonDefines.CMD_ACK) return
                Log.d(
                    TAG,
                    "[GET COUNT] ${CommonDefines.convertBigEndianInt(rxData, 2, 2)}"
                )
            }

            else -> {
                if (rxData.size == 10) {
                    index = CommonDefines.convertBigEndianInt(rxData, 0, 2).toInt()
                    time = CommonDefines.convertBigEndianInt(rxData, 2, 4)
                    val date = Date()
                    date.setTime(time * 1000L)
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    Log.d(
                        TAG,
                        "[SYNC START] index: " + index + ", Date: " + df.format(date) + ", " + time
                    )
                }
            }
        }
    }

    // -----! 첫 블루투스 사용 권한 설정 시작 !-----

    fun haveAllPermissions(context: Context) =
        ALL_BLE_PERMISSIONS
            .all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    // -----! 첫 블루투스 사용 권한 설정 끝 !-----


    val ALL_BLE_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    // 특성에서 데이터를 추출하는 곳.
//    @RequiresPermission(anyOf = ["android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH", "android.permission.ACCESS_FINE_LOCATION", "android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"])
//    fun readCharacterisitic(serviceUUID: UUID, characteristicUUID: UUID) {
//        val service = viewModel.gatt.value?.getService(serviceUUID)
//        val characteristic = service?.getCharacteristic(characteristicUUID)
//
//        if (characteristic != null) {
//            val success = viewModel.gatt.value?.readCharacteristic(characteristic)
//            Log.v("bluetooth", "Read status: $success")
//        }
//    }
    override fun onDestroyView() {
        super.onDestroyView()
        // 브로드캐스트 리시버 해제
        requireActivity().unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        bluetooth = context?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: throw Exception("Bluetooth is not supported by this device")
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        if (!isReceiverRegistered) {
            requireContext().registerReceiver(receiver, makeGattUpdateIntentFilter())
            if (bluetoothService != null) {
                val result = bluetoothService!!.connect(selectedDevice?.address.toString())
                Log.d("GATT상태 Receiver", "Connect request result=$result")
            }
        }
    }
    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(receiver)
            isReceiverRegistered = false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            requireContext().unregisterReceiver(receiver)
            viewModel.reset()
        }

    }
}