package com.sanj.cabme.broadcast;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.sanj.cabme.R;
import com.sanj.cabme.wrapper.Wrapper;

import java.util.Random;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyBroadCastReceiver extends BroadcastReceiver {

    private static final String NOTIFICATION_CHANNEL_ID = "10001";


    public void onReceive(Context context, Intent intent) {
        int REQUEST_CODE=new Random().nextInt();
        if (intent!=null){
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, MyService.class);
                context.startService(serviceIntent);
            } else {
                String description = intent.getStringExtra("description");
                Intent i = new Intent();
                PendingIntent pIntent = PendingIntent.getActivity(context, REQUEST_CODE, i, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
                bigTextStyle.setBigContentTitle("CABME Ride reminder");
                bigTextStyle.bigText(description);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
                        .setStyle(bigTextStyle)
                        .setSmallIcon(R.drawable.app_logo)
                        .setContentIntent(pIntent)
                        .setAutoCancel(false)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);
                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BabyCare", NotificationManager.IMPORTANCE_HIGH);
                    assert mNotificationManager != null;
                    mNotificationManager.createNotificationChannel((notificationChannel));
                    mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
                }
                assert mNotificationManager != null;
                mNotificationManager.notify(0, mBuilder.build());
            }
        }
    }
}
