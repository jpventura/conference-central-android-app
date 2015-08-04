/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.udacity.devrel.training.conference.android.common;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;

import java.util.Observable;

public abstract class Connection extends Observable {

    public static final int REQUEST_CODE = 1224;

    public enum State {
        CREATED {
            void connect(Connection connection) {
                connection.onSignUp();
            }
        },
        OPENING {
            void connect(Connection connection) {
                // If the error resolution in onActivityResult was
                // successful, i.e. the user chose a Google account and
                // allowed the application access its data, we should
                // continue processing errors.
                connection.onSignIn();
            }
        },
        OPENED {
            void disconnect(Connection connection) {
                connection.onSignOut();
            }
            void revoke(Connection connection) {
                connection.onRevokeAndSignOut();
            }
        },
        CLOSED {
            void connect(Connection connection) {
                connection.onSignIn();
            }
        };

        void connect(Connection connection) {}
        void disconnect(Connection connection) {}
        void revoke(Connection connection) {}
    }

    public abstract void onActivityResult(int resultCode);
    public abstract String getAuthToken(Account account);
    public abstract Bundle getToken(Account account);

    protected abstract void onSignUp();
    protected abstract void onSignIn();
    protected abstract void onSignOut();
    protected abstract void onRevokeAndSignOut();

    public void connect() {
        state.connect(this);
    }

    public void disconnect() {
        state.disconnect(this);
    }

    public void revoke() {
        state.revoke(this);
    }

    protected void changeState(State state) {
        this.state = state;
        setChanged();
        notifyObservers(state);
    }

    protected State state;
}
