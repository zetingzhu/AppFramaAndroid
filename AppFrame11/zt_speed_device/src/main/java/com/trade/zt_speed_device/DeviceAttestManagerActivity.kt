package com.trade.zt_speed_device

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class DeviceAttestManagerActivity : ComponentActivity() {

    private lateinit var manager: DeviceAttestManager
    private lateinit var etCloudProject: EditText
    private lateinit var btnRequest: Button
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_attest_manager)

        manager = DeviceAttestManager(this)
        etCloudProject = findViewById(R.id.etCloudProject)
        btnRequest = findViewById(R.id.btnRequest)
        tvResult = findViewById(R.id.tvResult)

        etCloudProject.setText(DeviceAttestManager.DEFAULT_CLOUD_PROJECT_NUMBER.toString())
        tvResult.text = "PlayIntegrity 支持: ${DeviceAttestManager.isPlayIntegritySupported(this)}"

        btnRequest.setOnClickListener { onRequestClicked() }
    }

    private fun onRequestClicked() {
        val projectNumber = etCloudProject.text?.toString()?.toLongOrNull()
        if (projectNumber == null || projectNumber <= 0L) {
            tvResult.text = "请填写有效的 Cloud Project Number"
            return
        }

        setBusy(true)
        tvResult.text = "获取中..."
        manager.requestAttestation(cloudProjectNumber = projectNumber)
            .addOnSuccessListener { result ->
                setBusy(false)
                tvResult.text = result.toServerPayloadJson()
            }
            .addOnFailureListener { e ->
                setBusy(false)
                tvResult.text = "FAIL: ${e.message}"
            }
    }

    private fun setBusy(busy: Boolean) {
        btnRequest.isEnabled = !busy
        etCloudProject.isEnabled = !busy
    }
}
