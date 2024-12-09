package com.tangoplus.tangoq.function

import android.app.KeyguardManager
import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class BiometricManager(private val fragment: Fragment) {
    private val biometricManager: BiometricManager = BiometricManager.from(fragment.requireContext())
    private val keyguardManager: KeyguardManager =
        fragment.requireContext().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private fun isDeviceSecure(): Boolean {
        return try {
            keyguardManager.isDeviceSecure
        } catch (e: Exception) {
            Log.e("BiometricAuth", "Device security check failed", e)
            false
        }
    }
    private fun checkDeviceCredentialStatus(): Int {
        return try {
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.DEVICE_CREDENTIAL or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
        } catch (e: Exception) {
            Log.e("BiometricAuth", "Device credential check failed", e)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }
    }

    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        fallbackToDeviceCredential: Boolean = true
    ) {
        Log.v("BiometricAuth", "Authentication started")
        Log.v("BiometricAuth", "Device secure: ${isDeviceSecure()}")

        // 디바이스에 보안 설정이 전혀 없는 경우 바로 성공 처리
        if (!isDeviceSecure()) {
            Log.v("BiometricAuth", "No device security, bypassing authentication")
            onSuccess()
            return
        }

        // 디바이스 자격 증명 상태 확인
        val deviceCredentialStatus = checkDeviceCredentialStatus()
        Log.v("BiometricAuth", "Device credential status: $deviceCredentialStatus")

        when (deviceCredentialStatus) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                promptDeviceCredential(onSuccess, onError)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError("기기 인증 하드웨어가 없습니다.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError("기기 인증 하드웨어를 사용할 수 없습니다.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                if (fallbackToDeviceCredential) {
                    onError("기기 인증 방법이 등록되지 않았습니다.")
                } else {
                    onError("기기 인증을 사용할 수 없습니다.")
                }
            }
            else -> {
                onError("알 수 없는 오류가 발생했습니다.")
            }
        }
    }


    // 생체 인증 요청
    private fun promptBiometric(onSuccess: () -> Unit, onError: (String) -> Unit, fallbackToDeviceCredential: Boolean) {
        val executor = ContextCompat.getMainExecutor(fragment.requireContext())

        val biometricPrompt = BiometricPrompt(
            fragment, // Context를 FragmentActivity로 캐스팅
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.v("인증실패", "promptBiometric - onAuthenticationSucceeded")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    if (fallbackToDeviceCredential) {
                        promptDeviceCredential(onSuccess, onError)
                    } else {
                        Log.v("인증실패", "promptBiometric - onAuthenticationFailed")
                        onError("인증에 실패했습니다.")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (fallbackToDeviceCredential) {
                        promptDeviceCredential(onSuccess, onError)
                    } else {
                        Log.v("인증실패", "promptBiometric - onAuthenticationError")
                        onError("인증에 실패했습니다.")
                    }
                }
            }
        )
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체 인증")
            .setSubtitle("앱 보안을 위해 생체 인증을 진행해 주세요")
            .setNegativeButtonText("취소")

        // 디바이스 자격 증명(PIN/패턴/비밀번호) 허용
        if (fallbackToDeviceCredential) {
            promptInfoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        }
        val promptInfo = promptInfoBuilder.build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun promptDeviceCredential(onSuccess: () -> Unit, onError: (String) -> Unit) {
        Log.v("BiometricAuth", "Prompting device credential")

        val executor = ContextCompat.getMainExecutor(fragment.requireContext())
        val biometricPrompt = BiometricPrompt(
            fragment,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    Log.v("BiometricAuth", "Device credential authentication succeeded")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    Log.e("BiometricAuth", "Device credential authentication failed")
                    onError("기기 인증에 실패했습니다.")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Log.e("BiometricAuth", "Device credential authentication error: $errorCode - $errString")
                    onError("기기 인증 중 오류가 발생했습니다: $errString")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("기기 인증")
            .setSubtitle("PIN, 패턴 또는 비밀번호로 인증해주세요")
            .setDeviceCredentialAllowed(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}