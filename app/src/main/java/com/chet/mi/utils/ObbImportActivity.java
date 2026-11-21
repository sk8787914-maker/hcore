package com.chet.mi.utils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.util.regex.Pattern;

/** SAF import for user-selected OBB files. No storage permission required. */
public final class ObbImportActivity extends Activity {
    private static final int REQUEST_OBB = 701;
    private String packageName;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        packageName = getIntent().getStringExtra("package_name");
        if (packageName == null || !packageName.matches("[A-Za-z0-9_.]+")) { finish(); return; }
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.setType("*/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(picker, REQUEST_OBB);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQUEST_OBB || result != RESULT_OK || data == null) { finish(); return; }
        final java.util.ArrayList<Uri> selected = new java.util.ArrayList<>();
        if (data.getClipData() != null) {
            for (int i=0; i<data.getClipData().getItemCount(); i++)
                selected.add(data.getClipData().getItemAt(i).getUri());
        } else if (data.getData() != null) selected.add(data.getData());
        new Thread(() -> {
            int count = 0;
            String failure = null;
            for (Uri uri : selected) {
                try { importOne(uri); count++; }
                catch (Exception e) { failure = e.getMessage(); break; }
            }
            final int done = count;
            final String error = failure;
            runOnUiThread(() -> {
                Toast.makeText(this, error == null ? "Imported " + done + " OBB file(s)" : "Import failed: " + error, Toast.LENGTH_LONG).show();
                finish();
            });
        }).start();
    }
    private void importOne(Uri uri) throws Exception {
        String name = null;
        try (android.database.Cursor c = getContentResolver().query(uri,
                new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) name = c.getString(0);
        }
        if (name == null || !name.matches("^(main|patch)\\.[0-9]+\\." + Pattern.quote(packageName) + "\\.obb$"))
            throw new IOException("Select a main/patch OBB for " + packageName);
        File dest = new File(getFilesDir(), "virtual/data/app/" + packageName + "/obb");
        if (!dest.isDirectory() && !dest.mkdirs()) throw new IOException("Cannot create destination");
        File tmp = new File(dest, name + ".partial");
        File out = new File(dest, name);
        long bytes = 0;
        try {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(tmp)) {
                if (in == null) throw new IOException("Cannot open selected file");
                byte[] buf = new byte[128 * 1024]; int n;
                while ((n = in.read(buf)) != -1) { output.write(buf, 0, n); bytes += n; }
                output.getFD().sync();
            }
            if (bytes == 0 || tmp.length() != bytes) throw new IOException("Incomplete file");
            if (out.exists() && !out.delete()) throw new IOException("Cannot replace old OBB");
            if (!tmp.renameTo(out)) throw new IOException("Cannot finalize OBB");
        } finally { if (tmp.exists()) tmp.delete(); }
    }
}
