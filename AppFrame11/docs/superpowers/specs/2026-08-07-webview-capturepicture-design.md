# WebView capturePicture 演示页设计

日期：2026-08-07  
模块：`zt_webviewcap`

## 目标

在 `zt_webviewcap` 中提供可对照的两套页面，演示调用 `WebView.capturePicture()`：截图后弹窗预览，并保存到本地文件，Toast 文件路径。

## 范围

- 入口页 + Compose 截图页 + XML 截图页
- 公共截图/转 Bitmap/存文件逻辑
- `INTERNET` 权限与 Activity 注册

不在范围：`PixelCopy` / 现代截图替代方案、外部分享、相册 MediaStore 写入、单元测试硬性要求。

## 架构

```
MainActivity (入口)
├── ComposeCaptureActivity  (Compose + AndroidView)
└── XmlCaptureActivity      (XML 布局)
         │
         └── WebViewCaptureHelper (公共：capturePicture → Bitmap → 文件)
```

## 页面

### MainActivity（启动入口）

- 两个按钮：
  1. 「Compose WebView 截图」→ `ComposeCaptureActivity`
  2. 「XML WebView 截图」→ `XmlCaptureActivity`
- 可用现有 Compose 脚手架实现入口 UI。

### ComposeCaptureActivity

- 顶部工具栏：标题 +「截图」按钮
- 下方 `AndroidView` 嵌入 `WebView`
- 启动加载固定 URL：`https://www.baidu.com`
- WebView 开启 JavaScript
- 截图成功：Compose Dialog 预览 Bitmap；保存文件后 Toast 路径

### XmlCaptureActivity

- 布局文件：`activity_xml_capture.xml`
  - 顶部 Button（截图）
  - 下方 `WebView` 占满剩余空间
- 行为与 Compose 页一致（同一 URL、同一 Helper）
- 预览：`AlertDialog` + `ImageView`

## 公共逻辑：WebViewCaptureHelper

输入：`WebView`、`Context`（用于写文件）

流程：

1. 调用 `webView.capturePicture()` 得到 `Picture`
2. 按 `Picture` 宽高创建 `Bitmap`，`Canvas` 绘制
3. 将 Bitmap 压缩为 PNG，写入应用缓存目录（例如 `cacheDir/webview_capture/`），文件名带时间戳
4. 返回结果：`Bitmap` + 文件绝对路径；失败时给出可读错误信息

说明：`capturePicture()` 已废弃；本模块刻意调用该 API 做演示。若返回空图或异常，Toast 提示，不崩溃。

## Manifest / 依赖

- `AndroidManifest.xml`：
  - `<uses-permission android:name="android.permission.INTERNET" />`
  - 注册 `ComposeCaptureActivity`、`XmlCaptureActivity`
- `build.gradle.kts`：如需 WebView，使用系统 `android.webkit`（无需额外依赖）；入口若用 Material 按钮沿用现有 Compose 依赖

## 错误处理

| 场景 | 行为 |
|------|------|
| WebView 尚未布局完成 / Picture 宽高为 0 | Toast 提示暂不可截图 |
| 写文件失败 | Toast 失败原因；若已有 Bitmap 仍可预览 |
| 页面未加载完 | 仍允许点击截图（不强制等 `onPageFinished`） |

## 成功标准

1. 从入口可分别进入两个页面
2. 两页均能加载百度
3. 点击截图均调用 `capturePicture`
4. 成功后弹窗可见预览图，且 Toast 显示已保存文件路径
5. 应用缓存目录下能找到对应 PNG

## 文件清单（实现时）

| 文件 | 作用 |
|------|------|
| `MainActivity.kt` | 入口导航 |
| `ComposeCaptureActivity.kt` | Compose 截图页 |
| `XmlCaptureActivity.kt` | XML 截图页 |
| `WebViewCaptureHelper.kt` | 公共截图逻辑 |
| `res/layout/activity_xml_capture.xml` | XML 页布局 |
| `AndroidManifest.xml` | 权限 + Activity |
| `strings.xml` | 文案 |
