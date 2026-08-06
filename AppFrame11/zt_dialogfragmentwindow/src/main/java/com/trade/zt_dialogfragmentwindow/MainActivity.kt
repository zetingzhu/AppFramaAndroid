package com.trade.zt_dialogfragmentwindow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.trade.zt_dialogfragmentwindow.base.DialogHostManager
import com.trade.zt_dialogfragmentwindow.databinding.ActivityMainBinding
import com.trade.zt_dialogfragmentwindow.demo.TestDialogA
import com.trade.zt_dialogfragmentwindow.demo.TestDialogB
import com.trade.zt_dialogfragmentwindow.demo.TestDialogC

/**
 * DialogHostFragment 层级测试页：
 * 1. 只开 A
 * 2. 先 A 再 B 置顶 → B 盖住 A
 * 3. 先 A 再 B 压底 → A 仍在最上层，B 在底下
 * 4. 打开 C：全屏宽贴底
 * 5. 在弹框内继续互相打开，验证多处逻辑共用同一 Host
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnShowA.setOnClickListener {
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogA(layerHint = "层级：单独弹出 A")
            )
        }

        binding.btnShowAThenBTop.setOnClickListener {
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogA(layerHint = "层级：先弹出的 A（应被 B 盖住）")
            )
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogB(layerHint = "层级：后弹出的 B 置顶（应盖住 A）"),
                pushToBottom = false
            )
        }

        binding.btnShowAThenBBottom.setOnClickListener {
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogA(layerHint = "层级：先弹出的 A（应仍在最上层）")
            )
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogB(layerHint = "层级：后弹出的 B 压底（应被 A 盖住）"),
                pushToBottom = true
            )
        }

        binding.btnShowCBottom.setOnClickListener {
            DialogHostManager.show(
                supportFragmentManager,
                TestDialogC(layerHint = "层级：全屏宽 + 贴底")
            )
        }

        binding.btnDismissAll.setOnClickListener {
            DialogHostManager.dismissAll(supportFragmentManager)
        }
    }
}
