package com.tangoplus.tangoq.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseBinding
import com.tangoplus.tangoq.dialog.ExerciseSearchDialogFragment
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.mediapipe.PoseLandmarkerHelper.Companion.TAG
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseJson
import kotlinx.coroutines.launch


class ExerciseFragment : Fragment(), OnCategoryClickListener {
    lateinit var binding : FragmentExerciseBinding
//    var verticalDataList = mutableListOf<ExerciseVO>()
    private val viewModel : ExerciseViewModel by activityViewModels()

    // ------! 블루투스 변수 !------
//    var mDeviceInfoList: ArrayList<BluetoothDeviceInfo> = arrayListOf()
//    private var mDevice: BluetoothDevice? = null
//    private var isReceiverRegistered = false
//    private  val REQUEST_SELECT_DEVICE = 1
//    private  val REQUEST_ENABLE_BT = 2
//    private var mBtAdapter: BluetoothAdapter? = null
//    private var mDeviceList: ArrayList<BluetoothDevice>? = arrayListOf()
//    private var mScanning = false
//    private var mHandler: Handler? = null
//    private  val SCAN_PERIOD: Long = 5000
//    lateinit var singleton_bt_device : Singleton_bt_device
//    private val UART_PROFILE_CONNECTED = 20
//    private  val UART_PROFILE_DISCONNECTED = 21
//    var mState = UART_PROFILE_DISCONNECTED
//    var mService: BluetoothLeService? = null
//    val mDeviceName = "PRBMD02"
//    val mAddress = "01:02:03:04:05:06"
//    val PERMISSION_REQUEST_CODE = 6000
//    val adapter = BLEListAdapter(mDeviceInfoList, this@)
//    private var mPreTime: Long = 0
//    private val sharedPref : SharedPreferences by lazy {
//        requireActivity().getSharedPreferences("TangoQ", Context.MODE_PRIVATE)
//    }

    companion object {
        private const val ARG_SN = "SN"
        fun newInstance(sn : Int): ExerciseFragment {
            val fragment = ExerciseFragment()
            val args = Bundle()
            args.putInt(ARG_SN, sn)
            fragment.arguments = args
            return fragment
        }
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
//        binding.sflEc.startShimmer()
        binding.nsvE.isNestedScrollingEnabled = false
        binding.rvEMainCategory.isNestedScrollingEnabled = false
        binding.rvEMainCategory.overScrollMode = 0
        val sn = arguments?.getInt(ARG_SN) ?: -1
        Log.v("EDsn", "$sn")
//        binding.ibtnEcACTVClear.setOnClickListener {
//            binding.actvEcSearch.text.clear()
//        }

//        (activity as MainActivity).setTopLayoutFull(requireActivity().findViewById(R.id.flMain), requireActivity().findViewById(R.id.clMain))

        // ------------------------! 블루투스 연결 시작 !------------------------
//        singleton_bt_device = Singleton_bt_device.getInstance(requireContext())
//        val deviceAddress = sharedPref.getString("device_address", null)
//        mHandler = Handler()
//        Log.w("init", "${singleton_bt_device.init}")
//
//        // 권한 체크
//        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
//            || ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
//            || ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
//            || ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
//            || ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//            // 필요한 권한이 없는 경우, 권한 요청
//            ActivityCompat.requestPermissions(requireActivity(), arrayOf(
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.BLUETOOTH,
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
//        } else {
//            // 필요한 권한이 모두 있는 경우, BluetoothAdapter 초기화
//            initializeBluetoothAdapter()
//        }
//
//
//
//        TedPermissionWrapper.checkPermission(requireContext())
//        mBtAdapter = singleton_bt_device.mBtAdapter // bluetoothadapter는 남아있음. mService
//        if (mBtAdapter == null) {
//            Toast.makeText(requireContext(), "블루투스를 사용할 수 없습니다.", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        singleton_bt_device.mDeviceList.value = arrayListOf()
//        if (singleton_bt_device.init == false) {
////            control_init()
//            service_init()
//            singleton_bt_device.init= true
//
//        }
//
////        lifecycleScope.launch {
////            if (!mBtAdapter!!.isEnabled()) {
////                Log.i("mBtAdapter", "BT not enabled yet")
////            } else if (mBtAdapter!!.isEnabled) {
////                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
////            }
////        }
//
//        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(requireContext(), "저전력 블루투스가 지원되지 않습니다.", Toast.LENGTH_LONG).show()
//
//        }
        when (isNetworkAvailable(requireContext())) {
            true -> {
                lifecycleScope.launch {
//                    val categoryArrayList = fetchExerciseAll(getString(R.string.IP_ADDRESS_t_exercise_description)).sortedBy { it.first }.toMutableList()
//            val typeArrayList = fetchExerciseType(getString(R.string.IP_ADDRESS_t_Exercise_Description))

                    val categoryArrayList = mutableListOf<Pair<Int, String>>()
                    categoryArrayList.add(Pair(1, "기본 밸런스 운동프로그램"))
                    categoryArrayList.add(Pair(2, "기본 스트레칭 운동"))
                    categoryArrayList.add(Pair(3, "근육중심 운동프로그램"))
                    categoryArrayList.add(Pair(4, "운동기구 활용 스트레칭 프로그램"))
                    categoryArrayList.add(Pair(5, "운동기구 활용 운동프로그램"))
                    val typeArrayList = listOf("목관절", "어깨", "팔꿉", "손목", "척추", "복부", "엉덩", "무릎","발목" )

                    try { // ------! rv vertical 시작 !------
//                        Log.v("cateSize", "mainCategoryList: ${categoryArrayList}, subCategoryList: $typeArrayList")
                        val adapter = ExerciseCategoryRVAdapter(categoryArrayList, typeArrayList,this@ExerciseFragment, this@ExerciseFragment, sn, "mainCategory" )
                        binding.rvEMainCategory.adapter = adapter
                        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                        binding.rvEMainCategory.layoutManager = linearLayoutManager
                        // ------! rv vertical 끝 !------

                        // ------# exercise 전부 미리 다운받아 VM에 넣기  #------
                        if (viewModel.allExercises.isEmpty()) {
                            viewModel.allExercises = fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description)).toMutableList()
                            Log.v("VM>AllExercises", "${viewModel.allExercises.size}")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}")
                    }

                    binding.linearLayout7.setOnClickListener{
                        val dialog = ExerciseSearchDialogFragment()
                        dialog.show(requireActivity().supportFragmentManager, "ExerciseSearchDialogFragment")
                    }
                }
            }
            false -> {

            }
        }

    }

    override fun onCategoryClick(sn: Int, category: String) {

    }

//    override fun categoryScroll(view: View) {
//        scrollToView(view)
//    }

    // ------! 이미지 스크롤 !------
//    private fun scrollToView(view: View) {
//        // 1. 뷰의 위치를 저장할 배열을 생성합니다.
//        val location = IntArray(2)
//        // 2. 뷰의 위치를 'window' 기준으로 계산하여 배열에 저장합니다
//        view.getLocationInWindow(location)
//        val viewTop = location[1]
//        // 3. 스크롤 뷰의 위치를 저장할 배열을 생성합니다.
//        val scrollViewLocation = IntArray(2)
//
//        // 4. 스크롤 뷰의 위치를 'window' 기준으로 계산하여 배열에 저장합니다.
//        binding.nsvE.getLocationInWindow(scrollViewLocation)
//        val scrollViewTop = scrollViewLocation[1]
//        // 5. 현재 스크롤 뷰의 스크롤된 y 위치를 얻습니다.
//        val scrollY = binding.nsvE.scrollY
//        // 6. 스크롤할 위치를 계산합니다.
//        //    현재 스크롤 위치에 뷰의 상대 위치를 더하여 올바른 스크롤 위치를 계산합니다.
//        val scrollTo = scrollY + viewTop - scrollViewTop
//        // 7. 스크롤 뷰를 해당 위치로 스크롤합니다.
//        binding.nsvE.smoothScrollTo(0, scrollTo)
//    }

//    private fun initializeBluetoothAdapter() {
//        val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//        startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
//    }
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        when (requestCode) {
//            PERMISSION_REQUEST_CODE -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    initializeBluetoothAdapter()
//                }
//            }
//        }
//    }
//
//
//    // ------! 2 scan 시작 !------
//    @SuppressLint("MissingPermission")
//    private fun scanLeDevice(enable: Boolean) {
//        if (enable) {
//            mHandler?.postDelayed({
//                mScanning = false
//                mBtAdapter?.stopLeScan(mLeScanCallback)
//            }, SCAN_PERIOD)
//            mScanning = true
//            mBtAdapter?.startLeScan(mLeScanCallback)
//        } else {
//            mScanning = false
//            mBtAdapter?.stopLeScan(mLeScanCallback)
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private val mLeScanCallback =
//        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
//            val deviceAddress = sharedPref.getString("device_address", null)
//            if (device.address == deviceAddress) {
//                mDevice = device
//                scanLeDevice(false)
//            }
//            Log.d(
//                ContentValues.TAG,
//                "dev name: " + device.getName() + ", addr: " + device.getAddress() + ", rssi: " + rssi
//            )
//
//            if (isAdded) {
//                requireActivity().runOnUiThread(Runnable {
//                    if (device.name != null)
//                        addDevice(device, rssi)
//                })
//            }
//        }
//
//    @SuppressLint("MissingPermission")
//    private fun addDevice(device: BluetoothDevice, rssi: Int) {
//        var deviceFound = false
//        for (listDev in singleton_bt_device.mDeviceList.value!!) {
//            if (listDev.getAddress() == device.getAddress()) {
//                deviceFound = true
//
//                if (device.address == mAddress) {
//                    val toConnectDevice = device
//                    mScanning = false
//                    mBtAdapter?.stopLeScan(mLeScanCallback)
//                    requireActivity().runOnUiThread {
//                        val layoutInflater = LayoutInflater.from(context)
//                        val view = layoutInflater.inflate(R.layout.dialog_bs_bluetooth, null)
//                        view.findViewById<TextView>(R.id.tvBtBSDeviceName).text = toConnectDevice.name
//                        view.findViewById<TextView>(R.id.tvBtBSAddress).text = toConnectDevice.address
//
//                        val dialog = BottomSheetDialog(requireContext())
//                        dialog.setContentView(view)
//                        dialog.show()
//                        view.findViewById<Button>(R.id.btnBtBSConnect).setOnClickListener {
//                            if (mState == UART_PROFILE_DISCONNECTED) {
//                                try {
//                                    mDevice = toConnectDevice // 있으면 mDevice를 내가 지목한 장치로 선택하고 연결 시작
//                                    if (mService?.connect(mDevice!!.getAddress()) == true) {
//                                        scanLeDevice(false)
//                                    }
//                                } catch (ex: Exception) {
//                                    Log.e(ContentValues.TAG, ex.toString())
//                                    Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
//                                }
//                            } else {
//                                Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                        view.findViewById<ImageButton>(R.id.ibtnBtBSBack).setOnClickListener { dialog.dismiss() }
//                        view.findViewById<Button>(R.id.btnBtBSCancel).setOnClickListener { dialog.dismiss() }
////                        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
////                            setTitle("알림")
////                            setMessage("${toConnectDevice.name}을 연결하시겠습니까?\n주소${toConnectDevice.address}")
////                            setPositiveButton("예") { dialog, _ ->
////                                if (mState == UART_PROFILE_DISCONNECTED) {
////                                    try {
////                                        mDevice = toConnectDevice // 있으면 mDevice를 내가 지목한 장치로 선택하고 연결 시작
////                                        if (mService?.connect(mDevice!!.getAddress()) == true) {
////                                            scanLeDevice(false)
////                                        }
////                                    } catch (ex: Exception) {
////                                        Log.e(ContentValues.TAG, ex.toString())
////                                        Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
////                                    }
////                                } else {
////                                    Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG)
////                                        .show()
////                                }
////                            }
////                            setNegativeButton("아니오") { dialog, _ ->
////                            }
////                            create()
////                        }.show()
//                    }
//                }
//                break
//            }
//        }
//        if (!deviceFound) {
//            singleton_bt_device.mDeviceList.value!!.add(device)
//            val deviceName = device.name ?: "N/A"
//            val deviceAddress = device.address ?: "N/A"
//            mDeviceInfoList.add(
//                BluetoothDeviceInfo(
//                    deviceName,
//                    deviceAddress,
//                    rssi.toString() + "",
//                    device
//                )
//            )
////            adapter.notifyDataSetChanged()
//
//        }
//    }
//
//    private fun clearDevice() {
//        if (singleton_bt_device.mDevice != null) {
//            singleton_bt_device.mDevice = null
//        }
//        singleton_bt_device.mDeviceList.value?.clear()
//        singleton_bt_device.mDeviceInfoList.clear()
////        adapter.notifyDataSetChanged()
//    }
//
//    private fun service_init() {
//        val bindIntent: Intent = Intent(requireContext(), BluetoothLeService::class.java)
//        requireActivity().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
//        LocalBroadcastManager.getInstance(requireContext())
//            .registerReceiver(mUartStatusChangeReceiver, makeGattUpdateIntentFilter())
//        Log.w("serviceInit", "serviceInit Success !")
//    }
//
//    private fun makeGattUpdateIntentFilter(): IntentFilter {
//        val intentFilter = IntentFilter()
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
//        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
//        intentFilter.addAction(BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED)
//        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
//        intentFilter.addAction(BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART)
//        return intentFilter
//    }
//
//    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName, service: IBinder) {
//            mService = (service as BluetoothLeService.LocalBinder).getService()
//            Log.d(ContentValues.TAG, "onServiceConnection mService=" + mService)
//            mService!!.initialize()
//            if (mDevice != null) { // 기존 정보가 있을 때,
//                mService!!.connect(mDevice!!.address)
//            } else {
//
//            }
//
//
//
//            if (!mService!!.initialize()) {
//                Log.e(ContentValues.TAG, "Unable to initialize Bluetooth")
////                finish()
//            } else {
//
//            }
//        }
//        override fun onServiceDisconnected(name: ComponentName) {
//            mService = null
//        }
//    }
//
//    private val mUartStatusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            if (action == BluetoothLeService.ACTION_GATT_CONNECTED) {
//                Log.d(ContentValues.TAG, "UART GATT Connected")
//                mState = UART_PROFILE_CONNECTED
//                singleton_bt_device.mDevice = mDevice
//                Log.w("macAd저장", "${sharedPref.getString("device_address", null)}")
//
//            } else if (action == BluetoothLeService.ACTION_GATT_DISCONNECTED) {
//                Log.d(ContentValues.TAG, "UART Gatt Disconnected")
//                mState = UART_PROFILE_DISCONNECTED
//                mService?.close()
//                requireActivity().runOnUiThread(Runnable {
//
//
//                    mDevice = null
//                    singleton_bt_device.mDevice = null
//                    Toast.makeText(requireContext(), "연결 종료!", Toast.LENGTH_LONG).show()
//                })
//
//            } else if (action == BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED) {
//                mService?.enableTxNotification()
//
//            } else if (action == BluetoothLeService.ACTION_DATA_AVAILABLE) {
//                singleton_bt_device.txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
//                val txValue = singleton_bt_device.txValue
//                requireActivity().runOnUiThread(Runnable {
//                    try {
////                        if (binding.tvRecvData.getLineCount() > 255) binding.tvRecvData.setText("")
//                        val recvData = String(txValue!!)
//                        val curTime = System.currentTimeMillis()
//                        val duringTime: Float = (curTime - mPreTime) / 1000.0f
//                        mPreTime = curTime
//                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
//                        val date = Date(curTime)
//                        val strTemp = "[ProcessTime] " + sdf.format(date) + "\r\n" +
//                                "\tResult Time: " + duringTime + "\r\n"
////                        binding.tvRecvData.append(strTemp);
//                        Log.d("dataAvailable", strTemp)
//                        val hexData = StringBuilder()
//                        for (b in txValue) {
//                            hexData.append(String.format("%02x ", b.toInt() and 0xFF))
//                        }
//                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, hexData.toString()))
//
//                        val decimalData = StringBuilder()
//                        for (b in txValue) {
//                            decimalData.append(String.format("%d ", b.toInt() and 0xFF))
//                        }
//                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, decimalData.toString()))
////                        if (cb_send_hex?.isChecked() == true)
////                        binding.tvRecvData.append("[recv] $hexData\r\n")
////                        binding.tvRecvData.append("[recv] $decimalData\r\n")
////                        else binding.tvRecvData!!.append(
////                            "[recv] $recvData\r\n"
////                        ) // + "\r\n");
////                        binding.svRecvData.post(Runnable { binding.svRecvData.fullScroll(View.FOCUS_DOWN) })
//                        recv_process(txValue)
//
//                        //long time = CommonDefines.convertBigEndianInt(txValue, 2, 4);
//                        //Log.d(TAG, String.format("time:%d", time));
//                    } catch (e: Exception) {
//                        Log.e(ContentValues.TAG, e.toString())
//                    }
//                })
//            } else if (action == BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED){
//                Log.v("broadcast", "FCF action success?") // success 여기까지 됨.
//                if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                    Log.v("connect성공", "FIND_CHARACTERISTIC_FINISHED")
//                    var mPreTime: Long = 0
//                    val curTime = System.currentTimeMillis()
//                    mPreTime = curTime
//                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
//                    val date = Date(curTime)
//                    var strTemp = "[Request time] " + sdf.format(date)
//                    //binding.tvRecvData.append(strTemp + "\r\n");
//                    Log.d(ContentValues.TAG, strTemp)
//                    val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
//                    strTemp = ""
//                    var i = 0
//                    while (i < buf.size) {
//                        strTemp += String.format("%02X ", buf[i])
//                        i++
//                    }
//                    Log.d(ContentValues.TAG, "[send data - byte] $strTemp")
//
//                    Toast.makeText(requireContext(), "[send] $strTemp\r\n", Toast.LENGTH_LONG).show()
////                        binding.tvRecvData.append("[send] $strTemp\r\n")
//                    mService?.writeRxCharacteristic(buf)
//                }
//            } else if (action == BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART) {
//                Log.d(ContentValues.TAG, "Device doesn't support UART")
//                mService?.disconnect()
//            }
//        }
//    }
//
//    private fun recv_process(rxData: ByteArray?) {
//        val cmd = rxData!![0]
//        val ack = rxData[1]
//        var time: Long = 0
//        var index = 0
//        when (cmd) {
//            CommonDefines.CMD_GET_COUNT -> {
//                if (ack != CommonDefines.CMD_ACK) return
//                val get_count = CommonDefines.convertBigEndianInt(rxData, 2, 2).toInt()
//                Log.d(ContentValues.TAG, "[GET COUNT] $get_count")
//            }
//
//            else -> {
//                if (rxData.size == 10) {
//                    index = CommonDefines.convertBigEndianInt(rxData, 0, 2).toInt()
//                    time = CommonDefines.convertBigEndianInt(rxData, 2, 4)
//                    val date = Date()
//                    date.setTime(time * 1000L)
//                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
//                    Log.d(ContentValues.TAG, "[SYNC START] index: " + index + ", Date: " + df.format(date) + ", " + time)
//                }
//            }
//        }
//    }
//    @SuppressLint("MissingPermission")
//    override fun onResume() {
//        super.onResume()
//        Log.d(ContentValues.TAG, "onResume()")
////        if (!isReceiverRegistered) {
////            if (!mBtAdapter?.isEnabled()!!) {
////                Log.i(ContentValues.TAG, "onResume() - BT not enabled yet")
////                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
////                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
////            }
////        }
//
//    }
//
//    @SuppressLint("MissingPermission")
//    override fun onPause() {
//        super.onPause()
//        if (isReceiverRegistered) {
////            requireContext().unregisterReceiver(mUartStatusChangeReceiver)
////            isReceiverRegistered = false
//            mBtAdapter?.stopLeScan(mLeScanCallback)
//        }
//    }
//    @SuppressLint("MissingPermission")
//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d(ContentValues.TAG, "onDestroy() but, enabled mBtAdapter")
//        if (isReceiverRegistered) {
////            try {
////                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mUartStatusChangeReceiver)
////            } catch (e: Exception) {
////                Log.e(TAG, e.toString())
////            }
//            mBtAdapter?.stopLeScan(mLeScanCallback)
////            requireActivity().unbindService(mServiceConnection)
////            if (mService != null) {
////                mService!!.stopSelf()
////                mService = null
////            }
////            isReceiverRegistered = false
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Log.d(
//            ContentValues.TAG,
//            "onActivityResult() requestCode: $requestCode, resultCode: $resultCode"
//        )
//        when (requestCode) {
//            REQUEST_SELECT_DEVICE -> if (resultCode == Activity.RESULT_OK && data != null) {
//                Log.v("권한 허용", "${singleton_bt_device.init}")
//            }
//            REQUEST_ENABLE_BT -> if (resultCode == Activity.RESULT_OK) {
//                Toast.makeText(requireContext(), "블루투스가 동작 중입니다.", Toast.LENGTH_LONG).show()
//            } else {
//                Log.d(ContentValues.TAG, "BT not enabled")
////                Toast.makeText(requireContext(), "Problem in BT Turning ON ", Toast.LENGTH_LONG).show()
////                finish()
//            }
//
//            else -> Log.e(ContentValues.TAG, "wrong requestCode")
//        }
//    }
////    @SuppressLint("SetTextI18n")
////    override fun onDeviceClick(device: BluetoothDeviceInfo) {
////        // 아이템 클릭 시 동작
////        mDevice = device.device
////    }
////
////    override fun onDeviceLongClick(device: BluetoothDeviceInfo): Boolean {
////        Log.e(ContentValues.TAG, "[LongClick] Dev Name: " + device.device_name + ", mac address: " + device.mac_address + ", rssi: " + device.rssi)
////        return true
////    }
//
//    class BluetoothDeviceInfo(
//        var device_name: String,
//        var mac_address: String,
//        var rssi: String,
//        var device: BluetoothDevice
//    )
//
//    // ------! horizontal 아이템 눌렀을 때 !------

}