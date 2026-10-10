package com.example.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

class AudioTranscriptionService(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun startRecording(): Boolean {
        return try {
            val audioFile = File(context.cacheDir, "voice_memo_${System.currentTimeMillis()}.m4a")
            currentAudioFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            true
        } catch (e: Exception) {
            Log.e("AudioTranscription", "Failed to start recording", e)
            isRecording = false
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
                currentAudioFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AudioTranscription", "Failed to stop recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            null
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaRecorder = null
            isRecording = false
            currentAudioFile?.delete()
            currentAudioFile = null
        }
    }

    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(Exception("يرجى ضبط مفتاح GEMINI_API_KEY في إعدادات التطبيق أو استخدام الإدخال النصي"))
            }

            val bytes = FileInputStream(audioFile).use { it.readBytes() }
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Try first with gemini-3.5-transcribe as requested
            val result = callGeminiTranscription(apiKey, "gemini-3.5-transcribe", base64Data)
            if (result.isSuccess) {
                return@withContext result
            }

            // Fallback to gemini-3.5-flash
            Log.w("AudioTranscription", "Primary model failed, falling back to gemini-3.5-flash: ${result.exceptionOrNull()?.message}")
            callGeminiTranscription(apiKey, "gemini-3.5-flash", base64Data)
        } catch (e: Exception) {
            Log.e("AudioTranscription", "Transcription error", e)
            Result.failure(e)
        }
    }

    private fun callGeminiTranscription(apiKey: String, model: String, base64Audio: String): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "قم بتفريغ هذا التسجيل الصوتي بدقة باللغة العربية. اكتب النص المسموع فقط كما نُطق (مثل اسم الصنف أو المبلغ أو البيان أو الملاحظة) بدون أي شروحات أو علامات تنصيص إضافية.")
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "audio/mp4")
                                    put("data", base64Audio)
                                })
                            })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return Result.failure(Exception("API Error (${response.code}): $responseString"))
            }

            val respJson = JSONObject(responseString)
            val candidates = respJson.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

            if (text.isNotBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("لم يتم استخراج أي نص من الصوت"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
