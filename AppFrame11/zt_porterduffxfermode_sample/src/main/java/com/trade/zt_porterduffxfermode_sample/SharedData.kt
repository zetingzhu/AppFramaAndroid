package com.trade.zt_porterduffxfermode_sample

import android.graphics.PorterDuff

/**
 * 封装单条 PorterDuff 混合模式展示项的数据结构。
 * @property mode Android 图形库中的枚举值，代表不同的混合模式
 * @property title 在界面上显示的标题（例如："CLEAR"）
 * @property description 描述该混合模式的基本原理
 * @property effect 描述在该示例页面下看到的具体视觉效果
 * @property srcAlpha 源图形的整体透明度 (0.0f - 1.0f)，默认 1.0f (完全不透明)
 * @property useGradient 是否使用线性渐变 (LinearGradient) 绘制源图形，默认 false
 */
data class PorterDuffModeItem(
    val mode: PorterDuff.Mode,
    val title: String,
    val description: String,
    val effect: String,
    val srcAlpha: Float = 1.0f,
    val useGradient: Boolean = false
)

/**
 * 提取的公共数据单例对象。
 * 目的是为了让 Compose, Java View, Kotlin View 三种实现方式能够复用同一份数据列表，
 * 避免在多个类中重复定义。
 */
object SharedData {
    // 使用 @JvmField 注解，是为了在 Java 代码 (如 JavaPorterDuffActivity.java) 
    // 中能像访问普通静态变量一样访问它: SharedData.PORTER_DUFF_MODE_ITEMS
    @JvmField
    val PORTER_DUFF_MODE_ITEMS = listOf(
        PorterDuffModeItem(PorterDuff.Mode.CLEAR, "CLEAR", "清除源与目标重叠区域（不受源透明度影响）。", "重叠区域完全透明"),

        PorterDuffModeItem(PorterDuff.Mode.SRC, "SRC (100%)", "仅保留源图像。", "结果只显示不透明的绿色源图形"),
        PorterDuffModeItem(PorterDuff.Mode.SRC, "SRC (50%)", "仅保留源图像。", "结果只显示半透明的绿色源图形", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.DST, "DST", "仅保留目标图像（不受源透明度影响）。", "结果只显示蓝色目标图形"),

        PorterDuffModeItem(PorterDuff.Mode.SRC_OVER, "SRC_OVER (100%)", "源覆盖目标（默认模式）。", "重叠区域完全显示源图形"),
        PorterDuffModeItem(PorterDuff.Mode.SRC_OVER, "SRC_OVER (50%)", "源半透明覆盖目标。", "重叠区域的源图形半透明，透出底层的蓝色目标", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.DST_OVER, "DST_OVER (100%)", "目标覆盖在源之上。", "重叠区域优先显示目标"),
        PorterDuffModeItem(PorterDuff.Mode.DST_OVER, "DST_OVER (50%)", "目标覆盖在源之上。", "重叠区域仍显示目标，非重叠区的源图形变为半透明", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.SRC_IN, "SRC_IN (100%)", "仅保留源与目标相交部分。", "只显示不透明的绿色交集"),
        PorterDuffModeItem(PorterDuff.Mode.SRC_IN, "SRC_IN (50%)", "仅保留源与目标相交部分。", "交集处的绿色源图形变为半透明", 0.5f),
        PorterDuffModeItem(PorterDuff.Mode.SRC_IN, "SRC_IN (线性渐变)", "目标图形使用从上到下的线性渐变，源图形为纯色矩形", "绿色源图形(上层)被蓝色渐变目标图形(下层)裁剪，呈现出从上到下的绿色渐隐效果", 1.0f, true),

        PorterDuffModeItem(PorterDuff.Mode.DST_IN, "DST_IN (100%)", "仅保留目标与源相交部分。", "只显示不透明的蓝色交集"),
        PorterDuffModeItem(PorterDuff.Mode.DST_IN, "DST_IN (50%)", "仅保留目标与源相交部分。保留下来的目标区域透明度受源影响。", "相交区域保留蓝色且变为半透明，其余完全透明", 0.5f),
        PorterDuffModeItem(PorterDuff.Mode.DST_IN, "DST_IN (线性渐变)", "源图形使用由不透明到透明的线性渐变。", "目标图形(蓝色)在相交处呈现出渐隐的遮罩效果，常用于实现倒影或边缘淡出", 1.0f, true),

        PorterDuffModeItem(PorterDuff.Mode.SRC_OUT, "SRC_OUT (100%)", "仅保留源中不与目标重叠部分。", "显示不透明的绿色非重叠区"),
        PorterDuffModeItem(PorterDuff.Mode.SRC_OUT, "SRC_OUT (50%)", "仅保留源中不与目标重叠部分。", "绿色非重叠区变为半透明", 0.5f),
        PorterDuffModeItem(PorterDuff.Mode.SRC_OUT, "SRC_OUT (线性渐变)", "目标图形使用从上到下的线性渐变，源图形为纯色矩形", "绿色源图形(上层)被蓝色渐变目标图形(下层)掏空，呈现出上方被擦除、下方保留的渐变效果", 1.0f, true),

        PorterDuffModeItem(PorterDuff.Mode.DST_OUT, "DST_OUT (100%)", "仅保留目标中不与源重叠部分。源图形完全不透明时，效果等同于 CLEAR。", "显示蓝色非重叠区，相交区域完全透明"),
        PorterDuffModeItem(PorterDuff.Mode.DST_OUT, "DST_OUT (50%)", "仅保留目标中不与源重叠部分。目标图形在相交处被半透明擦除。", "相交区域未被完全挖空，底部蓝色变为半透明状态", 0.5f),
        PorterDuffModeItem(PorterDuff.Mode.DST_OUT, "DST_OUT (线性渐变)", "源图形使用线性渐变。", "目标图形(蓝色)在相交处呈现出渐变擦除的效果（类似橡皮擦渐变用力）", 1.0f, true),

        PorterDuffModeItem(PorterDuff.Mode.SRC_ATOP, "SRC_ATOP (100%)", "源绘制在目标之上，源超出目标部分被裁剪。", "重叠区是源色，非重叠保留目标"),
        PorterDuffModeItem(PorterDuff.Mode.SRC_ATOP, "SRC_ATOP (50%)", "源绘制在目标之上，源超出目标部分被裁剪。", "重叠区源色半透明透出目标色，非重叠区目标色不透明", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.DST_ATOP, "DST_ATOP (100%)", "目标绘制在源之上，目标超出源部分被裁剪。", "重叠区是目标色，非重叠保留源"),
        PorterDuffModeItem(PorterDuff.Mode.DST_ATOP, "DST_ATOP (50%)", "目标绘制在源之上，目标超出源部分被裁剪。", "重叠区目标色不透明，非重叠区源色变半透明", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.XOR, "XOR (100%)", "仅显示双方非重叠区域。", "交集完全透明，显示两侧外轮廓"),
        PorterDuffModeItem(PorterDuff.Mode.XOR, "XOR (50%)", "仅显示双方非重叠区域。", "交集未被完全擦除呈现半透明目标色，非交集源色半透明", 0.5f),
        PorterDuffModeItem(PorterDuff.Mode.XOR, "XOR (线性渐变)", "源图形使用线性渐变。", "交集区域出现从透明到不透明的渐变掏空效果", 1.0f, true),

        PorterDuffModeItem(PorterDuff.Mode.DARKEN, "DARKEN (100%)", "重叠区域取较暗颜色。", "交集颜色更暗"),
        PorterDuffModeItem(PorterDuff.Mode.DARKEN, "DARKEN (50%)", "重叠区域取较暗颜色。", "混合程度减弱，交集变暗程度较轻", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.LIGHTEN, "LIGHTEN (100%)", "重叠区域取较亮颜色。", "交集颜色更亮"),
        PorterDuffModeItem(PorterDuff.Mode.LIGHTEN, "LIGHTEN (50%)", "重叠区域取较亮颜色。", "混合程度减弱，交集变亮程度较轻", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.MULTIPLY, "MULTIPLY (100%)", "源与目标颜色相乘。", "交集明显变暗"),
        PorterDuffModeItem(PorterDuff.Mode.MULTIPLY, "MULTIPLY (50%)", "源与目标颜色相乘。", "混合程度减弱，交集变暗不如100%时明显", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.SCREEN, "SCREEN (100%)", "源与目标做滤色混合。", "交集明显变亮"),
        PorterDuffModeItem(PorterDuff.Mode.SCREEN, "SCREEN (50%)", "源与目标做滤色混合。", "混合程度减弱，交集变亮不如100%时明显", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.ADD, "ADD (100%)", "源与目标颜色通道相加并截断。", "交集趋向高亮"),
        PorterDuffModeItem(PorterDuff.Mode.ADD, "ADD (50%)", "源与目标颜色通道相加并截断。", "添加的亮度减弱，交集高亮程度降低", 0.5f),

        PorterDuffModeItem(PorterDuff.Mode.OVERLAY, "OVERLAY (100%)", "叠加混合，暗处更暗亮处更亮。", "交集对比度增强"),
        PorterDuffModeItem(PorterDuff.Mode.OVERLAY, "OVERLAY (50%)", "叠加混合，暗处更暗亮处更亮。", "叠加效果减半，对比度增强程度降低", 0.5f)
    )
}
