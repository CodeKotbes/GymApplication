package com.example.gymapplication.gymUI

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gymapplication.ui.theme.SmoothMcFitTheme
import java.util.Locale

@Composable
fun GymApp(viewModel: GymViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences("gym_settings", Context.MODE_PRIVATE)
    }
    var isDarkMode by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", true))
    }

    var hasSeenOnboarding by remember {
        mutableStateOf(sharedPreferences.getBoolean("has_seen_onboarding", false))
    }

    val activeSession by viewModel.activeSession.collectAsState()
    val workoutDuration by viewModel.workoutDuration.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isOnboardingMode = currentRoute == "howToUse" && !hasSeenOnboarding

    SmoothMcFitTheme(darkTheme = isDarkMode) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!isOnboardingMode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (activeSession != null) {
                            val minutes = workoutDuration / 60
                            val seconds = workoutDuration % 60
                            val timeString =
                                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 8.dp)
                                    .clickable {
                                        navController.navigate("active_workout") {
                                            launchSingleTop = true
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = CardDefaults.cardElevation(8.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "LIVE: ${activeSession!!.name.uppercase()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Dauer: $timeString",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.triggerWorkoutSummary()

                                            navController.navigate("active_workout") {
                                                launchSingleTop = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Text("BEENDEN", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            val tabs = listOf(
                                Triple("equipment", "ÜBUNGEN", Icons.Default.FitnessCenter),
                                Triple("plans", "PLÄNE", Icons.Default.List),
                                Triple("history", "FORTSCHRITT", Icons.Default.ShowChart),
                                Triple("calendar", "KALENDER", Icons.Default.DateRange),
                                Triple("settings", "OPTIONEN", Icons.Default.Settings)
                            )

                            tabs.forEach { (route, label, icon) ->
                                NavigationBarItem(
                                    selected = currentRoute == route,
                                    onClick = {
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    alwaysShowLabel = true,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (hasSeenOnboarding) "equipment" else "howToUse",
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { 100 },
                        animationSpec = tween(300)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                        targetOffsetX = { -100 },
                        animationSpec = tween(300)
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                        initialOffsetX = { -100 },
                        animationSpec = tween(300)
                    )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                        targetOffsetX = { 100 },
                        animationSpec = tween(300)
                    )
                }
            ) {
                composable("equipment") { EquipmentScreen(viewModel) }
                composable("plans") { PlanScreen(viewModel, navController) }
                composable("history") { HistoryScreen(viewModel) }
                composable("calendar") { CalendarScreen(viewModel, navController) }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onThemeToggle = { newTheme ->
                            isDarkMode = newTheme
                            sharedPreferences.edit().putBoolean("is_dark_mode", newTheme).apply()
                        },
                        onNavigateToHowToUse = { navController.navigate("howToUse") }
                    )
                }
                composable("active_workout") {
                    ActiveWorkoutScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable("howToUse") {
                    HowToUseScreen(
                        onBack = { navController.popBackStack() },
                        isOnboarding = !hasSeenOnboarding,
                        onFinishOnboarding = {
                            sharedPreferences.edit().putBoolean("has_seen_onboarding", true).apply()
                            hasSeenOnboarding = true
                            navController.navigate("equipment") {
                                popUpTo("howToUse") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}