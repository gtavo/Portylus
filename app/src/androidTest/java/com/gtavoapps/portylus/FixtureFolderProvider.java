package com.gtavoapps.portylus;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import java.io.*;

/** Read-only SAF hierarchy in the test APK; deletion and writes are deliberately unsupported. */
public final class FixtureFolderProvider extends DocumentsProvider {
    private static final String[] DOCS = { Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME,
        Document.COLUMN_MIME_TYPE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE };
    @Override public boolean onCreate() { return true; }
    @Override public boolean isChildDocument(String parent, String child) {
        if (parent.equals(child)) return true;
        if ("root".equals(parent)) return java.util.Arrays.asList("course","second","geometry","aula","zeta","one","two").contains(child);
        return "course".equals(parent) && java.util.Arrays.asList("geometry","one","two").contains(child);
    }
    @Override public Cursor queryRoots(String[] projection) {
        MatrixCursor c = new MatrixCursor(new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES});
        c.addRow(new Object[]{"root", "root", "Matemáticas", 0, "application/pdf"});
        return c;
    }
    private void add(MatrixCursor c, String id, String name, boolean folder, long time) {
        c.addRow(new Object[]{id, name, folder ? Document.MIME_TYPE_DIR : "application/pdf", (time == 0 ? 0 : 1792368000000L + time), 0, folder ? null : 1399});
    }
    @Override public Cursor queryDocument(String id, String[] projection) throws FileNotFoundException {
        MatrixCursor c = new MatrixCursor(DOCS);
        switch(id) {
            case "root": add(c,id,"Matemáticas",true,0); break;
            case "archive": add(c,id,"Archivo",true,0); break;
            case "course": add(c,id,"1º ESO",true,0); break;
            case "second": add(c,id,"2º ESO",true,0); break;
            case "geometry": add(c,id,"Geometría",true,0); break;
            case "aula": add(c,id,"Aula.pdf",false,1000); break;
            case "zeta": add(c,id,"Zeta.pdf",false,3000); break;
            case "one": add(c,id,"Tema 1.pdf",false,1000); break;
            case "two": add(c,id,"Tema 2.pdf",false,2000); break;
            default: throw new FileNotFoundException(id);
        }
        return c;
    }
    @Override public Cursor queryChildDocuments(String parent, String[] projection, String sort) throws FileNotFoundException {
        MatrixCursor c = new MatrixCursor(DOCS);
        switch(parent) {
            case "root":
                add(c,"course","1º ESO",true,0); add(c,"second","2º ESO",true,0);
                add(c,"zeta","Zeta.pdf",false,3000); add(c,"aula","Aula.pdf",false,1000); break;
            case "course":
                add(c,"geometry","Geometría",true,0);
                add(c,"two","Tema 2.pdf",false,2000); add(c,"one","Tema 1.pdf",false,1000); break;
            case "archive": case "second": case "geometry": break;
            default: throw new FileNotFoundException(parent);
        }
        return c;
    }
    @Override public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        queryDocument(id,null).close();
        File file = new File(getContext().getCacheDir(),"browser-fixture.pdf");
        if (!file.exists()) try (InputStream input=getContext().getAssets().open("base.pdf");
            OutputStream output=new FileOutputStream(file)) {
            byte[] buffer=new byte[8192]; int count;
            while((count=input.read(buffer))!=-1) output.write(buffer,0,count);
        } catch(IOException error) { throw new FileNotFoundException(error.toString()); }
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
    }
}
