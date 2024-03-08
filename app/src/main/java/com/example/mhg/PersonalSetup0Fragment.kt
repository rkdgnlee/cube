package com.example.mhg

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.mhg.databinding.FragmentPersonalSetup0Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern


@Suppress("CAST_NEVER_SUCCEEDS")
class PersonalSetup0Fragment : Fragment() {
    lateinit var binding : FragmentPersonalSetup0Binding
    // ----- datastore 만들기 -----
    val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "settings")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPersonalSetup0Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val idCondition = MutableLiveData(false)
        val nameCondition = MutableLiveData(false)
        val pwCondition = MutableLiveData(false)
        val pwCompare = MutableLiveData(false)
        val genderCondition = MutableLiveData(false)
        val gradeCondition = MutableLiveData(false)

        val idPattern = "^[a-zA-Z0-9]{4,16}$" // 영문, 숫자 4 ~ 16자 패턴
        val namePatternKor =  "^[가-힣]{2,8}\$"
        val namePatternEng = "^[a-zA-Z\\s]{4,20}$"
        val pwPattern = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[$@$!%*#?&.^])[A-Za-z[0-9]$@$!%*#?&.^]{8,20}$" // 영문, 특수문자, 숫자 8 ~ 20자 패턴
        val IdPattern = Pattern.compile(idPattern)
        val NamePatternKor = Pattern.compile(namePatternKor)
        val NamePatternEng = Pattern.compile(namePatternEng)
        val Pwpattern = Pattern.compile(pwPattern)

        // ----- ! ID 조건 코드 ! -----
        binding.etId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                idCondition.value = IdPattern.matcher(binding.etId.text.toString()).find()
                if (idCondition.value == true) {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.success_green))
                    binding.tvIdCondition.text = "조건에 일치합니다"
                } else {
                    binding.tvIdCondition.setTextColor(binding.tvIdCondition.resources.getColor(R.color.orange))
                    binding.tvIdCondition.text = "조건에 일치하지 않습니다"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        // ----- ! 이름 조건 코드 ! -----
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                nameCondition.value = NamePatternKor.matcher(binding.etName.text.toString()).find() || NamePatternEng.matcher(binding.etName.text.toString()).find()

            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ----- ! 비밀번호 조건 코드 ! -----
        binding.etPw.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                pwCondition.value = Pwpattern.matcher(binding.etPw.text.toString()).find()
                if (pwCondition.value == true) {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.success_green))
                    binding.tvPwCondition.text = "조건에 일치합니다"
                } else {
                    binding.tvPwCondition.setTextColor(binding.tvPwCondition.resources.getColor(R.color.orange))
                    binding.tvPwCondition.text = "영문, 숫자, 특수문자( ! @ # $ % ^ & * ? .)를 모두 포함해서 8~20자리를 입력해주세요"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ----- ! 비밀번호 확인 코드 ! -----
        binding.etPwRepeat.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                pwCompare.value = (binding.etPw.text.toString() == binding.etPwRepeat.text.toString())
                if (pwCompare.value == true) {
                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.success_green))
                    binding.tvPwCompare.text = "비밀번호가 일치합니다"
                } else {
                    binding.tvPwCompare.setTextColor(binding.tvPwCompare.resources.getColor(R.color.orange))
                    binding.tvPwCompare.text = "비밀번호가 일치하지 않습니다"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ----- ! 성별 확인 코드 ! -----
        binding.rgGender.setOnCheckedChangeListener {group, checkId -> genderCondition.value = group.findViewById<RadioButton>(checkId).isChecked }
        genderCondition.observe(viewLifecycleOwner) {condition ->
            if (condition) {
                (binding.rbtnM.isChecked == true || binding.rbtnF.isChecked == true)
            }
        }

        // ----- ! 등급 확인 코드 ! -----
        binding.rgGrade.setOnCheckedChangeListener {group, checkId -> gradeCondition.value = group.findViewById<RadioButton>(checkId).isChecked }
        gradeCondition.observe(viewLifecycleOwner) {condition ->
            if (condition) {
                binding.rbtnGrade1.isChecked == true || binding.rbtnGrade2.isChecked == true || binding.rbtnGrade3.isChecked == true
            }
        }
        // ----- 모든 조건 충족 시작 -----
        fun checkConditions(): Boolean {
            return idCondition.value == true &&
                    nameCondition.value == true &&
                    pwCondition.value == true &&
                    pwCompare.value == true &&
                    genderCondition.value == true &&
                    gradeCondition.value == true
        }
        val allConditions = MediatorLiveData<Boolean>().apply {
            addSource(idCondition) { value = checkConditions() }
            addSource(nameCondition) { value = checkConditions() }
            addSource(pwCondition) { value = checkConditions() }
            addSource(pwCompare) { value = checkConditions() }
            addSource(genderCondition) { value = checkConditions() }
            addSource(gradeCondition) { value = checkConditions() }
        }

        allConditions.observe(viewLifecycleOwner) {
            binding.btnSignIn2.isEnabled = it
            binding.btnSignIn2.setBackgroundColor(
                if (it) binding.btnSignIn2.resources.getColor(R.color.success_green)
                else binding.btnSignIn2.resources.getColor(R.color.orange)
            )
        }
        // ----- 모든 조건 충족 끝 -----

        // ----- ! 서버로 전송하는 코드 ! -----
        binding.btnSignIn2.setOnClickListener {
            val JsonObj = JSONObject()
            JsonObj.put("user_id", binding.etId.text.toString().trim())
            JsonObj.put("user_name", binding.etName.text.toString().trim())
            JsonObj.put("user_password", binding.etPw.text.toString().trim())
            if (binding.rbtnM.isChecked == true) {
                JsonObj.put("user_gender", "M")
            } else {
                JsonObj.put("user_gender", "F")
            }
            if (binding.rbtnGrade1.isChecked == true) {
                JsonObj.put("user_grade", "1")
            } else if (binding.rbtnGrade2.isChecked == true) {
                JsonObj.put("user_grade", "2")
            } else {
                JsonObj.put("user_grade", "3")
            }
            fetchJson(getString(R.string.IP_ADDRESS), JsonObj.toString(), "PUT", requireActivity())
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)

        }

//        fetchJson(R.string.IP_ADDRESS.toString(), JsonObj.toString(), "PUT")


    }

    // ----- ! 응답받은 token 기기 내 datastore에 저장하는 함수 ! -----


    fun fetchJson(myUrl : String, json: String, category: String, context: Context){
        suspend fun saveToken(context: Context, token: String) {
            val dataStore = context.dataStore
            val tokenKey = stringPreferencesKey("login_token")
            dataStore.edit { preferences ->
                preferences[tokenKey] = token
            }
        }

        val client = OkHttpClient()
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url("${myUrl}?category=$category/")
            .post(body)
            .build()


        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OKHTTP3", "Failed to execute request!")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.e("OKHTTP3", "Success to execute request!: $responseBody")
//                val jsonDataArray = JSONArray(responseBody)
//                val jsonObj = jsonDataArray.getJSONObject(0)
//
//                (context as Fragment).lifecycleScope.launch(Dispatchers.Main) {
//                    saveToken(context, jsonObj.getString("login_token"))
//                }
            }
        })

    }

}
