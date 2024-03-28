package com.example.mhg

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.activityViewModels
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentIntro4Binding

class Intro4Fragment : Fragment() {
    lateinit var binding: FragmentIntro4Binding
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    val viewModel: UserViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIntro4Binding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        val idCondition = MutableLiveData(false)
//        val idConfirm = MutableLiveData(false)
//        val nameCondition = MutableLiveData(false)
//        val mobileCondition = MutableLiveData(false)
//        val pwCondition = MutableLiveData(false)
//        val pwCompare = MutableLiveData(false)
//        val genderCondition = MutableLiveData(false)
//
//        val idPattern = "^[a-zA-Z0-9]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
//        val namePatternKor =  "^[가-힣]{2,8}\$"
//        val namePatternEng = "^[a-zA-Z\\s]{4,20}$"
//        val mobilePattern = "^010[0-9]{8,9}$"
//        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
//        val IdPattern = Pattern.compile(idPattern)
//        val NamePatternKor = Pattern.compile(namePatternKor)
//        val NamePatternEng = Pattern.compile(namePatternEng)
//        val MobilePattern = Pattern.compile(mobilePattern)
//        val Pwpattern = Pattern.compile(pwPattern)

//        // -----! SNS회원가입 시 받아온 정보 (회원가입) !-----
//        viewModel.isGoogleLogin.observe(viewLifecycleOwner) { isGoogleLogin ->
//            if (isGoogleLogin) {
//                viewModel.User.observe(viewLifecycleOwner) { user ->
//                    binding.etName.setText(user?.getString("user_name"))
//                    binding.etEmail.setText(user?.getString("user_email"))
//                }
//                apiSignIn()
//                pwCondition.value = true
//                pwCompare.value = true
//            }
//        }
//        viewModel.isNaverLogin.observe(viewLifecycleOwner) {isNaverLogin ->
//            if (isNaverLogin) {
//                viewModel.User.observe(viewLifecycleOwner) { user ->
//                    binding.etName.setText(user?.getString("user_name"))
//                    binding.etEmail.setText(user?.getString("user_email"))
//                    binding.etMobile.setText(user?.getString("user_mobile"))
//                }
//                apiSignIn()
//                binding.etMobile.setBackgroundColor(binding.etMobile.resources.getColor(R.color.skeletongrey))
//                pwCondition.value = true
//                pwCompare.value = true
//                mobileCondition.value = true
//            }
//        }
//        viewModel.isKakaoLogin.observe(viewLifecycleOwner) {isKakaoLogin ->
//            if (isKakaoLogin) {
//                viewModel.User.observe(viewLifecycleOwner) { user ->
//                    binding.etName.setText(user?.getString("user_name"))
//                    binding.etEmail.setText(user?.getString("user_email"))
//                    binding.etMobile.setText(user?.getString("user_mobile"))
//                }
//                apiSignIn()
//                binding.etMobile.setBackgroundColor(binding.etMobile.resources.getColor(R.color.skeletongrey))
//                pwCondition.value = true
//                pwCompare.value = true
//                mobileCondition.value = true
//            }
//        }


        // ----- ! ID 조건 코드 ! -----
//        binding.etId.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                idCondition.value = IdPattern.matcher(binding.etId.text.toString()).find()
//                if (idCondition.value == true) {
////                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.success_green))
//                    binding.tvIdCondition.text = "조건에 일치합니다. 중복 확인을 해주시기 바랍니다."
//                } else {
//                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.orange))
//                    binding.tvIdCondition.text = "조건에 일치하지 않습니다"
//                }
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//        binding.btnIdConfirm.setOnClickListener{
//            fetchSELECTJson(getString(R.string.IP_ADDRESS_T_USER), binding.etMobile.text.toString()) {
//                binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.success_green))
//                binding.tvIdCondition.text = "조건이 모두 일치합니다."
//                idConfirm.value = true
//            }
//        }
//
//        // ----- ! 이름 조건 코드 ! -----
//        binding.etName.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                nameCondition.value = NamePatternKor.matcher(binding.etName.text.toString()).find() || NamePatternEng.matcher(binding.etName.text.toString()).find()
//
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//
//        // ----- ! 핸드폰 번호 조건 코드 !-----
//        binding.etMobile.addTextChangedListener(object: TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                mobileCondition.value = MobilePattern.matcher(binding.etMobile.text.toString()).find()
//            }
//        })
//
//
//        // ----- ! 비밀번호 조건 코드 ! -----
//        binding.etPw.addTextChangedListener(object : TextWatcher {
//            @SuppressLint("SetTextI18n")
//            override fun afterTextChanged(s: Editable?) {
//                pwCondition.value = Pwpattern.matcher(binding.etPw.text.toString()).find()
//                if (pwCondition.value == true) {
//                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.success_green))
//                    binding.tvPwCondition.text = "조건에 일치합니다"
//                } else {
//                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.orange))
//                    binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ? .)를 모두 포함해서 8~20자리를 입력해주세요"
//                }
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//
//        // ----- ! 비밀번호 확인 코드 ! -----
//        binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
//                if (pwCompare.value == true) {
//                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.success_green))
//                    binding.tvPwCompare.text = "비밀번호가 일치합니다"
//                } else {
//                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.orange))
//                    binding.tvPwCompare.text = "비밀번호가 일치하지 않습니다"
//                }
//            }
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        })
//
//        // ----- ! 성별 확인 코드 ! -----
//        binding.rgGender.setOnCheckedChangeListener {group, checkId -> genderCondition.value = group.findViewById<RadioButton>(checkId).isChecked }
//        genderCondition.observe(viewLifecycleOwner) {condition ->
//            if (condition) {
//                (binding.rbtnM.isChecked == true || binding.rbtnF.isChecked == true)
//            }
//        }
//
//        // ----- ! 등급 확인 코드 ! -----
//
//        // ----- 모든 조건 충족 시작 -----
//        fun checkConditions(): Boolean {
//            return idConfirm.value == true &&
//                    nameCondition.value == true &&
//                    mobileCondition.value == true &&
//                    pwCondition.value == true &&
//                    pwCompare.value == true &&
//                    genderCondition.value == true
//        }
//        val allConditions = MediatorLiveData<Boolean>().apply {
//            addSource(idConfirm) { value = checkConditions() }
//            addSource(nameCondition) { value = checkConditions() }
//            addSource(mobileCondition) { value = checkConditions() }
//            addSource(pwCondition) { value = checkConditions() }
//            addSource(pwCompare) { value = checkConditions() }
//            addSource(genderCondition) { value = checkConditions() }
//
//        }
//
//        allConditions.observe(viewLifecycleOwner) {
//            binding.btnSignIn.isEnabled = it
//            binding.btnSignIn.setBackgroundColor(
//                if (it) binding.btnSignIn.resources.getColor(R.color.success_green)
//                else binding.btnSignIn.resources.getColor(R.color.orange)
//            )
//        }
//
//        binding.btnSignIn.setOnClickListener {
//            val JsonObj = JSONObject()
//            JsonObj.put("user_id", binding.etId.text.toString().trim())
//            JsonObj.put("user_email", binding.etEmail.text.toString().trim())
//            JsonObj.put("user_mobile", binding.etMobile.text.toString().trim())
//            JsonObj.put("user_name", binding.etName.text.toString().trim())
//            JsonObj.put("user_password", binding.etPw.text.toString().trim())
//            if (binding.rbtnM.isChecked == true) {
//                JsonObj.put("user_gender", "MALE")
//            } else {
//                JsonObj.put("user_gender", "FEMALE")
//            }
//            NetworkService.fetchINSERTJson(getString(R.string.IP_ADDRESS_T_USER), JsonObj.toString()) {
//                val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
//                t_userInstance?.jsonObject = JsonObj
//                Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")
//                val intent = Intent(requireContext(), MainActivity::class.java)
//                startActivity(intent)
//
//            }
        }
//
//
//
//
//    }
//    // ----- ! 응답받은 token 기기 내 datastore에 저장하는 함수 ! -----
//    fun fetchSELECTJson(myUrl : String, user_id:String, callback: () -> Unit){
//        val client = OkHttpClient()
////        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull())
//        val request = Request.Builder()
//            .url("${myUrl}read.php?user_id=$user_id")
//            .get()
//            .build()
//
//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("OKHTTP3", "Failed to execute request!")
//            }
//            override fun onResponse(call: Call, response: Response)  {
//                val responseBody = response.body?.string()
//                Log.e("OKHTTP3", "Success to execute request!: $responseBody")
//                val jsonObj__ = responseBody?.let { JSONObject(it) }
//                if (jsonObj__?.getString("status") == "404") {
//                    activity?.runOnUiThread{
//                        callback()
//                    }
//                } else {
//                    val jsonObj = jsonObj__?.getJSONObject("data")
//                    val t_userInstance = context?.let { Singleton_t_user.getInstance(requireContext()) }
//                    t_userInstance?.jsonObject = jsonObj
//                    Log.e("OKHTTP3>싱글톤", "${t_userInstance?.jsonObject}")
//                    callback()
//                }
//            }
//        })
//    }
//
//    private fun apiSignIn() {
//        binding.etName.isEnabled = false
//        binding.etEmail.isEnabled = false
//        binding.etPw.isEnabled = false
//        binding.etPwRepeat.isEnabled = false
//        binding.etName.setBackgroundColor(binding.etName.resources.getColor(R.color.skeletongrey))
//        binding.etEmail.setBackgroundColor(binding.etEmail.resources.getColor(R.color.skeletongrey))
//        binding.etPw.setBackgroundColor(binding.etPw.resources.getColor(R.color.skeletongrey))
//        binding.etPwRepeat.setBackgroundColor(binding.etPwRepeat.resources.getColor(R.color.skeletongrey))
//    }


}