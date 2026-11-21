package com.chet.mi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import android.text.InputType;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chet.mi.utils.Prefs;

import org.lsposed.lsparanoid.Obfuscate;

@Obfuscate
public class LogAct extends AppCompatActivity {

    static {
        try {
            System.loadLibrary("ReNnLd");
        } catch (UnsatisfiedLinkError ignored) {
        }
    }

    private Prefs prefs;
    private final String USER = "USER";

    private EditText textUsername;
    private AppCompatButton btnLogin;

    // UI
    private TextView premium;
    private LinearLayout loginCard;
    private LinearLayout mainContainer;
    private ImageView logo;
    private ImageView eyeToggle;
    private ImageView btnPaste;
    private View btnSecure;
    private TextView deviceIDText;
    private boolean isPasswordVisible = false;

    // Permissions
    private static final int REQUEST_MANAGE_STORAGE_PERMISSION = 100;
    private static final int REQUEST_MANAGE_UNKNOWN_APP_SOURCES = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (iskokofm()) {
            new AlertDialog.Builder(LogAct.this)
                    .setTitle("VPN Detected")
                    .setMessage("Please disable VPN to continue")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        setContentView(R.layout.activity_login);
        initTts();
        prefs = new Prefs(this);
        checkAndRequestPermissions();

        textUsername = findViewById(R.id.userkey);
        btnLogin = findViewById(R.id.login);
        premium = findViewById(R.id.premium);
        loginCard = findViewById(R.id.loginCard);
        mainContainer = findViewById(R.id.mainContainer);
        logo = findViewById(R.id.logo);
        eyeToggle = findViewById(R.id.eyeToggle);
        btnPaste = findViewById(R.id.btnPaste);
        btnSecure = findViewById(R.id.btnSecure);
        deviceIDText = findViewById(R.id.deviceIDText);

        textUsername.setText(prefs.getSt(USER, ""));

        // Display actual Device Model Name dynamically
        try {
            String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toUpperCase() : "DEVICE";
            String model = Build.MODEL != null ? Build.MODEL : "";
            String device = Build.DEVICE != null ? Build.DEVICE : "";
            String fullDeviceName = manufacturer + " " + model + " (" + device + ")";
            if (deviceIDText != null) {
                deviceIDText.setText(fullDeviceName);
            }
        } catch (Exception ignored) {}

        if (eyeToggle != null) {
            eyeToggle.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    textUsername.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyeToggle.setAlpha(0.5f);
                } else {
                    textUsername.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyeToggle.setAlpha(1.0f);
                }
                isPasswordVisible = !isPasswordVisible;
                textUsername.setSelection(textUsername.getText().length());
            });
        }

        if (btnPaste != null) {
            btnPaste.setOnClickListener(v -> {
                try {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                        CharSequence pasteData = clipboard.getPrimaryClip().getItemAt(0).getText();
                        if (pasteData != null && pasteData.length() > 0) {
                            textUsername.setText(pasteData.toString().trim());
                            textUsername.setSelection(textUsername.getText().length());
                            Toast.makeText(LogAct.this, "Key pasted from clipboard", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LogAct.this, "Clipboard is empty", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(LogAct.this, "Unable to read clipboard", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnSecure != null) {
            btnSecure.setOnClickListener(v ->
                Toast.makeText(LogAct.this, "✔ 256-Bit SSL Encrypted Gateway Active", Toast.LENGTH_SHORT).show()
            );
        }

        startIntroAnimation();

        btnLogin.setOnClickListener(v -> {
            String userKey = textUsername.getText().toString().trim();
            if (userKey.isEmpty()) {
                textUsername.setError("Please enter license key");
                return;
            }
            if (iskokofm()) {
                new AlertDialog.Builder(LogAct.this)
                        .setTitle("VPN Detected")
                        .setMessage("Please disable VPN to continue")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            prefs.setSt(USER, userKey);

            btnLogin.setEnabled(false);
            btnLogin.setText("VERIFYING KEY...");
            btnLogin.setTextColor(Color.WHITE);

            Login(this, userKey);
        });
    }

    private void startIntroAnimation() {
        if (mainContainer != null) {
            mainContainer.setVisibility(View.VISIBLE);
            mainContainer.setAlpha(1f);
        }
        if (logo != null) {
            logo.setAlpha(0f);
            logo.setTranslationY(-120f);
            logo.animate().alpha(1f).translationY(0f).setDuration(1000).setInterpolator(new DecelerateInterpolator(1.4f)).start();
            ObjectAnimator logoFloat = ObjectAnimator.ofFloat(logo, View.TRANSLATION_Y, 0f, -6f, 0f);
            logoFloat.setStartDelay(1100);
            logoFloat.setDuration(2600);
            logoFloat.setRepeatCount(ValueAnimator.INFINITE);
            logoFloat.start();
        }
        View logoGlow = findViewById(R.id.logoGlow);
        if (logoGlow != null) {
            ObjectAnimator glowRotation = ObjectAnimator.ofFloat(logoGlow, View.ROTATION, 0f, 360f);
            glowRotation.setDuration(7000);
            glowRotation.setInterpolator(new LinearInterpolator());
            glowRotation.setRepeatCount(ValueAnimator.INFINITE);
            glowRotation.start();
            logoGlow.animate().alpha(0.8f).setDuration(1200).setInterpolator(new DecelerateInterpolator()).start();
        }
        if (premium != null) {
            premium.setAlpha(0f);
            premium.setTranslationY(80f);
            premium.animate().alpha(1f).translationY(0f).setDuration(1000).setStartDelay(350).setInterpolator(new DecelerateInterpolator(1.4f)).start();
        }
        if (loginCard != null) {
            loginCard.setAlpha(0f);
            loginCard.setTranslationY(180f);
            loginCard.animate().alpha(1f).translationY(0f).setDuration(1200).setStartDelay(450).setInterpolator(new DecelerateInterpolator(1.4f)).start();
        }
    }

    private Dialog loadingDialog;
    private TextToSpeech tts;

    private void initTts() {
        try {
            tts = new TextToSpeech(getApplicationContext(), status -> {
                if (status != TextToSpeech.ERROR && tts != null) {
                    tts.setLanguage(Locale.US);
                    tts.setPitch(1.2f); // High-pitch female voice tone
                    tts.setSpeechRate(0.95f);
                }
            });
        } catch (Exception ignored) {}
    }

    private void speakWelcome() {
        if (tts != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak("Welcome to Uncheated Loader. Key verified successfully.", TextToSpeech.QUEUE_FLUSH, null, "WELCOME");
                } else {
                    tts.speak("Welcome to Uncheated Loader. Key verified successfully.", TextToSpeech.QUEUE_FLUSH, null);
                }
            } catch (Exception ignored) {}
        }
    }

    private void showIosLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_ios_loading);
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loadingDialog.getWindow().setDimAmount(0.65f);
        }
        loadingDialog.setCancelable(false);
        loadingDialog.show();
    }

    private void hideIosLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception ignored) {}
        }
    }

    private void Login(final Context m_Context, final String userKey) {
        showIosLoadingDialog();

        btnLogin.setEnabled(false);
        btnLogin.setText("VERIFYING KEY...");
        btnLogin.setTextColor(Color.WHITE);

        Handler loginHandler = new Handler(Looper.getMainLooper(), msg -> {
            hideIosLoadingDialog();
            if (msg.what == 0) {
                // Play female welcome voice note
                speakWelcome();
                
                // Success — go to main screen
                Intent i = new Intent(m_Context, MAct.class);
                startActivity(i);
                finish();
            } else {
                // Failure — re-enable button and show error from server
                btnLogin.setEnabled(true);
                btnLogin.setText("LOGIN");
                btnLogin.setTextColor(Color.WHITE);
                String errMsg = (String) msg.obj;
                new AlertDialog.Builder(LogAct.this)
                        .setTitle("Login Failed")
                        .setMessage(errMsg)
                        .setPositiveButton("OK", null)
                        .show();
            }
            return true;
        });

        new Thread(() -> {
            String result = "";
            try {
                result = Check(m_Context, userKey);
            } catch (Exception e) {
                result = "Connection error: " + e.getMessage();
            }

            if ("OK".equals(result)) {
                loginHandler.sendEmptyMessage(0);
            } else {
                Message msg = Message.obtain();
                msg.what = 1;
                msg.obj = (result != null && !result.isEmpty()) ? result : "Invalid License Key";
                loginHandler.sendMessage(msg);
            }
        }).start();
    }

    private void checkAndRequestPermissions() {
        if (!isStoragePermissionGranted()) {
            requestStoragePermissionDirect();
        } else if (!canRequestPackageInstalls()) {
            requestUnknownAppPermissionsDirect();
        }
    }

    private boolean isStoragePermissionGranted() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager();
    }

    private void requestStoragePermissionDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.fromParts("package", getPackageName(), null));
            startActivityForResult(intent, REQUEST_MANAGE_STORAGE_PERMISSION);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_MANAGE_STORAGE_PERMISSION);
        }
    }

    private boolean canRequestPackageInstalls() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || getPackageManager().canRequestPackageInstalls();
    }

    private void requestUnknownAppPermissionsDirect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_MANAGE_UNKNOWN_APP_SOURCES);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MANAGE_STORAGE_PERMISSION) {
            checkAndRequestPermissions();
        } else if (requestCode == REQUEST_MANAGE_UNKNOWN_APP_SOURCES) {
            if (canRequestPackageInstalls()) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    private boolean iskokofm() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static native String Check(Context mContext, String userKey);
}
