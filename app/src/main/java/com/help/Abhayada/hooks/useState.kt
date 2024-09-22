package com.help.Abhayada.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue


@Composable
fun <T>useState(item: T): Pair<T, (T) -> Unit> {
    var state by rememberSaveable { mutableStateOf(item) }

    val setState = {
        x:T -> state = x
    }

    return Pair(state, setState)
}