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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.JobChallanEntity
import com.example.data.local.QcInspectionEntity
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen

@Composable
fun QualityControlScreen(
    qcInspections: List<QcInspectionEntity>,
    challans: List<JobChallanEntity>,
    isHindi: Boolean,
    searchQuery: String,
    canCreateQc: Boolean,
    canDeleteQc: Boolean,
    canWhatsApp: Boolean,
    onRecordQc: (QcInspectionEntity, String) -> Unit,
    onDeleteQc: (QcInspectionEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedStageFilter by remember { mutableStateOf("ALL") }
    var showNewQcDialog by remember { mutableStateOf(false) }

    val stages = listOf(
        "ALL" to "All Stages",
        "Post-Cutting" to "Post-Cutting",
        "Post-Katha Work" to "Post-Katha",
        "Post-Embroidery" to "Post-Embroidery",
        "Post-Stitching" to "Post-Stitching",
        "Post-Dhaga Cutting" to "Post-Dhaga",
        "Final Inspection" to "Final QC"
    )

    val filteredQc = remember(qcInspections, selectedStageFilter, searchQuery) {
        qcInspections.filter { qc ->
            val stageMatch = selectedStageFilter == "ALL" || qc.stage.equals(selectedStageFilter, ignoreCase = true)
            val queryMatch = searchQuery.isBlank() ||
                qc.lotNo.contains(searchQuery, ignoreCase = true) ||
                qc.challanNo.contains(searchQuery, ignoreCase = true) ||
                qc.karigarName.contains(searchQuery, ignoreCase = true) ||
                qc.defectReason.contains(searchQuery, ignoreCase = true)
            stageMatch && queryMatch
        }
    }

    val totalInspected = remember(qcInspections) { qcInspections.sumOf { it.inspectedPcs } }
    val totalDefects = remember(qcInspections) { qcInspections.sumOf { it.defectivePcs } }
    val passRate = if (totalInspected > 0) {
        ((totalInspected - totalDefects).toDouble() / totalInspected.toDouble()) * 100.0
    } else 100.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qc_inspection_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. QC Summary & Production Workflow Sync Banner
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                                text = if (isHindi) "क्वालिटी कंट्रोल (QC) और डिफेक्ट ट्रैकिंग इंजन"
                                else "Multi-Stage Quality Control (QC) Engine",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Post-Cutting • Post-Katha • Post-Embroidery • Post-Stitching • Post-Dhaga • Final Inspection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        if (canCreateQc) {
                            Button(
                                onClick = { showNewQcDialog = true },
                                modifier = Modifier.testTag("btn_record_qc")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isHindi) "नया QC चेक" else "Record QC")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QcKpiBox(
                            label = "Inspected Pcs",
                            value = "$totalInspected Pcs",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        QcKpiBox(
                            label = "Defective Pcs",
                            value = "$totalDefects Pcs",
                            color = if (totalDefects > 0) CrimsonDefectAlert else EmeraldQcPass,
                            modifier = Modifier.weight(1f)
                        )
                        QcKpiBox(
                            label = "First-Pass Yield",
                            value = "${String.format("%.1f", passRate)}%",
                            color = EmeraldQcPass,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 2. Stage Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stages.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedStageFilter == key,
                        onClick = { selectedStageFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        // 3. QC Inspection Cards with Defect Reason, Corrective Action & WhatsApp Alert
        items(filteredQc, key = { it.id }) { qc ->
            val statusColor = when (qc.qcDecision) {
                "PASSED" -> EmeraldQcPass
                "REWORK_REQUIRED" -> AmberReworkWarn
                else -> CrimsonDefectAlert
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qc_card_${qc.id}"),
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
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = qc.stage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "${qc.lotNo} (${qc.challanNo})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = qc.itemName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (qc.qcDecision) {
                                        "PASSED" -> Icons.Default.CheckCircle
                                        "REWORK_REQUIRED" -> Icons.Default.Warning
                                        else -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = qc.qcDecision,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = statusColor,
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
                                text = "Karigar: ${qc.karigarName} • Inspected: ${qc.inspectedPcs} Pcs • Defective: ${qc.defectivePcs} Pcs (${qc.severity})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔍 Defect / Observation: ${qc.defectReason}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "🛠️ Corrective Action: ${qc.correctiveAction}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QA Inspector: ${qc.inspectorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (canWhatsApp) {
                                Button(
                                    onClick = {
                                        val msg = WhatsAppTemplateEngine.formatQcInspectionAlert(qc)
                                        WhatsAppTemplateEngine.launchWhatsAppIntent(
                                            context = context,
                                            rawPhone = "9829211122",
                                            message = msg
                                        )
                                        onShowMessage("Sent Hindi QC Alert for ${qc.lotNo} via WhatsApp")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp QC", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            if (canDeleteQc) {
                                IconButton(onClick = { onDeleteQc(qc) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete QC Record",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewQcDialog) {
        RecordQcInspectionDialog(
            challans = challans,
            onDismiss = { showNewQcDialog = false },
            onConfirm = { inspection, nextStage ->
                onRecordQc(inspection, nextStage)
                showNewQcDialog = false
            }
        )
    }
}

@Composable
private fun QcKpiBox(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun RecordQcInspectionDialog(
    challans: List<JobChallanEntity>,
    onDismiss: () -> Unit,
    onConfirm: (QcInspectionEntity, String) -> Unit
) {
    val defaultChallan = challans.firstOrNull()
    var lotNo by remember { mutableStateOf(defaultChallan?.lotNo ?: "LOT-CB-401") }
    var challanNo by remember { mutableStateOf(defaultChallan?.challanNo ?: "CBC-ST-2026-101") }
    var itemName by remember { mutableStateOf(defaultChallan?.itemName ?: "Indigo V-Cut Katha Kurti (32\")") }
    var karigarName by remember { mutableStateOf(defaultChallan?.karigarName ?: "Rameshwar Lal Tailor") }
    var stage by remember { mutableStateOf("Post-Stitching") }
    var inspectedPcs by remember { mutableStateOf((defaultChallan?.totalPcs ?: 240).toString()) }
    var defectivePcs by remember { mutableStateOf("0") }
    var severity by remember { mutableStateOf("MINOR") }
    var defectReason by remember {
        mutableStateOf("13 SPI Lockstitch, 10x2.5\" Front Patti & 1.5\" V-Cut verified")
    }
    var correctiveAction by remember {
        mutableStateOf("Approve Lot for Steam Press & Credit Speed Booster Bonus")
    }
    var qcDecision by remember { mutableStateOf("PASSED") }
    var nextWorkflowStage by remember { mutableStateOf("PRESSING_PACKING") }
    var inspectorName by remember { mutableStateOf("Mahendra Sharma (Sr. QA)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Stage QC Inspection & Sync Workflow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Select Critical QC Stage:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Post-Cutting" to "KATHA_WORK",
                        "Post-Katha Work" to "EMBROIDERY",
                        "Post-Embroidery" to "STITCHING",
                        "Post-Stitching" to "DHAGA_CUTTING",
                        "Post-Dhaga Cutting" to "PRESSING_PACKING",
                        "Final Inspection" to "DISPATCHED"
                    ).forEach { (stg, nextStg) ->
                        FilterChip(
                            selected = stage == stg,
                            onClick = {
                                stage = stg
                                nextWorkflowStage = nextStg
                            },
                            label = { Text(stg, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lotNo,
                        onValueChange = { lotNo = it },
                        label = { Text("Lot No") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = challanNo,
                        onValueChange = { challanNo = it },
                        label = { Text("Challan No") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = karigarName,
                        onValueChange = { karigarName = it },
                        label = { Text("Karigar") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = inspectedPcs,
                        onValueChange = { inspectedPcs = it },
                        label = { Text("Inspected Pcs") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = defectivePcs,
                        onValueChange = {
                            defectivePcs = it
                            val def = it.toIntOrNull() ?: 0
                            if (def > 0) {
                                qcDecision = "REWORK_REQUIRED"
                                severity = if (def >= 10) "CRITICAL" else "MAJOR"
                                defectReason = "Stitching <13 SPI / Uncut Dhaga / Katha pattern mismatch"
                                correctiveAction = "Return $def Pcs to Karigar for immediate rework"
                            } else {
                                qcDecision = "PASSED"
                                severity = "MINOR"
                            }
                        },
                        label = { Text("Defects") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Quick Defect Reason Presets
                Text(
                    text = "Quick Defect Reason Presets:",
                    style = MaterialTheme.typography.labelSmall
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Zero Defects (13 SPI Exact)",
                        "Katha Design Mismatch (Sec 45)",
                        "Scissor Cut / कपड़ा डैमेज",
                        "Uncut Dhaga Threads (Sec 44)",
                        "Front Patti / V-Cut Variance"
                    ).forEach { preset ->
                        FilterChip(
                            selected = defectReason == preset,
                            onClick = { defectReason = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = defectReason,
                    onValueChange = { defectReason = it },
                    label = { Text("Defect Reason / Observation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = correctiveAction,
                    onValueChange = { correctiveAction = it },
                    label = { Text("Corrective Action & Workflow Sync") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("PASSED", "REWORK_REQUIRED", "QUARANTINED").forEach { dec ->
                        FilterChip(
                            selected = qcDecision == dec,
                            onClick = { qcDecision = dec },
                            label = { Text(dec, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entity = QcInspectionEntity(
                        lotNo = lotNo.trim(),
                        challanNo = challanNo.trim(),
                        itemName = itemName.trim(),
                        stage = stage,
                        karigarName = karigarName.trim(),
                        inspectedPcs = inspectedPcs.toIntOrNull() ?: 200,
                        defectivePcs = defectivePcs.toIntOrNull() ?: 0,
                        defectReason = defectReason.trim(),
                        severity = severity,
                        correctiveAction = correctiveAction.trim(),
                        qcDecision = qcDecision,
                        inspectorName = inspectorName.trim()
                    )
                    onConfirm(entity, nextWorkflowStage)
                },
                modifier = Modifier.testTag("btn_submit_qc_dialog")
            ) {
                Text("Save QC & Sync Lot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
