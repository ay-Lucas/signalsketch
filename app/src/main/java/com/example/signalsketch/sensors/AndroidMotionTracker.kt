package com.example.signalsketch.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidMotionTracker(
    context: Context
) : MotionTracker {
    private val sensorManager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    override val motionSamples: Flow<MotionSample> = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(
                    MotionSample(
                        sensorType = event.sensor.type,
                        values = event.values.toList(),
                        timestampNanos = event.timestamp
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val accelerometerRegistered = accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false
        val gyroscopeRegistered = gyroscope?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false

        if (!accelerometerRegistered && !gyroscopeRegistered) {
            close()
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    override fun startTracking(): Boolean {
        return accelerometer != null && gyroscope != null
    }

    override fun stopTracking() = Unit
}
