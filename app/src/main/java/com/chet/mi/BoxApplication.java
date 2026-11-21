package com.chet.mi;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;

import com.hCore.hCoreX;
import com.hCore.app.configuration.ClientConfiguration;
import com.hCore.app.configuration.AppLifecycleCallback;

public class BoxApplication extends Application {

    static {
        try {
            System.loadLibrary("ReNnLd");
        } catch (Throwable ignored) {}
    }

    public static native String getSdkKey();

    private static final String TAG = "BoxApplication";
    public static BoxApplication gApp;
    public static boolean ENABLE_IMGUI = false;

    public static BoxApplication get() {
        return gApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            hCoreX.get().doAttachBaseContext(base, new ClientConfiguration() {
                @Override
                public String getHostPackageName() {
                    return getPackageName();
                }

                @Override
                public boolean isHideRoot() {
                    return true;
                }

                @Override
                public boolean isHideXposed() {
                    return true;
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "hCore attachBaseContext error", t);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gApp = this;

        try {
            hCoreX.get().doCreate();
            hCoreX.get().addAppLifecycleCallback(new AppLifecycleCallback() {
                @Override
                public void beforeApplicationOnCreate(String packageName, String processName, Application app, int userId) {
                    if ("com.pubg.imobile".equals(packageName)) {
                        loadNativeLibsFromLoader();
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "hCore onCreate error", t);
        }
    }

    public void loadNativeLibsFromLoader() {
        try {
            File loaderDir = new File(getFilesDir(), "loader");
            if (loaderDir.exists() && loaderDir.isDirectory()) {
                File[] files = loaderDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".so")) {
                            try {
                                f.setReadable(true, false);
                                f.setWritable(true, false);
                                f.setExecutable(true, false);
                            } catch (Throwable ignored) {}
                        }
                    }

                    File bgmiLib = new File(loaderDir, "libbgmi.so");
                    if (bgmiLib.exists()) {
                        try {
                            System.load(bgmiLib.getAbsolutePath());
                            Log.d("Load", "libbgmi.so LOADED");
                        } catch (Throwable le) {
                            Log.e("Load", "libbgmi.so error: " + le.getMessage());
                        }
                    }

                    for (File f : files) {
                        if (f.isFile() && f.getName().endsWith(".so") && !f.getName().equalsIgnoreCase("libbgmi.so")) {
                            try {
                                System.load(f.getAbsolutePath());
                                Log.d("Load", f.getName() + " LOADED SUCCESSFULLY");
                            } catch (Throwable le) {
                                Log.e("Load", "Error loading " + f.getName(), le);
                            }
                        }
                    }
                }
            } else {
                Log.e("Load", "Loader directory NOT FOUND");
            }

        } catch (Throwable t) {
            Log.e("Load", "Library load error", t);
        }
    }
}
