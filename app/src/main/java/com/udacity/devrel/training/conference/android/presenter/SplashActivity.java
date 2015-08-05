package com.udacity.devrel.training.conference.android.presenter;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.udacity.devrel.training.conference.android.R;

import java.io.IOException;

public class SplashActivity extends AccountAuthenticatorActivity {

    private AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mAccountManager = AccountManager.get(this);

        SharedPreferences sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        String name = sharedPreferences.getString(getString(R.string.account_name_key), null);
        String type = getString(R.string.auth_token_type);

//        if (null == name) {
//            AddAccountTask addAccountTask = new AddAccountTask();
//            addAccountTask.execute(type);
//        } else {
//            GetAuthTokenTask getAuthTokenTask = new GetAuthTokenTask();
//            getAuthTokenTask.execute(new Account(name, type));
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class AddAccountTask extends  AsyncTask<String, Void, Bundle> {
        @Override
        protected Bundle doInBackground(String... types) {
            final AccountManagerFuture<Bundle> future = mAccountManager
                    .addAccount(types[0], types[0], null, null, SplashActivity.this, null, null);
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

        @Override
        protected void onPostExecute(Bundle bundle) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != data) {
            finishLogin(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class GetAuthTokenTask extends AsyncTask<Account, Void, Bundle> {
        @Override
        protected Bundle doInBackground(Account... accounts) {
            final AccountManagerFuture<Bundle> future = mAccountManager
                    .getAuthToken(accounts[0], accounts[0].type, null, SplashActivity.this, null, null);
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

        @Override
        protected void onPostExecute(Bundle bundle) {
            Log.e("ventura", "porra " + bundle.toString());
        }
    }

    private void finishLogin(Intent intent) {
        Log.d("ventura", "> finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));


            Log.d("ventura", "> finishLogin > addAccountExplicitly");
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = getString(R.string.auth_token_type);

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, null, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);


        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
