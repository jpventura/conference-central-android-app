/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.udacity.devrel.training.conference.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.google.android.gms.common.SignInButton;
import com.udacity.devrel.training.conference.android.R;

public class LoginView extends RelativeLayout implements View.OnClickListener {

    public interface OnLoginListener {
        void onLogin();
    }

    private SignInButton signInButton;
    private OnLoginListener onLoginListener;

    public LoginView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(this);
    }

    public void setOnLoginListener(OnLoginListener onloginListener) {
        this.onLoginListener = onloginListener;
    }

    @Override
    public void onClick(View view) {
        if ((view.getId() == R.id.sign_in_button) && (null != onLoginListener)) {
            onLoginListener.onLogin();
        }
    }

}
