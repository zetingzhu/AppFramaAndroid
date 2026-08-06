package com.trade.appframe11;


import android.content.Context;
import android.media.MediaDrm;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.android.gms.appset.AppSet;
import com.google.android.gms.appset.AppSetIdClient;
import com.google.android.gms.appset.AppSetIdInfo;
import com.google.android.gms.tasks.Task;

import java.util.UUID;

/**
 * @author: zeting
 * @date: 2026/7/21
 * <p>
 * 设备标识符获取工具类
 */
public class DeviceIdUtils {

    // Widevine DRM Scheme 的标准 UUID
    private static final UUID WIDEVINE_UUID = new UUID(0xEDEF8BA979D64ACEL, -0x773B93E12050DE46L);

    // ==========================================
    // 1. Android ID
    // ==========================================

    /**
     * 获取 Android ID
     *
     * @param context 上下文
     * @return 32位16进制字符串，获取失败返回空字符串 ""
     * @note Android 8.0+ 之后，不同签名的 App 获取到的 Android ID 是隔离且不相同的
     */
    @NonNull
    public static String getAndroidId(@NonNull Context context) {
        try {
            String androidId = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
            return androidId != null ? androidId : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    // ==========================================
    // 2. App Set ID (异步获取)
    // ==========================================

    public interface AppSetIdCallback {
        /**
         * 获取成功
         *
         * @param appSetId 36位的 UUID 字符串
         * @param scope    作用域：1 代表 SCOPE_APP (单个App), 2 代表 SCOPE_DEVELOPER (同一开发者)
         */
        void onSuccess(@NonNull String appSetId, int scope);

        /**
         * 获取失败（如缺少 Google Play Services 等原因）
         */
        void onFailure(@NonNull Exception e);
    }

    /**
     * 异步获取 App Set ID
     *
     * @param context  上下文
     * @param callback 结果回调
     * @note 依赖 Google Play Services 库: com.google.android.gms:play-services-appset
     */
    public static void getAppSetId(@NonNull Context context, final AppSetIdCallback callback) {
        try {
            AppSetIdClient client = AppSet.getClient(context.getApplicationContext());
            Task<AppSetIdInfo> task = client.getAppSetIdInfo();

            task.addOnSuccessListener(info -> {
                if (callback != null) {
                    callback.onSuccess(info.getId(), info.getScope());
                }
            }).addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e);
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    // ==========================================
    // 3. DRM ID (Widevine Unique ID)
    // ==========================================

    /**
     * 同步获取 DRM ID (Hardware Unique ID)
     *
     * @return Base64 编码后的设备硬件唯一标识，获取失败返回空字符串 ""
     * @note 硬件级全局唯一，卸载重装/恢复出厂设置都不会改变。建议在子线程中调用。
     */
    @NonNull
    public static String getDrmId() {
        if (!MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
            return "";
        }
        MediaDrm mediaDrm = null;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
            byte[] deviceUniqueId = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);

            if (deviceUniqueId != null && deviceUniqueId.length > 0) {
                // 转为 Base64 字符串
                return Base64.encodeToString(deviceUniqueId, Base64.NO_WRAP);
            }
        } catch (android.media.UnsupportedSchemeException e) {
            // 明确处理不支持该方案的异常
            e.printStackTrace();
        } catch (Exception e) {
            // 处理其他可能的异常（如模拟器环境）
            e.printStackTrace();
        } finally {
            if (mediaDrm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    mediaDrm.close();
                } else {
                    mediaDrm.release();
                }
            }
        }
        return "";
    }
}