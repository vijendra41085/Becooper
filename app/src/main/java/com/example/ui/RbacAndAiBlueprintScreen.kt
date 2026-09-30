package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AuditLogEntity
import com.example.data.local.RbacPermissionEntity
import com.example.domain.ArchitectureDeliverablesData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RbacAndAiBlueprintScreen(
    companyProfile: CompanyMasterProfile,
    activeRole: String,
    isHindi: Boolean,
    rbacPermissions: List<RbacPermissionEntity>,
    auditLogs: List<AuditLogEntity>,
    aiResponseText: String,
    isAiLoading: Boolean,
    onSwitchRole: (String) -> Unit,
    onToggleRbac: (RbacPermissionEntity, String) -> Unit,
    onUpdateCompanyProfile: (CompanyMasterProfile) -> Unit,
    onAskAi: (String) -> Unit,
    onScanChallanBitmap: (android.graphics.Bitmap) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableStateOf(0) } // 0 = AI Copilot & OCR, 1 = RBAC & Company Master, 2 = Audit Trail, 3 = Full-Stack Code & Schema
    var customAiQuestion by remember { mutableStateOf("") }
    var showEditCompanyDialog by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker for Challan/Invoice OCR
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        onScanChallanBitmap(bitmap)
                        onShowMessage("Scanning Challan image via Gemini Flash Multimodal OCR...")
                    }
                }
            } catch (e: Exception) {
                onShowMessage("Could not read selected image: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rbac_ai_blueprint_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sub-Navigation Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    label = { Text(if (isHindi) "1. AI असिस्टेंट व OCR" else "1. AI Assistant & OCR") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    label = { Text(if (isHindi) "2. RBAC व कंपनी मास्टर" else "2. RBAC & Company Master") },
                    leadingIcon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    label = { Text(if (isHindi) "3. ऑडिट ट्रेल (${auditLogs.size})" else "3. Audit Trail (${auditLogs.size})") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    label = { Text(if (isHindi) "4. आर्किटेक्चर व कोड" else "4. Full-Stack Blueprints") },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // SECTION 0: Database-Grounded AI Assistant + Multimodal Challan OCR
        if (selectedSection == 0) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BECOOPER / C B CREATIONS — AI ERP Copilot",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "100% Grounded in Live Room DB + Gemini Flash API (Challan OCR & Bottleneck AI)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("btn_scan_challan_ocr")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isHindi) "चालान स्कैन (OCR)" else "Scan Challan")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHindi) "त्वरित डेटाबेस प्रश्न (Tap to Query Real ERP Database):"
                            else "One-Tap Database-Grounded Queries (Sec 46 & 72):",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val presetQuestions = listOf(
                            "आज कितने पीस बने और कौन सा लॉट किस स्टेज पर है?",
                            "किस कारीगर का payment pending है और सबसे ज्यादा उत्पादन किसका है?",
                            "Style Kurti का प्रति पीस total manufacturing cost और profit क्या है?",
                            "कौन सा job order delayed / rework में है और किस मटेरियल का कम स्टॉक है?"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            presetQuestions.forEach { q ->
                                OutlinedButton(
                                    onClick = { onAskAi(q) },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "💬 $q",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customAiQuestion,
                                onValueChange = { customAiQuestion = it },
                                label = { Text(if (isHindi) "अपना प्रश्न पूछें..." else "Ask ERP AI about production, cost, karigars...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (customAiQuestion.isNotBlank()) {
                                        onAskAi(customAiQuestion)
                                    }
                                }
                            ) {
                                Text("Ask")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        if (isAiLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("Querying C B CREATIONS database & Gemini Flash...")
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = aiResponseText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.security_warning_prototype),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // SECTION 1: Company Master (C B CREATIONS / BECOOPER) + Granular RBAC Matrix
        if (selectedSection == 1) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Company Master — ${companyProfile.firmName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = companyProfile.brandName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            if (activeRole == "Administrator") {
                                OutlinedButton(onClick = { showEditCompanyDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Master")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Address: ${companyProfile.addressLine}", style = MaterialTheme.typography.bodySmall)
                        Text("GSTIN/UIN: ${companyProfile.gstin} | State: ${companyProfile.stateName} (${companyProfile.stateCode}) | PIN: ${companyProfile.pinCode}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Contact Person: ${companyProfile.contactPerson} | Mobile/WhatsApp: ${companyProfile.phones}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Role-Based Access Control (RBAC) — Active Role: $activeRole",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Switch roles below to test live UI permission enforcement across all ERP screens:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Viewer (Read-Only)",
                                "Administrator",
                                "Production Manager",
                                "Inventory Clerk",
                                "Karigar (Worker)",
                                "Brand Representative"
                            ).forEach { r ->
                                FilterChip(
                                    selected = activeRole == r,
                                    onClick = { onSwitchRole(r) },
                                    label = { Text(r) },
                                    modifier = Modifier.testTag("chip_role_$r")
                                )
                            }
                        }
                    }
                }
            }

            val rolePerms = rbacPermissions.filter { it.roleName == activeRole }
            items(rolePerms, key = { it.id }) { perm ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${perm.roleName} → Module: ${perm.moduleName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RbacToggleItem("View", perm.canView) { onToggleRbac(perm, "VIEW") }
                            RbacToggleItem("Create", perm.canCreate) { onToggleRbac(perm, "CREATE") }
                            RbacToggleItem("Edit", perm.canEdit) { onToggleRbac(perm, "EDIT") }
                            RbacToggleItem("Delete", perm.canDelete) { onToggleRbac(perm, "DELETE") }
                            RbacToggleItem("WhatsApp", perm.canDispatchWhatsApp) { onToggleRbac(perm, "WHATSAPP") }
                        }
                    }
                }
            }
        }

        // SECTION 2: Immutable Audit Trail
        if (selectedSection == 2) {
            items(auditLogs, key = { it.id }) { log ->
                val dateStr = remember(log.timestamp) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${log.module}] ${log.actionType} — ${log.recordRef}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Old Value: ${log.previousValue}", style = MaterialTheme.typography.bodySmall)
                        Text("New Value: ${log.newValue}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Performed By Role: ${log.performedByRole}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        // SECTION 3: Full-Stack Architecture, ERD, Prisma Schema, REST API & TypeScript Code Vault
        if (selectedSection == 3) {
            items(ArchitectureDeliverablesData.deliverables, key = { it.id }) { item ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
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
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(item.title, item.codeOrSpec))
                                    onShowMessage("Copied ${item.title} to clipboard!")
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.codeOrSpec,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditCompanyDialog) {
        var firm by remember { mutableStateOf(companyProfile.firmName) }
        var brand by remember { mutableStateOf(companyProfile.brandName) }
        var addr by remember { mutableStateOf(companyProfile.addressLine) }
        var gstin by remember { mutableStateOf(companyProfile.gstin) }
        var contact by remember { mutableStateOf(companyProfile.contactPerson) }
        var phones by remember { mutableStateOf(companyProfile.phones) }

        AlertDialog(
            onDismissRequest = { showEditCompanyDialog = false },
            title = { Text("Edit Company Master (Future Documents Only)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = firm, onValueChange = { firm = it }, label = { Text("Firm Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand & Domains") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = addr, onValueChange = { addr = it }, label = { Text("Registered Address") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = gstin, onValueChange = { gstin = it }, label = { Text("GSTIN/UIN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contact Person") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phones, onValueChange = { phones = it }, label = { Text("Mobile / WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateCompanyProfile(
                            companyProfile.copy(
                                firmName = firm.trim(),
                                brandName = brand.trim(),
                                addressLine = addr.trim(),
                                gstin = gstin.trim(),
                                contactPerson = contact.trim(),
                                phones = phones.trim()
                            )
                        )
                        showEditCompanyDialog = false
                    }
                ) {
                    Text("Save Master")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditCompanyDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RbacToggleItem(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
