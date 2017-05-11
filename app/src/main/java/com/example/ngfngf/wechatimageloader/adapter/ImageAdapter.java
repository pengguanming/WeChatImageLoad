package com.example.ngfngf.wechatimageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.ngfngf.wechatimageloader.R;
import com.example.ngfngf.wechatimageloader.bean.ImgContainer;
import com.example.ngfngf.wechatimageloader.utils.ImageLoader;

import java.util.List;

/**
 * Created by ngfngf on 2017/2/19.
 */

public class ImageAdapter extends BaseAdapter {
    private String mDirPath;
    private List<String> mImgPaths;
    private LayoutInflater mLayoutInflater;
    private int mScreenWidth;

    public ImageAdapter(Context context, List<String> mData, String dirPath) {
        mImgPaths = mData;
        mDirPath = dirPath;
        mLayoutInflater = LayoutInflater.from(context);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth=outMetrics.widthPixels;
    }

    @Override
    public int getCount() {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int pos, View convertView, ViewGroup parentContainer) {
        final ViewHolder mHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.iten_girdview, parentContainer, false);
            ImageButton imageButton = (ImageButton) convertView.findViewById(R.id.id_item_select);
            ImageView imageView = (ImageView) convertView.findViewById(R.id.id_item_image);
            mHolder = new ViewHolder(imageView, imageButton);
            convertView.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }
        //重置状态
        mHolder.mImageButton.setImageResource(R.mipmap.picture_unselected);
        mHolder.mImageView.setImageResource(R.mipmap.pictures_no);
        mHolder.mImageView.setColorFilter(null);

        mHolder.mImageView.setMaxWidth(mScreenWidth/3);
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImgPaths.get(pos), mHolder.mImageView);

        //设置图片的点击选中事件
        final String filePath = mDirPath + "/" + mImgPaths.get(pos);
        if (ImgContainer.sSelectedImg.contains(filePath)){
            mHolder.mImageView.setColorFilter(Color.parseColor("#77000000"));
            mHolder.mImageButton.setImageResource(R.mipmap.pictures_selected);
        }
        mHolder.mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //已经选中
                if ( ImgContainer.sSelectedImg.contains(filePath)) {
                    ImgContainer.sSelectedImg.remove(filePath);
                    mHolder.mImageView.setColorFilter(null);//Color.parseColor(null)
                    mHolder.mImageButton.setImageResource(R.mipmap.picture_unselected);
                } else {//未被选中
                    ImgContainer.sSelectedImg.add(filePath);
                    mHolder.mImageView.setColorFilter(Color.parseColor("#77000000"));
                    mHolder.mImageButton.setImageResource(R.mipmap.pictures_selected);
                }
                //notifyDataSetChanged();
            }
        });

        return convertView;
    }

    public class ViewHolder {
        ImageView mImageView;
        ImageButton mImageButton;

        public ViewHolder() {
        }

        public ViewHolder(ImageView imageView, ImageButton imageButton) {

            mImageView = imageView;
            mImageButton = imageButton;
        }
    }
}
