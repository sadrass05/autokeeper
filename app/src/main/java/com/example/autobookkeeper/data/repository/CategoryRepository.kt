package com.example.autobookkeeper.data.repository

import com.example.autobookkeeper.data.dao.CategoryDao
import com.example.autobookkeeper.data.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories()
    }

    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun getCategoryByName(name: String): Category? {
        return categoryDao.getCategoryByName(name)
    }

    suspend fun insertCategory(category: Category): Long {
        return categoryDao.insert(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    suspend fun initDefaultCategories() {
        val defaultCategories = listOf(
            Category(name = "餐饮", icon = "utensils", color = 0xFFF44336.toInt()),
            Category(name = "交通", icon = "car", color = 0xFF2196F3.toInt()),
            Category(name = "购物", icon = "shopping", color = 0xFF4CAF50.toInt()),
            Category(name = "娱乐", icon = "gamepad", color = 0xFFFF9800.toInt()),
            Category(name = "医疗", icon = "heart", color = 0xFF9C27B0.toInt()),
            Category(name = "教育", icon = "book", color = 0xFF00BCD4.toInt()),
            Category(name = "住房", icon = "home", color = 0xFF607D8B.toInt()),
            Category(name = "理财", icon = "trending_up", color = 0xFFE91E63.toInt()),
            Category(name = "理财支出", icon = "money_bill", color = 0xFFFF5722.toInt())
        )

        defaultCategories.forEach { category ->
            if (getCategoryByName(category.name) == null) {
                insertCategory(category)
            }
        }
    }
}