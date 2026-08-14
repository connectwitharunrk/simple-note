package com.arunrk.simplenote.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Base for the MVI stores in this app.
 *
 * The split between [state] and [effects] is the important part:
 *
 * - **State** is everything currently true about the screen. It is durable and replayable —
 *   a new collector after a rotation or a window resize gets the current value and renders
 *   exactly what was there before.
 * - **Effects** are things that happen *once*: a snackbar, a navigation, dismissing the
 *   keyboard. Delivered through a [Channel] so each is consumed exactly once. Modelling a
 *   snackbar as state is the classic MVI bug — it reappears on every recomposition until
 *   something explicitly clears it.
 *
 * Intents are the only way in. The UI never mutates state; it describes what the user did and
 * the store decides what that means.
 */
abstract class MviViewModel<Intent : Any, State : Any, Effect : Any>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * BUFFERED rather than unlimited or conflated: effects emitted before the UI subscribes
     * are held rather than dropped, and none is silently replaced by a later one.
     */
    private val _effects = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = _effects.receiveAsFlow()

    protected val currentState: State get() = _state.value

    /** The single entry point for everything the user can do on this screen. */
    abstract fun onIntent(intent: Intent)

    /** Atomic read-modify-write, so concurrent coroutines cannot lose an update. */
    protected fun updateState(reduce: State.() -> State) {
        _state.update(reduce)
    }

    protected fun emitEffect(effect: Effect) {
        _effects.trySend(effect)
    }
}
