package com.help.Abhayada

import android.content.Context
import android.gesture.Gesture
import android.gesture.GestureLibraries
import android.gesture.GestureLibrary
import android.gesture.GestureStroke
import android.graphics.Path
import java.io.File

object GestureLibraryHelper {
    fun createAGesture(context: Context) {
        val gestureLibrary: GestureLibrary = GestureLibraries.fromFile(File(context.filesDir, "gestures"))
        val path = Path().apply {
            moveTo(50f, 50f)
            lineTo(100f, 150f)
            lineTo(150f, 50f)
            moveTo(75f, 100f)
            lineTo(125f, 100f)
        }
        val gesture = Gesture().apply {
            addStroke(GestureStroke(listOf(path)))
        }
        gestureLibrary.addGesture("A", gesture)
        gestureLibrary.save()
    }
}