package com.rushi.coinmaster.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CoinMasterDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        // Create database with version 5
        var db = helper.createDatabase(TEST_DB, 5)

        // Insert dummy data compatible with v5 schema
        db.execSQL("INSERT INTO accounts (name, type, balance_paise, color_hex, icon_name, is_deleted) VALUES ('Cash Account', 'CASH', 100000, '#FFFFFF', 'cash', 0)")
        db.execSQL("INSERT INTO categories (name, bucket_type, color_hex, icon_name, display_order, is_deleted) VALUES ('Envelope A', 'NEEDS', '#FF0000', 'need', 1, 0)")

        db.close()

        // Re-open and migrate to version 6
        db = helper.runMigrationsAndValidate(TEST_DB, 6, true, CoinMasterDatabase.MIGRATION_5_6)

        // Verify that categories table has the new columns
        val cursorCategories = db.query("SELECT * FROM categories LIMIT 1")
        val categoryIdCol = cursorCategories.getColumnIndex("expense_category_id")
        val expenseTypeCol = cursorCategories.getColumnIndex("expense_type")
        assert(categoryIdCol != -1)
        assert(expenseTypeCol != -1)
        cursorCategories.moveToFirst()
        assert(cursorCategories.getString(expenseTypeCol) == "VARIABLE") // default value
        cursorCategories.close()

        // Verify that transactions table has the new column
        val cursorTransactions = db.query("SELECT * FROM transactions LIMIT 1")
        val recipientIdCol = cursorTransactions.getColumnIndex("transfer_recipient_id")
        assert(recipientIdCol != -1)
        cursorTransactions.close()
    }
}
