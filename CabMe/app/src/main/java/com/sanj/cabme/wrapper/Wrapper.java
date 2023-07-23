package com.sanj.cabme.wrapper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sanj.cabme.R;
import com.sanj.cabme.broadcast.MyBroadCastReceiver;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Objects;
import java.util.Random;

import dmax.dialog.SpotsDialog;

public class Wrapper {
    public static String authenticatedUniqueNumber;
    public static String driverNID;
    public static Boolean isDeleted = false;

    public void errorToast(String text, Context context) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            View view = toast.getView();
            view.getBackground().setColorFilter(context.getResources().getColor(R.color.red_700), PorterDuff.Mode.SRC_IN);
            toast.setGravity(Gravity.TOP, 0, 0);
            TextView textView = view.findViewById(android.R.id.message);
            textView.setTextColor(context.getResources().getColor(R.color.white));
        }
        toast.show();
    }

    public void successToast(String text, Context context) {
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            View view = toast.getView();
            view.getBackground().setColorFilter(context.getResources().getColor(R.color.green_700), PorterDuff.Mode.SRC_IN);
            toast.setGravity(Gravity.TOP, 0, 0);
            TextView textView = view.findViewById(android.R.id.message);
            textView.setTextColor(context.getResources().getColor(R.color.white));
        }
        toast.show();
    }

    public AlertDialog waitingDialog(String message, Context context) {
        return new SpotsDialog.Builder()
                .setCancelable(false)
                .setMessage(message)
                .setContext(context)
                .build();
    }

    public void messageDialog(String message, Context context) {
        new androidx.appcompat.app.AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle("CABME")
                .setMessage(message)
                .create().show();
    }
    public void setVaccinesReminder(Context mContext, String date, String description) {
        int REQUEST_CODE=new Random().nextInt();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(Objects.requireNonNull(simpleDateFormat.parse(date)));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(mContext, MyBroadCastReceiver.class);
        intent.putExtra("description",description);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, REQUEST_CODE, intent, 0);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        assert alarmManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }else{
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
        }
    }

}
