package com.example.survicebg.Notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.survicebg.Service.SavePictureService;

public class StopServiceReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 333;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, SavePictureService.class);
        context.stopService(service);
    }
}
