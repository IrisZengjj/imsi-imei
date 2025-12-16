package com.imsi.imei.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.imsi.imei.R
import com.imsi.imei.utils.DataTransmitter
import com.imsi.imei.utils.DeviceInfoJsonExporter
import ga.mdm.DeviceInfoManager
import org.json.JSONObject
import java.io.File

class MainActivity : Activity(), View.OnClickListener {

    // UI 元素
    private var btnHardwareInfo: Button? = null
    private var btnSoftwareInfo: Button? = null
    private var btnSimInfo: Button? = null
    private var tvPhoneNumber: TextView? = null

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 在应用启动时导出设备信息到JSON文件
        exportDeviceInfoToJson()
        // 在应用启动时触发加密上传
        uploadDataToServer()

        // 初始化按钮和TextView
        btnHardwareInfo = findViewById(R.id.btn_hardware_info)
        btnSoftwareInfo = findViewById(R.id.btn_software_info)
        btnSimInfo = findViewById(R.id.btn_sim_info)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)

        // 设置点击事件监听器
        btnHardwareInfo?.setOnClickListener(this)
        btnSoftwareInfo?.setOnClickListener(this)
        btnSimInfo?.setOnClickListener(this)

        // 展示手机号信息
        displayTelephoneInfo()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_hardware_info -> {
                // 跳转到设备属性页面
                val intent = Intent(this, HardwareInfoActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_software_info -> {
                // 跳转到软件信息页面
                val intent = Intent(this, SoftwareInfoActivity::class.java)
                startActivity(intent)
            }
            R.id.btn_sim_info -> {
                // 跳转到SIM卡信息页面
                val intent = Intent(this, SimInfoActivity::class.java)
                startActivity(intent)
            }
        }
    }

    /**
     * 展示手机号信息
     */
    private fun displayTelephoneInfo() {
        try {
            val deviceInfoManager = DeviceInfoManager.getInstance()
            val deviceInfo = deviceInfoManager.getDeviceInfo()

            val phoneNumber1 = if (deviceInfo != null && deviceInfo.size > 17) deviceInfo[17]?.toString() ?: "N/A" else "N/A"
            val phoneNumber2 = if (deviceInfo != null && deviceInfo.size > 18) deviceInfo[18]?.toString() ?: "N/A" else "N/A"
            val phoneNumberText = "手机号\n卡1：$phoneNumber1\n卡2：$phoneNumber2"

            tvPhoneNumber?.text = phoneNumberText
        } catch (e: Exception) {
            Log.e(TAG, "无法获取或展示设备信息", e)
            Toast.makeText(this, "无法获取设备信息", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * JSON导出到本地
     */
    private fun exportDeviceInfoToJson() {
        val success = DeviceInfoJsonExporter.exportDeviceInfoToJson(this)
        if (success) {
            Toast.makeText(this, "设备信息已成功保存为JSON文件", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "设备信息保存失败", Toast.LENGTH_SHORT).show()
        }
    }
    /**
     * 处理数据加密上传
     */
    private fun uploadDataToServer() {
        // 在后台线程中执行网络操作
        Thread {
            val success = DataTransmitter.sendDeviceInfo(this)
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "数据已成功加密并上传", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "数据上传失败", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
