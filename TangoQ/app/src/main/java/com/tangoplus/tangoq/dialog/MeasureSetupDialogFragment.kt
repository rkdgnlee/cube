package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MediatorLiveData
import com.tangoplus.tangoq.MeasureSkeletonActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentMeasureSetupDialogBinding
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import java.util.regex.Pattern
import kotlin.math.asin

class MeasureSetupDialogFragment : DialogFragment(), SensorEventListener {
    private lateinit var  binding: FragmentMeasureSetupDialogBinding
    private val mvm : MeasureViewModel by activityViewModels()
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var currentBiasZ = 0f
    private var currentBiasX = 0f
    private var filteredAngleZ = 0f
    private var filteredAngleX = 0f
    private val ALPHA = 0.1f
    private val INTERPOLATION_FACTOR = 0.1f
    companion object {
        const val ARG_SETUP_CASE = "measure_setup_case"
        fun newInstance(case: Int) : MeasureSetupDialogFragment {
            val fragment = MeasureSetupDialogFragment()
            val args = Bundle()
            args.putInt(ARG_SETUP_CASE, case)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FlexableDialogFragment)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeasureSetupDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val case = arguments?.getInt(ARG_SETUP_CASE) ?: 1

        setCaseVisibility(case)
        when (case) {
            0 -> {
                binding.clMSDAgreement1.visibility = View.VISIBLE
                binding.clMSDAgreement2.visibility = View.VISIBLE

                binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}

                binding.ibtnMSDAgreement1.setOnClickListener {
                    showAgreement4()
                }
                binding.ibtnMSDAgreement2.setOnClickListener {
                    showAgreement5()
                }
                binding.clMSDAgreement1.setOnClickListener {
                    mvm.setupAgreement1.value = if (mvm.setupAgreement1.value == true) false else true
                }
                binding.clMSDAgreement2.setOnClickListener {
                    mvm.setupAgreement2.value = if (mvm.setupAgreement2.value == true) false else true
                }
                mvm.setupAgreement1.observe(viewLifecycleOwner) { agreement1 ->
                    binding.ivMSDAgreement1.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                        if (agreement1) {
                            R.drawable.icon_part_checkbox_enabled
                        } else {
                            R.drawable.icon_part_checkbox_disabled
                        }
                    ))
                }
                mvm.setupAgreement2.observe(viewLifecycleOwner) {agreement2->
                    binding.ivMSDAgreement2.setImageDrawable(ContextCompat.getDrawable(requireContext(),
                        if (agreement2) {
                            R.drawable.icon_part_checkbox_enabled
                        } else {
                            R.drawable.icon_part_checkbox_disabled
                        }
                    ))
                }

                // 버튼 observer 세팅
                val buttonEnabled = MediatorLiveData<Boolean>().apply {
                    val update = {
                        value = (mvm.setupNameCondition.value == true &&
                                mvm.setupAgreement1.value == true &&
                                mvm.setupAgreement2.value == true)
                    }
                    addSource(mvm.setupNameCondition) { update() }
                    addSource(mvm.setupAgreement1) { update() }
                    addSource(mvm.setupAgreement2) { update() }
                }

                buttonEnabled.observe(viewLifecycleOwner) { enabled ->
                    binding.btnMSDFinish.apply {
                        isEnabled = enabled
                        backgroundTintList = ColorStateList.valueOf(resources.getColor(
                            if (isEnabled) R.color.mainColor else R.color.subColor400
                            , null))
                    }
                }
                binding.btnMSDDeny.setOnSingleClickListener {
                    dismiss()
                    (activity as MeasureSkeletonActivity).finish()
                }
                val userName = userJson?.optString("user_name")
                mvm.setupName = userName ?: ""
            }
            1 -> {
                sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

                accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                binding.btnMSDConfirm1.setOnSingleClickListener {
                    sensorManager.unregisterListener(this)
                    dismiss()
                }
            }
            else -> {

            }
        }

        // 버튼 셋엄
        val namePatternCheck = Pattern.compile(
            "^(?:[가-힣]{2,5}|[a-zA-Z]{2,})(?: [가-힣]{2,5}| [a-zA-Z]{2,})?(?: [가-힣]{2,5}| [a-zA-Z]{2,})?\$"
        )
        binding.etMSDName.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mvm.setupName = s.toString()
                mvm.setupNameCondition.value = namePatternCheck.matcher(binding.etMSDName.text.toString()).find()
//                Log.v("idPw", "${svm.idCondition.value}")
            }
        })

        binding.etMSDName.setText(mvm.setupName)

        binding.ibtnMSDNameClear.setOnClickListener{ binding.etMSDName.setText("")}
        binding.btnMSDFinish.setOnClickListener {
            mvm.setupName = binding.etMSDName.text.toString()
            dismiss()
        }
    }

    private fun setCaseVisibility(case: Int) {
        when (case) {
            0 -> {
                binding.clMTD0.visibility = View.VISIBLE
                binding.clMTD1.visibility = View.GONE
                dialog?.setCancelable(false)
            }
            1 -> {
                binding.clMTD0.visibility = View.GONE
                binding.clMTD1.visibility = View.VISIBLE
                Log.v("케이스1", "case1 is finished $case")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bckgnd_rectangle_20))

//        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun showAgreement4() {
        val dialog = AgreementDetailDialogFragment.newInstance("agreement4")
        dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
    }

    private fun showAgreement5() {
        val dialog = AgreementDetailDialogFragment.newInstance("agreement5")
        dialog.show(requireActivity().supportFragmentManager, "agreement_dialog")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // 수직 감지 (Z축)
            val z = event.values[2]
            val clampedZ = z.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            val angleZ = Math.toDegrees(asin((clampedZ / SensorManager.GRAVITY_EARTH).toDouble()))
            filteredAngleZ = lowPassFilterZ(angleZ.toFloat())
            val normalizedAngleZ = (filteredAngleZ + 90).coerceIn(0f, 180f)
            val targetBiasZ = 1 - (normalizedAngleZ / 180f)
            currentBiasZ = interpolateZ(targetBiasZ)

            // 수평 감지 (X축)
            val x = event.values[0]
            val clampedX = x.coerceIn(-SensorManager.GRAVITY_EARTH, SensorManager.GRAVITY_EARTH)
            val angleX = Math.toDegrees(asin((clampedX / SensorManager.GRAVITY_EARTH).toDouble()))
            filteredAngleX = lowPassFilterX(angleX.toFloat())
            val normalizedAngleX = (filteredAngleX + 90).coerceIn(0f, 180f)
            val targetBiasX = normalizedAngleX / 180f
            currentBiasX = interpolateX(targetBiasX)

            // 하나의 LayoutParams 객체에 두 값을 모두 설정
            val layoutParams = binding.cvMTDGyro.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.verticalBias = currentBiasZ
            layoutParams.horizontalBias = currentBiasX
            binding.cvMTDGyro.layoutParams = layoutParams

            // 한 번만 requestLayout 호출
            binding.cvMTDGyro.requestLayout()

            // 두 조건을 함께 확인하여 배경색 결정
            val isVerticalCorrect = normalizedAngleZ in 88f..92f
            val isHorizontalCorrect = normalizedAngleX in 88f..92f

            if (isVerticalCorrect && isHorizontalCorrect) {
                // 두 방향 모두 정확할 때만 mainColor 적용
                binding.cvMTDGyro.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mainColor))
            } else {
                // 하나라도 정확하지 않으면 subColor 적용
                binding.cvMTDGyro.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.mainHalfColor))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    // ------! 센서 시작 !------
    private fun lowPassFilterZ(input: Float): Float {
        filteredAngleZ += ALPHA * (input - filteredAngleZ)
        return filteredAngleZ
    }

    private fun lowPassFilterX(input: Float): Float {
        filteredAngleX += ALPHA * (input - filteredAngleX)
        return filteredAngleX
    }

    private fun interpolateZ(target: Float): Float {
        return currentBiasZ + (target - currentBiasZ) * INTERPOLATION_FACTOR
    }

    private fun interpolateX(target: Float): Float {
        return currentBiasX + (target - currentBiasX) * INTERPOLATION_FACTOR
    }
}