package com.sanj.cabme.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.sanj.cabme.wrapper.Wrapper;

public class MyService extends IntentService {

    public MyService() {
        super("MyService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
