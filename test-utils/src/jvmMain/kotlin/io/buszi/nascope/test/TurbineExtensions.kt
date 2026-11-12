package io.buszi.nascope.test

import app.cash.turbine.Event
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

/** For flows that complete: awaits every item up to the completion event and returns them. */
suspend fun <T> ReceiveTurbine<T>.awaitItemsUntilComplete(): List<T> {
    val items = mutableListOf<T>()
    while (true) {
        when (val event = awaitEvent()) {
            is Item<T> -> items += event.value
            Event.Complete -> return items
            is Event.Error -> throw event.throwable
        }
    }
}
