package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "meal_plans")
@JsonClass(generateAdapter = true)
data class MealPlan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val mealType: String, // "Breakfast", "Lunch", "Dinner"
    val name: String,
    val ingredients: String, // Comma-separated or short description
    val carbonScore: Double, // in kg CO2
    val alternativeName: String, // suggested swap name
    val alternativeCarbonScore: Double, // in kg CO2
    val alternativeDescription: String, // description of why / how
    val isSwapped: Boolean = false, // if the user has tapped single-tap swap
    val timestamp: Long = System.currentTimeMillis()
)
