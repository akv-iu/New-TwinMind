package com.twinmind.voicerecorder.presentation.navigation

object NavigationRoutes {
    const val DASHBOARD = "dashboard"
    const val RECORDING = "recording"
    const val RECORDING_WITH_ID = "recording/{sessionId}"
    const val SESSION_DETAIL = "session/{sessionId}"
    const val SUMMARY = "summary/{sessionId}"
}

sealed class Screen(val route: String) {
    object Dashboard : Screen(NavigationRoutes.DASHBOARD)
    object Recording : Screen(NavigationRoutes.RECORDING)
    data class RecordingWithId(val sessionId: String) : Screen("recording/$sessionId")
    data class SessionDetail(val sessionId: String) : Screen("session/$sessionId")
    data class Summary(val sessionId: String) : Screen("summary/$sessionId")
}