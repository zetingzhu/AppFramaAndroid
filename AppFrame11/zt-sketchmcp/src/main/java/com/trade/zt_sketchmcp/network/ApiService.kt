package com.trade.zt_sketchmcp.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Interface defining API endpoints.
 */
interface ApiService {

    /**
     * Example API endpoint for fetching details.
     * Uses POST with form-data.
     */
    @FormUrlEncoded
    @POST("cust/bankCard/detail")
    suspend fun getBankCardDetail(
        @Field("id") id: Int,
        @Field("appssid") appssid: String = "08"
    ): Any // Replace `Any` with actual Response Data Class
}
