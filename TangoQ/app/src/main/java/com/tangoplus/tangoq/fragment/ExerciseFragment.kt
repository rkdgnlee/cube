package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.adapter.SpinnerAdapter
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.NetworkExerciseService.fetchExerciseJson
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.BLEListAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentExerciseBinding
import com.tangoplus.tangoq.`object`.CommonDefines
import com.tangoplus.tangoq.`object`.Singleton_bt_device
import com.tangoplus.tangoq.`object`.TedPermissionWrapper
import com.tangoplus.tangoq.service.BluetoothLeService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date


@Suppress("UNREACHABLE_CODE")
class ExerciseFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseBinding
    var verticalDataList = mutableListOf<ExerciseVO>()

    // ------! 블루투스 변수 !------
    var mDeviceInfoList: ArrayList<BluetoothDeviceInfo> = arrayListOf()
    private var mDevice: BluetoothDevice? = null
    private var isReceiverRegistered = false
    private  val REQUEST_SELECT_DEVICE = 1
    private  val REQUEST_ENABLE_BT = 2
    private var mBtAdapter: BluetoothAdapter? = null
    private var mDeviceList: ArrayList<BluetoothDevice>? = arrayListOf()
    private var mScanning = false
    private var mHandler: Handler? = null
    private  val SCAN_PERIOD: Long = 5000
    lateinit var singleton_bt_device : Singleton_bt_device
    private val UART_PROFILE_CONNECTED = 20
    private  val UART_PROFILE_DISCONNECTED = 21
    var mState = UART_PROFILE_DISCONNECTED
    var mService: BluetoothLeService? = null
    val mDeviceName = "PRBMD02"
    val mAddress = "01:02:03:04:05:06"
//    val adapter = BLEListAdapter(mDeviceInfoList, this@)
    private var mPreTime: Long = 0
    private val sharedPref : SharedPreferences by lazy {
        requireActivity().getSharedPreferences("TangoQ", Context.MODE_PRIVATE)
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.sflEc.startShimmer()
        binding.nsvEc.isNestedScrollingEnabled = false
        binding.rvEcAll.isNestedScrollingEnabled = false
        binding.rvEcAll.overScrollMode = 0
        binding.ibtnEcACTVClear.setOnClickListener {
            binding.actvEcSearch.text.clear()
        }
        // -----! 카테고리  시작 !-----
        val CategoryList = arrayOf("전체","목", "어깨", "팔꿉", "손목", "몸통", "복부", "엉덩", "무릎", "발목", "전신", "유산소", "코어", "몸통")
        val adapter2 = ExerciseCategoryRVAdapter(CategoryList,  this@ExerciseFragment )
        adapter2.category = CategoryList
        binding.rvEcCategory.adapter = adapter2
        val linearLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEcCategory.layoutManager = linearLayoutManager2
        // -----! 카테고리 끝 !-----



        lifecycleScope.launch {
            val responseArrayList = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))

            // ------! 자동완성 시작 !------
            val keywordList = mutableListOf<String>()
            for (i in 0 until responseArrayList.size) {
                keywordList.add(responseArrayList[i].exerciseName.toString())
            }
            val adapterActv = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, keywordList)
            binding.actvEcSearch.setAdapter(adapterActv)

            binding.actvEcSearch.setOnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position) as String
                val filterList = verticalDataList.filter { item ->
                    item.exerciseName == selectedItem
                }.toMutableList()
                val adapter = ExerciseRVAdapter(this@ExerciseFragment, filterList, "main")
                binding.rvEcAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEcAll.layoutManager = linearLayoutManager
                adapter.notifyDataSetChanged()
            }

            // ------! 자동완성 끝 !------

            try { // ------! rv vertical 시작 !------
                binding.sflEc.stopShimmer()
                binding.sflEc.visibility= View.GONE
                verticalDataList = responseArrayList.toMutableList()
                val adapter = ExerciseRVAdapter(this@ExerciseFragment, verticalDataList, "main")
                adapter.exerciseList = verticalDataList
                binding.rvEcAll.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                binding.rvEcAll.layoutManager = linearLayoutManager

                // ------! rv vertical 끝 !------
                val filterList = arrayListOf<String>()
                filterList.add("필터")
                filterList.add("최신순")
                filterList.add("인기순")
                filterList.add("추천순")
                binding.spnEcFilter.adapter = SpinnerAdapter(requireContext(), R.layout.item_spinner, filterList)
                binding.spnEcFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                    @SuppressLint("NotifyDataSetChanged")
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        when (position) {
                            0 -> {}
                            1 -> {
                                verticalDataList.sortBy { it.exerciseDescription }
                                adapter.notifyDataSetChanged()
                            }
                            2 -> {
                                verticalDataList.sortByDescending { it.videoTime }
                                adapter.notifyDataSetChanged()
                            }
                            3 -> {
                                verticalDataList.sortBy { it.exerciseIntensity }
                                adapter.notifyDataSetChanged()
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }


            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        } // ------! rv vertical 끝 !------

        // ------------------------! 블루투스 연결 시작 !------------------------
        singleton_bt_device = Singleton_bt_device.getInstance(requireContext())
        val deviceAddress = sharedPref.getString("device_address", null)
        mHandler = Handler()
        Log.w("init", "${singleton_bt_device.init}")
        TedPermissionWrapper.checkPermission(requireContext())
        mBtAdapter = singleton_bt_device.mBtAdapter // bluetoothadapter는 남아있음. mService
        if (mBtAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth is not avaliable", Toast.LENGTH_LONG).show()
            return
        }

        singleton_bt_device.mDeviceList.value = arrayListOf()
        if (singleton_bt_device.init == false) {
//            control_init()
            service_init()
            singleton_bt_device.init= true

        }
        if (!mBtAdapter!!.isEnabled()) {
            Log.i(ContentValues.TAG, "BT not enabled yet")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }

        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(requireContext(), "Bluetooth low energy not supported", Toast.LENGTH_LONG).show()

        }
        binding.ibtnEcBLEConnect.setOnClickListener {
            if (!isReceiverRegistered) {
                if (!mBtAdapter?.isEnabled()!!) {
                    Toast.makeText(requireContext(), "블루투스가 켜지지 않았습니다.", Toast.LENGTH_SHORT).show()
                    Log.i(ContentValues.TAG, "onResume() - BT not enabled yet")
                    val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
                }
            }
            clearDevice()
            scanLeDevice(false)
            scanLeDevice(true)
        }
    }
    // ------! 2 scan 시작 !------
    @SuppressLint("MissingPermission")
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mHandler?.postDelayed(Runnable {
                mScanning = false
                mBtAdapter?.stopLeScan(mLeScanCallback)
            }, SCAN_PERIOD)
            mScanning = true
            mBtAdapter?.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBtAdapter?.stopLeScan(mLeScanCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private val mLeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            val deviceAddress = sharedPref.getString("device_address", null)
            if (device.address == deviceAddress) {
                mDevice = device
                scanLeDevice(false)
            }
            Log.d(
                ContentValues.TAG,
                "dev name: " + device.getName() + ", addr: " + device.getAddress() + ", rssi: " + rssi
            )

            if (isAdded) {
                requireActivity().runOnUiThread(Runnable {
                    if (device.name != null)
                        addDevice(device, rssi)
                })
            }
        }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, rssi: Int) {
        var deviceFound = false
        for (listDev in singleton_bt_device.mDeviceList.value!!) {
            if (listDev.getAddress() == device.getAddress()) {
                deviceFound = true

                if (device.address == mAddress) {
                    val toConnectDevice = device
                    mScanning = false
                    mBtAdapter?.stopLeScan(mLeScanCallback)
                    requireActivity().runOnUiThread {
                        val layoutInflater = LayoutInflater.from(context)
                        val view = layoutInflater.inflate(R.layout.dialog_bs_bluetooth, null)
                        view.findViewById<TextView>(R.id.tvBtBSDeviceName).text = toConnectDevice.name
                        view.findViewById<TextView>(R.id.tvBtBSAddress).text = toConnectDevice.address

                        val dialog = BottomSheetDialog(requireContext())
                        dialog.setContentView(view)
                        dialog.show()
                        view.findViewById<Button>(R.id.btnBtBSConnect).setOnClickListener {
                            if (mState == UART_PROFILE_DISCONNECTED) {
                                try {
                                    mDevice = toConnectDevice // 있으면 mDevice를 내가 지목한 장치로 선택하고 연결 시작
                                    if (mService?.connect(mDevice!!.getAddress()) == true) {
                                        scanLeDevice(false)
                                    }
                                } catch (ex: Exception) {
                                    Log.e(ContentValues.TAG, ex.toString())
                                    Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG).show()
                            }
                        }
                        view.findViewById<ImageButton>(R.id.ibtnBtBSBack).setOnClickListener { dialog.dismiss() }
                        view.findViewById<Button>(R.id.btnBtBSCancel).setOnClickListener { dialog.dismiss() }
//                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
//                            setTitle("알림")
//                            setMessage("${toConnectDevice.name}을 연결하시겠습니까?\n주소${toConnectDevice.address}")
//                            setPositiveButton("예") { dialog, _ ->
//                                if (mState == UART_PROFILE_DISCONNECTED) {
//                                    try {
//                                        mDevice = toConnectDevice // 있으면 mDevice를 내가 지목한 장치로 선택하고 연결 시작
//                                        if (mService?.connect(mDevice!!.getAddress()) == true) {
//                                            scanLeDevice(false)
//                                        }
//                                    } catch (ex: Exception) {
//                                        Log.e(ContentValues.TAG, ex.toString())
//                                        Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
//                                    }
//                                } else {
//                                    Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG)
//                                        .show()
//                                }
//                            }
//                            setNegativeButton("아니오") { dialog, _ ->
//                            }
//                            create()
//                        }.show()
                    }
                }
                break
            }
        }
        if (!deviceFound) {
            singleton_bt_device.mDeviceList.value!!.add(device)
            val deviceName = device.name ?: "N/A"
            val deviceAddress = device.address ?: "N/A"
            mDeviceInfoList.add(
                BluetoothDeviceInfo(
                    deviceName,
                    deviceAddress,
                    rssi.toString() + "",
                    device
                )
            )
//            adapter.notifyDataSetChanged()

        }
    }

    private fun clearDevice() {
        if (singleton_bt_device.mDevice != null) {
            singleton_bt_device.mDevice = null
        }
        singleton_bt_device.mDeviceList.value?.clear()
        singleton_bt_device.mDeviceInfoList.clear()
//        adapter.notifyDataSetChanged()
    }

    private fun service_init() {
        val bindIntent: Intent = Intent(requireContext(), BluetoothLeService::class.java)
        requireActivity().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mUartStatusChangeReceiver, makeGattUpdateIntentFilter())
        Log.w("serviceInit", "serviceInit Success !")
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART)
        return intentFilter
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = (service as BluetoothLeService.LocalBinder).getService()
            Log.d(ContentValues.TAG, "onServiceConnection mService=" + mService)
            mService!!.initialize()
            if (mDevice != null) { // 기존 정보가 있을 때,
                mService!!.connect(mDevice!!.address)
            } else {

            }



            if (!mService!!.initialize()) {
                Log.e(ContentValues.TAG, "Unable to initialize Bluetooth")
//                finish()
            } else {

            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    private val mUartStatusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothLeService.ACTION_GATT_CONNECTED) {
                Log.d(ContentValues.TAG, "UART GATT Connected")
                mState = UART_PROFILE_CONNECTED
                singleton_bt_device.mDevice = mDevice
                Log.w("macAd저장", "${sharedPref.getString("device_address", null)}")

            } else if (action == BluetoothLeService.ACTION_GATT_DISCONNECTED) {
                Log.d(ContentValues.TAG, "UART Gatt Disconnected")
                mState = UART_PROFILE_DISCONNECTED
                mService?.close()
                requireActivity().runOnUiThread(Runnable {


                    mDevice = null
                    singleton_bt_device.mDevice = null
                    Toast.makeText(requireContext(), "연결 종료!", Toast.LENGTH_LONG).show()
                })

            } else if (action == BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED) {
                mService?.enableTxNotification()

            } else if (action == BluetoothLeService.ACTION_DATA_AVAILABLE) {
                singleton_bt_device.txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                val txValue = singleton_bt_device.txValue
                requireActivity().runOnUiThread(Runnable {
                    try {
//                        if (binding.tvRecvData.getLineCount() > 255) binding.tvRecvData.setText("")
                        val recvData = String(txValue!!)
                        val curTime = System.currentTimeMillis()
                        val duringTime: Float = (curTime - mPreTime) / 1000.0f
                        mPreTime = curTime
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        val date = Date(curTime)
                        val strTemp = "[ProcessTime] " + sdf.format(date) + "\r\n" +
                                "\tResult Time: " + duringTime + "\r\n"
//                        binding.tvRecvData.append(strTemp);
                        Log.d("dataAvailable", strTemp)
                        val hexData = StringBuilder()
                        for (b in txValue) {
                            hexData.append(String.format("%02x ", b.toInt() and 0xFF))
                        }
                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, hexData.toString()))

                        val decimalData = StringBuilder()
                        for (b in txValue) {
                            decimalData.append(String.format("%d ", b.toInt() and 0xFF))
                        }
                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, decimalData.toString()))
//                        if (cb_send_hex?.isChecked() == true)
//                        binding.tvRecvData.append("[recv] $hexData\r\n")
//                        binding.tvRecvData.append("[recv] $decimalData\r\n")
//                        else binding.tvRecvData!!.append(
//                            "[recv] $recvData\r\n"
//                        ) // + "\r\n");
//                        binding.svRecvData.post(Runnable { binding.svRecvData.fullScroll(View.FOCUS_DOWN) })
                        recv_process(txValue)

                        //long time = CommonDefines.convertBigEndianInt(txValue, 2, 4);
                        //Log.d(TAG, String.format("time:%d", time));
                    } catch (e: Exception) {
                        Log.e(ContentValues.TAG, e.toString())
                    }
                })
            } else if (action == BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED){
                Log.v("broadcast", "FCF action success?") // success 여기까지 됨.
                if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
                    Log.v("connect성공", "FIND_CHARACTERISTIC_FINISHED")
                    var mPreTime: Long = 0
                    val curTime = System.currentTimeMillis()
                    mPreTime = curTime
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    val date = Date(curTime)
                    var strTemp = "[Request time] " + sdf.format(date)
                    //binding.tvRecvData.append(strTemp + "\r\n");
                    Log.d(ContentValues.TAG, strTemp)
                    val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
                    strTemp = ""
                    var i = 0
                    while (i < buf.size) {
                        strTemp += String.format("%02X ", buf[i])
                        i++
                    }
                    Log.d(ContentValues.TAG, "[send data - byte] $strTemp")

                    Toast.makeText(requireContext(), "[send] $strTemp\r\n", Toast.LENGTH_LONG).show()
//                        binding.tvRecvData.append("[send] $strTemp\r\n")
                    mService?.writeRxCharacteristic(buf)
                }
            } else if (action == BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART) {
                Log.d(ContentValues.TAG, "Device doesn't support UART")
                mService?.disconnect()
            }
        }
    }

    private fun recv_process(rxData: ByteArray?) {
        val cmd = rxData!![0]
        val ack = rxData[1]
        var time: Long = 0
        var index = 0
        when (cmd) {
            CommonDefines.CMD_GET_COUNT -> {
                if (ack != CommonDefines.CMD_ACK) return
                val get_count = CommonDefines.convertBigEndianInt(rxData, 2, 2).toInt()
                Log.d(ContentValues.TAG, "[GET COUNT] $get_count")
            }

            else -> {
                if (rxData.size == 10) {
                    index = CommonDefines.convertBigEndianInt(rxData, 0, 2).toInt()
                    time = CommonDefines.convertBigEndianInt(rxData, 2, 4)
                    val date = Date()
                    date.setTime(time * 1000L)
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    Log.d(ContentValues.TAG, "[SYNC START] index: " + index + ", Date: " + df.format(date) + ", " + time)
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        Log.d(ContentValues.TAG, "onResume()")
//        if (!isReceiverRegistered) {
//            if (!mBtAdapter?.isEnabled()!!) {
//                Log.i(ContentValues.TAG, "onResume() - BT not enabled yet")
//                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
//            }
//        }

    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
//            requireContext().unregisterReceiver(mUartStatusChangeReceiver)
//            isReceiverRegistered = false
            mBtAdapter?.stopLeScan(mLeScanCallback)
        }
    }
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        Log.d(ContentValues.TAG, "onDestroy() but, enabled mBtAdapter")
        if (isReceiverRegistered) {
//            try {
//                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mUartStatusChangeReceiver)
//            } catch (e: Exception) {
//                Log.e(TAG, e.toString())
//            }
            mBtAdapter?.stopLeScan(mLeScanCallback)
//            requireActivity().unbindService(mServiceConnection)
//            if (mService != null) {
//                mService!!.stopSelf()
//                mService = null
//            }
//            isReceiverRegistered = false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            ContentValues.TAG,
            "onActivityResult() requestCode: $requestCode, resultCode: $resultCode"
        )
        when (requestCode) {
            REQUEST_SELECT_DEVICE -> if (resultCode == Activity.RESULT_OK && data != null) {
                Log.v("권한 허용", "${singleton_bt_device.init}")
            }
            REQUEST_ENABLE_BT -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Bluetooth has turned on ", Toast.LENGTH_LONG).show()
            } else {
                Log.d(ContentValues.TAG, "BT not enabled")
//                Toast.makeText(requireContext(), "Problem in BT Turning ON ", Toast.LENGTH_LONG).show()
//                finish()
            }

            else -> Log.e(ContentValues.TAG, "wrong requestCode")
        }
    }
//    @SuppressLint("SetTextI18n")
//    override fun onDeviceClick(device: BluetoothDeviceInfo) {
//        // 아이템 클릭 시 동작
//        mDevice = device.device
//    }
//
//    override fun onDeviceLongClick(device: BluetoothDeviceInfo): Boolean {
//        Log.e(ContentValues.TAG, "[LongClick] Dev Name: " + device.device_name + ", mac address: " + device.mac_address + ", rssi: " + device.rssi)
//        return true
//    }

    class BluetoothDeviceInfo(
        var device_name: String,
        var mac_address: String,
        var rssi: String,
        var device: BluetoothDevice
    )
    @SuppressLint("NotifyDataSetChanged")
    override fun onCategoryClick(category: String) {
        var filterList = mutableListOf<ExerciseVO>()
        if (category == "전체") {
            filterList = verticalDataList
        } else {
            filterList = verticalDataList.filter { item ->
                item.exerciseName!!.contains(category)
            }.toMutableList()
        }
        val adapter = ExerciseRVAdapter(this@ExerciseFragment, filterList, "main")
        binding.rvEcAll.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvEcAll.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
    }
}