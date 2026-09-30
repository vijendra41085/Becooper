package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.JobChallanEntity
import com.example.data.local.KarigarEntity
import com.example.data.local.MaterialItemEntity
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.CrimsonDefectAlert
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen

@Composable
fun InventoryAndChallanScreen(
    companyProfile: CompanyMasterProfile,
    materials: List<MaterialItemEntity>,
    challans: List<JobChallanEntity>,
    karigars: List<KarigarEntity>,
    activeRole: String,
    isHindi: Boolean,
    searchQuery: String,
    canCreateInventory: Boolean,
    canEditInventory: Boolean,
    canDeleteInventory: Boolean,
    canCreateChallan: Boolean,
    canEditChallan: Boolean,
    canDeleteChallan: Boolean,
    canDispatchWhatsApp: Boolean,
    onSaveMaterial: (MaterialItemEntity, Boolean, String) -> Unit,
    onDeleteMaterial: (MaterialItemEntity) -> Unit,
    onSaveChallan: (JobChallanEntity, Boolean, String) -> Unit,
    onDeleteChallan: (JobChallanEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Job Challans & WhatsApp, 1 = Raw Material & Lot Inventory
    var whatsappPreviewChallan by remember { mutableStateOf<JobChallanEntity?>(null) }
    var editingChallan by remember { mutableStateOf<JobChallanEntity?>(null) }
    var showNewChallanDialog by remember { mutableStateOf(false) }
    var editingMaterial by remember { mutableStateOf<MaterialItemEntity?>(null) }
    var showNewMaterialDialog by remember { mutableStateOf(false) }

    val filteredChallans = remember(challans, searchQuery) {
        if (searchQuery.isBlank()) challans
        else challans.filter {
            it.challanNo.contains(searchQuery, ignoreCase = true) ||
                it.lotNo.contains(searchQuery, ignoreCase = true) ||
                it.karigarName.contains(searchQuery, ignoreCase = true) ||
                it.itemName.contains(searchQuery, ignoreCase = true) ||
                it.kathaThreadColor.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredMaterials = remember(materials, searchQuery) {
        if (searchQuery.isBlank()) materials
        else materials.filter {
            it.skuCode.contains(searchQuery, ignoreCase = true) ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.assignedLotNo.contains(searchQuery, ignoreCase = true) ||
                it.colorShade.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalPiecesInProduction = remember(challans) { challans.sumOf { it.totalPcs } }
    val lowStockCount = remember(materials) { materials.count { it.currentStock <= it.reorderLevel } }
    val totalKarigarPayable = remember(karigars) { karigars.sumOf { it.runningBalanceInr } }
    val reworkCount = remember(challans) { challans.count { it.qcStatus == "REWORK_REQUIRED" } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_challan_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Official C B CREATIONS Hero Banner + Live Database Metrics
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_textile),
                        contentDescription = "C B Creations Garment Workshop Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xAA0B1528),
                                        Color(0xEE0B1528)
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = companyProfile.firmName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "GSTIN: ${companyProfile.gstin} • Sanganer, Jaipur (08)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFFD599)
                                    )
                                    Text(
                                        text = "${companyProfile.contactPerson}: ${companyProfile.phones}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = Color(0x33FFFFFF),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "FY ${companyProfile.financialYear}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // 4 Live Database KPI Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HeroStatChip(
                                    label = if (isHindi) "कुल पीस (Today)" else "Active Pcs",
                                    value = "$totalPiecesInProduction Pcs",
                                    accent = Color(0xFF38BDF8),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = if (isHindi) "कम स्टॉक" else "Low Stock",
                                    value = "$lowStockCount SKU",
                                    accent = if (lowStockCount > 0) Color(0xFFFBBF24) else Color(0xFF34D399),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = if (isHindi) "रीवर्क (QC)" else "QC Rework",
                                    value = "$reworkCount Lots",
                                    accent = if (reworkCount > 0) Color(0xFFF87171) else Color(0xFF34D399),
                                    modifier = Modifier.weight(1f)
                                )
                                HeroStatChip(
                                    label = if (isHindi) "कारीगर बकाया" else "Karigar Due",
                                    value = "₹${totalKarigarPayable.toInt()}",
                                    accent = Color(0xFFFBBF24),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. End-to-End Connected Workflow Pipeline Strip (Sec 12 & 82)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isHindi) {
                            "कनेक्टेड जॉब ऑर्डर वर्कफ़्लो (Job Order → Dispatch Traceability)"
                        } else {
                            "Connected Garment Workflow Pipeline (Job Order → Dispatch)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stages = listOf(
                            "1. CUTTING",
                            "2. MATERIAL ISSUE",
                            "3. KATHA WORK",
                            "4. EMBROIDERY",
                            "5. STITCHING (13 SPI)",
                            "6. DHAGA CUTTING",
                            "7. PRESS & PACK",
                            "8. QC & DISPATCH"
                        )
                        stages.forEach { stage ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Sub-Navigation Toggle + Primary Action Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedSubTab == 0,
                        onClick = { selectedSubTab = 0 },
                        label = {
                            Text(
                                if (isHindi) "चालान व WhatsApp (${filteredChallans.size})"
                                else "Job Challans (${filteredChallans.size})"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_job_challans")
                    )
                    FilterChip(
                        selected = selectedSubTab == 1,
                        onClick = { selectedSubTab = 1 },
                        label = {
                            Text(
                                if (isHindi) "स्टॉक / मटेरियल (${filteredMaterials.size})"
                                else "Inventory (${filteredMaterials.size})"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tab_raw_inventory")
                    )
                }

                if (selectedSubTab == 0 && canCreateChallan) {
                    Button(
                        onClick = { showNewChallanDialog = true },
                        modifier = Modifier.testTag("btn_new_challan"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Issue Challan", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "नया चालान" else "New Challan")
                    }
                } else if (selectedSubTab == 1 && canCreateInventory) {
                    Button(
                        onClick = { showNewMaterialDialog = true },
                        modifier = Modifier.testTag("btn_new_material"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Material", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHindi) "नया मटेरियल" else "Add SKU")
                    }
                }
            }
        }

        // 4. Tab 0: Job Challans with Size-Wise Matrix (S..5XL) & Direct Hindi WhatsApp Dispatch
        if (selectedSubTab == 0) {
            items(filteredChallans, key = { it.id }) { challan ->
                ChallanCard(
                    challan = challan,
                    isHindi = isHindi,
                    canEdit = canEditChallan,
                    canDelete = canDeleteChallan,
                    canWhatsApp = canDispatchWhatsApp,
                    onOpenWhatsAppPreview = { whatsappPreviewChallan = challan },
                    onEdit = { editingChallan = challan },
                    onDelete = { onDeleteChallan(challan) }
                )
            }
        } else {
            // Tab 1: Raw Material Inventory + Material Accountability Card
            item {
                MaterialAccountabilitySummaryCard(challans = challans, isHindi = isHindi)
            }
            items(filteredMaterials, key = { it.id }) { item ->
                MaterialInventoryCard(
                    item = item,
                    canEdit = canEditInventory,
                    canDelete = canDeleteInventory,
                    onEdit = { editingMaterial = item },
                    onDelete = { onDeleteMaterial(item) }
                )
            }
        }
    }

    // --- Hindi WhatsApp Template Preview & Dispatch Dialog (Templates A, B, C, D) ---
    whatsappPreviewChallan?.let { challan ->
        var selectedTemplateOverride by remember(challan) { mutableStateOf(challan.challanType) }
        val previewChallan = challan.copy(challanType = selectedTemplateOverride)
        val formattedHindiMessage = WhatsAppTemplateEngine.formatChallanMessage(previewChallan)

        AlertDialog(
            onDismissRequest = { whatsappPreviewChallan = null },
            title = {
                Column {
                    Text(
                        text = "🟢 C B CREATIONS — WhatsApp Dispatch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To: ${challan.karigarName} (+91 ${challan.karigarPhone})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "STITCHING_ISSUE" to "A. कटिंग/सिलाई इशू",
                            "DHAGA_ISSUE" to "B. धागा कटिंग",
                            "KATHA_ISSUE" to "C. काथा वर्क निर्देश",
                            "FABRICATION_ISSUE" to "D. फैब्रिकेशन इशू"
                        ).forEach { (typeKey, label) ->
                            FilterChip(
                                selected = selectedTemplateOverride == typeKey,
                                onClick = { selectedTemplateOverride = typeKey },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE7FFDB),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formattedHindiMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF111B21),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            WhatsAppTemplateEngine.launchWhatsAppIntent(
                                context = context,
                                rawPhone = challan.karigarPhone,
                                message = formattedHindiMessage,
                                useWhatsAppWeb = false
                            )
                            onShowMessage("Opened Phone WhatsApp for ${challan.karigarName}")
                            whatsappPreviewChallan = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        modifier = Modifier.testTag("btn_confirm_send_whatsapp")
                    ) {
                        Text("Phone WhatsApp", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = {
                            WhatsAppTemplateEngine.launchWhatsAppIntent(
                                context = context,
                                rawPhone = challan.karigarPhone,
                                message = formattedHindiMessage,
                                useWhatsAppWeb = true
                            )
                            onShowMessage("Opened WhatsApp Web for ${challan.karigarName}")
                            whatsappPreviewChallan = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54)),
                        modifier = Modifier.testTag("btn_confirm_send_whatsapp_web")
                    ) {
                        Text("WhatsApp Web", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("CB Creations Challan", formattedHindiMessage))
                            onShowMessage("Copied Hindi WhatsApp Challan to clipboard!")
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy")
                    }
                    TextButton(onClick = { whatsappPreviewChallan = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // --- Create / Edit Challan Dialog (Full Size-Wise S..5XL Matrix & Audit Trail) ---
    if (showNewChallanDialog || editingChallan != null) {
        ChallanEditorDialog(
            existing = editingChallan,
            karigars = karigars,
            onDismiss = {
                showNewChallanDialog = false
                editingChallan = null
            },
            onConfirm = { updated, isEdit, prevSummary ->
                onSaveChallan(updated, isEdit, prevSummary)
                showNewChallanDialog = false
                editingChallan = null
            }
        )
    }

    // --- Create / Edit Material SKU Dialog ---
    if (showNewMaterialDialog || editingMaterial != null) {
        MaterialEditorDialog(
            existing = editingMaterial,
            onDismiss = {
                showNewMaterialDialog = false
                editingMaterial = null
            },
            onConfirm = { updated, isEdit, prevSummary ->
                onSaveMaterial(updated, isEdit, prevSummary)
                showNewMaterialDialog = false
                editingMaterial = null
            }
        )
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0x28FFFFFF),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChallanCard(
    challan: JobChallanEntity,
    isHindi: Boolean,
    canEdit: Boolean,
    canDelete: Boolean,
    canWhatsApp: Boolean,
    onOpenWhatsAppPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val qcColor = when (challan.qcStatus) {
        "PASSED" -> EmeraldQcPass
        "REWORK_REQUIRED" -> AmberReworkWarn
        "QUARANTINED" -> CrimsonDefectAlert
        else -> MaterialTheme.colorScheme.secondary
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("challan_card_${challan.challanNo}"),
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = challan.lotNo,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = challan.itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = qcColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "QC: ${challan.qcStatus}",
                        style = MaterialTheme.typography.labelSmall,
                        color = qcColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "कारीगर (Karigar): ${challan.karigarName} (${challan.karigarPhone}) • दिनांक: ${challan.issueDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Size-wise breakdown S..5XL pill box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Size-Wise Matrix: S:${challan.sizeS} | M:${challan.sizeM} | L:${challan.sizeL} | XL:${challan.sizeXl} | XXL:${challan.sizeXxl} | 3XL:${challan.size3xl} | 4XL:${challan.size4xl} | 5XL:${challan.size5xl}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "कुल पीस: ${challan.totalPcs} Pcs | कटिंग: ${challan.cuttingPcs} | काथा धागा: ${challan.kathaThreadColor} | लेबल: ${challan.labelPcs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val baseVal = (challan.totalPcs * challan.ratePerPc).toInt()
                    Text(
                        text = "Rate: ₹${challan.ratePerPc}/pc (₹$baseVal) • Time: ${challan.actualHoursTaken}h/${challan.targetDeadlineHours}h",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (challan.speedBonusInr > 0) {
                        Text(
                            text = "⚡ Speed Booster Bonus: +₹${challan.speedBonusInr.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldQcPass,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (canWhatsApp) {
                        Button(
                            onClick = onOpenWhatsAppPreview,
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_whatsapp_${challan.challanNo}")
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Send WhatsApp",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHindi) "WhatsApp भेजें" else "SEND WHATSAPP",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    if (canEdit) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("btn_edit_challan_${challan.challanNo}")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Challan")
                        }
                    }
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("btn_delete_challan_${challan.challanNo}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Challan",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialAccountabilitySummaryCard(
    challans: List<JobChallanEntity>,
    isHindi: Boolean
) {
    val totalCuttingIssued = challans.sumOf { it.cuttingPcs }
    val totalLabelsIssued = challans.sumOf { it.labelPcs }
    val totalFinishedAccepted = challans.sumOf {
        if (it.qcStatus == "REWORK_REQUIRED") (it.totalPcs - 6).coerceAtLeast(0) else it.totalPcs
    }
    val totalReworkOrShortage = (totalCuttingIssued - totalFinishedAccepted).coerceAtLeast(0)
    val lossPercent = if (totalCuttingIssued > 0) {
        (totalReworkOrShortage.toDouble() / totalCuttingIssued.toDouble()) * 100.0
    } else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isHindi) "मटेरियल जवाबदेही (Material Accountability - Sec 29)" else "Lot Material Accountability & Reconciliation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Issued Cutting Pcs: $totalCuttingIssued | Labels Issued: $totalLabelsIssued | Accepted Finished: $totalFinishedAccepted | Rework/Hold: $totalReworkOrShortage (${String.format("%.1f", lossPercent)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MaterialInventoryCard(
    item: MaterialItemEntity,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isLowStock = item.currentStock <= item.reorderLevel

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("material_card_${item.skuCode}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.skuCode,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = item.assignedLotNo,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isLowStock) {
                        Surface(
                            color = CrimsonDefectAlert.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Low Stock",
                                    tint = CrimsonDefectAlert,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "LOW STOCK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CrimsonDefectAlert
                                )
                            }
                        }
                    }
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Color/Shade: ${item.colorShade} • Category: ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stock: ${item.currentStock} ${item.unit} (Reorder @ ${item.reorderLevel}) • Rate: ₹${item.unitCostInr}/${item.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isLowStock) CrimsonDefectAlert else EmeraldQcPass,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row {
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit SKU")
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete SKU",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallanEditorDialog(
    existing: JobChallanEntity?,
    karigars: List<KarigarEntity>,
    onDismiss: () -> Unit,
    onConfirm: (JobChallanEntity, Boolean, String) -> Unit
) {
    val isEdit = existing != null
    var challanNo by remember { mutableStateOf(existing?.challanNo ?: "CBC-ST-2026-${(104..999).random()}") }
    var challanType by remember { mutableStateOf(existing?.challanType ?: "STITCHING_ISSUE") }
    var lotNo by remember { mutableStateOf(existing?.lotNo ?: "LOT-CB-404") }
    var itemName by remember { mutableStateOf(existing?.itemName ?: "Jaipur Katha V-Cut Kurti (32\")") }
    var karigarName by remember { mutableStateOf(existing?.karigarName ?: (karigars.firstOrNull()?.name ?: "Rameshwar Lal Tailor")) }
    var karigarPhone by remember { mutableStateOf(existing?.karigarPhone ?: (karigars.firstOrNull()?.phone ?: "9829211122")) }
    var kathaThreadColor by remember { mutableStateOf(existing?.kathaThreadColor ?: "रानी पिंक (Rani Pink #08)") }
    var sizeS by remember { mutableStateOf((existing?.sizeS ?: 20).toString()) }
    var sizeM by remember { mutableStateOf((existing?.sizeM ?: 40).toString()) }
    var sizeL by remember { mutableStateOf((existing?.sizeL ?: 50).toString()) }
    var sizeXl by remember { mutableStateOf((existing?.sizeXl ?: 40).toString()) }
    var sizeXxl by remember { mutableStateOf((existing?.sizeXxl ?: 30).toString()) }
    var size3xl by remember { mutableStateOf((existing?.size3xl ?: 20).toString()) }
    var size4xl by remember { mutableStateOf((existing?.size4xl ?: 10).toString()) }
    var size5xl by remember { mutableStateOf((existing?.size5xl ?: 10).toString()) }
    var ratePerPc by remember { mutableStateOf((existing?.ratePerPc ?: 68.0).toString()) }
    var targetHours by remember { mutableStateOf((existing?.targetDeadlineHours ?: 48).toString()) }
    var actualHours by remember { mutableStateOf((existing?.actualHoursTaken ?: 40).toString()) }

    val computedTotal =
        (sizeS.toIntOrNull() ?: 0) +
            (sizeM.toIntOrNull() ?: 0) +
            (sizeL.toIntOrNull() ?: 0) +
            (sizeXl.toIntOrNull() ?: 0) +
            (sizeXxl.toIntOrNull() ?: 0) +
            (size3xl.toIntOrNull() ?: 0) +
            (size4xl.toIntOrNull() ?: 0) +
            (size5xl.toIntOrNull() ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEdit) "Edit Challan (${existing?.challanNo})" else "Create Size-Wise Job Challan")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "STITCHING_ISSUE" to "Stitching",
                        "KATHA_ISSUE" to "Katha Work",
                        "DHAGA_ISSUE" to "Dhaga Cut",
                        "FABRICATION_ISSUE" to "Fabrication"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = challanType == type,
                            onClick = { challanType = type },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = challanNo,
                        onValueChange = { challanNo = it },
                        label = { Text("Challan No") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lotNo,
                        onValueChange = { lotNo = it },
                        label = { Text("Lot No") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item / Style Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = karigarName,
                        onValueChange = { karigarName = it },
                        label = { Text("Karigar Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = karigarPhone,
                        onValueChange = { karigarPhone = it },
                        label = { Text("WhatsApp Phone") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = kathaThreadColor,
                    onValueChange = { kathaThreadColor = it },
                    label = { Text("Katha Thread Color (काथा धागा कलर)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Size-Wise Pieces (Auto Total = $computedTotal Pcs)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = sizeS, onValueChange = { sizeS = it }, label = { Text("S") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = sizeM, onValueChange = { sizeM = it }, label = { Text("M") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = sizeL, onValueChange = { sizeL = it }, label = { Text("L") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = sizeXl, onValueChange = { sizeXl = it }, label = { Text("XL") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = sizeXxl, onValueChange = { sizeXxl = it }, label = { Text("XXL") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = size3xl, onValueChange = { size3xl = it }, label = { Text("3XL") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = size4xl, onValueChange = { size4xl = it }, label = { Text("4XL") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = size5xl, onValueChange = { size5xl = it }, label = { Text("5XL") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ratePerPc,
                        onValueChange = { ratePerPc = it },
                        label = { Text("Rate ₹/Pc") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = targetHours,
                        onValueChange = { targetHours = it },
                        label = { Text("Target Hrs") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = actualHours,
                        onValueChange = { actualHours = it },
                        label = { Text("Actual Hrs") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prev = existing?.let {
                        "Pcs=${it.totalPcs}, Thread=${it.kathaThreadColor}, Rate=₹${it.ratePerPc}"
                    } ?: "None"
                    val entity = JobChallanEntity(
                        id = existing?.id ?: 0,
                        challanNo = challanNo.trim(),
                        challanType = challanType,
                        lotNo = lotNo.trim(),
                        itemName = itemName.trim(),
                        karigarId = existing?.karigarId ?: 1,
                        karigarName = karigarName.trim(),
                        karigarPhone = karigarPhone.trim(),
                        issueDate = existing?.issueDate ?: "30/09/2026",
                        targetDeadlineHours = targetHours.toIntOrNull() ?: 48,
                        actualHoursTaken = actualHours.toIntOrNull() ?: 40,
                        sizeS = sizeS.toIntOrNull() ?: 0,
                        sizeM = sizeM.toIntOrNull() ?: 0,
                        sizeL = sizeL.toIntOrNull() ?: 0,
                        sizeXl = sizeXl.toIntOrNull() ?: 0,
                        sizeXxl = sizeXxl.toIntOrNull() ?: 0,
                        size3xl = size3xl.toIntOrNull() ?: 0,
                        size4xl = size4xl.toIntOrNull() ?: 0,
                        size5xl = size5xl.toIntOrNull() ?: 0,
                        totalPcs = computedTotal.coerceAtLeast(1),
                        cuttingPcs = computedTotal.coerceAtLeast(1),
                        kathaThreadColor = kathaThreadColor.trim(),
                        labelPcs = computedTotal.coerceAtLeast(1),
                        ratePerPc = ratePerPc.toDoubleOrNull() ?: 68.0,
                        qcStatus = existing?.qcStatus ?: "PASSED"
                    )
                    onConfirm(entity, isEdit, prev)
                }
            ) {
                Text(if (isEdit) "Update & Log Audit" else "Create Challan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun MaterialEditorDialog(
    existing: MaterialItemEntity?,
    onDismiss: () -> Unit,
    onConfirm: (MaterialItemEntity, Boolean, String) -> Unit
) {
    val isEdit = existing != null
    var sku by remember { mutableStateOf(existing?.skuCode ?: "FAB-COT-${(100..999).random()}") }
    var name by remember { mutableStateOf(existing?.name ?: "Jaipur Handblock Cotton Roll") }
    var category by remember { mutableStateOf(existing?.category ?: "FABRIC") }
    var shade by remember { mutableStateOf(existing?.colorShade ?: "Sanganeri Indigo") }
    var unit by remember { mutableStateOf(existing?.unit ?: "Meters") }
    var stock by remember { mutableStateOf((existing?.currentStock ?: 500.0).toString()) }
    var reorder by remember { mutableStateOf((existing?.reorderLevel ?: 120.0).toString()) }
    var cost by remember { mutableStateOf((existing?.unitCostInr ?: 92.0).toString()) }
    var lotNo by remember { mutableStateOf(existing?.assignedLotNo ?: "LOT-CB-404") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Material SKU" else "Add Raw Material / Trim") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU Code") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lotNo, onValueChange = { lotNo = it }, label = { Text("Assigned Lot") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Material Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = shade, onValueChange = { shade = it }, label = { Text("Color/Shade") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = reorder, onValueChange = { reorder = it }, label = { Text("Reorder Lvl") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Rate ₹") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prev = existing?.let { "Stock=${it.currentStock}, Rate=₹${it.unitCostInr}" } ?: "None"
                    onConfirm(
                        MaterialItemEntity(
                            id = existing?.id ?: 0,
                            skuCode = sku.trim(),
                            name = name.trim(),
                            category = category.trim(),
                            colorShade = shade.trim(),
                            unit = unit.trim(),
                            currentStock = stock.toDoubleOrNull() ?: 0.0,
                            reorderLevel = reorder.toDoubleOrNull() ?: 50.0,
                            unitCostInr = cost.toDoubleOrNull() ?: 0.0,
                            assignedLotNo = lotNo.trim()
                        ),
                        isEdit,
                        prev
                    )
                }
            ) {
                Text(if (isEdit) "Update Material" else "Save Material")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
