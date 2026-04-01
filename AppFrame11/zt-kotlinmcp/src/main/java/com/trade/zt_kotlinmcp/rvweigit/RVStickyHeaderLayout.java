package com.trade.zt_kotlinmcp.rvweigit;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;


/**
 * @author: zeting
 * @date: 2023/8/30
 * 头部吸顶布局。只要用 StickyHeaderLayoutV8 包裹{@link RecyclerView},
 * 适配器必须继承 RVStickyHeaderAdapter 就可以实现列表头部吸顶功能。
 * StickyHeaderLayoutV8 只能包裹 RecyclerView，而且只能包裹一个 RecyclerView。
 */
public class RVStickyHeaderLayout<VH extends RecyclerView.ViewHolder> extends FrameLayout {
    private static final String TAG = RVStickyHeaderLayout.class.getSimpleName();
    private Context mContext;
    private RecyclerView mRecyclerView;

    //吸顶容器，用于承载吸顶布局。
    private FrameLayout mStickyLayout;

    //保存吸顶布局的缓存池。它以列表组头的viewType为key,ViewHolder为value对吸顶布局进行保存和回收复用。
    private final SparseArray<VH> mStickyViews = new SparseArray<>();

    //用于在吸顶布局中保存viewType的key。
    private final int VIEW_TAG_TYPE = -101;

    //用于在吸顶布局中保存ViewHolder的key。
    private final int VIEW_TAG_HOLDER = -102;
    // 给 View 设置一个当前列表 位置
    public static final int VIEW_TAG_POSITION = -999;

    //记录当前吸顶的组。
    private int mCurrentStickyGroup = -1;

    //是否吸顶。
    private boolean isSticky = true;

    //是否已经注册了adapter刷新监听
    private boolean isRegisterDataObserver = false;

    private OnStickyChangedListener mListener;

    public RVStickyHeaderLayout(@NonNull Context context) {
        super(context);
        mContext = context;
    }

    public RVStickyHeaderLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public RVStickyHeaderLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 0 || !(child instanceof RecyclerView)) {
            //外界只能向StickyHeaderLayout添加一个RecyclerView,而且只能添加RecyclerView。
            throw new IllegalArgumentException("StickyHeaderLayout can host only one direct child --> RecyclerView");
        }
        super.addView(child, index, params);
        mRecyclerView = (RecyclerView) child;
        addOnScrollListener();
        addStickyLayout();
    }

    /**
     * 添加滚动监听
     */
    private void addOnScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // 在滚动的时候，需要不断的更新吸顶布局。
//                Log.d(TAG, "头部固定 - onScrolled dy ：" + dy + " isSticky:" + isSticky);
                if (isSticky) {
                    updateStickyView(false, 5);
                }
            }
        });
    }

    /**
     * 添加吸顶容器
     */
    private void addStickyLayout() {
        mStickyLayout = new FrameLayout(mContext);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        mStickyLayout.setLayoutParams(lp);
        super.addView(mStickyLayout, 1, lp);
    }

    /**
     * 强制更新吸顶布局。
     */
    public void updateStickyView() {
        updateStickyView(true, 6);
    }

    /**
     * 更新吸顶布局。
     *
     * @param imperative 是否强制更新。
     */
    private void updateStickyView(boolean imperative, int tag) {
        RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
        if (adapter instanceof RVStickyHeaderAdapter) {

            // 记录旧的吸顶组
            int oldGroupIndex = mCurrentStickyGroup;


            RVStickyHeaderAdapter gAdapter = (RVStickyHeaderAdapter) adapter;
            registerAdapterDataObserver(gAdapter);
            //获取列表显示的第一个项。
            int firstVisibleItem = getFirstVisibleItem();

            // 通过 adapter 将 firstVisibleItem 映射到所属分组的 Header position，
            // 这样同一分组内的所有 item 都映射到同一个 headerPos，避免频繁重建吸顶布局。
            int headerPos = gAdapter.getStickyHeaderPosition(firstVisibleItem);

            // 判断当前视图是否需要固定
            boolean stickyHeaderBoo = gAdapter.isStickyHeaderBoo(firstVisibleItem);
//            Log.d(TAG, "头部固定 - stickyHeaderBoo：" + stickyHeaderBoo + " first:" + firstVisibleItem + " headerPos:" + headerPos);

            //如果当前吸顶的组头不是我们要吸顶的组头，就更新吸顶布局。这样做可以避免频繁的更新吸顶布局。
            if (imperative || mCurrentStickyGroup != headerPos) {
                mCurrentStickyGroup = headerPos;

                if (headerPos != -1) {
                    //获取吸顶布局的viewType —— 使用 headerPos 而非 firstVisibleItem，
                    //确保始终拿到 Header 类型的 viewType。
                    int viewType = gAdapter.getItemViewType(headerPos);

                    //如果当前的吸顶布局的类型和我们需要的一样，就直接获取它的ViewHolder，否则就回收。
                    VH holder = recycleStickyView(viewType);

                    //标志holder是否是从当前吸顶布局取出来的。
                    boolean flag = holder != null;

                    if (holder == null) {
                        //从缓存池中获取吸顶布局。
                        holder = getStickyViewByType(viewType);
                    }

                    if (holder == null) {
                        //如果没有从缓存池中获取到吸顶布局，则通过 RecyclerViewAdapter 创建。
                        holder = (VH) gAdapter.onCreateViewHolder(mStickyLayout, viewType);
                        holder.itemView.setTag(VIEW_TAG_TYPE, viewType);
                        holder.itemView.setTag(VIEW_TAG_HOLDER, holder);
                    }

                    //通过 RecyclerViewAdapter 更新吸顶布局的数据。
                    //使用 headerPos 绑定，确保悬停头部显示正确的分组标题。
                    gAdapter.onBindViewHolderHead(holder, headerPos, true);

                    //如果holder不是从当前吸顶布局取出来的，就需要把吸顶布局添加到容器里。
                    if (!flag) {
                        mStickyLayout.addView(holder.itemView);
                    }
                } else {
                    //回收旧的吸顶布局。
                    recycle();
                }
            }

            if (mRecyclerView.computeVerticalScrollOffset() == 0) {
                // 滑动到顶部
                recycle();
            }

            if (stickyHeaderBoo) {
                mStickyLayout.setVisibility(VISIBLE);
                //这里是处理第一次打开时，吸顶布局已经添加到StickyLayout，但StickyLayout的高依然为0的情况。
                if (mStickyLayout.getChildCount() > 0 && mStickyLayout.getHeight() == 0) {
                    mStickyLayout.requestLayout();
                }
            } else {
                mStickyLayout.setVisibility(GONE);
            }

            //设置mStickyLayout的Y偏移量 —— 传入 headerPos 用于查找下一个分组 Header。
            float offSet = calculateOffset(gAdapter, headerPos, stickyHeaderBoo);
//            Log.d(TAG, "头部固定 - 移动距离：" + offSet + " first:" + firstVisibleItem + " headerPos:" + headerPos + " hb:" + stickyHeaderBoo + " tag:" + tag);

            mStickyLayout.setTranslationY(offSet);

            if (mListener != null && oldGroupIndex != mCurrentStickyGroup) {
                // 吸顶的项改变了
                mListener.onStickyChanged(oldGroupIndex, mCurrentStickyGroup);
            }
        } else {
            //回收旧的吸顶布局。
            recycle();
        }
    }

    /**
     * 注册adapter刷新监听
     */
    private void registerAdapterDataObserver(RVStickyHeaderAdapter adapter) {
        if (!isRegisterDataObserver) {
            isRegisterDataObserver = true;
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    updateStickyViewDelayed(1);
                }

                @Override
                public void onItemRangeChanged(int positionStart, int itemCount) {
                    updateStickyViewDelayed(2);
                }

                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    updateStickyViewDelayed(3);
                }

                @Override
                public void onItemRangeRemoved(int positionStart, int itemCount) {
                    updateStickyViewDelayed(4);
                }

            });
        }
    }

    private void updateStickyViewDelayed(int tag) {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                updateStickyView(true, tag);
            }
        }, 64);
    }

    /**
     * 判断是否需要先回收吸顶布局，如果要回收，则回收吸顶布局并返回null。
     * 如果不回收，则返回吸顶布局的ViewHolder。
     * 这样做可以避免频繁的添加和移除吸顶布局。
     *
     * @param viewType
     * @return
     */
    private VH recycleStickyView(int viewType) {
        if (mStickyLayout.getChildCount() > 0) {
            View view = mStickyLayout.getChildAt(0);
            int type = (int) view.getTag(VIEW_TAG_TYPE);
            if (type == viewType) {
                return (VH) view.getTag(VIEW_TAG_HOLDER);
            } else {
                recycle();
            }
        }
        return null;
    }

    /**
     * 回收并移除吸顶布局
     */
    private void recycle() {
        mCurrentStickyGroup = -1;
        if (mStickyLayout.getChildCount() > 0) {
            View view = mStickyLayout.getChildAt(0);
            mStickyViews.put((int) (view.getTag(VIEW_TAG_TYPE)), (VH) (view.getTag(VIEW_TAG_HOLDER)));
            mStickyLayout.removeAllViews();
        }
    }


    /**
     * 从缓存池中获取吸顶布局
     *
     * @param viewType 吸顶布局的viewType
     * @return
     */
    private VH getStickyViewByType(int viewType) {
        return mStickyViews.get(viewType);
    }

    /**
     * 计算StickyLayout的偏移量。因为如果下一个组的组头顶到了StickyLayout，
     * 就要把StickyLayout顶上去，直到下一个组的组头变成吸顶布局。否则会发生两个组头重叠的情况。
     *
     * @param gAdapter         适配器，用于查找下一个分组 Header
     * @param currentHeaderPos 当前吸顶的 Header position
     * @param stickyHeaderBoo  当前是否需要吸顶
     * @return 返回偏移量。
     */
    private float calculateOffset(RVStickyHeaderAdapter gAdapter, int currentHeaderPos, boolean stickyHeaderBoo) {
        if (!stickyHeaderBoo || currentHeaderPos == -1) {
            return 0;
        }
        // 通过 adapter 找到下一个分组的 Header position，而不是简单的 +1
        int nextHeaderPos = gAdapter.getNextStickyHeaderPosition(currentHeaderPos);
        if (nextHeaderPos == -1) {
            return 0;
        }

        RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            //获取下一个分组 Header 的 itemView
            View view = layoutManager.findViewByPosition(nextHeaderPos);
            if (view != null) {
                float off = view.getY() - mStickyLayout.getHeight();
//                Log.i(TAG, "头部固定 - 计算高度 off:" + off + "   y：" + view.getY() + " height:" + (mStickyLayout.getHeight()) + " nextHeader:" + nextHeaderPos);
                if (off < 0) {
                    return off;
                }
            }
        }
        return 0;
    }

    /**
     * 获取当前第一个显示的item .
     */
    private int getFirstVisibleItem() {
        int firstVisibleItem = -1;
        RecyclerView.LayoutManager layout = mRecyclerView.getLayoutManager();
        if (layout != null) {
            if (layout instanceof GridLayoutManager) {
                firstVisibleItem = ((GridLayoutManager) layout).findFirstVisibleItemPosition();
            } else if (layout instanceof LinearLayoutManager) {
                firstVisibleItem = ((LinearLayoutManager) layout).findFirstVisibleItemPosition();
            } else if (layout instanceof StaggeredGridLayoutManager) {
                int[] firstPositions = new int[((StaggeredGridLayoutManager) layout).getSpanCount()];
                ((StaggeredGridLayoutManager) layout).findFirstVisibleItemPositions(firstPositions);
                firstVisibleItem = getMin(firstPositions);
            }
        }
        return firstVisibleItem;
    }

    private int getMin(int[] arr) {
        int min = arr[0];
        for (int x = 1; x < arr.length; x++) {
            if (arr[x] < min) min = arr[x];
        }
        return min;
    }

    /**
     * 是否吸顶
     *
     * @return
     */
    public boolean isSticky() {
        return isSticky;
    }

    /**
     * 设置是否吸顶。
     *
     * @param sticky
     */
    public void setSticky(boolean sticky) {
//        Log.d(TAG, "头部固定 - 设置显示隐藏 sticky：" + sticky);
        if (mRecyclerView != null) {
            if (isSticky != sticky) {
                isSticky = sticky;
                if (mStickyLayout != null) {
                    if (isSticky) {
                        mStickyLayout.setVisibility(VISIBLE);
                        updateStickyView(false, 7);
                    } else {
                        recycle();
                        mStickyLayout.setVisibility(GONE);
                    }
                }
            }
        }
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if (mRecyclerView != null) {
            mRecyclerView.computeVerticalScrollOffset();
        }
        return super.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (mRecyclerView != null) {
            mRecyclerView.computeVerticalScrollRange();
        }
        return super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        if (mRecyclerView != null) {
            mRecyclerView.computeVerticalScrollExtent();
        }
        return super.computeVerticalScrollExtent();
    }

    @Override
    public void scrollBy(int x, int y) {
        if (mRecyclerView != null) {
            mRecyclerView.scrollBy(x, y);
        } else {
            super.scrollBy(x, y);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mRecyclerView != null) {
            mRecyclerView.scrollTo(x, y);
        } else {
            super.scrollTo(x, y);
        }
    }

    /**
     * 监听吸顶项改变
     *
     * @param l
     */
    public void setOnStickyChangedListener(OnStickyChangedListener l) {
        mListener = l;
    }

    public interface OnStickyChangedListener {
        /**
         * @param oldGroupIndex 旧的吸顶组下标，-1表示没有吸顶
         * @param newGroupIndex 新的吸顶组下标，-1表示没有吸顶
         */
        void onStickyChanged(int oldGroupIndex, int newGroupIndex);
    }
}
