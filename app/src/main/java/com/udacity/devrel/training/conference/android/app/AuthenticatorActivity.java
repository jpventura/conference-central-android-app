/* Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.udacity.devrel.training.conference.android.app;

import android.accounts.AccountAuthenticatorActivity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

import com.udacity.devrel.training.conference.android.MainActivity;
import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.view.AuthenticatorView;
import com.udacity.devrel.training.conference.android.view.AuthenticatorView.OnSignUpListener;

import java.util.Observable;
import java.util.Observer;

public class AuthenticatorActivity extends AccountAuthenticatorActivity implements Observer {

    private AuthenticatorView view;
    private ProgressBar loginProgress;

    public static final int RC_SIGN_IN = 100;

    private OnSignUpListener onSignUpListener = new OnSignUpListener() {
        @Override
        public void onSignUp() {
            loginProgress.setVisibility(View.VISIBLE);
            // googleClient.connect();
            mGoogleApiClient.connect();

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = (AuthenticatorView) View.inflate(this, R.layout.activity_authenticator, null);
        view.setOnSignUpListener(onSignUpListener);
        setContentView(view);
        loginProgress = (ProgressBar) view.findViewById(R.id.login_progress);

        googleClient = new GoogleClient(this);
        googleClient.addObserver(this);
        mGoogleApiClient = new GoogleApiClient.Builder(this, connectionCallbacks, onConnectionFailedListener)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    GoogleClient googleClient;
    private boolean mIntentInProgress;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onStart() {
        super.onStart();
        // mGoogleApiClient.connect();
        // googleClient.signIn();
    }

    private ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            loginProgress.setVisibility(View.GONE);
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d("ventura", "suspended " + Integer.toString(i));
           //  mGoogleApiClient.connect();
        }
    };

    private OnConnectionFailedListener onConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d("ventura", "onConnectionFailed");
            Log.d("ventura", "onConnectionFailed " + Integer.toString(connectionResult.getErrorCode()));
            if (connectionResult.hasResolution()) {
                try {
                    mIntentInProgress = true;
                    startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
                            RC_SIGN_IN, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    // The intent was canceled before it was sent.  Return to the default
                    // state and attempt to connect to get an updated ConnectionResult.
                    Log.d("ventura", e.toString());
                    mIntentInProgress = false;
                    mGoogleApiClient.connect();
                }
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
//        if (requestCode == RC_SIGN_IN) {
//            googleClient.reconnect();
//        }
//    }

    if (requestCode == RC_SIGN_IN) {
        mIntentInProgress = false;

        if (!mGoogleApiClient.isConnecting()) {
            mGoogleApiClient.connect();
        }
    }
}

    public void update(Observable observable, Object data) {

        GoogleClient.State state = (GoogleClient.State) data;
        Log.d("ventura", "state " + state.toString());
        if (state.equals(GoogleClient.State.OPENED)) {
            loginProgress.setVisibility(View.GONE);
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}
