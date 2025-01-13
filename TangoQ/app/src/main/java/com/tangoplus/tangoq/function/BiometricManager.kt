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
        } catch (e: IllegalArgumentException) {
            Log.e("BiometricAuth", "credential check failed IllegalArgumentException :  ${e.message}" )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        } catch (e: IllegalStateException) {
            Log.e("BiometricAuth", "credential check failed IllegalStateException : ${e.message}")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        } catch (e: NullPointerException) {
            Log.e("BiometricAuth", "credential check failed NullPointerException : ${e.message}" )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        }  catch (e: ClassNotFoundException) {
            Log.e("BiometricAuth", "credential check failed ClassNotFoundException : ${e.message}" )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        } catch (e: Exception) {
            Log.e("BiometricAuth", "credential check failed Exception : ${e.message}" )
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