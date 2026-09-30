package com.example.domain

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiRestApi {
    @POST("v1beta/models/gemini-flash-latest:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiAiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiRestApi by lazy {
        val json = Json { ignoreUnknownKeys = true }
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiRestApi::class.java)
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeErpBottlenecksAndSchedule(
        erpSnapshotSummary: String,
        userQuestion: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext """
⚡ [AI Diagnostic Preview — Configure GEMINI_API_KEY in AI Studio Secrets Panel for Live Gemini API Calls]

📊 C B CREATIONS Production Bottleneck & Scheduling Analysis:
1. Critical Bottleneck Detected:
   • Lot LOT-CB-403 (Dhaga Cutting Stage - Rekha Devi) is running 3h behind the 24h target with 6 pieces flagged for rework (uncut inner overlock threads).
   • Action: Re-assign 50 pieces of upcoming Dhaga Cutting from LOT-CB-402 to auxiliary finisher line and verify scissor-safety check before pressing.

2. Inventory Re-order Alert:
   • SKU KTH-MUST-12 (Mustard Haldi Katha Thread) is at 45 Cones (below 75 Cones reorder threshold). Issue PO for 150 Cones immediately so LOT-CB-402 does not stall.

3. Speed Booster Leaderboard Insight:
   • Rameshwar Lal Tailor achieved 96 Speed Score (38h vs 48h target, 0 QC defects), earning ₹1,632 (+10% Speed Booster Bonus). Assign priority Lot LOT-CB-404 V-Cut Kurti to his line.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are the AI ERP Production & Supply Chain Copilot for C B CREATIONS (Jaipur Garment Manufacturing). Respond concisely in bilingual English + Hindi bullet points covering bottlenecks, QC actions, Karigar scheduling, and inventory alerts."
                    )
                )
            ),
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = "ERP Live Data Snapshot:\n$erpSnapshotSummary\n\nTask: $userQuestion")
                    )
                )
            )
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No response generated from Gemini."
        } catch (e: Exception) {
            "Gemini API Error: ${e.localizedMessage ?: "Network/Key error"}. Verify GEMINI_API_KEY in AI Studio Secrets."
        }
    }

    suspend fun performChallanOcr(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext """
📄 [OCR Extraction Preview — Add GEMINI_API_KEY in AI Studio Secrets Panel for Live Multimodal Vision OCR]
• Detected Challan Type: C B CREATIONS - कटिंग / सिलाई इशू चालान
• Lot No.: LOT-CB-404 | Style: Rani Pink Hand-Katha V-Cut Kurti (32")
• Size Matrix: S:20 | M:35 | L:45 | XL:45 | XXL:30 | 3XL:15 | Total: 190 Pcs
• Issued Trims: 190 Cutting Pcs, Rani Pink Katha Thread (#08), 190 Woven Labels
• Stitching Spec Verified: 13 SPI Double Stitch, 10×2.5" Front Patti, 1.5" V-Cut.
            """.trimIndent()
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(
                            text = "Extract garment challan or invoice details from this image for C B CREATIONS ERP: Challan No, Date, Karigar Name, Lot No, Size-wise pieces (S, M, L, XL, XXL, 3XL, 4XL, 5XL), Total Pieces, Katha Thread Color, and any QC or rate notes. Format cleanly in Hindi and English."
                        ),
                        Part(
                            inlineData = InlineData(
                                mimeType = "image/jpeg",
                                data = bitmap.toBase64Jpeg()
                            )
                        )
                    )
                )
            )
        )

        try {
            val response = api.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "Could not extract text from image."
        } catch (e: Exception) {
            "OCR API Error: ${e.localizedMessage ?: "Check GEMINI_API_KEY in Secrets panel"}"
        }
    }
}
