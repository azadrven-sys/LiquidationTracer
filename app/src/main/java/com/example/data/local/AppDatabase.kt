package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AppConfig
import com.example.data.model.LiquidationRecord
import com.example.data.model.UnitItem
import kotlinx.coroutines.CoroutineScope

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LiquidationRecord::class, UnitItem::class, AppConfig::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun liquidationDao(): LiquidationDao
    abstract fun unitDao(): UnitDao
    abstract fun configDao(): ConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `liquidation_records` ADD COLUMN `updatedAt` INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `app_config` ADD COLUMN `defaultAmount` REAL NOT NULL DEFAULT 1.0")
                } catch (_: Exception) {}
                try {
                    db.execSQL("ALTER TABLE `liquidation_records` ADD COLUMN `updatedAt` INTEGER")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `app_config` ADD COLUMN `defaultAmount` REAL NOT NULL DEFAULT 1.0")
                } catch (_: Exception) {
                    db.execSQL("DROP TABLE IF EXISTS `app_config`")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `defaultTargetPercentage` REAL NOT NULL, `defaultUnit` TEXT NOT NULL, `defaultAmount` REAL NOT NULL, `currencySymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
                }
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `app_config`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `defaultTargetPercentage` REAL NOT NULL, `defaultUnit` TEXT NOT NULL, `defaultAmount` REAL NOT NULL, `currencySymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `app_config`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `defaultTargetPercentage` REAL NOT NULL, `defaultUnit` TEXT NOT NULL, `defaultAmount` REAL NOT NULL, `currencySymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `app_config`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `defaultTargetPercentage` REAL NOT NULL, `defaultUnit` TEXT NOT NULL, `defaultAmount` REAL NOT NULL, `currencySymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `app_config`")
                db.execSQL("CREATE TABLE IF NOT EXISTS `app_config` (`id` INTEGER NOT NULL, `defaultTargetPercentage` REAL NOT NULL, `defaultUnit` TEXT NOT NULL, `defaultAmount` REAL NOT NULL, `currencySymbol` TEXT NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val db = try {
                    buildDatabase(context)
                } catch (e: Exception) {
                    try {
                        context.deleteDatabase("liquidation_database")
                    } catch (_: Exception) {}
                    buildDatabase(context)
                }
                INSTANCE = db
                db
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "liquidation_database"
            )
            .addMigrations(MIGRATION_6_7, MIGRATION_5_7, MIGRATION_5_6, MIGRATION_4_6, MIGRATION_3_6, MIGRATION_2_6, MIGRATION_1_6)
            .fallbackToDestructiveMigration(true)
            .fallbackToDestructiveMigrationOnDowngrade(true)
            .build()
        }
    }
}
