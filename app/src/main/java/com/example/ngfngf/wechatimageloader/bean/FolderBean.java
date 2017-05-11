package com.example.ngfngf.wechatimageloader.bean;

/**
 * Created by ngfngf on 2017/2/15.
 */

public class FolderBean {
    private String dir;//文件夹路径
    private String firstImgPath;//第一张图片的路径
    private String name;//文件夹名称
    private int count;//图片数量

    public FolderBean() {
    }

    public FolderBean(String dir, String firstImgPath, String name, int count) {
        this.dir = dir;
        this.firstImgPath = firstImgPath;
        this.name = name;
        this.count = count;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf=this.dir.lastIndexOf("/");
        this.name=this.dir.substring(lastIndexOf+1);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }



    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
