package com.example.ngfngf.wechatimageloader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.ngfngf.wechatimageloader.R;
import com.example.ngfngf.wechatimageloader.bean.FolderBean;
import com.example.ngfngf.wechatimageloader.utils.ImageLoader;

import java.util.List;

/**
 * Created by ngfngf on 2017/2/21.
 */

public class ListDirAdapter extends ArrayAdapter<FolderBean> {
    private LayoutInflater mLayoutInflater;
    private List<FolderBean> mDatas;

    public ListDirAdapter(Context context, List<FolderBean> list) {
        super(context,0,list);
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mHolder = null;
        if (convertView == null) {
            mHolder=new ViewHolder();
            convertView = mLayoutInflater.inflate(R.layout.item_popup_main, parent, false);
            mHolder.mImg = (ImageView) convertView.findViewById(R.id.id_id_item_image);
            mHolder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
            mHolder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
            convertView.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) convertView.getTag();
        }
        FolderBean bean = getItem(position);
        //重置图片，第二屏可用复用第一屏的控件，当第二屏加载时，显示第一屏的图片忽然变成第二屏的图片
        mHolder.mImg.setImageResource(R.mipmap.pictures_no);
        ImageLoader.getInstance().loadImage(bean.getFirstImgPath(), mHolder.mImg);
        mHolder.mDirCount.setText(String.valueOf(bean.getCount()));
        mHolder.mDirName.setText(bean.getName());
        return convertView;
    }

    private class ViewHolder {
        ImageView mImg;
        TextView mDirName;
        TextView mDirCount;
    }
}
