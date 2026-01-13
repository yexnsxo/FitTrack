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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.time.LocalDate

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
        containerColor = Color(0xffF5F5F5),
        topBar = { Header() },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Destination.entries.forEach { destination ->
                    val currentRoute = currentDestination?.route?.substringBefore("?")
                    val selected = currentRoute == destination.route

                    NavigationBarItem(
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Main40, // 선택되었을 때 아이콘 색상
                            unselectedIconColor = Color.Gray, // 선택되지 않았을 때 아이콘 색상
                            selectedTextColor = Main40,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.White
                        ),
                        onClick = {
                            if (currentRoute == destination.route) return@NavigationBarItem

                            when (destination) {
                                Destination.TODO -> {
                                    // ✅ Timer(쿼리 포함)에서 Todo로 갈 때는 "navigate"가 아니라 "pop"이 안전함
                                    val popped = navController.popBackStack(Destination.TODO.route, inclusive = false)

                                    // ✅ 혹시 Todo가 스택에 없으면(예: intent로 timer부터 온 경우) navigate로 보정
                                    if (!popped) {
                                        navController.navigate(Destination.TODO.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }

                                Destination.TIMER -> {
                                    // ✅ 타이머 탭을 "직접" 눌러 들어갈 때만 초기화(원래 로직 유지)
                                    if (timerViewModel.targetRowId.value == null) {
                                        timerViewModel.clearWorkout()
                                    }
                                    navController.navigate(Destination.TIMER.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label, modifier = Modifier.offset(y = (-3).dp)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val timerPattern =
            "${Destination.TIMER.route}?rowId={rowId}&name={name}&target={target}&type={type}&sets={sets}&setReps={setReps}&setWeights={setWeights}"

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
                RecordScreen(viewModel = recordViewModel, navController = navController)
            }
            composable(
                "editRecord/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val dateStr = backStackEntry.arguments?.getString("date")
                if (dateStr != null) {
                    val date = LocalDate.parse(dateStr)
                    EditRecordScreen(date = date, navController = navController)
                }
            }
            composable(
                route = timerPattern,
                arguments = listOf(
                    navArgument("rowId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("target") { type = NavType.StringType; defaultValue = "" },
                    navArgument("type") { type = NavType.StringType; defaultValue = "" },
                    navArgument("sets") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("setReps") { type = NavType.StringType; defaultValue = "" },
                    navArgument("setWeights") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val rowId = backStackEntry.arguments?.getLong("rowId") ?: -1L
                val name = backStackEntry.arguments?.getString("name") ?: ""
                val target = backStackEntry.arguments?.getString("target") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: ""
                val sets = backStackEntry.arguments?.getInt("sets") ?: 0
                val setReps = backStackEntry.arguments?.getString("setReps") ?: ""
                val setWeights = backStackEntry.arguments?.getString("setWeights") ?: ""

                LaunchedEffect(rowId) {
                    if (rowId != -1L) {
                        timerViewModel.initWorkout(rowId, name, target, type, sets, setReps, setWeights)
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
            .height(120.dp)
            .background(Main40)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.header_logo), // 여기에 본인이 저장한 파일 이름 입력
                contentDescription = "App Logo",
                modifier = Modifier
                    .height(60.dp) // 이미지 높이 조절
            )
            Text(
                text = "오늘도 득근하세요!",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}
