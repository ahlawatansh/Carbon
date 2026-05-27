package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.GeminiCarbonResponse
import com.example.data.model.MealPlan
import com.example.data.repository.MealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EvaluationState {
    object Idle : EvaluationState
    object Loading : EvaluationState
    data class Success(val response: GeminiCarbonResponse) : EvaluationState
    data class Error(val message: String) : EvaluationState
}

class MealViewModel(private val repository: MealRepository) : ViewModel() {

    // Planned meals reactive flow
    val plannedMeals: StateFlow<List<MealPlan>> = repository.allMeals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Form/Evaluation state for adding custom meals
    private val _evaluationState = MutableStateFlow<EvaluationState>(EvaluationState.Idle)
    val evaluationState: StateFlow<EvaluationState> = _evaluationState.asStateFlow()

    init {
        // Automatically default pre-populate planned meals on first launch
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    /**
     * Swaps an individual meal to its low-carbon alternative (1-tap swap!).
     */
    fun toggleSwap(meal: MealPlan) {
        viewModelScope.launch {
            repository.updateMeal(meal.copy(isSwapped = !meal.isSwapped))
        }
    }

    /**
     * Delete a meal from the planner.
     */
    fun deleteMeal(meal: MealPlan) {
        viewModelScope.launch {
            repository.deleteMealById(meal.id)
        }
    }

    /**
     * Clear all planned meals and restore standard mock data.
     */
    fun resetPlanner() {
        viewModelScope.launch {
            repository.clearAll()
            repository.prepopulateIfEmpty()
        }
    }

    /**
     * Query carbon foot-print from repository.
     */
    fun scoreRecipe(name: String, ingredients: String) {
        if (name.isBlank()) {
            _evaluationState.value = EvaluationState.Error("Please enter a meal name.")
            return
        }
        viewModelScope.launch {
            _evaluationState.value = EvaluationState.Loading
            try {
                val response = repository.scoreRecipeWithGemini(name, ingredients)
                _evaluationState.value = EvaluationState.Success(response)
            } catch (e: Exception) {
                _evaluationState.value = EvaluationState.Error("Failed to evaluate carbon footprint: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Saves evaluated recipe directly into the calendar planner.
     */
    fun addEvaluatedMealToPlanner(
        dayOfWeek: String,
        mealType: String,
        response: GeminiCarbonResponse,
        ingredients: String
    ) {
        viewModelScope.launch {
            val meal = MealPlan(
                dayOfWeek = dayOfWeek,
                mealType = mealType,
                name = response.mealName,
                ingredients = ingredients.ifEmpty { "Default standard portion" },
                carbonScore = response.originalCarbonScore,
                alternativeName = response.alternativeMealName,
                alternativeCarbonScore = response.alternativeCarbonScore,
                alternativeDescription = response.alternativeDescription,
                isSwapped = false
            )
            repository.insertMeal(meal)
            _evaluationState.value = EvaluationState.Idle
        }
    }

    /**
     * Directly inserts a simple custom meal without scanning.
     */
    fun addSimpleMeal(
        dayOfWeek: String,
        mealType: String,
        name: String,
        ingredients: String,
        carbonScore: Double,
        altName: String,
        altScore: Double,
        altDesc: String
    ) {
        viewModelScope.launch {
            val meal = MealPlan(
                dayOfWeek = dayOfWeek,
                mealType = mealType,
                name = name,
                ingredients = ingredients,
                carbonScore = carbonScore,
                alternativeName = altName,
                alternativeCarbonScore = altScore,
                alternativeDescription = altDesc,
                isSwapped = false
            )
            repository.insertMeal(meal)
        }
    }

    fun clearEvaluation() {
        _evaluationState.value = EvaluationState.Idle
    }

    // Custom viewmodel provider factory
    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(context)
            val repository = MealRepository(database.mealPlanDao())
            return MealViewModel(repository) as T
        }
    }
}
