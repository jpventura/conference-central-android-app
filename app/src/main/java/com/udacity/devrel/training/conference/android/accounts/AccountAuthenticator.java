package com.udacity.devrel.training.conference.android.accounts;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;

import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.common.Connection;
import com.udacity.devrel.training.conference.android.common.GoogleConnection;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

    private Context mContext;
    private Connection mConnection;

    public AccountAuthenticator(Context context) {
        super(context);
        mContext = context;
        mConnection = GoogleConnection.getInstance();
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        return null;
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
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Bundle bundle = (null == options) ? new Bundle() : options;

        if ((null == account) && (null == authTokenType)) {
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_REQUEST);
            return bundle;
        }

        if (!getAuthTokenType().equals(authTokenType)) {
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS);
            return bundle;
        }

        bundle.putString(AccountManager.KEY_AUTHTOKEN, "afasdfasdf");
        bundle.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);

        return bundle;
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
