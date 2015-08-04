/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.Connection;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;
import com.udacity.devrel.training.conference.android.presenter.LoginPresenter;

import java.io.IOException;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

    private Context mContext;
    private Connection mConnection;

    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
        mConnection = GoogleConnection.getInstance();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
                             String accountType,
                             String authTokenType,
                             String[] requiredFeatures,
                             Bundle options) throws NetworkErrorException {
        Log.e("ventura", "addAccount 1");
        Bundle bundle = (null == options) ? new Bundle() : options;

        if ((null == accountType) && (null == authTokenType)) {
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
            return bundle;
        }

//        Log.e("ventura", "addAccount 11");
//        final AccountManager accountManager = AccountManager.get(mContext);
//        if (accountManager.getAccountsByType(accountType).length == 0) {
//            final String error = mContext.getString(R.string.error_add_account);
//            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
//            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, error);
//            Toast.makeText(mContext, error, Toast.LENGTH_LONG).show();
//            return bundle;
//        }

        Log.e("ventura", "addAccount 2");
//        if (!getAuthTokenType().equals(authTokenType)) {
//            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
//            return bundle;
//        }

        Log.e("ventura", "addAccount 3");
        final Intent intent = new Intent(mContext, LoginPresenter.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);

        bundle.putParcelable(AccountManager.KEY_INTENT, intent);

        Log.e("ventura", "addAccount 4");
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
                               Account account,
                               String authTokenType,
                               Bundle options) throws NetworkErrorException {
        if ((null == account) && (null == authTokenType)) {
            Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
            return bundle;
        }

        if (!getAuthTokenType().equals(authTokenType)) {
            Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            return bundle;
        }

//        Bundle bundle = (null == options) ? new Bundle() : options;
//        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
//        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        // String authToken = mConnection.getAuthToken(account);
        Bundle authToken = mConnection.getToken(account);

        if (null != authToken) {
            authToken.putAll(options);
            return authToken;
        }

        return addAccount(response, account.type, authTokenType, null, options);
//
//
//        return (null == result) ? addAccount(response, account.type, authTokenType, null, options) : result;
//        if (null == authToken) {
//            final Intent intent = new Intent(mContext, LoginPresenter.class);
//            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
//            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
//        } else {
//            bundle.putString(AccountManager.KEY_AUTHTOKEN, authToken);
//        }
//
//        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }

    private String getAuthTokenType() {
        return mContext.getString(R.string.auth_token_type);
    }
}
