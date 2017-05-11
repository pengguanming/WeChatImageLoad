package com.example.ngfngf.wechatimageloader.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by ngfngf on 2017/2/13.
 */

public class ImageLoader {
    private static ImageLoader sImageLoader;//单例持有引用
    private LruCache<String, Bitmap> mLruCache;//二级缓存图片的临时容器
    private ExecutorService mThreadPool;//线程池
    //任务队列
    private LinkedList<Runnable> mTaskQueues;
    //后台轮询线程
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;
    //同步信号量
    private volatile Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);
    //保证线程池并发数量，且执行策略的顺序
    private volatile Semaphore mSemaphoreThreadPool;
    //UI线程的handler
    private Handler mUIHandler;
    //默认线程数
    private static final int DEFAULT_THREAD_COUNT = 1;
    //默认图片加载策略类型
    private Type mType = Type.LIFO;


    //图片加载策略（先进先出，后进先出）
    public enum Type {
        FIFIO, LIFO;
    }

    private ImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    //初始化
    private void init(int threadCount, Type type) {
        mPoolThread = new Thread() {
            @Override
            public void run() {
                //初始化后台轮询器
                Looper.prepare();//子线程开启消息循环
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池取出任务进行执行
                        mThreadPool.execute(getTask());
                        try {
                            //保证线程池的无空闲线程时,阻塞mTaskQueues,不影响加载策略
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //mPoolThreadHandler初始化完毕,释放信号量,并发addTask方法
                //阻塞addTask,防止mPoolThreadHandler未创建就调用造成的空指针
                mSemaphorePoolThreadHandler.release();
                //启动轮询器在后台进行轮询
                Looper.loop();
            }
        };
        mPoolThread.start();

        //初始化LruCache,获取最大可用内存
        int MaxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = MaxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        //创建请求队列
        mTaskQueues = new LinkedList<Runnable>();
        //设置加载策略类型
        mType = type;
        //初始化线程信号量
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    //从任务队列取出一个方法
    private Runnable getTask() {
        if (mType == Type.FIFIO) {
            return mTaskQueues.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueues.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance() {
        if (sImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (sImageLoader == null) {
                    sImageLoader = new ImageLoader(DEFAULT_THREAD_COUNT, Type.LIFO);
                }
            }
        }
        return sImageLoader;
    }

    public static ImageLoader getInstance(int threadCount, Type type) {
        if (sImageLoader == null) {
            synchronized (ImageLoader.class) {
                if (sImageLoader == null) {
                    sImageLoader = new ImageLoader(threadCount, type);
                }
            }
        }
        return sImageLoader;
    }

    //传入图片路径，ImageView加载图片
    public void loadImage(final String path, final ImageView imageView) {
        //  提高组件复用，防止混乱
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    //获得图片，为ImageView回调设置图片
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap mBitmap = holder.getBitmap();
                    ImageView mImageView = holder.getImageView();
                    String mPath = holder.getPath();
                    //imageView在复用时可能没变化，但path变化l，所以将path与对应的getTag进行比对
                    if (mImageView.getTag().toString().equals(mPath)) {
                        mImageView.setImageBitmap(mBitmap);
                    }
                }
            };
        }
        //根据path在缓存获取Bitmap
        Bitmap bitmap = getBitmapFromLru(path);
        if (bitmap != null) {
            refreshBitmap(bitmap, imageView, path);
        } else {
            addTask(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    //图片压缩
                    //1、获取图片显示的大小
                    ImageSize mImageSize = getImageViewSize(imageView);
                    //2、压缩图片
                    Bitmap b = decodeSampleBitmapFromPath(path, mImageSize.getWidth(), mImageSize.getHeight());
                    //3、把图片加载到缓存
                    addBitmapToLruCache(path, b);
                    refreshBitmap(b, imageView, path);
                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(Bitmap b, ImageView imageView, String path) {
        Message message = Message.obtain();
        message.obj = new ImgBeanHolder(b, imageView, path);
        mUIHandler.sendMessage(message);
    }

    //将图片加载到LruCache中
    private void addBitmapToLruCache(String path, Bitmap b) {
        //判断LruCache中之前是否已经有b，若无则添加
        if (getBitmapFromLru(path) == null) {
            if (b != null) {
                mLruCache.put(path, b);
            }
        }

    }

    //根据图片需要显示的宽高进行压缩
    private Bitmap decodeSampleBitmapFromPath(String path, int width, int height) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        //获取图片宽高，并不把图片加载到内存中
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInSampleSize(options, width, height);
        //使用获取到的inSampleSize再次解析图片
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    //根据需求的宽和高以及图片的宽高计算SampleSize；
    private int caculateInSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqwidth || height > reqheight) {
            int widthRadio = Math.round(width * 1.0f / reqwidth);
            int heightRadio = Math.round(height * 1.0f / reqheight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    //根据ImageView获取适当的宽高
    private ImageSize getImageViewSize(ImageView imageView) {
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        //获取imageView的实际宽度,当imageView没加载到布局里时
        int width = imageView.getWidth();
        if (width <= 0) {
            //获取imageView在layout中声明的宽度
            width = lp.width;
        }
        //可能设置Wrap_content||match_parent,则检查最大宽度
        if (width <= 0) {
            width = imageView.getMaxWidth();
        }
        //设置为imageView屏幕的宽度
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }

        //获取imageView的实际高度
        int height = imageView.getHeight();
        if (height <= 0) {
            //获取imageView在layout中声明的高度
            height = lp.height;
        }
        //可能设置Wrap_content||match_parent,则检查最大高度
        if (height <= 0) {
            height = imageView.getMaxHeight();
        }
        //设置为屏幕的高度
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        return new ImageSize(width, height);
    }

    //通过反射获取ImageView的某个属性
    public static int getImageViewFeildValue(Object obj, String fieldName) {
        int value = 0;
        try {
            Field f = ImageView.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            int fieldValue = f.getInt(obj);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueues.add(runnable);
        try {
            if (mPoolThreadHandler == null)
                mSemaphorePoolThreadHandler.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mPoolThreadHandler.sendEmptyMessage(0x110);
    }

    private class ImageSize {
        int width;
        int height;

        public ImageSize() {
        }

        public ImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
    }

    private Bitmap getBitmapFromLru(String path) {
        return mLruCache.get(path);
    }

    private class ImgBeanHolder {
        private Bitmap bitmap;
        private ImageView imageView;
        private String path;

        public ImgBeanHolder() {
        }

        public ImgBeanHolder(Bitmap bitmap, ImageView imageView, String path) {
            this.bitmap = bitmap;
            this.imageView = imageView;
            this.path = path;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public ImageView getImageView() {
            return imageView;
        }

        public void setImageView(ImageView imageView) {
            this.imageView = imageView;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
