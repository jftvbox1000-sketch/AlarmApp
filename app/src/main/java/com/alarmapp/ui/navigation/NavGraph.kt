package com.alarmapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alarmapp.ui.alarmeditor.AlarmEditorScreen
import com.alarmapp.ui.alarmlist.AlarmListScreen
import com.alarmapp.ui.debug.DebugScreen
import com.alarmapp.ui.holiday.HolidayScreen

object Routes {
    const val ALARM_LIST = "alarm_list"
    const val ALARM_EDITOR = "alarm_editor?alarmId={alarmId}"
    const val HOLIDAY_MANAGER = "holiday_manager"
    const val DEBUG = "debug"

    fun alarmEditor(alarmId: Long? = null) =
        if (alarmId != null) "alarm_editor?alarmId=$alarmId" else "alarm_editor"
}

@Composable
fun AlarmNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.ALARM_LIST) {
        composable(Routes.ALARM_LIST) {
            AlarmListScreen(
                onAddAlarm = { navController.navigate(Routes.alarmEditor()) },
                onEditAlarm = { id -> navController.navigate(Routes.alarmEditor(id)) },
                onManageHolidays = { navController.navigate(Routes.HOLIDAY_MANAGER) },
                onDebug = { navController.navigate(Routes.DEBUG) }
            )
        }
        composable(
            route = Routes.ALARM_EDITOR,
            arguments = listOf(navArgument("alarmId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: -1L
            AlarmEditorScreen(
                alarmId = if (alarmId == -1L) null else alarmId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.HOLIDAY_MANAGER) {
            HolidayScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.DEBUG) {
            DebugScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
