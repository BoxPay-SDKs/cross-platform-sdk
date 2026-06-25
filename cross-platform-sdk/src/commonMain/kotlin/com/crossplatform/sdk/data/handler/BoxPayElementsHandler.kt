package com.crossplatform.sdk.data.handler

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BoxPayElementsHandler {

    private val _isPayable = MutableStateFlow(false)
    val isPayable: StateFlow<Boolean> = _isPayable        // Kotlin/Compose callers

    // Java-friendly listener (SAM interface → no Unit.INSTANCE in Java)
    private var payableListener: PayableListener? = null

    fun setOnPayableChanged(listener: PayableListener) {
        payableListener = listener
        listener.onChanged(_isPayable.value)              // emit current state now
    }

    // merchant → SDK
    fun pay() { onSubmit?.invoke() }

    // ── internal wiring set by the SDK ──
    internal var onSubmit: (() -> Unit)? = null
    internal fun setPayable(value: Boolean) {
        _isPayable.value = value
        payableListener?.onChanged(value)
    }

    fun interface PayableListener {
        fun onChanged(payable: Boolean)
    }
}