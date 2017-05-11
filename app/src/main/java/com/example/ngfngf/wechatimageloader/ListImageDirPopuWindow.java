package com.example.ngfngf.wechatimageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.example.ngfngf.wechatimageloader.adapter.ListDirAdapter;
import com.example.ngfngf.wechatimageloader.bean.FolderBean;

import java.util.List;

/**
 * Created by ngfngf on 2017/2/21.
 */

public class ListImageDirPopuWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertVIew;
    private ListView mListView;
    private List<FolderBean> mDatas;
    private ListDirAdapter mListDirAdapter;
    private OnDIrSelectedListener mOnDIrSelectedListener;

    public interface OnDIrSelectedListener {
        void onSelected(FolderBean bean);
    }

    public ListImageDirPopuWindow(Context context, List<FolderBean> datas) {
        super(context);
        mDatas = datas;
        calWidthAndHeight(context);
        mConvertVIew = LayoutInflater.from(context).inflate(R.layout.popup_main, null);
        setContentView(mConvertVIew);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        //外部可触摸,点击消失
        setOutsideTouchable(true);
        // 这个是为了点击“返回Back”也能使其消失，并且并不会影响你的背景
        setBackgroundDrawable(new BitmapDrawable());
        //拦截Touch事件,若在该控件外则让其消失
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initView(context);
        initEvevt();
    }

    private void initEvevt() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mOnDIrSelectedListener != null) {
                    mOnDIrSelectedListener.onSelected(mDatas.get(i));
                }
            }
        });
    }

    public void setOnDIrSelectedListener(OnDIrSelectedListener onDIrSelectedListener) {
        mOnDIrSelectedListener = onDIrSelectedListener;
    }

    private void initView(Context context) {
        mListView = (ListView) mConvertVIew.findViewById(R.id.id_list_dir);
        mListDirAdapter = new ListDirAdapter(context, mDatas);
        mListView.setAdapter(mListDirAdapter);
    }

    //计算popuWindow的宽高
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mWidth = outMetrics.widthPixels;
        mHeight = (int) (outMetrics.heightPixels * 0.7);
    }
}
