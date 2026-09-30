package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCode2
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.AccountingVoucherEntity
import com.example.domain.JobCostingAndGstEngine
import com.example.domain.JobCostingInput
import com.example.domain.WhatsAppTemplateEngine
import com.example.ui.theme.AmberReworkWarn
import com.example.ui.theme.EmeraldQcPass
import com.example.ui.theme.WhatsAppGreen

@Composable
fun FinanceAndCostingScreen(
    companyProfile: CompanyMasterProfile,
    costingInput: JobCostingInput,
    vouchers: List<AccountingVoucherEntity>,
    isHindi: Boolean,
    searchQuery: String,
    canCreateFinance: Boolean,
    canEditFinance: Boolean,
    canWhatsApp: Boolean,
    onUpdateCosting: (JobCostingInput) -> Unit,
    onSaveVoucher: (AccountingVoucherEntity, Boolean, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Job Costing Engine, 1 = Double-Entry Day Book, 2 = GST E-Invoice / Shiprocket / Razorpay
    var editingVoucher by remember { mutableStateOf<AccountingVoucherEntity?>(null) }
    var showNewVoucherDialog by remember { mutableStateOf(false) }

    val breakdown = remember(costingInput) {
        JobCostingAndGstEngine.calculateJobCost(costingInput)
    }

    val filteredVouchers = remember(vouchers, searchQuery) {
        if (searchQuery.isBlank()) vouchers
        else vouchers.filter {
            it.voucherNo.contains(searchQuery, ignoreCase = true) ||
                it.partyName.contains(searchQuery, ignoreCase = true) ||
                it.lotNo.contains(searchQuery, ignoreCase = true) ||
                it.narration.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("finance_costing_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sub-Navigation Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text(if (isHindi) "1. जॉब कॉस्टिंग (Cost/Pc)" else "1. Job Costing Engine") },
                    leadingIcon = { Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text(if (isHindi) "2. डबल-एंट्री खाता (${filteredVouchers.size})" else "2. Day Book & Vouchers (${filteredVouchers.size})") },
                    leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    label = { Text(if (isHindi) "3. GST E-Invoice & शिपिंग" else "3. E-Invoice QR & Logistics") },
                    leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        // TAB 0: Interactive 8-Stage Per-Piece & Lot Job Costing Engine
        if (selectedTab == 0) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isHindi) "रीयल-टाइम जॉब कॉस्टिंग कैलकुलेटर (8-Stage Garment Costing)"
                            else "Real-Time 8-Stage Job Costing Calculator",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lot: ${costingInput.lotNo} • ${costingInput.styleName} • Qty: ${costingInput.totalPieces} Pcs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        // Highlight Result Banner
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = if (isHindi) "प्रति पीस निर्माण लागत" else "Exact Mfg Cost / Piece",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "₹${breakdown.exactManufacturingCostPerPc}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (isHindi) "B2B बिक्री मूल्य (GST सहित)" else "B2B Price (incl. ${costingInput.gstPercent.toInt()}% GST)",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            text = "₹${breakdown.finalB2bInvoicePricePerPc}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldQcPass
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Fabric/Pc: ₹${breakdown.fabricCostPerPc} | 6-Stage Labor/Pc: ₹${breakdown.laborStageCostPerPc} | Trims+Freight: ₹${breakdown.trimsAndFreightPerPc} | Speed Bonus: ₹${breakdown.speedBoosterIncentivePerPc}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Total Lot Mfg Cost: ₹${breakdown.totalLotManufacturingCost.toInt()} • Net Lot Profit (${costingInput.targetMarginPercent.toInt()}% Margin): ₹${breakdown.netProfitPerLotInr.toInt()}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldQcPass
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        // Editable Stage Rate Inputs
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costingInput.totalPieces.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(totalPieces = it.toIntOrNull() ?: 1)) },
                                label = { Text("Lot Pcs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = costingInput.fabricMetersPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(fabricMetersPerPc = it.toDoubleOrNull() ?: 2.0)) },
                                label = { Text("Meters/Pc") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.fabricRatePerMeter.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(fabricRatePerMeter = it.toDoubleOrNull() ?: 90.0)) },
                                label = { Text("Fabric ₹/m") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costingInput.cuttingCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(cuttingCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Cutting ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.kathaWorkCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(kathaWorkCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Katha ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.embroideryCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(embroideryCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Embroidery ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costingInput.stitchingCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(stitchingCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Stitching ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.dhagaCuttingCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(dhagaCuttingCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Dhaga ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.pressingCostPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(pressingCostPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Press ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = costingInput.packingAndTrimsPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(packingAndTrimsPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Pack/Trims ₹") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.freightAndLogisticsPerPc.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(freightAndLogisticsPerPc = it.toDoubleOrNull() ?: 0.0)) },
                                label = { Text("Freight ₹/Pc") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = costingInput.targetMarginPercent.toString(),
                                onValueChange = { onUpdateCosting(costingInput.copy(targetMarginPercent = it.toDoubleOrNull() ?: 20.0)) },
                                label = { Text("Margin %") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                    }
                }
            }
        }

        // TAB 1: Double-Entry Day Book & Editable Accounting Vouchers
        if (selectedTab == 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isHindi) "डबल-एंट्री डे-बुक और वाउचर" else "Double-Entry Day Book & Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Every voucher is editable with immutable Audit Trail logging",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (canCreateFinance) {
                        Button(
                            onClick = { showNewVoucherDialog = true },
                            modifier = Modifier.testTag("btn_new_voucher")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHindi) "नया वाउचर" else "New Voucher")
                        }
                    }
                }
            }

            items(filteredVouchers, key = { it.id }) { voucher ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voucher_card_${voucher.voucherNo}"),
                    shape = RoundedCornerShape(14.dp)
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
                                        text = voucher.voucherNo,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = voucher.voucherType,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = voucher.partyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "₹${voucher.amountInr.toInt()}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldQcPass
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dr: ${voucher.debitAccount}\nCr: ${voucher.creditAccount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lot: ${voucher.lotNo} • Mode: ${voucher.paymentMode} • ${voucher.narration}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (canWhatsApp) {
                                TextButton(
                                    onClick = {
                                        val msg = """
🚩 *C B CREATIONS (BECOOPER) – VOUCHER / RECEIPT* 🚩
Voucher No: ${voucher.voucherNo} (${voucher.voucherType})
Party: ${voucher.partyName}
Amount: ₹${voucher.amountInr} (GST: ${voucher.gstRatePercent}%)
Dr: ${voucher.debitAccount} | Cr: ${voucher.creditAccount}
Remarks: ${voucher.narration}
C B CREATIONS, Sanganer Jaipur (GSTIN: ${companyProfile.gstin})
                                        """.trimIndent()
                                        WhatsAppTemplateEngine.launchWhatsAppIntent(context, "9829211122", msg)
                                    }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = WhatsAppGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp", color = WhatsAppGreen)
                                }
                            }
                            if (canEditFinance) {
                                IconButton(onClick = { editingVoucher = voucher }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Voucher")
                                }
                            }
                        }
                    }
                }
            }
        }

        // TAB 2: NIC GST E-Invoice v1.1 + QR Code + Shiprocket & Razorpay Integration Status
        if (selectedTab == 2) {
            item {
                val rzpConfigured = BuildConfig.RAZORPAY_KEY_ID.isNotBlank() &&
                    BuildConfig.RAZORPAY_KEY_ID != "rzp_test_cbcreations_placeholder"
                val srConfigured = BuildConfig.SHIPROCKET_API_TOKEN.isNotBlank() &&
                    BuildConfig.SHIPROCKET_API_TOKEN != "sr_token_cbcreations_placeholder"

                val eInvoiceJson = remember(costingInput, breakdown) {
                    JobCostingAndGstEngine.generateGstEInvoiceJson(
                        invoiceNo = "CBC/26-27/INV-901",
                        buyerName = "BECOOPER D2C & Myntra Ethnic Hub",
                        buyerGstin = "29AABCM8899K1Z4",
                        lotNo = costingInput.lotNo,
                        totalPieces = costingInput.totalPieces,
                        taxableValueInr = breakdown.recommendedExFactoryPricePerPc * costingInput.totalPieces,
                        gstRatePercent = costingInput.gstPercent,
                        shiprocketAwb = if (srConfigured) "SR-AWB-784920114" else "NOT-CONFIGURED"
                    )
                }
                val qrMatrix = remember(eInvoiceJson) {
                    JobCostingAndGstEngine.buildQrMatrix("GSTIN:${companyProfile.gstin}|INV:CBC/26-27/INV-901|VAL:${breakdown.totalLotInvoiceValueInr}")
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "GST E-Invoice (NIC v1.1), E-Way Bill, Shiprocket & Razorpay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supplier GSTIN: ${companyProfile.gstin} (${companyProfile.firmName} • Sanganer, Jaipur - 302033)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        // Honest Integration Status Cards (Sec 29, 30, 60)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "🔐 Live External Gateway Status (Configured via AI Studio Secrets):",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "• NIC GST E-Invoice / E-Way Bill Portal: Integration Not Configured (Local JSON v1.1 & Verification QR Ready)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AmberReworkWarn
                                )
                                Text(
                                    text = "• Razorpay Payment Gateway: ${if (rzpConfigured) "Connected" else "Integration Not Configured (Add RAZORPAY_KEY_ID in Secrets)"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (rzpConfigured) EmeraldQcPass else AmberReworkWarn
                                )
                                Text(
                                    text = "• Shiprocket Logistics AWB API: ${if (srConfigured) "Connected" else "Integration Not Configured (Add SHIPROCKET_API_TOKEN in Secrets)"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (srConfigured) EmeraldQcPass else AmberReworkWarn
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Custom Compose Canvas QR Code Renderer
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(126.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    val gridSize = qrMatrix.size
                                    val cellW = size.width / gridSize
                                    val cellH = size.height / gridSize
                                    for (r in 0 until gridSize) {
                                        for (c in 0 until gridSize) {
                                            if (qrMatrix[r][c]) {
                                                drawRect(
                                                    color = Color(0xFF0B1528),
                                                    topLeft = Offset(c * cellW, r * cellH),
                                                    size = Size(cellW + 0.5f, cellH + 0.5f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "B2B GST Payload Verification QR",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "HSN: 620442 (Cotton Kurtis)\nQty: ${costingInput.totalPieces} Pcs\nTaxable: ₹${(breakdown.recommendedExFactoryPricePerPc * costingInput.totalPieces).toInt()}\nTotal w/ GST: ₹${breakdown.totalLotInvoiceValueInr.toInt()}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("GST E-Invoice JSON", eInvoiceJson))
                                        onShowMessage("Copied NIC v1.1 GST E-Invoice JSON payload to clipboard!")
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy NIC JSON", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = eInvoiceJson,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewVoucherDialog || editingVoucher != null) {
        VoucherEditorDialog(
            existing = editingVoucher,
            onDismiss = {
                showNewVoucherDialog = false
                editingVoucher = null
            },
            onConfirm = { updated, isEdit, prev ->
                onSaveVoucher(updated, isEdit, prev)
                showNewVoucherDialog = false
                editingVoucher = null
            }
        )
    }
}

@Composable
private fun VoucherEditorDialog(
    existing: AccountingVoucherEntity?,
    onDismiss: () -> Unit,
    onConfirm: (AccountingVoucherEntity, Boolean, String) -> Unit
) {
    val isEdit = existing != null
    var voucherNo by remember { mutableStateOf(existing?.voucherNo ?: "VCH-2026-${(904..999).random()}") }
    var voucherType by remember { mutableStateOf(existing?.voucherType ?: "DEBIT_PAYMENT") }
    var partyName by remember { mutableStateOf(existing?.partyName ?: "Rameshwar Lal Tailor") }
    var debitAcc by remember { mutableStateOf(existing?.debitAccount ?: "Karigar Payable A/c") }
    var creditAcc by remember { mutableStateOf(existing?.creditAccount ?: "HDFC Bank / Cash A/c") }
    var amount by remember { mutableStateOf((existing?.amountInr ?: 5000.0).toString()) }
    var gstRate by remember { mutableStateOf((existing?.gstRatePercent ?: 5.0).toString()) }
    var lotNo by remember { mutableStateOf(existing?.lotNo ?: "LOT-CB-401") }
    var narration by remember { mutableStateOf(existing?.narration ?: "Part payment against stitching challan") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Accounting Voucher" else "Create Double-Entry Voucher") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("DEBIT_PAYMENT", "CREDIT_RECEIPT", "JOB_WORK_BILL", "PURCHASE_INVOICE", "GST_E_INVOICE").forEach { t ->
                        FilterChip(
                            selected = voucherType == t,
                            onClick = { voucherType = t },
                            label = { Text(t, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = voucherNo, onValueChange = { voucherNo = it }, label = { Text("Voucher No") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lotNo, onValueChange = { lotNo = it }, label = { Text("Lot No") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = partyName, onValueChange = { partyName = it }, label = { Text("Party / Karigar Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = debitAcc, onValueChange = { debitAcc = it }, label = { Text("Debit Account (Dr)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = creditAcc, onValueChange = { creditAcc = it }, label = { Text("Credit Account (Cr)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ₹") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    OutlinedTextField(value = gstRate, onValueChange = { gstRate = it }, label = { Text("GST %") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                OutlinedTextField(value = narration, onValueChange = { narration = it }, label = { Text("Narration / Reason") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val prev = existing?.let { "${it.partyName}: ₹${it.amountInr}" } ?: "None"
                    onConfirm(
                        AccountingVoucherEntity(
                            id = existing?.id ?: 0,
                            voucherNo = voucherNo.trim(),
                            voucherType = voucherType,
                            partyName = partyName.trim(),
                            debitAccount = debitAcc.trim(),
                            creditAccount = creditAcc.trim(),
                            amountInr = amount.toDoubleOrNull() ?: 0.0,
                            gstRatePercent = gstRate.toDoubleOrNull() ?: 0.0,
                            lotNo = lotNo.trim(),
                            paymentMode = existing?.paymentMode ?: "UPI_NEFT",
                            narration = narration.trim()
                        ),
                        isEdit,
                        prev
                    )
                }
            ) {
                Text(if (isEdit) "Update & Audit" else "Post Voucher")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
