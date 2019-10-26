package com.example.multithreademo;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.WeakHashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String DOWNLOAD_URL = "https://desk-fd.zol-img.com.cn/t_s1920x1080c5/g5/M00/07/07/ChMkJlXw8QmIO6kEABYK" +
            "y-RYbJ4AACddwM0pT0AFgrj303.jpg\n";
//    private static final String DOWNLOAD_URL ="https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1572088269692&di=" +
//        "d0d6dddeaf30fb82a17f4bc09e171bff&imgtype=0&src=http%3A%2F%2Fpics0.baidu.com%2Ffeed%2F3812b31bb051f819a76c16cc83073be92f73e726.jpeg%3Ftoken%3D339613f17f9d7745b34fbd7f48eac0f8%26s%3D61D9AB6680B3A5846B44958803003091";

    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;


    private CalculateThread calculateThread;

    private MyHandler myHandler = new MyHandler( this );
    private MyUiHandler uiHandler = new MyUiHandler( this );


    //进度条常量
    private final static int START_NUM = 1;
    private final static int ADDING_NUM = 2;
    private final static int ENDING_NUM = 3;
    private final static int CANCEL_NUM = 4;


    ProgressBar progressBar;

    private TextView textView;

    private Button btn_multi;
    private Button btn_asynchronous;
    private Button btn_handler;
    private Button btn_async_task;
    private Button btn_other;

    private ImageView iv_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        progressBar = findViewById( R.id.progress_bar_h );

        textView = findViewById( R.id.tv_textView );

        btn_multi = findViewById( R.id.btn_multi );
        btn_asynchronous = findViewById( R.id.btn_asynchronous );
        btn_handler = findViewById( R.id.btn_handler );
        btn_async_task = findViewById( R.id.btn_async_task );
        btn_other = findViewById( R.id.btn_other );

        iv_image = findViewById( R.id.iv_image );


        btn_multi.setOnClickListener( this );
        btn_asynchronous.setOnClickListener( this );
        btn_handler.setOnClickListener( this );
        btn_async_task.setOnClickListener( this );
        btn_other.setOnClickListener( this );

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {


//            多线程计算
            case R.id.btn_multi:
                calculateThread = new CalculateThread();
                calculateThread.start();

                break;

//            异步任务计算
            case R.id.btn_asynchronous:
                new MyAsyncTask(this).execute(100);
                break;

//            Handler下载图片

            case R.id.btn_handler:

                new Thread( new DownloadImageFetcher( DOWNLOAD_URL ) ).start();

                break;
//            AsyncTask下载图片
            case R.id.btn_async_task:
                new DownloadImage(this).execute(DOWNLOAD_URL);
                break;

            case R.id.btn_other:

                break;

        }


    }


    //自定义静态类
    static class MyHandler extends Handler {

        private WeakReference <Activity> ref;

        public MyHandler(Activity activity) {
            this.ref = new WeakReference <>( activity );

        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage( msg );

            MainActivity activity = (MainActivity) ref.get();

            if (activity == null) {

                return;

            }
            switch (msg.what) {
                case START_NUM:
                    activity.progressBar.setVisibility( View.VISIBLE );
                    break;

                case ADDING_NUM:
                    activity.progressBar.setProgress( msg.arg1 );
                    activity.textView.setText( "计算已完成" + msg.arg1 + "%" );

                    break;

                case ENDING_NUM:

                    activity.progressBar.setVisibility( View.GONE );
                    activity.textView.setText( "计算已完成，结果为：" + msg.arg1 );
                    activity.myHandler.removeCallbacks( activity.calculateThread );
                    break;


                case CANCEL_NUM:
                    activity.progressBar.setProgress( 0 );
                    activity.progressBar.setVisibility( View.GONE );
                    activity.textView.setText( "计算已取消" );
                    break;


            }


        }


    }


    class CalculateThread extends Thread {

        @Override
        public void run() {
            int result = 0;
            boolean isCancel = false;
            myHandler.sendEmptyMessage( START_NUM );

            for (int i = 0; i <= 100; i++) {

                try {
                    Thread.sleep( 100 );
                    result += i;

                } catch (InterruptedException e) {

                    e.printStackTrace();

                    isCancel = true;
                    break;
                }

                if (i % 5 == 0) {
                    Message msg = Message.obtain();

                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    myHandler.sendMessage( msg );

                }

            }
            if (!isCancel) {

                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage( msg );
            }

        }
    }




    static class MyUiHandler extends Handler {

        private WeakReference <Activity> ref;

        public MyUiHandler(Activity activity) {
            this.ref = new WeakReference <>( activity );

        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage( msg );

            MainActivity activity = (MainActivity) ref.get();

            if (activity == null) {

                return;

            }
            switch (msg.what) {
                case MSG_SHOW_PROGRESS:

                    activity.progressBar.setVisibility( View.VISIBLE );
                    break;

                case MSG_SHOW_IMAGE:

                    activity.progressBar.setVisibility( View.GONE );
                    activity.iv_image.setImageBitmap( (Bitmap) msg.obj );

                    break;


            }


        }


    }

    private class DownloadImageFetcher implements Runnable {

        private String imgUrl;


        public DownloadImageFetcher(String strUrl) {
            this.imgUrl = strUrl;

        }

        @Override
        public void run() {


            InputStream in = null;

            uiHandler.obtainMessage( MSG_SHOW_PROGRESS ).sendToTarget();


            try {
                URL url = new URL( imgUrl );
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                in = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream( in );

                Message msg = uiHandler.obtainMessage();
                msg.what = MSG_SHOW_IMAGE;
                msg.obj = bitmap;
                uiHandler.sendMessage( msg );

            } catch (java.io.IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {

                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

//    1. 创建AsyncTask子类继承AsyncTask类
//    2.为3个泛型参数指定类型；若不使用，可用java.lang.Void类型代替，
//   输入参数 = Integer类型、执行进度 = Integer类型、执行结果 = Integer类型

    static class MyAsyncTask extends AsyncTask<Integer, Integer, Integer> {

        private WeakReference<AppCompatActivity> ref;

        public MyAsyncTask(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        // 执行线程任务前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setVisibility(View.VISIBLE);
        }

        // 接收输入参数、执行任务中的耗时操作、返回线程任务执行的结果
        @Override
        protected Integer doInBackground(Integer... integers) {
            int sleep = integers[0];
            int result = 0;

            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(sleep);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (i % 5 == 0) {
                    publishProgress(i);
                }

                if (isCancelled()) {
                    break;
                }
            }
            return result;
        }


        // 在主线程中显示线程任务执行的进度
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setProgress(values[0]);
            activity.textView.setText("计算已完成" + values[0] + "%");
        }
        // 接收线程任务执行结果、将执行结果显示到UI组件
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            MainActivity activity = (MainActivity) this.ref.get();
            activity.textView.setText("已计算完成，结果为：" + result);
            activity.progressBar.setVisibility(View.GONE);
        }

        // 将异步任务设置为：取消状态
        @Override
        protected void onCancelled() {
            super.onCancelled();

            MainActivity activity = (MainActivity) this.ref.get();
            activity.textView.setText("计算已取消");

            activity.progressBar.setProgress(0);
            activity.progressBar.setVisibility(View.GONE);
        }
    }


    //下载图片的线程
    static class DownloadImage extends AsyncTask<String, Bitmap, Bitmap> {
        private WeakReference <AppCompatActivity> ref;

        public DownloadImage(AppCompatActivity activity) {
            this.ref = new WeakReference <>( activity );
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            return downloadImage( url );
        }

        private Bitmap downloadImage(String strUrl) {
            InputStream stream = null;
            Bitmap bitmap = null;

            MainActivity activity = (MainActivity) this.ref.get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                URL url = new URL( strUrl );
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int totalLen = connection.getContentLength();
                if (totalLen == 0) {
                    activity.progressBar.setProgress( 0 );
                }

                if (connection.getResponseCode() == 200) {
                    stream = connection.getInputStream();
//                    bitmap = BitmapFactory.decodeStream(stream);

                    int len = -1;
                    int progress = 0;
                    byte[] tmps = new byte[1024];
                    while ((len = stream.read( tmps )) != -1) {
                        progress += len;
                        activity.progressBar.setProgress( progress );
                        bos.write( tmps, 0, len );
                    }
                    bitmap = BitmapFactory.decodeByteArray( bos.toByteArray(), 0, bos.size() );
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bitmap;
        }


        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            MainActivity activity = (MainActivity) this.ref.get();
            if (bitmap != null) {
                activity.iv_image.setImageBitmap(bitmap);
            }
        }
    }


    }







