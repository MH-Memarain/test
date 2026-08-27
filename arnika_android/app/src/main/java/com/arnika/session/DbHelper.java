package com.arnika.session;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    public static class Note{public long id;public String title,folder,text;public long updated;Note(long i,String t,String f,String x,long u){id=i;title=t;folder=f;text=x;updated=u;}}
    public static class Sample{public long id;public String name,path,fp;public boolean def;Sample(long i,String n,String p,String f,boolean d){id=i;name=n;path=p;fp=f;def=d;}}
    public DbHelper(Context c){super(c,"arnika.db",null,1);}
    public void onCreate(SQLiteDatabase d){d.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,folder TEXT,body TEXT,updated INTEGER)");d.execSQL("CREATE TABLE samples(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,path TEXT,fp TEXT,is_default INTEGER DEFAULT 0,created INTEGER)");}
    public void onUpgrade(SQLiteDatabase d,int a,int b){}
    public long save(long id,String title,String folder,String body){ContentValues v=new ContentValues();v.put("title",title.trim().isEmpty()?"Session":title.trim());v.put("folder",folder.trim().isEmpty()?"Sessions":folder.trim());v.put("body",body);v.put("updated",System.currentTimeMillis());if(id>0){getWritableDatabase().update("notes",v,"id=?",new String[]{""+id});return id;}return getWritableDatabase().insert("notes",null,v);}
    public List<Note> notes(){ArrayList<Note> o=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,title,folder,body,updated FROM notes ORDER BY updated DESC",null);try{while(c.moveToNext())o.add(new Note(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4)));}finally{c.close();}return o;}
    public void delNote(long id){getWritableDatabase().delete("notes","id=?",new String[]{""+id});}
    public long addSample(String n,String p,String f){SQLiteDatabase d=getWritableDatabase();boolean first=samples().isEmpty();if(first)d.execSQL("UPDATE samples SET is_default=0");ContentValues v=new ContentValues();v.put("name",n);v.put("path",p);v.put("fp",f);v.put("is_default",first?1:0);v.put("created",System.currentTimeMillis());return d.insert("samples",null,v);}
    public List<Sample> samples(){ArrayList<Sample> o=new ArrayList<>();Cursor c=getReadableDatabase().rawQuery("SELECT id,name,path,fp,is_default FROM samples ORDER BY is_default DESC,created DESC",null);try{while(c.moveToNext())o.add(new Sample(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4)==1));}finally{c.close();}return o;}
    public Sample def(){for(Sample s:samples())if(s.def)return s;return null;}
    public void setDef(long id){SQLiteDatabase d=getWritableDatabase();d.execSQL("UPDATE samples SET is_default=0");ContentValues v=new ContentValues();v.put("is_default",1);d.update("samples",v,"id=?",new String[]{""+id});}
    public void delSample(long id){getWritableDatabase().delete("samples","id=?",new String[]{""+id});}
}
