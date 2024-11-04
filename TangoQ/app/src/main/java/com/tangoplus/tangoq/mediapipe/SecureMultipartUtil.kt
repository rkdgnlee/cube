package com.tangoplus.tangoq.mediapipe

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom
import java.security.KeyStore
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.userAgent
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.*
import java.util.ServiceLoader.load
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

class SecureMultipartUtil(private val context: Context, val userInfoSn: Int) {
    private fun getUserKeyAlias(): String {
        return "${KEY_ALIAS}_$userInfoSn"
    }
    companion object {
        private const val TAG = "SecureMultipartUtil"
        private const val ALGORITHM = "AES/CBC/PKCS7Padding"
        private const val KEY_SIZE = 256
        private var KEY_ALIAS = "MultipartKey"
        private const val TIMEOUT_USEC = 10000L
        private const val TARGET_VIDEO_BITRATE = 2000000 // 2Mbps
        private const val TARGET_AUDIO_BITRATE = 128000  // 128Kbps
        private const val OUTPUT_VIDEO_WIDTH = 1280      // 720p
        private const val OUTPUT_VIDEO_HEIGHT = 720
        private const val OUTPUT_VIDEO_FRAME_RATE = 30
        private const val MIME_TYPE = "video/avc"        // H.264/AVC
    }

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val encryptCipher = Cipher.getInstance(ALGORITHM)
    private val decryptCipher = Cipher.getInstance(ALGORITHM)

    init {
        initializeKeyIfNeeded()
    }

    private fun initializeKeyIfNeeded() {
        if (!keyStore.containsAlias(getUserKeyAlias())) {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                getUserKeyAlias(),
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(KEY_SIZE)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }

    // 파일 암호화
    fun encryptFile(file: File): Pair<File, String> {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val inputBytes = file.readBytes()
        val encryptedBytes = encryptCipher.doFinal(inputBytes)

        // IV를 암호화된 데이터 앞에 추가
        val encryptedWithIv = iv + encryptedBytes

        // 암호화된 임시 파일 생성
        val encryptedFile = File.createTempFile("encrypted_${file.nameWithoutExtension}", ".enc", context.cacheDir)
        FileOutputStream(encryptedFile).use {
            it.write(encryptedWithIv)
        }

        // 파일의 원래 미디어 타입 저장
        val mediaType = when {
            file.extension.equals("jpg", true) ||
                    file.extension.equals("jpeg", true) -> "image/jpeg"
            file.extension.equals("json", true) -> "application/json"
            else -> "application/octet-stream"
        }

        return Pair(encryptedFile, mediaType)
    }

    fun encryptString(plaintext: String): String {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        encryptCipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encrypted = encryptCipher.doFinal(plaintext.toByteArray())
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    @Throws(Exception::class)
    fun compressAndEncryptVideo(inputFile: File): File {
        // 1단계: 비디오 압축
        val compressedFile = compressVideo(inputFile)

        return encryptVideoFile(compressedFile)
    }

    @Throws(Exception::class)
    private fun compressVideo(inputFile: File): File {
        val tempFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
        val extractor = MediaExtractor()
        val muxer = MediaMuxer(tempFile.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        try {
            extractor.setDataSource(inputFile.path)
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            // 비디오/오디오 트랙 찾기
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("video/") == true) {
                    videoTrackIndex = i
                } else if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                }
            }

            if (videoTrackIndex == -1) {
                throw IllegalArgumentException("No video track found")
            }

            // 비디오 인코더 설정
            val inputFormat = extractor.getTrackFormat(videoTrackIndex)
            val outputFormat = MediaFormat.createVideoFormat(MIME_TYPE, OUTPUT_VIDEO_WIDTH, OUTPUT_VIDEO_HEIGHT)
            outputFormat.apply {
                setInteger(MediaFormat.KEY_BIT_RATE, TARGET_VIDEO_BITRATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }

            val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            // TODO: 실제 인코딩 프로세스 구현
            // 1. Surface 생성 및 입력 설정
            // 2. 프레임 추출 및 인코딩
            // 3. 인코딩된 데이터를 muxer로 쓰기

            return tempFile
        } finally {
            extractor.release()
            muxer.release()
        }
    }
    @Throws(Exception::class)
    fun encryptVideoFile(inputFile: File): File {
        val outputFile = File(context.cacheDir, "encrypted_${System.currentTimeMillis()}.enc")
        val secretKey = keyStore.getKey(getUserKeyAlias(), null) as SecretKey

        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = encryptCipher.iv

        outputFile.outputStream().use { fileOut ->
            // IV를 파일 시작 부분에 저장
            fileOut.write(iv)

            // 파일 내용 암호화
            inputFile.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                val cipherOut = CipherOutputStream(fileOut, encryptCipher)

                cipherOut.use { output ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
        inputFile.delete()
        return outputFile
    }

    @Throws(Exception::class)
    fun decryptAndPlayVideo(encryptedFile: File): File {
        val outputFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.mp4")
        val secretKey = keyStore.getKey(getUserKeyAlias(), null) as SecretKey

        encryptedFile.inputStream().use { fileIn ->
            // IV 읽기 (첫 16바이트)
            val iv = ByteArray(16)
            fileIn.read(iv)

            // 복호화 초기화
            val ivSpec = IvParameterSpec(iv)
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            // 파일 복호화
            outputFile.outputStream().use { fileOut ->
                val cipherIn = CipherInputStream(fileIn, decryptCipher)
                cipherIn.use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        fileOut.write(buffer, 0, bytesRead)
                    }
                }
            }
        }

        return outputFile
    }

    fun cleanupTempFiles() {
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("encrypted_")) {
                file.delete()
            }
        }
    }
    // ------# 복호화 #------
    fun decryptFile(encryptedFile: File): File {
        val encryptedBytes = encryptedFile.readBytes()

        // IV 추출 (첫 16바이트)
        val iv = encryptedBytes.slice(0..15).toByteArray()
        // 나머지 암호화된 데이터
        val encryptedData = encryptedBytes.slice(16..encryptedBytes.lastIndex).toByteArray()

        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        decryptCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val decryptedBytes = decryptCipher.doFinal(encryptedData)

        // 복호화된 임시 파일 생성
        val decryptedFile = File.createTempFile("decrypted_", "", context.cacheDir)
        FileOutputStream(decryptedFile).use {
            it.write(decryptedBytes)
        }

        return decryptedFile
    }

    // 문자열 복호화
    fun decryptString(encryptedData: String): String {
        val encryptedBytes = Base64.getDecoder().decode(encryptedData)
        // IV 추출 (첫 16바이트)
        val iv = encryptedBytes.slice(0..15).toByteArray()
        // 나머지 암호화된 데이터
        val encryptedContent = encryptedBytes.slice(16..encryptedBytes.lastIndex).toByteArray()

        val key = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        decryptCipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val decryptedBytes = decryptCipher.doFinal(encryptedContent)
        return String(decryptedBytes)
    }
}