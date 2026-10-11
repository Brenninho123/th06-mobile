package com.brenninho.th06mobile;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class SetupActivity extends Activity {
    public static final String ACTION_IMPORT = "com.brenninho.th06mobile.IMPORT";

    private static final int REQUEST_PICK_FOLDER = 1;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView statusView;
    private ProgressBar progressBar;
    private Button selectButton;
    private Button playButton;
    private boolean importing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean forceSetup = getIntent() != null && ACTION_IMPORT.equals(getIntent().getAction());
        if (!forceSetup && GameDataImporter.hasGameData(this)) {
            launchGame();
            return;
        }

        buildLayout();
        refreshState();
    }

    private int dp(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private void buildLayout() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(dp(32), dp(24), dp(32), dp(24));

        TextView title = new TextView(this);
        title.setText(R.string.setup_title);
        title.setTextColor(Color.rgb(0xF5, 0xE6, 0xE8));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        title.setTypeface(Typeface.SERIF, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        column.addView(title);

        TextView description = new TextView(this);
        description.setText(R.string.setup_description);
        description.setTextColor(Color.rgb(0xC9, 0xB8, 0xBB));
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(12), 0, dp(20));
        column.addView(description);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);

        selectButton = new Button(this);
        selectButton.setText(R.string.setup_select_folder);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFolder();
            }
        });
        buttons.addView(selectButton);

        playButton = new Button(this);
        playButton.setText(R.string.setup_play);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGame();
            }
        });
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        playParams.leftMargin = dp(16);
        buttons.addView(playButton, playParams);
        column.addView(buttons);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.INVISIBLE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(360), LinearLayout.LayoutParams.WRAP_CONTENT);
        progressParams.topMargin = dp(20);
        column.addView(progressBar, progressParams);

        statusView = new TextView(this);
        statusView.setTextColor(Color.rgb(0xE8, 0x9A, 0xA6));
        statusView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, dp(8), 0, 0);
        column.addView(statusView);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(0x1A, 0x05, 0x0A));
        LinearLayout center = new LinearLayout(this);
        center.setGravity(Gravity.CENTER);
        center.addView(column);
        scroll.addView(center);
        setContentView(scroll);
    }

    private void refreshState() {
        List<String> missing = GameDataImporter.missingFiles(this);
        boolean ready = missing.isEmpty();

        playButton.setVisibility(ready ? View.VISIBLE : View.GONE);
        selectButton.setEnabled(!importing);
        playButton.setEnabled(!importing);

        if (importing) {
            return;
        }

        if (ready) {
            statusView.setText(R.string.setup_existing);
        } else if (missing.size() < 6) {
            statusView.setText(getString(R.string.setup_missing, TextUtils.join(", ", missing)));
        } else {
            statusView.setText("");
        }
    }

    private void pickFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_PICK_FOLDER || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        startImport(data.getData());
    }

    private void startImport(final Uri treeUri) {
        stopRunningGame();
        importing = true;
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        refreshState();

        new Thread(new Runnable() {
            @Override
            public void run() {
                String error = null;
                try {
                    GameDataImporter.importFromTree(SetupActivity.this, treeUri, new GameDataImporter.Listener() {
                        @Override
                        public void onProgress(final String fileName, final int percent) {
                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(percent);
                                    statusView.setText(getString(R.string.setup_copying, fileName, percent));
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    error = e.getMessage() != null ? e.getMessage() : e.toString();
                }

                final String finalError = error;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        finishImport(finalError);
                    }
                });
            }
        }, "th06-import").start();
    }

    private void finishImport(String error) {
        importing = false;
        progressBar.setVisibility(View.INVISIBLE);
        refreshState();

        if (error != null) {
            statusView.setText(getString(R.string.setup_failed, error));
            return;
        }

        List<String> missing = GameDataImporter.missingFiles(this);
        if (!missing.isEmpty()) {
            statusView.setText(getString(R.string.setup_missing, TextUtils.join(", ", missing)));
            return;
        }

        statusView.setText(R.string.setup_done);
        launchGame();
    }

    private void stopRunningGame() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (manager == null) {
            return;
        }

        List<ActivityManager.RunningAppProcessInfo> processes = manager.getRunningAppProcesses();
        if (processes == null) {
            return;
        }

        String gameProcess = getPackageName() + GameActivity.PROCESS_SUFFIX;
        for (ActivityManager.RunningAppProcessInfo info : processes) {
            if (gameProcess.equals(info.processName)) {
                Process.killProcess(info.pid);
            }
        }
    }

    private void launchGame() {
        stopRunningGame();
        startActivity(new Intent(this, GameActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!importing) {
            super.onBackPressed();
        }
    }
}
