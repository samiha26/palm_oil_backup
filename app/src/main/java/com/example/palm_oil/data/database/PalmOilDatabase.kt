package com.example.palm_oil.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [ReconFormEntity::class, HarvesterProofEntity::class],
    version = 3,
    exportSchema = true
)
abstract class PalmOilDatabase : RoomDatabase() {
    abstract fun reconFormDao(): ReconFormDao
    abstract fun harvesterProofDao(): HarvesterProofDao

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

        // Migration from version 2 to 3 (add harvester proof table)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the harvester_proofs table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS harvester_proofs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tree_id TEXT NOT NULL,
                        plot_id TEXT NOT NULL,
                        image_path TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        is_synced INTEGER NOT NULL DEFAULT 0,
                        sync_timestamp INTEGER,
                        location_latitude REAL,
                        location_longitude REAL,
                        notes TEXT
                    )
                """.trimIndent())
                
                // Create indexes for better query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_harvester_proofs_tree_id ON harvester_proofs(tree_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_harvester_proofs_plot_id ON harvester_proofs(plot_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_harvester_proofs_created_at ON harvester_proofs(created_at)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_harvester_proofs_is_synced ON harvester_proofs(is_synced)")
            }
        }

        fun getDatabase(context: Context): PalmOilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PalmOilDatabase::class.java,
                    "palm_oil_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration() // Only for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
