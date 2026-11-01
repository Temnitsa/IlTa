package com.iltatrainer.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.iltatrainer.app.domain.SessionConfig
import com.iltatrainer.app.domain.SessionResult

/** Lightweight in-memory navigation stack: no navigation-compose dependency needed. */
sealed interface Screen {
    data object Topics : Screen
    data class Section(val sectionId: String) : Screen
    data class RuleDetail(val ruleId: String) : Screen
    data class Practice(val config: SessionConfig, val title: String) : Screen
    data class Result(val result: SessionResult, val title: String) : Screen
    data object Stats : Screen
    data object Settings : Screen
}

class AppNavigator(initial: Screen = Screen.Topics) {
    var current by mutableStateOf(initial)
        private set

    private val stack = ArrayDeque<Screen>().apply { add(initial) }

    fun push(screen: Screen) {
        stack.addLast(screen)
        current = screen
    }

    /** Replaces the top screen (e.g. practice → result so back after result returns home). */
    fun replace(screen: Screen) {
        if (stack.isNotEmpty()) stack.removeLast()
        push(screen)
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeLast()
        current = stack.last()
        return true
    }

    fun popToTab(tab: Screen) {
        while (stack.size > 1 && stack.last() != tab) stack.removeLast()
        current = stack.last()
    }

    fun resetTo(screen: Screen) {
        stack.clear()
        stack.addLast(screen)
        current = screen
    }

    val canGoBack: Boolean get() = stack.size > 1
}
