package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "material_items")
data class MaterialItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val skuCode: String,
    val name: String,
    val category: String, // FABRIC, KATHA_THREAD, LABEL, TRIMS_BUTTON_ZIP, CUTTING_LOT
    val colorShade: String,
    val unit: String, // Meters, Cones, Pieces, Rolls
    val currentStock: Double,
    val reorderLevel: Double,
    val unitCostInr: Double,
    val assignedLotNo: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "karigars")
data class KarigarEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String, // Fabric Cutter, Fabricator (Stitching), Katha Worker, Embroidery Worker, Dhaga Cutter, Pressman, Packer
    val phone: String,
    val rateAmount: Double,
    val rateUnit: String, // PER_PIECE, PER_METER, PER_PROCESS
    val runningBalanceInr: Double,
    val completedLots: Int,
    val totalPiecesDone: Int,
    val defectFreePercent: Double,
    val avgSpeedScore: Int, // 0..100
    val earnedIncentiveInr: Double,
    val isActivePunchIn: Boolean = false,
    val activeLotNo: String = "",
    val punchInTimestamp: Long = 0L
)

@Serializable
@Entity(tableName = "job_challans")
data class JobChallanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val challanNo: String,
    val challanType: String, // STITCHING_ISSUE, DHAGA_ISSUE, KATHA_ISSUE, EMBROIDERY_ISSUE, PURCHASE_CHALLAN, DELIVERY_CHALLAN
    val lotNo: String,
    val itemName: String,
    val karigarId: Int,
    val karigarName: String,
    val karigarPhone: String,
    val issueDate: String,
    val targetDeadlineHours: Int,
    val actualHoursTaken: Int,
    val sizeS: Int = 0,
    val sizeM: Int = 0,
    val sizeL: Int = 0,
    val sizeXl: Int = 0,
    val sizeXxl: Int = 0,
    val size3xl: Int = 0,
    val size4xl: Int = 0,
    val size5xl: Int = 0,
    val totalPcs: Int,
    val cuttingPcs: Int,
    val kathaThreadColor: String,
    val labelPcs: Int,
    val ratePerPc: Double,
    val speedBonusInr: Double = 0.0,
    val qcStatus: String = "PENDING", // PENDING, PASSED, REWORK_REQUIRED, QUARANTINED
    val workflowStage: String = "STITCHING", // CUTTING, KATHA_WORK, EMBROIDERY, STITCHING, DHAGA_CUTTING, PRESSING_PACKING, FINAL_QC, DISPATCHED
    val notes: String = ""
)

@Serializable
@Entity(tableName = "qc_inspections")
data class QcInspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lotNo: String,
    val challanNo: String,
    val itemName: String,
    val stage: String, // Post-Cutting, Post-Katha Work, Post-Embroidery, Post-Stitching, Post-Dhaga Cutting, Final Inspection
    val karigarName: String,
    val inspectedPcs: Int,
    val defectivePcs: Int,
    val defectReason: String,
    val severity: String, // MINOR, MAJOR, CRITICAL
    val correctiveAction: String,
    val qcDecision: String, // PASSED, REWORK_REQUIRED, QUARANTINED
    val inspectorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "accounting_vouchers")
data class AccountingVoucherEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val voucherNo: String,
    val voucherType: String, // DEBIT_PAYMENT, CREDIT_RECEIPT, JOB_WORK_BILL, PURCHASE_INVOICE, GST_E_INVOICE
    val partyName: String,
    val debitAccount: String,
    val creditAccount: String,
    val amountInr: Double,
    val gstRatePercent: Double,
    val lotNo: String,
    val paymentMode: String, // RAZORPAY_LINK, UPI_NEFT, CASH, LEDGER_ADJUST
    val shiprocketAwb: String = "",
    val irnHash: String = "",
    val narration: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val module: String, // INVENTORY, JOB_WORK, KARIGAR_RATE, QC_ENGINE, FINANCE, RBAC
    val recordRef: String,
    val actionType: String, // CREATED, EDITED, RATE_UPDATED, QC_STATUS_SYNC, RBAC_CHANGED, DELETED
    val previousValue: String,
    val newValue: String,
    val performedByRole: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "rbac_permissions")
data class RbacPermissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val roleName: String, // Administrator, Production Manager, Inventory Clerk, Karigar (Worker), Brand Representative, Viewer (Read-Only)
    val moduleName: String, // INVENTORY, JOB_WORK, PRODUCTION_QC, FINANCE, AI_BLUEPRINT
    val canView: Boolean,
    val canCreate: Boolean,
    val canEdit: Boolean,
    val canDelete: Boolean,
    val canDispatchWhatsApp: Boolean
)

@Serializable
@Entity(tableName = "manual_time_logs")
data class ManualTimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val personCategory: String, // KARIGAR (कारीगर), OTHER_STAFF (बाकी स्टाफ / वर्कर)
    val roleOrDepartment: String, // Stitching, Katha, Dhaga, Cutting, Supervisor, Helper, Master Ji, Store, Packing, QC
    val phone: String = "9829211122",
    val workDate: String, // e.g., 30/09/2026
    val inTimeManual: String, // e.g., 09:30 AM
    val outTimeManual: String, // e.g., 06:30 PM
    val totalHoursManual: Double, // e.g., 8.5 hours
    val breakMinutesManual: Int = 30,
    val lotNo: String = "",
    val piecesCompleted: Int = 0,
    val rateType: String = "PER_PIECE", // PER_PIECE, PER_HOUR, PER_DAY
    val rateAmountInr: Double = 0.0,
    val totalEarnedInr: Double = 0.0,
    val remarks: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

