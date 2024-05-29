package com.tangoplus.tangoq.dialog

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat.requireViewById
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.SetupActivity
import com.tangoplus.tangoq.data.SignInViewModel
import com.tangoplus.tangoq.databinding.FragmentGoogleSignInDialogBinding
import com.tangoplus.tangoq.listener.OnSingleClickListener
import com.tangoplus.tangoq.`object`.NetworkUserService.StoreUserInSingleton
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserINSERTJson
import com.tangoplus.tangoq.`object`.NetworkUserService.fetchUserUPDATEJson
import com.tangoplus.tangoq.`object`.NetworkUserService.getUserSELECTJson
import com.tangoplus.tangoq.`object`.Singleton_t_user
import java.net.URLEncoder
import java.util.concurrent.TimeUnit


class GoogleSignInDialogFragment : DialogFragment() {
    lateinit var binding: FragmentGoogleSignInDialogBinding
    val viewModel : SignInViewModel by activityViewModels()
    val auth = Firebase.auth
    var verificationId = ""
    lateinit var transformMobile : String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGoogleSignInDialogBinding.inflate(inflater)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // -----! 통신사 선택 시작 !-----
        binding.tvGSTelecom.setOnSingleClickListener {
            showTelecomBottomSheetDialog(requireActivity())
        } // -----! 통신사 선택 끝 !-----

        // -----! 인증 문자 확인 시작 !-----
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {}
            override fun onVerificationFailed(p0: FirebaseException) {}
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                this@GoogleSignInDialogFragment.verificationId = verificationId
                Log.v("onCodeSent", "메시지 발송 성공")
                // -----! 메시지 발송에 성공하면 스낵바 호출 !------
                Snackbar.make(requireView(), "메시지 발송에 성공했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()
                binding.btnGSAuthConfirm.isEnabled = true
            }
        }
        binding.btnGSAuthSend.setOnSingleClickListener {
            transformMobile = phoneNumber82(binding.etGSMobile.text.toString())
            val dialog = AlertDialog.Builder(requireActivity())
                .setTitle("📩 문자 인증 ")
                .setMessage("$transformMobile 로 인증 하시겠습니까?")
                .setPositiveButton("예") { _, _ ->
                    val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(transformMobile)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(requireActivity())
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                    auth.setLanguageCode("kr")


                    transformMobile.replace("-", "")
                    transformMobile.replace("-", "")
                    Log.w("전화번호", transformMobile)
                    val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
                    alphaAnimation.duration = 600
                    binding.etGSAuthNumber.isEnabled = true

                }
                .setNegativeButton("아니오", null)
                .show()

            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
            binding.btnGSAuthConfirm.isEnabled = true
        }
        binding.btnGSAuthConfirm.setOnSingleClickListener {
            if (binding.btnGSAuthConfirm.text == "가입 완료하기") {
                getUserSELECTJson(getString(R.string.IP_ADDRESS_t_user), transformMobile) {jsonObj ->
                    if ( jsonObj?.getInt("status") == 404 ) {
                        Log.v("viewModelJson", "${viewModel.googleJson}")

                        fetchUserINSERTJson(getString(R.string.IP_ADDRESS_t_user), viewModel.googleJson.toString()) { // TODO insert가 제대로 동작하는지 확인해야함
                            StoreUserInSingleton(requireActivity(), jsonObj)
                            Log.v("구글>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                            setupInit()
                        }
                    } else {
                        fetchUserUPDATEJson(getString(R.string.IP_ADDRESS_t_user), viewModel.googleJson.toString(), transformMobile) {
                            if (jsonObj != null) {
                                StoreUserInSingleton(requireActivity(), jsonObj)
                            }
                        }
                        Log.v("구글>싱글톤", "${Singleton_t_user.getInstance(requireContext()).jsonObject}")
                        mainInit()
                    }
                }
            } else {
                val credential = PhoneAuthProvider.getCredential(verificationId, binding.etGSAuthNumber.text.toString())
                signInWithPhoneAuthCredential(credential)
            }

        }  // -----! 인증 문자 확인 끝 !-----
        // -----! 휴대폰 인증 끝 !-----

        binding.ibtnGSBack.setOnClickListener {
            dismiss()
            com.google.firebase.ktx.Firebase.auth.signOut()
        }
    }


    private fun setupInit() {
        dismiss()
        requireActivity().finish()
        val intent = Intent(requireActivity(), SetupActivity::class.java)
        startActivity(intent)
    }

    private fun mainInit() {
        dismiss()
        requireActivity().finish()
        val intent = Intent(requireActivity(), MainActivity::class.java)
        startActivity(intent)
    }
    @RequiresApi(Build.VERSION_CODES.P)
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    requireActivity().runOnUiThread {
                        binding.etGSAuthNumber.isEnabled = false
                        binding.etGSMobile.isEnabled = false
                        binding.btnGSAuthConfirm.text = "가입 완료하기"
                        val googleMobile = transformMobile.replaceFirst("+82 10", "+8210")
                        viewModel.googleJson.put("user_mobile", googleMobile)
                        val snackbar = Snackbar.make(requireView(), "인증에 성공했습니다 !", Snackbar.LENGTH_SHORT)
                        snackbar.setAction("확인", object: View.OnClickListener {
                            override fun onClick(v: View?) {
                                snackbar.dismiss()
                            }
                        })
                        snackbar.setActionTextColor(Color.WHITE)
                        snackbar.show()
                    }
                } else {
                    Log.w(ContentValues.TAG, "mobile auth failed.")
                }
            }
    }
    fun phoneNumber82(msg: String) : String {
        val firstNumber: String = msg.substring(0,3)
        var phoneEdit = msg.substring(3)
        when (firstNumber) {
            "010" -> phoneEdit = "+82 10$phoneEdit"
            "011" -> phoneEdit = "+8211$phoneEdit"
            "016" -> phoneEdit = "+8216$phoneEdit"
            "017" -> phoneEdit = "+8217$phoneEdit"
            "018" -> phoneEdit = "+8218$phoneEdit"
            "019" -> phoneEdit = "+8219$phoneEdit"
            "106" -> phoneEdit = "+82106$phoneEdit"
        }
        return phoneEdit
    }
    private fun showTelecomBottomSheetDialog(context: FragmentActivity) {
        val bottomsheetfragment = SignInBSDialogFragment()
        bottomsheetfragment.setOnCarrierSelectedListener(object : SignInBSDialogFragment.onTelecomSelectedListener {
            override fun onTelecomSelected(telecom: String) {
                binding.tvGSTelecom.text = telecom
            }
        })
        val fragmentManager = context.supportFragmentManager
        bottomsheetfragment.show(fragmentManager, bottomsheetfragment.tag)
    }
    private fun View.setOnSingleClickListener(action: (v: View) -> Unit) {
        val listener = View.OnClickListener { action(it) }
        setOnClickListener(OnSingleClickListener(listener))
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setDimAmount(0.6f)
        dialog?.window?.setBackgroundDrawable(resources.getDrawable(R.drawable.background_dialog))
    }
}