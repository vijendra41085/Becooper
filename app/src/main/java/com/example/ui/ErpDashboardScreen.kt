package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AccountingVoucherEntity
import com.example.data.local.JobChallanEntity
import com.example.data.local.KarigarEntity
import com.example.data.local.MaterialItemEntity
import com.example.data.local.QcInspectionEntity
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen

data class KarigarRealtimeAlert(
    val id: String,
    val severity: String, // CRITICAL, WARNING, ACTIVE_FLOOR, BONUS
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val phone: String,
    val whatsappMessage: String,
    val associatedKarigar: KarigarEntity? = null
)

@Composable
fun ErpDashboardScreen(
    companyProfile: CompanyMasterProfile,
    materials: List<MaterialItemEntity>,
    challans: List<JobChallanEntity>,
    karigars: List<KarigarEntity>,
    qcInspections: List<QcInspectionEntity>,
    vouchers: List<AccountingVoucherEntity>,
    activeRole: String,
    isHindi: Boolean,
    searchQuery: String,
    canWhatsApp: Boolean,
    useWhatsAppWebMode: Boolean = false,
    onToggleKarigarPunch: (KarigarEntity, String) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedAlertFilter by remember { mutableStateOf("ALL") } // ALL, CRITICAL, FLOOR, PAYOUT

    // --- 1. Live Database Aggregations for Production Status ---
    val totalPiecesInProduction = remember(challans) { challans.sumOf { it.totalPcs } }
    val qcPassedPieces = remember(challans) {
        challans.filter { it.qcStatus == "PASSED" }.sumOf { it.totalPcs }
    }
    val reworkOrPendingPieces = remember(challans) {
        challans.filter { it.qcStatus != "PASSED" }.sumOf { it.totalPcs }
    }
    val activeFloorKarigars = remember(karigars) { karigars.count { it.isActivePunchIn } }
    val totalKarigarPayable = remember(karigars) { karigars.sumOf { it.runningBalanceInr } }
    val lowStockMaterials = remember(materials) {
        materials.filter { it.currentStock <= it.reorderLevel }
    }

    // Stage-wise breakdown computed from live challans
    val stageBreakdown = remember(challans) {
        val stages = listOf(
            "CUTTING" to (if (isHindi) "कटिंग (Cutting)" else "Cutting"),
            "KATHA_WORK" to (if (isHindi) "काथा वर्क (Katha)" else "Katha Work"),
            "EMBROIDERY" to (if (isHindi) "एम्ब्रॉयडरी" else "Embroidery"),
            "STITCHING" to (if (isHindi) "सिलाई (13 SPI)" else "Stitching"),
            "DHAGA_CUTTING" to (if (isHindi) "धागा कटिंग" else "Dhaga Cut"),
            "PRESSING_PACKING" to (if (isHindi) "प्रेस व पैकिंग" else "Press & Pack")
        )
        stages.map { (stageKey, label) ->
            val matching = challans.filter { it.workflowStage == stageKey }
            val pcs = matching.sumOf { it.totalPcs }
            val lots = matching.size
            Triple(label, pcs, lots)
        }
    }

    // --- 2. Pending / Action-Required Challans ---
    val pendingChallans = remember(challans, searchQuery) {
        val base = challans.filter {
            it.qcStatus != "PASSED" || it.actualHoursTaken >= it.targetDeadlineHours || it.workflowStage != "DISPATCHED"
        }
        if (searchQuery.isBlank()) base
        else base.filter {
            it.challanNo.contains(searchQuery, ignoreCase = true) ||
                it.lotNo.contains(searchQuery, ignoreCase = true) ||
                it.karigarName.contains(searchQuery, ignoreCase = true) ||
                it.itemName.contains(searchQuery, ignoreCase = true)
        }
    }

    // --- 3. Real-Time Karigar Alerts Generator (Grounded in Room DB) ---
    val karigarAlerts = remember(karigars, challans, qcInspections, lowStockMaterials, isHindi) {
        val list = mutableListOf<KarigarRealtimeAlert>()

        // A. Rework / QC Defect Alerts
        challans.filter { it.qcStatus == "REWORK_REQUIRED" || it.qcStatus == "QUARANTINED" }.forEach { ch ->
            val overdueHrs = (ch.actualHoursTaken - ch.targetDeadlineHours).coerceAtLeast(0)
            list.add(
                KarigarRealtimeAlert(
                    id = "rework_${ch.challanNo}",
                    severity = "CRITICAL",
                    title = if (isHindi) "🔴 QC रीवर्क अलर्ट: ${ch.karigarName} (${ch.lotNo})"
                    else "🔴 QC Rework Required: ${ch.karigarName} (${ch.lotNo})",
                    subtitle = "${ch.itemName} • ${ch.totalPcs} Pcs • Stage: ${ch.workflowStage} ${if (overdueHrs > 0) "• Delayed by ${overdueHrs}h" else ""} • ${ch.notes}",
                    actionLabel = if (isHindi) "WhatsApp रीवर्क नोटिस" else "Send Rework Alert",
                    phone = ch.karigarPhone,
                    whatsappMessage = WhatsAppTemplateEngine.formatChallanMessage(ch)
                )
            )
        }

        // B. Live Floor Punch-In Telemetry Alerts
        karigars.filter { it.isActivePunchIn }.forEach { k ->
            val mins = if (k.punchInTimestamp > 0L) {
                ((System.currentTimeMillis() - k.punchInTimestamp) / 60000L).coerceAtLeast(1L)
            } else 45L
            list.add(
                KarigarRealtimeAlert(
                    id = "floor_${k.id}",
                    severity = "ACTIVE_FLOOR",
                    title = if (isHindi) "🟢 लाइव फ्लोर पंच-इन: ${k.name}"
                    else "🟢 Live Floor Active: ${k.name}",
                    subtitle = "Role: ${k.role} • Active Lot: ${k.activeLotNo.ifBlank { "LOT-CB-401" }} • Elapsed: ${mins}m • Speed Score: ${k.avgSpeedScore}/100",
                    actionLabel = if (isHindi) "पंच-आउट करें" else "Punch Out",
                    phone = k.phone,
                    whatsappMessage = "C B CREATIONS: ${k.name} is actively working on ${k.activeLotNo} (${mins} mins elapsed).",
                    associatedKarigar = k
                )
            )
        }

        // C. High Running Balance / Payout Due Alerts
        karigars.filter { it.runningBalanceInr >= 10000.0 }.forEach { k ->
            list.add(
                KarigarRealtimeAlert(
                    id = "payout_${k.id}",
                    severity = "WARNING",
                    title = if (isHindi) "🟠 कारीगर पेमेंट बकाया: ${k.name} (₹${k.runningBalanceInr.toInt()})"
                    else "🟠 High Karigar Payable: ${k.name} (₹${k.runningBalanceInr.toInt()})",
                    subtitle = "${k.role} • Completed: ${k.totalPiecesDone} Pcs (${k.completedLots} Lots) • Rate: ₹${k.rateAmount}/${k.rateUnit} • Speed Bonus: +₹${k.earnedIncentiveInr.toInt()}",
                    actionLabel = if (isHindi) "WhatsApp हिसाब भेजें" else "WhatsApp Ledger",
                    phone = k.phone,
                    whatsappMessage = """
🚩 *C B CREATIONS (BECOOPER) – KARIGAR LEDGER* 🚩
कारीगर: ${k.name} (${k.role})
कुल पीस: ${k.totalPiecesDone} Pcs (${k.completedLots} Lots)
दर: ₹${k.rateAmount} (${k.rateUnit})
स्पीड बोनस: +₹${k.earnedIncentiveInr.toInt()}
वर्तमान बकाया (Balance Due): ₹${k.runningBalanceInr.toInt()}
📞 98292 11122 | Sanganer, Jaipur
                    """.trimIndent(),
                    associatedKarigar = k
                )
            )
        }

        // D. Material Shortage Impacting Karigars
        lowStockMaterials.forEach { mat ->
            list.add(
                KarigarRealtimeAlert(
                    id = "stock_${mat.skuCode}",
                    severity = "CRITICAL",
                    title = if (isHindi) "🔴 धागा/मटेरियल कम स्टॉक: ${mat.name}"
                    else "🔴 Material Shortage Alert: ${mat.name}",
                    subtitle = "Assigned Lot: ${mat.assignedLotNo} • Stock: ${mat.currentStock} ${mat.unit} (Reorder @ ${mat.reorderLevel} ${mat.unit}) • Shade: ${mat.colorShade}",
                    actionLabel = if (isHindi) "स्टॉक देखें" else "View Inventory",
                    phone = "9829211122",
                    whatsappMessage = "URGENT PO ALERT - C B CREATIONS: ${mat.name} (${mat.colorShade}) for ${mat.assignedLotNo} is down to ${mat.currentStock} ${mat.unit}."
                )
            )
        }

        list
    }

    val filteredAlerts = remember(karigarAlerts, selectedAlertFilter, searchQuery) {
        karigarAlerts.filter { alert ->
            val filterMatch = when (selectedAlertFilter) {
                "CRITICAL" -> alert.severity == "CRITICAL"
                "FLOOR" -> alert.severity == "ACTIVE_FLOOR"
                "PAYOUT" -> alert.severity == "WARNING"
                else -> true
            }
            val queryMatch = searchQuery.isBlank() ||
                alert.title.contains(searchQuery, ignoreCase = true) ||
                alert.subtitle.contains(searchQuery, ignoreCase = true)
            filterMatch && queryMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_erp_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // 1. HERO BRAND HEADER & EXECUTIVE KPI SUMMARY STRIP
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_textile),
                        contentDescription = "C B Creations Jaipur Garment Factory Floor",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xAA0B1528),
                                        Color(0xF20B1528)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = companyProfile.firmName,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = Color(0xFFD97706),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "LIVE ERP",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${companyProfile.brandName} • GSTIN: ${companyProfile.gstin}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFD599)
                                    )
                                    Text(
                                        text = "61, Bus Stand K Pass, Shyopur Marg, Sanganer, Jaipur - 302033",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onNavigateToTab(5) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_dashboard_ai_copilot")
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD599),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "AI असिस्टेंट" else "AI & RBAC",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            // 4 Executive Summary KPI Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardTopMetricBox(
                                    label = if (isHindi) "कुल उत्पादन" else "Total Output",
                                    value = "$totalPiecesInProduction Pcs",
                                    sub = "${challans.size} Active Lots",
                                    accent = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardTopMetricBox(
                                    label = if (isHindi) "पेंडिंग चालान" else "Pending Challans",
                                    value = "${pendingChallans.size} Lots",
                                    sub = "$reworkOrPendingPieces Pcs Hold",
                                    accent = Color(0xFFFBBF24),
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardTopMetricBox(
                                    label = if (isHindi) "लाइव कारीगर" else "Floor Karigars",
                                    value = "$activeFloorKarigars/${karigars.size}",
                                    sub = "Punched In",
                                    accent = Color(0xFF34D399),
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardTopMetricBox(
                                    label = if (isHindi) "कारीगर अलर्ट" else "Active Alerts",
                                    value = "${karigarAlerts.size}",
                                    sub = "₹${(totalKarigarPayable / 1000).toInt()}k Due",
                                    accent = Color(0xFFF87171),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. PRODUCTION STATUS SUMMARY CARD (WITH CUSTOM CANVAS PROPORTION BAR)
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_production_status_summary"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = if (isHindi) "1. उत्पादन स्थिति सारांश (Production Status)"
                                    else "1. Production Status Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "स्टेज-वाइज पीस, QC पास दर और वर्कफ़्लो प्रगति"
                                    else "Real-time stage distribution, QC yield & lot velocity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = { onNavigateToTab(1) },
                            modifier = Modifier.testTag("btn_view_all_production")
                        ) {
                            Text(if (isHindi) "सभी लॉट" else "All Lots")
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Canvas Multi-Segment Production Health Bar
                    val passRatio = if (totalPiecesInProduction > 0) {
                        qcPassedPieces.toFloat() / totalPiecesInProduction.toFloat()
                    } else 0.7f
                    val reworkRatio = (1f - passRatio).coerceIn(0f, 1f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                    ) {
                        val totalW = size.width
                        val passW = totalW * passRatio
                        drawRoundRect(
                            color = EmeraldQcPass,
                            topLeft = Offset.Zero,
                            size = Size(passW, size.height),
                            cornerRadius = CornerRadius(7f, 7f)
                        )
                        if (reworkRatio > 0f) {
                            drawRoundRect(
                                color = AmberReworkWarn,
                                topLeft = Offset(passW, 0f),
                                size = Size(totalW - passW, size.height),
                                cornerRadius = CornerRadius(7f, 7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✅ QC Passed / Ready: $qcPassedPieces Pcs (${(passRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldQcPass,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⚠️ In Rework / Hold: $reworkOrPendingPieces Pcs (${(reworkRatio * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = AmberReworkWarn,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    // 6-Stage Mini Summary Grid (2 rows of 3 cards)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        stageBreakdown.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { (stageName, pcsCount, lotCount) ->
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onNavigateToTab(1) },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = stageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "$pcsCount Pcs",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "$lotCount Active Lot(s)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 3. REAL-TIME KARIGAR ALERTS SUMMARY CARD (FLOOR, REWORK, PAYOUT, STOCK)
        // =====================================================================
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_karigar_alerts_summary"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CrimsonDefectAlert
                            )
                            Column {
                                Text(
                                    text = if (isHindi) "2. रीयल-टाइम कारीगर व फ्लोर अलर्ट (${filteredAlerts.size})"
                                    else "2. Real-Time Karigar & Floor Alerts (${filteredAlerts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isHindi) "रीवर्क, लाइव पंच-इन टाइमर, बकाया पेमेंट और धागा शॉर्टेज"
                                    else "Live punch-in telemetry, QC rework flags & payout alerts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(
                            onClick = { onNavigateToTab(2) },
                            modifier = Modifier.testTag("btn_open_karigar_hub")
                        ) {
                            Text(if (isHindi) "कारीगर हब" else "Karigars")
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Filter Chips for Alert Types
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to (if (isHindi) "सभी (${karigarAlerts.size})" else "All Alerts (${karigarAlerts.size})"),
                            "CRITICAL" to (if (isHindi) "🔴 रीवर्क / शॉर्टेज" else "🔴 Critical / Rework"),
                            "FLOOR" to (if (isHindi) "🟢 लाइव पंच-इन" else "🟢 Live Floor Active"),
                            "PAYOUT" to (if (isHindi) "🟠 पेमेंट बकाया" else "🟠 Payout Due")
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedAlertFilter == key,
                                onClick = { selectedAlertFilter = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("chip_alert_filter_$key")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (filteredAlerts.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isHindi) "✅ कोई पेंडिंग कारीगर अलर्ट या टेस्ट डेटा नहीं है (डेटाबेस साफ़ है)।"
                                else "✅ No active Karigar alerts (Database is clean — 0 test records).",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        filteredAlerts.forEach { alert ->
                            val containerTone = when (alert.severity) {
                                "CRITICAL" -> CrimsonDefectAlert.copy(alpha = 0.10f)
                                "ACTIVE_FLOOR" -> EmeraldQcPass.copy(alpha = 0.10f)
                                else -> AmberReworkWarn.copy(alpha = 0.12f)
                            }
                            val borderAccent = when (alert.severity) {
                                "CRITICAL" -> CrimsonDefectAlert
                                "ACTIVE_FLOOR" -> EmeraldQcPass
                                else -> AmberReworkWarn
                            }

                            Surface(
                                color = containerTone,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("karigar_alert_item_${alert.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = alert.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = borderAccent,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = alert.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (alert.associatedKarigar != null && alert.severity == "ACTIVE_FLOOR") {
                                            OutlinedButton(
                                                onClick = {
                                                    onToggleKarigarPunch(
                                                        alert.associatedKarigar,
                                                        alert.associatedKarigar.activeLotNo
                                                    )
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("btn_alert_punch_${alert.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Stop,
                                                    contentDescription = null,
                                                    tint = CrimsonDefectAlert,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(alert.actionLabel, style = MaterialTheme.typography.labelSmall)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        if (canWhatsApp) {
                                            Button(
                                                onClick = {
                                                    WhatsAppTemplateEngine.launchWhatsAppIntent(
                                                        context = context,
                                                        rawPhone = alert.phone,
                                                        message = alert.whatsappMessage,
                                                        useWhatsAppWeb = useWhatsAppWebMode
                                                    )
                                                    onShowMessage("Dispatched WhatsApp alert to +91 ${alert.phone}")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("btn_alert_whatsapp_${alert.id}")
                                            ) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isHindi) "WhatsApp भेजें" else "WhatsApp Alert",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. PENDING CHALLANS & DEADLINE TRACKER SUMMARY CARDS
        // =====================================================================
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = if (isHindi) "3. पेंडिंग चालान व लॉट ट्रैकर (${pendingChallans.size})"
                            else "3. Pending Challans & Lot Tracker (${pendingChallans.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHindi) "साइज-वाइज (S-5XL) विवरण, समय सीमा और सीधा WhatsApp डिस्पैच"
                            else "Size-wise S–5XL quantities, deadline hours & instant WhatsApp dispatch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = { onNavigateToTab(1) },
                    modifier = Modifier.testTag("btn_manage_all_challans")
                ) {
                    Text(if (isHindi) "नया चालान +" else "Manage All")
                }
            }
            if (pendingChallans.isEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "कोई पेंडिंग चालान नहीं है (टेस्ट डेटा हटा दिया गया है)।"
                            else "No pending challans (Test data cleared — ready for live entries).",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { onNavigateToTab(4) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(if (isHindi) "रिपोर्ट्स व WhatsApp" else "Reports & WA", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        items(pendingChallans, key = { it.id }) { challan ->
            val isDelayed = challan.actualHoursTaken > challan.targetDeadlineHours
            val statusColor = when {
                challan.qcStatus == "REWORK_REQUIRED" -> CrimsonDefectAlert
                isDelayed -> AmberReworkWarn
                else -> EmeraldQcPass
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_pending_challan_${challan.challanNo}"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = challan.challanNo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${challan.lotNo} • ${challan.workflowStage}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${challan.itemName} — ${challan.karigarName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${challan.qcStatus} (${challan.actualHoursTaken}h/${challan.targetDeadlineHours}h)",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sizes: S:${challan.sizeS} M:${challan.sizeM} L:${challan.sizeL} XL:${challan.sizeXl} XXL:${challan.sizeXxl} 3XL:${challan.size3xl} 4XL:${challan.size4xl} 5XL:${challan.size5xl} • Total: ${challan.totalPcs} Pcs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Thread: ${challan.kathaThreadColor} • Cutting: ${challan.cuttingPcs} Pcs • Labels: ${challan.labelPcs} Pcs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToTab(3) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_dashboard_qc_${challan.challanNo}")
                        ) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "QC जांच करें" else "Inspect QC",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        if (canWhatsApp) {
                            Button(
                                onClick = {
                                    val msg = WhatsAppTemplateEngine.formatChallanMessage(challan)
                                    WhatsAppTemplateEngine.launchWhatsAppIntent(
                                        context = context,
                                        rawPhone = challan.karigarPhone,
                                        message = msg
                                    )
                                    onShowMessage("Sent Hindi Challan ${challan.challanNo} to ${challan.karigarName} via WhatsApp")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_dashboard_wa_${challan.challanNo}")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHindi) "🟢 WhatsApp चालान" else "🟢 SEND WHATSAPP",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTopMetricBox(
    label: String,
    value: String,
    sub: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0x2EFFFFFF),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE2E8F0),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
