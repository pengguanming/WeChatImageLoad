package com.example.ngfngf.wechatimageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ngfngf.wechatimageloader.adapter.ImageAdapter;
import com.example.ngfngf.wechatimageloader.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ListImageDirPopuWindow.OnDIrSelectedListener {
    private ImageAdapter mImageAdapter;
    private GridView mGridView;
    private List<String> mImgs;
    private RelativeLayout mBottomLy;
    private TextView mDirName;
    private TextView mDirCount;
    private ProgressDialog mProgressDialog;
    private File mCurrentDir;
    private int mMaxCount;
    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private static final int DATA_LOADED = 0x110;
    private ListImageDirPopuWindow mListImageDirPopuWindow;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DATA_LOADED) {
                mProgressDialog.dismiss();
                //绑定数据到view中
                data2view();
                initDirPopuWindow();
            }
        }
    };

    private void initDirPopuWindow() {
        mListImageDirPopuWindow = new ListImageDirPopuWindow(this, mFolderBeans);
        //点击显示popuWindow时，背景变暗，消失时，背景变亮
        mListImageDirPopuWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        //设置接口popuWindow的接口回调
        mListImageDirPopuWindow.setOnDIrSelectedListener(MainActivity.this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDatas();
        initEnvet();
    }

    private void data2view() {
        if (mCurrentDir == null) {
            Toast.makeText(MainActivity.this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());
        mImageAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImageAdapter);
        mDirCount.setText(String.valueOf(mMaxCount));
        mDirName.setText(mCurrentDir.getName());
    }

    //利用COntentProvider扫描手机中的图片
    private void initDatas() {
        //检查SDCard是否可用
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "SDCard不可用！", Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore.Images.Media.MIME_TYPE + "= ?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
                //存储dirPath，防止重复遍历
                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    //文件当前路径
                    String paht = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //文件的父路径
                    File parentFile = new File(paht).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(paht);
                    }

                    if (parentFile.list() == null) {
                        continue;
                    }
                    //返回父目录下的图片文件数量
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File file, String s) {
                            if (s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);
                    //遍历获取最多图片的文件夹路径
                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                //关闭游标
                cursor.close();
                //通知Handler扫描图片完成
                mHandler.sendEmptyMessage(DATA_LOADED);
            }
        }.start();
    }

    private void initView() {
        mGridView = (GridView) findViewById(R.id.id_girgView);
        mBottomLy = (RelativeLayout) findViewById(R.id.bottom_ly);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
    }

    private void initEnvet() {
        mBottomLy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //设置popuWindow的动画效果
                mListImageDirPopuWindow.setAnimationStyle(R.style.dir_popuwindow_anim);
                //显示在mBottomLy上面
                //相对某个控件的位置（正左下方），无偏移  showAsDropDown(View anchor, int xoff, int yoff)：
                //相对于父控件的位置（例如正中央Gravity.CENTER，下方Gravity.BOTTOM等），可以设置偏移或无偏移
                // showAtLocation(View parent, int gravity, int x, int y)：
                mListImageDirPopuWindow.showAsDropDown(mBottomLy, 0, 0);
                lightOff();
            }
        });
    }

    //内容区域变暗
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.4f;
        getWindow().setAttributes(lp);
    }

    //内容区域变亮
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }


    //回调popuWindow的item点击事件，更新路径及其图片
    @Override
    public void onSelected(FolderBean bean) {
        mCurrentDir = new File(bean.getDir());
        mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png")) {
                    return true;
                }
                return false;
            }
        }));
        mImageAdapter = new ImageAdapter(MainActivity.this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImageAdapter);
        mDirName.setText(mCurrentDir.getName());
        mDirCount.setText(String.valueOf(mImgs.size()));
        //隐藏PopuWinidow
        mListImageDirPopuWindow.dismiss();
    }
}
