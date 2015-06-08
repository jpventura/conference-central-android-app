package com.udacity.devrel.training.conference.android.app;

import android.content.Intent;

/**
 * Created by ventura on 07/06/15.
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // startService(new Intent(this, GoogleAuthenticatorService.class));
    }
}
