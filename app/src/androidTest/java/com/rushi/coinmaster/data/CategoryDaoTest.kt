package com.rushi.coinmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rushi.coinmaster.data.local.dao.CategoryDao
import com.rushi.coinmaster.data.local.database.CoinMasterDatabase
import com.rushi.coinmaster.data.local.entity.CategoryEntity
import com.rushi.coinmaster.data.local.model.BucketType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: CoinMasterDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CoinMasterDatabase::class.java).build()
        categoryDao = db.categoryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun writeAndReadCategory() = runBlocking {
        val category = CategoryEntity(
            name = "Groceries",
            bucketType = BucketType.NEEDS,
            colorHex = "#FF0000",
            iconName = "ic_shopping",
            displayOrder = 1
        )
        val id = categoryDao.insertCategory(category)
        val fetched = categoryDao.getCategoryById(id)

        assertEquals("Groceries", fetched?.name)
        assertEquals(BucketType.NEEDS, fetched?.bucketType)
        assertEquals(1, fetched?.displayOrder)
    }

    @Test
    fun testOrderingAndSoftDelete() = runBlocking {
        val cat1 = CategoryEntity(
            name = "Rent",
            bucketType = BucketType.NEEDS,
            colorHex = "#FFFFFF",
            iconName = "ic_home",
            displayOrder = 2
        )
        val cat2 = CategoryEntity(
            name = "Investments",
            bucketType = BucketType.SAVINGS,
            colorHex = "#00FF00",
            iconName = "ic_invest",
            displayOrder = 1
        )
        categoryDao.insertCategory(cat1)
        val id2 = categoryDao.insertCategory(cat2)

        // Assert visible and sorted by display_order
        val categories = categoryDao.getCategoriesFlow().first()
        assertEquals(2, categories.size)
        assertEquals("Investments", categories[0].name) // displayOrder = 1 comes first
        assertEquals("Rent", categories[1].name) // displayOrder = 2 comes second

        // Soft delete Investments
        categoryDao.softDeleteCategory(id2)

        val categoriesAfter = categoryDao.getCategoriesFlow().first()
        assertEquals(1, categoriesAfter.size)
        assertEquals("Rent", categoriesAfter[0].name)
        assertNull(categoryDao.getCategoryById(id2))
    }
}
