package com.example.mhg

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues.TAG
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentSignIn3Binding
import java.util.regex.Pattern


class SignIn3Fragment : Fragment() {
    interface OnFragmentInteractionListener {
        fun onFragmentInteraction()
    }
    private var listener : OnFragmentInteractionListener? = null
    lateinit var binding : FragmentSignIn3Binding
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignIn3Binding.inflate(inflater)

        viewModel.User.observe(viewLifecycleOwner) {user ->
            if (user != null && user.has("user_email")) {
                binding.etEmail.setText(user.getString("user_email"))
                viewModel.emailCondition.value = true
            } else {
                binding.etEmail.text.clear()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailPattern =  "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"
        val EmailPattern = Pattern.compile(emailPattern)

        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.emailCondition.value = EmailPattern.matcher(binding.etEmail.text.toString()).find()
                // -----! 뷰모델에 값 넣기 !-----
                viewModel.User.value?.put("user_email", binding.etEmail.text)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        binding.etEmail.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                listener?.onFragmentInteraction()
                true
            } else {
                false
            }
        }
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

}