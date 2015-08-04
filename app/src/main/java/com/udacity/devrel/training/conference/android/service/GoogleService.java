package com.udacity.devrel.training.conference.android.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class GoogleService extends Service {

    enum Test {
        ZERO, ONE;

        Test fromInteger(int num) {
            return null;
        }

        void execute() {
        }

        static void execute(int i) {
        }

    }

    public GoogleService() {
        Log.e("ventura", "GoogleService");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class ConnectionHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Test.values()[msg.what].execute();
        }

    }

}
