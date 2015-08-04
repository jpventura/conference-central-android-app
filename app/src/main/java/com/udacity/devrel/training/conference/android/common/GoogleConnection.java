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

package com.udacity.devrel.training.conference.android.common;

import android.app.Activity;
import android.content.IntentSender;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;

import java.lang.ref.WeakReference;

public class GoogleConnection extends Connection
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static GoogleConnection sGoogleConnection;

    private WeakReference<Activity> activityWeakReference;
    private GoogleApiClient mGoogleApiClient;
    private ConnectionResult connectionResult;

    public static Connection getInstance(Activity activity) {
        if (null == sGoogleConnection) {
            sGoogleConnection = new GoogleConnection(activity);
        }
        return sGoogleConnection;
    }

    public static Connection getInstance() {
        return sGoogleConnection;
    }

    public void onSignUp() {
        // We have an intent which will allow our user to sign in or
        // resolve an error.  For example if the user needs to
        // select an account to sign in with, or if they need to
        // consent to the permissions your app is requesting.
        try {
            // Send the pending intent that we stored on the most recent
            // OnConnectionFailed callback.  This will allow the user to
            // resolve the error currently preventing our connection to
            // Google Play services.
            Activity activity = activityWeakReference.get();
            changeState(State.OPENING);
            connectionResult.startResolutionForResult(activity, REQUEST_CODE);
        } catch (IntentSender.SendIntentException e) {
            // The intent was canceled before it was sent.  Attempt to
            // connect to get an updated ConnectionResult.
            changeState(State.CREATED);
            mGoogleApiClient.connect();
        }
    }

    public void onSignIn() {
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }

    public void onSignOut() {
        // We clear the default account on sign out so that Google Play
        // services will not return an onConnected callback without user
        // interaction.
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        mGoogleApiClient.disconnect();
        changeState(State.CLOSED);
        mGoogleApiClient.connect();
    }

    public void onRevokeAndSignOut() {
        // After we revoke permissions for the user with a GoogleApiClient
        // instance, we must discard it and create a new one.
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        // Our sample has caches no user data from Google+, however we
        // would normally register a callback on revokeAccessAndDisconnect
        // to delete user data so that we comply with Google developer
        // policies.
        Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient);
        changeState(State.CLOSED);

        mGoogleApiClient = buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    private GoogleConnection(Activity activity) {
        changeState(State.CLOSED);
        activityWeakReference = new WeakReference<>(activity);
        mGoogleApiClient = buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    public String getAccountName() {
        return Plus.AccountApi.getAccountName(mGoogleApiClient);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        get.execute();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        // We call connect() to attempt to re-establish the connection or get a
        // ConnectionResult that we can attempt to resolve.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (state.equals(State.CLOSED) && result.hasResolution()) {
            changeState(State.CREATED);
            connectionResult = result;
        } else {
            connect();
        }
    }

    public void onActivityResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            connect();
        } else {
            // If the error resolution was not successful or the user canceled,
            // we should stop processing errors.
            changeState(State.CREATED);
        }
    }

    private GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(activityWeakReference.get())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(new Scope("email")).build();
    }

    private AsyncTask<Void, Void, String> get = new AsyncTask<Void, Void, String>() {
        @Override
        protected String doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            changeState(State.OPENED);
        }
    };
}
