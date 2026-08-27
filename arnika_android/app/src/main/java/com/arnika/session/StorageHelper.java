package com.arnika.session;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.io.OutputStream;

public final class StorageHelper{
    private StorageHelper(){}
    public static Uri tree(Context c){String s=c.getSharedPreferences("ui",Context.MODE_PRIVATE).getString("storage_tree",null);return s==null?null:Uri.parse(s);}
    public static Uri write(Context c,String name,String mime,byte[] data)throws Exception{Uri t=tree(c);if(t==null)return null;Uri parent=DocumentsContract.buildDocumentUriUsingTree(t,DocumentsContract.getTreeDocumentId(t));Uri f=DocumentsContract.createDocument(c.getContentResolver(),parent,mime,name);if(f==null)throw new Exception("Cannot create document");try(OutputStream o=c.getContentResolver().openOutputStream(f,"w")){if(o==null)throw new Exception("Cannot open output");o.write(data);}return f;}
}
