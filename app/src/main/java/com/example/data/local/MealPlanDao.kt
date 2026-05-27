package com.example.data.local

import androidx.room.*
import com.example.data.model.MealPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans ORDER BY timestamp ASC")
    fun getAllMeals(): Flow<List<MealPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealPlan>)

    @Update
    suspend fun updateMeal(meal: MealPlan)

    @Delete
    suspend fun deleteMeal(meal: MealPlan)

    @Query("DELETE FROM meal_plans WHERE id = :id")
    suspend fun deleteMealById(id: Int)

    @Query("DELETE FROM meal_plans")
    suspend fun clearAll()
}
