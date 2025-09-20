package com.qube.piprapay_tool.Class;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private View rootView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keyboard dismiss logic
        rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                Rect r = new Rect();
                rootView.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootView.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                boolean isKeyboardShowing = keypadHeight > screenHeight * 0.15;

                if (!isKeyboardShowing) {
                    clearFocusFromAllEditTexts(rootView);
                }
            });
        }

    }

    private void clearFocusFromAllEditTexts(View view) {
        if (view instanceof EditText) {
            view.clearFocus();
        } else if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                clearFocusFromAllEditTexts(vg.getChildAt(i));
            }
        }
    }
}


