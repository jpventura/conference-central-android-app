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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.udacity.devrel.training.conference.android.MainActivity;
import com.udacity.devrel.training.conference.android.R;
import com.udacity.devrel.training.conference.android.view.AuthenticatorView;
import com.udacity.devrel.training.conference.android.view.AuthenticatorView.OnSignUpListener;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private AuthenticatorView view;

    private OnSignUpListener onSignUpListener = new OnSignUpListener() {
        @Override
        public void onSignUp() {
            startActivity(new Intent(AuthenticatorActivity.this, MainActivity.class));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        view = (AuthenticatorView) View.inflate(this, R.layout.activity_authenticator, null);
        view.setOnSignUpListener(onSignUpListener);
        setContentView(view);
    }

}
