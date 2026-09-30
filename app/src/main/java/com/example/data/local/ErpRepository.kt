package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ErpRepository(private val dao: ErpDao) {
    val materials: Flow<List<MaterialItemEntity>> = dao.getAllMaterials()
    val karigars: Flow<List<KarigarEntity>> = dao.getAllKarigars()
    val challans: Flow<List<JobChallanEntity>> = dao.getAllChallans()
    val qcInspections: Flow<List<QcInspectionEntity>> = dao.getAllQcInspections()
    val vouchers: Flow<List<AccountingVoucherEntity>> = dao.getAllVouchers()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getRecentAuditLogs()
    val rbacPermissions: Flow<List<RbacPermissionEntity>> = dao.getAllRbacPermissions()
    val manualTimeLogs: Flow<List<ManualTimeLogEntity>> = dao.getAllManualTimeLogs()

    /**
     * Initializes ONLY the RBAC Role Permissions Matrix (including the strict
     * "Viewer (Read-Only)" user who cannot create, edit, or delete anything).
     * Does NOT seed any test/demo transactions so the ERP starts 100% clean.
     */
    suspend fun ensureInitialDataSeeded() {
        if (dao.countRbacPermissions() > 0) return
        seedDefaultRbacMatrix()
    }

    private suspend fun seedDefaultRbacMatrix() {
        val roles = listOf(
            "Viewer (Read-Only)",
            "Administrator",
            "Production Manager",
            "Inventory Clerk",
            "Karigar (Worker)",
            "Brand Representative"
        )
        val modules = listOf(
            "INVENTORY",
            "JOB_WORK",
            "PRODUCTION_QC",
            "FINANCE",
            "AI_BLUEPRINT"
        )
        for (role in roles) {
            for (module in modules) {
                val perm = when (role) {
                    "Viewer (Read-Only)" -> RbacPermissionEntity(
                        roleName = role,
                        moduleName = module,
                        canView = true,
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canDispatchWhatsApp = false
                    )
                    "Administrator" -> RbacPermissionEntity(
                        roleName = role,
                        moduleName = module,
                        canView = true,
                        canCreate = true,
                        canEdit = true,
                        canDelete = true,
                        canDispatchWhatsApp = true
                    )
                    "Production Manager" -> RbacPermissionEntity(
                        roleName = role,
                        moduleName = module,
                        canView = true,
                        canCreate = module != "FINANCE",
                        canEdit = module != "FINANCE",
                        canDelete = module == "JOB_WORK" || module == "PRODUCTION_QC",
                        canDispatchWhatsApp = true
                    )
                    "Inventory Clerk" -> RbacPermissionEntity(
                        roleName = role,
                        moduleName = module,
                        canView = module != "FINANCE",
                        canCreate = module == "INVENTORY" || module == "JOB_WORK",
                        canEdit = module == "INVENTORY",
                        canDelete = false,
                        canDispatchWhatsApp = module == "INVENTORY" || module == "JOB_WORK"
                    )
                    "Karigar (Worker)" -> RbacPermissionEntity(
                        roleName = role,
                        moduleName = module,
                        canView = module == "JOB_WORK" || module == "PRODUCTION_QC",
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canDispatchWhatsApp = false
                    )
                    else -> RbacPermissionEntity( // Brand Representative
                        roleName = role,
                        moduleName = module,
                        canView = true,
                        canCreate = false,
                        canEdit = false,
                        canDelete = false,
                        canDispatchWhatsApp = true
                    )
                }
                dao.insertRbacPermission(perm)
            }
        }
    }

    /**
     * Completely removes and clears all test/transactional data across all tables.
     */
    suspend fun clearAllTransactionalData(activeRole: String) {
        dao.clearAllMaterials()
        dao.clearAllKarigars()
        dao.clearAllChallans()
        dao.clearAllQcInspections()
        dao.clearAllVouchers()
        dao.clearAllManualTimeLogs()
        dao.clearAllAuditLogs()
        dao.clearRbacPermissions()
        seedDefaultRbacMatrix()
        dao.insertAuditLog(
            AuditLogEntity(
                module = "SYSTEM",
                recordRef = "ALL_TABLES",
                actionType = "DATA_CLEARED",
                previousValue = "All records",
                newValue = "Cleared all test/transactional data (0 records)",
                performedByRole = activeRole
            )
        )
    }

    suspend fun saveMaterialWithAudit(
        item: MaterialItemEntity,
        isEdit: Boolean,
        previousSummary: String,
        activeRole: String
    ) {
        if (isEdit) {
            dao.updateMaterial(item)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "INVENTORY",
                    recordRef = item.skuCode,
                    actionType = "EDITED",
                    previousValue = previousSummary,
                    newValue = "${item.name}: Stock=${item.currentStock} ${item.unit}, Rate=₹${item.unitCostInr}",
                    performedByRole = activeRole
                )
            )
        } else {
            dao.insertMaterial(item)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "INVENTORY",
                    recordRef = item.skuCode,
                    actionType = "CREATED",
                    previousValue = "None",
                    newValue = "${item.name} (${item.currentStock} ${item.unit}) for ${item.assignedLotNo}",
                    performedByRole = activeRole
                )
            )
        }
    }

    suspend fun deleteMaterialWithAudit(item: MaterialItemEntity, activeRole: String) {
        dao.deleteMaterialById(item.id)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "INVENTORY",
                recordRef = item.skuCode,
                actionType = "DELETED",
                previousValue = "${item.name} (${item.currentStock} ${item.unit})",
                newValue = "Deleted",
                performedByRole = activeRole
            )
        )
    }

    suspend fun saveChallanWithAudit(
        challan: JobChallanEntity,
        isEdit: Boolean,
        previousSummary: String,
        activeRole: String
    ) {
        if (isEdit) {
            dao.updateChallan(challan)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "JOB_WORK",
                    recordRef = challan.challanNo,
                    actionType = "EDITED",
                    previousValue = previousSummary,
                    newValue = "Pcs=${challan.totalPcs}, Thread=${challan.kathaThreadColor}, Rate=₹${challan.ratePerPc}, QC=${challan.qcStatus}",
                    performedByRole = activeRole
                )
            )
        } else {
            dao.insertChallan(challan)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "JOB_WORK",
                    recordRef = challan.challanNo,
                    actionType = "CREATED",
                    previousValue = "None",
                    newValue = "${challan.challanType} to ${challan.karigarName} (${challan.totalPcs} Pcs, Lot ${challan.lotNo})",
                    performedByRole = activeRole
                )
            )
        }
    }

    suspend fun deleteChallanWithAudit(challan: JobChallanEntity, activeRole: String) {
        dao.deleteChallanById(challan.id)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "JOB_WORK",
                recordRef = challan.challanNo,
                actionType = "DELETED",
                previousValue = "${challan.challanNo} (${challan.totalPcs} Pcs - ${challan.karigarName})",
                newValue = "Deleted",
                performedByRole = activeRole
            )
        )
    }

    suspend fun updateKarigarWithAudit(
        karigar: KarigarEntity,
        previousSummary: String,
        actionLabel: String,
        activeRole: String
    ) {
        dao.updateKarigar(karigar)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "KARIGAR_RATE",
                recordRef = karigar.name,
                actionType = actionLabel,
                previousValue = previousSummary,
                newValue = "Role=${karigar.role}, Rate=₹${karigar.rateAmount}/${karigar.rateUnit}, Balance=₹${karigar.runningBalanceInr}",
                performedByRole = activeRole
            )
        )
    }

    suspend fun insertKarigarWithAudit(karigar: KarigarEntity, activeRole: String) {
        dao.insertKarigar(karigar)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "KARIGAR_RATE",
                recordRef = karigar.name,
                actionType = "CREATED",
                previousValue = "None",
                newValue = "Role=${karigar.role}, Rate=₹${karigar.rateAmount}/${karigar.rateUnit}",
                performedByRole = activeRole
            )
        )
    }

    suspend fun recordQcInspectionAndSyncWorkflow(
        inspection: QcInspectionEntity,
        nextWorkflowStage: String,
        activeRole: String
    ) {
        dao.insertQcInspection(inspection)
        dao.updateChallanQcAndStageByLot(
            lotNo = inspection.lotNo,
            qcStatus = inspection.qcDecision,
            stage = nextWorkflowStage
        )
        dao.insertAuditLog(
            AuditLogEntity(
                module = "QC_ENGINE",
                recordRef = "${inspection.lotNo} (${inspection.stage})",
                actionType = "QC_STATUS_SYNC",
                previousValue = "Inspected=${inspection.inspectedPcs}, Defects=${inspection.defectivePcs}",
                newValue = "Decision=${inspection.qcDecision}, Stage->$nextWorkflowStage, Action=${inspection.correctiveAction}",
                performedByRole = activeRole
            )
        )
    }

    suspend fun deleteQcInspectionWithAudit(inspection: QcInspectionEntity, activeRole: String) {
        dao.deleteQcInspectionById(inspection.id)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "QC_ENGINE",
                recordRef = "${inspection.lotNo} (${inspection.stage})",
                actionType = "DELETED",
                previousValue = "Decision=${inspection.qcDecision}, Defects=${inspection.defectivePcs}",
                newValue = "Deleted",
                performedByRole = activeRole
            )
        )
    }

    suspend fun saveVoucherWithAudit(
        voucher: AccountingVoucherEntity,
        isEdit: Boolean,
        previousSummary: String,
        activeRole: String
    ) {
        if (isEdit) {
            dao.updateVoucher(voucher)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "FINANCE",
                    recordRef = voucher.voucherNo,
                    actionType = "EDITED",
                    previousValue = previousSummary,
                    newValue = "${voucher.partyName}: ₹${voucher.amountInr} (${voucher.debitAccount} -> ${voucher.creditAccount})",
                    performedByRole = activeRole
                )
            )
        } else {
            dao.insertVoucher(voucher)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "FINANCE",
                    recordRef = voucher.voucherNo,
                    actionType = "CREATED",
                    previousValue = "None",
                    newValue = "${voucher.voucherType}: ₹${voucher.amountInr} - ${voucher.partyName}",
                    performedByRole = activeRole
                )
            )
        }
    }

    suspend fun updateRbacPermissionWithAudit(
        permission: RbacPermissionEntity,
        previousSummary: String,
        activeRole: String
    ) {
        dao.updateRbacPermission(permission)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "RBAC",
                recordRef = "${permission.roleName} -> ${permission.moduleName}",
                actionType = "RBAC_CHANGED",
                previousValue = previousSummary,
                newValue = "View=${permission.canView}, Create=${permission.canCreate}, Edit=${permission.canEdit}, Del=${permission.canDelete}, WA=${permission.canDispatchWhatsApp}",
                performedByRole = activeRole
            )
        )
    }

    suspend fun saveManualTimeLogWithAudit(
        log: ManualTimeLogEntity,
        isEdit: Boolean,
        previousSummary: String,
        activeRole: String
    ) {
        if (isEdit) {
            dao.updateManualTimeLog(log)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "MANUAL_TIME",
                    recordRef = "${log.personName} (${log.workDate})",
                    actionType = "EDITED",
                    previousValue = previousSummary,
                    newValue = "In=${log.inTimeManual}, Out=${log.outTimeManual}, Hrs=${log.totalHoursManual}, Pcs=${log.piecesCompleted}, Earned=₹${log.totalEarnedInr}",
                    performedByRole = activeRole
                )
            )
        } else {
            dao.insertManualTimeLog(log)
            dao.insertAuditLog(
                AuditLogEntity(
                    module = "MANUAL_TIME",
                    recordRef = "${log.personName} (${log.workDate})",
                    actionType = "CREATED",
                    previousValue = "None",
                    newValue = "${log.personCategory} [${log.roleOrDepartment}]: ${log.inTimeManual}-${log.outTimeManual} (${log.totalHoursManual}h), Pcs=${log.piecesCompleted}, ₹${log.totalEarnedInr}",
                    performedByRole = activeRole
                )
            )
        }
    }

    suspend fun deleteManualTimeLogWithAudit(log: ManualTimeLogEntity, activeRole: String) {
        dao.deleteManualTimeLogById(log.id)
        dao.insertAuditLog(
            AuditLogEntity(
                module = "MANUAL_TIME",
                recordRef = "${log.personName} (${log.workDate})",
                actionType = "DELETED",
                previousValue = "${log.inTimeManual}-${log.outTimeManual} (${log.totalHoursManual}h), ₹${log.totalEarnedInr}",
                newValue = "Deleted",
                performedByRole = activeRole
            )
        )
    }
}
