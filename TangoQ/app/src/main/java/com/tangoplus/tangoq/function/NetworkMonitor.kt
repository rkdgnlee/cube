package com.tangoplus.tangoq.function

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NetworkMonitor(
    private val lifecycleScope: CoroutineScope,
    private val networkObserver: NetworkConnectionObserver
) {
    private var stableJob: Job? = null

    fun startObserving(onStableConnection: (Boolean) -> Unit) {
        lifecycleScope.launch {
            networkObserver.isConnected.collect { connected ->
                if (connected) {
                    stableJob?.cancel()
                    stableJob = launch {
                        delay(1500) // 1.5초 동안 연결이 유지되면
                        if (networkObserver.isConnected.value) {
                            onStableConnection(true)
                        }
                    }
                } else {
                    stableJob?.cancel()
                    onStableConnection(false)
                }
            }
        }
    }
}