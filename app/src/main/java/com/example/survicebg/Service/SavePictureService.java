package com.example.survicebg.Service;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.survicebg.MainActivity;
import com.example.survicebg.Notification.NotificationReceiver;
import com.example.survicebg.Notification.StopServiceReceiver;
import com.example.survicebg.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.Provider;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.survicebg.Service.ApplicationClass.ACTION_PREVIOUS;
import static com.example.survicebg.Service.ApplicationClass.CHANNEL_ID_2;

public class SavePictureService extends Service {
    IBinder myBinder = new MyBinder();
    private ClipboardManager mClipboardManager;
    private static Call mCall;
    private static final OkHttpClient mOkHttpClient = new OkHttpClient();

    @Override
    public void onCreate() {
        super.onCreate();
        mClipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        mClipboardManager.addPrimaryClipChangedListener(
                mOnPrimaryClipChangedListener);
        showNotification();
        System.out.println("Service started running..");
    }
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//
//        if (mClipboardManager != null) {
//            mClipboardManager.removePrimaryClipChangedListener(
//                    mOnPrimaryClipChangedListener);
//        }
//    }
    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {

                    String charSequence = mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
                    String s1 = urlGetter(charSequence)+"?__a=1";

                    fetchJSON(s1);
                }
            };

    public void fetchJSON(String Url) {
        Log.d("URL",Url);
        final Request request = new Request.Builder()
                .url(Url)
                .method("GET", null)
                .build();
        mCall = mOkHttpClient.newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Error",e.getMessage());
                Toast.makeText(SavePictureService.this, "Unable to find Post", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    final JSONObject jsonObject = new JSONObject(response.body().string()).getJSONObject("graphql").getJSONObject("shortcode_media");
                    Log.d("RES",jsonObject.toString());
                    String type = jsonObject.getString("__typename");
                    if(type.equals("GraphImage") || type.equals("GraphSidecar")){
                        String imageUrl = jsonObject.getString("display_url");
                        downloadImage(imageUrl);
                    }else{
                        String videoUrl = jsonObject.getString("video_url");
                        downloadVideo(videoUrl);
                    }

                }catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(SavePictureService.this, "Unable to find Post", Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
    }

    private void downloadVideo(String videoUrl) {
        String fileName = UUID.randomUUID().toString() + ".mp4";
        File direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
                + "/" + "SBG" + "/");
        if (!direct.exists()) {
            direct.mkdir();
        }
        DownloadManager downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(videoUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(fileName)
                .setMimeType("video/mp4")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,
                        File.separator + "SBG" + File.separator + fileName);
        Long reference = downloadManager.enqueue(request);
    }

    public void downloadImage(String imageUrl) {
        Log.d("ImageDownload","calling");
        String fileName = UUID.randomUUID().toString() + ".png";
        File direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                + "/" + "SBG" + "/");
        if (!direct.exists()) {
            direct.mkdir();
        }
//
        DownloadManager downloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(imageUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(fileName)
                .setMimeType("image/jpeg")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,
                        File.separator + "SBG" + File.separator + fileName);
        Long reference = downloadManager.enqueue(request);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }
    public class MyBinder extends Binder {
        public SavePictureService getService(){
            return SavePictureService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;

    }
    public static String urlGetter(String url){
        String newString="";
        int i=0;
        for(char k:url.toCharArray()){
            if(i==5){
                System.out.println(newString);
                break;
            }
            if(k == '/'){
                i++;
            }
            newString+=k;

        }
        return newString;
    }
    public void showNotification(){
        Bitmap icon = BitmapFactory.decodeResource(getResources(),R.drawable.ic_dog);
        Intent intent = new Intent(this, StopServiceReceiver.class);
        PendingIntent prevPending = PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        String s = "Stop";
        Notification notification = new NotificationCompat.Builder(this,CHANNEL_ID_2)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(icon)
                .setContentTitle("ISave is ON")
                .setContentText("Copy link to fast download")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setContentIntent(contentIntent)
                .addAction(R.mipmap.ic_launcher,"Stop",prevPending)
                .build();
        startForeground(2,notification);
    }
}
