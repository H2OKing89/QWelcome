package com.kingpaging.qwelcome.testutil;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ImportFixtureProvider extends ContentProvider {
    public static final Uri CONTENT_URI = Uri.parse(
        "content://com.kingpaging.qwelcome.test.importfixture/template-pack.json"
    );

        private static final String TEMPLATE_PACK_JSON =
                "{"
                        + "\"schemaVersion\":1,"
                        + "\"kind\":\"template-pack\","
                        + "\"exportedAt\":\"2026-08-12T00:00:00Z\","
                        + "\"appVersion\":\"1.0.0\","
                        + "\"templates\":[{"
                        + "\"id\":\"fixture-template\","
                        + "\"name\":\"Fixture Template\","
                        + "\"content\":\"Welcome {{ customer_name }} to {{ ssid }}\","
                        + "\"createdAt\":\"2026-08-12T00:00:00Z\","
                        + "\"modifiedAt\":\"2026-08-12T00:00:00Z\","
                        + "\"slug\":\"fixture-template\","
                        + "\"sortOrder\":0,"
                        + "\"tags\":[]}],"
                        + "\"defaults\":{}}";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "application/json";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        if (!CONTENT_URI.equals(uri)) {
            throw new IllegalArgumentException("Unexpected URI: " + uri);
        }

        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            try (OutputStream outputStream = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                outputStream.write(TEMPLATE_PACK_JSON.getBytes(StandardCharsets.UTF_8));
            }
            return pipe[0];
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write import fixture", exception);
        }
    }

    @Override
    public Cursor query(
        Uri uri,
        String[] projection,
        String selection,
        String[] selectionArgs,
        String sortOrder
    ) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
        Uri uri,
        ContentValues values,
        String selection,
        String[] selectionArgs
    ) {
        return 0;
    }
}