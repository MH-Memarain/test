package com.arnika.session;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.io.File;
import java.io.FileOutputStream;

public final class VoiceEngine{
    public interface Callback{void done(String fp,File file,String error);}
    private VoiceEngine(){}
    public static void capture(Context c,File out,int ms,Callback cb){new Thread(()->{try{if(c.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("Microphone permission");int sr=16000,min=AudioRecord.getMinBufferSize(sr,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,sr,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,4096));int total=sr*ms/1000;short[] all=new short[total],buf=new short[Math.max(1024,min/2)];int pos=0;r.startRecording();while(pos<total){int n=r.read(buf,0,Math.min(buf.length,total-pos));if(n>0){System.arraycopy(buf,0,all,pos,n);pos+=n;}}r.stop();r.release();if(out!=null)writeWav(out,all,sr);cb.done(fp(all),out,null);}catch(Exception e){cb.done(null,out,e.getMessage());}}).start();}
    private static String fp(short[] x){double sum=0,z=0;for(int i=0;i<x.length;i++){double v=x[i]/32768.0;sum+=v*v;if(i>0&&((x[i]>=0)!=(x[i-1]>=0)))z++;}double rms=Math.sqrt(sum/Math.max(1,x.length)),zc=z/Math.max(1,x.length-1);return String.format(java.util.Locale.US,"%.6f,%.6f",rms,zc);}
    public static double similarity(String a,String b){try{String[]x=a.split(","),y=b.split(",");double d=Math.abs(Double.parseDouble(x[0])-Double.parseDouble(y[0]))*5+Math.abs(Double.parseDouble(x[1])-Double.parseDouble(y[1]))*2;return Math.max(0,1-d);}catch(Exception e){return 0;}}
    private static void writeWav(File f,short[] s,int sr)throws Exception{FileOutputStream o=new FileOutputStream(f);int data=s.length*2;byte[] h=new byte[44];byte[] riff="RIFF".getBytes(),wave="WAVEfmt ".getBytes(),dat="data".getBytes();System.arraycopy(riff,0,h,0,4);le(h,4,36+data);System.arraycopy(wave,0,h,8,8);le(h,16,16);h[20]=1;h[22]=1;le(h,24,sr);le(h,28,sr*2);h[32]=2;h[34]=16;System.arraycopy(dat,0,h,36,4);le(h,40,data);o.write(h);for(short v:s){o.write(v&255);o.write((v>>8)&255);}o.close();}
    private static void le(byte[]b,int p,int v){b[p]=(byte)v;b[p+1]=(byte)(v>>8);b[p+2]=(byte)(v>>16);b[p+3]=(byte)(v>>24);}
}
