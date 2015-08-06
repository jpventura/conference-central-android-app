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

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;

import com.udacity.devrel.training.conference.android.common.GoogleConnection;

import java.lang.ref.WeakReference;

public class AddAccountTask extends AsyncTask<Void, Void, OAuthAccount> {
    private WeakReference<AccountAuthenticatorActivity> mActivity;

    public AddAccountTask(AccountAuthenticatorActivity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    protected OAuthAccount doInBackground(Void... params) {
        return GoogleConnection.getInstance(mActivity.get()).getAccount();
    }

    @Override
    protected void onPostExecute(OAuthAccount account) {
        if (null == account) {
            mActivity.get().setAccountAuthenticatorResult(null);
            mActivity.get().setResult(AccountAuthenticatorActivity.RESULT_CANCELED, null);
        } else {
            addAccountExplicitly(account);
        }

        mActivity.get().finish();
    }

    private void addAccountExplicitly(OAuthAccount account) {
        final AccountManager accountManager = AccountManager.get(mActivity.get());
        accountManager.addAccountExplicitly(account, null, null);
        accountManager.setAuthToken(account, account.type, account.token);

        Parcel in = Parcel.obtain();
        account.writeToParcel(in, 0);
        Bundle bundle = Bundle.CREATOR.createFromParcel(in);
        Intent intent = new Intent();
        intent.putExtras(bundle);

        mActivity.get().setAccountAuthenticatorResult(bundle);
        mActivity.get().setResult(AccountAuthenticatorActivity.RESULT_OK, intent);
    }
}
