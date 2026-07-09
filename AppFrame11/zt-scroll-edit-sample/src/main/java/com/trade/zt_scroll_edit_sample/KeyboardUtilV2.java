package com.trade.zt_scroll_edit_sample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

/**
 * 键盘与滚动表单适配工具类。
 * <p>
 * 本工具类只负责监听 WindowInsets、焦点变化、触摸事件，并将计算结果通过回调返回；
 * <b>不会自动调用 {@link View#setPadding(int, int, int, int)}</b>，padding / margin 由业务页面自行设置。
 * </p>
 *
 * 使用此方案前提条件设置边到边，必须设置下面代码
 * WindowCompat.setDecorFitsSystemWindows(window, false)
 *
 *
 * <h3>获取实例</h3>
 * <pre>
 * KeyboardUtils keyboardUtils = KeyboardUtils.getInstance();
 * </pre>
 *
 * <h3>使用前准备</h3>
 * <pre>
 * // Activity onCreate 中（必须）
 * WindowCompat.setDecorFitsSystemWindows(window, false);
 * </pre>
 *
 * <h3>场景一：单页 Activity，提交按钮在 Activity 底部</h3>
 * <pre>
 * // 1. 监听 insets，自行设置 padding
 * KeyboardUtils.getInstance().observeWindowInsets(rootLayout, llBottomLayout, state -> {
 *     rootLayout.setPadding(
 *         state.systemBarsLeft,
 *         state.systemBarsTop,
 *         state.systemBarsRight,
 *         state.getContentBottomPadding(false)
 *     );
 * });
 *
 * // 2. 输入框获焦时自动滚动（可选）
 * KeyboardUtils.getInstance().registerEditTextFocusScroll(activity, scrollView, topOffsetPx);
 *
 * // 3. 点击空白处隐藏键盘（二选一）
 * // 方式 A：在 dispatchTouchEvent 中调用
 * KeyboardUtils.getInstance().handleTouchToHideKeyboard(activity, ev);
 * // 方式 B：绑定到根布局
 * KeyboardUtils.getInstance().attachTouchToHideKeyboard(activity, rootLayout);
 * </pre>
 *
 * <h3>场景二：ViewPager2 + 多 Fragment，提交按钮在 Fragment 底部</h3>
 * <pre>
 * // Activity：只处理 systemBars
 * KeyboardUtils.getInstance().observeWindowInsets(rootLayout, null, state -> {
 *     rootLayout.setPadding(
 *         state.systemBarsLeft, state.systemBarsTop,
 *         state.systemBarsRight, state.systemBarsBottom
 *     );
 * });
 * KeyboardUtils.getInstance().handleTouchToHideKeyboard(activity, ev); // dispatchTouchEvent 中
 *
 * // Fragment onResume：
 * KeyboardUtils.getInstance().observeFragmentWindowInsets(fragment, fragmentRoot, bottomLayout, state -> {
 *     fragmentRoot.setPadding(
 *         state.systemBarsLeft, 0,
 *         state.systemBarsRight,
 *         state.getContentBottomPadding(true) // Fragment 传 true
 *     );
 * });
 * KeyboardUtils.getInstance().attachFragmentScrollEdit(fragment, scrollView, topOffsetPx);
 *
 * // Fragment onPause：
 * KeyboardUtils.getInstance().removeFragmentWindowInsetsObserver(fragment);
 * KeyboardUtils.getInstance().detachFragmentScrollEdit(fragment);
 * </pre>
 *
 * <h3>场景三：快捷绑定（单页 Activity 一键接入）</h3>
 * <pre>
 * KeyboardUtils.getInstance().attachScrollEdit(activity, rootLayout, bottomLayout, scrollView, topOffsetPx, insetsListener);
 * // onDestroy:
 * KeyboardUtils.getInstance().detachScrollEdit(activity);
 * </pre>
 *
 * @author zeting
 * @date 2020/11/6
 */
public final class KeyboardUtilV2 {

    private static final int TAG_SCROLL_EDIT_FOCUS_LISTENER = -9;
    private static final int TAG_FRAGMENT_FOCUS_LISTENER = -10;
    private static final int TAG_FRAGMENT_KEYBOARD_ROOT = -11;
    private static final int TAG_TOUCH_TO_HIDE_KEYBOARD = -12;
    private static final int TAG_FRAGMENT_TOUCH_TO_HIDE_KEYBOARD = -13;

    private static volatile KeyboardUtilV2 sInstance;

    private KeyboardUtilV2() {
    }

    /**
     * 获取 {@link KeyboardUtilV2} 单例（双重检查锁）。
     */
    public static KeyboardUtilV2 getInstance() {
        if (sInstance == null) {
            synchronized (KeyboardUtilV2.class) {
                if (sInstance == null) {
                    sInstance = new KeyboardUtilV2();
                }
            }
        }
        return sInstance;
    }

    // -------------------------------------------------------------------------
    // WindowInsets 相关
    // -------------------------------------------------------------------------

    /**
     * WindowInsets 状态数据，由 {@link OnWindowInsetsListener#onInsetsChanged(WindowInsetsState)} 回调给业务层。
     * <p>
     * 业务层根据这些值自行决定如何设置 padding / margin / translationY 等。
     * </p>
     */
    public static final class WindowInsetsState {

        /** 系统栏左侧 inset（px） */
        public final int systemBarsLeft;
        /** 系统栏顶部 inset（px），通常用于适配状态栏 */
        public final int systemBarsTop;
        /** 系统栏右侧 inset（px） */
        public final int systemBarsRight;
        /** 系统栏底部 inset（px），通常用于适配导航栏 */
        public final int systemBarsBottom;
        /** 键盘（IME）底部 inset（px），键盘未弹出时为 0 */
        public final int imeBottom;
        /** 键盘是否可见 */
        public final boolean imeVisible;
        /**
         * 底部锚定视图高度（px），即 anchorView（如提交按钮容器）的测量高度。
         * 用于计算键盘弹起时需要预留的底部空间。
         */
        public final int anchorHeight;

        public WindowInsetsState(@NonNull final Insets systemBars,
                                 @NonNull final Insets imeInsets,
                                 final boolean imeVisible,
                                 final int anchorHeight) {
            this.systemBarsLeft = systemBars.left;
            this.systemBarsTop = systemBars.top;
            this.systemBarsRight = systemBars.right;
            this.systemBarsBottom = systemBars.bottom;
            this.imeBottom = imeInsets.bottom;
            this.imeVisible = imeVisible;
            this.anchorHeight = anchorHeight;
        }

        /**
         * 计算内容区域推荐的底部 padding。
         *
         * @param isFragment {@code true} 表示 Fragment 场景（提交按钮在 Fragment 内）；
         *                   {@code false} 表示 Activity 场景（提交按钮在 Activity 内）
         * @return 推荐底部 padding（px）
         * <ul>
         *   <li>Activity：{@code max(systemBarsBottom, imeBottom - anchorHeight)}</li>
         *   <li>Fragment：{@code max(0, imeBottom - anchorHeight - systemBarsBottom)}，
         *       因 Fragment 通常不再重复叠加导航栏高度</li>
         * </ul>
         */
        public int getContentBottomPadding(@Nullable final Boolean isFragment) {
            if (Boolean.TRUE.equals(isFragment)) {
                return Math.max(0, imeBottom - anchorHeight - systemBarsBottom);
            }
            return Math.max(systemBarsBottom, imeBottom - anchorHeight);
        }
    }

    /**
     * WindowInsets 变化监听。
     * <p>
     * 在 {@link #onInsetsChanged(WindowInsetsState)} 中接收 insets 数据并自行设置布局；
     * 可选重写 {@link #onKeyboardShow(int)} / {@link #onKeyboardHide()} 监听键盘显示/隐藏。
     * </p>
     */
    public interface OnWindowInsetsListener {

        /**
         * WindowInsets 发生变化时回调（systemBars / 键盘高度变化时均会触发）。
         *
         * @param state 当前 insets 状态，业务层据此设置 padding
         */
        void onInsetsChanged(@NonNull WindowInsetsState state);

        /**
         * 键盘显示时回调。
         *
         * @param height 键盘高度（px），等于 {@link WindowInsetsState#imeBottom}
         */
        default void onKeyboardShow(int height) {
        }

        /**
         * 键盘隐藏时回调。
         * <p>可在此调用 {@link KeyboardUtilV2#getInstance()}{@code .clearInputFocus(activity)} 清除输入框焦点。</p>
         */
        default void onKeyboardHide() {
        }
    }

    /**
     * Activity 层监听 WindowInsets。
     * <p>
     * 仅注册监听并回调数据，不会修改任何 View 的 padding。
     * </p>
     *
     * @param rootView   需要接收 insets 回调的根布局（通常是页面根 ConstraintLayout / LinearLayout）
     * @param anchorView 底部锚定视图，如提交按钮容器 {@code ll_bottom_layout}；
     *                   传 {@code null} 表示不扣除底部按钮高度
     * @param listener   insets 变化回调，业务层在回调中自行 {@code setPadding}
     * @see #removeWindowInsetsObserver(View)
     */
    public void observeWindowInsets(@NonNull final View rootView,
                                           @Nullable final View anchorView,
                                           @NonNull final OnWindowInsetsListener listener) {
        final boolean[] keyboardVisible = {false};
        final int[] keyboardHeight = {0};
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            WindowInsetsState state = new WindowInsetsState(
                    systemBars,
                    imeInsets,
                    imeVisible,
                    getAnchorViewHeight(anchorView)
            );
            listener.onInsetsChanged(state);
            if (imeVisible) {
                if (!keyboardVisible[0] || keyboardHeight[0] != imeInsets.bottom) {
                    keyboardVisible[0] = true;
                    keyboardHeight[0] = imeInsets.bottom;
                    listener.onKeyboardShow(imeInsets.bottom);
                }
            } else if (keyboardVisible[0]) {
                keyboardVisible[0] = false;
                keyboardHeight[0] = 0;
                listener.onKeyboardHide();
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * 移除 Activity 层 WindowInsets 监听。
     * <p>建议在 Activity {@code onDestroy} 中调用。</p>
     *
     * @param rootView 与 {@link #observeWindowInsets(View, View, OnWindowInsetsListener)} 传入的 rootView 一致
     */
    public void removeWindowInsetsObserver(@NonNull final View rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null);
        ViewCompat.requestApplyInsets(rootView);
    }

    /**
     * Fragment 层监听 WindowInsets。
     * <p>
     * 因 ViewPager2 同时只有一个 Fragment 处于 RESUMED，建议在 {@code onResume} 中绑定、
     * {@code onPause} 中解绑，避免多 Fragment 监听冲突。
     * </p>
     *
     * @param fragment   当前 Fragment
     * @param rootView   Fragment 根布局（含 ScrollView + 底部按钮的容器）
     * @param anchorView Fragment 底部锚定视图（提交按钮容器），传 {@code null} 则不扣除按钮高度
     * @param listener   insets 变化回调；底部 padding 建议使用 {@code state.getContentBottomPadding(true)}
     * @see #removeFragmentWindowInsetsObserver(Fragment)
     */
    public void observeFragmentWindowInsets(@NonNull final Fragment fragment,
                                                   @NonNull final View rootView,
                                                   @Nullable final View anchorView,
                                                   @NonNull final OnWindowInsetsListener listener) {
        if (fragment.getView() == null) {
            return;
        }
        removeFragmentWindowInsetsObserver(fragment);
        observeWindowInsets(rootView, anchorView, listener);
        fragment.getView().setTag(TAG_FRAGMENT_KEYBOARD_ROOT, rootView);
    }

    /**
     * Fragment 层移除 WindowInsets 监听。
     * <p>建议在 Fragment {@code onPause} 中调用。</p>
     *
     * @param fragment 当前 Fragment
     */
    public void removeFragmentWindowInsetsObserver(@NonNull final Fragment fragment) {
        final View fragmentView = fragment.getView();
        if (fragmentView == null) {
            return;
        }
        Object tag = fragmentView.getTag(TAG_FRAGMENT_KEYBOARD_ROOT);
        if (tag instanceof View) {
            removeWindowInsetsObserver((View) tag);
            fragmentView.setTag(TAG_FRAGMENT_KEYBOARD_ROOT, null);
        }
    }

    // -------------------------------------------------------------------------
    // 触摸隐藏键盘
    // -------------------------------------------------------------------------

    /**
     * 清除当前输入框焦点并隐藏软键盘。
     *
     * @param activity 当前 Activity，传 {@code null} 时忽略
     */
    public void clearInputFocusAndHideKeyboard(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        clearInputFocus(activity);
        hideSoftInput(activity);
    }

    /**
     * 隐藏软键盘。
     *
     * @param activity 当前 Activity，传 {@code null} 时忽略
     */
    public void hideSoftInput(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return;
        }
        View focus = activity.getCurrentFocus();
        if (focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } else {
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /**
     * 判断触摸点是否在当前输入框区域之外。
     * <p>
     * 使用 {@link View#getGlobalVisibleRect(Rect)} 计算可见区域，兼容 ScrollView 滚动后
     * 输入框位置变化，避免误判断为区外导致先隐藏再弹出键盘。
     * </p>
     *
     * @param event    触摸事件
     * @param editText 当前焦点输入框，非 {@link EditText} 时返回 {@code false}
     * @return {@code true} 表示触摸在输入框外部
     */
    public boolean isTouchOutsideEditText(@NonNull final MotionEvent event,
                                          @Nullable final View editText) {
        if (!(editText instanceof EditText)) {
            return false;
        }
        return !isTouchInsideView(editText, event);
    }

    /**
     * 判断触摸点是否落在任意可见 {@link EditText} 上。
     * <p>用于切换/重复点击输入框时不触发隐藏键盘。</p>
     */
    private boolean isTouchInsideAnyEditText(@NonNull final View root,
                                             @NonNull final MotionEvent event) {
        if (root instanceof EditText && isTouchInsideView(root, event)) {
            return true;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (isTouchInsideAnyEditText(group.getChildAt(i), event)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 判断触摸点是否落在指定 View 的屏幕可见区域内 */
    private boolean isTouchInsideView(@Nullable final View view,
                                      @NonNull final MotionEvent event) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        Rect visibleRect = new Rect();
        if (!view.getGlobalVisibleRect(visibleRect)) {
            return false;
        }
        int touchX = (int) event.getRawX();
        int touchY = (int) event.getRawY();
        return visibleRect.contains(touchX, touchY);
    }

    /**
     * 当有输入框获取焦点时，触摸非输入框区域则清除焦点并隐藏键盘。
     * <p>
     * <b>推荐用法：</b>在 Activity {@code dispatchTouchEvent} 中调用，不影响事件继续分发。
     * </p>
     * <pre>
     * {@literal @}Override
     * public boolean dispatchTouchEvent(MotionEvent ev) {
     *     KeyboardUtils.getInstance().handleTouchToHideKeyboard(this, ev);
     *     return super.dispatchTouchEvent(ev);
     * }
     * </pre>
     *
     * @param activity 当前 Activity
     * @param event    触摸事件，仅处理 {@link MotionEvent#ACTION_DOWN}
     * @return 固定返回 {@code false}，不消费事件，保证后续点击正常分发
     */
    public boolean handleTouchToHideKeyboard(@Nullable final Activity activity,
                                                    @Nullable final MotionEvent event) {
        if (activity == null || event == null || event.getAction() != MotionEvent.ACTION_DOWN) {
            return false;
        }
        View focus = activity.getCurrentFocus();
        if (!(focus instanceof EditText)) {
            return false;
        }
        // 触摸落在任意 EditText 上时不隐藏，避免滚动后重复点击或切换输入框时键盘闪烁
        View decorView = activity.getWindow().getDecorView();
        if (isTouchInsideAnyEditText(decorView, event)) {
            return false;
        }
        clearInputFocusAndHideKeyboard(activity);
        return false;
    }

    /**
     * 为指定 View 绑定触摸隐藏键盘。
     * <p>触摸非输入框区域时清除焦点并隐藏键盘，适用于不想重写 {@code dispatchTouchEvent} 的场景。</p>
     *
     * @param activity   当前 Activity
     * @param targetView 接收触摸事件的 View，通常是页面根布局
     * @see #detachTouchToHideKeyboard(View)
     */
    public void attachTouchToHideKeyboard(@NonNull final Activity activity,
                                                 @NonNull final View targetView) {
        detachTouchToHideKeyboard(targetView);
        View.OnTouchListener listener = (v, event) -> {
            handleTouchToHideKeyboard(activity, event);
            return false;
        };
        targetView.setOnTouchListener(listener);
        targetView.setTag(TAG_TOUCH_TO_HIDE_KEYBOARD, listener);
    }

    /**
     * 移除指定 View 的触摸隐藏键盘绑定。
     *
     * @param targetView 与 {@link #attachTouchToHideKeyboard(Activity, View)} 传入的 targetView 一致
     */
    public void detachTouchToHideKeyboard(@NonNull final View targetView) {
        Object tag = targetView.getTag(TAG_TOUCH_TO_HIDE_KEYBOARD);
        if (tag instanceof View.OnTouchListener) {
            targetView.setOnTouchListener(null);
            targetView.setTag(TAG_TOUCH_TO_HIDE_KEYBOARD, null);
        }
    }

    /**
     * Fragment 层绑定触摸隐藏键盘。
     * <p>建议在 {@code onResume} 中调用，绑定到 Fragment 根布局。</p>
     *
     * @param fragment   当前 Fragment
     * @param targetView Fragment 根布局
     * @see #detachFragmentTouchToHideKeyboard(Fragment)
     */
    public void attachFragmentTouchToHideKeyboard(@NonNull final Fragment fragment,
                                                         @NonNull final View targetView) {
        final Activity activity = fragment.getActivity();
        final View fragmentView = fragment.getView();
        if (activity == null || fragmentView == null) {
            return;
        }
        detachFragmentTouchToHideKeyboard(fragment);
        attachTouchToHideKeyboard(activity, targetView);
        fragmentView.setTag(TAG_FRAGMENT_TOUCH_TO_HIDE_KEYBOARD, targetView);
    }

    /**
     * Fragment 层移除触摸隐藏键盘绑定。
     * <p>建议在 {@code onPause} 中调用。</p>
     *
     * @param fragment 当前 Fragment
     */
    public void detachFragmentTouchToHideKeyboard(@NonNull final Fragment fragment) {
        final View fragmentView = fragment.getView();
        if (fragmentView == null) {
            return;
        }
        Object tag = fragmentView.getTag(TAG_FRAGMENT_TOUCH_TO_HIDE_KEYBOARD);
        if (tag instanceof View) {
            detachTouchToHideKeyboard((View) tag);
            fragmentView.setTag(TAG_FRAGMENT_TOUCH_TO_HIDE_KEYBOARD, null);
        }
    }

    // -------------------------------------------------------------------------
    // 焦点与滚动
    // -------------------------------------------------------------------------

    /**
     * 清除当前获取焦点的 {@link EditText}。
     * <p>若当前焦点不是 EditText，则不处理。</p>
     *
     * @param activity 当前 Activity，传 {@code null} 时忽略
     */
    public void clearInputFocus(@Nullable final Activity activity) {
        if (activity == null) {
            return;
        }
        View focus = activity.getCurrentFocus();
        if (focus instanceof EditText) {
            focus.clearFocus();
        }
    }

    /**
     * 将目标输入框滚动到距 ScrollView 顶部 {@code topOffsetPx} 的位置。
     * <p>通常在输入框获取焦点时调用，避免被键盘遮挡。</p>
     *
     * @param scrollView   表单所在的 ScrollView
     * @param target       获取焦点的输入框 View
     * @param topOffsetPx  距 ScrollView 顶部的偏移（px），如 {@code R.dimen.margin_100dp}
     */
    public void scrollOnInputFocus(@NonNull final ScrollView scrollView,
                                          @NonNull final View target,
                                          final int topOffsetPx) {
        scrollView.post(() -> {
            Context context = scrollView.getContext();
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }
            int scrollY = calcScrollOffset(scrollView, target) - topOffsetPx;
            int maxScrollY = Math.max(
                    0,
                    (scrollView.getChildAt(0) != null ? scrollView.getChildAt(0).getHeight() : 0) - scrollView.getHeight()
            );
            scrollView.smoothScrollTo(0, Math.max(0, Math.min(scrollY, maxScrollY)));
        });
    }

    /**
     * 注册全局焦点监听：任意 EditText 获取焦点时自动滚动 ScrollView。
     * <p>适用于单页 Activity；多 Fragment 场景请使用 {@link #attachFragmentScrollEdit}。</p>
     *
     * @param activity    当前 Activity
     * @param scrollView  表单 ScrollView
     * @param topOffsetPx 滚动后输入框距 ScrollView 顶部的偏移（px）
     * @return 焦点监听器，用于 {@link #unregisterEditTextFocusScroll(Activity, OnGlobalFocusChangeListener)} 解绑
     */
    @NonNull
    public OnGlobalFocusChangeListener registerEditTextFocusScroll(@NonNull final Activity activity,
                                                                          @NonNull final ScrollView scrollView,
                                                                          final int topOffsetPx) {
        OnGlobalFocusChangeListener listener = (oldFocus, newFocus) -> {
            if (newFocus instanceof EditText) {
                scrollOnInputFocus(scrollView, newFocus, topOffsetPx);
            }
        };
        activity.getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalFocusChangeListener(listener);
        return listener;
    }

    /**
     * 注销 EditText 焦点滚动监听。
     *
     * @param activity 当前 Activity
     * @param listener {@link #registerEditTextFocusScroll} 返回的监听器
     */
    public void unregisterEditTextFocusScroll(@NonNull final Activity activity,
                                                     @NonNull final OnGlobalFocusChangeListener listener) {
        activity.getWindow().getDecorView().getViewTreeObserver()
                .removeOnGlobalFocusChangeListener(listener);
    }

    // -------------------------------------------------------------------------
    // 组合绑定（快捷接入）
    // -------------------------------------------------------------------------

    /**
     * 单页 Activity 一键绑定：输入框获焦自动滚动 + WindowInsets 监听。
     * <p>
     * padding 仍由 {@code insetsListener} 回调中自行设置，本方法不会自动 setPadding。
     * </p>
     *
     * @param activity        当前 Activity
     * @param rootView        页面根布局
     * @param anchorView      底部提交按钮容器，传 {@code null} 不扣除按钮高度
     * @param scrollView      表单 ScrollView
     * @param topOffsetPx     获焦滚动偏移（px）
     * @param insetsListener  insets 回调，在 {@code onInsetsChanged} 中设置 padding
     * @see #detachScrollEdit(Activity)
     */
    public void attachScrollEdit(@NonNull final Activity activity,
                                        @Nullable final View rootView,
                                        @Nullable final View anchorView,
                                        @NonNull final ScrollView scrollView,
                                        final int topOffsetPx,
                                        @NonNull final OnWindowInsetsListener insetsListener) {
        OnGlobalFocusChangeListener listener = registerEditTextFocusScroll(activity, scrollView, topOffsetPx);
        activity.getWindow().getDecorView().setTag(TAG_SCROLL_EDIT_FOCUS_LISTENER, listener);
        if (rootView != null) {
            rootView.post(() -> observeWindowInsets(rootView, anchorView, insetsListener));
        }
    }

    /**
     * 解绑单页 Activity 滚动表单（仅解绑焦点滚动，insets 需自行 {@link #removeWindowInsetsObserver}）。
     *
     * @param activity 当前 Activity
     */
    public void detachScrollEdit(@NonNull final Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        Object tag = decorView.getTag(TAG_SCROLL_EDIT_FOCUS_LISTENER);
        if (tag instanceof OnGlobalFocusChangeListener) {
            unregisterEditTextFocusScroll(activity, (OnGlobalFocusChangeListener) tag);
            decorView.setTag(TAG_SCROLL_EDIT_FOCUS_LISTENER, null);
        }
    }

    /**
     * Fragment 层绑定输入框获焦自动滚动。
     * <p>
     * 仅当焦点属于当前 Fragment 的 View 树时才滚动，避免 ViewPager 相邻页误触发。
     * 建议在 {@code onResume} 中调用。
     * </p>
     *
     * @param fragment    当前 Fragment
     * @param scrollView  Fragment 内表单 ScrollView
     * @param topOffsetPx 滚动后输入框距 ScrollView 顶部的偏移（px）
     * @see #detachFragmentScrollEdit(Fragment)
     */
    public void attachFragmentScrollEdit(@NonNull final Fragment fragment,
                                                @NonNull final ScrollView scrollView,
                                                final int topOffsetPx) {
        final Activity activity = fragment.getActivity();
        final View fragmentView = fragment.getView();
        if (activity == null || fragmentView == null) {
            return;
        }
        detachFragmentScrollEdit(fragment);
        OnGlobalFocusChangeListener listener = (oldFocus, newFocus) -> {
            if (newFocus instanceof EditText && isViewInFragment(newFocus, fragmentView)) {
                scrollOnInputFocus(scrollView, newFocus, topOffsetPx);
            }
        };
        activity.getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalFocusChangeListener(listener);
        fragmentView.setTag(TAG_FRAGMENT_FOCUS_LISTENER, listener);
    }

    /**
     * Fragment 层解绑输入框获焦滚动。
     * <p>建议在 {@code onPause} 中调用。</p>
     *
     * @param fragment 当前 Fragment
     */
    public void detachFragmentScrollEdit(@NonNull final Fragment fragment) {
        final View fragmentView = fragment.getView();
        final Activity activity = fragment.getActivity();
        if (fragmentView == null || activity == null) {
            return;
        }
        Object tag = fragmentView.getTag(TAG_FRAGMENT_FOCUS_LISTENER);
        if (tag instanceof OnGlobalFocusChangeListener) {
            unregisterEditTextFocusScroll(activity, (OnGlobalFocusChangeListener) tag);
            fragmentView.setTag(TAG_FRAGMENT_FOCUS_LISTENER, null);
        }
    }

    // -------------------------------------------------------------------------
    // 内部工具
    // -------------------------------------------------------------------------

    /** 获取底部锚定视图高度，优先取已布局高度，否则取测量高度 */
    private int getAnchorViewHeight(@Nullable final View anchorView) {
        if (anchorView == null) {
            return 0;
        }
        return anchorView.getHeight() > 0 ? anchorView.getHeight() : anchorView.getMeasuredHeight();
    }

    /** 判断 view 是否属于指定 Fragment 的 View 树 */
    private boolean isViewInFragment(@NonNull final View view, @NonNull final View fragmentView) {
        View parent = view;
        while (parent != null) {
            if (parent == fragmentView) {
                return true;
            }
            if (parent.getParent() instanceof View) {
                parent = (View) parent.getParent();
            } else {
                break;
            }
        }
        return false;
    }

    /** 计算 target 相对 ScrollView 内容顶部的 Y 偏移 */
    private int calcScrollOffset(@NonNull final ScrollView scrollView, @NonNull final View target) {
        View view = target;
        int offset = 0;
        while (view.getParent() instanceof View && view.getParent() != scrollView) {
            offset += view.getTop();
            view = (View) view.getParent();
        }
        return offset;
    }

}
