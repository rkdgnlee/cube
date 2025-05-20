package com.tangoplus.tangoq.db

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.function.SecurePreferencesManager
import com.tangoplus.tangoq.function.SecurePreferencesManager.decryptFile
import com.tangoplus.tangoq.function.SecurePreferencesManager.encryptFile
import com.tangoplus.tangoq.viewmodel.MeasureViewModel
import com.tangoplus.tangoq.function.SecurePreferencesManager.generateAESKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object FileStorageUtil {
    private const val IMAGE_DIR = "images"
    private const val VIDEO_DIR = "videos"
    private const val JSON_DIR = "json"
    // URL에서 파일 저장
    /* 1. SHA-256을 통해 hash 값 받기
    * 2. 저장해서 hash값이 같으면 저장 -> 암호화
    * 3. hash값이 다른 것들은 일단 SharedPreferences에 저장하고 넘기기
    * 4. 암호화가 끝난 후 hash값이 다른 재다운로드 파일들을 다운로드 받아야함.
    * */
    suspend fun saveFileFromUrl(context: Context, fileName: String, fileType: FileType): Boolean {
        return withContext(Dispatchers.IO) {
            val url = context.getString(R.string.file_url) + fileName  // url 형식 + 파일 이름
//            Log.v("url에서파일저장", url)
            val dir = getDirectory(context, fileType)
            val file = File(dir, fileName)  // 파일 이름을 그대로 사용

            // 파일있으면 다운로드 생략
            if (file.exists()) {

                Log.v("SaveFileFromUrl", "File already exists !")
                return@withContext true
            }
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val tempFile = File.createTempFile("temp_", null, context.cacheDir)
                    connection.inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    encryptFile(tempFile, file, generateAESKey(context))
                    tempFile.delete()

                    true
                } else {
                    false
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e("saveURLError", "indexOutOfBound: ${e.message}")
                false
            } catch (e: IllegalArgumentException) {
                Log.e("saveURLError", "illegalArgument: ${e.message}")
                false
            } catch (e: NullPointerException) {
                Log.e("saveURLError", "nullPointer: ${e.message}")
                false
            } catch (e: java.lang.Exception) {
                Log.e("saveURLError", "exception: ${e.message}")
                false
            }
        }
    }

    fun getPathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                path = it.getString(columnIndex)
            }
        }
        return path
    }
    fun saveJo(context: Context, fileName: String, jsonObject: JSONObject, mViewModel: MeasureViewModel): Boolean {
        val dir = context.cacheDir
        val file = File(dir, fileName)
        return try {
            val fileContent = jsonObject.toString().toByteArray() // JSONObject를 String으로 변환 후 ByteArray로 변환
            file.outputStream().use { it.write(fileContent) }
            mViewModel.staticJsonFiles.add(file)
            true
        } catch (e: IOException) {
            Log.e("SaveFile", "Error saving file: ${e.message}")
            false
        }
    }

    fun saveJa(context: Context, fileName: String, jsonArray: JSONArray,  mViewModel: MeasureViewModel): Boolean {
        val dir = context.cacheDir
        val file = File(dir, fileName)

        return try {
            val fileContent = jsonArray.toString().toByteArray() // JSONObject를 String으로 변환 후 ByteArray로 변환
            file.outputStream().use { it.write(fileContent) }
            mViewModel.dynamicJsonFile = file
            true
        } catch (e: IOException) {
            Log.e("SaveFile", "Error saving file: ${e.message}")
            false
        }
    }

    // 자동으로 복호화해서 가져오기.
    suspend fun getFile(context: Context, fileName: String): File? {
        return withContext(Dispatchers.IO) {
            val cache = SecurePreferencesManager.DecryptedFileCache.getInstance()
            // 캐시에서 먼저 확인
            val cachedData = cache?.get(fileName)
            if (cachedData != null) {
                return@withContext saveToInternalStorage(context, fileName, cachedData)
            }

            val fileType = getFileTypeFromExtension(fileName)
            val dir = getDirectory(context, fileType)
            val encryptedFile = File(dir, fileName)

            if (!encryptedFile.exists()) return@withContext null

            try {
                // 파일을 메모리로 읽어옴
                val encryptedData = encryptedFile.readBytes()
                val decryptedData = decryptFile(encryptedData, generateAESKey(context))
                    ?: return@withContext null

                cache?.put(fileName, decryptedData)
                saveToInternalStorage(context, fileName, decryptedData)
            } catch (e: IndexOutOfBoundsException) {
                Log.e("getFileError", "indexOutOfBounds: ${e.message}")
                null
            } catch (e: IllegalArgumentException) {
                Log.e("getFileError", "illegalArgument: ${e.message}")
                null
            } catch (e: NullPointerException) {
                Log.e("getFileError", "nullPointer: ${e.message}")
                null
            } catch (e: IllegalStateException) {
                Log.e("getFileError", "illegalState: ${e.message}")
                null
            } catch (e: java.lang.Exception) {
                Log.e("getFileError", "exception: ${e.message}")
                null
            }
        }
    }

    // 손상된 파일 삭제 함수
    suspend fun deleteCorruptedFile(context: Context, fileName: String) {
        withContext(Dispatchers.IO) {
            // 캐시에서 삭제
            val cache = SecurePreferencesManager.DecryptedFileCache.getInstance()
            cache?.clear()

            // 암호화된 파일 삭제
            val fileType = getFileTypeFromExtension(fileName)
            val dir = getDirectory(context, fileType)
            val encryptedFile = File(dir, fileName)
            if (encryptedFile.exists()) {
                encryptedFile.delete()
                Log.v("DeleteFile", "Deleted file")
            }

            // 내부 저장소의 복호화된 파일 삭제
            val internalFile = File(context.filesDir, fileName)
            if (internalFile.exists()) {
                internalFile.delete()
                Log.v("DeleteFile", "Deleted file")
            }
        }
    }
    private fun saveToInternalStorage(context: Context, fileName: String, data: ByteArray): File {
        val internalFile = File(context.filesDir, fileName)
        internalFile.writeBytes(data)
//        Log.v("FileStorage", "File saved to internal storage: ${internalFile.absolutePath}")
        return internalFile
    }

//    fun deleteFile(context: Context, fileName: String): Boolean {
//        val fileType = getFileTypeFromExtension(fileName)
//        val dir = getDirectory(context, fileType)
//        val file = File(dir, fileName)
//        return file.delete()
//    }

    fun readJsonFile(file: File): JSONObject? {
        return try {
            val jsonContent = file.readText(Charsets.UTF_8)
            JSONObject(jsonContent)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("readJsonError", "index: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("readJsonError", "illegalArgument: ${e.message}")
            null
        } catch (e: NullPointerException) {
            Log.e("readJsonError", "nullPointer: ${e.message}")
            null
        } catch (e: java.lang.Exception) {
            Log.e("readJsonError", "exception: ${e.message}")
            null
        }
    }

    fun readJsonArrayFile(file: File): JSONArray? {
        return try {
            val jsonContent = file.readText(Charsets.UTF_8)
            JSONArray(jsonContent)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("readJsonArrError", "index: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            Log.e("readJsonArrError", "illegalArgument: ${e.message}")
            null
        } catch (e: NullPointerException) {
            Log.e("readJsonArrError", "NullPointer: ${e.message}")
            null
        } catch (e: java.lang.Exception) {
            Log.e("readJsonArrError", "exception: ${e.message}")
            null
        }
    }

    // 파일 타입에 따른 디렉토리 가져오기
    private fun getFileTypeFromExtension(fileName: String): FileType {
        return when (fileName.substringAfterLast(".", "").lowercase()) {
            "jpg", "jpeg", "png" -> FileType.IMAGE
            "mp4", "mov", "avi" -> FileType.VIDEO
            "json" -> FileType.JSON
            else -> throw IllegalArgumentException("Unsupported file type")
        }
    }

    private fun getDirectory(context: Context, fileType: FileType): File {
        val baseDir = when (fileType) {
            FileType.IMAGE -> context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            FileType.VIDEO -> context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            FileType.JSON -> context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        }

        val dir = File(baseDir, when (fileType) {
            FileType.IMAGE -> IMAGE_DIR
            FileType.VIDEO -> VIDEO_DIR
            FileType.JSON -> JSON_DIR
        })

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    enum class FileType {
        IMAGE, VIDEO, JSON
    }
//
//    private fun calculateFileHash(file: File): String {
//        val digest = MessageDigest.getInstance("SHA-256")
//        file.inputStream().use { input ->
//            val buffer = ByteArray(1024)
//            var bytesRead: Int
//            while (input.read(buffer).also { bytesRead = it } != -1) {
//                digest.update(buffer, 0, bytesRead)
//            }
//        }
//        return digest.digest().joinToString("") { "%02x".format(it) }
//    }

//    fun isFileIntegrityValid(file: File, expectedHash: String): Boolean {
//        return calculateFileHash(file) == expectedHash
//    }
}