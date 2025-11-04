package com.twinmind.voicerecorder.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.twinmind.voicerecorder.presentation.screen.DashboardScreen
import com.twinmind.voicerecorder.presentation.screen.RecordingScreen
import com.twinmind.voicerecorder.presentation.screen.SessionDetailScreen
import com.twinmind.voicerecorder.presentation.screen.SummaryScreen
import com.twinmind.voicerecorder.presentation.screen.TranscriptScreen

@Composable
fun VoiceRecorderNavigation(
    navController: NavHostController,
    onStartRecordingWithPermissions: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = NavigationRoutes.DASHBOARD
    ) {
        composable(NavigationRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToRecording = {
                    navController.navigate(NavigationRoutes.RECORDING)
                },
                onNavigateToSession = { sessionId ->
                    navController.navigate("session/$sessionId")
                }
            )
        }
        
        composable(NavigationRoutes.RECORDING) {
            RecordingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartRecording = onStartRecordingWithPermissions
            )
        }
        
        composable(NavigationRoutes.RECORDING_WITH_ID) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            RecordingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStartRecording = onStartRecordingWithPermissions
            )
        }
        
        composable(NavigationRoutes.SESSION_DETAIL) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                SessionDetailScreen(
                    sessionId = sessionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToTranscript = {
                        navController.navigate("transcript/$sessionId")
                    }
                )
            }
        }
        
        composable(NavigationRoutes.TRANSCRIPT) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                TranscriptScreen(
                    sessionId = sessionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToSummary = { sessionId ->
                        navController.navigate("summary/$sessionId")
                    }
                )
            }
        }
        
        composable(NavigationRoutes.SUMMARY) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            if (sessionId != null) {
                SummaryScreen(
                    sessionId = sessionId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}