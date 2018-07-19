package com.xyfindables.sdk.gatt

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

fun <T> asyncBle(
        context: CoroutineContext = XYBluetoothGatt.GattThread,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        parent: Job? = null,
        block: suspend CoroutineScope.() -> XYBluetoothResult<T>
): Deferred<XYBluetoothResult<T>> {
    return async(context, start, parent, block)
}