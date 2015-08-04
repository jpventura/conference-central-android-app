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

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.udacity.devrel.training.conference.android.MainActivity;
import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.Connection;
import com.udacity.devrel.training.conference.android.common.Connection.State;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;
import com.udacity.devrel.training.conference.android.view.LoginView;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CancellationException;

public class LoginPresenter extends AccountAuthenticatorActivity
        implements LoginView.OnLoginListener, Observer {

    private LoginView loginView;
    private Connection connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginView = (LoginView) View.inflate(this, R.layout.view_login, null);
        loginView.setOnLoginListener(this);
        setContentView(loginView);
        connection = GoogleConnection.getInstance(this);
        connection.addObserver(this);

        final AccountManager am = AccountManager.get(this);
        String name = "joao.ventura@gmail.com";
        String type = getString(R.string.auth_token_type);
        final AccountManagerFuture<Bundle> future = am.getAuthToken(new Account(name, type), type, null, LoginPresenter.this, null, null);

        AsyncTask<Void, Void, Bundle> xxx = new AsyncTask<Void, Void, Bundle>() {
            @Override
            protected Bundle doInBackground(Void... params) {
                try {
                    return future.getResult();
                } catch(OperationCanceledException e) {
                    Bundle bundle = new Bundle();
                    bundle.putString("porra", "OperationCanceledException");
                    bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                    return bundle;
                } catch (AuthenticatorException e) {
                    Bundle bundle = new Bundle();
                    bundle.putString("porra", "AuthenticatorException");
                    bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                    return bundle;
                } catch (IOException e) {
                    Bundle bundle = new Bundle();
                    bundle.putString("porra", "IOException");
                    bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
                    return bundle;
                }
            }

            @Override
            protected void onPostExecute(Bundle bundle) {
                Log.e("ventura", "bundle " + bundle.toString());
            }
        };

        xxx.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection.deleteObserver(this);
        connection.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case GoogleConnection.REQUEST_CODE:
                connection.onActivityResult(resultCode);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onLogin() {
        connection.connect();
    }

    @Override
    public void update(Observable observable, Object data) {
        if ((observable == connection) && State.OPENED.equals(data)) {
            navigateToHome();
        }
    }

    private void navigateToHome() {
        startActivity(new Intent(this, MainActivity.class));
    }

    class GetBundle extends AsyncTask<Void, Void, Bundle> {
        @Override
        protected Bundle doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Bundle bundle) {
            super.onPostExecute(bundle);
        }
    }
}
