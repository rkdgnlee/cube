package com.tangoplus.tangoq.db

import android.content.Context
import android.os.Environment
import android.util.Log
import com.tangoplus.tangoq.data.MeasureViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object FileStorageUtil {
    private const val IMAGE_DIR = "images"
    private const val VIDEO_DIR = "videos"
    private const val JSON_DIR = "json"

    // URL에서 파일 저장
    fun saveFileFromUrl(context: Context, url: String, fileType: FileType): Boolean {
        val fileName = url.substringAfterLast("/").replace("https://", "")
        val dir = getDirectory(context, fileType)
        val file = File(dir, fileName)

        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.v("SaveFileFromUrl", "Success To Save File : ${file.absolutePath}")
                true
            } else {
                Log.e("SaveFileFromUrl", "HTTP error code: ${connection.responseCode}")
                false
            }
        } catch (e: Exception) {
            Log.e("SaveFileFromUrl", "Error saving file from URL: ${e.message}")
            false
        }
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
            Log.v("영상JSON주소", file.absolutePath)
            mViewModel.dynamicJsonFile = file
            true
        } catch (e: IOException) {
            Log.e("SaveFile", "Error saving file: ${e.message}")
            false
        }
    }
//    fun getCacheFile(context: Context, fileName: String): File? {
//        val dir = context.cacheDir
//        val file = File(dir, fileName)
//        Log.v("file가져오기", "파라미터이름: ${fileName}, 주소: ${file.absolutePath}")
//        return if (file.exists()) file else null
//    }

    fun getFile(context: Context, fileName: String): File? {
        val fileType = getFileTypeFromExtension(fileName)
        val dir = getDirectory(context, fileType)
        val file = File(dir, fileName)
        Log.v("file가져오기", "파라미터이름: ${fileName}, 주소: ${file.absolutePath}")
        return if (file.exists()) file else null
    }

    fun deleteFile(context: Context, fileName: String): Boolean {
        val fileType = getFileTypeFromExtension(fileName)
        val dir = getDirectory(context, fileType)
        val file = File(dir, fileName)
        return file.delete()
    }

    fun readJsonFile(file: File): JSONObject? {
        return try {
            val jsonContent = file.readText(Charsets.UTF_8)
            JSONObject(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun readJsonArrayFile(file: File): JSONArray? {
        return try {
            val jsonContent = file.readText(Charsets.UTF_8)
            JSONArray(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 파일 타입에 따른 디렉토리 가져오기
    fun getFileTypeFromExtension(fileName: String): FileType {
        return when (fileName.substringAfterLast(".", "").lowercase()) {
            "jpg", "jpeg", "png" -> FileType.IMAGE
            "mp4", "mov", "avi" -> FileType.VIDEO
            "json" -> FileType.JSON
            else -> throw IllegalArgumentException("Unsupported file type")
        }
    }

    fun getDirectory(context: Context, fileType: FileType): File {
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
}