package com.example.palm_oil.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [ReconFormEntity::class],
    version = 2,
    exportSchema = true
)
abstract class PalmOilDatabase : RoomDatabase() {
    abstract fun reconFormDao(): ReconFormDao

    companion object {
        @Volatile
        private var INSTANCE: PalmOilDatabase? = null

        // Migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop the old table
                database.execSQL("DROP TABLE IF EXISTS recon_forms")
                
                // Create the new table with proper column names and constraints
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS recon_forms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tree_id TEXT NOT NULL,
                        plot_id TEXT NOT NULL,
                        number_of_fruits INTEGER NOT NULL,
                        harvest_days INTEGER NOT NULL,
                        image1_path TEXT,
                        image2_path TEXT,
                        image3_path TEXT,
                        created_at INTEGER NOT NULL,
                        is_synced INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // Create indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recon_forms_tree_id ON recon_forms(tree_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recon_forms_plot_id ON recon_forms(plot_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recon_forms_created_at ON recon_forms(created_at)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_recon_forms_is_synced ON recon_forms(is_synced)")
            }
        }

        fun getDatabase(context: Context): PalmOilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PalmOilDatabase::class.java,
                    "palm_oil_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Only for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
