package io.buszi.nascope.test

import app.cash.turbine.Event.Item
import app.cash.turbine.ReceiveTurbine

suspend fun <T> ReceiveTurbine<T>.cancelAndConsumeItems(): List<T> =
    cancelAndConsumeRemainingEvents().map { event ->
        if (event is Item<T>) {
            event.value
        } else {
            throw AssertionError("Found non-item event $event")
        }
    }
