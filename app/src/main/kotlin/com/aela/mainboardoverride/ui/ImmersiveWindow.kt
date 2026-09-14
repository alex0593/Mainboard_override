package com.aela.mainboardoverride.ui

import android.view.ViewTreeObserver
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Hides system bars while preserving Android's temporary edge-swipe navigation. */
internal fun Window.hideGameSystemBars() {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

/** Reapplies immersion when this window gains focus; returns listener cleanup. */
internal fun Window.bindGameImmersion(): () -> Unit {
    val view = decorView
    val observer = view.viewTreeObserver
    val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
        if (focused) hideGameSystemBars()
    }
    observer.addOnWindowFocusChangeListener(listener)
    val initialHide = Runnable { hideGameSystemBars() }
    view.post(initialHide)
    return {
        view.removeCallbacks(initialHide)
        if (observer.isAlive) observer.removeOnWindowFocusChangeListener(listener)
        else view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
    }
}
