package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.local.AccountingVoucherEntity
import com.example.data.local.JobChallanEntity
import com.example.data.local.KarigarEntity
import com.example.data.local.ManualTimeLogEntity
import com.example.data.local.MaterialItemEntity
import com.example.data.local.QcInspectionEntity
import com.example.domain.JobCostingAndGstEngine
import com.example.domain.JobCostingInput
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReportingAndWhatsAppHubScreen(
    companyProfile: CompanyMasterProfile,
    materials: List<MaterialItemEntity>,
    challans: List<JobChallanEntity>,
    karigars: List<KarigarEntity>,
    manualTimeLogs: List<ManualTimeLogEntity>,
    qcInspections: List<QcInspectionEntity>,
    vouchers: List<AccountingVoucherEntity>,
    costingInput: JobCostingInput,
    activeRole: String,
    isHindi: Boolean,
    useWhatsAppWebMode: Boolean,
    isReadOnlyViewer: Boolean,
    onSetWhatsAppWebMode: (Boolean) -> Unit,
    onSwitchToReadOnlyViewer: () -> Unit,
    onClearAllData: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedReportTab by remember { mutableIntStateOf(0) }
    // 0 = Production & Size Report, 1 = Karigar & Payout Report, 2 = Inventory & Accountability, 3 = QC & Finance Report
    var showEmbeddedWhatsAppWeb by remember { mutableStateOf(false) }
    var quickRecipientPhone by remember { mutableStateOf("9829211122") }

    // Live aggregations from Room DB (0 when cleared, real values when entered)
    val totalChallans = challans.size
    val totalPieces = challans.sumOf { it.totalPcs }
    val sizeTotals = remember(challans) {
        mapOf(
            "S" to challans.sumOf { it.sizeS },
            "M" to challans.sumOf { it.sizeM },
            "L" to challans.sumOf { it.sizeL },
            "XL" to challans.sumOf { it.sizeXl },
            "XXL" to challans.sumOf { it.sizeXxl },
            "3XL" to challans.sumOf { it.size3xl },
            "4XL" to challans.sumOf { it.size4xl },
            "5XL" to challans.sumOf { it.size5xl }
        )
    }
    val totalKarigarBalance = karigars.sumOf { it.runningBalanceInr }
    val totalIncentives = karigars.sumOf { it.earnedIncentiveInr }
    val totalInspected = qcInspections.sumOf { it.inspectedPcs }
    val totalDefects = qcInspections.sumOf { it.defectivePcs }
    val totalVoucherValue = vouchers.sumOf { it.amountInr }
    val costBreakdown = remember(costingInput) {
        JobCostingAndGstEngine.calculateJobCost(costingInput)
    }

    val formattedReportText = remember(
        selectedReportTab,
        challans,
        karigars,
        materials,
        qcInspections,
        vouchers,
        companyProfile
    ) {
        when (selectedReportTab) {
            0 -> """
📊 *${companyProfile.firmName} (BECOOPER) — PRODUCTION & SIZE-WISE REPORT*
📍 ${companyProfile.addressLine} | GSTIN: ${companyProfile.gstin}
• Total Job Challans: $totalChallans
• Total Production Pieces: $totalPieces Pcs
• Size-Wise Breakdown:
  S:${sizeTotals["S"]} | M:${sizeTotals["M"]} | L:${sizeTotals["L"]} | XL:${sizeTotals["XL"]} | XXL:${sizeTotals["XXL"]} | 3XL:${sizeTotals["3XL"]} | 4XL:${sizeTotals["4XL"]} | 5XL:${sizeTotals["5XL"]}
• QC Passed Lots: ${challans.count { it.qcStatus == "PASSED" }} | Rework/Pending: ${challans.count { it.qcStatus != "PASSED" }}
${if (challans.isEmpty()) "• Note: No active challans in database (Clean Slate)." else challans.joinToString("\n") { "  - ${it.challanNo} (${it.lotNo}): ${it.totalPcs} Pcs [${it.workflowStage} / ${it.qcStatus}]" }}
            """.trimIndent()

            1 -> """
👷 *${companyProfile.firmName} — KARIGAR & STAFF MANUAL TIME + PAYOUT REPORT*
• Total Registered Karigars: ${karigars.size}
• Manual Time Entries Logged: ${manualTimeLogs.size} (Total Manual Hours: ${String.format("%.1f", manualTimeLogs.sumOf { it.totalHoursManual })} Hrs)
• Total Running Balance Payable: ₹${totalKarigarBalance.toInt()} | Manual Time Earned: ₹${manualTimeLogs.sumOf { it.totalEarnedInr }.toInt()}
${if (manualTimeLogs.isEmpty()) "• Manual Time Logs: 0 entries recorded yet." else manualTimeLogs.joinToString("\n") { "  - [${it.personCategory}] ${it.personName} (${it.roleOrDepartment}): ${it.workDate} | ${it.inTimeManual}-${it.outTimeManual} (${it.totalHoursManual}h) | Pcs: ${it.piecesCompleted} | ₹${it.totalEarnedInr.toInt()}" }}
${if (karigars.isNotEmpty()) "\n• Karigar Master Balances:\n" + karigars.joinToString("\n") { "  - ${it.name} (${it.role}): Rate ₹${it.rateAmount} | Due: ₹${it.runningBalanceInr.toInt()}" } else ""}
            """.trimIndent()

            2 -> """
📦 *${companyProfile.firmName} — INVENTORY & MATERIAL ACCOUNTABILITY REPORT*
• Total SKUs Tracked: ${materials.size}
• Low Stock SKUs: ${materials.count { it.currentStock <= it.reorderLevel }}
• Total Cutting Pcs Issued: ${challans.sumOf { it.cuttingPcs }} | Labels Issued: ${challans.sumOf { it.labelPcs }}
${if (materials.isEmpty()) "• Note: Inventory is currently empty (0 test data)." else materials.joinToString("\n") { "  - ${it.skuCode} ${it.name}: ${it.currentStock} ${it.unit} (Lot: ${it.assignedLotNo})" }}
            """.trimIndent()

            else -> """
🧾 *${companyProfile.firmName} — QC, FINANCE & JOB COSTING REPORT*
• QC Inspections: ${qcInspections.size} (Inspected: $totalInspected Pcs | Defective: $totalDefects Pcs)
• Accounting Vouchers Posted: ${vouchers.size} (Total Value: ₹${totalVoucherValue.toInt()})
• Standard Kurti Mfg Cost/Pc: ₹${costBreakdown.exactManufacturingCostPerPc} | B2B Price w/ GST: ₹${costBreakdown.finalB2bInvoicePricePerPc}
            """.trimIndent()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reporting_whatsapp_hub_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // =====================================================================
        // 1. WHATSAPP WEB LOGIN OR PHONE WHATSAPP HUB CARD
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_whatsapp_hub"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isHindi) "🟢 WhatsApp हब: WhatsApp Web लॉगिन या Phone WhatsApp"
                        else "🟢 WhatsApp Center: WhatsApp Web Login or Phone WhatsApp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isHindi) "चालान और रिपोर्ट भेजने के लिए अपना पसंदीदा माध्यम चुनें:"
                        else "Choose how Challans & Reports are dispatched across the entire ERP:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSetWhatsAppWebMode(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!useWhatsAppWebMode) WhatsAppGreen
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!useWhatsAppWebMode) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_mode_phone_whatsapp")
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "Phone WhatsApp" else "Use Phone App",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Button(
                            onClick = { onSetWhatsAppWebMode(true) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (useWhatsAppWebMode) WhatsAppGreen
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (useWhatsAppWebMode) Color.White
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_mode_whatsapp_web")
                        ) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "WhatsApp Web" else "WhatsApp Web",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEmbeddedWhatsAppWeb = !showEmbeddedWhatsAppWeb },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_toggle_inapp_wa_web")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showEmbeddedWhatsAppWeb) "Hide Web QR Panel"
                                else "In-App WhatsApp Web Login",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://web.whatsapp.com")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(browserIntent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_open_browser_wa_web")
                        ) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open web.whatsapp.com", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (showEmbeddedWhatsAppWeb) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.useWideViewPort = true
                                        settings.loadWithOverviewMode = true
                                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                                        // Desktop User-Agent so web.whatsapp.com renders QR Login instead of mobile redirect
                                        settings.userAgentString =
                                            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                                        webViewClient = WebViewClient()
                                        webChromeClient = WebChromeClient()
                                        loadUrl("https://web.whatsapp.com")
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. READ-ONLY USER ("Viewer - जो कुछ भी एडिट न कर सके") & DATA RESET CARD
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_readonly_and_clear_data"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "🔒 Read-Only User (जो कुछ भी एडिट न कर सके) & डेटा क्लीनअप"
                                else "🔒 Read-Only User Role & Clean Database Control",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current Role: $activeRole ${if (isReadOnlyViewer) "• (STRICT VIEW-ONLY — All Add/Edit/Delete Blocked)" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isReadOnlyViewer) CrimsonDefectAlert else EmeraldQcPass,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSwitchToReadOnlyViewer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReadOnlyViewer) CrimsonDefectAlert
                                else MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_activate_readonly_user")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isReadOnlyViewer) "Active: Viewer (Read-Only)"
                                else "Switch to Viewer (No Edit)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (!isReadOnlyViewer) {
                            OutlinedButton(
                                onClick = onClearAllData,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_clear_all_test_data")
                            ) {
                                Icon(
                                    Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = CrimsonDefectAlert,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isHindi) "टेस्ट डेटा साफ़ करें" else "Clear All Data",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonDefectAlert
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. REPORTING PLATFORM (रिपोर्टिंग प्लेटफॉर्म)
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_reporting_platform"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = if (isHindi) "📊 C B CREATIONS — लाइव रिपोर्टिंग प्लेटफॉर्म"
                                else "📊 C B CREATIONS — Live Reporting Platform",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "100% Real-Time Database Reports • Export to CSV, Phone WhatsApp, or WhatsApp Web",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Report Category Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to (if (isHindi) "1. उत्पादन व साइज रिपोर्ट" else "1. Production & Size Report"),
                            1 to (if (isHindi) "2. कारीगर पेमेंट रिपोर्ट" else "2. Karigar Payout Report"),
                            2 to (if (isHindi) "3. स्टॉक व मटेरियल रिपोर्ट" else "3. Stock & Accountability"),
                            3 to (if (isHindi) "4. QC, लेखा व कॉस्ट रिपोर्ट" else "4. QC, Finance & Costing")
                        ).forEach { (idx, title) ->
                            FilterChip(
                                selected = selectedReportTab == idx,
                                onClick = { selectedReportTab = idx },
                                label = { Text(title) },
                                modifier = Modifier.testTag("chip_report_tab_$idx")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // KPI Summary Strip for Current Database State
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportMetricTile(
                            label = "Challans / Lots",
                            value = "$totalChallans ($totalPieces Pcs)",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricTile(
                            label = "Karigars / Due",
                            value = "${karigars.size} (₹${totalKarigarBalance.toInt()})",
                            modifier = Modifier.weight(1f)
                        )
                        ReportMetricTile(
                            label = "SKUs / Vouchers",
                            value = "${materials.size} SKU / ${vouchers.size} Vch",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Formatted Live Report Output Box
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedReportText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // Recipient Phone + Dispatch Buttons (Phone WhatsApp vs WhatsApp Web vs Copy CSV)
                    OutlinedTextField(
                        value = quickRecipientPhone,
                        onValueChange = { quickRecipientPhone = it },
                        label = {
                            Text(
                                if (isHindi) "रिपोर्ट भेजने के लिए WhatsApp नंबर"
                                else "Recipient WhatsApp Mobile Number (for Report Dispatch)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                WhatsAppTemplateEngine.launchWhatsAppIntent(
                                    context = context,
                                    rawPhone = quickRecipientPhone,
                                    message = formattedReportText,
                                    useWhatsAppWeb = false
                                )
                                onShowMessage("Sent Report via Phone WhatsApp App to +91 $quickRecipientPhone")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_send_report_phone_wa")
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Phone WhatsApp", style = MaterialTheme.typography.labelSmall)
                        }

                        Button(
                            onClick = {
                                WhatsAppTemplateEngine.launchWhatsAppIntent(
                                    context = context,
                                    rawPhone = quickRecipientPhone,
                                    message = formattedReportText,
                                    useWhatsAppWeb = true
                                )
                                onShowMessage("Opened WhatsApp Web (web.whatsapp.com) with Report for +91 $quickRecipientPhone")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_send_report_web_wa")
                        ) {
                            Icon(Icons.Default.DesktopWindows, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp Web", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("CB Creations Report", formattedReportText))
                                onShowMessage("Copied Report to clipboard!")
                            },
                            modifier = Modifier.testTag("btn_copy_report")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
