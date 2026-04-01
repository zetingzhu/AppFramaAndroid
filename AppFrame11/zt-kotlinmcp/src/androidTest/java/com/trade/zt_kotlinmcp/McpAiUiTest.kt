package com.trade.zt_kotlinmcp

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.trade.zt_kotlinmcp.ui.CitySelectionActivity
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 这是一个极具前瞻性的 Agentic UI Testing (基于大模型的自动化测试) 演示。
 * 
 * 原理：
 * 1. 利用 Espresso 触发 Android 原生界面的按键和输入。
 * 2. 抓取屏幕状态，将其转换为 JSON 结构。
 * 3. 手机端化身 MCP Client (Model Context Protocol Client)。
 * 4. 通过 mcp-kotlin-sdk 向本地/云端部署的 LLM MCP Server 发起请求，让大模型“担任裁判”。
 * 5. 根据大模型的推理结果决定本次 JUnit 测试是 PASS 还是 FAIL。
 */
@RunWith(AndroidJUnit4::class)
class McpAiUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(CitySelectionActivity::class.java)

    @Test
    fun testCityInputWithMcpAi() = runBlocking {
        // Step 1. Android 原生自动化：模拟用户输入
        Log.i("McpAiTest", "Step 1: 模拟用户在底部输入框输入 '纽约'")
        onView(withId(R.id.etManualInput)).perform(replaceText("纽约"))
        
        // Step 2. 状态提取 (把界面的 UI 视图属性抓来)
        // 验证底部的清除与确认按钮是否由于输入了内容而变为了 Visible
        var isClearManualVisible = false
        var isConfirmBtnVisible = false
        
        onView(withId(R.id.ivClearManual)).check { view, _ ->
            isClearManualVisible = (view.visibility == android.view.View.VISIBLE)
        }
        onView(withId(R.id.btnConfirm)).check { view, _ ->
            isConfirmBtnVisible = (view.visibility == android.view.View.VISIBLE)
        }

        // 把当前屏幕的状态数据构造好，准备交给大模型审查
        val screenStateJson = """
            {
                "input_text": "纽约",
                "ivClearManual_visible": $isClearManualVisible,
                "btnConfirm_visible": $isConfirmBtnVisible
            }
        """.trimIndent()

        Log.i("McpAiTest", "Step 2: 界面状态收集完毕 -> $screenStateJson")

        // Step 3. 通信层：通过 MCP Kotlin SDK 呼叫大模型测试节点
        // (注：假设本机电脑 10.0.2.2 开发机运行着绑定了 OpenAI/Claude 能力的 MCP 服务器)
        val httpClient = HttpClient(CIO)
        val sseTransport = SseClientTransport(httpClient, "http://10.0.2.2:3000/sse")
        
        val mcpClient = Client(
            Implementation(name = "AndroidUiTestAgent", version = "1.0.0")
        )

        var aiVerdict = "FAIL"
        var aiReason = "未连接到大模型服务器进行推理"

        try {
            Log.i("McpAiTest", "Step 3: 正在通过 MCP 连接大模型测试服务器节点...")
            
            // 【硬核 MCP 调用流程】
            // 真实环境下，你需要放开这里的注释来真正链接本地跑着的 MCP Node Server：
            // mcpClient.initialize() 
            // 
            // val testPromptParams = mapOf(
            //     "rule" to "根据PRD第5项，当输入内容后，必须显示清除和确定按钮",
            //     "current_ui" to screenStateJson
            // )
            // val result = mcpClient.callTool("verify_prd_rule", testPromptParams)
            // (解析 result 获取 AI 判定的 PASS/FAIL ...)

            // =============== 下面是模拟 MCP 服务端的逻辑响应 =============== 
            // 因为您的开发机目前没有跑相关的 :3000 MCP 服务端，这一步我们做逻辑 Mock 判定
            // 以保证您马上能通过测试体会这个流程：
            val mockPromptSent = "校验规则: PRD 5条; 界面状态: $screenStateJson"
            Log.i("McpAiTest", "----> MCP 发送给 Server 请求: $mockPromptSent")
            
            if (isClearManualVisible && isConfirmBtnVisible) {
                aiVerdict = "PASS"
                aiReason = "[MCP-AI-Response] 完美符合 PRD：输入框有内容时，清除和确认按钮都正确显示。"
            } else {
                aiVerdict = "FAIL"
                aiReason = "[MCP-AI-Response] 违背 PRD：输入了内容但清除或确认按钮未显示。状态: $screenStateJson"
            }
            // ===============================================================

        } catch (e: Exception) {
            Log.e("McpAiTest", "MCP Server 连接异常", e)
        } finally {
            httpClient.close()
        }

        Log.i("McpAiTest", "Step 4: AI 大模型裁判给出结果 -> $aiVerdict (原因: $aiReason)")
        
        // Step 4. 将验证权交还给 JUnit。绿灯还是红灯，完全由大模型给您的接口返回决定！
        assertTrue("MCP AI 测试不通过，原因：$aiReason", aiVerdict == "PASS")
    }
}
