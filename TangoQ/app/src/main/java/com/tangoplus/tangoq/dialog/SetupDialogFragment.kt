package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.shuhart.stepview.StepView
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.SetupVPAdapter
import com.tangoplus.tangoq.viewmodel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetupDialogBinding
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.listener.WeightVisibilityListener
import com.tangoplus.tangoq.`object`.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.`object`.Singleton_t_user

class SetupDialogFragment : DialogFragment() {
    lateinit var binding: FragmentSetupDialogBinding
    val uvm: UserViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -----! singletom에 넣고, update 통신 !-----
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val userSn = userJson?.optString("user_sn")
//        arg = arguments?.getString(ARG_SETUP).toString()
        binding.btnSD.text = "다음으로"

        //------! 페이지 변경 call back 메소드 시작 !------
        binding.vpSD.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            @SuppressLint("SetTextI18n")
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 2) {
                    binding.btnSD.text = "완료"
                } else {
                    binding.btnSD.text = "다음으로"
                }
            }
        }) // -----! 페이지 변경 call back 메소드 끝 !-----

//        Log.v("arg", arg)
        binding.vpSD.adapter = SetupVPAdapter(childFragmentManager, lifecycle)
        binding.vpSD.isUserInputEnabled = false

        binding.ibtnSDBack.setOnClickListener { dismiss() }

        binding.svSD.go(uvm.setupStep, false)

        uvm.setupProgress = 34
        binding.pvSD.progress = uvm.setupProgress.toFloat()
        binding.svSD.state.animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("성별")
                    add("신장, 몸무게")
                    add("위험부위")
                }
            }).stepsNumber(3)
            .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
            .commit()

        // ------! 이전 버튼 시작 !------
        binding.ibtnSDBack.setOnSingleClickListener {
            if (binding.vpSD.currentItem == 0) {
                dismiss()
            } else {
                setPreviousPage()
            }
        }
        // ------! 이전 버튼 끝 !------

        binding.btnSD.setOnSingleClickListener {
            if (binding.btnSD.text == "완료" && uvm.step3.value == true) {

                if (userSn != null) {
                    // ------! 1. 초기 설정 완료 !------
                    val jsonObj = uvm.User.value
                    Log.v("JSON몸통", "$jsonObj")

                    fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jsonObj.toString(), userSn.toString()) {
                        userJson.put("height", uvm.User.value?.optString("user_height"))
                        userJson.put("weight", uvm.User.value?.optString("user_weight"))
                        userJson.put("user_goal", uvm.User.value?.optString("user_goal"))
                        userJson.put("user_part", uvm.User.value?.optString("user_part"))
                        requireActivity().runOnUiThread{
                            uvm.setupProgress = 34
                            uvm.setupStep = 0
                            uvm.step1.value = null
                            uvm.step21.value = null
                            uvm.step22.value = null
                            uvm.step2.value = null
                            uvm.step31.value = null
                            uvm.step32.value = null
                            uvm.step3.value = null
                            uvm.User.value = null
                        }
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        dismiss()
                    }
                }
                // ------! 3. 미설정된 목표, 통증부위 !------
            } else if (binding.btnSD.text == "완료" && uvm.step32.value == false) {
                Toast.makeText(requireContext(), "통증 부위를 올바르게 해주세요", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && uvm.step31.value == true) {
                showFragmentComponent()
                binding.btnSD.text = "완료"

            } else if (binding.btnSD.text == "다음으로" && uvm.step31.value == false) {
                Toast.makeText(requireContext(), "목표 설정을 올바르게 해주세요", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && uvm.step2.value == true) {
                setNextPage()

            } else if (binding.btnSD.text == "다음으로" && uvm.step22.value == false) {
                Toast.makeText(requireContext(), "정확한 몸무게를 입력해주세요 ", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && uvm.step21.value == true) {
                showFragmentComponent()

            } else if (binding.btnSD.text == "다음으로" && uvm.step21.value == false) {
                Toast.makeText(requireContext(), "정확한 키를 입력해주세요 ", Toast.LENGTH_SHORT).show()

            } else {
                // ------! 4. 초기 설정일 때 페이징 !------
                if (uvm.step1.value != null) {
                    setNextPage()
                }
                // ------! 5. 개인정보 수정일 때 페이징 !------
            }
        }
    }

    fun setPreviousPage() {
        uvm.setupStep -= 1
        binding.vpSD.currentItem = uvm.setupStep
        binding.svSD.go(uvm.setupStep, true)
        uvm.setupProgress -= 34
        binding.pvSD.progress = uvm.setupProgress.toFloat()
    }

    fun setNextPage() {
        uvm.setupStep += 1
        binding.vpSD.currentItem = uvm.setupStep
        binding.svSD.go(uvm.setupStep, true)
        uvm.setupProgress += 34
        binding.pvSD.progress = uvm.setupProgress.toFloat()
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun showFragmentComponent() {
        val fragmentManager = childFragmentManager
        val currentFragment = fragmentManager.findFragmentByTag("f${binding.vpSD.currentItem}")

        if (currentFragment is WeightVisibilityListener) {
            currentFragment.visibleWeight()
        }
    }
    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }


}