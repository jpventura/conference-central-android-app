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

package com.udacity.devrel.training.conference.android.accounts;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class OAuthAccount extends Account implements Parcelable {
    public final String token;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OAuthAccount)) return false;
        return super.equals(o) && ((OAuthAccount)o).token.equals(token);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + token.hashCode();
    }

    public OAuthAccount(String name, String type, String token) {
        super(name, type);

        if (TextUtils.isEmpty(token)) {
            throw new IllegalArgumentException("the token must not be empty: " + token);
        }

        this.token = token;
    }

    public OAuthAccount(Parcel in) {
        super(in);
        token = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(token);
    }

    public static final Creator<OAuthAccount> CREATOR = new Creator<OAuthAccount>() {
        public OAuthAccount createFromParcel(Parcel source) {
            return new OAuthAccount(source);
        }

        public OAuthAccount[] newArray(int size) {
            return new OAuthAccount[size];
        }
    };

    @Override
    public String toString() {
        return "Account {name=" + name + ", type=" + type + ", token=" + token + "}";
    }
}
