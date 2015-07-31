package com.udacity.devrel.training.conference.android.presenter;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;
import com.udacity.devrel.training.conference.android.common.State;
import com.udacity.devrel.training.conference.android.view.ProfileView;

import java.util.Observable;
import java.util.Observer;

public class ProfilePresenter extends ActionBarActivity
        implements Observer, ProfileView.OnProfileListener {

    private ProfileView profileView;
    private GoogleConnection googleConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileView = (ProfileView) View.inflate(this, R.layout.view_profile, null);
        profileView.setOnLoginListener(this);
        setContentView(profileView);
        googleConnection = GoogleConnection.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleConnection.addObserver(this);
    }

    @Override
    public void update(Observable observable, Object data) {
        if ((observable == googleConnection) && !State.OPENED.equals(data)) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleConnection.deleteObserver(this);
    }

    @Override
    public void onLogout() {
        googleConnection.disconnect();
    }

    @Override
    public void onRevoke() {
        googleConnection.revokeAccessAndDisconnect();
    }


}
