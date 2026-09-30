package com.example.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.BuildConfig
import com.example.data.local.JobChallanEntity
import com.example.data.local.QcInspectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@Serializable
data class MetaWhatsAppTextRequest(
    val messaging_product: String = "whatsapp",
    val to: String,
    val type: String = "text",
    val text: MetaTextBody
)

@Serializable
data class MetaTextBody(
    val preview_url: Boolean = false,
    val body: String
)

@Serializable
data class MetaWhatsAppResponse(
    val messaging_product: String? = null
)

interface MetaWhatsAppApiService {
    @POST("v20.0/{phoneNumberId}/messages")
    suspend fun sendMessage(
        @Path("phoneNumberId") phoneNumberId: String,
        @Header("Authorization") bearerToken: String,
        @Body body: MetaWhatsAppTextRequest
    ): MetaWhatsAppResponse
}

object WhatsAppTemplateEngine {

    /**
     * A. Cutting / Stitching Issue Challan (Pre-configured Hindi Template - Sec 37 & 43)
     */
    fun formatStitchingIssueChallan(challan: JobChallanEntity): String {
        return """
🚩 *C B CREATIONS – CUTTING ISSUE* 🚩
(Brand: BECOOPER • www.becooper.in)

📅 *दिनांक:* ${challan.issueDate}
👤 *कारीगर:* ${challan.karigarName}
🧾 *चालान नं.:* ${challan.challanNo}
👗 *आइटम:* ${challan.itemName}

📏 *SIZE-WISE PIECES:*
• S: ${challan.sizeS} | M: ${challan.sizeM} | L: ${challan.sizeL} | XL: ${challan.sizeXl} | XXL: ${challan.sizeXxl} | 3XL: ${challan.size3xl} | 4XL: ${challan.size4xl} | 5XL: ${challan.size5xl}

🔢 *कुल पीस (Total Pcs):* ${challan.totalPcs}

*इशू सामान:*
✂️ *कटिंग पीस:* ${challan.cuttingPcs} पीस
🧵 *काथा धागा कलर:* ${challan.kathaThreadColor}
🏷️ *लेबल:* ${challan.labelPcs} पीस

*मुख्य कटिंग / सिलाई निर्देश:*
• 1 इंच में 13 टाँके (13 Stitch), लुब व डबल सिलाई
• फ्रंट पट्टी (Front Patti): 10 × 2.5 इंच | V-कट: 1.5 इंच
• कुर्ती लंबाई: 32 इंच | स्लीव: 17 इंच (1 इंच पट्टी + टाँकी)
• दोनों साइड चाक पट्टी
• बॉटम चेस्ट से 1.5 इंच प्लस, नीचे से कम से कम (Minimum) फोल्ड रखें

⚠️ *महत्वपूर्ण सूचना:* कटिंग पीस, लेबल व सभी सामान गिनकर ही प्राप्त करें। ले जाने के बाद किसी कमी की ज़िम्मेदारी C B CREATIONS की नहीं होगी।

📞 *Contact:* 98292 11122, 95099 01475
*C B CREATIONS* — Sanganer, Jaipur (GSTIN: 08CCYPJ9736Q1Z6)
        """.trimIndent()
    }

    /**
     * B. Dhaga Cutting Issue Challan (Pre-configured Hindi Template - Sec 38 & 44)
     */
    fun formatDhagaCuttingIssueChallan(challan: JobChallanEntity): String {
        return """
🚩 *C B CREATIONS – धागा कटिंग इशू* 🚩

📅 *दिनांक:* ${challan.issueDate}
👤 *कारीगर:* ${challan.karigarName}
🧾 *चालान नं.:* ${challan.challanNo}
📦 *Lot / Style:* ${challan.lotNo} (${challan.itemName})
🔢 *कुल पीस:* ${challan.totalPcs}

*आवश्यक निर्देश:*
• सभी पीस से धागा बिल्कुल साफ़-सुथरा एवं बारीकी से काटें।
• कैंची से कपड़े या सिलाई (Stitch) को कोई नुकसान न पहुँचे।
• काम पूरा करके सभी पीस साफ़ मोड़कर (Fold करके) जमा कराएं।
• सभी पीस गिनकर ही प्राप्त करें।

⚠️ *सूचना:* बाद में किसी कमी पाए जाने पर ज़िम्मेदारी कारीगर की होगी।
📞 98292 11122 | 📞 95099 01475
*C B CREATIONS (BECOOPER)*
        """.trimIndent()
    }

    /**
     * C. Katha Work Issue Challan (Pre-configured Hindi Template - Sec 39 & 45)
     */
    fun formatKathaWorkIssueChallan(challan: JobChallanEntity): String {
        return """
🚩 *C B CREATIONS – काथा वर्क आवश्यक निर्देश* 🚩

📅 *दिनांक:* ${challan.issueDate}
👤 *कारीगर:* ${challan.karigarName}
🧾 *चालान:* ${challan.challanNo}
📦 *Lot:* ${challan.lotNo}
🔢 *कुल पीस:* ${challan.totalPcs}
🧵 *धागा कलर:* ${challan.kathaThreadColor} (जो धागा इशू किया गया है, उसी कलर से काथा वर्क करना है)

⚠️ *ज़रूरी हिदायतें:*
• डिज़ाइन: दिए गए Katha Design के अनुसार ही कार्य करें। गलत होने पर दोबारा खुलवा कर Rework करवाया जाएगा, माल स्वीकार नहीं किया जाएगा।
• पीस की सुरक्षा: किसी भी पीस पर कैंची (Scissor) से कट या Tear नहीं होना चाहिए। यदि कोई पीस कटता या फटता है, तो पूरी ज़िम्मेदारी आपकी होगी।
• धागा: केवल जारी किया गया Thread Color ही इस्तेमाल करें।

*नोट:* कृपया सभी कटिंग पीस गिनकर और चेक करके ही लेकर जाएँ। ले जाने के बाद हमारी कोई ज़िम्मेदारी नहीं होगी।
📞 98292 11122 | 📞 95099 01475
*C B CREATIONS* — Sanganer, Jaipur
        """.trimIndent()
    }

    /**
     * D. Fabricator Work Issue Challan (Pre-configured Hindi Template - Sec 40 & 46)
     */
    fun formatFabricatorIssueChallan(challan: JobChallanEntity): String {
        return """
🚩 *C B CREATIONS – FABRICATOR ISSUE* 🚩

📅 *दिनांक:* ${challan.issueDate}
👤 *Fabricator:* ${challan.karigarName}
🧾 *Challan:* ${challan.challanNo}
📋 *Job Order:* JO-${challan.lotNo}
📦 *Lot:* ${challan.lotNo}
👗 *Product / Style:* ${challan.itemName}

*SIZE-WISE QTY:*
S: ${challan.sizeS} | M: ${challan.sizeM} | L: ${challan.sizeL} | XL: ${challan.sizeXl} | XXL: ${challan.sizeXxl} | 3XL: ${challan.size3xl} | 4XL: ${challan.size4xl} | 5XL: ${challan.size5xl}

🔢 *Total:* ${challan.totalPcs} पीस
🧵 *Material:* कटिंग पीस (${challan.cuttingPcs}), काथा धागा (${challan.kathaThreadColor}), लेबल (${challan.labelPcs})
💰 *Rate:* ₹${challan.ratePerPc} / Piece
📅 *Expected Return:* ${challan.targetDeadlineHours} Hours

*Instructions:*
1 इंच = 13 स्टिच, Lub & Double Stitch, Front Patti 10×2.5 Inch, V-Cut 1.5 Inch.

⚠️ *सभी पीस एवं मटेरियल गिनकर ही प्राप्त करें।*
*C B CREATIONS (BECOOPER)*
📞 98292 11122 | 📞 95099 01475
        """.trimIndent()
    }

    fun formatChallanMessage(challan: JobChallanEntity): String {
        return when (challan.challanType) {
            "DHAGA_ISSUE" -> formatDhagaCuttingIssueChallan(challan)
            "KATHA_ISSUE" -> formatKathaWorkIssueChallan(challan)
            "FABRICATION_ISSUE" -> formatFabricatorIssueChallan(challan)
            else -> formatStitchingIssueChallan(challan)
        }
    }

    fun formatQcInspectionAlert(qc: QcInspectionEntity): String {
        return """
🚩 *C B CREATIONS – QUALITY CONTROL (QC) REPORT* 🚩
📦 Lot: ${qc.lotNo} | Challan: ${qc.challanNo}
🔍 Stage: ${qc.stage}
👤 Karigar: ${qc.karigarName}
✅ Inspected: ${qc.inspectedPcs} Pcs | ❌ Defective: ${qc.defectivePcs} Pcs
⚠️ Defect Reason: ${qc.defectReason} (${qc.severity})
🛠️ Action: ${qc.correctiveAction}
📌 Status: *${qc.qcDecision}*
Inspector: ${qc.inspectorName}
📞 98292 11122 | 95099 01475
        """.trimIndent()
    }

    /**
     * Dispatches via either Phone WhatsApp App (whatsapp:// / api.whatsapp.com)
     * or WhatsApp Web (https://web.whatsapp.com/send) based on selected mode.
     */
    fun launchWhatsAppIntent(
        context: Context,
        rawPhone: String,
        message: String,
        useWhatsAppWeb: Boolean = false
    ) {
        val sanitizedPhone = rawPhone.filter { it.isDigit() }.let {
            if (it.length == 10) "91$it" else it.ifBlank { "919829211122" }
        }
        try {
            if (useWhatsAppWeb) {
                val webUri = Uri.parse(
                    "https://web.whatsapp.com/send?phone=$sanitizedPhone&text=${Uri.encode(message)}"
                )
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } else {
                // First try native phone WhatsApp scheme, fallback to api.whatsapp.com
                val nativeUri = Uri.parse(
                    "whatsapp://send?phone=$sanitizedPhone&text=${Uri.encode(message)}"
                )
                val nativeIntent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(nativeIntent)
                } catch (_: Exception) {
                    val apiUri = Uri.parse(
                        "https://api.whatsapp.com/send?phone=$sanitizedPhone&text=${Uri.encode(message)}"
                    )
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, apiUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share via WhatsApp").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private val metaService: MetaWhatsAppApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://graph.facebook.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MetaWhatsAppApiService::class.java)
    }

    suspend fun dispatchViaMetaCloudApi(
        phoneNumberId: String,
        recipientPhone: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val token = BuildConfig.WHATSAPP_CLOUD_TOKEN
        if (token.isBlank() || token == "wa_cloud_token_placeholder") {
            return@withContext Result.failure(
                IllegalStateException("Integration Not Configured: WHATSAPP_CLOUD_TOKEN not set in Secrets panel.")
            )
        }
        try {
            val formattedPhone = recipientPhone.filter { it.isDigit() }.let {
                if (it.length == 10) "91$it" else it
            }
            metaService.sendMessage(
                phoneNumberId = phoneNumberId,
                bearerToken = "Bearer $token",
                body = MetaWhatsAppTextRequest(
                    to = formattedPhone,
                    text = MetaTextBody(body = message)
                )
            )
            Result.success("Dispatched via Meta WhatsApp Cloud API to +$formattedPhone")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
