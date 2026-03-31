package com.trade.zt_sketchmcp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trade.zt_sketchmcp.network.ApiService
import com.trade.zt_sketchmcp.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * States for Network Requests
 */
sealed class NetworkState<out T> {
    object Idle : NetworkState<Nothing>()
    object Loading : NetworkState<Nothing>()
    data class Success<out T>(val data: T) : NetworkState<T>()
    data class Error(val exception: Throwable) : NetworkState<Nothing>()
}

class CoverViewModel : ViewModel() {

    // Instantiating ApiService
    private val apiService = RetrofitClient.createService(ApiService::class.java)

    // StateFlow to expose state to CoverActivity
    private val _bankCardState = MutableStateFlow<NetworkState<Any>>(NetworkState.Idle)
    val bankCardState: StateFlow<NetworkState<Any>> = _bankCardState

    fun fetchBankCardDetail(id: Int, appssid: String = "08") {
        viewModelScope.launch {
            _bankCardState.value = NetworkState.Loading
            try {
                // Suspends until network returns result
                val response = apiService.getBankCardDetail(id, appssid)
                Log.d("CoverViewModel", "API Success: $response")
                _bankCardState.value = NetworkState.Success(response)
            } catch (e: Exception) {
                Log.e("CoverViewModel", "API Error", e)
                _bankCardState.value = NetworkState.Error(e)
            }
        }
    }
}
