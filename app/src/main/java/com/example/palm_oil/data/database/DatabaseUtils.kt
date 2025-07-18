package com.example.palm_oil.data.database

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DatabaseCallback(
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Initialize database with any required data
        scope.launch(Dispatchers.IO) {
            populateDatabase()
        }
    }

    private suspend fun populateDatabase() {
        // Add any initial data here if needed
        // For example, default settings, lookup tables, etc.
    }
}

/**
 * Database utilities for schema management and debugging
 */
object DatabaseUtils {
    
    /**
     * Create the database schema SQL for manual inspection
     */
    fun getCreateTableSQL(): String {
        return """
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
            );
            
            CREATE INDEX IF NOT EXISTS index_recon_forms_tree_id ON recon_forms(tree_id);
            CREATE INDEX IF NOT EXISTS index_recon_forms_plot_id ON recon_forms(plot_id);
            CREATE INDEX IF NOT EXISTS index_recon_forms_created_at ON recon_forms(created_at);
            CREATE INDEX IF NOT EXISTS index_recon_forms_is_synced ON recon_forms(is_synced);
        """.trimIndent()
    }
    
    /**
     * Get database statistics
     */
    suspend fun getDatabaseStats(dao: ReconFormDao): DatabaseStats {
        return DatabaseStats(
            totalForms = dao.getFormsCount(),
            unsyncedForms = dao.getUnsyncedFormsCount(),
            formsWithImages = dao.getReconFormsWithImages().size
        )
    }
}

data class DatabaseStats(
    val totalForms: Int,
    val unsyncedForms: Int,
    val formsWithImages: Int
)
