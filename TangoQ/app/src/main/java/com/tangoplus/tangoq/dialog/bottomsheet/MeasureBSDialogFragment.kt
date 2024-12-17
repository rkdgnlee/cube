package com.tangoplus.tangoq.dialog.bottomsheet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.databinding.FragmentMeasureBSDialogBinding
import com.tangoplus.tangoq.dialog.LoadingDialogFragment
import com.tangoplus.tangoq.function.SaveSingletonManager
import com.tangoplus.tangoq.db.Singleton_t_measure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeasureBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding: FragmentMeasureBSDialogBinding
    lateinit var singletonMeasure : Singleton_t_measure
    private lateinit var ssm : SaveSingletonManager
    val mvm : MeasureViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeasureBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singletonMeasure = Singleton_t_measure.getInstance(requireContext())
        ssm = SaveSingletonManager(requireContext(), requireActivity())
        val measures = singletonMeasure.measures

        val dates = measures?.let { measure ->
            List(measure.size) { i ->
                measure.get(i).regDate
            }
        } ?: emptyList()

        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvMBSD.layoutManager = layoutManager
        val adapter = StringRVAdapter(this@MeasureBSDialogFragment, dates.toMutableList(), "measure",  mvm)
        binding.rvMBSD.adapter = adapter

        setupButtons()
        binding.btnMBSD.setOnClickListener {
            mvm.selectedMeasureDate.value = mvm.selectMeasureDate.value
            val dialog = LoadingDialogFragment.newInstance("측정파일")
            dialog.show(activity?.supportFragmentManager ?: return@setOnClickListener, "LoadingDialogFragment")

            // ------# 로딩을 통해 파일 가져오기 #------
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentMeasure = singletonMeasure.measures?.find { it.regDate == mvm.selectedMeasureDate.value }
                    val uriTuples = currentMeasure?.sn?.let { it1 -> ssm.get1MeasureUrls(it1) }
                    if (uriTuples != null) {
                        Log.v("리스너1", "$uriTuples")
                        ssm.downloadFiles(uriTuples)
                        val editedMeasure = ssm.insertUrlToMeasureVO(uriTuples, currentMeasure)
                        Log.v("리스너2", "$editedMeasure")

                        // singleton의 인덱스 찾아서 ja와 값 넣기
                        val singletonIndex = singletonMeasure.measures?.indexOfLast { it.regDate == mvm.selectedMeasureDate.value }
                        if (singletonIndex != null && singletonIndex >= 0) {
                            singletonMeasure.measures?.set(singletonIndex, editedMeasure)
                            mvm.selectedMeasure = editedMeasure
                            Log.v("수정완료", "index: $singletonIndex, VO: $editedMeasure")
                            withContext(Dispatchers.Main) {
                                dialog.dismiss()
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    Log.e("measureSelectError", "MeasureBSIllegalState: ${e.message}")
                } catch (e: IllegalArgumentException) {
                    Log.e("measureSelectError", "MeasureBSIllegalArgument: ${e.message}")
                } catch (e: NullPointerException) {
                    Log.e("measureSelectError", "MeasureBSNullPointer: ${e.message}")
                } catch (e: InterruptedException) {
                    Log.e("measureSelectError", "MeasureBSInterrupted: ${e.message}")
                } catch (e: IndexOutOfBoundsException) {
                    Log.e("measureSelectError", "MeasureBSIndexOutOfBounds: ${e.message}")
                } catch (e: Exception) {
                    Log.e("measureSelectError", "MeasureBS: ${e.message}")
                } finally {
                    withContext(Dispatchers.Main) {
                        if (dialog.isAdded && dialog.isVisible) {
                            dialog.dismiss()
                        }
                    }
                }

            }
            Log.w("selectedMeasureDate", "selectedMeasure: ${mvm.selectedMeasureDate.value}, selectMeasure: ${mvm.selectMeasureDate.value}")
            Log.w("selectedMeasure", "${mvm.selectedMeasure}")
            dismiss()
        }
    }

    private fun setupButtons() {
        binding.ibtnMBSDExit.setOnClickListener { dismiss() }

    }
}