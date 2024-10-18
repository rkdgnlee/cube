package com.tangoplus.tangoq.dialog

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
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetupDialogBinding
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.listener.WeightVisibilityListener
import com.tangoplus.tangoq.`object`.NetworkUser.fetchUserUPDATEJson
import com.tangoplus.tangoq.`object`.Singleton_t_user

class SetupDialogFragment : DialogFragment() {
    lateinit var binding: FragmentSetupDialogBinding
    val viewModel: UserViewModel by activityViewModels()
//    lateinit var arg : String

//    companion object {
//        private const val ARG_SETUP = "startSetup"
//
//        fun newInstance(arg: String) : SetupDialogFragment {
//            val fragment = SetupDialogFragment()
//            val args = Bundle()
//            args.putString(ARG_SETUP, arg)
//            fragment.arguments = args
//            return fragment
//        }
//    }

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
        val t_userData = Singleton_t_user.getInstance(requireContext()).jsonObject
        val userSn = t_userData?.optString("user_sn")
//        arg = arguments?.getString(ARG_SETUP).toString()
        binding.btnSD.text = "다음으로"

        //------! 페이지 변경 call back 메소드 시작 !------
        binding.vpSD.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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

        binding.svSD.go(viewModel.setupStep, false)
//        when (arg) {
//            "startSetup" -> {
//                viewModel.setupProgress = 34
//                binding.pvSD.progress = viewModel.setupProgress
//                binding.svSD.getState().animationType(StepView.ANIMATION_CIRCLE)
//                    .steps(object : ArrayList<String?>() {
//                        init {
//                            add("성별")
//                            add("신장, 몸무게")
//                            add("목표")
//                        }
//                    }).stepsNumber(3)
//                    .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
//                    .commit()
//            }
//            else -> {
//                viewModel.setupProgress = 50
//                binding.pvSD.progress = viewModel.setupProgress
//                binding.svSD.getState().animationType(StepView.ANIMATION_CIRCLE)
//                    .steps(object : ArrayList<String?>() {
//                        init {
//                            add("신장, 몸무게")
//                            add("목표")
//                        }
//                    }).stepsNumber(2)
//                    .animationDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
//                    .commit()
//            }
//        }
        viewModel.setupProgress = 34
        binding.pvSD.progress = viewModel.setupProgress
        binding.svSD.getState().animationType(StepView.ANIMATION_CIRCLE)
            .steps(object : ArrayList<String?>() {
                init {
                    add("성별")
                    add("신장, 몸무게")
                    add("목표")
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
            if (binding.btnSD.text == "완료" && viewModel.step3.value == true) {

                if (userSn != null) {
                    // ------! 1. 초기 설정 완료 !------
                    val jsonObj = viewModel.User.value
                    Log.v("JSON몸통", "$jsonObj")

                    fetchUserUPDATEJson(requireContext(), getString(R.string.API_user), jsonObj.toString(), userSn.toString()) {
                        t_userData.put("height", viewModel.User.value?.optString("user_height"))
                        t_userData.put("weight", viewModel.User.value?.optString("user_weight"))
                        t_userData.put("user_goal", viewModel.User.value?.optString("user_goal"))
                        t_userData.put("user_part", viewModel.User.value?.optString("user_part"))
                        requireActivity().runOnUiThread{
                            viewModel.setupProgress = 34
                            viewModel.setupStep = 0
                            viewModel.step1.value = null
                            viewModel.step21.value = null
                            viewModel.step22.value = null
                            viewModel.step2.value = null
                            viewModel.step31.value = null
                            viewModel.step32.value = null
                            viewModel.step3.value = null
                            viewModel.User.value = null
                        }
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        dismiss()
                    }
//                    if (arg == "startSetup") {
//                        val jsonObj = viewModel.User.value
//                        Log.v("JSON몸통", "$jsonObj")
//
//                        fetchUserUPDATEJson(getString(R.string.API_user), jsonObj.toString(), userSn.toString()) {
//                            t_userData.put("height", viewModel.User.value?.optString("user_height"))
//                            t_userData.put("weight", viewModel.User.value?.optString("user_weight"))
//                            t_userData.put("user_goal", viewModel.User.value?.optString("user_goal"))
//                            t_userData.put("user_part", viewModel.User.value?.optString("user_part"))
//                            requireActivity().runOnUiThread{
//                                viewModel.setupProgress = 34
//                                viewModel.setupStep = 0
//                                viewModel.step1.value = null
//                                viewModel.step21.value = null
//                                viewModel.step22.value = null
//                                viewModel.step2.value = null
//                                viewModel.step31.value = null
//                                viewModel.step32.value = null
//                                viewModel.step3.value = null
//                                viewModel.User.value = null
//                            }
//                            val intent = Intent(requireContext(), MainActivity::class.java)
//                            startActivity(intent)
//                            dismiss()
//                        }
//                    } else {
//                        // ------! 2. 개인정보 수정 완료 !------
//                        dismiss()
//                    }
                }
                // ------! 3. 미설정된 목표, 통증부위 !------
            } else if (binding.btnSD.text == "완료" && viewModel.step32.value == false) {
                Toast.makeText(requireContext(), "통증 부위를 올바르게 해주세요", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && viewModel.step31.value == true) {
                showFragmentComponent()
                binding.btnSD.text = "완료"

            } else if (binding.btnSD.text == "다음으로" && viewModel.step31.value == false) {
                Toast.makeText(requireContext(), "목표 설정을 올바르게 해주세요", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && viewModel.step2.value == true) {
                setNextPage()

            } else if (binding.btnSD.text == "다음으로" && viewModel.step22.value == false) {
                Toast.makeText(requireContext(), "정확한 몸무게를 입력해주세요 ", Toast.LENGTH_SHORT).show()

            } else if (binding.btnSD.text == "다음으로" && viewModel.step21.value == true) {
                showFragmentComponent()

            } else if (binding.btnSD.text == "다음으로" && viewModel.step21.value == false) {
                Toast.makeText(requireContext(), "정확한 키를 입력해주세요 ", Toast.LENGTH_SHORT).show()

            } else {
                // ------! 4. 초기 설정일 때 페이징 !------
                if (viewModel.step1.value != null) {
                    setNextPage()
                }
                // ------! 5. 개인정보 수정일 때 페이징 !------
            }
        }
    }

    fun setPreviousPage() {
        viewModel.setupStep -= 1
        binding.vpSD.currentItem = viewModel.setupStep
        binding.svSD.go(viewModel.setupStep, true)
        viewModel.setupProgress -= 34
        binding.pvSD.progress = viewModel.setupProgress
//        when (arg) {
//            "startSetup" -> {
//                viewModel.setupProgress -= 34
//                binding.pvSD.progress = viewModel.setupProgress
//            }
//            else -> {
//                viewModel.setupProgress -= 50
//                binding.pvSD.progress = viewModel.setupProgress
//            }
//        }
    }

    fun setNextPage() {
        viewModel.setupStep += 1
        binding.vpSD.currentItem = viewModel.setupStep
        binding.svSD.go(viewModel.setupStep, true)
        viewModel.setupProgress += 34
        binding.pvSD.progress = viewModel.setupProgress
//        when (arg) {
//            "startSetup" -> {
//                viewModel.setupProgress += 34
//                binding.pvSD.progress = viewModel.setupProgress
//            }
//            else -> {
//                viewModel.setupProgress += 50
//                binding.pvSD.progress = viewModel.setupProgress
//            }
//        }
    }

    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }

    private fun showFragmentComponent() {
        val fragmentManager = childFragmentManager // DialogFragment를 사용하므로 childFragmentManager를 사용합니다
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