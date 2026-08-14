package com.arunrk.simplenote.testsupport

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base for ViewModel tests.
 *
 * `viewModelScope` dispatches on `Dispatchers.Main`, so the test replaces it with a
 * [StandardTestDispatcher]. Tests then run with `runTest(testDispatcher)` so the ViewModel's
 * coroutines and the test body share one virtual clock — without that, `advanceTimeBy` would
 * move the test's clock while the ViewModel's debounce sat on a different one.
 *
 * Standard rather than Unconfined on purpose: work queued by the ViewModel does not run until
 * the test explicitly advances, which is what lets a test observe the optimistic state after
 * an intent but before the network call resolves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelTest {

    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun installMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun removeMainDispatcher() {
        Dispatchers.resetMain()
    }
}

/**
 * Starts recording a ViewModel's one-shot effects.
 *
 * Two details make this work, and both are easy to get wrong:
 *
 * 1. The collector runs in `backgroundScope`. An effects flow never completes, so collecting
 *    it in the test scope itself would leave `runTest` waiting on it forever.
 * 2. Reading through [EffectRecorder] calls `runCurrent()` first. `advanceUntilIdle()`
 *    deliberately *stops* advancing once only `backgroundScope` coroutines are left, so a
 *    pending delivery to this collector would otherwise never be dispatched and every
 *    assertion would see an empty list.
 */
fun <T> TestScope.recordEffects(effects: Flow<T>): EffectRecorder<T> {
    val collected = mutableListOf<T>()
    // UNDISPATCHED so the collector is subscribed before this function returns, rather than
    // sitting in the queue while the test fires the very intents it means to observe.
    backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
        effects.collect { collected += it }
    }
    return EffectRecorder(this, collected)
}

class EffectRecorder<T> internal constructor(
    private val scope: TestScope,
    private val collected: MutableList<T>,
) {
    /** Everything emitted so far, after draining whatever is pending. */
    val all: List<T>
        get() {
            scope.runCurrent()
            return collected.toList()
        }

    val last: T get() = all.last()

}

/**
 * Convenience for asserting on one kind of effect among several.
 *
 * The receiver is star-projected so a call site only has to name the effect type it wants,
 * rather than spelling out both type arguments.
 */
inline fun <reified R> EffectRecorder<*>.ofType(): List<R> = all.filterIsInstance<R>()
