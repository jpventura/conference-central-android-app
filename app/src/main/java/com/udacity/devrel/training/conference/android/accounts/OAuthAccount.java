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

    private OAuthAccount(String name, String type) {
        this(name, type, null);
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

    public String toString() {
        return "Account {name=" + name + ", type=" + type + ", token=" + token + "}";
    }
}
