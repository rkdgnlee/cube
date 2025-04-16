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
import com.tangoplus.tangoq.vo.DateDisplay
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
            val regDate = currentItem.regDate.substring(0, 10) // , ${currentItem.userName}
            holder.tvMIName.text = regDate
            val dangerPartsExplanation = "위험부위: ${currentItem.dangerParts.map { it.first }.joinToString()}"
            holder.tvMISub.text = dangerPartsExplanation
            holder.tvMIScore.text = currentItem.overall.toString()
            val hideBadgeFunction = fragment.hideBadgeOnClick(holder.tvMIName, holder.clMI, "${holder.tvMIName.text}", ContextCompat.getColor(fragment.requireContext(), R.color.thirdColor))

            holder.clMI.setOnSingleClickListener {
                try {
                    viewModel.selectedMeasure = currentItem

                    ssm = SaveSingletonManager(fragment.requireContext(), fragment.requireActivity())
                    fragment.lifecycleScope.launch(Dispatchers.IO) {
//                        ssm.setRecent5MeasureResult(position)
                        val dialog = LoadingDialogFragment.newInstance("측정파일")
                        dialog.show(fragment.requireActivity().supportFragmentManager, "LoadingDialogFragment")

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
                                    // 다운로드 후 이동
                                    dialog.dismiss()
                                    fragment.requireActivity().supportFragmentManager.beginTransaction().apply {
                                        replace(R.id.flMain, MeasureDetailFragment())
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