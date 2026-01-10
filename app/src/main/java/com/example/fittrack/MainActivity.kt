package com.example.fittrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.fittrack.ui.theme.FitTrackTheme
import com.example.fittrack.ui.theme.Main40
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets

// BUG FIX: 빌드 오류 해결을 위해 필요한 클래스들의 주소(import)를 추가합니다.
import com.example.fittrack.RecordScreen
import com.example.fittrack.TimerScreen
import com.example.fittrack.TodoScreen
import com.example.fittrack.TimerViewModel

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    TODO("todo", "Todo", Icons.Filled.Check, ""),
    RECORD("record", "Record", Icons.Filled.CameraEnhance, "Record"),
    TIMER("timer", "Timer", Icons.Filled.AccessTime, "Timer")
}
class MainActivity : ComponentActivity() {

    private val recordViewModel: RecordViewModel by viewModels { RecordViewModelFactory(application) }
    // BUG FIX: TimerViewModel을 MainActivity에서 생성합니다.
    private val timerViewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitTrackTheme {
                // 참고: BottomNavigationBar와 MainScreen이 중복으로 호출되고 있습니다.
                // 이는 UI가 겹쳐보이는 문제를 일으킬 수 있으므로, 추후 하나를 삭제하는 것을 권장합니다.
                BottomNavigationBar(
                    modifier = Modifier.fillMaxSize(),
                    recordViewModel = recordViewModel,
                    timerViewModel = timerViewModel // BUG FIX: 생성된 timerViewModel 전달
                )
                MainScreen(
                    modifier = Modifier,
                    recordViewModel = recordViewModel,
                    timerViewModel = timerViewModel // BUG FIX: 생성된 timerViewModel 전달
                )
            }
        }
    }
}

// BUG FIX: MainScreen이 timerViewModel을 받도록 수정합니다.
@Composable
fun MainScreen(modifier: Modifier, recordViewModel: RecordViewModel, timerViewModel: TimerViewModel) {
    val navController = rememberNavController()
    val startDestination = Destination.TODO
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xffFFFEF4),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { Header() },
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(destination.route)
                            selectedDestination = index
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
            timerViewModel = timerViewModel // BUG FIX: AppNavHost에 timerViewModel 전달
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

// BUG FIX: AppNavHost가 timerViewModel을 받도록 수정합니다.
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
        modifier=modifier
    ) {
        Destination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    Destination.TODO -> TodoScreen()
                    Destination.RECORD -> RecordScreen(viewModel = recordViewModel)
                    // BUG FIX: TimerScreen에 timerViewModel을 전달합니다.
                    Destination.TIMER -> TimerScreen(viewModel = timerViewModel)
                }
            }
        }
    }
}

// BUG FIX: BottomNavigationBar가 timerViewModel을 받도록 수정합니다.
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    recordViewModel: RecordViewModel,
    timerViewModel: TimerViewModel
) {
    val navController = rememberNavController()
    val startDestination = Destination.TODO
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
                Destination.entries.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        icon = {
                            Icon(
                                destination.icon,
                                contentDescription = destination.contentDescription
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(contentPadding),
            recordViewModel = recordViewModel,
            timerViewModel = timerViewModel // BUG FIX: AppNavHost에 timerViewModel 전달
        )
    }
}
