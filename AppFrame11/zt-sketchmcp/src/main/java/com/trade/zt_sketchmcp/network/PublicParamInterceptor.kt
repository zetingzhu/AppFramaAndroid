package com.trade.zt_sketchmcp.network

import com.trade.zt_sketchmcp.BuildConfig
import com.trade.zt_sketchmcp.utils.GlobalHelper
import com.trade.zt_sketchmcp.utils.SPUtils
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor for adding public headers and request parameters.
 * Handles both URL query parameters and rewriting Form-data (if it's a FormBody POST).
 */
class PublicParamInterceptor : Interceptor {

    private fun isLoginDo(yes: String, no: String): String {
        val isLoggedIn = SPUtils.get(SPUtils.USER_TOKEN, "").isNotEmpty()
        return if (isLoggedIn) yes else no
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // 1. Add Public Headers
        val requestBuilder = originalRequest.newBuilder()
            .header("v-flag", isLoginDo(yes = "false", no = "true"))
            .header("channel", "googleplay") // 默认固定传googleplay
            .header("appId", "13") // 多少包传多少
            .header("versionName", BuildConfig.VERSION_NAME)
            .header("versionCode", BuildConfig.VERSION_CODE.toString())
            .header("apiName", originalRequest.url.encodedPath) // AES加密path简写
            .header("mulFlag", "1")
            .header("token", SPUtils.get(SPUtils.USER_TOKEN, ""))
            .header("client-id", "13")
            .header("appssid", "13")
            .header("userId", SPUtils.get(SPUtils.CUR_UID, SPUtils.get(SPUtils.USER_ID, "")))
            .header("currentUserId", SPUtils.get(SPUtils.CUR_UID, SPUtils.get(SPUtils.USER_ID, "")))

        // 2. Add Public Parameters (URL Query parameters)
        val originalUrl = originalRequest.url
        val urlBuilder = originalUrl.newBuilder()
            .addQueryParameter("language", "es")
            .addQueryParameter("versionName", BuildConfig.VERSION_NAME)
            .addQueryParameter("versionCode", BuildConfig.VERSION_CODE.toString())
            .addQueryParameter("googleGaid", GlobalHelper.getGaid())
            .addQueryParameter("lbs", "0,0")
            .addQueryParameter("userId", SPUtils.get(SPUtils.CUR_UID, SPUtils.get(SPUtils.USER_ID, "")))
        
        val modifiedUrl = urlBuilder.build()
        requestBuilder.url(modifiedUrl)

        // 3. (Optional) If POST request and body is FormBody, inject Form-Data parameters.
        val originalBody = originalRequest.body
        if (originalRequest.method == "POST" && originalBody is FormBody) {
            val formBodyBuilder = FormBody.Builder()
            // Copy existing parameters
            for (i in 0 until originalBody.size) {
                formBodyBuilder.addEncoded(originalBody.encodedName(i), originalBody.encodedValue(i))
            }
            // Add public Form-Data parameters
            formBodyBuilder.add("language", "es")
            formBodyBuilder.add("versionName", BuildConfig.VERSION_NAME)
            formBodyBuilder.add("versionCode", BuildConfig.VERSION_CODE.toString())
            formBodyBuilder.add("googleGaid", GlobalHelper.getGaid())
            formBodyBuilder.add("lbs", "0,0")
            formBodyBuilder.add("userId", SPUtils.get(SPUtils.CUR_UID, SPUtils.get(SPUtils.USER_ID, "")))
            
            requestBuilder.post(formBodyBuilder.build())
        }

        val finalRequest = requestBuilder.build()
        return chain.proceed(finalRequest)
    }
}
