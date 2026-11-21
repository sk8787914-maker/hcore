package com.chet.mi.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.chet.mi.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hCore.hCoreX;
import com.hCore.entity.pm.InstallResult;

import org.lsposed.lsparanoid.Obfuscate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Obfuscate
public class AppManager extends AppCompatActivity {

    private final Context ctx;

    public static final int INSTALL_APP = 1;
    public static final int COPY_OBB = 3;

    public interface CopyCallback {
        void onCopyCompleted(boolean success);
        default void onProgressUpdate(int progress) {}
    }

    public AppManager(Context ctx) {
        this.ctx = ctx;
    }

    @SuppressLint("StaticFieldLeak")
    public void appManager(String pkg, int method, CopyCallback callback, boolean showDialog) {

        new AsyncTask<Void, Integer, Boolean>() {

            String errorMessage = "";

            Dialog progressDialog;
            ProgressBar progressBar;
            TextView progressText;
            TextView progressSubtext;

            long totalBytes = 0;
            long copiedBytes = 0;

            @Override
            protected void onPreExecute() {

                if (method == COPY_OBB && showDialog) {

                    progressDialog = new Dialog(ctx);
                    progressDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                    progressDialog.setContentView(R.layout.progress_dialog);
                    if (progressDialog.getWindow() != null) {
                        progressDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }
                    progressDialog.setCancelable(false);

                    progressBar = progressDialog.findViewById(R.id.progressBar);
                    progressText = progressDialog.findViewById(R.id.progressText);
                    progressSubtext = progressDialog.findViewById(R.id.progressSubtext);

                    if (progressBar != null) {
                        progressBar.setMax(100);
                        progressBar.setProgress(0);
                    }

                    if (progressText != null) {
                        progressText.setText("0%");
                    }
                    if (progressSubtext != null) {
                        progressSubtext.setText("Copying OBB: 0%");
                    }

                    progressDialog.show();
                }
            }

            @Override
            protected Boolean doInBackground(Void... voids) {

                if (method == COPY_OBB) {
                    return copyObbFolder();
                }

                if (method == INSTALL_APP) {
                    return installApp(pkg);
                }

                return false;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                if (callback != null) {
                    callback.onProgressUpdate(values[0]);
                }

                if (progressBar != null) {
                    progressBar.setProgress(values[0]);
                }

                if (progressText != null) {
                    progressText.setText(values[0] + "%");
                }
                if (progressSubtext != null) {
                    progressSubtext.setText("Copying OBB: " + values[0] + "%");
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignored) {}
                }

                if (callback != null) {
                    callback.onCopyCompleted(success);
                }
            }

            private boolean installApp(String pkg) {
                try {
                    if (hCoreX.get().isInstalled(pkg, 0)) {
                        return true;
                    }
                    PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(pkg, 0);
                    String apkPath = packageInfo.applicationInfo.sourceDir;
                    InstallResult res = hCoreX.get().installPackageAsUser(apkPath, 0);
                    if (res != null && res.success) {
                        return true;
                    } else {
                        errorMessage = "Sandbox install error: " + (res != null ? res.msg : "Unknown");
                        return false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    errorMessage = "Package not found!";
                    return false;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }

            private boolean copyObbFolder() {

                try {

                    File sourceDir = new File("/storage/emulated/0/Android/obb/" + pkg);

                    if (!sourceDir.isDirectory() || !sourceDir.canRead()) {
                        if (copyObbWithRoot(pkg)) return true;
                        errorMessage = "Direct OBB access denied.";
                        return false;
                    }

                    File destDir = getMundoObbDir(pkg);

                    if (sourceDir.getAbsolutePath().equals(destDir.getAbsolutePath())) {
                        return true;
                    }

                    if (!destDir.isDirectory() && !destDir.mkdirs()) {
                        errorMessage = "Unable to create virtual OBB destination";
                        return false;
                    }

                    File mainObb = findMainObb(sourceDir, pkg);
                    if (mainObb == null || mainObb.length() <= 0) {
                        if (copyObbWithRoot(pkg)) return true;
                        errorMessage = "Main OBB missing: " + sourceDir.getAbsolutePath();
                        return false;
                    }

                    totalBytes = mainObb.length();
                    copiedBytes = 0;

                    if (!copyFile(mainObb, new File(destDir, mainObb.getName()))) return false;
                    File[] patches = sourceDir.listFiles((dir, name) -> name.matches(
                            "^patch\\.[0-9]+\\." + Pattern.quote(pkg) + "\\.obb$"));
                    if (patches != null) {
                        for (File patch : patches) {
                            if (patch.length() > 0 && !copyFile(patch, new File(destDir, patch.getName()))) return false;
                        }
                    }
                    return checkObbContainer(pkg);

                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }

            private File findMainObb(File sourceDir, String packageName) {
                File[] files = sourceDir.listFiles();
                if (files == null) return null;

                String pattern = "^main\\.[0-9]+\\." + Pattern.quote(packageName) + "\\.obb$";
                File selected = null;
                long selectedVersion = -1L;
                for (File file : files) {
                    if (!file.isFile() || !file.getName().matches(pattern)) continue;
                    String[] parts = file.getName().split("\\.");
                    long version = -1L;
                    if (parts.length > 1) {
                        try {
                            version = Long.parseLong(parts[1]);
                        } catch (NumberFormatException ignored) {}
                    }
                    if (selected == null || version > selectedVersion) {
                        selected = file;
                        selectedVersion = version;
                    }
                }
                return selected;
            }

            private boolean copyFile(File source, File target) {
                File partial = new File(target.getParentFile(), target.getName() + ".partial");
                try {
                    File parent = target.getParentFile();
                    if (parent == null || (!parent.isDirectory() && !parent.mkdirs()))
                        throw new java.io.IOException("Cannot create OBB destination");
                    if (!source.isFile() || !source.canRead() || source.length() <= 0)
                        throw new java.io.IOException("OBB source unreadable or empty");
                    totalBytes = source.length();
                    if (target.isFile() && target.length() == totalBytes) {
                        publishProgress(100);
                        return true;
                    }
                    try (FileInputStream input = new FileInputStream(source);
                         FileOutputStream output = new FileOutputStream(partial)) {
                        byte[] buffer = new byte[256 * 1024];
                        int length;
                        int previousProgress = -1;
                        while ((length = input.read(buffer)) != -1) {
                            output.write(buffer, 0, length);
                            copiedBytes += length;
                            int progress = (int) Math.min(100L, copiedBytes * 100L / totalBytes);
                            if (progress != previousProgress) { publishProgress(progress); previousProgress = progress; }
                        }
                        output.getFD().sync();
                    }
                    if (partial.length() != totalBytes)
                        throw new java.io.IOException("Incomplete OBB copy");
                    if (target.exists() && !target.delete())
                        throw new java.io.IOException("Cannot replace old OBB");
                    if (!partial.renameTo(target))
                        throw new java.io.IOException("Cannot finalize OBB copy");
                    return target.isFile() && target.length() == totalBytes;
                } catch (Exception e) {
                    errorMessage = "OBB copy failed: " + e.getMessage();
                    return false;
                } finally {
                    if (partial.exists()) partial.delete();
                }
            }

        }.execute();
    }

    private boolean copyObbWithRoot(String pkg) {
        if (!pkg.matches("[A-Za-z0-9_.]+")) return false;
        File dest = getMundoObbDir(pkg);
        if (!dest.isDirectory() && !dest.mkdirs()) return false;
        String src = "/storage/emulated/0/Android/obb/" + pkg;
        String script = "set -e; found=0; for f in '" + src + "'/main.*.obb '" + src + "'/patch.*.obb; do "
                + "[ -f \"$f\" ] || continue; "
                + "name=${f##*/}; case \"$name\" in *'" + pkg + "'.obb) ;; *) continue;; esac; "
                + "cp \"$f\" '" + dest.getAbsolutePath().replace("'", "'\\''") + "'/\"$name\".partial; "
                + "cmp \"$f\" '" + dest.getAbsolutePath().replace("'", "'\\''") + "'/\"$name\".partial; "
                + "mv -f '" + dest.getAbsolutePath().replace("'", "'\\''") + "'/\"$name\".partial '" + dest.getAbsolutePath().replace("'", "'\\''") + "'/\"$name\"; done";
        Process proc = null;
        try {
            proc = new ProcessBuilder("su", "-c", script).redirectErrorStream(true).start();
            try (InputStream in = proc.getInputStream()) {
                byte[] buffer = new byte[4096];
                while (in.read(buffer) != -1) { /* drain process output */ }
            }
            if (!proc.waitFor(900, TimeUnit.SECONDS)) { proc.destroyForcibly(); return false; }
            return proc.exitValue() == 0 && checkObbContainer(pkg);
        } catch (Exception ignored) {
            return false;
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    private File getMundoObbDir(String pkg) {
        File dir = new File(ctx.getFilesDir(), "virtual/data/app/" + pkg + "/obb");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public boolean checkObbContainer(String packageName) {
        File destDir = getMundoObbDir(packageName);
        if (!destDir.exists() || !destDir.isDirectory()) return false;
        File[] files = destDir.listFiles();
        if (files == null) return false;
        String pattern = "^main\\.[0-9]+\\." + Pattern.quote(packageName) + "\\.obb$";
        for (File file : files) {
            if (file.isFile() && file.length() > 0 && file.getName().matches(pattern)) return true;
        }
        return false;
    }

    public void copyObbFolderAsync(String pkg, final CopyCallback callback) {
        copyObbFolderAsync(pkg, callback, true);
    }

    public void copyObbFolderAsync(String pkg, final CopyCallback callback, boolean showDialog) {
        if (checkObbContainer(pkg)) {
            if (callback != null) {
                callback.onCopyCompleted(true);
            }
            return;
        }

        appManager(pkg, COPY_OBB, callback, showDialog);
    }
}
