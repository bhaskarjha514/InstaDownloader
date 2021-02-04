package com.example.survicebg.Notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.survicebg.Service.SavePictureService;

import static com.example.survicebg.Service.ApplicationClass.ACTION_PREVIOUS;
import static com.example.survicebg.Service.SavePictureService.urlGetter;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String actionName = intent.getAction();
        Intent serviceIntent = new Intent(context, SavePictureService.class);
        if (actionName!=null){
            switch (actionName){
                case ACTION_PREVIOUS:
                    serviceIntent.putExtra("ActionName","playPause");
                    context.startService(serviceIntent);
                    break;
            }
        }
        receiverUrl(context,intent);
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
        }
    }
}
