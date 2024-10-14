package com.tangoplus.tangoq.dialog

import android.content.Context.INPUT_METHOD_SERVICE
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tangoplus.tangoq.adapter.ExerciseHistoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentExerciseSearchDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.listener.OnHistoryClickListener
import com.tangoplus.tangoq.listener.OnHistoryDeleteListener
import com.tangoplus.tangoq.`object`.Singleton_t_user
import java.util.Locale

class ExerciseSearchDialogFragment : DialogFragment(), OnHistoryDeleteListener, OnHistoryClickListener {
    lateinit var binding : FragmentExerciseSearchDialogBinding
    private val viewModel : ExerciseViewModel by activityViewModels()
    private lateinit var prefsManager : PreferencesManager
    private var isKeyboardVisible = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseSearchDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefsManager = PreferencesManager(requireContext())
        binding.clESDEmpty.visibility = View.GONE
        viewModel.searchHistory.value?.clear()

        // ------# 검색에 포커스 #------
        binding.etESDSearch.requestFocus()
        binding.etESDSearch.postDelayed({
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etESDSearch, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
        val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        imm!!.hideSoftInputFromWindow(view.windowToken, 0)
        isKeyboardVisible = true


        binding.ibtnESDClear.setOnClickListener{
            binding.etESDSearch.setText("")
            binding.rv1.adapter = null
        }

        for (i in prefsManager.getLastSn() downTo 1) {
            if (prefsManager.getStoredHistory(i) != "") {
                viewModel.searchHistory.value?.add(Pair(i ,prefsManager.getStoredHistory(i)))
                Log.v("storedHistory", prefsManager.getStoredHistory(i))
            }

        }
        Log.v("searchHistory", "${viewModel.searchHistory.value}")


        var adapter2 = ExerciseHistoryRVAdapter(viewModel.searchHistory.value!!, this@ExerciseSearchDialogFragment, this@ExerciseSearchDialogFragment)
        setAdapter(adapter2, binding.rv2)


        binding.etESDSearch.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {

                // ------# 첫번쨰 rv 필터링 하기 #------
                val query = s.toString()
                val filteredList = mutableListOf<ExerciseVO>()
                if (query.isNotEmpty()) {
                    val filteredPattern = query.toLowerCase(Locale.getDefault()).trim()
                    for (indices in viewModel.allExercises) {
                        if (indices.exerciseName?.toLowerCase(Locale.getDefault())?.contains(filteredPattern) == true) {
                            filteredList.add(indices)
                        }
                    }

                    val adapter1 = ExerciseRVAdapter(this@ExerciseSearchDialogFragment, filteredList, mutableListOf(), Pair(0,0) ,"main")
                    setAdapter(adapter1, binding.rv1)
                    if (filteredList.isEmpty()) binding.clESDEmpty.visibility = View.VISIBLE else binding.clESDEmpty.visibility = View.GONE
                }
            }
        })

        viewModel.searchHistory.observe(viewLifecycleOwner) {
            adapter2 = ExerciseHistoryRVAdapter(it, this@ExerciseSearchDialogFragment, this@ExerciseSearchDialogFragment)
            setAdapter(adapter2, binding.rv2)
        }

        val rootView = binding.root
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height

            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardNowVisible = keypadHeight > screenHeight * 0.15

            if (isKeyboardVisible && !isKeyboardNowVisible && binding.etESDSearch.text.isNotEmpty()) {
                prefsManager.setStoredHistory(binding.etESDSearch.text.toString())
                Log.v("saveHistory", "sn: ${prefsManager.getLastSn()}, history: ${prefsManager.getStoredHistory(prefsManager.getLastSn())}")

            }

            isKeyboardVisible = isKeyboardNowVisible
        }

        binding.btnESDCategory.setOnClickListener{ dismiss() }
        binding.tvESDClear.setOnClickListener {
            prefsManager.deleteAllHistory()
            binding.rv2.adapter = null
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun setAdapter(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>, rv: RecyclerView) {
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        rv.layoutManager = layoutManager
        rv.adapter = adapter
    }

    override fun onHistoryDelete(history: Pair<Int,String>) {
        viewModel.searchHistory.value?.remove(viewModel.searchHistory.value!!.find { it.second == history.second })
        prefsManager.deleteStoredHistory(history.first)
        binding.rv2.adapter?.notifyDataSetChanged()
    }

    override fun onHistoryClick(history: String) {
        binding.etESDSearch.setText(history)
        binding.etESDSearch.requestFocus()
    }

}