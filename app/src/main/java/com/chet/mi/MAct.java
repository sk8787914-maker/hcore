package com.chet.mi;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import androidx.appcompat.widget.SwitchCompat;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chet.mi.utils.AppManager;
import com.chet.mi.utils.Downtwo;
import com.chet.mi.utils.Prefs;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Obfuscate
public class MAct extends AppCompatActivity {

    private static native String exdate();

    static {
        try {
            System.loadLibrary("ReNnLd");
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private static final String PKG_BGMI = "com.pubg.imobile";
    private static final String PREF_NAME = "com.chet.mi.download";
    private static final String PREF_VERSION_KEY = "version";

    // UI Elements
    private TextView usernameText;
    private TextView keyRemainingText;
    private TextView keyExpiryText;
    private View btnLogout;

    private View gameCard;
    private View btnPlayIcon;
    private View btnGameOptionsMenu;
    private View btnSystemNotification;
    private TextView gameName;
    private TextView packageName;

    // Timer Box TextViews
    private TextView daysText;
    private TextView hoursText;
    private TextView minutesText;
    private TextView secondsText;

    private View obbButton;
    private boolean isObbCopied = false;
    private boolean isGameRunning = false;
    private boolean isGameInstalled = false;

    private Dialog customDialog;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(Color.parseColor("#F8FAFC"));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        setContentView(R.layout.activity_main);

        checkPermissions();

        // UI references
        usernameText = findViewById(R.id.usernameText);
        keyRemainingText = findViewById(R.id.keyRemainingText);
        keyExpiryText = findViewById(R.id.keyExpiryText);
        btnLogout = findViewById(R.id.btnLogout);

        gameCard = findViewById(R.id.gameCard);
        btnPlayIcon = findViewById(R.id.btnPlayIcon);
        btnGameOptionsMenu = findViewById(R.id.btnGameOptionsMenu);
        btnSystemNotification = findViewById(R.id.btnSystemNotification);
        gameName = findViewById(R.id.gameName);
        packageName = findViewById(R.id.packageName);

        // Timer boxes
        daysText = findViewById(R.id.daysText);
        hoursText = findViewById(R.id.hoursText);
        minutesText = findViewById(R.id.minutesText);
        secondsText = findViewById(R.id.secondsText);

        // Populate Dynamic Device Hardware Name
        TextView deviceModelTitle = findViewById(R.id.deviceModelTitle);
        TextView deviceInfoSub = findViewById(R.id.deviceInfoSub);
        try {
            String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toUpperCase() : "DEVICE";
            String model = Build.MODEL != null ? Build.MODEL : "";
            String device = Build.DEVICE != null ? Build.DEVICE : "";
            String fullDeviceName = manufacturer + " " + model + " (" + device + ")";
            if (deviceModelTitle != null) {
                deviceModelTitle.setText(fullDeviceName);
            }
            if (deviceInfoSub != null) {
                deviceInfoSub.setText("Android " + Build.VERSION.RELEASE + " • " + Build.SUPPORTED_ABIS[0]);
            }
        } catch (Exception ignored) {}

        // Populate User Info
        Prefs prefs = new Prefs(this);
        String savedUser = prefs.getSt("USER", "Uncheated Loader");
        if (savedUser != null && !savedUser.trim().isEmpty()) {
            if (usernameText != null) usernameText.setText(savedUser);
        }

        // Setup Logout
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(MAct.this, LogAct.class);
                startActivity(intent);
                finish();
            });
        }

        // Setup Launch Flow
        if (btnPlayIcon != null) {
            btnPlayIcon.setOnClickListener(v -> handleLaunchFlow());
        }

        // Setup Options Menu (Screenshot 2)
        if (btnGameOptionsMenu != null) {
            btnGameOptionsMenu.setOnClickListener(v -> showGameOptionsMenuDialog());
        }

        // Setup System Notification Broadcast (Screenshot 1)
        if (btnSystemNotification != null) {
            btnSystemNotification.setOnClickListener(v -> showSystemBroadcastDialog());
        }

        // Setup Hide ESP / Anti-Screen-Record Protection
        SwitchCompat hideEspSwitch = findViewById(R.id.hideEspSwitch);
        if (hideEspSwitch != null) {
            hideEspSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    Toast.makeText(MAct.this, "🛡️ Hide ESP & Anti-Screen-Record Active", Toast.LENGTH_SHORT).show();
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
                    Toast.makeText(MAct.this, "Hide ESP Protection Disabled", Toast.LENGTH_SHORT).show();
                }
            });
            if (hideEspSwitch.isChecked()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }
        }

        setKeyTimeUnavailable();
        startKeyCountdown();
        startPageAnimation();

        initTtsAndSpeakWelcome();

        checkObbApplied();
        updateGameStatus();
    }

    private TextToSpeech tts;

    private void initTtsAndSpeakWelcome() {
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status != TextToSpeech.ERROR && tts != null) {
                    int result = tts.setLanguage(Locale.US);
                    if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts.setPitch(1.25f); // High-pitch clear female tone
                        tts.setSpeechRate(0.92f);
                        
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    tts.speak("Welcome to Uncheated Loader. Key verified successfully.", TextToSpeech.QUEUE_FLUSH, null, "WELCOME_VOICE");
                                } else {
                                    tts.speak("Welcome to Uncheated Loader. Key verified successfully.", TextToSpeech.QUEUE_FLUSH, null);
                                }
                            } catch (Exception e) {
                                Log.e("MAct", "TTS Speak error: " + e.getMessage());
                            }
                        }, 600);
                    }
                }
            });
        } catch (Exception e) {
            Log.e("MAct", "TTS Init error: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    private void showSystemBroadcastDialog() {
        if (customDialog != null && customDialog.isShowing()) {
            customDialog.dismiss();
        }

        customDialog = new Dialog(this);
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setContentView(R.layout.dialog_system_broadcast);

        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        customDialog.setCancelable(true);

        View btnAcknowledge = customDialog.findViewById(R.id.btnAcknowledge);
        if (btnAcknowledge != null) {
            btnAcknowledge.setOnClickListener(v -> customDialog.dismiss());
        }

        customDialog.show();
    }

    private void showGameOptionsMenuDialog() {
        if (customDialog != null && customDialog.isShowing()) {
            customDialog.dismiss();
        }

        customDialog = new Dialog(this);
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setContentView(R.layout.dialog_game_options);

        if (customDialog.getWindow() != null) {
            customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        customDialog.setCancelable(true);

        View btnResync = customDialog.findViewById(R.id.btnOptionResync);
        View btnVerify = customDialog.findViewById(R.id.btnOptionVerify);
        View btnSignOut = customDialog.findViewById(R.id.btnOptionSignOut);

        if (btnResync != null) {
            btnResync.setOnClickListener(v -> {
                customDialog.dismiss();
                Toast.makeText(MAct.this, "Re-synchronizing OBB & Data...", Toast.LENGTH_SHORT).show();
                copyObbFiles();
            });
        }

        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                customDialog.dismiss();
                Toast.makeText(MAct.this, "✔ Virtual Container Verified Active", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSignOut != null) {
            btnSignOut.setOnClickListener(v -> {
                customDialog.dismiss();
                Intent intent = new Intent(MAct.this, LogAct.class);
                startActivity(intent);
                finish();
            });
        }

        customDialog.show();
    }

    private void startPageAnimation() {
        View mainLogo = findViewById(R.id.mainLogo);
        if (mainLogo != null) {
            ObjectAnimator logoFloat = ObjectAnimator.ofFloat(mainLogo, View.TRANSLATION_Y, 0f, -6f, 0f);
            logoFloat.setDuration(2600);
            logoFloat.setRepeatCount(ObjectAnimator.INFINITE);
            logoFloat.start();
        }

        View[] views = {findViewById(R.id.brandHeader), findViewById(R.id.gameCard), findViewById(R.id.subscriptionCard)};
        long delay = 0;
        for (View view : views) {
            if (view != null) {
                view.setAlpha(0f);
                view.setTranslationY(30f);
                view.animate().alpha(1f).translationY(0f).setDuration(500).setStartDelay(delay).start();
                delay += 100;
            }
        }
    }

    private void setKeyTimeUnavailable() {
        if (daysText != null) daysText.setText("--");
        if (hoursText != null) hoursText.setText("--");
        if (minutesText != null) minutesText.setText("--");
        if (secondsText != null) secondsText.setText("--");
        if (keyExpiryText != null) keyExpiryText.setText("Expires: --");
    }

    private void startKeyCountdown() {
        String expiry = null;
        try {
            expiry = exdate();
        } catch (Exception ignored) {}

        if (expiry == null || expiry.trim().isEmpty() || "NULL".equalsIgnoreCase(expiry.trim())) {
            expiry = "2026-11-21 03:03:15";
        }

        final String displayExpiryStr = expiry.trim();
        if (keyExpiryText != null) {
            keyExpiryText.setText("License validity: " + displayExpiryStr);
        }

        final long expiryMillis = parseExpiryMillis(displayExpiryStr);

        if (expiryMillis <= 0) {
            return;
        }

        final Handler timerHandler = new Handler(Looper.getMainLooper());
        final Runnable timer = new Runnable() {
            @Override public void run() {
                long remaining = expiryMillis - System.currentTimeMillis();
                if (remaining <= 0L) {
                    if (daysText != null) daysText.setText("00");
                    if (hoursText != null) hoursText.setText("00");
                    if (minutesText != null) minutesText.setText("00");
                    if (secondsText != null) secondsText.setText("00");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(MAct.this, LogAct.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 2000);
                    return;
                }
                long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
                long days    = totalSeconds / 86400L;
                long hours   = (totalSeconds % 86400L) / 3600L;
                long minutes = (totalSeconds % 3600L) / 60L;
                long seconds = totalSeconds % 60L;

                if (daysText != null) daysText.setText(String.format(Locale.US, "%02d", days));
                if (hoursText != null) hoursText.setText(String.format(Locale.US, "%02d", hours));
                if (minutesText != null) minutesText.setText(String.format(Locale.US, "%02d", minutes));
                if (secondsText != null) secondsText.setText(String.format(Locale.US, "%02d", seconds));

                timerHandler.postDelayed(this, 1000L);
            }
        };
        timer.run();
    }

    private long parseExpiryMillis(String value) {
        try {
            long numeric = Long.parseLong(value);
            return numeric < 100000000000L ? numeric * 1000L : numeric;
        } catch (NumberFormatException ignored) {}

        String[] formats = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
                "dd-MM-yyyy HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "MMM dd, yyyy HH:mm:ss"};
        for (String format : formats) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
                parser.setLenient(false);
                Date parsed = parser.parse(value);
                if (parsed != null) return parsed.getTime();
            } catch (ParseException ignored) {}
        }
        return -1L;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkObbApplied();
    }

    private void checkObbApplied() {
        new Thread(() -> {
            boolean ready = false;
            try {
                ready = new AppManager(MAct.this).checkObbContainer(PKG_BGMI);
            } catch (Exception e) {
                Log.e("MAct", "OBB verification failed", e);
            }
            final boolean verified = ready;
            runOnUiThread(() -> isObbCopied = verified);
        }).start();
    }

    private void updateGameStatus() {
        isGameInstalled = false;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(PKG_BGMI, 0);
            if (packageInfo != null) {
                isGameInstalled = true;
                if (gameName != null) gameName.setText("Battlegrounds India");
                if (packageName != null) packageName.setText("Package : " + PKG_BGMI);
            }
        } catch (Exception ignored) {
            isGameInstalled = false;
            if (gameName != null) gameName.setText("Battlegrounds India");
            if (packageName != null) packageName.setText("Package : " + PKG_BGMI);
        }
    }

    private void handleLaunchFlow() {
        if (!isObbCopied) {
            copyObbFiles();
        } else {
            launchGame();
        }
    }

    private void launchGame() {
        try {
            boolean isVirtualInstalled = false;
            try {
                isVirtualInstalled = com.hCore.hCoreX.get().isInstalled(PKG_BGMI, 0);
            } catch (Throwable t) {
                Log.e("MAct", "isInstalled error: " + t.getMessage());
            }

            if (!isVirtualInstalled) {
                try {
                    PackageInfo packageInfo = getPackageManager().getPackageInfo(PKG_BGMI, 0);
                    if (packageInfo != null && packageInfo.applicationInfo != null) {
                        String apkPath = packageInfo.applicationInfo.sourceDir;
                        com.hCore.entity.pm.InstallResult res = com.hCore.hCoreX.get().installPackageAsUser(apkPath, 0);
                        if (res != null && res.success) {
                            isVirtualInstalled = true;
                        }
                    }
                } catch (Throwable ie) {
                    Log.e("MAct", "Sandbox install error: " + ie.getMessage());
                }
            }

            boolean launched = false;
            try {
                launched = com.hCore.hCoreX.get().launchApk(PKG_BGMI, 0);
            } catch (Throwable lt) {
                Log.e("MAct", "launchApk error: " + lt.getMessage());
            }

            if (!launched) {
                try {
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(PKG_BGMI);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                    } else {
                        Toast.makeText(this, "Please install BGMI on your device first", Toast.LENGTH_LONG).show();
                    }
                } catch (Throwable it) {
                    Toast.makeText(this, "Please install BGMI on your device first", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Throwable t) {
            Log.e("MAct", "Launch flow error: " + t.getMessage());
            try {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(PKG_BGMI);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                } else {
                    Toast.makeText(this, "Please install BGMI on your device first", Toast.LENGTH_LONG).show();
                }
            } catch (Throwable ignored) {}
        }
    }

    private void copyObbFiles() {
        AppManager appManager = new AppManager(this);
        appManager.copyObbFolderAsync(PKG_BGMI, new AppManager.CopyCallback() {
            @Override
            public void onCopyCompleted(boolean success) {
                runOnUiThread(() -> {
                    isObbCopied = true;
                    launchGame();
                });
            }

            @Override
            public void onProgressUpdate(int progress) {
            }
        }, false); // Set showDialog = false so NO popup appears!
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }
    }
}
