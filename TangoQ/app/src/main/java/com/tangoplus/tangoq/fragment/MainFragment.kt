package com.tangoplus.tangoq.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.tangoplus.tangoq.adapter.PartRVAdapter
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMainBinding
import com.tangoplus.tangoq.dialog.AlarmDialogFragment
import com.tangoplus.tangoq.dialog.GuideDialogFragment
import com.tangoplus.tangoq.dialog.QRCodeDialogFragment
import com.tangoplus.tangoq.dialog.bottomsheet.MeasureBSDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.isFirstRun
import com.tangoplus.tangoq.api.DeviceService.isNetworkAvailable
import com.tangoplus.tangoq.api.NetworkRecommendation.getRecommendationProgress
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.fragment.ExtendedFunctions.scrollToView
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.MeasurementManager.createMeasureComment
import com.tangoplus.tangoq.viewmodel.AnalysisViewModel
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.datastore.core.IOException
import com.tangoplus.tangoq.db.MeasureDatabase
import com.tangoplus.tangoq.fragment.ExtendedFunctions.createGuide
import com.tangoplus.tangoq.function.MeasurementManager.createResultComment
import com.tangoplus.tangoq.function.MeasurementManager.extractVideoCoordinates
import com.tangoplus.tangoq.viewmodel.FragmentViewModel
import com.tangoplus.tangoq.vo.DataDynamicVO
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.SocketTimeoutException

class MainFragment : Fragment() {
    lateinit var binding: FragmentMainBinding
    private val avm by activityViewModels<AnalysisViewModel>()
    private val mvm : MeasureViewModel by activityViewModels()
    private val pvm: ProgressViewModel by activityViewModels()
    private val fvm : FragmentViewModel by activityViewModels()
    private lateinit var startForResult: ActivityResultLauncher<Intent>
    private lateinit var prefsManager : PreferencesManager
    private var measures : MutableList<MeasureVO>? = null
    private var singletonMeasure : MutableList<MeasureVO>? = null
    private var latestRecSn = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (pvm.fromProgramCustom) {
            renderProgramRV()
            pvm.fromProgramCustom = false
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
        binding.ibtnMAlarm.setOnSingleClickListener {
            val dialog = AlarmDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "AlarmDialogFragment")
        }

        binding.ibtnMQRCode.setOnSingleClickListener{
            val dialog = QRCodeDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "LoginScanDialogFragment")
        }

        if (isFirstRun("GuideDialogFragment_isFirstRun")) {
            val dialog = GuideDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "GuideDialogFragment")
        }

        binding.ivMRefresh.setOnSingleClickListener {
            renderProgramRV()
        }

        when (isNetworkAvailable(requireContext())) {
            true -> {
                measures = Singleton_t_measure.getInstance(requireContext()).measures

                // ------# 초기 measure 설정 #------
                if (!measures.isNullOrEmpty()) {
//                    if (mvm.selectedMeasureDate.value == null) {
//                        mvm.selectedMeasureDate.value =
//                            measures?.let { avm.createDateDisplayList(it).get(0) }
//                    }
//                    if (mvm.selectMeasureDate.value == null) {
//                        mvm.selectMeasureDate.value = measures?.let { avm.createDateDisplayList(it).get(0) }
//                    }
                    val setNavToMD = listOf(binding.clM1, binding.tvMOverall)
                    setNavToMD.forEach {
                        it.setOnSingleClickListener {
                            (activity as MainActivity).binding.bnbMain.selectedItemId = R.id.measure
                            // 다운로드 후 이동

                            requireActivity().supportFragmentManager.beginTransaction().apply {
                                replace(R.id.flMain, MeasureDetailFragment())
                                commit()
                            }
                        }
                    }
                    // ------# 측정결과 있을 때 도움말 툴팁 #------
                    if (isFirstRun("Tooltip_isFirstRun_existed_${Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid")}")) {
                        existedMeasurementGuide()
                    }
                } else {
                    if (isFirstRun("Tooltip_isFirstRun_not_existed_${Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid")}")) {
                        notExistedMeasurementGuide()
                    }
                    binding.cvMResult1.visibility =View.INVISIBLE
                    binding.cvMResult2.visibility = View.INVISIBLE
                }
                updateUI()

                binding.tvMMeasureDate.setOnSingleClickListener {
                    val dialog = MeasureBSDialogFragment()
                    dialog.show(requireActivity().supportFragmentManager, "MeasureBSDialogFragment")
                }
            }
            false -> {
                // ------# 인터넷 연결이 없을 때 #------
            }
        }

//        lifecycleScope.launch {
//            avm.mdMeasureResult = mvm.selectedMeasure?.measureResult?.optJSONArray(1) ?: JSONArray()
//            // 비디오 사이즈 6개넣어서 그대로 씀.
//            val connections = listOf(25, 26) // 좌측 골반의 pose번호를 가져옴
//            val titleList = listOf("좌측 무릎", "우측 무릎")
//            val coordinates = extractVideoCoordinates(avm.mdMeasureResult)
//            val dataDynamicVOList = mutableListOf<DataDynamicVO>()
//
//            for (i in connections.indices step 2) {
//                val connection1 = connections[i]
//                val connection2 = connections[i + 1]
//                val title1 = titleList[i]
//                val title2 = titleList[i + 1]
//
//                val filteredCoordinate1 = mutableListOf<Pair<Float, Float>>()
//                val filteredCoordinate2 = mutableListOf<Pair<Float, Float>>()
//
//                for (element in coordinates) {
//                    // 단순히 해당 인덱스의 좌표를 가져와서 추가
//                    filteredCoordinate1.add(element[connection1])
//                    filteredCoordinate2.add(element[connection2])
//                }
//
//                val dataDynamicVO = DataDynamicVO(
//                    data1 = filteredCoordinate1,
//                    title1 = title1,
//                    data2 = filteredCoordinate2,
//                    title2 = title2
//                )
//                dataDynamicVOList.add(dataDynamicVO)
//            }
//            val md = MeasureDatabase.getDatabase(requireContext())
//            val mDao = md.measureDao()
//            withContext(Dispatchers.IO) {
//                val allInfo = Singleton_t_user.getInstance(requireContext()).jsonObject?.optString("user_uuid")?.let { mDao.getAllInfo(it) }
//                val currentInfo = allInfo?.get(2)
//                Log.v("currentInfo", "$currentInfo")
//                val statics = currentInfo?.sn?.let { mDao.getStaticsBy1Info(it) }
//                val dynamics = Pair(dataDynamicVOList.flatMap{ it.data1} , dataDynamicVOList.flatMap { it.data2 })
//                if (currentInfo != null && statics != null) {
//                    val commen = createResultComment(currentInfo,  statics, dynamics)
//                    Log.v("comment", commen)
//                }
//            }
//        }
    }

    // 상단 어댑터와 하단 어댑터 같이 나옴
    private fun setPartAdapter(index: Int) {
        val layoutManager1 = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvM1.layoutManager = layoutManager1
        val filteredParts = measures?.get(index)?.dangerParts?.filter { it.second == 1f || it.second == 2f}
        val partAdapter = PartRVAdapter(this@MainFragment, filteredParts?.toMutableList(), avm, fvm,"main")
        binding.rvM1.adapter = partAdapter
        binding.rvM1.isNestedScrollingEnabled = false
    }

    // 블러 유무 판단하기
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
//        Log.v("measure있는지", "${measures?.size}")
        startShimmer()
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
            stopShimmer()
            binding.btnMProgram.apply {
                text = "측정 시작하기"
                setOnSingleClickListener{
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

                    // ------# 바텀시트에서 변한 selectedMeasureDate 에 맞게 변함 #------


                    mvm.selectedMeasureDate.observe(viewLifecycleOwner) { selectedDate ->
                        try {
                            lifecycleScope.launch(Dispatchers.Main) {

//                                Log.v("날짜 비교", "$selectedDate, ${measures!!.map { it.regDate }} ")
                                val dateIndex = measures?.indexOf(measures?.find { it.regDate == selectedDate.fullDateTime })
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

//                                            Log.v("써머리들어간 후", "$summaryComments")
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

                                renderProgramRV()
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

    private fun renderProgramRV() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                startShimmer()
                binding.rvM2.visibility = View.GONE

                binding.clMRefresh.visibility = View.GONE
                // progress가 들어가지 않은 recommendation이다 -> 혹시모르니까 그냥 data 받아오기
                val progressRec = getRecommendationProgress(getString(R.string.API_recommendation), requireContext(), mvm.selectedMeasure?.sn ?: 0)
                mvm.selectedMeasure?.recommendations = progressRec
                Singleton_t_measure.getInstance(requireContext()).measures?.find { it.sn == mvm.selectedMeasure?.sn }?.recommendations = progressRec
                setAdapter()
                binding.rvM2.visibility = View.VISIBLE
            } catch (e: IOException) {
                binding.clMRefresh.visibility = View.VISIBLE
                Log.e("renderProgram", "IOException: ${e.message}")
            } catch (e: SocketTimeoutException) {
                binding.clMRefresh.visibility = View.VISIBLE
                Log.e("renderProgram", "IOException: ${e.message}")
            } catch (e: Exception) {
                binding.clMRefresh.visibility = View.VISIBLE
                Log.e("renderProgram", "IOException: ${e.message}")
            } finally {
                stopShimmer()
            }
        }
    }


    private fun setAdapter() {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
//        Log.w("rec갯수", "${mvm.selectedMeasure?.recommendations?.size}, ${mvm.selectedMeasure?.recommendations?.map { it.title }}")
        val adapter = MainProgressRVAdapter(this@MainFragment,  mvm.selectedMeasure?.recommendations?: listOf(), pvm)
        binding.rvM2.layoutManager = layoutManager
        binding.rvM2.adapter = adapter
        binding.btnMProgram.text = when {
            pvm.isExpanded -> "접기"
            else -> "더보기"
        }
        binding.btnMProgram.setOnSingleClickListener {
            if (binding.btnMProgram.text == "더보기") {
                Handler(Looper.getMainLooper()).postDelayed({
                    scrollToView(binding.btnMProgram, binding.nsvM)
                }, 250)
                binding.btnMProgram.text = "접기"
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    scrollToView(binding.rvM1, binding.nsvM)
                }, 250)
                binding.btnMProgram.text = "더보기"
            }
            adapter.toggleExpand(binding.rvM2)

        }
    }

    private fun startShimmer() {
        binding.sflM.startShimmer()
        binding.sflM.visibility = View.VISIBLE
    }
    private fun stopShimmer() {
        binding.sflM.stopShimmer()
        binding.sflM.visibility = View.GONE
    }

    private fun existedMeasurementGuide() {
        binding.clM2.isEnabled = false
        binding.clM2.isClickable = false
        context.let { safeContext ->
            if (safeContext != null) {
                createGuide(
                    context = safeContext,
                    text = "최근 측정에서 나온\n위험 부위를 탭해서 확인해보세요",
                    anchor = binding.rvM1,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                        if (safeContext != null) {
                            createGuide(
                                context = safeContext,
                                text = "탭하여 지난 측정 결과 선택하세요\n측정 결과와 지난 프로그램을 볼 수 있습니다",
                                anchor = binding.tvMMeasureDate,
                                gravity = Gravity.BOTTOM,
                                dismiss = {
                                    if (safeContext != null) {
                                        createGuide(
                                            context = safeContext,
                                            text = "탭해서 현재 위험 부위와 관련된\n운동 프로그램을 시작할 수 있습니다",
                                            anchor = binding.rvM2,
                                            gravity = Gravity.TOP,
                                            dismiss = {
                                                binding.clM2.isEnabled = false
                                                binding.clM2.isClickable = false
                                            }
                                        )
                                    }
                                }
                            )
                        }

                    }
                )
            }

        }
    }

    private fun notExistedMeasurementGuide() {
        context.let { safeContext ->
            if (safeContext != null) {
                createGuide(
                    context = requireContext(),
                    text = "가장 최근 측정 결과의 종합 점수입니다\n7가지 자세와 설문을 통해 종합적으로 산출됩니다",
                    anchor = binding.tvMOverall,
                    gravity = Gravity.BOTTOM,
                    dismiss = {
                        if (safeContext != null) {
                            createGuide(
                                context = requireContext(),
                                text = "측정을 시작해서 몸의 균형상태를 확인해 보세요",
                                anchor = binding.btnMProgram,
                                gravity = Gravity.BOTTOM,
                                dismiss = {
                                }
                            )
                        }
                    }
                )
            }
        }
    }

}