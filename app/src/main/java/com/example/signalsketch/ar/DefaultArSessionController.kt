package com.example.signalsketch.ar

import android.view.MotionEvent
import com.example.signalsketch.position.TrackingQuality
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2

class DefaultArSessionController : ArSessionController {
    private val _sessionState = MutableStateFlow(ArSessionState())
    override val sessionState: StateFlow<ArSessionState> = _sessionState.asStateFlow()

    override fun onSessionCreated() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.CREATED,
            trackingStatus = "AR session created.",
            lastErrorMessage = null
        )
    }

    override fun onSessionResumed() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.RESUMED,
            trackingStatus = "AR session resumed.",
            lastErrorMessage = null
        )
    }

    override fun onSessionPaused() {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.PAUSED,
            trackingQuality = TrackingQuality.UNAVAILABLE,
            trackingStatus = "AR session paused."
        )
    }

    override fun onSessionFailed(message: String?) {
        _sessionState.value = _sessionState.value.copy(
            lifecycleState = ArSessionLifecycleState.FAILED,
            trackingQuality = TrackingQuality.UNAVAILABLE,
            trackingStatus = message ?: "AR session failed.",
            lastErrorMessage = message
        )
    }

    override fun onFrameUpdated(frame: Frame) {
        val horizontalPlaneFound = frame.getUpdatedTrackables(Plane::class.java)
            .any { plane ->
                plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING &&
                    plane.trackingState == TrackingState.TRACKING
            }

        val camera = frame.camera
        val pose = camera.pose
        val trackingQuality = when (camera.trackingState) {
            TrackingState.TRACKING -> TrackingQuality.GOOD
            TrackingState.PAUSED -> TrackingQuality.LIMITED
            TrackingState.STOPPED -> TrackingQuality.UNAVAILABLE
        }
        val trackingStatus = when (camera.trackingState) {
            TrackingState.TRACKING -> "AR camera tracking is active."
            TrackingState.PAUSED -> camera.trackingFailureReason.toStatusMessage()
            TrackingState.STOPPED -> "AR camera tracking stopped."
        }

        _sessionState.value = _sessionState.value.copy(
            hasDetectedHorizontalPlane = _sessionState.value.hasDetectedHorizontalPlane || horizontalPlaneFound,
            xMeters = pose.tx(),
            yMeters = pose.tz(),
            headingDegrees = pose.toHeadingDegrees(),
            trackingQuality = trackingQuality,
            trackingStatus = trackingStatus
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

    private fun com.google.ar.core.Pose.toHeadingDegrees(): Float {
        val qx = qx()
        val qy = qy()
        val qz = qz()
        val qw = qw()
        val yawRadians = atan2(
            2f * (qw * qy + qx * qz),
            1f - 2f * (qy * qy + qz * qz)
        )
        var heading = Math.toDegrees(yawRadians.toDouble()).toFloat()
        if (heading < 0f) heading += 360f
        return heading
    }

    private fun TrackingFailureReason.toStatusMessage(): String {
        return when (this) {
            TrackingFailureReason.NONE -> "AR camera is not currently tracking."
            TrackingFailureReason.BAD_STATE -> "AR tracking is temporarily unavailable."
            TrackingFailureReason.INSUFFICIENT_LIGHT -> "AR tracking needs more light."
            TrackingFailureReason.EXCESSIVE_MOTION -> "AR tracking is degraded by fast motion."
            TrackingFailureReason.INSUFFICIENT_FEATURES -> "AR tracking needs more visible features."
            TrackingFailureReason.CAMERA_UNAVAILABLE -> "AR camera is unavailable."
        }
    }
}
