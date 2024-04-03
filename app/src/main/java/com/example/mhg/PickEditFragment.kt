package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickEditBinding
import com.example.mhg.`object`.NetworkExerciseService.updatePickItemJson
import com.example.mhg.`object`.Singleton_t_user
import org.json.JSONObject


class PickEditFragment : Fragment() {
    lateinit var binding: FragmentPickEditBinding
    val viewModel: ExerciseViewModel by activityViewModels()

    var title = ""
    companion object {
        private const val ARG_TITLE = "title"
        fun newInstance(title: String): PickEditFragment {
            val fragment = PickEditFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickEditBinding.inflate(inflater)
        return binding.root
    }
    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! 데이터 선언 !-----
        val appClass = requireContext().applicationContext as AppClass
        val t_userData = Singleton_t_user.getInstance(requireContext())
        title = requireArguments().getString(ARG_TITLE).toString()
        binding.etPickEditName.setText(title)

        // -----! 초기 편집에 들어왔을 때 셋팅 시작 !-----
        val currentPickItem = appClass.pickItems.value?.find { it.pickName == title }
        if (currentPickItem != null) {
            binding.etPickEditName.setText(currentPickItem.pickName.toString())
            binding.etPickEditExplainTitle.setText(currentPickItem.pickExplainTitle.toString())
            binding.etPickEditExplain.setText(currentPickItem.pickExplainTitle.toString())
        }  // -----! 초기 편집에 들어왔을 때 셋팅 끝 !-----

//        val currentPickItem = appClass.pickItems.value?.get(appClass.pickList.value!!.indexOf(title))
        if (currentPickItem?.exercises != null) {
            val adapter = HomeVerticalRecyclerViewAdapter(currentPickItem.exercises!!, "add")
            binding.rvPickEdit.adapter = adapter
            viewModel.exerciseUnits.value = currentPickItem.exercises
        }
        // -----! EditText 등 제목 VM 연동 시작 !-----
        binding.etPickEditName.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pickEditItem.value?.put("favorite_name", s.toString())
            }
        })
        binding.etPickEditExplainTitle.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pickEditItem.value?.put("favorite_explain_title", s.toString())
            }
        })

        binding.etPickEditExplain.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.pickEditItem.value?.put("favorite_explain", s.toString())
            }
        })

        // 수정된 것들 임시로 VM에 담고 가져오기
        binding.etPickEditName.setText(viewModel.pickEditItem.value?.optString("favorite_name").toString())
        binding.etPickEditExplainTitle.setText(viewModel.pickEditItem.value?.optString("favorite_explain_title").toString())
        binding.etPickEditExplain.setText(viewModel.pickEditItem.value?.optString("favorite_explain").toString())
        // -----! EditText 등 제목 VM 연동 끝 !-----

        viewModel.exerciseUnits.observe(viewLifecycleOwner) { basketUnits ->
            val adapter = HomeVerticalRecyclerViewAdapter(basketUnits, "add")
            adapter.verticalList = basketUnits
            val linearLayoutManager2 =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPickEdit.layoutManager = linearLayoutManager2

            // -----! item에 swipe 및 dragNdrop 연결 !-----
            val callback = ItemTouchCallback(adapter).apply {
//                setClamp(260f)
//                removePreviousClamp(binding.rvPickadd)
            }
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(binding.rvPickEdit)
            binding.rvPickEdit.adapter = adapter
            adapter.startDrag(object : HomeVerticalRecyclerViewAdapter.OnStartDragListener {
                override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                    touchHelper.startDrag(viewHolder)
                }
            })
            adapter.notifyDataSetChanged()
            // -----! swipe, drag 연동 !-----
            if (viewModel.exerciseUnits.value?.size != 0 ) {
                binding.ivPickEditNull.visibility = View.GONE
            } else {
                binding.ivPickEditNull.visibility = View.VISIBLE
            }
        }

        binding.nsvPickEdit.isNestedScrollingEnabled = true
        binding.rvPickEdit.isNestedScrollingEnabled = false
        binding.rvPickEdit.overScrollMode = View.OVER_SCROLL_NEVER

        // -----! 장바구니로 가기 !-----
        binding.tvPickEditGoBasket.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, PickBasketFragment.newInstance(title))
                    .addToBackStack(null)
                    .commit()
            }
            it.isClickable = true
        }

        binding.tvPickEditBack.setOnClickListener {
            if (!it.isClickable) { return@setOnClickListener }
            it.isClickable = false
            requireActivity().supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, PickDetailFragment.newInstance(title))
                commit()
            }
            viewModel.exerciseUnits.value?.clear()
            it.isClickable = true
        }
        // -----! 즐겨찾기 하나 만들기 시작 !-----
        binding.btnPickEditFinish.setOnClickListener {
            updatePickEdit()
            requireActivity().supportFragmentManager.beginTransaction().apply {
                setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                replace(R.id.flMain, PickDetailFragment.newInstance(title))
                commit()
            }
        } // -----! 즐겨찾기 하나 만들기 끝 !-----
    }
    private fun updatePickEdit() {

        // -----! 데이터 갈무리 후 appClass 수정 시작 !-----
        val appClass = requireContext().applicationContext as AppClass
        val index = appClass.pickItems.value?.indexOfFirst { it.pickName == title }

        val pickItem = PickItemVO(
            pickSn = index?.let { appClass.pickItems.value!![it].pickSn.toString() },
            pickName = binding.etPickEditName.text.toString(),
            pickExplainTitle = binding.etPickEditExplainTitle.text.toString(),
            pickExplain = binding.etPickEditExplain.text.toString(),
            exercises = viewModel.exerciseUnits.value?.toMutableList(),
        )
        Log.w("PickItemSave", "시리얼넘버: ${pickItem.pickSn}, 운동들: ${pickItem.exercises}")

        if (index != null) {
            appClass.pickItems.value?.set(index, pickItem)
            appClass.pickList.value?.set(index, pickItem.pickName.toString())
            title = pickItem.pickName.toString()
        }
        viewModel.exerciseUnits.value?.clear()
        // -----! 데이터 갈무리 후 appClass 수정 끝 !-----

        val descriptionIdList = mutableListOf<String>()
        for (i in 0 until (viewModel.exerciseUnits.value?.size ?: Log.w(TAG, "unvalid Data"))) {
            descriptionIdList.add(viewModel.exerciseUnits.value?.get(i)?.exerciseDescriptionId.toString())
        }

        // -----! json으로 변환 후 update 시작 !-----
        val JsonObj = JSONObject()
        JsonObj.put("favorite_name", pickItem.pickName)
        JsonObj.put("exercise_description_ids", descriptionIdList)
        JsonObj.put("favorite_explain_title", pickItem.pickExplainTitle)
        JsonObj.put("favorite_explain", pickItem.pickExplain)
        updatePickItemJson(getString(R.string.IP_ADDRESS_t_favorite), pickItem.pickSn.toString(), JsonObj.toString()) {
            // TODO 반환된 값으로 뭐 해도 되고 안해도 됨.
        }

        // -----! json으로 변환 후 update 시작 !-----

    }
}