package com.trade.zt_scroll_edit_sample

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class ScrollEditFormFragment : Fragment() {
    val TAG = ScrollEditFormFragment::class.java.simpleName
    private val keyboardUtils by lazy { KeyboardUtilV2.getInstance() }

    private var scrollView: ScrollView? = null
    private var fragmentRoot: View? = null
    private var bottomLayout: View? = null
    private var pageIndex: Int = PAGE_BASIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageIndex = arguments?.getInt(ARG_PAGE_INDEX, PAGE_BASIC) ?: PAGE_BASIC
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_scroll_edit_form, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentRoot = view.findViewById(R.id.fragment_root)
        scrollView = view.findViewById(R.id.scroll_content)
        bottomLayout = view.findViewById(R.id.ll_bottom_layout)
        val titleView = view.findViewById<TextView>(R.id.tv_page_title)
        val subtitleView = view.findViewById<TextView>(R.id.tv_page_subtitle)
        val basicLayout = view.findViewById<LinearLayout>(R.id.layout_basic_info)
        val extraLayout = view.findViewById<LinearLayout>(R.id.layout_extra_info)
        val btnSubmit = view.findViewById<MaterialButton>(R.id.btn_submit)

        when (pageIndex) {
            PAGE_BASIC -> {
                titleView.setText(R.string.scroll_edit_basic_title)
                subtitleView.setText(R.string.scroll_edit_basic_subtitle)
                basicLayout.isVisible = true
                extraLayout.isVisible = false
            }

            PAGE_EXTRA -> {
                titleView.setText(R.string.scroll_edit_extra_title)
                subtitleView.setText(R.string.scroll_edit_extra_subtitle)
                basicLayout.isVisible = false
                extraLayout.isVisible = true
            }
        }

        btnSubmit.setOnClickListener {
            (activity as? ScrollEditSubmitCallback)?.onSubmitRequested()
        }
    }

    override fun onResume() {
        super.onResume()
        val root = fragmentRoot ?: return
        val bottom = bottomLayout ?: return
        val sv = scrollView ?: return
        val topOffset = resources.getDimensionPixelOffset(R.dimen.margin_100dp)
        keyboardUtils.observeFragmentWindowInsets(
            this,
            root,
            bottom,
            object : KeyboardUtilV2.OnWindowInsetsListener {
                override fun onInsetsChanged(state: KeyboardUtilV2.WindowInsetsState) {
                    val contentBottomPadding = state.getContentBottomPadding(true)
                    Log.d(TAG, "底部键盘处理后的高度  contentBottomPadding:" + contentBottomPadding)

                    root.setPadding(
                        state.systemBarsLeft, 0,
                        state.systemBarsRight,
                        contentBottomPadding
                    )
                }

                override fun onKeyboardHide() {
                    keyboardUtils.clearInputFocus(activity)
                }
            }
        )
        keyboardUtils.attachFragmentScrollEdit(this, sv, topOffset)
    }

    override fun onPause() {
        keyboardUtils.removeFragmentWindowInsetsObserver(this)
        keyboardUtils.detachFragmentScrollEdit(this)
        super.onPause()
    }

    override fun onDestroyView() {
        fragmentRoot = null
        bottomLayout = null
        scrollView = null
        super.onDestroyView()
    }

    fun validateRequiredFields(): Boolean {
        val view = view ?: return false
        return when (pageIndex) {
            PAGE_BASIC -> validateBasicFields(view)
            else -> true
        }
    }

    fun collectSummaryLines(): List<String> {
        val view = view ?: return emptyList()
        return when (pageIndex) {
            PAGE_BASIC -> listOf(
                "姓名: ${view.findViewById<EditText>(R.id.et_name).text.toString().trim()}",
                "手机: ${view.findViewById<EditText>(R.id.et_phone).text.toString().trim()}",
                "邮箱: ${view.findViewById<EditText>(R.id.et_email).text.toString().trim()}",
                "性别: ${view.findViewById<EditText>(R.id.et_gender).text.toString().trim()}",
                "身份证: ${view.findViewById<EditText>(R.id.et_id_card).text.toString().trim()}",
                "出生日期: ${view.findViewById<EditText>(R.id.et_birthday).text.toString().trim()}",
                "公司: ${view.findViewById<EditText>(R.id.et_company).text.toString().trim()}",
                "职位: ${view.findViewById<EditText>(R.id.et_job).text.toString().trim()}",
                "紧急联系人: ${
                    view.findViewById<EditText>(R.id.et_emergency_name).text.toString().trim()
                }",
                "紧急联系人电话: ${
                    view.findViewById<EditText>(R.id.et_emergency_phone).text.toString().trim()
                }",
                "个人简介: ${view.findViewById<EditText>(R.id.et_bio).text.toString().trim()}"
            )

            PAGE_EXTRA -> listOf(
                "地址: ${view.findViewById<EditText>(R.id.et_address).text.toString().trim()}",
                "备注: ${view.findViewById<EditText>(R.id.et_remark).text.toString().trim()}"
            )

            else -> emptyList()
        }
    }

    private fun validateBasicFields(view: View): Boolean {
        val etName = view.findViewById<EditText>(R.id.et_name)
        val etPhone = view.findViewById<EditText>(R.id.et_phone)
        val etEmail = view.findViewById<EditText>(R.id.et_email)

        return when {
            etName.text.toString().trim().isEmpty() -> {
                etName.error = getString(R.string.scroll_edit_error_required)
                etName.requestFocus()
                false
            }

            etPhone.text.toString().trim().isEmpty() -> {
                etPhone.error = getString(R.string.scroll_edit_error_required)
                etPhone.requestFocus()
                false
            }

            etEmail.text.toString().trim().isEmpty() -> {
                etEmail.error = getString(R.string.scroll_edit_error_required)
                etEmail.requestFocus()
                false
            }

            else -> true
        }
    }

    companion object {
        const val PAGE_BASIC = 0
        const val PAGE_EXTRA = 1
        private const val ARG_PAGE_INDEX = "page_index"

        fun newInstance(pageIndex: Int): ScrollEditFormFragment {
            return ScrollEditFormFragment().apply {
                arguments = bundleOf(ARG_PAGE_INDEX to pageIndex)
            }
        }
    }
}
