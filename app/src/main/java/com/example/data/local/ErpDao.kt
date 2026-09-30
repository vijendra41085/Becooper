package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ErpDao {
    // --- Material & Inventory ---
    @Query("SELECT * FROM material_items ORDER BY lastUpdated DESC")
    fun getAllMaterials(): Flow<List<MaterialItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(item: MaterialItemEntity)

    @Update
    suspend fun updateMaterial(item: MaterialItemEntity)

    @Query("DELETE FROM material_items WHERE id = :id")
    suspend fun deleteMaterialById(id: Int)

    @Query("SELECT COUNT(*) FROM material_items")
    suspend fun countMaterials(): Int

    // --- Karigars & Rate Master ---
    @Query("SELECT * FROM karigars ORDER BY avgSpeedScore DESC, totalPiecesDone DESC")
    fun getAllKarigars(): Flow<List<KarigarEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKarigar(karigar: KarigarEntity)

    @Update
    suspend fun updateKarigar(karigar: KarigarEntity)

    @Query("DELETE FROM karigars WHERE id = :id")
    suspend fun deleteKarigarById(id: Int)

    // --- Job Challans (Stitching / Dhaga / Katha / Purchase / Delivery) ---
    @Query("SELECT * FROM job_challans ORDER BY id DESC")
    fun getAllChallans(): Flow<List<JobChallanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallan(challan: JobChallanEntity)

    @Update
    suspend fun updateChallan(challan: JobChallanEntity)

    @Query("DELETE FROM job_challans WHERE id = :id")
    suspend fun deleteChallanById(id: Int)

    @Query("UPDATE job_challans SET qcStatus = :qcStatus, workflowStage = :stage WHERE lotNo = :lotNo")
    suspend fun updateChallanQcAndStageByLot(lotNo: String, qcStatus: String, stage: String)

    // --- Quality Control (QC) Inspections ---
    @Query("SELECT * FROM qc_inspections ORDER BY timestamp DESC")
    fun getAllQcInspections(): Flow<List<QcInspectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQcInspection(inspection: QcInspectionEntity)

    @Update
    suspend fun updateQcInspection(inspection: QcInspectionEntity)

    @Query("DELETE FROM qc_inspections WHERE id = :id")
    suspend fun deleteQcInspectionById(id: Int)

    // --- Accounting Vouchers & Ledger ---
    @Query("SELECT * FROM accounting_vouchers ORDER BY timestamp DESC")
    fun getAllVouchers(): Flow<List<AccountingVoucherEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: AccountingVoucherEntity)

    @Update
    suspend fun updateVoucher(voucher: AccountingVoucherEntity)

    @Query("DELETE FROM accounting_vouchers WHERE id = :id")
    suspend fun deleteVoucherById(id: Int)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // --- RBAC Permissions ---
    @Query("SELECT * FROM rbac_permissions ORDER BY roleName ASC, moduleName ASC")
    fun getAllRbacPermissions(): Flow<List<RbacPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRbacPermission(permission: RbacPermissionEntity)

    @Update
    suspend fun updateRbacPermission(permission: RbacPermissionEntity)

    @Query("SELECT COUNT(*) FROM rbac_permissions")
    suspend fun countRbacPermissions(): Int

    @Query("DELETE FROM rbac_permissions")
    suspend fun clearRbacPermissions()

    // --- Manual Time Logs (Karigars & Other Staff) ---
    @Query("SELECT * FROM manual_time_logs ORDER BY timestamp DESC")
    fun getAllManualTimeLogs(): Flow<List<ManualTimeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualTimeLog(log: ManualTimeLogEntity)

    @Update
    suspend fun updateManualTimeLog(log: ManualTimeLogEntity)

    @Query("DELETE FROM manual_time_logs WHERE id = :id")
    suspend fun deleteManualTimeLogById(id: Int)

    @Query("DELETE FROM manual_time_logs")
    suspend fun clearAllManualTimeLogs()

    // --- Clear All Test / Transactional Data ---
    @Query("DELETE FROM material_items")
    suspend fun clearAllMaterials()

    @Query("DELETE FROM karigars")
    suspend fun clearAllKarigars()

    @Query("DELETE FROM job_challans")
    suspend fun clearAllChallans()

    @Query("DELETE FROM qc_inspections")
    suspend fun clearAllQcInspections()

    @Query("DELETE FROM accounting_vouchers")
    suspend fun clearAllVouchers()

    @Query("DELETE FROM audit_logs")
    suspend fun clearAllAuditLogs()
}
