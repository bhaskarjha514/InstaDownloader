package com.example.survicebg.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.survicebg.R;
import com.example.survicebg.ServerUtils.FetchData;
import com.example.survicebg.ServerUtils.ResponseListener;

import org.json.JSONException;

import java.io.File;
import java.util.UUID;

import static com.example.survicebg.Service.SavePictureService.urlGetter;

public class ShareActivity extends AppCompatActivity {
    private Thread fetchDataThread;
    private Dialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        Intent intent = getIntent();
        receiverUrl(this,intent);
    }
    private void fetchDataThreadCall(String url){
        fetchDataThread = new Thread(){
            @Override
            public void run() {
                super.run();
                FetchData.getInstance().fetchJson(ShareActivity.this, url, new ResponseListener() {
                    @Override
                    public void responseObject(Object data, String type) throws JSONException {
                        cancelDialog(dialog);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (data.toString().equals("error")){
                                    Toast.makeText(ShareActivity.this, "Unable to find Post", Toast.LENGTH_SHORT).show();
                                    finish();
                                }else {
                                    showDownloadingDialog(data.toString(),type);
                                }
                            }
                        });

                    }
                });
            }
        };
        fetchDataThread.start();
    }
    private void receiverUrl(Context context, Intent intent) {
        String url = null;
        String action = intent.getAction();
        String type = intent.getType();
        if(type!=null && type.equals("text/plain")){
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            url = urlGetter(sharedText)+"?__a=1";
            Log.d("GETTING",url);
        }
        if(url!=null){
            Log.d("GETTINGURL",url);
//            fetchJSON(url);
            fetchDataThreadCall(url);
            showDownloadDialog(false);
        }
    }
    private void showDownloadDialog(boolean gotUrl){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.downloading_dialog);
        ProgressBar progressBar = dialog.findViewById(R.id.progress_circular);
        TextView hideTv = dialog.findViewById(R.id.hideTv);
        TextView cancelTv = dialog.findViewById(R.id.cancel_button);
        if (!gotUrl){
            hideTv.setVisibility(View.GONE);
        }else{
            hideTv.setVisibility(View.VISIBLE);
        }
        dialog.show();
        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
    }
    private void showDownloadingDialog(String url,String type){
        Log.d("DownloadUrl",url);
        String fileName = null;
        File direct = null;
        Uri downloadUri = Uri.parse(url);

        DownloadManager downloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(downloadUri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


        if(type.equals("GraphImage")){
            fileName = UUID.randomUUID().toString() + ".png";
            direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                    + "/" + "SBG" + "/");
            request.setTitle(fileName);
            request.setMimeType("image/png");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,File.separator+"ISave"+File.separator+fileName);

        }if(type.equals("GraphVideo")){
            fileName = UUID.randomUUID().toString() + ".mp4";
            direct = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath()
                    + "/" + "SBG" + "/");
            request.setTitle(fileName);
            request.setMimeType("video/mp4");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,File.separator+"ISave"+File.separator+fileName);
        }
        if (!direct.exists()) {
            direct.mkdir();
        }
        final long downloadId = downloadManager.enqueue(request);
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.downloading_dialog);
        ProgressBar progressBar = dialog.findViewById(R.id.progress_circular);
        TextView hideTv = dialog.findViewById(R.id.hideTv);
        TextView cancelTv = dialog.findViewById(R.id.cancel_button);
        Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finish();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                dialog.show();
                boolean downloading = true;
                while (downloading){
                    DownloadManager.Query q = new DownloadManager.Query();
                    q.setFilterById(downloadId);
                    Cursor cursor = downloadManager.query(q);
                    cursor.moveToFirst();
                    int bytes_downloaded = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }
                    final int dl_progress = (int) ((bytes_downloaded * 100l) / bytes_total);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress((int)dl_progress);
                        }
                    });
                    cursor.close();
                }
            }
        }).start();
    }
    private void cancelDialog(Dialog dialog){
        dialog.dismiss();
    }
}