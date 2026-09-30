package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AccountingVoucherEntity
import com.example.data.local.AuditLogEntity
import com.example.data.local.ErpDatabase
import com.example.data.local.ErpRepository
import com.example.data.local.JobChallanEntity
import com.example.data.local.KarigarEntity
import com.example.data.local.ManualTimeLogEntity
import com.example.data.local.MaterialItemEntity
import com.example.data.local.QcInspectionEntity
import com.example.data.local.RbacPermissionEntity
import com.example.domain.GeminiAiService
import com.example.domain.JobCostingAndGstEngine
import com.example.domain.JobCostingInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompanyMasterProfile(
    val firmName: String = "C B CREATIONS",
    val brandName: String = "BECOOPER (www.becooper.in • erp.becooper.in)",
    val addressLine: String = "61, BUS STAND K PASS, SHYOPUR MARG, WARD NO. 32, SANGANER, JAIPUR, RAJASTHAN - 302033, INDIA",
    val gstin: String = "08CCYPJ9736Q1Z6",
    val stateName: String = "Rajasthan",
    val stateCode: String = "08",
    val pinCode: String = "302033",
    val contactPerson: String = "Vijender Ji",
    val phones: String = "+91 98292 11122, +91 95099 01475",
    val financialYear: String = "2026-27"
)

class ErpViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ErpRepository

    val materials: StateFlow<List<MaterialItemEntity>>
    val karigars: StateFlow<List<KarigarEntity>>
    val challans: StateFlow<List<JobChallanEntity>>
    val qcInspections: StateFlow<List<QcInspectionEntity>>
    val vouchers: StateFlow<List<AccountingVoucherEntity>>
    val auditLogs: StateFlow<List<AuditLogEntity>>
    val rbacPermissions: StateFlow<List<RbacPermissionEntity>>
    val manualTimeLogs: StateFlow<List<ManualTimeLogEntity>>

    private val _companyProfile = MutableStateFlow(CompanyMasterProfile())
    val companyProfile: StateFlow<CompanyMasterProfile> = _companyProfile.asStateFlow()

    private val _activeRole = MutableStateFlow("Administrator")
    val activeRole: StateFlow<String> = _activeRole.asStateFlow()

    private val _isHindiMode = MutableStateFlow(false)
    val isHindiMode: StateFlow<Boolean> = _isHindiMode.asStateFlow()

    private val _useWhatsAppWebMode = MutableStateFlow(false)
    val useWhatsAppWebMode: StateFlow<Boolean> = _useWhatsAppWebMode.asStateFlow()

    private val _globalSearchQuery = MutableStateFlow("")
    val globalSearchQuery: StateFlow<String> = _globalSearchQuery.asStateFlow()

    private val _costingInput = MutableStateFlow(JobCostingInput())
    val costingInput: StateFlow<JobCostingInput> = _costingInput.asStateFlow()

    private val _aiResponseText = MutableStateFlow(
        "नमस्ते Vijender Ji! C B CREATIONS AI Assistant तैयार है। नीचे दिए गए किसी भी प्रश्न पर टैप करें या चालान फोटो स्कैन करें — सभी उत्तर लाइव डेटाबेस (Room DB) से दिए जाते हैं।"
    )
    val aiResponseText: StateFlow<String> = _aiResponseText.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    init {
        val dao = ErpDatabase.getDatabase(application).erpDao()
        repository = ErpRepository(dao)

        materials = repository.materials.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        karigars = repository.karigars.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        challans = repository.challans.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        qcInspections = repository.qcInspections.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        vouchers = repository.vouchers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        auditLogs = repository.auditLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        rbacPermissions = repository.rbacPermissions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        manualTimeLogs = repository.manualTimeLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.ensureInitialDataSeeded()
        }
    }

    fun setActiveRole(role: String) {
        _activeRole.value = role
        showBanner("Active RBAC Role switched to: $role")
    }

    fun toggleHindiMode() {
        _isHindiMode.value = !_isHindiMode.value
    }

    fun setWhatsAppWebMode(useWeb: Boolean) {
        _useWhatsAppWebMode.value = useWeb
        showBanner(
            if (useWeb) "WhatsApp Mode: WhatsApp Web (web.whatsapp.com)"
            else "WhatsApp Mode: Phone WhatsApp App"
        )
    }

    fun clearAllTestData() {
        if (isReadOnlyViewer()) {
            showBanner("🔒 Read-Only User: 'Viewer (Read-Only)' कुछ भी एडिट या क्लियर नहीं कर सकता।")
            return
        }
        viewModelScope.launch {
            repository.clearAllTransactionalData(_activeRole.value)
            showBanner("All test/transactional data cleared from database (0 records).")
        }
    }

    fun isReadOnlyViewer(): Boolean = _activeRole.value == "Viewer (Read-Only)"

    fun updateGlobalSearch(query: String) {
        _globalSearchQuery.value = query
    }

    fun clearBanner() {
        _statusBannerMessage.value = null
    }

    fun showBanner(message: String) {
        _statusBannerMessage.value = message
    }

    fun updateCompanyProfile(updated: CompanyMasterProfile) {
        if (isReadOnlyViewer()) {
            showBanner("🔒 Read-Only User: 'Viewer (Read-Only)' कुछ भी एडिट नहीं कर सकता।")
            return
        }
        _companyProfile.value = updated
        showBanner("Company Master updated for ${updated.firmName} (GSTIN: ${updated.gstin})")
    }

    /**
     * Checks RBAC permission for the currently active role & module.
     * Action can be: "VIEW", "CREATE", "EDIT", "DELETE", "WHATSAPP"
     */
    fun hasPermission(moduleName: String, action: String): Boolean {
        val role = _activeRole.value
        if (role == "Viewer (Read-Only)") {
            return action == "VIEW"
        }
        val perm = rbacPermissions.value.firstOrNull {
            it.roleName == role && it.moduleName == moduleName
        } ?: return role == "Administrator"

        return when (action) {
            "VIEW" -> perm.canView
            "CREATE" -> perm.canCreate
            "EDIT" -> perm.canEdit
            "DELETE" -> perm.canDelete
            "WHATSAPP" -> perm.canDispatchWhatsApp
            else -> false
        }
    }

    // --- Material & Inventory Operations ---
    fun saveMaterial(item: MaterialItemEntity, isEdit: Boolean, previousSummary: String = "") {
        if (!hasPermission("INVENTORY", if (isEdit) "EDIT" else "CREATE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot ${if (isEdit) "edit" else "create"} Inventory items.")
            return
        }
        viewModelScope.launch {
            repository.saveMaterialWithAudit(item, isEdit, previousSummary, _activeRole.value)
            showBanner("Saved material ${item.skuCode} (${item.name}) & logged in Audit Trail.")
        }
    }

    fun deleteMaterial(item: MaterialItemEntity) {
        if (!hasPermission("INVENTORY", "DELETE")) {
            showBanner("RBAC Guard: ${_activeRole.value} does not have DELETE permission on Inventory.")
            return
        }
        viewModelScope.launch {
            repository.deleteMaterialWithAudit(item, _activeRole.value)
            showBanner("Deleted ${item.skuCode} with audit entry.")
        }
    }

    // --- Job Challan Operations ---
    fun saveChallan(challan: JobChallanEntity, isEdit: Boolean, previousSummary: String = "") {
        if (!hasPermission("JOB_WORK", if (isEdit) "EDIT" else "CREATE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot ${if (isEdit) "edit" else "create"} Job Challans.")
            return
        }
        viewModelScope.launch {
            val baseAmount = challan.totalPcs * challan.ratePerPc
            val speedEval = JobCostingAndGstEngine.evaluateSpeedBooster(
                baseJobAmountInr = baseAmount,
                targetDeadlineHours = challan.targetDeadlineHours,
                actualHoursTaken = challan.actualHoursTaken,
                defectivePieces = if (challan.qcStatus == "PASSED") 0 else 4,
                qcStatus = challan.qcStatus
            )
            val enrichedChallan = challan.copy(speedBonusInr = speedEval.bonusAmountInr)
            repository.saveChallanWithAudit(enrichedChallan, isEdit, previousSummary, _activeRole.value)
            showBanner("Saved Challan ${challan.challanNo} (${challan.totalPcs} Pcs) | Speed Bonus: ₹${speedEval.bonusAmountInr}")
        }
    }

    fun deleteChallan(challan: JobChallanEntity) {
        if (!hasPermission("JOB_WORK", "DELETE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot delete Job Challans.")
            return
        }
        viewModelScope.launch {
            repository.deleteChallanWithAudit(challan, _activeRole.value)
            showBanner("Deleted Challan ${challan.challanNo} & recorded in Audit Trail.")
        }
    }

    // --- Karigar Rate Master & Punch-In/Out Tracking ---
    fun saveKarigar(karigar: KarigarEntity, isEdit: Boolean, previousSummary: String = "") {
        if (!hasPermission("JOB_WORK", if (isEdit) "EDIT" else "CREATE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot modify Karigar Rate Master.")
            return
        }
        viewModelScope.launch {
            if (isEdit) {
                repository.updateKarigarWithAudit(
                    karigar = karigar,
                    previousSummary = previousSummary,
                    actionLabel = "RATE_UPDATED",
                    activeRole = _activeRole.value
                )
                showBanner("Updated ${karigar.name} rate/ledger & recorded Audit Log.")
            } else {
                repository.insertKarigarWithAudit(karigar, _activeRole.value)
                showBanner("Added Karigar ${karigar.name} (${karigar.role}).")
            }
        }
    }

    fun toggleKarigarPunch(karigar: KarigarEntity, lotNo: String) {
        if (isReadOnlyViewer()) {
            showBanner("🔒 Read-Only User: 'Viewer (Read-Only)' पंच-इन/आउट या कोई भी बदलाव नहीं कर सकता।")
            return
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = if (karigar.isActivePunchIn) {
                val elapsedMinutes = ((now - karigar.punchInTimestamp) / 60000L).coerceAtLeast(1L)
                karigar.copy(
                    isActivePunchIn = false,
                    activeLotNo = "",
                    punchInTimestamp = 0L,
                    completedLots = karigar.completedLots + 1
                ).also {
                    showBanner("${karigar.name} punched out from ${karigar.activeLotNo} (${elapsedMinutes}m session recorded).")
                }
            } else {
                karigar.copy(
                    isActivePunchIn = true,
                    activeLotNo = lotNo.ifBlank { "LOT-CB-401" },
                    punchInTimestamp = now
                ).also {
                    showBanner("${karigar.name} punched IN on ${lotNo.ifBlank { "LOT-CB-401" }}.")
                }
            }
            repository.updateKarigarWithAudit(
                karigar = updated,
                previousSummary = "Punch=${karigar.isActivePunchIn}, Lot=${karigar.activeLotNo}",
                actionLabel = if (updated.isActivePunchIn) "PUNCH_IN" else "PUNCH_OUT",
                activeRole = _activeRole.value
            )
        }
    }

    // --- Manual Time Entry for Karigars & All Other Staff ---
    fun saveManualTimeLog(
        log: ManualTimeLogEntity,
        isEdit: Boolean,
        previousSummary: String = ""
    ) {
        if (isReadOnlyViewer() || !hasPermission("JOB_WORK", if (isEdit) "EDIT" else "CREATE")) {
            showBanner("🔒 RBAC Guard: ${_activeRole.value} मैन्युअल टाइम एंट्री एडिट या सेव नहीं कर सकता।")
            return
        }
        viewModelScope.launch {
            repository.saveManualTimeLogWithAudit(log, isEdit, previousSummary, _activeRole.value)
            showBanner("Saved Manual Time for ${log.personName}: ${log.inTimeManual} - ${log.outTimeManual} (${log.totalHoursManual} Hrs)")
        }
    }

    fun deleteManualTimeLog(log: ManualTimeLogEntity) {
        if (isReadOnlyViewer() || !hasPermission("JOB_WORK", "DELETE")) {
            showBanner("🔒 RBAC Guard: ${_activeRole.value} मैन्युअल टाइम एंट्री डिलीट नहीं कर सकता।")
            return
        }
        viewModelScope.launch {
            repository.deleteManualTimeLogWithAudit(log, _activeRole.value)
            showBanner("Deleted Manual Time entry for ${log.personName}.")
        }
    }

    // --- Quality Control (QC) Inspection & Workflow Integration ---
    fun recordQcInspection(inspection: QcInspectionEntity, nextWorkflowStage: String) {
        if (!hasPermission("PRODUCTION_QC", "CREATE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot record QC inspections.")
            return
        }
        viewModelScope.launch {
            repository.recordQcInspectionAndSyncWorkflow(
                inspection = inspection,
                nextWorkflowStage = nextWorkflowStage,
                activeRole = _activeRole.value
            )
            showBanner("QC ${inspection.qcDecision} recorded for ${inspection.lotNo} -> Stage synced to $nextWorkflowStage.")
        }
    }

    fun deleteQcInspection(inspection: QcInspectionEntity) {
        if (!hasPermission("PRODUCTION_QC", "DELETE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot delete QC records.")
            return
        }
        viewModelScope.launch {
            repository.deleteQcInspectionWithAudit(inspection, _activeRole.value)
            showBanner("Removed QC record for ${inspection.lotNo} with audit trail.")
        }
    }

    // --- Costing & Double-Entry Accounting Vouchers ---
    fun updateCostingInput(updated: JobCostingInput) {
        if (isReadOnlyViewer()) {
            showBanner("🔒 Read-Only User: 'Viewer (Read-Only)' कॉस्टिंग डेटा एडिट नहीं कर सकता।")
            return
        }
        _costingInput.value = updated
    }

    fun saveVoucher(voucher: AccountingVoucherEntity, isEdit: Boolean, previousSummary: String = "") {
        if (!hasPermission("FINANCE", if (isEdit) "EDIT" else "CREATE")) {
            showBanner("RBAC Guard: ${_activeRole.value} cannot ${if (isEdit) "edit" else "create"} Accounting Vouchers.")
            return
        }
        viewModelScope.launch {
            repository.saveVoucherWithAudit(voucher, isEdit, previousSummary, _activeRole.value)
            showBanner("Saved Voucher ${voucher.voucherNo} (₹${voucher.amountInr}) in Double-Entry Day Book.")
        }
    }

    // --- RBAC Matrix Toggle ---
    fun toggleRbacPermission(permission: RbacPermissionEntity, field: String) {
        if (_activeRole.value != "Administrator") {
            showBanner("RBAC Guard: Only Administrator can modify Role Permissions.")
            return
        }
        val prev = "V=${permission.canView},C=${permission.canCreate},E=${permission.canEdit},D=${permission.canDelete},WA=${permission.canDispatchWhatsApp}"
        val updated = when (field) {
            "VIEW" -> permission.copy(canView = !permission.canView)
            "CREATE" -> permission.copy(canCreate = !permission.canCreate)
            "EDIT" -> permission.copy(canEdit = !permission.canEdit)
            "DELETE" -> permission.copy(canDelete = !permission.canDelete)
            "WHATSAPP" -> permission.copy(canDispatchWhatsApp = !permission.canDispatchWhatsApp)
            else -> permission
        }
        viewModelScope.launch {
            repository.updateRbacPermissionWithAudit(updated, prev, _activeRole.value)
            showBanner("Updated RBAC ${permission.roleName} -> ${permission.moduleName} ($field)")
        }
    }

    // --- Grounded Database AI Assistant & OCR Scanner ---
    fun runAiErpAnalysis(question: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val totalPcs = challans.value.sumOf { it.totalPcs }
            val passedLots = challans.value.count { it.qcStatus == "PASSED" }
            val reworkLots = challans.value.filter { it.qcStatus == "REWORK_REQUIRED" }
            val totalKarigarOut = karigars.value.sumOf { it.runningBalanceInr }
            val topKarigarOut = karigars.value.maxByOrNull { it.runningBalanceInr }
            val lowStockList = materials.value.filter { it.currentStock <= it.reorderLevel }
            val costBreakdown = JobCostingAndGstEngine.calculateJobCost(_costingInput.value)

            val snapshot = buildString {
                appendLine("Company: ${_companyProfile.value.firmName}, GSTIN: ${_companyProfile.value.gstin}, Sanganer Jaipur")
                appendLine("Total Challans: ${challans.value.size}, Total Pieces in Production: $totalPcs, QC Passed Lots: $passedLots")
                appendLine("Delayed/Rework Lots: ${reworkLots.joinToString { "${it.lotNo} (${it.karigarName} - ${it.workflowStage})" }}")
                appendLine("Total Karigar Payable Outstanding: ₹$totalKarigarOut (Highest: ${topKarigarOut?.name} ₹${topKarigarOut?.runningBalanceInr})")
                appendLine("Low Stock Alerts: ${lowStockList.joinToString { "${it.skuCode} ${it.name} (${it.currentStock}/${it.reorderLevel} ${it.unit})" }}")
                appendLine("Current Kurti Cost/Piece: ₹${costBreakdown.exactManufacturingCostPerPc}, B2B Selling Price: ₹${costBreakdown.finalB2bInvoicePricePerPc}")
            }

            // First build a 100% database-grounded answer so it never invents numbers:
            val dbGroundedHeader = """
📌 [C B CREATIONS — REAL DATABASE QUERY RESULT]
• आज का कुल उत्पादन (Total Active Pieces): $totalPcs Pcs (${challans.value.size} Job Challans)
• QC Passed Lots: $passedLots | Rework/Delayed: ${reworkLots.size} (${reworkLots.joinToString { it.lotNo }.ifBlank { "None" }})
• कारीगर बकाया (Total Karigar Outstanding): ₹$totalKarigarOut (सर्वोच्च: ${topKarigarOut?.name ?: "-"} — ₹${topKarigarOut?.runningBalanceInr ?: 0.0})
• कम स्टॉक अलर्ट (Low Stock): ${lowStockList.joinToString { "${it.name} (${it.currentStock} ${it.unit})" }.ifBlank { "All sufficient" }}
• प्रति पीस निर्माण लागत (Kurti Cost/Pc): ₹${costBreakdown.exactManufacturingCostPerPc} | Net Lot Profit: ₹${costBreakdown.netProfitPerLotInr}
            """.trimIndent()

            val llmInsight = GeminiAiService.analyzeErpBottlenecksAndSchedule(snapshot, question)
            _aiResponseText.value = "$dbGroundedHeader\n\n$llmInsight"
            _isAiLoading.value = false
        }
    }

    fun runChallanImageOcr(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponseText.value = GeminiAiService.performChallanOcr(bitmap)
            _isAiLoading.value = false
        }
    }
}
