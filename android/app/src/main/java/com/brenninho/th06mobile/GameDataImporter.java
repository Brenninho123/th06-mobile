package com.brenninho.th06mobile;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GameDataImporter {
    public interface Listener {
        void onProgress(String fileName, int percent);
    }

    private static final String DAT_PREFIX = "紅魔郷";
    private static final String CONFIG_FILE = "東方紅魔郷.cfg";
    private static final String[] DAT_SUFFIXES = {"CM.DAT", "ED.DAT", "IN.DAT", "MD.DAT", "ST.DAT", "TL.DAT"};
    private static final String[] COPIED_DIRECTORIES = {"bgm", "replay"};

    private static final class Entry {
        final Uri uri;
        final String target;
        final long size;
        final boolean keepExisting;

        Entry(Uri uri, String target, long size, boolean keepExisting) {
            this.uri = uri;
            this.target = target;
            this.size = size;
            this.keepExisting = keepExisting;
        }
    }

    private GameDataImporter() {
    }

    public static File dataDir(Context context) {
        return context.getFilesDir();
    }

    public static List<String> missingFiles(Context context) {
        List<String> missing = new ArrayList<>();
        File dir = dataDir(context);
        for (String suffix : DAT_SUFFIXES) {
            String name = DAT_PREFIX + suffix;
            if (!new File(dir, name).isFile()) {
                missing.add(name);
            }
        }
        return missing;
    }

    public static boolean hasGameData(Context context) {
        return missingFiles(context).isEmpty();
    }

    public static void importFromTree(Context context, Uri treeUri, Listener listener) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String rootId = DocumentsContract.getTreeDocumentId(treeUri);
        List<Entry> entries = new ArrayList<>();

        collect(resolver, treeUri, rootId, "", entries);

        boolean foundDat = false;
        long totalBytes = 0;
        for (Entry entry : entries) {
            if (entry.target.startsWith(DAT_PREFIX)) {
                foundDat = true;
            }
            totalBytes += Math.max(entry.size, 0);
        }

        if (!foundDat) {
            throw new IOException("no Touhou 6 .DAT files were found in the selected folder");
        }

        File dir = dataDir(context);
        byte[] buffer = new byte[256 * 1024];
        long copiedBytes = 0;
        int lastPercent = -1;

        for (Entry entry : entries) {
            File output = new File(dir, entry.target);
            if (entry.keepExisting && output.exists()) {
                copiedBytes += Math.max(entry.size, 0);
                continue;
            }

            File parent = output.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IOException("cannot create " + parent.getName());
            }

            File temp = new File(output.getPath() + ".part");
            InputStream input = resolver.openInputStream(entry.uri);
            if (input == null) {
                throw new IOException("cannot open " + entry.target);
            }

            try {
                OutputStream out = new FileOutputStream(temp);
                try {
                    int read;
                    while ((read = input.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                        copiedBytes += read;
                        int percent = totalBytes > 0 ? (int) Math.min(100, copiedBytes * 100 / totalBytes) : 0;
                        if (percent != lastPercent && listener != null) {
                            lastPercent = percent;
                            listener.onProgress(entry.target, percent);
                        }
                    }
                } finally {
                    out.close();
                }
            } finally {
                input.close();
            }

            if (output.exists() && !output.delete()) {
                throw new IOException("cannot replace " + entry.target);
            }
            if (!temp.renameTo(output)) {
                throw new IOException("cannot write " + entry.target);
            }
        }

        File replayDir = new File(dir, "replay");
        if (!replayDir.isDirectory()) {
            replayDir.mkdirs();
        }
    }

    private static void collect(ContentResolver resolver, Uri treeUri, String documentId, String relativeDir,
                                List<Entry> entries) throws IOException {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId);
        String[] projection = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        };

        Cursor cursor = resolver.query(childrenUri, projection, null, null, null);
        if (cursor == null) {
            throw new IOException("cannot list the selected folder");
        }

        try {
            while (cursor.moveToNext()) {
                String childId = cursor.getString(0);
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                long size = cursor.isNull(3) ? -1 : cursor.getLong(3);

                if (name == null) {
                    continue;
                }

                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
                    if (relativeDir.isEmpty()) {
                        String directory = mapDirectory(name);
                        if (directory != null) {
                            collect(resolver, treeUri, childId, directory + "/", entries);
                        }
                    }
                    continue;
                }

                String target = mapFile(relativeDir, name);
                if (target != null) {
                    Uri fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId);
                    boolean keepExisting = target.equals("score.dat") || target.equals(CONFIG_FILE);
                    entries.add(new Entry(fileUri, target, size, keepExisting));
                }
            }
        } finally {
            cursor.close();
        }
    }

    private static String mapDirectory(String name) {
        for (String directory : COPIED_DIRECTORIES) {
            if (directory.equalsIgnoreCase(name)) {
                return directory;
            }
        }
        return null;
    }

    private static String mapFile(String relativeDir, String name) {
        if (!relativeDir.isEmpty()) {
            return relativeDir + name.toLowerCase(Locale.ROOT);
        }

        String upper = name.toUpperCase(Locale.ROOT);

        if (upper.equals("SCORE.DAT")) {
            return "score.dat";
        }

        for (String suffix : DAT_SUFFIXES) {
            if (upper.endsWith(suffix) && upper.length() > suffix.length()) {
                return DAT_PREFIX + suffix;
            }
        }

        if (upper.endsWith(".CFG")) {
            return CONFIG_FILE;
        }

        if (upper.startsWith("MSGOTHIC") && (upper.endsWith(".TTC") || upper.endsWith(".TTF"))) {
            return "msgothic.ttc";
        }

        return null;
    }
}
