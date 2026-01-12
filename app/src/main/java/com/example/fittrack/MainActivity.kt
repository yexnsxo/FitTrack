package com.example.fittrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

    private var intentToProcess by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intentToProcess = intent
        enableEdgeToEdge()
        timerViewModel.bindService(this)
        setContent {
            FitTrackTheme {
                MainScreen(
                    recordViewModel = recordViewModel,
                    timerViewModel = timerViewModel,
                    intentToProcess = intentToProcess,
                    onIntentProcessed = { intentToProcess = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentToProcess = intent
    }

    override fun onDestroy() {
        super.onDestroy()
        timerViewModel.unbindService(this)
    }
}

@Composable
fun MainScreen(
    recordViewModel: RecordViewModel,
    timerViewModel: TimerViewModel,
    intentToProcess: Intent?,
    onIntentProcessed: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val todoViewModel: TodoViewModel = viewModel(
        factory = TodoViewModelFactory(LocalContext.current.applicationContext)
    )

    LaunchedEffect(intentToProcess) {
        if (intentToProcess != null) {
            intentToProcess.getStringExtra("destination")?.let {
                navController.navigate(it) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }
            onIntentProcessed()
        }
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
                                if (destination == Destination.TIMER && timerViewModel.targetRowId.value == null) {
                                    timerViewModel.clearWorkout()
                                }
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
        val timerPattern =
            "${Destination.TIMER.route}?rowId={rowId}&name={name}&target={target}&type={type}&sets={sets}"

        NavHost(
            navController = navController,
            startDestination = Destination.TODO.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Destination.TODO.route) {
                TodoScreen(
                    vm = todoViewModel,
                    timerViewModel = timerViewModel,
                    navController = navController,
                    recordViewModel = recordViewModel
                )
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
                    navArgument("sets") { type = NavType.IntType; defaultValue = 0 }
                )
            ) { backStackEntry ->
                val rowId = backStackEntry.arguments?.getLong("rowId") ?: -1L
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val target = backStackEntry.arguments?.getInt("target") ?: 0
                val type = backStackEntry.arguments?.getString("type") ?: ""
                val sets = backStackEntry.arguments?.getInt("sets") ?: 0

                LaunchedEffect(rowId) {
                    if (rowId != -1L) {
                        timerViewModel.initWorkout(rowId, name, target, type, sets)
                    }
                }

                TimerScreen(
                    viewModel = timerViewModel,
                    todoViewModel = todoViewModel,
                    onFinish = {
                        timerViewModel.clearWorkout() // ✅ 완료 후 초기화
                        navController.popBackStack(Destination.TODO.route, inclusive = false)
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
            Text(
                text = "FitTrack",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "당신의 운동 파트너",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}
