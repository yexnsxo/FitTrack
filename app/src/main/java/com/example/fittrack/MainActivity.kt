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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fittrack.ui.theme.FitTrackTheme
import com.example.fittrack.ui.theme.Main40

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    TODO("todo", "Todo", Icons.Filled.Check, ""),
    RECORD("record", "Record", Icons.Filled.CameraEnhance, "Record"),
    TIMER("timer", "Timer", Icons.Filled.AccessTime, "Timer")
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
                    modifier = Modifier.fillMaxSize(),
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
    modifier: Modifier,
    recordViewModel: RecordViewModel,
    timerViewModel: TimerViewModel,
    intentToProcess: Intent?,
    onIntentProcessed: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = Destination.TODO

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xffFFFEF4),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Header() },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.contentDescription) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            recordViewModel = recordViewModel,
            timerViewModel = timerViewModel
        )
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(120.dp)
            .background(Main40)
            .padding(horizontal = 24.dp, vertical = 0.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Text(
                text = "FitTrack",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "당신의 운동 파트너",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: Destination,
    modifier: Modifier = Modifier,
    recordViewModel: RecordViewModel,
    timerViewModel: TimerViewModel
) {
    NavHost(
        navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.TODO -> TodoScreen(
                        navController = navController,
                        recordViewModel = recordViewModel,
                        timerViewModel = timerViewModel
                    )

                    Destination.RECORD -> RecordScreen(viewModel = recordViewModel)
                    Destination.TIMER -> TimerScreen(viewModel = timerViewModel)
                }
            }
        }
    }
}
