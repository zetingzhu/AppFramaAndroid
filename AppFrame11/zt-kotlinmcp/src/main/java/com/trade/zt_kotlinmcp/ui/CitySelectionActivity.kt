package com.trade.zt_kotlinmcp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.trade.zt_kotlinmcp.R

class CitySelectionActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var ivClearSearch: ImageView
    private lateinit var tvCancelSearch: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var alphabetIndexView: AlphabetIndexView
    private lateinit var etManualInput: EditText
    private lateinit var ivClearManual: ImageView
    private lateinit var btnConfirm: Button

    private lateinit var adapter: CityAdapter
    private var allCities: List<CityItem> = emptyList()
    private var originalScrollPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_city_selection)

        initViews()
        initData()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Issue 4.3: Clear input and focus on resume/start
        if (::etManualInput.isInitialized) {
            etManualInput.setText("")
            etManualInput.clearFocus()
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        // Issue 4.2: Clicking anywhere outside EditText hides keyboard and clears focus
        if (ev != null && ev.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                
                // If the top search cancel button is tapped, let it process its own click
                val cancelRect = android.graphics.Rect()
                tvCancelSearch.getGlobalVisibleRect(cancelRect)
                
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) && 
                    !cancelRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard(v)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        ivClearSearch = findViewById(R.id.ivClearSearch)
        tvCancelSearch = findViewById(R.id.tvCancelSearch)
        recyclerView = findViewById(R.id.recyclerView)
        alphabetIndexView = findViewById(R.id.alphabetIndexView)
        etManualInput = findViewById(R.id.etManualInput)
        ivClearManual = findViewById(R.id.ivClearManual)
        btnConfirm = findViewById(R.id.btnConfirm)

        val backBtn = findViewById<ImageView>(R.id.ivBack)
        backBtn.setOnClickListener { finish() }

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun initData() {
        val groupedList = mutableListOf<CityItem>()

        // A
        groupedList.add(CityItem.Header("A"))
        groupedList.add(CityItem.Content("Austin", "1"))
        groupedList.add(CityItem.Content("Atlanta", "2"))
        groupedList.add(CityItem.Content("Albuquerque", "3"))
        groupedList.add(CityItem.Content("Anchorage", "4"))
        groupedList.add(CityItem.Content("Arlington", "5"))

        // B
        groupedList.add(CityItem.Header("B"))
        groupedList.add(CityItem.Content("Boston", "6"))
        groupedList.add(CityItem.Content("Baltimore", "7"))
        groupedList.add(CityItem.Content("Buffalo", "8"))
        groupedList.add(CityItem.Content("Boise", "9"))

        // C
        groupedList.add(CityItem.Header("C"))
        groupedList.add(CityItem.Content("Chicago", "10"))
        groupedList.add(CityItem.Content("Charlotte", "11"))
        groupedList.add(CityItem.Content("Columbus", "12"))
        groupedList.add(CityItem.Content("Cleveland", "13"))
        groupedList.add(CityItem.Content("Cincinnati", "14"))

        // D
        groupedList.add(CityItem.Header("D"))
        groupedList.add(CityItem.Content("Denver", "15"))
        groupedList.add(CityItem.Content("Dallas", "16"))
        groupedList.add(CityItem.Content("Detroit", "17"))
        groupedList.add(CityItem.Content("Durham", "18"))

        // E
        groupedList.add(CityItem.Header("E"))
        groupedList.add(CityItem.Content("El Paso", "19"))
        groupedList.add(CityItem.Content("Eugene", "20"))

        // F
        groupedList.add(CityItem.Header("F"))
        groupedList.add(CityItem.Content("Fort Worth", "21"))
        groupedList.add(CityItem.Content("Fresno", "22"))
        groupedList.add(CityItem.Content("Fargo", "23"))

        // G
        groupedList.add(CityItem.Header("G"))
        groupedList.add(CityItem.Content("Green Bay", "24"))
        groupedList.add(CityItem.Content("Greensboro", "25"))

        // H
        groupedList.add(CityItem.Header("H"))
        groupedList.add(CityItem.Content("Houston", "26"))
        groupedList.add(CityItem.Content("Honolulu", "27"))
        groupedList.add(CityItem.Content("Hartford", "28"))

        // I
        groupedList.add(CityItem.Header("I"))
        groupedList.add(CityItem.Content("Indianapolis", "29"))
        groupedList.add(CityItem.Content("Irvine", "30"))

        // J
        groupedList.add(CityItem.Header("J"))
        groupedList.add(CityItem.Content("Jacksonville", "31"))
        groupedList.add(CityItem.Content("Jersey City", "32"))

        // K
        groupedList.add(CityItem.Header("K"))
        groupedList.add(CityItem.Content("Kansas City", "33"))
        groupedList.add(CityItem.Content("Knoxville", "34"))

        // L
        groupedList.add(CityItem.Header("L"))
        groupedList.add(CityItem.Content("Los Angeles", "35"))
        groupedList.add(CityItem.Content("Las Vegas", "36"))
        groupedList.add(CityItem.Content("Louisville", "37"))
        groupedList.add(CityItem.Content("Lexington", "38"))

        // M
        groupedList.add(CityItem.Header("M"))
        groupedList.add(CityItem.Content("Miami", "39"))
        groupedList.add(CityItem.Content("Minneapolis", "40"))
        groupedList.add(CityItem.Content("Memphis", "41"))
        groupedList.add(CityItem.Content("Milwaukee", "42"))
        groupedList.add(CityItem.Content("Mesa", "43"))

        // N
        groupedList.add(CityItem.Header("N"))
        groupedList.add(CityItem.Content("New York", "44"))
        groupedList.add(CityItem.Content("Nashville", "45"))
        groupedList.add(CityItem.Content("New Orleans", "46"))
        groupedList.add(CityItem.Content("Newark", "47"))

        // O
        groupedList.add(CityItem.Header("O"))
        groupedList.add(CityItem.Content("Oakland", "48"))
        groupedList.add(CityItem.Content("Oklahoma City", "49"))
        groupedList.add(CityItem.Content("Orlando", "50"))
        groupedList.add(CityItem.Content("Omaha", "51"))

        // P
        groupedList.add(CityItem.Header("P"))
        groupedList.add(CityItem.Content("Philadelphia", "52"))
        groupedList.add(CityItem.Content("Phoenix", "53"))
        groupedList.add(CityItem.Content("Portland", "54"))
        groupedList.add(CityItem.Content("Pittsburgh", "55"))

        // Q
        groupedList.add(CityItem.Header("Q"))
        groupedList.add(CityItem.Content("Queens", "56"))

        // R
        groupedList.add(CityItem.Header("R"))
        groupedList.add(CityItem.Content("Raleigh", "57"))
        groupedList.add(CityItem.Content("Richmond", "58"))
        groupedList.add(CityItem.Content("Reno", "59"))

        // S
        groupedList.add(CityItem.Header("S"))
        groupedList.add(CityItem.Content("Seattle", "60"))
        groupedList.add(CityItem.Content("San Francisco", "61"))
        groupedList.add(CityItem.Content("San Diego", "62"))
        groupedList.add(CityItem.Content("San Antonio", "63"))
        groupedList.add(CityItem.Content("Sacramento", "64"))
        groupedList.add(CityItem.Content("Salt Lake City", "65"))
        groupedList.add(CityItem.Content("St. Louis", "66"))

        // T
        groupedList.add(CityItem.Header("T"))
        groupedList.add(CityItem.Content("Tampa", "67"))
        groupedList.add(CityItem.Content("Tucson", "68"))
        groupedList.add(CityItem.Content("Tulsa", "69"))

        // U
        groupedList.add(CityItem.Header("U"))
        groupedList.add(CityItem.Content("Urban Honolulu", "70"))

        // V
        groupedList.add(CityItem.Header("V"))
        groupedList.add(CityItem.Content("Virginia Beach", "71"))

        // W
        groupedList.add(CityItem.Header("W"))
        groupedList.add(CityItem.Content("Washington", "72"))
        groupedList.add(CityItem.Content("Wichita", "73"))
        groupedList.add(CityItem.Content("Winston-Salem", "74"))

        // X
        groupedList.add(CityItem.Header("X"))
        groupedList.add(CityItem.Content("Xenia", "75"))

        // Y
        groupedList.add(CityItem.Header("Y"))
        groupedList.add(CityItem.Content("Yonkers", "76"))

        // Z
        groupedList.add(CityItem.Header("Z"))
        groupedList.add(CityItem.Content("Zanesville", "77"))

        // #
        groupedList.add(CityItem.Header("#"))
        groupedList.add(CityItem.Content("纽约", "78"))
        groupedList.add(CityItem.Content("洛杉矶", "79"))
        groupedList.add(CityItem.Content("芝加哥", "80"))
        groupedList.add(CityItem.Content("旧金山", "81"))
        groupedList.add(CityItem.Content("北京", "82"))
        groupedList.add(CityItem.Content("上海", "83"))
        groupedList.add(CityItem.Content("广州", "84"))
        groupedList.add(CityItem.Content("深圳", "85"))
        groupedList.add(CityItem.Content("成都", "86"))
        groupedList.add(CityItem.Content("杭州", "87"))

        allCities = groupedList

        adapter = CityAdapter(allCities) { selectedCity ->
            returnCityResult(selectedCity)
        }
        recyclerView.adapter = adapter
    }

    private fun setupListeners() {
        // ============= Top Search Bar Logic =============
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            tvCancelSearch.visibility = if (hasFocus) View.VISIBLE else View.GONE
            if (hasFocus) {
                originalScrollPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                if (keyword.isNotEmpty()) {
                    ivClearSearch.visibility = View.VISIBLE
                    filterCities(keyword)
                } else {
                    ivClearSearch.visibility = View.GONE
                    adapter.updateData(allCities)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivClearSearch.setOnClickListener {
            etSearch.setText("")
        }

        tvCancelSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
            hideKeyboard(etSearch)
            if (originalScrollPosition >= 0 && originalScrollPosition < adapter.itemCount) {
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(originalScrollPosition, 0)
            }
        }

        // ============= Bottom Manual Input Logic =============
        etManualInput.setOnFocusChangeListener { _, hasFocus ->
            // PRD: 点击拉起键盘，提示语隐藏
            etManualInput.hint = if (hasFocus) "" else "无选项可手动填写"
        }

        etManualInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString()?.trim() ?: ""
                if (input.isNotEmpty()) {
                    ivClearManual.visibility = View.VISIBLE
                    btnConfirm.visibility = View.VISIBLE
                } else {
                    ivClearManual.visibility = View.GONE
                    btnConfirm.visibility = View.GONE
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivClearManual.setOnClickListener {
            etManualInput.setText("")
        }

        btnConfirm.setOnClickListener {
            val customCity = etManualInput.text.toString().trim()
            if (customCity.isNotEmpty()) {
                returnCityResult(customCity)
            }
        }

        // PRD 5.5 "点击页面其他位置退出该页面" -> 收起键盘并失去焦点 (这里的实现是通过 RecyclerView 滚动或者点击空白)
        recyclerView.setOnTouchListener { _, _ ->
            if (etManualInput.hasFocus()) {
                etManualInput.clearFocus()
                hideKeyboard(etManualInput)
            }
            if (etSearch.hasFocus()) {
                etSearch.clearFocus()
                hideKeyboard(etSearch)
            }
            false
        }


        // ============= Right Alphabet Index Bar Logic =============
        alphabetIndexView.onIndexChangeListener = { letter ->
            val position = adapter.getPositionForSection(letter)
            if (position != -1) {
                (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
            }
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
                val currentLetter = adapter.getSectionForPosition(firstVisiblePos)
                if (currentLetter != null) {
                    alphabetIndexView.setSelectedIndexByLetter(currentLetter)
                }
            }
        })
    }

    private fun filterCities(keyword: String) {
        val filtered = mutableListOf<CityItem>()
        var currentHeader: CityItem.Header? = null
        
        allCities.forEach { item ->
            if (item is CityItem.Header) {
                currentHeader = item
            } else if (item is CityItem.Content && item.text.contains(keyword, ignoreCase = true)) {
                if (currentHeader != null && !filtered.contains(currentHeader)) {
                    filtered.add(currentHeader!!)
                }
                filtered.add(item)
            }
        }
        adapter.updateData(filtered)
    }

    private fun returnCityResult(city: String) {
        val resultIntent = Intent().apply {
            putExtra("SELECTED_CITY", city)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
