package com.tangoplus.tangoq.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.vo.MeasureVO
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.RvMeasureItemBinding
import com.tangoplus.tangoq.db.Singleton_t_measure
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.fragment.ExtendedFunctions.hideBadgeOnClick
import com.tangoplus.tangoq.fragment.MeasureDetailFragment
import com.tangoplus.tangoq.function.SaveSingletonManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasureHistoryRVAdapter(val fragment: Fragment, val measures: MutableList<MeasureVO>, private val viewModel : MeasureViewModel): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private lateinit var ssm : SaveSingletonManager
    inner class MHViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMIName: TextView = view.findViewById(R.id.tvMIName)
        val tvMIScore : TextView = view.findViewById(R.id.tvMIScore)
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
            holder.tvMIName.text = "${currentItem.regDate.substring(0, 10)}, ${currentItem.userName}"
            holder.tvMISub.text = "위험부위: ${currentItem.dangerParts.map { it.first }.joinToString()}"
            holder.tvMIScore.text = currentItem.overall.toString()
            val hideBadgeFunction = fragment.hideBadgeOnClick(holder.tvMIName, holder.clMI, "${holder.tvMIName.text}", ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

            holder.clMI.setOnClickListener {
                try {
                    // measure 선택하기
                    viewModel.selectedMeasure = currentItem
                    val dialog = LoadingDialogFragment.newInstance("측정파일")
                    dialog.show(fragment.requireActivity().supportFragmentManager, "LoadingDialogFragment")

                    ssm = SaveSingletonManager(fragment.requireContext(), fragment.requireActivity())
                    CoroutineScope(Dispatchers.IO).launch {
                        val currentMeasure = viewModel.selectedMeasure
                        val uriTuples = currentMeasure?.sn?.let { ssm.get1MeasureUrls(it) }
                        if (uriTuples != null) {
                            ssm.downloadFiles(uriTuples)
                            val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)
                            Log.v("리스너", "$editedMeasure")

                            // 파일 다운로드 후 url과 data (JSONARRAY) 넣기
                            val singletonMeasure = Singleton_t_measure.getInstance(fragment.requireContext()).measures
                            val singletonIndex = singletonMeasure?.indexOfLast { it.regDate == currentMeasure.regDate }
                            if (singletonIndex != null && singletonIndex >= 0) {
                                singletonMeasure.set(singletonIndex, editedMeasure)
                                viewModel.selectedMeasure = editedMeasure
                                Log.v("수정완료", "index: $singletonIndex, VO: $editedMeasure")
                                withContext(Dispatchers.Main) {
                                    dialog.dismiss()
                                    // 뱃지 제거
                                    hideBadgeFunction?.invoke()
                                    // 다운로드 후 이동
                                    fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                                        setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                                        replace(R.id.flMain, MeasureDetailFragment())
                                        addToBackStack(null)
                                        commit()
                                    }
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