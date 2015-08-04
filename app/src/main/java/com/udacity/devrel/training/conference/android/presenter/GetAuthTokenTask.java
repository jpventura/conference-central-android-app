package com.udacity.devrel.training.conference.android.presenter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import com.udacity.devrel.training.conference.android.R;

import java.io.IOException;
import java.lang.ref.WeakReference;

public class GetAuthTokenTask extends AsyncTask<Account, Void, Bundle> {

    private WeakReference<Activity> mActivity;

    public GetAuthTokenTask(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    protected Bundle doInBackground(Account... accounts) {
        final Activity activity = mActivity.get();
        final AccountManager am = AccountManager.get(activity);
        final String type = activity.getString(R.string.auth_token_type);
        final AccountManagerFuture<Bundle> future =
                am.getAuthToken(accounts[0], type, null, activity, null, null);
        try {
            return future.getResult();
        } catch (OperationCanceledException e) {
            Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_CANCELED);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
            return bundle;
        } catch (IOException e) {
            Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_REMOTE_EXCEPTION);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
            return bundle;
        } catch (AuthenticatorException e) {
            Bundle bundle = new Bundle();
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, e.getMessage());
            return bundle;
        }
    }

}
