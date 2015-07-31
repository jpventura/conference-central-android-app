package com.udacity.devrel.training.conference.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.udacity.devrel.training.conference.android.R;

public class ProfileView extends RelativeLayout implements View.OnClickListener {

    private Button signOutButton;

    public interface OnProfileListener {
        void onLogout();
        void onRevoke();
    }

    private OnProfileListener onProfileListener;

    public ProfileView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);
    }

    public void setOnLoginListener(OnProfileListener onProfileListener) {
        this.onProfileListener = onProfileListener;
    }

    @Override
    public void onClick(View view) {
        if (null != onProfileListener) {
            onProfileListener.onLogout();
        }
    }

}
