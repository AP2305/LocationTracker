package com.example.locationtracker

import android.animation.*
import android.annotation.TargetApi
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.Property
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

import java.util.ArrayList

object MarkerAnimation {


    private var count = 0

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    internal fun animateMarkerToICS(
        marker: Marker,
        finalPosition: LatLng,
        latLngInterpolator: LatLngInterpolator
    ) {

        val typeEvaluator = TypeEvaluator<LatLng> { fraction, startValue, endValue ->
            latLngInterpolator.interpolate(
                fraction,
                startValue,
                endValue
            )
        }
        val property = Property.of(Marker::class.java, LatLng::class.java, "position")
        var animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition)
        animator!!.duration = 3000
        animator!!.start()

    }

    }
