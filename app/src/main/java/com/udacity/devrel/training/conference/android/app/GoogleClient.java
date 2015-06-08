package com.udacity.devrel.training.conference.android.app;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.plus.Plus;

import java.lang.ref.WeakReference;
import java.util.Observable;

/**
 * Created by ventura on 07/06/15.
 */
public class GoogleClient extends Observable {

    public enum State {
        CREATED, OPENING, OPENED, CLOSED
    }

    private WeakReference<Activity> activity;
    private GoogleApiClient googleApiClient;
    private State state;

    private ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d("ventura", "connected");
            state = State.OPENED;
            setChanged();
            notifyObservers(State.OPENED);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d("ventura", "suspended");
            state = State.CLOSED;
            setChanged();
            notifyObservers(State.CLOSED);
        }
    };


    private OnConnectionFailedListener onConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            if (!mIntentInProgress && connectionResult.hasResolution()) {
                try {
                    mIntentInProgress = true;
                    activity.get().startIntentSenderForResult(connectionResult.getResolution().getIntentSender(),
                            AuthenticatorActivity.RC_SIGN_IN, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    mIntentInProgress = false;
                    googleApiClient.connect();
                }
            }
        }
    };

    public void signIn() {
        if (!googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    public void reconnect() {
        mIntentInProgress = false;

        if (!googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    public GoogleClient(Activity activity) {
        this.activity = new WeakReference<>(activity);
        this.state = State.CREATED;
        googleApiClient = new GoogleApiClient.Builder(activity, connectionCallbacks, onConnectionFailedListener)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
    }

    private boolean mIntentInProgress;

    public void connect() {
            googleApiClient.connect();
    }

    public void disconnect() {
        googleApiClient.disconnect();
    }

}
