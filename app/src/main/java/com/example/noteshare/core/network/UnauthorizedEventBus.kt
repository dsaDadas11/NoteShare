package com.example.noteshare.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 全局 401 事件总线 */
@Singleton
class UnauthorizedEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    fun emit() { _events.tryEmit(Unit) }
}
