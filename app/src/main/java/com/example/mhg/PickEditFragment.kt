package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mhg.Adapter.HomeVerticalRecyclerViewAdapter
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickEditBinding
import com.example.mhg.`object`.Singleton_t_user


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
        }

        // -----! 초기 편집에 들어왔을 때 셋팅 끝 !-----

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
        }
    }
    private fun updatePickEdit() {

        // -----! 데이터 갈무리 후 appClass 수정 시작 !-----
        val appClass = requireContext().applicationContext as AppClass
        val pickItem = PickItemVO(
            pickName = binding.etPickEditName.text.toString(),
            pickExplainTitle = binding.etPickEditExplainTitle.text.toString(),
            pickExplain = binding.etPickEditExplain.text.toString(),
            exercises = viewModel.exerciseUnits.value,
        )

        val index = appClass.pickItems.value?.indexOfFirst { it.pickName == title }
        if (index != null && index != -1) {
            appClass.pickItems.value?.set(index, pickItem)
            appClass.pickList.value?.set(index, pickItem.pickName.toString())
            title = pickItem.pickName.toString()
        }
        // -----! 데이터 갈무리 후 appClass 수정 끝 !-----

        val descriptionIdList = mutableListOf<String>()
        for (i in 0 until (viewModel.exerciseUnits.value?.size ?: Log.w(TAG, "unvalid Data"))) {
            descriptionIdList.add(viewModel.exerciseUnits.value?.get(i)?.exerciseDescriptionId.toString())
        }



    }
}