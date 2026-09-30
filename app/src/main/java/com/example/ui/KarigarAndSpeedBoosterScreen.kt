package com.example.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.JobChallanEntity
import com.example.data.local.KarigarEntity
import com.example.data.local.ManualTimeLogEntity
import com.example.domain.JobCostingAndGstEngine
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen
import kotlin.math.roundToInt

@Composable
fun KarigarAndSpeedBoosterScreen(
    karigars: List<KarigarEntity>,
    challans: List<JobChallanEntity>,
    manualTimeLogs: List<ManualTimeLogEntity>,
    activeRole: String,
    isHindi: Boolean,
    searchQuery: String,
    useWhatsAppWebMode: Boolean,
    canCreate: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    canWhatsApp: Boolean,
    onSaveKarigar: (KarigarEntity, Boolean, String) -> Unit,
    onSaveManualTimeLog: (ManualTimeLogEntity, Boolean, String) -> Unit,
    onDeleteManualTimeLog: (ManualTimeLogEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedMainView by remember { mutableIntStateOf(0) }
    // 0 = Manual Time Register (Karigars + Other Staff), 1 = Karigar Rate Master & Speed Booster

    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, KARIGAR, OTHER_STAFF
    var editingTimeLog by remember { mutableStateOf<ManualTimeLogEntity?>(null) }
    var showNewTimeLogDialog by remember { mutableStateOf(false) }
    var prefillKarigarForTimeLog by remember { mutableStateOf<KarigarEntity?>(null) }

    var editingKarigar by remember { mutableStateOf<KarigarEntity?>(null) }
    var showNewKarigarDialog by remember { mutableStateOf(false) }

    // Interactive Speed Booster Simulator state
    var simPieces by remember { mutableStateOf("240") }
    var simRate by remember { mutableStateOf("68") }
    var simTargetHours by remember { mutableStateOf("48") }
    var simActualHours by remember { mutableStateOf("36") }
    var simDefects by remember { mutableStateOf("0") }

    val filteredTimeLogs = remember(manualTimeLogs, selectedCategoryFilter, searchQuery) {
        manualTimeLogs.filter { log ->
            val catMatch = selectedCategoryFilter == "ALL" || log.personCategory == selectedCategoryFilter
            val queryMatch = searchQuery.isBlank() ||
                log.personName.contains(searchQuery, ignoreCase = true) ||
                log.roleOrDepartment.contains(searchQuery, ignoreCase = true) ||
                log.lotNo.contains(searchQuery, ignoreCase = true) ||
                log.workDate.contains(searchQuery, ignoreCase = true)
            catMatch && queryMatch
        }
    }

    val filteredKarigars = remember(karigars, searchQuery) {
        if (searchQuery.isBlank()) karigars
        else karigars.filter { k ->
            k.name.contains(searchQuery, ignoreCase = true) ||
                k.role.contains(searchQuery, ignoreCase = true) ||
                k.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalKarigarManualHours = remember(manualTimeLogs) {
        manualTimeLogs.filter { it.personCategory == "KARIGAR" }.sumOf { it.totalHoursManual }
    }
    val totalStaffManualHours = remember(manualTimeLogs) {
        manualTimeLogs.filter { it.personCategory == "OTHER_STAFF" }.sumOf { it.totalHoursManual }
    }
    val totalManualPieces = remember(manualTimeLogs) {
        manualTimeLogs.sumOf { it.piecesCompleted }
    }
    val totalManualPayable = remember(manualTimeLogs) {
        manualTimeLogs.sumOf { it.totalEarnedInr }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("karigar_speed_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top View Switcher: Manual Time Register vs Karigar Rate Master & Speed Booster
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMainView == 0,
                    onClick = { selectedMainView = 0 },
                    label = {
                        Text(
                            if (isHindi) "1. ⏱️ मैन्युअल टाइम रजिस्टर (${manualTimeLogs.size})"
                            else "1. ⏱️ Manual Time Entry (${manualTimeLogs.size})"
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("tab_manual_time_register")
                )
                FilterChip(
                    selected = selectedMainView == 1,
                    onClick = { selectedMainView = 1 },
                    label = {
                        Text(
                            if (isHindi) "2. 👷 कारीगर रेट मास्टर व बोनस (${karigars.size})"
                            else "2. 👷 Karigar Master & Speed Booster (${karigars.size})"
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.testTag("tab_karigar_rate_master")
                )
            }
        }

        // =====================================================================
        // VIEW 0: MANUAL TIME ENTRY REGISTER (KARIGARS + ALL OTHER STAFF)
        // =====================================================================
        if (selectedMainView == 0) {
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_manual_time_header"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "⏱️ मैन्युअल टाइम एंट्री (कारीगर व बाकी स्टाफ)"
                                    else "⏱️ Manual Time Entry (Karigars & All Staff)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isHindi) "आने का समय (In-Time), जाने का समय (Out-Time), कुल घंटे और पीस मैन्युअल दर्ज करें"
                                    else "Manually enter In-Time, Out-Time, Total Hours, Lot No., Pieces & Daily/Piece Wage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }

                            if (canCreate) {
                                Button(
                                    onClick = {
                                        prefillKarigarForTimeLog = null
                                        showNewTimeLogDialog = true
                                    },
                                    modifier = Modifier.testTag("btn_add_manual_time")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHindi) "टाइम डालें" else "Add Time")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ManualTimeSummaryBox(
                                label = if (isHindi) "कारीगर घंटे" else "Karigar Hrs",
                                value = "${String.format("%.1f", totalKarigarManualHours)} Hrs",
                                modifier = Modifier.weight(1f)
                            )
                            ManualTimeSummaryBox(
                                label = if (isHindi) "बाकी स्टाफ घंटे" else "Other Staff Hrs",
                                value = "${String.format("%.1f", totalStaffManualHours)} Hrs",
                                modifier = Modifier.weight(1f)
                            )
                            ManualTimeSummaryBox(
                                label = if (isHindi) "कुल पीस / राशि" else "Pcs / Earned",
                                value = "$totalManualPieces Pcs • ₹${totalManualPayable.toInt()}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Category Filter Chips (All / Karigars / Other Staff)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to (if (isHindi) "सभी एंट्री (${manualTimeLogs.size})" else "All Entries (${manualTimeLogs.size})"),
                        "KARIGAR" to (if (isHindi) "🧵 कारीगर (Karigars)" else "🧵 Karigars (Piece/Hour)"),
                        "OTHER_STAFF" to (if (isHindi) "👥 बाकी स्टाफ / वर्कर (Staff)" else "👥 Other Staff (Supervisor/Helper)")
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategoryFilter == key,
                            onClick = { selectedCategoryFilter = key },
                            label = { Text(label) },
                            modifier = Modifier.testTag("chip_time_cat_$key")
                        )
                    }
                }
            }

            if (filteredTimeLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isHindi) "अभी कोई मैन्युअल टाइम एंट्री नहीं है।"
                                else "No manual time entries recorded yet.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHindi) "ऊपर 'टाइम डालें (Add Time)' बटन दबाकर कारीगर या स्टाफ का आने-जाने का समय और कुल घंटे दर्ज करें।"
                                else "Tap 'Add Time' above to manually log In-Time, Out-Time, Total Hours, and Pieces for any Karigar or Staff member.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(filteredTimeLogs, key = { it.id }) { log ->
                val isKarigar = log.personCategory == "KARIGAR"
                val pcsPerHour = if (log.totalHoursManual > 0 && log.piecesCompleted > 0) {
                    String.format("%.1f", log.piecesCompleted.toDouble() / log.totalHoursManual)
                } else "-"
                val minsPerPiece = if (log.piecesCompleted > 0 && log.totalHoursManual > 0) {
                    String.format("%.1f", (log.totalHoursManual * 60.0) / log.piecesCompleted.toDouble())
                } else "-"

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_time_card_${log.id}"),
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
                                    Surface(
                                        color = if (isKarigar) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isKarigar) "कारीगर (KARIGAR)" else "स्टाफ (OTHER STAFF)",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = log.roleOrDepartment,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${log.personName} • 📅 ${log.workDate}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                color = EmeraldQcPass.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "${log.totalHoursManual} Hrs",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldQcPass
                                    )
                                    Text(
                                        text = "₹${log.totalEarnedInr.toInt()}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🕒 आने का समय (In): ${log.inTimeManual}  →  जाने का समय (Out): ${log.outTimeManual}  |  ब्रेक: ${log.breakMinutesManual}m  |  कुल: ${log.totalHoursManual} घंटे",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (log.piecesCompleted > 0 || log.lotNo.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "📦 लॉट: ${log.lotNo.ifBlank { "-" }} • तैयार पीस: ${log.piecesCompleted} Pcs • स्पीड: $pcsPerHour Pcs/Hr ($minsPerPiece Min/Pc)",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = "💰 दर (Rate): ₹${log.rateAmountInr} (${log.rateType}) • कुल राशि: ₹${log.totalEarnedInr.toInt()} ${if (log.remarks.isNotBlank()) "• ${log.remarks}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canWhatsApp) {
                                val waMsg = """
🚩 *C B CREATIONS (BECOOPER) – MANUAL TIME & WAGE SLIP* 🚩
👤 नाम: ${log.personName} (${log.roleOrDepartment})
📅 दिनांक: ${log.workDate}
🕒 समय (In - Out): ${log.inTimeManual} से ${log.outTimeManual} (ब्रेक: ${log.breakMinutesManual}m)
⏱️ कुल काम के घंटे: *${log.totalHoursManual} Hours*
${if (log.piecesCompleted > 0) "📦 लॉट: ${log.lotNo} | तैयार पीस: ${log.piecesCompleted} Pcs ($pcsPerHour Pcs/Hr)\n" else ""}💰 दर: ₹${log.rateAmountInr} (${log.rateType})
✅ कुल देय राशि (Total Earned): *₹${log.totalEarnedInr.toInt()}*
📞 98292 11122 | Sanganer, Jaipur
                                """.trimIndent()

                                Button(
                                    onClick = {
                                        WhatsAppTemplateEngine.launchWhatsAppIntent(
                                            context = context,
                                            rawPhone = log.phone,
                                            message = waMsg,
                                            useWhatsAppWeb = useWhatsAppWebMode
                                        )
                                        onShowMessage("Sent Manual Time Slip for ${log.personName} via WhatsApp")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (useWhatsAppWebMode) "WA Web Slip" else "WhatsApp Slip",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (canEdit) {
                                IconButton(onClick = { editingTimeLog = log }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Manual Time")
                                }
                            }
                            if (canDelete) {
                                IconButton(onClick = { onDeleteManualTimeLog(log) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Manual Time",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =====================================================================
            // VIEW 1: KARIGAR RATE MASTER & SPEED BOOSTER SIMULATOR
            // =====================================================================
            item {
                val baseJobInr = (simPieces.toIntOrNull() ?: 240) * (simRate.toDoubleOrNull() ?: 68.0)
                val defCount = simDefects.toIntOrNull() ?: 0
                val speedEval = JobCostingAndGstEngine.evaluateSpeedBooster(
                    baseJobAmountInr = baseJobInr,
                    targetDeadlineHours = simTargetHours.toIntOrNull() ?: 48,
                    actualHoursTaken = simActualHours.toIntOrNull() ?: 36,
                    defectivePieces = defCount,
                    qcStatus = if (defCount == 0) "PASSED" else "REWORK_REQUIRED"
                )

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = "Speed Booster", tint = AmberReworkWarn)
                            Column {
                                Text(
                                    text = if (isHindi) "प्रोडक्शन स्पीड बूस्टर कैलकुलेटर (मैन्युअल घंटों के आधार पर)"
                                    else "Production Speed Booster Calculator (Based on Manual Hours)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Enter target hours vs. manual actual hours taken by Karigar to calculate bonus",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = simPieces,
                                onValueChange = { simPieces = it },
                                label = { Text("Lot Pcs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = simRate,
                                onValueChange = { simRate = it },
                                label = { Text("Rate ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = simTargetHours,
                                onValueChange = { simTargetHours = it },
                                label = { Text("Target Hrs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = simActualHours,
                                onValueChange = { simActualHours = it },
                                label = { Text("Manual Hrs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = simDefects,
                                onValueChange = { simDefects = it },
                                label = { Text("Defects") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = if (speedEval.bonusAmountInr > 0) EmeraldQcPass.copy(alpha = 0.12f)
                            else AmberReworkWarn.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🏆 ${speedEval.tierBadge} • Speed Score: ${speedEval.speedScore}/100",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (speedEval.bonusAmountInr > 0) EmeraldQcPass else AmberReworkWarn
                                )
                                Text(
                                    text = "Base Pay: ₹${baseJobInr.toInt()} + Bonus: ₹${speedEval.bonusAmountInr.toInt()} = Total: ₹${(baseJobInr + speedEval.bonusAmountInr).toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "कारीगर रेट मास्टर व खाता (${filteredKarigars.size})"
                        else "Karigar Rate Master & Ledger (${filteredKarigars.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (canCreate) {
                        Button(
                            onClick = { showNewKarigarDialog = true },
                            modifier = Modifier.testTag("btn_add_karigar")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "नया कारीगर / स्टाफ" else "Add Karigar")
                        }
                    }
                }
            }

            itemsIndexed(filteredKarigars, key = { _, k -> k.id }) { _, karigar ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("karigar_card_${karigar.id}"),
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
                                    text = karigar.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${karigar.role} • +91 ${karigar.phone} • Rate: ₹${karigar.rateAmount} (${karigar.rateUnit})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Due: ₹${karigar.runningBalanceInr.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canCreate) {
                                OutlinedButton(
                                    onClick = {
                                        prefillKarigarForTimeLog = karigar
                                        showNewTimeLogDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHindi) "मैन्युअल टाइम डालें" else "Enter Manual Time",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (canEdit) {
                                IconButton(onClick = { editingKarigar = karigar }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Karigar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Manual Time Entry Dialog (For Karigars AND All Other Factory Staff) ---
    if (showNewTimeLogDialog || editingTimeLog != null) {
        ManualTimeLogEditorDialog(
            existing = editingTimeLog,
            prefillKarigar = prefillKarigarForTimeLog,
            isHindi = isHindi,
            onDismiss = {
                showNewTimeLogDialog = false
                editingTimeLog = null
                prefillKarigarForTimeLog = null
            },
            onConfirm = { log, isEdit, prev ->
                onSaveManualTimeLog(log, isEdit, prev)
                showNewTimeLogDialog = false
                editingTimeLog = null
                prefillKarigarForTimeLog = null
            }
        )
    }

    // --- Karigar Master Editor Dialog ---
    if (showNewKarigarDialog || editingKarigar != null) {
        KarigarEditorDialog(
            existing = editingKarigar,
            onDismiss = {
                showNewKarigarDialog = false
                editingKarigar = null
            },
            onConfirm = { updated, isEdit, prev ->
                onSaveKarigar(updated, isEdit, prev)
                showNewKarigarDialog = false
                editingKarigar = null
            }
        )
    }
}

@Composable
private fun ManualTimeSummaryBox(
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

@Composable
private fun ManualTimeLogEditorDialog(
    existing: ManualTimeLogEntity?,
    prefillKarigar: KarigarEntity?,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (ManualTimeLogEntity, Boolean, String) -> Unit
) {
    val isEdit = existing != null
    var personCategory by remember {
        mutableStateOf(existing?.personCategory ?: "KARIGAR")
    }
    var personName by remember {
        mutableStateOf(existing?.personName ?: prefillKarigar?.name ?: "")
    }
    var roleOrDept by remember {
        mutableStateOf(existing?.roleOrDepartment ?: prefillKarigar?.role ?: "Fabricator (Stitching)")
    }
    var phone by remember {
        mutableStateOf(existing?.phone ?: prefillKarigar?.phone ?: "9829211122")
    }
    var workDate by remember {
        mutableStateOf(existing?.workDate ?: "30/09/2026")
    }
    var inTimeManual by remember {
        mutableStateOf(existing?.inTimeManual ?: "09:30 AM")
    }
    var outTimeManual by remember {
        mutableStateOf(existing?.outTimeManual ?: "06:30 PM")
    }
    var totalHoursManual by remember {
        mutableStateOf((existing?.totalHoursManual ?: 8.5).toString())
    }
    var breakMinutes by remember {
        mutableStateOf((existing?.breakMinutesManual ?: 30).toString())
    }
    var lotNo by remember {
        mutableStateOf(existing?.lotNo ?: "LOT-CB-401")
    }
    var piecesCompleted by remember {
        mutableStateOf((existing?.piecesCompleted ?: 0).toString())
    }
    var rateType by remember {
        mutableStateOf(
            existing?.rateType ?: if (personCategory == "KARIGAR") "PER_PIECE" else "PER_DAY"
        )
    }
    var rateAmount by remember {
        mutableStateOf((existing?.rateAmountInr ?: prefillKarigar?.rateAmount ?: 68.0).toString())
    }
    var remarks by remember {
        mutableStateOf(existing?.remarks ?: "")
    }

    val hrsVal = totalHoursManual.toDoubleOrNull() ?: 0.0
    val pcsVal = piecesCompleted.toIntOrNull() ?: 0
    val rateVal = rateAmount.toDoubleOrNull() ?: 0.0

    val computedEarned = remember(rateType, hrsVal, pcsVal, rateVal) {
        val raw = when (rateType) {
            "PER_PIECE" -> pcsVal * rateVal
            "PER_HOUR" -> hrsVal * rateVal
            else -> rateVal // PER_DAY
        }
        (raw * 100.0).roundToInt() / 100.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isEdit) "मैन्युअल टाइम एडिट करें (Edit Manual Time)"
                else "⏱️ मैन्युअल टाइम डालें (कारीगर व बाकी स्टाफ)"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1. Category Toggle: Karigar vs Other Staff
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = personCategory == "KARIGAR",
                        onClick = {
                            personCategory = "KARIGAR"
                            rateType = "PER_PIECE"
                            roleOrDept = "Fabricator (Stitching)"
                        },
                        label = { Text("🧵 कारीगर (Karigar)") }
                    )
                    FilterChip(
                        selected = personCategory == "OTHER_STAFF",
                        onClick = {
                            personCategory = "OTHER_STAFF"
                            rateType = "PER_DAY"
                            roleOrDept = "Supervisor / मास्टर जी"
                        },
                        label = { Text("👥 बाकी स्टाफ (Other Staff)") }
                    )
                }

                // 2. Quick Role / Department Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val rolesList = if (personCategory == "KARIGAR") {
                        listOf(
                            "Fabricator (Stitching)",
                            "Katha Worker",
                            "Dhaga Cutter",
                            "Fabric Cutter",
                            "Embroidery Worker",
                            "Pressman",
                            "Packer"
                        )
                    } else {
                        listOf(
                            "Supervisor / मास्टर जी",
                            "Helper / हेल्पर",
                            "Cutting Master",
                            "Store Keeper",
                            "QC Inspector",
                            "Packing / Dispatch Staff",
                            "Office / Accounts Staff"
                        )
                    }
                    rolesList.forEach { r ->
                        FilterChip(
                            selected = roleOrDept == r,
                            onClick = { roleOrDept = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { personName = it },
                        label = { Text("नाम (Name)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = workDate,
                        onValueChange = { workDate = it },
                        label = { Text("दिनांक (Date)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // 3. Manual In-Time, Out-Time, and Total Hours
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = inTimeManual,
                        onValueChange = { inTimeManual = it },
                        label = { Text("आने का समय (In)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = outTimeManual,
                        onValueChange = { outTimeManual = it },
                        label = { Text("जाने का समय (Out)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = totalHoursManual,
                        onValueChange = { totalHoursManual = it },
                        label = { Text("कुल घंटे (Total Hrs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = breakMinutes,
                        onValueChange = { breakMinutes = it },
                        label = { Text("ब्रेक (Mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                // 4. Lot No, Pieces Completed & Wage Basis
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = lotNo,
                        onValueChange = { lotNo = it },
                        label = { Text("लॉट नं. (Lot)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = piecesCompleted,
                        onValueChange = { piecesCompleted = it },
                        label = { Text("तैयार पीस (Pcs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "PER_PIECE" to "प्रति पीस (Per Piece)",
                        "PER_HOUR" to "प्रति घंटा (Per Hour)",
                        "PER_DAY" to "दिहाड़ी / रोज़ाना (Per Day)"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = rateType == key,
                            onClick = { rateType = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateAmount,
                        onValueChange = { rateAmount = it },
                        label = { Text("दर / दिहाड़ी ₹ (Rate)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("नोट्स (Remarks)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✅ ऑटो हिसाब: ${hrsVal} घंटे | ${pcsVal} पीस | कुल देय राशि = ₹$computedEarned",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prev = existing?.let {
                        "${it.inTimeManual}-${it.outTimeManual} (${it.totalHoursManual}h), Pcs=${it.piecesCompleted}"
                    } ?: "None"
                    onConfirm(
                        ManualTimeLogEntity(
                            id = existing?.id ?: 0,
                            personName = personName.trim().ifBlank { "Staff / Karigar" },
                            personCategory = personCategory,
                            roleOrDepartment = roleOrDept,
                            phone = phone.trim(),
                            workDate = workDate.trim(),
                            inTimeManual = inTimeManual.trim(),
                            outTimeManual = outTimeManual.trim(),
                            totalHoursManual = hrsVal,
                            breakMinutesManual = breakMinutes.toIntOrNull() ?: 0,
                            lotNo = lotNo.trim(),
                            piecesCompleted = pcsVal,
                            rateType = rateType,
                            rateAmountInr = rateVal,
                            totalEarnedInr = computedEarned,
                            remarks = remarks.trim()
                        ),
                        isEdit,
                        prev
                    )
                },
                modifier = Modifier.testTag("btn_save_manual_time_dialog")
            ) {
                Text(if (isEdit) "अपडेट करें (Update)" else "सेव करें (Save Time)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun KarigarEditorDialog(
    existing: KarigarEntity?,
    onDismiss: () -> Unit,
    onConfirm: (KarigarEntity, Boolean, String) -> Unit
) {
    val isEdit = existing != null
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var role by remember { mutableStateOf(existing?.role ?: "Fabricator (Stitching)") }
    var phone by remember { mutableStateOf(existing?.phone ?: "9829211122") }
    var rateAmount by remember { mutableStateOf((existing?.rateAmount ?: 68.0).toString()) }
    var rateUnit by remember { mutableStateOf(existing?.rateUnit ?: "PER_PIECE") }
    var balanceInr by remember { mutableStateOf((existing?.runningBalanceInr ?: 0.0).toString()) }
    var advancePaidInr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Edit Karigar / Staff Rate & Ledger" else "Add New Karigar / Staff to Master")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name (कारीगर / स्टाफ का नाम)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Fabricator (Stitching)",
                        "Katha Worker",
                        "Fabric Cutter",
                        "Embroidery Worker",
                        "Dhaga Cutter",
                        "Pressman",
                        "Packer",
                        "Supervisor / Staff"
                    ).forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("WhatsApp Mobile") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateUnit,
                        onValueChange = { rateUnit = it },
                        label = { Text("Unit (PER_PIECE/DAY/HR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rateAmount,
                        onValueChange = { rateAmount = it },
                        label = { Text("Rate ₹") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = balanceInr,
                        onValueChange = { balanceInr = it },
                        label = { Text("Current Balance ₹") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                if (isEdit) {
                    OutlinedTextField(
                        value = advancePaidInr,
                        onValueChange = { advancePaidInr = it },
                        label = { Text("Deduct Advance / Instant Payment ₹") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prev = existing?.let {
                        "Role=${it.role}, Rate=₹${it.rateAmount}, Balance=₹${it.runningBalanceInr}"
                    } ?: "None"
                    val curBal = balanceInr.toDoubleOrNull() ?: 0.0
                    val deduct = advancePaidInr.toDoubleOrNull() ?: 0.0
                    val finalBal = (curBal - deduct).coerceAtLeast(0.0)

                    val entity = KarigarEntity(
                        id = existing?.id ?: 0,
                        name = name.trim().ifBlank { "New Karigar" },
                        role = role,
                        phone = phone.trim(),
                        rateAmount = rateAmount.toDoubleOrNull() ?: 50.0,
                        rateUnit = rateUnit.trim(),
                        runningBalanceInr = finalBal,
                        completedLots = existing?.completedLots ?: 0,
                        totalPiecesDone = existing?.totalPiecesDone ?: 0,
                        defectFreePercent = existing?.defectFreePercent ?: 100.0,
                        avgSpeedScore = existing?.avgSpeedScore ?: 90,
                        earnedIncentiveInr = existing?.earnedIncentiveInr ?: 0.0
                    )
                    onConfirm(entity, isEdit, prev)
                }
            ) {
                Text(if (isEdit) "Save & Audit Log" else "Create Karigar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
