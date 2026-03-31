package com.trade.zt_sketchmcp

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AboutYouActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_you)

        // 绑定顶部导航
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
        findViewById<TextView>(R.id.tv_skip).setOnClickListener {
            Toast.makeText(this, "Skipped", Toast.LENGTH_SHORT).show()
        }

        // 绑定输入框
        val etFirstName = findViewById<TextInputEditText>(R.id.et_first_name)
        val etMiddleName = findViewById<TextInputEditText>(R.id.et_middle_name)
        val etLastName = findViewById<TextInputEditText>(R.id.et_last_name)

        // 绑定底部弹出选择区域 (Gender 和 Date of birth)
        findViewById<LinearLayout>(R.id.ll_gender).setOnClickListener {
            Toast.makeText(this, "Gender picker logic goes here", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.ll_dob).setOnClickListener {
            Toast.makeText(this, "Date of birth picker logic goes here", Toast.LENGTH_SHORT).show()
        }

        // 提交按钮
        val btnContinue = findViewById<CardView>(R.id.btn_continue)
        btnContinue.setOnClickListener {
            val fName = etFirstName.text.toString().trim()
            val mName = etMiddleName.text.toString().trim()
            val lName = etLastName.text.toString().trim()

            var hasError = false
            val tilFirstName = findViewById<TextInputLayout>(R.id.til_first_name)
            val tilLastName = findViewById<TextInputLayout>(R.id.til_last_name)

            // 重置错误状态
            tilFirstName.error = null
            tilLastName.error = null
            tilFirstName.isErrorEnabled = false
            tilLastName.isErrorEnabled = false
            tilFirstName.hint = "First Name"
            tilLastName.hint = "*姓（请用英文填写）"

            val errorColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F42855"))

            // 简单校验
            if (fName.isEmpty()) {
                tilFirstName.isErrorEnabled = true
                tilFirstName.setErrorTextColor(errorColor)
                tilFirstName.boxStrokeErrorColor = errorColor
                tilFirstName.errorIconDrawable = null
                tilFirstName.error = " "
                tilFirstName.hint = "名（请用英文填写）"
                hasError = true
            }
            if (lName.isEmpty()) {
                tilLastName.isErrorEnabled = true
                tilLastName.setErrorTextColor(errorColor)
                tilLastName.boxStrokeErrorColor = errorColor
                tilLastName.errorIconDrawable = null
                tilLastName.error = " "
                tilLastName.hint = "*姓（请用英文填写）"
                hasError = true
            }
            
            if (hasError) {
                Toast.makeText(this, "请填写所有必填信息", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 执行提交逻辑
            val submitData = "First Name: $fName\nMiddle Name: $mName\nLast Name: $lName\nGender: Gender\nDOB: 2/12/2020"
            Toast.makeText(this, "Submitting...\n$submitData", Toast.LENGTH_LONG).show()
        }
    }
}
