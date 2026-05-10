package com.sunflower.timetracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunflower.timetracker.presentation.navigation.Screen
import com.sunflower.timetracker.presentation.screens.analysis.AnalysisScreen
import com.sunflower.timetracker.presentation.screens.home.HomeScreen
import com.sunflower.timetracker.presentation.screens.tags.TagsScreen
import com.sunflower.timetracker.presentation.theme.AccentGreen
import com.sunflower.timetracker.presentation.theme.Background
import com.sunflower.timetracker.presentation.theme.Outline
import com.sunflower.timetracker.presentation.theme.Primary
import com.sunflower.timetracker.presentation.theme.Surface
import com.sunflower.timetracker.presentation.theme.TextTertiary
import com.sunflower.timetracker.presentation.theme.TimeTrackerTheme
import com.sunflower.timetracker.presentation.viewmodel.AnalysisViewModel
import com.sunflower.timetracker.presentation.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TimeTrackerTheme {
                TimeTrackerAppScreen()
            }
        }
    }
}

data class NavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val navItems = listOf(
    NavItem(Screen.Home,     "Timer",    Icons.Filled.Timer,    Icons.Outlined.Timer),
    NavItem(Screen.Analysis, "Analysis", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    NavItem(Screen.Tags,     "Tags", Icons.AutoMirrored.Filled.Label,
        Icons.AutoMirrored.Outlined.Label
    )
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimeTrackerAppScreen() {
    val navController   = rememberNavController()
    val homeVm: HomeViewModel     = hiltViewModel()
    val analysisVm: AnalysisViewModel = hiltViewModel()
    val active by homeVm.activeState.collectAsState()

    Scaffold(
        containerColor = Background,
        bottomBar = {
            BottomNav(navController = navController, hasActiveSession = active.session != null)
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route)     { HomeScreen(vm = homeVm) }
            composable(Screen.Analysis.route) { AnalysisScreen(vm = analysisVm) }
            composable(Screen.Tags.route)     { TagsScreen(vm = homeVm) }
        }
    }
}

@Composable
private fun BottomNav(navController: androidx.navigation.NavController, hasActiveSession: Boolean) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentDest  = navBackStack?.destination

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface)
                .border(1.dp, Outline, RoundedCornerShape(20.dp))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = currentDest?.hierarchy?.any { it.route == item.screen.route } == true
                NavBarItem(
                    item      = item,
                    selected  = selected,
                    badge     = item.screen == Screen.Home && hasActiveSession,
                    onClick   = {
                        navController.navigate(item.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    selected: Boolean,
    badge: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        BadgedBox(
            badge = {
                if (badge) Badge(
                    containerColor = AccentGreen,
                    modifier       = Modifier.size(8.dp)
                ) {}
            }
        ) {
            Icon(
                imageVector      = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint             = if (selected) Primary else TextTertiary,
                modifier         = Modifier.size(24.dp)
            )
        }
        Text(
            text       = item.label,
            color      = if (selected) Primary else TextTertiary,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize   = 10.sp
        )
    }
}