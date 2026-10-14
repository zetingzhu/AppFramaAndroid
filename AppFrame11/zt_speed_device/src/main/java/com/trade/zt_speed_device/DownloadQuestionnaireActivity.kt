package com.trade.zt_speed_device

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class DownloadQuestionnaireActivity : ComponentActivity() {

    private lateinit var tvFileInfo: TextView
    private lateinit var tvResult: TextView

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            tvResult.text = "存储权限已授予，请再次点击按钮"
        } else {
            tvResult.text = "需要存储权限才能在低版本系统写入 Download 目录"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_download_questionnaire)

        tvFileInfo = findViewById(R.id.tvFileInfo)
        tvResult = findViewById(R.id.tvResult)

        tvFileInfo.text = buildString {
            appendLine("文件名: ${DownloadQuestionnaireHelper.FILE_NAME}")
            appendLine("写入内容: ${DownloadQuestionnaireHelper.FILE_CONTENT}")
            append("路径: ${DownloadQuestionnaireHelper.downloadsPathHint()}")
        }

        findViewById<Button>(R.id.btnWrite).setOnClickListener {
            if (!ensureLegacyStoragePermission()) return@setOnClickListener
            val result = DownloadQuestionnaireHelper.writeToDownloads(this)
            tvResult.text = if (result.success) {
                "写入成功\n${result.pathHint}"
            } else {
                "写入失败: ${result.errorMessage}\n${result.pathHint}"
            }
        }

        findViewById<Button>(R.id.btnRead).setOnClickListener {
            if (!ensureLegacyStoragePermission(forRead = true)) return@setOnClickListener
            val result = DownloadQuestionnaireHelper.readFromDownloadsIfExists(this)
            tvResult.text = when {
                result.exists -> "文件存在\n${result.pathHint}\n\n内容:\n${result.content.orEmpty()}"
                else -> "未找到文件\n${result.pathHint}\n${result.errorMessage.orEmpty()}"
            }
        }

        findViewById<Button>(R.id.btnList).setOnClickListener {
            if (!ensureLegacyStoragePermission(forRead = true)) return@setOnClickListener
            val result = DownloadQuestionnaireHelper.listAccessibleDownloadFiles(this)
            tvResult.text = DownloadQuestionnaireHelper.formatListResult(result)
        }
    }

    /** Android 9 及以下写入公共 Download 需要 WRITE；读取在 10–12 可能需要 READ */
    private fun ensureLegacyStoragePermission(forRead: Boolean = false): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true
        }
        val permission = if (forRead && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Manifest.permission.READ_EXTERNAL_STORAGE
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        requestStoragePermission.launch(permission)
        return false
    }
}
