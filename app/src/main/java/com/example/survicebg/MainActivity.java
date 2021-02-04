package com.example.survicebg;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.CompoundButton;

import com.example.survicebg.ServerUtils.ResponseListener;
import com.example.survicebg.ServerUtils.Servers;
import com.example.survicebg.Service.SavePictureService;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.app.Service.START_STICKY;
import static com.example.survicebg.Service.SavePictureService.urlGetter;

public class MainActivity extends AppCompatActivity{
    SavePictureService savePictureService;
    public static final int REQUEST_CODE = 1;
    String url;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private SwitchMaterial switchMaterial;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
        receiverUrl();
        bindId();
    }

    private void bindId() {
        switchMaterial = findViewById(R.id.switchMt);
        sharedPreferences = getSharedPreferences("ServiceDetail", Context.MODE_PRIVATE);
        boolean isChecked = sharedPreferences.getBoolean("isChecked",false);
        Log.d("ServiceStatus",String.valueOf(isChecked));
        switchMaterial.setChecked(isChecked);
        switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked && !isMyServiceRunning(SavePictureService.class)){
                    startService(new Intent(MainActivity.this,SavePictureService.class));
                }
                if (!isChecked && isMyServiceRunning(SavePictureService.class)){
                    stopService(new Intent(MainActivity.this,SavePictureService.class));
                }
                changeSharedPref(isChecked);
            }
        });
        if(isChecked && !isMyServiceRunning(SavePictureService.class)){
            startService(new Intent(this,SavePictureService.class));
        }
        if (!isChecked && isMyServiceRunning(SavePictureService.class)){
            stopService(new Intent(this,SavePictureService.class));
        }
    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void changeSharedPref(boolean isChecked) {
        editor = sharedPreferences.edit();
        editor.putBoolean("isChecked",isChecked);
        editor.apply();
    }

    private void receiverUrl() {
        Intent intent2 = getIntent();
        String action = intent2.getAction();
        String type = intent2.getType();
        if(type!=null && type.equals("text/plain")){
            String sharedText = intent2.getStringExtra(Intent.EXTRA_TEXT);
            url = urlGetter(sharedText)+"?__a=1";
            Log.d("GETTING",url);
        }
        if(url!=null){
            fetchJSON(url);
        }
    }

    private String getIdFromUrl(String sharedText) {
        String newString="";
        int i=0;
        for(char k: sharedText.toCharArray()){
            if(k == '/'){
                i++;
            }

            if (i==5){
                return newString;
            }

            if(i==4 && k!='/'){
                newString+=k;
            }
        }
        return newString;
    }

    private void fetchImageVideo(String u){
        fetchData(u, new ResponseListener() {
            @Override
            public void responseObject(Object data, String aa) throws JSONException {
                Log.d("Res",data.toString());
                final JSONObject jsonObject = new JSONObject(data.toString()).getJSONObject("graphql").getJSONObject("shortcode_media");

                String type = jsonObject.getString("__typename");
                if(type.equals("GraphImage")){
                    String imageUrl = jsonObject.getString("display_url");
                    downloadImage(imageUrl);
                }else{
                    String videoUrl = jsonObject.getString("video_url");
                    downloadVideo(videoUrl);
                }
            }
        });
    }
    private static void fetchData(String u, ResponseListener responseListener){
        Servers.getInstance().fetchData(u,responseListener);
    }
    public void fetchJSON(String Url) {
        Call mCall;
        OkHttpClient mOkHttpClient= new OkHttpClient();
        final Request request = new Request.Builder()
                .url(Url)
                .method("GET", null)
                .build();
        mCall = mOkHttpClient.newCall(request);
        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("Error",e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try{
                    String res = response.body().string();
                    Log.d("RES",res);
                    final JSONObject jsonObject = new JSONObject(res).getJSONObject("graphql").getJSONObject("shortcode_media");

                    String type = jsonObject.getString("__typename");
                    if(type.equals("GraphImage")){
                        String imageUrl = jsonObject.getString("display_url");
                        downloadImage(imageUrl);
                    }else{
                        String videoUrl = jsonObject.getString("video_url");
                        downloadVideo(videoUrl);
                    }

                }catch (Exception e) {
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

    private void permission() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
        }else{
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==REQUEST_CODE){
            if (grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
            }
        }
    }

//    @Override
//    public void onServiceConnected(ComponentName name, IBinder service) {
//        SavePictureService.MyBinder myBinder = (SavePictureService.MyBinder) service;
//        savePictureService = myBinder.getService();
//    }
//
//    @Override
//    public void onServiceDisconnected(ComponentName name) {
//        savePictureService =null;
//    }

    @Override
    protected void onResume() {
//        Intent intent = new Intent(this, SavePictureService.class);
//        bindService(intent,this,BIND_AUTO_CREATE);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unbindService(this);
    }
}