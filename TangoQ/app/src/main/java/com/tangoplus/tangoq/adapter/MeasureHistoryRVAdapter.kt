package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.RvMeasureItemBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.hideBadgeOnClick
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.fragment.MeasureDetailFragment
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.viewmodel.FragmentViewModel
import com.tangoplus.tangoq.vo.DateDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasureHistoryRVAdapter(val fragment: Fragment, val measures: MutableList<MeasureVO>, private val viewModel : MeasureViewModel, private val fvm: FragmentViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var ssm : SaveSingletonManager
    inner class MHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMIName: TextView = view.findViewById(R.id.tvMIName)
        val tvMIScore : TextView = view.findViewById(R.id.tvMIScore)
        val tvMIType : TextView  = view.findViewById(R.id.tvMIType)
        val tvMISub : TextView = view.findViewById(R.id.tvMISub)
        val clMI : ConstraintLayout = view.findViewById(R.id.clMI)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = RvMeasureItemBinding.inflate(layoutInflater, parent, false)
        return MHViewHolder(binding.root)
    }

    override fun getItemCount(): Int {
        return measures.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentItem = measures[position]
        if (holder is MHViewHolder) {
            val regDate = currentItem.regDate.substring(0, 10) // , ${currentItem.userName}

            // 모바일, 키오스크 측정 판단
            val isMobileMeasure = if (currentItem.isMobile) {
                "모바일 앱 측정"
            } else {
                "키오스크 측정"
            }
            holder.tvMIName.text = regDate
            holder.tvMIType.text = isMobileMeasure
            val dangerPartsExplanation = "위험부위: ${currentItem.dangerParts.map { it.first }.joinToString()}"
            holder.tvMISub.text = dangerPartsExplanation
            holder.tvMIScore.text = currentItem.overall.toString()
            val hideBadgeFunction = fragment.hideBadgeOnClick(holder.tvMIName, holder.clMI, "${holder.tvMIName.text}", ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

            holder.clMI.setOnSingleClickListener {
                val loadingDialog = LoadingDialogFragment.newInstance("측정파일")
                loadingDialog.show(fragment.requireActivity().supportFragmentManager, "LoadingDialogFragment")
                try {
                    viewModel.selectedMeasure = currentItem
                    // 현재 선택한 fragment 저장


                    ssm = SaveSingletonManager(fragment.requireContext(), fragment.requireActivity(), viewModel)

                    fragment.lifecycleScope.launch(Dispatchers.IO) {
                        val currentMeasure = viewModel.selectedMeasure
                        val uriTuples = currentMeasure?.sn?.let { ssm.get1MeasureUrls(it) }
                        if (uriTuples != null) {
                            ssm.downloadFiles(uriTuples)
                            val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)

                            // 파일 다운로드 후 url과 data (JSONARRAY) 넣기
                            val singletonMeasure = Singleton_t_measure.getInstance(fragment.requireContext()).measures
                            val singletonIndex = singletonMeasure?.indexOfLast { it.regDate == currentMeasure.regDate }

                            withContext(Dispatchers.Main) {
                                if (singletonIndex != null && singletonIndex >= 0) {
                                    singletonMeasure.set(singletonIndex, editedMeasure)
                                    viewModel.selectedMeasure = editedMeasure
                                    viewModel.selectedMeasureDate.value = DateDisplay(editedMeasure.regDate, editedMeasure.regDate.substring(0, 11))
                                    viewModel.selectMeasureDate.value = DateDisplay(currentMeasure.regDate, currentMeasure.regDate.substring(0, 11))
                                    // 뱃지 제거
                                    hideBadgeFunction?.invoke()
                                    fvm.setCurrentFragment(FragmentViewModel.FragmentType.MEASURE_DETAIL_FRAGMENT)
                                    // 다운로드 후 이동
                                    loadingDialog.dismiss()
                                    fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                                        replace(R.id.flMain, MeasureDetailFragment())
                                        commit()
                                    }
                                    // 마지막 프레그먼트 저장
                                }
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e("measureHistoryError", "measureHistoryErrorIllegalState: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    Log.e("measureHistoryError", "measureHistoryErrorIllegalArgument: ${e.message}")
                } catch (e: NullPointerException) {
                    Log.e("measureHistoryError", "measureHistoryErrorNullPointer: ${e.message}")
                } catch (e: InterruptedException) {
                    Log.e("measureHistoryError", "measureHistoryErrorInterrupted: ${e.message}")
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("measureHistoryError", "measureHistoryErrorIndexOutOfBounds: ${e.message}")
                } catch (e: Exception) {
                    Log.e("measureHistoryError", "measureHistoryError: ${e.message}")
                }
            }
        }
    }
}