package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.CustomExerciseDialogFragment
import com.tangoplus.tangoq.dialog.LoginScanDialogFragment
import com.tangoplus.tangoq.dialog.FilterProgramDialogFragment
import com.tangoplus.tangoq.dialog.MeasureBSDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchCategoryAndSearch
import com.tangoplus.tangoq.`object`.NetworkExercise.fetchExerciseById
import com.tangoplus.tangoq.`object`.NetworkProgram.fetchProgramVOBySn
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import com.tangoplus.tangoq.view.BarChartRender
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.random.Random


class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    val viewModel : UserViewModel by activityViewModels()
    val eViewModel : ExerciseViewModel by activityViewModels()
    val mViewModel : MeasureViewModel by activityViewModels()
    private lateinit var program : ProgramVO
    private lateinit var currentExerciseItem : ExerciseVO
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    lateinit var prefsManager : PreferencesManager
    private var singletonMeasure : MutableList<MeasureVO>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)

        // ActivityResultLauncher 초기화
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 결과 처리
            }
        }
        return binding.root
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------# 스크롤 관리 #------
        binding.nsvM.isNestedScrollingEnabled = false
        prefsManager = PreferencesManager(requireContext())

        // ------# 알람 intent #------
        binding.ibtnMAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMQRCode.setOnClickListener{
            val dialog = LoginScanDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }

//        val dialog = SetupDialogFragment.newInstance("startSetup")
//        dialog.show(requireActivity().supportFragmentManager, "SetupDialogFragment")


        when (isNetworkAvailable(requireContext())) {
            true -> {
                val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject

                // ------# 키오스크에서 데이터 가져왔을 때 #------
                // TODO parts에 결과값이 들어가야함.

                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }

                (requireActivity() as MainActivity).dataLoaded.observe(viewLifecycleOwner) { isLoaded ->
                    if (isLoaded) {
                        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
                        updateUI()
                    }
                }

                binding.btnMCustom.setOnClickListener {
                    val dialog = CustomExerciseDialogFragment()
                    dialog.show(
                        requireActivity().supportFragmentManager,
                        "CustomExerciseDialogFragment"
                    )
                }
            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }
    }

    private fun stringToLocalDate(dateTimeString: String): LocalDate {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val localDateTime = LocalDateTime.parse(dateTimeString, formatter)
        return localDateTime.toLocalDate()
    }

    fun convertDateFormat(inputDate: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        return date?.let { outputFormat.format(it) } ?: ""
    }

    private fun updateUI() {
        val measureScores = singletonMeasure

        measureScores?.let { measure ->
            if (measure.size > 0) {
                eViewModel.selectedMeasureDate.observe(viewLifecycleOwner) { selectedIndex ->

                    val firstDate = measure.get(selectedIndex).regDate
                    binding.tvMMeasureDate.text = firstDate

                    val selectedDate = measure.get(selectedIndex).regDate ?: ""
                    binding.tvMMeasureDate.text = selectedDate
                    binding.tvMOverall.text = measure.get(selectedIndex).overall.toString()
                    setAdapter(eViewModel.selectedMeasureDate.value!!)

                    // eViewModel.currentProgram TODO 버튼과 연결되는 맞춤 프로그램도 달라져야 함.
                    mViewModel.selectedMeasure = singletonMeasure?.get(0)

                }
            }
        }
    }

    private fun setAdapter(index: Int) {
        val layoutManager1 = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvM1.layoutManager = layoutManager1
        val muscleAdapter = StringRVAdapter(this@MainFragment, singletonMeasure?.get(index)?.dangerParts, "part")
        binding.rvM1.adapter = muscleAdapter
        binding.rvM1.isNestedScrollingEnabled = false

        // ------# balance check #------
        /* 이미 순위가 매겨진 부위들을 넣어서 index별로 각 밸런스 체크에 들어간다 */
        val stages = mutableListOf<MutableList<String>>()
        val balanceParts1 = mutableListOf("어깨", "골반")
        stages.add(balanceParts1)
        val balanceParts2 = mutableListOf("어깨", "팔꿉", "좌측 전완")
        stages.add(balanceParts2)
        val balanceParts3 = mutableListOf("골반", "좌측 어깨", "목")
        stages.add(balanceParts3)
        val balanceParts4 = mutableListOf("좌측 허벅지", "좌측 골반", "좌측 어깨")
        stages.add(balanceParts4)

        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(0 , -4))
        // 부위에 대한 설명 타입 - 근육 긴장, 이상 감지, 불균형 등

        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvM2.layoutManager = layoutManager2
        val balanceAdapter = BalanceRVAdapter(this@MainFragment, stages, degrees)
        binding.rvM2.adapter = balanceAdapter
    }
}