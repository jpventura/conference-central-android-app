package com.udacity.devrel.training.conference.android.common;

import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.udacity.devrel.training.conference.android.AppConstants;

import java.io.IOException;

public abstract class GetAccessTokenTask extends AsyncTask<Void, Void, String> {

    private GoogleAccountCredential mGoogleAccountCredential;

    @Override
    protected abstract void onPostExecute(String accessToken);

    public GetAccessTokenTask(GoogleApiClient googleApiClient) {
        mGoogleAccountCredential = GoogleAccountCredential
                .usingAudience(googleApiClient.getContext(), AppConstants.AUDIENCE)
                .setSelectedAccountName(Plus.AccountApi.getAccountName(googleApiClient));
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            return mGoogleAccountCredential.getToken();
        } catch (GoogleAuthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
