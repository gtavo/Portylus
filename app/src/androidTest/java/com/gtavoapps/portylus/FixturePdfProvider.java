package com.gtavoapps.portylus;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/** Foreign read-only fixture. Java so the test APK can run without the target's Kotlin runtime. */
public final class FixturePdfProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "application/pdf"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] args, String order) {
        MatrixCursor cursor = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});
        cursor.addRow(new Object[]{"Externo.pdf"});
        return cursor;
    }
    @Override public synchronized ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode) || !("sample.pdf".equals(uri.getLastPathSegment()) || "other.pdf".equals(uri.getLastPathSegment())))
            throw new FileNotFoundException("Read only fixture");
        File file = new File(getContext().getCacheDir(), "fixture.pdf");
        if (!file.exists()) {
            try (InputStream input = getContext().getAssets().open("base.pdf");
                 FileOutputStream output = new FileOutputStream(file)) {
                byte[] bytes = new byte[8192];
                int count;
                while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            } catch (IOException error) { throw new FileNotFoundException(error.toString()); }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
