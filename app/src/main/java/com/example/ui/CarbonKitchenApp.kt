package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GeminiCarbonResponse
import com.example.data.model.MealPlan
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarbonKitchenApp(
    viewModel: MealViewModel,
    modifier: Modifier = Modifier
) {
    val meals by viewModel.plannedMeals.collectAsStateWithLifecycle()
    val evaluationState by viewModel.evaluationState.collectAsStateWithLifecycle()

    var selectedDayFilter by remember { mutableStateOf("All") }
    var isAddScopingDialogVisible by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf("Planner") } // Tabs: Planner, Impact, Pantry, Settings

    // Aggregate statistics
    val totalOriginalFootprint = meals.sumOf { it.carbonScore }
    val totalActualFootprint = meals.sumOf { if (it.isSwapped) it.alternativeCarbonScore else it.carbonScore }
    val totalSavedC02 = maxOf(0.0, totalOriginalFootprint - totalActualFootprint)
    val totalMealsCount = meals.size
    val optimizedMealsCount = meals.count { it.isSwapped }

    // Driving calculations: 1 kg CO2 ~ 2.6 miles in average car
    val milesEquivalentString = String.format(Locale.US, "%.1f", totalSavedC02 * 2.6)
    val treesSavedString = String.format(Locale.US, "%.1f", totalSavedC02 / 0.06)

    val daysOfWeek = listOf("All", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Elegant Sleek Header mimicking the Design HTML Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Brand elements
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Potted plant logo representation",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "CarbonKitchen",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = SleekOnBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "WEEKLY CO₂ NAVIGATOR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SleekMutedText,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Right User profile placeholder avatar
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(2.dp, SleekOutline, CircleShape)
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User avatar",
                            tint = SleekPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // High-fidelity Bottom Navigation Row representing the Design HTML navigation bar
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = SleekOutline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .background(MaterialTheme.colorScheme.background),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavTab(
                        label = "Impact",
                        icon = Icons.Default.Star,
                        isActive = currentTab == "Impact",
                        onClick = { currentTab = "Impact" }
                    )
                    BottomNavTab(
                        label = "Planner",
                        icon = Icons.Default.DateRange,
                        isActive = currentTab == "Planner",
                        onClick = { currentTab = "Planner" }
                    )
                    BottomNavTab(
                        label = "Pantry Swaps",
                        icon = Icons.Default.Search,
                        isActive = currentTab == "Pantry",
                        onClick = { currentTab = "Pantry" }
                    )
                    BottomNavTab(
                        label = "Settings",
                        icon = Icons.Default.Settings,
                        isActive = currentTab == "Settings",
                        onClick = { currentTab = "Settings" }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == "Planner") {
                FloatingActionButton(
                    onClick = {
                        viewModel.clearEvaluation()
                        isAddScopingDialogVisible = true
                    },
                    containerColor = SleekPrimary,
                    contentColor = SleekOnPrimary,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .testTag("add_meal_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Meal")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Score Custom Recipe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = currentTab,
            label = "tab_crossfade",
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) { tabState ->
            when (tabState) {
                "Impact" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        StatsHeaderPanel(
                            totalActualFootprint = totalActualFootprint,
                            totalOriginalFootprint = totalOriginalFootprint,
                            totalSavedC02 = totalSavedC02,
                            milesEquivalent = milesEquivalentString,
                            treesSaved = treesSavedString,
                            optimizedCount = optimizedMealsCount,
                            totalCount = totalMealsCount
                        )

                        // Smart Spotlight Swap suggestion matching the HTML feature exactly!
                        val unswappedMeal = meals.firstOrNull { !it.isSwapped }
                        if (unswappedMeal != null) {
                            Text(
                                text = "RECOMMENDED SPOTLIGHT SWAP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(28.dp),
                                border = BorderStroke(1.dp, SleekOutline),
                                colors = CardDefaults.cardColors(containerColor = SleekSurface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Auto awesome spark",
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "DYNAMIC SMART SWAP",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekMutedText,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // On Menu Original
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SleekOnMenuBg, RoundedCornerShape(16.dp))
                                            .border(BorderStroke(1.dp, SleekDashedBorder), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "ON PLAN MENU (${unswappedMeal.dayOfWeek})",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekMutedText
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = unswappedMeal.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekOnBackground
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${unswappedMeal.carbonScore} kg",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekErrorRed
                                            )
                                            Text(
                                                text = "CO₂ Impact",
                                                fontSize = 10.sp,
                                                color = SleekMutedText
                                            )
                                        }
                                    }

                                    // Swap connection arrows
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(SleekPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Swap icon",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // Smart Swap Choice
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(SleekEcoBetterBg, RoundedCornerShape(16.dp))
                                            .border(BorderStroke(1.dp, SleekPrimary), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "BETTER SMART CHOICE",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = unswappedMeal.alternativeName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekOnBackground
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${unswappedMeal.alternativeCarbonScore} kg",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SleekPrimary
                                            )
                                            val savedPct = (((unswappedMeal.carbonScore - unswappedMeal.alternativeCarbonScore) / unswappedMeal.carbonScore) * 100).toInt()
                                            Text(
                                                text = "$savedPct% saved",
                                                fontSize = 10.sp,
                                                color = SleekPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Action Button
                                    Button(
                                        onClick = { viewModel.toggleSwap(unswappedMeal) },
                                        shape = RoundedCornerShape(100.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            text = "SWAP FOR THIS WEEK",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            // All meals swapped state
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = SleekEcoBetterBg)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Perfect score",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "All Planned Meals Fully Optimized!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Your weekly carbon score is at the lowest baseline rate. Fantastic work!",
                                        fontSize = 13.sp,
                                        color = SleekMutedText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Local dynamic breakdown chart by day
                        Text(
                            text = "WEEKLY CARBON ALLOCATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(28.dp),
                            border = BorderStroke(1.dp, SleekOutline),
                            colors = CardDefaults.cardColors(containerColor = SleekSurface)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                val weekdayList = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                                weekdayList.forEach { wkday ->
                                    val dayMeals = meals.filter { it.dayOfWeek == wkday }
                                    val originalSum = dayMeals.sumOf { it.carbonScore }
                                    val currentSum = dayMeals.sumOf { if (it.isSwapped) it.alternativeCarbonScore else it.carbonScore }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = wkday.substring(0, 3),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekMutedText,
                                            modifier = Modifier.width(42.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SleekOnMenuBg)
                                        ) {
                                            val maxPossibleBar = 10.0
                                            val barFraction = if (originalSum > 0) (currentSum / maxPossibleBar).toFloat().coerceIn(0.02f, 1f) else 0f
                                            val baseFraction = if (originalSum > 0) (originalSum / maxPossibleBar).toFloat().coerceIn(0.02f, 1f) else 0f

                                            // Original potential background tracker
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(baseFraction)
                                                    .background(SleekErrorRed.copy(alpha = 0.25f))
                                            )
                                            // Actively minimized tracker
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth(barFraction)
                                                    .background(if (originalSum > currentSum) SleekPrimary else SleekMutedText)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = String.format(Locale.US, "%.1fkg", currentSum),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SleekOnBackground,
                                            modifier = Modifier.width(48.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "Planner" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        // Quick Reset trigger bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WEEKLY MEAL SCHEDULE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = { viewModel.resetPlanner() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SleekOnMenuBg,
                                    contentColor = SleekPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("reset_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restore Defaults",
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Defaults", fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Horizontal Day selection sliders
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(daysOfWeek) { day ->
                                val isSelected = selectedDayFilter == day
                                val backColor = if (isSelected) SleekPrimary else SleekSurface
                                val fontColor = if (isSelected) SleekOnPrimary else SleekOnBackground
                                val borderStroke = if (isSelected) null else BorderStroke(1.dp, SleekOutline)

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable { selectedDayFilter = day }
                                        .testTag("day_filter_${day.lowercase(Locale.US)}"),
                                    color = backColor,
                                    border = borderStroke,
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text(
                                        text = day,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = fontColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        val filteredMeals = remember(meals, selectedDayFilter) {
                            if (selectedDayFilter == "All") {
                                meals
                            } else {
                                meals.filter { it.dayOfWeek == selectedDayFilter }
                            }
                        }

                        if (filteredMeals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "Empty list",
                                        tint = SleekOutline,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No meals planned for $selectedDayFilter",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SleekOnBackground
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap 'Score Custom Recipe' to compute & add carbon-smart choices!",
                                        fontSize = 13.sp,
                                        color = SleekMutedText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("meals_list"),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredMeals, key = { it.id }) { meal ->
                                    SleekMealPlanCard(
                                        meal = meal,
                                        onSwapToggle = { viewModel.toggleSwap(meal) },
                                        onDelete = { viewModel.deleteMeal(meal) }
                                    )
                                }
                            }
                        }
                    }
                }

                "Pantry" -> {
                    PantryCarbonSwapsTab()
                }

                "Settings" -> {
                    SettingsTabPanel(onReset = { viewModel.resetPlanner() })
                }
            }
        }
    }

    // Modal Intelligent Carbon Scopes
    if (isAddScopingDialogVisible) {
        Dialog(onDismissRequest = { isAddScopingDialogVisible = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
                    .testTag("recipe_scanner_dialog"),
                shape = RoundedCornerShape(28.dp),
                color = SleekSurface,
                tonalElevation = 6.dp
            ) {
                RecipeScannerPanel(
                    evaluationState = evaluationState,
                    onScoreRequest = { name, ingredients -> viewModel.scoreRecipe(name, ingredients) },
                    onSaveRequest = { day, mealType, response, ingredients ->
                        viewModel.addEvaluatedMealToPlanner(day, mealType, response, ingredients)
                        isAddScopingDialogVisible = false
                    },
                    onDismiss = {
                        viewModel.clearEvaluation()
                        isAddScopingDialogVisible = false
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavTab(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (isActive) Color(0xFFD7E8CD) else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) SleekOnSurfaceVariant else SleekMutedText,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) SleekOnBackground else SleekMutedText
        )
    }
}

@Composable
fun StatsHeaderPanel(
    totalActualFootprint: Double,
    totalOriginalFootprint: Double,
    totalSavedC02: Double,
    milesEquivalent: String,
    treesSaved: String,
    optimizedCount: Int,
    totalCount: Int
) {
    val progressOfSavings = if (totalOriginalFootprint > 0.0) {
        (totalActualFootprint / totalOriginalFootprint).toFloat()
    } else {
        0f
    }
    val savedPercentage = if (totalOriginalFootprint > 0.0) {
        ((totalSavedC02 / totalOriginalFootprint) * 100).toInt()
    } else {
        0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("stats_dashboard"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurfaceVariant),
        border = BorderStroke(1.dp, SleekOutline)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CURRENT PLAN FOOTPRINT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekOnSurfaceVariant,
                    letterSpacing = 1.sp
                )
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFB7D1A9))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Saved Carbon badge",
                        tint = SleekPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "-$savedPercentage% CO₂",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = String.format(Locale.US, "%.1f", totalActualFootprint),
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    color = SleekOnSurfaceVariant,
                    lineHeight = 52.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "kg CO₂e",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sleek Horizontal line indicator bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFB7D1A9))
            ) {
                val progressFraction = if (progressOfSavings.isNaN()) 0f else progressOfSavings.coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressOfSavings)
                        .clip(RoundedCornerShape(100.dp))
                        .background(SleekPrimary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Goal limit: 12.0 kg",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SleekMutedText
                )
                Text(
                    text = "Saved total: ${String.format(Locale.US, "%.1f", totalSavedC02)} kg",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = SleekMutedText.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal comparison blocks and equivalents
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CAR EMISSION MILES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekMutedText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$milesEquivalent mi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOnSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TREE ABSORB RATE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekMutedText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$treesSaved trees",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOnSurfaceVariant
                    )
                }

                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = "ACTIVE SWAPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekMutedText,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$optimizedCount / $totalCount",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOnSurfaceVariant
                    )
                }
            }
        }
    }
}

// Sleek Meal Plan Card representing split-level options mirroring HTML design values
@Composable
fun SleekMealPlanCard(
    meal: MealPlan,
    onSwapToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_card_${meal.id}"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header information: day, time category, delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SleekOnSecondaryBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = meal.dayOfWeek,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekOnBackground
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SleekPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = meal.mealType.uppercase(Locale.US),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = SleekPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_meal_${meal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Meal schedule",
                        tint = SleekErrorRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Twin Choices Stack representing the split panels in Sleek CSS Design HTML
            
            // 1. Current Choice ("On Menu") Panel
            val originalAlpha = if (meal.isSwapped) 0.5f else 1.0f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekOnMenuBg, RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (meal.isSwapped) SleekOutline else SleekDashedBorder
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ON MENU CHOICE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekMutedText.copy(alpha = originalAlpha)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meal.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOnBackground.copy(alpha = originalAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ingredients: ${meal.ingredients}",
                        fontSize = 11.sp,
                        color = SleekMutedText.copy(alpha = originalAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${meal.carbonScore} kg",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekErrorRed.copy(alpha = originalAlpha)
                    )
                    Text(
                        text = "CO₂ impact",
                        fontSize = 9.sp,
                        color = SleekMutedText.copy(alpha = originalAlpha)
                    )
                }
            }

            // Connection arrows connector line badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (meal.isSwapped) SleekPrimary else SleekMutedText),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh, // Vertical-sync replacement representation
                        contentDescription = "connector icon",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 2. Sustainable Choice ("Better Choice") Panel
            val betterAlpha = if (meal.isSwapped) 1.0f else 0.7f
            val betterBorder = if (meal.isSwapped) BorderStroke(1.5.dp, SleekPrimary) else BorderStroke(1.dp, SleekOutline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SleekEcoBetterBg, RoundedCornerShape(16.dp))
                    .border(betterBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CARBON-SMART SWAP alternative",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SleekPrimary.copy(alpha = betterAlpha)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meal.alternativeName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekOnBackground.copy(alpha = betterAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = meal.alternativeDescription,
                        fontSize = 11.sp,
                        color = SleekMutedText.copy(alpha = betterAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${meal.alternativeCarbonScore} kg",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary.copy(alpha = betterAlpha)
                    )
                    val percentSaved = (((meal.carbonScore - meal.alternativeCarbonScore) / meal.carbonScore) * 100).toInt()
                    Text(
                        text = "-$percentSaved% CO₂",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekPrimary.copy(alpha = betterAlpha)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pill Shape Trigger Button matching "SWAP FOR THIS WEEK" from CSS Design HTML
            Button(
                onClick = onSwapToggle,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (meal.isSwapped) SleekOnMenuBg else SleekPrimary,
                    contentColor = if (meal.isSwapped) SleekOnBackground else Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("swap_button_${meal.id}")
            ) {
                Text(
                    text = if (meal.isSwapped) "UNDO SWAP" else "🌿 CO₂ SWAP FOR THIS WEEK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun RecipeScannerPanel(
    evaluationState: EvaluationState,
    onScoreRequest: (name: String, ingredients: String) -> Unit,
    onSaveRequest: (day: String, mealType: String, response: GeminiCarbonResponse, ingredients: String) -> Unit,
    onDismiss: () -> Unit
) {
    var mealNameInput by remember { mutableStateOf("") }
    var ingredientsInput by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf("Monday") }
    var selectedMealType by remember { mutableStateOf("Dinner") }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    var isDayExpanded by remember { mutableStateOf(false) }
    var isTypeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CO₂ Carbon Intelligence",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = SleekOnBackground
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close dialogue")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Crossfade(targetState = evaluationState, label = "stage_fade") { state ->
            when (state) {
                is EvaluationState.Idle -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Enter a recipe to check environmental carbon scores. CarbonKitchen AI automatically identifies low-carbon botanical replacement options.",
                            fontSize = 12.sp,
                            color = SleekMutedText,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = mealNameInput,
                            onValueChange = { mealNameInput = it },
                            label = { Text("Recipe/Meal Name (e.g., Spicy Salmon)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recipe_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                focusedLabelColor = SleekPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = ingredientsInput,
                            onValueChange = { ingredientsInput = it },
                            label = { Text("Ingredients list (e.g. Salmon, teriyaki marinade, rice)") },
                            placeholder = { Text("Optional elements checklist") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .testTag("recipe_ingredients_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SleekPrimary,
                                focusedLabelColor = SleekPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onScoreRequest(mealNameInput, ingredientsInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("analyze_button"),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Compute")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compute & Identify Swaps", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                is EvaluationState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SleekPrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Consulting Eco Databases...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekOnBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SleekOnMenuBg)
                                .padding(14.dp)
                        ) {
                            val factsList = remember {
                                listOf(
                                    "Eco Info: Plant-based swaps save up to 90% water compared to beef patties.",
                                    "Eco Info: Swapping beef for beans shrinks meal footprint values by up to 92%.",
                                    "Eco Info: Sourcing fresh organic herbs avoids high-impact dynamic air-freight carbon."
                                )
                            }
                            val selectedFact = remember { factsList.random() }
                            Text(
                                text = selectedFact,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = SleekPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                is EvaluationState.Success -> {
                    val res = state.response
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Impact comparison cards (using Sleek color code)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = SleekOnMenuBg),
                                border = BorderStroke(1.dp, SleekOutline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("ORIGINAL FOOTPRINT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = SleekMutedText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${res.originalCarbonScore} kg",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SleekErrorRed
                                    )
                                    Text(
                                        text = res.mealName,
                                        fontSize = 11.sp,
                                        color = SleekOnBackground,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1.2f),
                                colors = CardDefaults.cardColors(containerColor = SleekEcoBetterBg),
                                border = BorderStroke(1.5.dp, SleekPrimary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("CARBON-SMART", fontSize = 8.sp, fontWeight = FontWeight.Black, color = SleekPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${res.alternativeCarbonScore} kg",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SleekPrimary
                                    )
                                    Text(
                                        text = res.alternativeMealName,
                                        fontSize = 11.sp,
                                        color = SleekOnBackground,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SleekOnMenuBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SWAP INSIGHT DETAIL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.alternativeDescription,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = SleekMutedText
                                )
                            }
                        }

                        // Ingredient segments contribution
                        val breakdownList = res.ingredientBreakdown ?: emptyList()
                        if (breakdownList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "INGREDIENTS CARBON ANALYSIS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekMutedText,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            breakdownList.forEach { ing ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ing.name,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.4f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val barPct = if (res.originalCarbonScore > 0.0) {
                                        (ing.carbonScore / res.originalCarbonScore).toFloat().coerceIn(0.05f, 1f)
                                    } else {
                                        0.1f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(SleekOnMenuBg)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(barPct)
                                                .clip(CircleShape)
                                                .background(if (ing.carbonScore > 1.2) SleekErrorRed else SleekPrimary)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${ing.carbonScore} kg",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.7f),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Scheduler selections
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                Button(
                                    onClick = { isDayExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekOnMenuBg),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(selectedDay, color = SleekOnBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "dropdown", modifier = Modifier.size(14.dp), tint = SleekPrimary)
                                }
                                DropdownMenu(
                                    expanded = isDayExpanded,
                                    onDismissRequest = { isDayExpanded = false }
                                ) {
                                    days.forEach { d ->
                                        DropdownMenuItem(
                                            text = { Text(d) },
                                            onClick = {
                                                selectedDay = d
                                                isDayExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                val mealTypes = listOf("Breakfast", "Lunch", "Dinner")
                                Button(
                                    onClick = { isTypeExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SleekOnMenuBg),
                                    shape = RoundedCornerShape(100.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text(selectedMealType, color = SleekOnBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "dropdown", modifier = Modifier.size(14.dp), tint = SleekPrimary)
                                }
                                DropdownMenu(
                                    expanded = isTypeExpanded,
                                    onDismissRequest = { isTypeExpanded = false }
                                ) {
                                    mealTypes.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                selectedMealType = t
                                                isTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onSaveRequest(selectedDay, selectedMealType, res, ingredientsInput)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_meal_button"),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Integrate into Calendar Planner", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                is EvaluationState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Compute Failures",
                            tint = SleekErrorRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            fontSize = 14.sp,
                            color = SleekErrorRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onDismiss() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

// Cheatsheet list of pantry botanical alternatives matching "SWAP CHIPS" in standard carbon footprint levels
@Composable
fun PantryCarbonSwapsTab() {
    var searchQuery by remember { mutableStateOf("") }
    val fullPantryCards = remember {
        listOf(
            PantrySwapItem("Beef Burgers", "Smoked Black Bean Patty", 6.8, 0.6, "91% savings! Simmer black beans, walnuts & liquid smoke."),
            PantrySwapItem("Chicken Cuts", "Extra-firm Herb Tofu", 3.8, 0.5, "86% savings! Light tandoori-seared tofu retains full curry gravies."),
            PantrySwapItem("Pork Sausage", "Fennel-Herbed Mushrooms", 3.2, 0.7, "78% savings! Roasted cremini mushrooms seasoned with ground wild fennel."),
            PantrySwapItem("Atlantic Salmon", "Chickpea Teriyaki flakes", 2.4, 0.5, "79% savings! Flaked organic nori seaweed & seasoned mashed chickpeas."),
            PantrySwapItem("Dairy Cheddar Chesses", "Nutritional Yeast / Cashew", 2.0, 0.4, "80% savings! Cultured cashews recreate incredible cheese sharp meltability."),
            PantrySwapItem("Heavy Butter Sauce", "Light Coconut Cream / Olive", 1.8, 0.3, "83% savings! Ground premium cashews paired with extra-virgin olive bases.")
        )
    }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            fullPantryCards
        } else {
            fullPantryCards.filter {
                it.originalItem.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT)) ||
                it.smartSwapItem.lowercase(Locale.ROOT).contains(searchQuery.lowercase(Locale.ROOT))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "CARBON COMPASS REWIND CHEATSHEET",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SleekPrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Swap Cheatsheet Guide",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = SleekOnBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search ingredients or swaps...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(100.dp),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "look up") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SleekPrimary,
                focusedLabelColor = SleekPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            items(filteredList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SleekSurface),
                    border = BorderStroke(1.dp, SleekOutline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BOTANICAL ALTERNATIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                            val footprintDrop = (((item.originalCO2 - item.swapCO2) / item.originalCO2) * 100).toInt()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(SleekEcoBetterBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "-$footprintDrop% CO₂",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SleekPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${item.originalItem} → ${item.smartSwapItem}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = SleekOnBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.explanation,
                            fontSize = 12.sp,
                            color = SleekMutedText
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Original: ${item.originalCO2}kg",
                                fontSize = 11.sp,
                                color = SleekErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Carbon-Smart Alternative: ${item.swapCO2}kg",
                                fontSize = 11.sp,
                                color = SleekPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PantrySwapItem(
    val originalItem: String,
    val smartSwapItem: String,
    val originalCO2: Double,
    val swapCO2: Double,
    val explanation: String
)

@Composable
fun SettingsTabPanel(onReset: () -> Unit) {
    val context = LocalContext.current
    var triggerMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "SYSTEM SETTINGS & ENVIRONMENT INFORMATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SleekPrimary,
            letterSpacing = 1.sp
        )
        Text(
            text = "App Configuration",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = SleekOnBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "CO₂ Carbon Intelligence Formula",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekOnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Environmental food footprints are modeled based on standard regional global estimates modeled in kg CO2 equivalent per meal portion. AI calculations leverage real Gemini rest evaluation schemas.",
                    fontSize = 12.sp,
                    color = SleekMutedText,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekOutline)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Local Storage & Data Management",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekOnBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onReset()
                        triggerMessage = "Developer Note: Meals list successfully initialized with base environmental presets."
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SleekPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Re-populate Default Meals", fontWeight = FontWeight.Bold)
                }

                if (triggerMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = triggerMessage,
                        fontSize = 11.sp,
                        color = SleekPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credits card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SleekOnMenuBg)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CarbonKitchen is powered by local Room storage algorithms and secure server-safe Gemini modeling integrations.",
                    fontSize = 11.sp,
                    color = SleekMutedText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version 1.4.0 (Sleek Material You Concept)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekPrimary
                )
            }
        }
    }
}

// Fixed constant definition representing clean unswapped labels values
val SleekOnSecondaryBg = Color(0xFFE8F5E9)

@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
