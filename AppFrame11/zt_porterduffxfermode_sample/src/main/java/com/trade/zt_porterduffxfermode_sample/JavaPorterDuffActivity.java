package com.trade.zt_porterduffxfermode_sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Java 版本的 PorterDuffXfermode 演示页面。
 * 使用传统的 XML 布局与 findViewById 方式来构建界面。
 * 核心绘制逻辑在 [JavaPorterDuffView] 中实现。
 */
public class JavaPorterDuffActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 加载主容器布局（包含一个 ScrollView 和一个 LinearLayout 容器）
        setContentView(R.layout.activity_traditional);

        TextView tvHeader = findViewById(R.id.tv_header);
        tvHeader.setText("PorterDuffXfermode 示例列车 (Java View)");

        LinearLayout container = findViewById(R.id.container);
        LayoutInflater inflater = LayoutInflater.from(this);

        // 遍历 SharedData 中的公共数据，为每一个混合模式动态创建并添加一个卡片 View
        for (PorterDuffModeItem item : SharedData.PORTER_DUFF_MODE_ITEMS) {
            // 加载单个列表项的 XML 布局
            View itemView = inflater.inflate(R.layout.item_traditional, container, false);
            
            TextView tvTitle = itemView.findViewById(R.id.tv_title);
            TextView tvDesc = itemView.findViewById(R.id.tv_description);
            TextView tvEffect = itemView.findViewById(R.id.tv_effect);
            FrameLayout canvasContainer = itemView.findViewById(R.id.canvas_container);

            // 绑定文本数据
            tvTitle.setText(item.getTitle());
            tvDesc.setText(item.getDescription());
            tvEffect.setText("效果说明：" + item.getEffect());

            // 实例化纯 Java 编写的自定义绘制 View，并设置当前遍历到的混合模式
            JavaPorterDuffView demoView = new JavaPorterDuffView(this);
            demoView.setMode(item.getMode());
            // 设置源画笔的透明度
            demoView.setSrcAlpha(item.getSrcAlpha());
            // 传递是否使用渐变
            demoView.setUseGradient(item.getUseGradient());
            
            // 将画布添加到当前列表项的占位容器中
            canvasContainer.addView(demoView);

            // 将组装好的整个列表项添加到主页面的容器中
            container.addView(itemView);
        }
    }
}
