package com.qube.piprapay_tool.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.qube.piprapay_tool.Class.BaseActivity;
import com.qube.piprapay_tool.R;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        View linkOrigWeb = findViewById(R.id.link_orig_web);
        View linkOrigGithub = findViewById(R.id.link_orig_github);
        View linkRwGithub = findViewById(R.id.link_rw_github);

        if (linkOrigWeb != null) {
            linkOrigWeb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl("https://piprapay.com/");
                }
            });
        }

        if (linkOrigGithub != null) {
            linkOrigGithub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl("https://github.com/PipraPay/PipraPay");
                }
            });
        }

        if (linkRwGithub != null) {
            linkRwGithub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl("https://github.com/SHAJON-404/PipraPay-RW");
                }
            });
        }

        View githubLayout = findViewById(R.id.github_layout);
        if (githubLayout != null) {
            githubLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openUrl("https://github.com/SHAJON-404");
                }
            });
        }
    }

    private void openUrl(String url) {
        try {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
