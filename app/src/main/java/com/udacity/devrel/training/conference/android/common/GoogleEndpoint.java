package com.udacity.devrel.training.conference.android.common;


public class GoogleEndpoint {
    private static GoogleEndpoint ourInstance = new GoogleEndpoint();

    public static GoogleEndpoint getInstance() {
        return ourInstance;
    }

    private GoogleEndpoint() {
    }
}
