package com.imsi.imei.utils

import com.imsi.imei.utils.SecureDataUploader
import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 数据传输工具类
 * 实现设备信息的加密传输
 */
class DataTransmitter {
    companion object {
        private const val TAG = "DataTransmitter"
        // 真实设备将IP地址10.0.2.2替换为实际的服务器IP地址
        // private const val SERVER_URL = "http://10.0.2.2:8080/api"
        private const val SERVER_URL = "http://192.168.10.12:8081/api"
        private const val PUBLIC_KEY_ENDPOINT = "$SERVER_URL/configured-public-key"
        private const val DATA_UPLOAD_ENDPOINT = "$SERVER_URL/upload"

        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        private val gson = Gson()
        private val JSON = "application/json; charset=utf-8".toMediaType()

        /**
         * 获取服务器RSA公钥
         */
        private fun fetchPublicKey(): String? {
            try {
                val request = Request.Builder()
                    .url(PUBLIC_KEY_ENDPOINT)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("获取公钥失败: ${response.code}")
                    val publicKey = response.body?.string()
                    Log.d(TAG, "成功获取公钥，长度: ${publicKey?.length ?: 0}")
                    return publicKey ?: throw IOException("响应体为空")
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取公钥失败: ${e.message}", e)
                return null
            }
        }

        /**
         * 发送加密的设备信息到服务器
         */
        fun sendDeviceInfo(context: Context): Boolean {
            try {
                Log.d(TAG, "开始发送设备信息...")

                // 获取RSA公钥
                val publicKey = fetchPublicKey() ?: return false

                // 获取设备信息
                val deviceInfoManager = ga.mdm.DeviceInfoManager.getInstance()
                val deviceInfoArray = deviceInfoManager.getDeviceInfo()

                if (deviceInfoArray == null) {
                    Log.e(TAG, "设备信息获取失败")
                    return false
                }

                // 构建设备信息JSON
                val deviceInfo = JSONObject()

                // 添加硬件信息
                val hardwareJson = JSONObject()
                hardwareJson.put("设备名称", if (deviceInfoArray.size > 4) deviceInfoArray[4]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("设备型号", if (deviceInfoArray.size > 5) deviceInfoArray[5]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("硬件序列号", if (deviceInfoArray.size > 9) deviceInfoArray[9]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("IMEI主卡", if (deviceInfoArray.size > 0) deviceInfoArray[0]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("IMEI副卡", if (deviceInfoArray.size > 1) deviceInfoArray[1]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("CPU架构", if (deviceInfoArray.size > 15) deviceInfoArray[15]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("内存大小", if (deviceInfoArray.size > 2) deviceInfoArray[2]?.toString() ?: "未收集到信息" else "未收集到信息")
                hardwareJson.put("基带版本", if (deviceInfoArray.size > 7) deviceInfoArray[7]?.toString() ?: "未收集到信息" else "未收集到信息")
                deviceInfo.put("硬件信息", hardwareJson)

                // 添加软件信息
                val softwareJson = JSONObject()
                softwareJson.put("操作系统名称", if (deviceInfoArray.size > 6) deviceInfoArray[6]?.toString() ?: "未收集到信息" else "未收集到信息")
                softwareJson.put("操作系统版本", if (deviceInfoArray.size > 8) deviceInfoArray[8]?.toString() ?: "未收集到信息" else "未收集到信息")
                softwareJson.put("Android_ID", if (deviceInfoArray.size > 16) deviceInfoArray[16]?.toString() ?: "未收集到信息" else "未收集到信息")
                softwareJson.put("内核版本", if (deviceInfoArray.size > 7) deviceInfoArray[7]?.toString() ?: "未收集到信息" else "未收集到信息")
                deviceInfo.put("软件信息", softwareJson)

                // 添加SIM卡信息
                val simJson = JSONObject()
                simJson.put("卡1手机号", if (deviceInfoArray.size > 17) deviceInfoArray[17]?.toString() ?: "未收集到信息" else "未收集到信息")
                simJson.put("卡2手机号", if (deviceInfoArray.size > 18) deviceInfoArray[18]?.toString() ?: "未收集到信息" else "未收集到信息")
                simJson.put("卡1IMSI", if (deviceInfoArray.size > 13) deviceInfoArray[13]?.toString() ?: "未收集到信息" else "未收集到信息")
                simJson.put("卡2IMSI", if (deviceInfoArray.size > 14) deviceInfoArray[14]?.toString() ?: "未收集到信息" else "未收集到信息")
                simJson.put("卡1ICCID", if (deviceInfoArray.size > 11) deviceInfoArray[11]?.toString() ?: "未收集到信息" else "未收集到信息")
                simJson.put("卡2ICCID", if (deviceInfoArray.size > 12) deviceInfoArray[12]?.toString() ?: "未收集到信息" else "未收集到信息")
                deviceInfo.put("SIM卡信息", simJson)

                // 添加时间戳
                deviceInfo.put("采集时间", System.currentTimeMillis())

                // 加密数据
                val deviceInfoJson = deviceInfo.toString()
                Log.d(TAG, "原始设备信息JSON: $deviceInfoJson")

                val plainJson = deviceInfo
                SecureDataUploader.uploadData(
                    DATA_UPLOAD_ENDPOINT,
                    plainJson,
                    publicKey
                ) { success ->
                    if (success) {
                        Log.d(TAG, "设备信息上传成功")
                    } else {
                        Log.e(TAG, "设备信息上传失败")
                    }
                }
                return true
            } catch (e: Exception) {
                Log.e(TAG, "上传设备信息失败: ${e.message}", e)
                return false
            }
        }
    }
}