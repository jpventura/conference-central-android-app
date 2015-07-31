/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.udacity.devrel.training.conference.android.presenter;

import android.accounts.AccountAuthenticatorActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.udacity.devrel.training.conference.android.MainActivity;
import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;
import com.udacity.devrel.training.conference.android.common.State;
import com.udacity.devrel.training.conference.android.view.LoginView;

import java.util.Observable;
import java.util.Observer;

public class LoginPresenter extends AccountAuthenticatorActivity
        implements LoginView.OnLoginListener, Observer {

    private LoginView loginView;
    private GoogleConnection googleConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginView = (LoginView) View.inflate(this, R.layout.view_login, null);
        loginView.setOnLoginListener(this);
        setContentView(loginView);
        googleConnection = GoogleConnection.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleConnection.addObserver(this);
        // googleConnection.connect();

        Log.e("ventura", "onStart(" + googleConnection.getState() + ")");
        if (googleConnection.isOpened()) {
            navigateToHome();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleConnection.deleteObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        googleConnection.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GoogleConnection.REQUEST_CODE:
                googleConnection.onActivityResult(requestCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onLogin() {
        googleConnection.connect();
    }

    @Override
    public void update(Observable observable, Object data) {
        if ((observable == googleConnection) && State.OPENED.equals(data)) {
            navigateToHome();
        }

        Log.d("ventura", "LoginPresenter.update(" + data.toString() + ")");

    }

    private void navigateToHome() {
        startActivity(new Intent(this, MainActivity.class));
    }

}
