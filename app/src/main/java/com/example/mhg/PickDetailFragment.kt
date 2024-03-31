package com.example.mhg

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickDetailBinding
import com.example.mhg.`object`.NetworkExerciseService
import com.example.mhg.`object`.Singleton_t_user
import kotlinx.coroutines.launch


class PickDetailFragment : Fragment() {
    lateinit var binding : FragmentPickDetailBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    lateinit var title : String

    private lateinit var startForResult: ActivityResultLauncher<Intent>
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): PickDetailFragment {
            val fragment = PickDetailFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {


            }
        }

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickDetailBinding.inflate(inflater)

        val appClass = requireContext().applicationContext as AppClass
        val currentItem = appClass.pickItems.value?.find { it.pickName == title }
        if (currentItem != null) {
            if (currentItem.exercises!!.size != 0) {
                binding.ivPickDetailNull.visibility = View.GONE
            } else {
                binding.ivPickDetailNull.visibility = View.VISIBLE
            }
        }
        return binding.root

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nsvPickDetail.isNestedScrollingEnabled = true
        binding.rvPickDetail.isNestedScrollingEnabled = false
        binding.rvPickDetail.overScrollMode = View.OVER_SCROLL_NEVER

        title = requireArguments().getString(ARG_TITLE).toString()
        binding.actPickDetail.setText(title)


        // -----! singleton에서 전화번호 가져오기 시작 !-----
        val t_userData = Singleton_t_user.getInstance(requireContext())
        val user_mobile = t_userData.jsonObject?.optString("user_mobile")
        // -----! singleton에서 전화번호 가져오기 끝 !-----



        // ----- 운동 picklist, 제목 가져오기 시작 -----
        val appClass = requireContext().applicationContext as AppClass
        val pickList = mutableListOf<String>()
        appClass.pickList.observe(viewLifecycleOwner) { jsonArray ->
            pickList.clear()
            for (i in 0 until jsonArray.size) {
                pickList.add(jsonArray[i])
            }
            setPickDetail()
        }



        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, pickList)
        binding.actPickDetail.setAdapter(adapter)
        binding.actPickDetail.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {  }
            override fun afterTextChanged(s: Editable?) {
                title = s.toString()
                setPickDetail()

            }
        })
        // ----- 운동 picklist, 제목 가져오기 끝 -----

        // -----! 즐겨찾기로 운동 시작 !-----
        binding.btnPickStart.setOnClickListener {
            val resourceList = StorePickUrl()
            Log.w("list in intent", "$resourceList")
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putStringArrayListExtra("resourceList", ArrayList(resourceList))
            startForResult.launch(intent)


        }
        // -----! 즐겨찾기로 운동 끝 !-----

        binding.btnPickDetailBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, PickFragment())
                commit()
            }
            it.isClickable = true
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun setPickDetail() {

        val appClass = requireContext().applicationContext as AppClass
        val currentItem = appClass.pickItems.value?.get(appClass.pickList.value!!.indexOf(title))

        if (currentItem != null) {
            binding.tvPickDetailExplainTitle.text = currentItem.pickExplainTitle.toString()
            binding.tvPickDetailExplain.text = currentItem.pickExplain.toString()
            val RvAdapter = HomeVerticalRecyclerViewAdapter(currentItem.exercises!!, "type")
            RvAdapter.verticalList = currentItem.exercises
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickDetail.layoutManager = linearLayoutManager2
            binding.rvPickDetail.adapter = RvAdapter
            RvAdapter.notifyDataSetChanged()

            binding.tvPickDetailUnitNumber.text = currentItem.exercises.size.toString()

            var totalTime = 0
            for (i in 0 until currentItem.exercises.size) {
                val exercises = currentItem.exercises.get(i)
                Log.w("운동각 시간" ,"${exercises.videoTime!!.toInt()}")
                totalTime += exercises.videoTime!!.toInt()
            }
            Log.w("총 시간", "$totalTime")
            binding.tvPickDetailUnitTime.text = (totalTime.div(60)).toString()
        }
    }

    private fun StorePickUrl() : MutableList<String> {
        val resourceList = mutableListOf<String>()
        val currentItem = viewModel.pickItems.value?.find { it.pickName == title }
        for (i in 0 until currentItem?.exercises!!.size) {
            val exercises = currentItem.exercises.get(i)
            resourceList.add(exercises.videoFilepath.toString())
            Log.w("url연속으로 다 들어갔는지", "$resourceList")
        }
        return  resourceList
    }
}
