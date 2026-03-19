package com.example.signalsketch.ar

import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultArSessionController : ArSessionController {
    private val _sessionState = MutableStateFlow(ArSessionState())
    override val sessionState: StateFlow<ArSessionState> = _sessionState.asStateFlow()

    override fun onSessionCreated() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.CREATED,
            lastErrorMessage = null
        )
    }

    override fun onSessionResumed() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.RESUMED,
            lastErrorMessage = null
        )
    }

    override fun onSessionPaused() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.PAUSED
        )
    }

    override fun onSessionFailed(message: String?) {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.FAILED,
            lastErrorMessage = message
        )
    }

    override fun onFrameUpdated(frame: Frame) {
        val horizontalPlaneFound = frame.getUpdatedTrackables(Plane::class.java)
            .any { plane ->
                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    plane.trackingState == TrackingState.TRACKING
            }

        _sessionState.value = _sessionState.value.copy(
            hasDetectedHorizontalPlane = _sessionState.value.hasDetectedHorizontalPlane || horizontalPlaneFound
        )
    }

    override fun placeAnchor(frame: Frame, motionEvent: MotionEvent): Anchor? {
        val hit = frame.hitTest(motionEvent)
            .firstOrNull { hitResult ->
                val trackable = hitResult.trackable
                trackable is Plane &&
                    trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    trackable.isPoseInPolygon(hitResult.hitPose)
            }
            ?: return null

        val anchor = hit.createAnchor()
        _sessionState.value = _sessionState.value.copy(
            anchorCount = _sessionState.value.anchorCount + 1,
            lastErrorMessage = null
        )
        return anchor
    }

    override fun reset() {
        _sessionState.value = ArSessionState()
    }
}
