package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MainProgressRVAdapter
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.adapter.MainPartRVAdapter
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.MeasureBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.function.TooltipManager
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.api.NetworkRecommendation.getRecommendationProgress
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.function.MeasurementManager.createMeasureComment
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    private val avm by activityViewModels<AnalysisViewModel>()
    private val mvm : MeasureViewModel by activityViewModels()
    private val pvm: ProgressViewModel by activityViewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var prefsManager : PreferencesManager
    private var measures : MutableList<MeasureVO>? = null
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var latestRecSn = -1
    private lateinit var ssm : SaveSingletonManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (pvm.fromProgramCustom) {
            CoroutineScope(Dispatchers.IO).launch {
                val progressRec = getRecommendationProgress(getString(R.string.API_recommendation), requireContext(), mvm.selectedMeasure?.sn ?: 0)
                mvm.selectedMeasure?.recommendations = progressRec
                withContext(Dispatchers.Main){
                    if (isAdded) {
                        Singleton_t_measure.getInstance(requireContext()).measures?.find { it.sn == mvm.selectedMeasure?.sn }?.recommendations = progressRec
                        Log.v("날짜변경해도 잘들어가는지", "${mvm.selectedMeasureDate.value}, ${mvm.selectedMeasure?.regDate} ${mvm.selectedMeasure?.recommendations}")
                        setAdapter()
                    }

                    pvm.fromProgramCustom = false
                }
            }
        }
    }
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ------# 스크롤 관리 #------
        binding.nsvM.isNestedScrollingEnabled = false
        prefsManager = PreferencesManager(requireContext())
        val sn = Singleton_t_user.getInstance(requireContext()).jsonObject?.optInt("sn")
        if (sn != null) {
            prefsManager.setUserSn(sn)
        }

        latestRecSn = prefsManager.getLatestRecommendation()
        singletonMeasure = Singleton_t_measure.getInstance(requireContext()).measures
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

        binding.clM1.setOnClickListener{

            ssm = SaveSingletonManager(requireContext(), requireActivity())
            lifecycleScope.launch(Dispatchers.IO) {
                ssm.setRecent5MeasureResult(0)
                withContext(Dispatchers.Main) {
                    (activity as MainActivity).binding.bnbMain.selectedItemId = R.id.measure
                    // 다운로드 후 이동
                    requireActivity().supportFragmentManager.beginTransaction().apply {
                        replace(R.id.flMain, MeasureDetailFragment())
                        commit()
                    }
                }
            }
        }

        when (isNetworkAvailable(requireContext())) {
            true -> {
                measures = Singleton_t_measure.getInstance(requireContext()).measures


                // ------# 초기 measure 설정 #------
                if (!measures.isNullOrEmpty()) {
                    if (mvm.selectedMeasureDate.value == null) {
                        mvm.selectedMeasureDate.value = measures?.get(0)?.regDate
                    }
                    if (mvm.selectMeasureDate.value == null) {
                        mvm.selectMeasureDate.value = measures?.get(0)?.regDate
                    }

                    // ------# 측정결과 있을 때 도움말 툴팁 #------
                    if (isFirstRun("Tooltip_isFirstRun_existed")) {
                        existedMeasurementGuide()
                    }
                } else {
                    if (isFirstRun("Tooltip_isFirstRun_not_existed")) {
                        notExistedMeasurementGuide()
                    }
                }
                Log.v("선택된measureDate", "${mvm.selectedMeasureDate.value}")
                updateUI()

                binding.tvMMeasureDate.setOnClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }
            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }
    }

    // 상단 어댑터와 하단 어댑터 같이 나옴
    private fun setPartAdapter(index: Int) {
        val layoutManager1 = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvM1.layoutManager = layoutManager1
        val filteredParts = measures?.get(index)?.dangerParts?.filter { it.second == 1f || it.second == 2f}
        val partAdapter = MainPartRVAdapter(this@MainFragment, filteredParts?.toMutableList(), avm, "main")
        binding.rvM1.adapter = partAdapter
        binding.rvM1.isNestedScrollingEnabled = false
    }

    // 블러 유무 판단하기
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        Log.v("measure있는지", "${measures?.size}")

        if (measures.isNullOrEmpty()) {
            // ------# measure에 뭐라도 들어있으면 위 코드 #-------
            binding.tvMTitle.text = "${Singleton_t_user.getInstance(requireContext()).jsonObject?.getString("user_name") ?: ""}님"

            binding.tvMMeasureDate.visibility = View.GONE
            binding.tvMOverall.text = "-"
            binding.tvMMeasureResult1.text = "측정 데이터가 없습니다."
            binding.tvMMeasureResult2.text = "키오스크, 모바일에서 측정을 진행해주세요"
            binding.rvM1.visibility = View.GONE
            binding.tvM2.visibility = View.GONE
//            binding.tvMProgram.visibility = View.GONE


            binding.btnMProgram.apply {
                text = "측정 시작하기"
                setOnClickListener{
                    (activity as? MainActivity)?.launchMeasureSkeletonActivity()
                }
            }
        } else {
            measures?.let { measure ->
                if (measure.size > 0) {

                    binding.tvM2.visibility = View.VISIBLE
                    binding.tvMMeasureDate.visibility = View.VISIBLE
                    binding.rvM1.visibility = View.VISIBLE
                    binding.tvMTitle.text = "최근 측정 정보"
//                    binding.tvMProgram.visibility = View.VISIBLE
                    binding.btnMProgram.setOnClickListener {
                        requireActivity().supportFragmentManager.beginTransaction().apply {
                            replace(R.id.flMain, ProgramSelectFragment())
                            commit()
                        }
                    }
                    // ------# 바텀시트에서 변한 selectedMeasureDate 에 맞게 변함 #------


                    mvm.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                        try {
                            lifecycleScope.launch(Dispatchers.Main) {
                                Log.v("날짜 비교", "$selectedDate, ${measures!!.map { it.regDate }} ")
                                val dateIndex = measures?.indexOf(measures?.find { it.regDate == selectedDate })
                                if (dateIndex != null) {

                                    measures?.get(dateIndex)?.recommendations
                                    binding.tvMMeasureDate.text = measure[dateIndex].regDate.substring(0, 10)
                                    binding.tvMOverall.text = measure[dateIndex].overall.toString()
                                    setPartAdapter(dateIndex)


                                    // 목록을 만들 dangerParts담기
                                    avm.currentParts = mvm.selectedMeasure?.dangerParts?.map { it.first }

                                    val measureSize = measures?.get(dateIndex)?.dangerParts?.size
                                    if (measureSize != null) {
                                        if (measureSize > 1) {
                                            val summaryComments  = createMeasureComment(measures?.get(dateIndex)?.dangerParts)

                                            Log.v("써머리들어간 후", "$summaryComments")
                                            if (summaryComments.size > 1) {
                                                binding.tvMMeasureResult1.text = summaryComments[0]
                                                binding.tvMMeasureResult2.text = summaryComments[1]
                                            } else if (summaryComments.size == 1) {
                                                binding.tvMMeasureResult1.text = summaryComments[0]
                                                binding.tvMMeasureResult2.visibility = View.INVISIBLE
                                            } else {
                                                binding.tvMMeasureResult1.text = ""
                                            }
                                        }
                                    }
                                }

                                // progress가 들어가지 않은 recommendation이다 -> 혹시모르니까 그냥 data 받아오기
                                val progressRec = getRecommendationProgress(getString(R.string.API_recommendation), requireContext(), mvm.selectedMeasure?.sn ?: 0)
                                mvm.selectedMeasure?.recommendations = progressRec
                                Singleton_t_measure.getInstance(requireContext()).measures?.find { it.sn == mvm.selectedMeasure?.sn }?.recommendations = progressRec
//                                Log.v("날짜변경해도 잘들어가는지", "${mvm.selectedMeasureDate.value}, ${mvm.selectedMeasure?.regDate} ${mvm.selectedMeasure?.recommendations}")
                                setAdapter()
                            }
                        }  catch (e: IndexOutOfBoundsException) {
                            Log.e("MainIndex", "${e.message}")
                        } catch (e: IllegalArgumentException) {
                            Log.e("MainIllegalA", "${e.message}")
                        } catch (e: IllegalStateException) {
                            Log.e("MainIllegalS", "${e.message}")
                        }catch (e: NullPointerException) {
                            Log.e("MainNull", "${e.message}")
                        } catch (e: java.lang.Exception) {
                            Log.e("MainException", "${e.message}")
                        }

                    }
                }
            }
        }
    }
    private fun setAdapter() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val adapter = MainProgressRVAdapter(this@MainFragment, mvm.selectedMeasure?.recommendations ?: listOf())
        binding.rvM2.layoutManager = layoutManager
        binding.rvM2.adapter = adapter
        adapter.notifyDataSetChanged()
    }


    private fun existedMeasurementGuide() {
        binding.clM2.isEnabled = false
        binding.clM2.isClickable = false
        TooltipManager.createGuide(
            context = requireContext(),
            text = "최근 측정에서 나온\n위험 부위를 탭해서 확인해보세요",
            anchor = binding.rvM1,
            gravity = Gravity.BOTTOM,
            dismiss = {

                TooltipManager.createGuide(
                    context = requireContext(),
                    text = "탭하여 지난 측정 결과 선택하세요\n측정 결과와 지난 프로그램을 볼 수 있습니다",
                    anchor = binding.tvMMeasureDate,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                        TooltipManager.createGuide(
                            context = requireContext(),
                            text = "탭해서 현재 위험 부위와 관련된\n운동 프로그램을 시작할 수 있습니다",
                            anchor = binding.rvM2,
                            gravity = Gravity.TOP,
                            dismiss = {
                                binding.clM2.isEnabled = false
                                binding.clM2.isClickable = false
                            })
                    }
                )
            }
        )
    }

    private fun notExistedMeasurementGuide() {
        TooltipManager.createGuide(

            context = requireContext(),
            text = "가장 최근 측정 결과의 종합 점수입니다\n7가지 자세와 설문을 통해 종합적으로 산출됩니다",
            anchor = binding.tvMOverall,
            gravity = Gravity.BOTTOM,
            dismiss = {

                TooltipManager.createGuide(
                    context = requireContext(),
                    text = "측정을 완료하고 운동 프로그램을 추천받으세요",
                    anchor = binding.btnMProgram,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                    }
                )
            }
        )
    }
}