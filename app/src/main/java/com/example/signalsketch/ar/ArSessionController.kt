package com.example.signalsketch.ar

import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import kotlinx.coroutines.flow.StateFlow

interface ArSessionController {
    val sessionState: StateFlow<ArSessionState>

    fun onSessionCreated()

    fun onSessionResumed()

    fun onSessionPaused()

    fun onSessionFailed(message: String?)

    fun onFrameUpdated(frame: Frame)

    fun placeAnchor(frame: Frame, motionEvent: MotionEvent): Anchor?

    fun reset()
}
