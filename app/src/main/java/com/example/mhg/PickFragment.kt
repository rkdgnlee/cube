package com.example.mhg

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.PickRecyclerViewAdapter
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.ExerciseViewModel
import com.example.mhg.VO.PickItemVO
import com.example.mhg.databinding.FragmentPickBinding
import com.example.mhg.`object`.NetworkExerciseService.fetchPickItemsJsonByMobile
import com.example.mhg.`object`.Singleton_t_user
import kotlinx.coroutines.launch


class PickFragment : Fragment(), onPickDetailClickListener {
    lateinit var binding : FragmentPickBinding
    val viewModel : ExerciseViewModel by activityViewModels()
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPickBinding.inflate(inflater)

        // -----! singleton에서 전화번호 가져오기 시작 !-----
        val t_userData = Singleton_t_user.getInstance(requireContext())
        val user_mobile = t_userData.jsonObject?.optString("user_mobile")
        // -----! singleton에서 전화번호 가져오기 끝 !-----

        lifecycleScope.launch {

            // -----! 핸드폰 번호로 PickItems 가져오기 시작 !-----
            val allDataList = fetchPickItemsJsonByMobile(getString(R.string.IP_ADDRESS_t_favorite), "01011112222") // user_mobile 넣기
            val appClass = requireContext().applicationContext as AppClass

            // -----! appClass list관리 시작 !-----
            if (allDataList != null) {
                appClass.pickList.value?.clear()
                appClass.pickItems.value?.clear()
                viewModel.exerciseUnits.value?.clear()
                for (i in 0 until allDataList.length()) {
                    appClass.pickList.value?.add(allDataList.getJSONObject(i).getString("favorite_name"))
                    val pickItemVO = PickItemVO(
                        pickName = allDataList.getJSONObject(i).optString("favorite_name"),
                        pickExplain = allDataList.getJSONObject(i).optString("favorite_name"),
                        pickExplainTitle = allDataList.getJSONObject(i).optString("favorite_name"),
                        pickDisclosure = allDataList.getJSONObject(i).optString("favorite_name"),
//                        exercises = allDataList.getJSONObject(i).optString("exercise_description_ids")
                        exercises = mutableListOf()
                    )
                    appClass.pickItems.value?.add(pickItemVO)
                    Log.w("$TAG, pickitem", "${appClass.pickItems.value}")
                        // TODO 여기다가 받아온 값 넣으면 됨
                }
            }
            appClass.pickList.observe(viewLifecycleOwner) { jsonArray ->
                // 아무것도 없을 때 나오는 캐릭터
//                if (jsonArray.size != 0) {
//                    binding.ivPickNull.visibility= View.GONE
//                } else {
//                    binding.ivPickNull.visibility = View.VISIBLE
//                }
            } // -----! appClass list관리 끝 !-----

            val PickRecyclerViewAdapter = PickRecyclerViewAdapter(appClass.pickList.value!!, this@PickFragment, requireActivity())
            binding.rvPick.adapter = PickRecyclerViewAdapter
            val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            binding.rvPick.layoutManager = linearLayoutManager
            PickRecyclerViewAdapter.notifyDataSetChanged()

            binding.btnPickAdd.setOnClickListener {
                viewModel.exerciseUnits.value?.clear()
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    setCustomAnimations(R.anim.slide_in_left, R.anim.slide_in_right)
                    replace(R.id.flPick, PickAddFragment())
                    addToBackStack(null)
                    commit()
                }
            }
            // -----! 핸드폰 번호로 PickItems 가져오기 끝 !-----
        }







        return binding.root
    }
    override fun onPickClick(title: String) {
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, PickDetailFragment.newInstance(title))
            addToBackStack(null)
            commit()

        }
    }

}