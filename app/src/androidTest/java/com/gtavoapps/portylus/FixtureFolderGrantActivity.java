package com.gtavoapps.portylus;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Test APK owns the provider and grants the same scoped tree access as a document picker. */
public final class FixtureFolderGrantActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        grantUriPermission("com.gtavoapps.portylus", Uri.parse("content://com.gtavoapps.portylus.test.folders/tree/root"), flags);
        grantUriPermission("com.gtavoapps.portylus", Uri.parse("content://com.gtavoapps.portylus.test.folders/tree/archive"), flags);
        finish();
    }
}
