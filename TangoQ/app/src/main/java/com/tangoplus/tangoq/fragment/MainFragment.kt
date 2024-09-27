package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
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
import androidx.fragment.app.strictmode.Violation
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.adapter.BalanceRVAdapter
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.HistoryViewModel
import com.tangoplus.tangoq.data.MeasureVO
import com.tangoplus.tangoq.data.MeasureViewModel
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.ProgramCustomDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.MeasureBSDialogFragment
import com.tangoplus.tangoq.dialog.ProgramSelectDialogFragment
import com.tangoplus.tangoq.`object`.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.`object`.Singleton_t_measure
import jp.wasabeef.blurry.Blurry

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    val viewModel : UserViewModel by activityViewModels()
    val eViewModel : ExerciseViewModel by activityViewModels()
    val mViewModel : MeasureViewModel by activityViewModels()
    val hViewModel : HistoryViewModel by activityViewModels()
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
        binding.sflM.startShimmer()

        // ------# 알람 intent #------
        binding.ibtnMAlarm.setOnClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }
        binding.ibtnMQRCode.setOnClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }

        if (isFirstRun("GuideDialogFragment_isFirstRun")) {
            val dialog = GuideDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "GuideDialogFragment")
        }

        when (isNetworkAvailable(requireContext())) {
            true -> {
                val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
                isLoad(true)
                // ------# 키오스크에서 데이터 가져왔을 때 #------
                // TODO parts에 결과값이 들어가야함.

                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }

                (requireActivity() as MainActivity).dataLoaded.observe(viewLifecycleOwner) { isLoaded ->
                    if (isLoaded) {
                        binding.sflM.stopShimmer()
                        isLoad(false)
                        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures

                        // 완료 갯수 넣기 현재 고정 0개임 TODO 여기에 오늘 날짜 운동 기록을 가져와야함. (번호 대조 후 여기에 넣기)
//                        setEpisodeProgress(0)
                        mViewModel.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                        }
                        updateUI()
                    }
                }

                binding.btnMCustom.setOnClickListener {
                    val dialog = ProgramSelectDialogFragment()
                    dialog.show(
                        requireActivity().supportFragmentManager,
                        "ProgramSelectDialogFragment"
                    )
                }

                binding.ivMBlur.setOnClickListener {
                    val bnb : BottomNavigationView = requireActivity().findViewById(R.id.bnbMain)
                    bnb.selectedItemId = R.id.measure
                }
            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }
    }



    private fun isLoad(loading: Boolean) {
        if (loading) {
            binding.sflM.visibility = View.VISIBLE
            binding.clMMeasure.visibility = View.GONE
        } else {
            binding.sflM.visibility  =View.GONE
            binding.clMMeasure.visibility = View.VISIBLE
        }
    }

    // 상단 어댑터와 하단 어댑터 같이 나옴
    private fun setAdapter(index: Int) {
        val layoutManager1 = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvM1.layoutManager = layoutManager1
        val muscleAdapter = StringRVAdapter(this@MainFragment, singletonMeasure?.get(index)?.dangerParts?.map { it.first }?.toMutableList(), "part", mViewModel)
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
        val balanceParts4 = mutableListOf("허벅지",  "어깨")
        stages.add(balanceParts4)
        val balanceParts5 = mutableListOf("좌측 허벅지", "좌측 골반", "좌측 어깨")
        stages.add(balanceParts5)

        val degrees =  mutableListOf(Pair(1, 3), Pair(1,0), Pair(1, 2), Pair(2, -1), Pair(0 , -4))
        // 부위에 대한 설명 타입 - 근육 긴장, 이상 감지, 불균형 등

        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvM2.layoutManager = layoutManager2
        val balanceAdapter = BalanceRVAdapter(this@MainFragment, stages, degrees)
        binding.rvM2.adapter = balanceAdapter
    }

    // 블러 유무 판단하기
    private fun updateUI() {
        val measureScores = singletonMeasure

        measureScores?.let { measure ->
            if (measure.size > 0) {
                binding.tvMTitle.text = "연동 측정 운동"
                binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondBgContainerColor))
                binding.tvMMeasureDate.visibility = View.VISIBLE
                binding.rvM2.visibility = View.VISIBLE
                binding.tvMCustom.visibility = View.VISIBLE
                binding.tvMBlur.visibility = View.GONE
                binding.ivMBlur.visibility = View.GONE
                binding.clMMeasure.visibility = View.VISIBLE

                // ------# 바텀시트에서 변한 selectedMeasureDate 에 맞게 변함.
                mViewModel.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                    val dateIndex = singletonMeasure?.indexOf(singletonMeasure?.find { it.regDate == selectedDate }!!)

                    if (selectedDate != singletonMeasure?.get(0)?.regDate) {
                        setProgramButton(false)
                    } else {
                        setProgramButton(true)
                    }
//                    val firstDate = measure.get(selectedIndex).regDate
//                    binding.tvMMeasureDate.text = firstDate.substring(0, 10)
                    val selectedDate = measure.get(dateIndex!!).regDate
                    binding.tvMMeasureDate.text = selectedDate.substring(0, 10)
                    binding.tvMOverall.text = measure.get(dateIndex).overall.toString()
                    setAdapter(dateIndex)

                    Log.v("메인Date", "dateIndex: ${dateIndex}, selectedDate: $selectedDate, singletonMeasure ${singletonMeasure?.get(dateIndex)?.fileUris}")
                    binding.tvMMeasureResult1.text = "${singletonMeasure?.get(dateIndex)?.dangerParts?.get(0)?.first}부위가 부상 위험이 있습니다."
                    binding.tvMMeasureResult2.text = "${singletonMeasure?.get(dateIndex)?.dangerParts?.get(1)?.first}부위가 부상 위험이 있습니다."

                    // eViewModel.currentProgram TODO 버튼과 연결되는 맞춤 프로그램도 달라져야 함.
                }
            } else {
                binding.tvMTitle.text = "${Singleton_t_user.getInstance(requireContext()).jsonObject?.getString("user_name")}님"
                binding.constraintLayout2.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.tvMMeasureDate.visibility = View.GONE
                binding.rvM2.visibility = View.GONE
                binding.tvMCustom.visibility = View.GONE
                binding.tvMBlur.visibility = View.VISIBLE
                applyBlurToConstraintLayout(binding.clMMeasure, binding.ivMBlur)
                binding.clMMeasure.visibility = View.INVISIBLE
            }
        }
    }

//    private fun setEpisodeProgress(finishCount: Int) {
//        binding.tvMCustomProgress.text = "완료 ${finishCount}/${hViewModel.currentProgram?.exercises?.get(0)?.size}개"
//        binding.hpvMCustomProgress.progress = (finishCount * 100).div(hViewModel.currentProgram?.exercises?.get(0)?.size!!)
//    }

    private fun setProgramButton(isEnabled: Boolean) {
        if (isEnabled) {
            binding.btnMCustom.isEnabled = true
            binding.btnMCustom.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.mainColor)
            binding.btnMCustom.text = "맞춤 프로그램 시작하기"
//            setEpisodeProgress(0)
        } else {
            binding.btnMCustom.isEnabled = false
            binding.btnMCustom.text = "프로그램 완료, 재측정을 진행해 주세요"
            binding.btnMCustom.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.subColor150)
//            setEpisodeProgress(hViewModel.currentProgram?.exercises?.get(0)?.size!!)

        }
    }

    fun applyBlurToConstraintLayout(constraintLayout: ConstraintLayout, imageView: ImageView) {
        constraintLayout.post {
            Blurry.with(constraintLayout.context)
                .radius(10)
                .sampling(5)
                .async()
                .capture(constraintLayout) // ConstraintLayout의 스크린샷을 캡처하여 블러 처리
                .into(imageView) // 블러 처리된 이미지를 ImageView에 설정
        }

    }
}