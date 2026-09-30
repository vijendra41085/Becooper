package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MaterialItemEntity::class,
        KarigarEntity::class,
        JobChallanEntity::class,
        QcInspectionEntity::class,
        AccountingVoucherEntity::class,
        AuditLogEntity::class,
        RbacPermissionEntity::class,
        ManualTimeLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ErpDatabase : RoomDatabase() {
    abstract fun erpDao(): ErpDao

    companion object {
        @Volatile
        private var INSTANCE: ErpDatabase? = null

        fun getDatabase(context: Context): ErpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ErpDatabase::class.java,
                    "cb_creations_erp.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
