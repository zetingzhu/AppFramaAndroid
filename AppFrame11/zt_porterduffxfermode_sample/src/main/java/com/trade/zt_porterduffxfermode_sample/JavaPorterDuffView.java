package com.trade.zt_porterduffxfermode_sample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

/**
 * 纯 Java 编写的自定义 View。
 * 与 Kotlin 版本的 [PorterDuffDemoCanvasView] 功能完全一致。
 * 演示了如何通过设置 Paint 的 Xfermode 来实现图形混合。
 */
public class JavaPorterDuffView extends View {

    private PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
    private float srcAlpha = 1.0f;
    private boolean useGradient = false;
    
    private final Paint dstPaint;
    private final Paint srcPaint;
    private final Paint labelPaint;
    private final Paint bgPaint;

    public JavaPorterDuffView(Context context) {
        super(context);
        
        // 初始化目标图形 (DST) 画笔 - 蓝色
        dstPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dstPaint.setColor(Color.parseColor("#3F51B5"));

        // 初始化源图形 (SRC) 画笔 - 绿色
        srcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        srcPaint.setColor(Color.parseColor("#4CAF50"));

        // 初始化文本标签画笔
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.DKGRAY);
        labelPaint.setTextSize(34f);

        // 初始化背景画笔
        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#F3F5F8"));
    }

    /**
     * 设置当前 View 需要演示的混合模式。
     * 调用 invalidate() 触发重新执行 onDraw。
     */
    public void setMode(PorterDuff.Mode mode) {
        this.mode = mode;
        invalidate();
    }
    
    /**
     * 设置源图形的透明度
     */
    public void setSrcAlpha(float alpha) {
        this.srcAlpha = alpha;
        if (!useGradient) {
            this.srcPaint.setAlpha((int) (alpha * 255));
        }
        invalidate();
    }

    /**
     * 设置是否使用线性渐变绘制源图形
     */
    public void setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        // 1. 绘制底色。
        canvas.drawRect(0, 0, w, h, bgPaint);

        // 2. 创建离屏图层 (SaveLayer)
        int layer = canvas.saveLayer(0, 0, w, h, null);

        RectF dstRect = new RectF(w * 0.14f, h * 0.18f, w * 0.62f, h * 0.74f);
        RectF srcRect;
        if (useGradient) {
            // 为了演示完美的渐变遮罩(如边缘淡出)，当使用渐变时，让源图形完全覆盖目标图形
            srcRect = new RectF(dstRect);
        } else {
            srcRect = new RectF(w * 0.38f, h * 0.30f, w * 0.86f, h * 0.88f);
        }

        // 判断当前是否是需要 DST 作为渐变的模式
        boolean isDstGradientMode = useGradient && (mode == PorterDuff.Mode.SRC_IN || mode == PorterDuff.Mode.SRC_OUT);
        // 判断当前是否是需要 SRC 作为渐变的模式
        boolean isSrcGradientMode = useGradient && !isDstGradientMode;

        int startColor = Color.parseColor("#4CAF50");
        int endColor = 0x004CAF50; // 完全透明的绿色

        // === 处理目标画笔 (DST) ===
        if (isDstGradientMode) {
            int dstStartColor = Color.parseColor("#2196F3");
            int dstEndColor = 0x002196F3;
            dstPaint.setShader(new LinearGradient(
                    dstRect.left, dstRect.top,
                    dstRect.left, dstRect.bottom, // 垂直方向
                    dstStartColor, dstEndColor,
                    Shader.TileMode.CLAMP
            ));
        } else {
            dstPaint.setShader(null);
            dstPaint.setColor(Color.parseColor("#2196F3"));
        }

        // === 处理源画笔 (SRC) ===
        if (isSrcGradientMode) {
            srcPaint.setShader(new LinearGradient(
                    srcRect.left, srcRect.top,
                    srcRect.left, srcRect.bottom, // 垂直渐变
                    startColor, endColor,
                    Shader.TileMode.CLAMP
            ));
            srcPaint.setAlpha(255);
        } else {
            srcPaint.setShader(null);
            srcPaint.setColor(Color.parseColor("#4CAF50"));
            srcPaint.setAlpha((int) (srcAlpha * 255));
        }

        // 3. 绘制目标图形 (Destination)
        canvas.drawRoundRect(dstRect, 36f, 36f, dstPaint);

        // 4. 为画源图形的画笔设置 Xfermode
        srcPaint.setXfermode(new PorterDuffXfermode(mode));
        
        // 5. 绘制源图形 (Source)
        if (useGradient) {
            canvas.drawRect(srcRect, srcPaint);
        } else {
            canvas.drawOval(srcRect, srcPaint);
        }
        
        // 6. 清空画笔的 Xfermode
        srcPaint.setXfermode(null);

        // 7. 将离屏图层的内容合并回主画布
        canvas.restoreToCount(layer);

        // 8. 在主画布上绘制文字
        canvas.drawText("DST", w * 0.16f, h * 0.14f, labelPaint);
        canvas.drawText("SRC", w * 0.72f, h * 0.26f, labelPaint);
    }
}
