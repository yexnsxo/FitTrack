package com.example.fittrack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.fittrack.ui.theme.FitTrackTheme
import com.example.fittrack.ui.theme.Main40

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    TODO("todo", "Todo", Icons.Filled.Check),
    RECORD("record", "Record", Icons.Filled.CameraEnhance),
    TIMER("timer", "Timer", Icons.Filled.AccessTime)
}

class MainActivity : ComponentActivity() {
    private val recordViewModel: RecordViewModel by viewModels { RecordViewModelFactory(application) }
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                MainScreen(recordViewModel, timerViewModel)
            }
        }
    }
}

@Composable
fun MainScreen(recordViewModel: RecordViewModel, timerViewModel: TimerViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(navBackStackEntry) {
        Log.d("NAV", "current route = ${navBackStackEntry?.destination?.route}")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xffFFFEF4),
        topBar = { Header() },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val currentRoute = currentDestination?.route?.substringBefore("?")
                    val selected = currentRoute == destination.route

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != destination.route) {
                                when (destination) {
                                    Destination.TODO -> {
                                        // ✅ Todo로 복귀 시 Timer 스택을 제거하여 꼬임 방지
                                        navController.popBackStack(Destination.TODO.route, inclusive = false)
                                    }
                                    else -> {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        // ✅ 4. 라우트 패턴 수정: sets 추가
        val timerPattern = "${Destination.TIMER.route}?rowId={rowId}&name={name}&target={target}&type={type}&sets={sets}"

        NavHost(
            navController = navController,
            startDestination = Destination.TODO.route,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
        ) {
            composable(Destination.TODO.route) {
                TodoScreen(navController = navController, recordViewModel = recordViewModel)
            }
            composable(Destination.RECORD.route) {
                RecordScreen(viewModel = recordViewModel)
            }
            composable(
                route = timerPattern,
                arguments = listOf(
                    navArgument("rowId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("target") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" },
                    navArgument("sets") { type = NavType.IntType; defaultValue = 0 } // ✅ sets 추가
                )
            ) { backStackEntry ->
                val rowId = backStackEntry.arguments?.getLong("rowId") ?: -1L
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val target = backStackEntry.arguments?.getInt("target") ?: 0
                val type = backStackEntry.arguments?.getString("type") ?: ""
                val sets = backStackEntry.arguments?.getInt("sets") ?: 0 // ✅ sets 추출

                LaunchedEffect(rowId) {
                    if (rowId != -1L) {
                        // ✅ ViewModel 초기화 시 세트 정보도 함께 전달
                        timerViewModel.initWorkout(rowId, name, target, type, sets)
                    }
                }

                TimerScreen(
                    viewModel = timerViewModel,
                    onFinish = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Destination.TODO.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(100.dp)
            .background(Main40)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(text = "FitTrack", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "당신의 운동 파트너", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
        }
    }
}
