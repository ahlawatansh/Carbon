package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.MealPlanDao
import com.example.data.model.GeminiCarbonResponse
import com.example.data.model.IngredientScore
import com.example.data.model.MealPlan
import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.GenerationConfig
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Locale

class MealRepository(private val mealPlanDao: MealPlanDao) {

    val allMeals: Flow<List<MealPlan>> = mealPlanDao.getAllMeals()

    suspend fun insertMeal(meal: MealPlan) = withContext(Dispatchers.IO) {
        mealPlanDao.insertMeal(meal)
    }

    suspend fun updateMeal(meal: MealPlan) = withContext(Dispatchers.IO) {
        mealPlanDao.updateMeal(meal)
    }

    suspend fun deleteMealById(id: Int) = withContext(Dispatchers.IO) {
        mealPlanDao.deleteMealById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mealPlanDao.clearAll()
    }

    /**
     * Pre-populates standard meals if the planner is currently empty.
     */
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val existing = allMeals.firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val initialMeals = listOf(
                MealPlan(
                    dayOfWeek = "Monday",
                    mealType = "Lunch",
                    name = "Prime Beef Cheeseburger",
                    ingredients = "Beef patty, Brioche bun, Cheddar cheese, Tomato, Onion, Mayo",
                    carbonScore = 6.8,
                    alternativeName = "Smoked Black Bean & Walnut Burger",
                    alternativeCarbonScore = 0.6,
                    alternativeDescription = "A rich house-made black bean and walnut patty seasoned with liquid smoke. Keeps the warm brioche bun and toppings, which reduces environmental impact by 91%!",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Tuesday",
                    mealType = "Dinner",
                    name = "Creamy Chicken Masala",
                    ingredients = "Chicken breast, Butter-cream gravy, Garam masala, Basmati rice",
                    carbonScore = 3.8,
                    alternativeName = "Aromatic Tofu Butter Masala",
                    alternativeCarbonScore = 0.5,
                    alternativeDescription = "Grilled tandoori tofu pieces nestled in the exact same spiced curry sauce of Kashmiri chilies, ginger, and cashew cream, saving 3.3 kg CO2.",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Wednesday",
                    mealType = "Dinner",
                    name = "Pork Sausage Penne Pasta",
                    ingredients = "Italian pork sausage, Crushed tomatoes, Marinara, Penne pasta, Parmesan",
                    carbonScore = 3.2,
                    alternativeName = "Fennel-Herbed Mushroom Pasta",
                    alternativeCarbonScore = 0.7,
                    alternativeDescription = "Swaps pork with protein-rich browned cremini mushrooms infused with fennel seeds and garlic to preserve that signature herbaceous Italian flavor profile.",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Thursday",
                    mealType = "Lunch",
                    name = "Atlantic Salmon Teriyaki",
                    ingredients = "Atlantic Salmon filet, Teriyaki sauce, Broccoli, Jasmine rice",
                    carbonScore = 2.4,
                    alternativeName = "Crispy Terpeh Teriyaki Bowls",
                    alternativeCarbonScore = 0.5,
                    alternativeDescription = "Flashes fried organic tempeh triangles for crunch, glazed with sweet garlic-ginger sake glaze. Keeps the broccoli and rice completely identical.",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Friday",
                    mealType = "Dinner",
                    name = "Triple Cheese Pepperoni Pizza",
                    ingredients = "Pepperoni sausage, Mozzarella cheese, Cheddar, Yeast crust, Tomato sauce",
                    carbonScore = 3.5,
                    alternativeName = "Wood-fired Portobello & Olive Pizza",
                    alternativeCarbonScore = 0.9,
                    alternativeDescription = "An earthy blend of roasted garlic-basted Portobello mushrooms and black kalamata olives. Reduces impact by 74% while keeping standard bubbling cheese.",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Saturday",
                    mealType = "Dinner",
                    name = "Spicy Beef Tacos",
                    ingredients = "Ground beef, Hard corn shells, Sour cream, Cheddar, Jalapenos, Salsa",
                    carbonScore = 5.4,
                    alternativeName = "Smoky Lentil & Queso Tacos",
                    alternativeCarbonScore = 0.6,
                    alternativeDescription = "Savory brown lentils simmered in cumin and chipotle adobo, topped with freshly grated cheese and pico de gallo.",
                    isSwapped = false
                ),
                MealPlan(
                    dayOfWeek = "Sunday",
                    mealType = "Lunch",
                    name = "Classic Chicken Caesar Salad",
                    ingredients = "Grilled chicken strips, Romaine lettuce, Creamy Caesar dressing, Bacon bits, Croutons",
                    carbonScore = 3.0,
                    alternativeName = "Charred Chickpea Caesar Salad",
                    alternativeCarbonScore = 0.6,
                    alternativeDescription = "Replace chicken and bacon with oven-crisped smoky dynamic spiced chickpeas. Keeps the thick house Caesar dressing and shaved parm.",
                    isSwapped = false
                )
            )
            mealPlanDao.insertMeals(initialMeals)
        }
    }

    /**
     * Query Gemini api to score recipe and suggest a swap, falling back to local formulas.
     */
    suspend fun scoreRecipeWithGemini(mealName: String, ingredients: String): GeminiCarbonResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasApiKey = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY"

        if (hasApiKey) {
            try {
                val promptText = """
                    You are 'CarbonKitchen', an environmental food footprint AI modeling engine.
                    Calculate the carbon footprint (kg CO2 equivalent) of the following recipe:
                    Meal Name: $mealName
                    Ingredients: $ingredients

                    Provide a tasty, low-carbon 'Carbon-Smart Swap' alternative meal that uses the same spices/profile but replaces high-footprint items with a plant-based or low-impact protein alternative (e.g. replacing beef with beans/mushrooms, pork with lentils, poultry/fish with tofu/chickpeas).
                    
                    You MUST return ONLY a valid JSON object matching this schema. Do not add any markdown formatting, do not wrap in ```json blocks. Return only the parsable JSON string.

                    Schema fields:
                    {
                      "mealName": "Original meal name",
                      "originalCarbonScore": 4.2,
                      "ingredientBreakdown": [
                        { "name": "Main protein ingredient", "carbonScore": 3.6 },
                        { "name": "Other main", "carbonScore": 0.4 },
                        { "name": "Spices and accessories", "carbonScore": 0.2 }
                      ],
                      "alternativeMealName": "Low-Impact alternatives swap recipe",
                      "alternativeCarbonScore": 0.4,
                      "alternativeDescription": "Replace [muscle] with [low carbon protein]. Keeps the same aromatic flavor profile, spices, and preparation style but saves 3.8 kg CO2!"
                    }
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = promptText)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.2f)
                )

                val response = RetrofitClient.geminiService.generateContent(apiKey, request)
                val rawJsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (!rawJsonText.isNullOrBlank()) {
                    // Strip markdown block quotes if the model ignored request and put them anyway
                    val cleanedJsonText = rawJsonText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val adapter = RetrofitClient.moshiInstance.adapter(GeminiCarbonResponse::class.java)
                    val parsed = adapter.fromJson(cleanedJsonText)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                }
            } catch (e: Exception) {
                Log.e("MealRepository", "Gemini API error, falling back locally: ${e.message}", e)
            }
        }

        // Falls back to high-fidelity localized algorithm if API fails or is unset
        return@withContext calculateFallbackCarbonScore(mealName, ingredients)
    }

    /**
     * Fallback calculation engine when API is offline or key is missing.
     */
    private fun calculateFallbackCarbonScore(mealName: String, ingredients: String): GeminiCarbonResponse {
        val normalizedMeal = mealName.lowercase(Locale.ROOT)
        val normalizedIngs = ingredients.lowercase(Locale.ROOT)
        
        val breakdown = mutableListOf<IngredientScore>()
        var baseScore = 0.6 // minimal default veggie footprint

        // Simple realistic ingredient keyword weighting system (kg CO2-eq per meal portion)
        var proteinName = "Seasonal Veggies"
        var proteinScore = 0.3

        when {
            normalizedIngs.contains("beef") || normalizedMeal.contains("beef") || normalizedMeal.contains("burger") -> {
                proteinName = "Prime Beef Patty / Portion"
                proteinScore = 5.8
                baseScore += 5.8
            }
            normalizedIngs.contains("pork") || normalizedIngs.contains("ham") || normalizedIngs.contains("sausage") -> {
                proteinName = "Lean Pork Portion"
                proteinScore = 2.4
                baseScore += 2.4
            }
            normalizedIngs.contains("chicken") || normalizedIngs.contains("poultry") || normalizedMeal.contains("chicken") -> {
                proteinName = "Fresh Poultry Portion"
                proteinScore = 2.1
                baseScore += 2.1
            }
            normalizedIngs.contains("salmon") || normalizedIngs.contains("fish") || normalizedIngs.contains("tuna") -> {
                proteinName = "Seafood Portion"
                proteinScore = 1.6
                baseScore += 1.6
            }
            normalizedIngs.contains("cheese") || normalizedIngs.contains("butter") || normalizedIngs.contains("cream") -> {
                breakdown.add(IngredientScore("Heavy Dairy & Cheese", 1.2))
                baseScore += 1.2
            }
        }

        if (proteinScore > 0.3) {
            breakdown.add(IngredientScore(proteinName, proteinScore))
        }

        // Add additional typical accessories
        if (normalizedIngs.contains("avocado")) {
            breakdown.add(IngredientScore("Fresh Avocado", 0.5))
            baseScore += 0.5
        }
        if (normalizedIngs.contains("rice") || normalizedIngs.contains("bread") || normalizedIngs.contains("pasta")) {
            breakdown.add(IngredientScore("Grains & Carbs", 0.3))
            baseScore += 0.3
        }
        breakdown.add(IngredientScore("Spices, Herbs & Water", 0.2))

        val roundedOriginal = Math.round(baseScore * 10.0) / 10.0

        // Determine matching smart carbon alternatives swap based on proteins
        val swapName: String
        val swapScore: Double
        val swapDescription: String

        when {
            normalizedIngs.contains("beef") || normalizedMeal.contains("beef") || normalizedMeal.contains("burger") -> {
                swapName = "Gourmet Mushroom-Lentil alternative"
                swapScore = 0.5
                swapDescription = "Swap out high-impact beef for a rich, umami-loaded savory mushroom, brown lentil, and rolled oats blend. Preserves identical texture and spice notes while cutting carbon by over 90%!"
            }
            normalizedIngs.contains("pork") || normalizedIngs.contains("ham") || normalizedIngs.contains("sausage") -> {
                swapName = "Spiced Fennel Tempeh substitute"
                swapScore = 0.4
                swapDescription = "Utilize organic crumbled tempeh infused with fennel seed, garlic powder, and smoked paprika to reconstruct robust Italian pork flavors."
            }
            normalizedIngs.contains("chicken") || normalizedIngs.contains("poultry") || normalizedMeal.contains("chicken") -> {
                swapName = "Sautéed Organic Tofu cubes"
                swapScore = 0.4
                swapDescription = "Replace chicken breasts with press-drained, extra-firm organic tofu, lightly pan-seared to lock in seasonings, spices, and the curry gravy perfectly."
            }
            normalizedIngs.contains("salmon") || normalizedIngs.contains("fish") || normalizedIngs.contains("tuna") -> {
                swapName = "Tender Chickpea-Seaweed blend"
                swapScore = 0.3
                swapDescription = "Mash chickpeas combined with finely flaked nori seaweed sheets to recreate ocean-fresh teriyaki flaking, reducing the carbon footprint to nearly zero."
            }
            else -> {
                swapName = "Spiced Jackfruit & Cauliflower swap"
                swapScore = 0.3
                swapDescription = "A highly delicious shift using tender shredded jackfruit. Minimizes carbon impacts down to baseline organic soil values."
            }
        }

        return GeminiCarbonResponse(
            mealName = mealName,
            originalCarbonScore = roundedOriginal,
            ingredientBreakdown = breakdown,
            alternativeMealName = swapName,
            alternativeCarbonScore = swapScore,
            alternativeDescription = swapDescription
        )
    }
}
