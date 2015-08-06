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

package com.udacity.devrel.training.conference.android.accounts;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.Connection;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;
import com.udacity.devrel.training.conference.android.view.LoginView;

import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class AuthenticatorActivity extends AccountAuthenticatorActivity
        implements LoginView.OnLoginListener, Observer {
    private LoginView loginView;
    private Connection mConnection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginView = (LoginView) View.inflate(this, R.layout.view_login, null);
        loginView.setOnLoginListener(this);
        setContentView(loginView);

        if (!isAccountUnique()) {
            Toast.makeText(this, getString(R.string.error_add_account), Toast.LENGTH_LONG).show();
            finish();
        }

        mConnection = GoogleConnection.getInstance(this);
        mConnection.addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnection.deleteObserver(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Connection.REQUEST_CODE != requestCode) {
            mConnection.onActivityResult(resultCode);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onLogin() {
        mConnection.connect();
    }

    @Override
    public void update(Observable observable, Object data) {
        if ((observable == mConnection) && Connection.State.OPENED.equals(data)) {
            addAccountInBackground();
        }
    }

    private void addAccountInBackground() {
        AddAccountTask addAccountTask = new AddAccountTask(this);
        addAccountTask.execute();
    }

    private boolean isAccountUnique() {
        final AccountManager accountManager = AccountManager.get(this);
        String name = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String type = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        List<Account> accounts = Arrays.asList(accountManager.getAccountsByType(type));

        return accounts.isEmpty() || ((accounts.size() == 1) && accounts.contains(name));
    }
}
