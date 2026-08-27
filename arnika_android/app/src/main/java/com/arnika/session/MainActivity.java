package com.arnika.session;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements RecognitionListener{
    private static final int REQ_MIC=50,REQ_SAVE=51,REQ_TREE=52,REQ_AUDIO=53;
    private static final String[][] LANGS={{"خودکار / Auto","auto"},{"فارسی","fa-IR"},{"English","en-US"},{"العربية","ar-SA"},{"Italiano","it-IT"},{"Français","fr-FR"},{"中文","zh-CN"},{"Español","es-ES"},{"Deutsch","de-DE"},{"Русский","ru-RU"},{"Türkçe","tr-TR"},{"日本語","ja-JP"},{"한국어","ko-KR"},{"Português","pt-BR"},{"हिन्दी","hi-IN"},{"اردو","ur-PK"}};
    private static final String[][] TARGETS={{"فارسی","fa"},{"English","en"},{"العربية","ar"},{"Italiano","it"},{"Français","fr"},{"中文","zh"},{"Español","es"},{"Deutsch","de"},{"Русский","ru"},{"Türkçe","tr"},{"日本語","ja"},{"한국어","ko"},{"Português","pt"},{"हिन्दी","hi"},{"اردو","ur"}};
    private final Handler h=new Handler(Looper.getMainLooper());private SharedPreferences p;private DbHelper db;private SpeechRecognizer sr;private boolean listening=false;private String committed="",langDetected="";private long noteId=0;private EditText title,folder,body;private TextView status;private Button langBtn;private byte[] pending;private String pendingMime;private MediaPlayer player;private TextToSpeech tts;
    private int bg,card,card2,accent,accent2,fg,muted,gold,danger;

    @Override protected void onCreate(Bundle b){super.onCreate(b);p=getSharedPreferences("ui",MODE_PRIVATE);db=new DbHelper(this);palette();build();}
    @Override protected void onDestroy(){stop();if(player!=null)try{player.release();}catch(Exception ignored){}if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    private GradientDrawable box(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    private TextView tv(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.RIGHT);t.setPadding(dp(5),dp(5),dp(5),dp(5));if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,int c){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setTextSize(12);b.setBackground(box(c,14));return b;}
    private EditText edit(String hint,boolean multi){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted);e.setTextColor(fg);e.setTextSize(15);e.setPadding(dp(14),dp(11),dp(14),dp(11));e.setBackground(box(card,14));e.setGravity(Gravity.RIGHT|(multi?Gravity.TOP:Gravity.CENTER_VERTICAL));if(multi){e.setMinLines(11);e.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);}return e;}
    private void palette(){String t=p.getString("theme","midnight");if("classic".equals(t)){bg=0xff1d1915;card=0xff302820;card2=0xff493a29;accent=0xff9b6d2e;accent2=0xff287ca8;fg=0xfff7edda;muted=0xffc1ad8b;gold=0xfff0bd59;danger=0xffb94b5a;}else if("light".equals(t)){bg=0xffeef5fb;card=Color.WHITE;card2=0xffdfeaf5;accent=0xff0f5fa9;accent2=0xff0aa7c7;fg=0xff172b42;muted=0xff5d748a;gold=0xffa77115;danger=0xffcf4256;}else if("neon".equals(t)){bg=0xff0c081f;card=0xff21133e;card2=0xff32185d;accent=0xff6d3eff;accent2=0xff00d1ef;fg=Color.WHITE;muted=0xffbcaaDE;gold=0xffffd166;danger=0xffd94670;}else{bg=0xff061126;card=0xff0d2040;card2=0xff15315b;accent=0xff1767d1;accent2=0xff00b6e6;fg=0xffeef6ff;muted=0xff91abd0;gold=0xffefba48;danger=0xffd94558;}}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void build(){palette();getWindow().setStatusBarColor(accent);getWindow().setNavigationBarColor(bg);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);root.setPadding(dp(12),dp(10),dp(12),dp(10));root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout head=row();ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.arnika_logo_vector);head.addView(logo,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout n=new LinearLayout(this);n.setOrientation(LinearLayout.VERTICAL);n.addView(tv("ARNIKA SESSION",20,gold,true));n.addView(tv("Your Voice • Any Language • Your Session",11,muted,false));head.addView(n,new LinearLayout.LayoutParams(0,dp(62),1));Button settings=btn("⚙ تنظیمات",accent);settings.setOnClickListener(v->settings());head.addView(settings,new LinearLayout.LayoutParams(dp(105),dp(45)));root.addView(head);
        ScrollView sv=new ScrollView(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(0,dp(8),0,dp(30));title=edit("عنوان Session",false);folder=edit("پوشه / پروژه",false);folder.setText("Sessions");c.addView(title);c.addView(space(7));c.addView(folder);c.addView(space(8));
        LinearLayout tools=row();langBtn=btn("🌐 "+languageName(),card2);langBtn.setOnClickListener(v->languageDialog());tools.addView(langBtn,new LinearLayout.LayoutParams(0,dp(46),1));Button upload=btn("🎵 فایل صوتی",card2);upload.setOnClickListener(v->pickAudio());tools.addView(upload,new LinearLayout.LayoutParams(0,dp(46),1));c.addView(tools);c.addView(space(8));
        body=edit("شروع کنید؛ متن زنده اینجا نوشته می‌شود…",true);c.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(340)));c.addView(space(8));status=tv("آماده",12,muted,false);c.addView(status);
        LinearLayout main=row();Button start=btn("🎙 شروع سریع",accent);start.setOnClickListener(v->startRequested());Button stop=btn("■ توقف",danger);stop.setOnClickListener(v->stop());Button save=btn("✓ ذخیره",accent2);save.setOnClickListener(v->saveNote());main.addView(start,new LinearLayout.LayoutParams(0,dp(52),1));main.addView(stop,new LinearLayout.LayoutParams(0,dp(52),1));main.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));c.addView(main);c.addView(space(8));
        LinearLayout actions=row();Button tr=btn("🌐 ترجمه",card2);tr.setOnClickListener(v->translate());Button share=btn("↗ Share",card2);share.setOnClickListener(v->share());Button ex=btn("TXT / DOCX",card2);ex.setOnClickListener(v->exportDialog());actions.addView(tr,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(share,new LinearLayout.LayoutParams(0,dp(48),1));actions.addView(ex,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(actions);c.addView(space(8));
        LinearLayout more=row();Button samples=btn("👤 Samples",card2);samples.setOnClickListener(v->samples());Button archive=btn("🗂 آرشیو",card2);archive.setOnClickListener(v->archive());Button speak=btn("🔊 صدا",card2);speak.setOnClickListener(v->speak());more.addView(samples,new LinearLayout.LayoutParams(0,dp(48),1));more.addView(archive,new LinearLayout.LayoutParams(0,dp(48),1));more.addView(speak,new LinearLayout.LayoutParams(0,dp(48),1));c.addView(more);sv.addView(c);root.addView(sv,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);}
    private View space(int hgt){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(hgt)));return v;}

    private boolean mic(){if(Build.VERSION.SDK_INT<23||checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)return true;requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return false;}
    private void startRequested(){if(!mic())return;if("default".equals(p.getString("speaker_mode","all"))&&db.def()!=null){status.setText("بررسی صدای پیش‌فرض…");VoiceEngine.capture(this,null,1500,(fp,f,e)->h.post(()->{if(e!=null){toast(e);return;}double s=VoiceEngine.similarity(fp,db.def().fp);if(s>=.55)startRecognizer(null);else{status.setText("گوینده با Sample پیش‌فرض تطبیق ندارد");toast("Voice sample mismatch");}}));}else startRecognizer(null);}
    private Intent recognizerIntent(Uri audio){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);i.putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS",450L);i.putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS",300L);String l=language();if(!"auto".equals(l)){i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,l);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,l);}else if(Build.VERSION.SDK_INT>=34){i.putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION",true);i.putExtra("android.speech.extra.ENABLE_LANGUAGE_SWITCH",true);}if(audio!=null&&Build.VERSION.SDK_INT>=33)i.putExtra("android.speech.extra.AUDIO_SOURCE",audio);return i;}
    private void startRecognizer(Uri audio){stopRecognizerOnly();if(!SpeechRecognizer.isRecognitionAvailable(this)){toast("Speech Recognition service موجود نیست");return;}try{sr=(Build.VERSION.SDK_INT>=31&&p.getBoolean("on_device",true)&&SpeechRecognizer.isOnDeviceRecognitionAvailable(this))?SpeechRecognizer.createOnDeviceSpeechRecognizer(this):SpeechRecognizer.createSpeechRecognizer(this);}catch(Exception e){sr=SpeechRecognizer.createSpeechRecognizer(this);}sr.setRecognitionListener(this);committed=body.getText().toString().trim();listening=true;status.setText(audio==null?"Listening…":"Transcribing audio…");sr.startListening(recognizerIntent(audio));}
    private void restart(){if(!listening)return;h.postDelayed(()->{if(listening)try{sr.startListening(recognizerIntent(null));}catch(Exception e){stopRecognizerOnly();startRecognizer(null);}},100);}
    private void stopRecognizerOnly(){if(sr!=null){try{sr.cancel();sr.destroy();}catch(Exception ignored){}sr=null;}}
    private void stop(){listening=false;stopRecognizerOnly();status.setText(langDetected.isEmpty()?"متوقف شد":"متوقف شد • "+langDetected);}
    @Override public void onReadyForSpeech(Bundle b){status.setText("🎙 آمادهٔ دریافت صدا");}
    @Override public void onBeginningOfSpeech(){status.setText("● در حال شنیدن…");}
    @Override public void onRmsChanged(float r){}
    @Override public void onBufferReceived(byte[] b){}
    @Override public void onEndOfSpeech(){status.setText("در حال تبدیل…");}
    @Override public void onError(int e){if(!listening)return;if(e==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS){stop();toast("مجوز میکروفون لازم است");return;}status.setText("ادامهٔ شنیدن…");restart();}
    private String best(Bundle b){ArrayList<String>x=b==null?null:b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);return x==null||x.isEmpty()?"":x.get(0);}
    @Override public void onResults(Bundle b){String x=best(b).trim();if(!x.isEmpty()){committed=(committed+" "+x).trim();body.setText(committed);body.setSelection(body.length());}restart();}
    @Override public void onPartialResults(Bundle b){String x=best(b).trim();body.setText((committed+(x.isEmpty()?"":" "+x)).trim());body.setSelection(body.length());}
    @Override public void onEvent(int t,Bundle b){}
    @Override public void onLanguageDetection(Bundle b){if(b!=null){String l=b.getString("android.speech.DETECTED_LANGUAGE");if(l!=null){langDetected=l;status.setText("زبان: "+l);}}}
    @Override public void onRequestPermissionsResult(int r,String[]q,int[]g){super.onRequestPermissionsResult(r,q,g);if(r==REQ_MIC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)startRequested();}

    private String language(){return p.getString("language","auto");}private String languageName(){String x=language();for(String[]l:LANGS)if(l[1].equals(x))return l[0];return"Auto";}
    private void languageDialog(){String[]n=new String[LANGS.length];int k=0;for(int i=0;i<n.length;i++){n[i]=LANGS[i][0];if(LANGS[i][1].equals(language()))k=i;}new AlertDialog.Builder(this).setTitle("زبان گفتار").setSingleChoiceItems(n,k,(d,w)->{p.edit().putString("language",LANGS[w][1]).apply();langBtn.setText("🌐 "+LANGS[w][0]);d.dismiss();}).show();}
    private void saveNote(){noteId=db.save(noteId,title.getText().toString(),folder.getText().toString(),body.getText().toString());toast("Session ذخیره شد");}
    private void archive(){List<DbHelper.Note>ns=db.notes();if(ns.isEmpty()){toast("آرشیو خالی است");return;}String[]a=new String[ns.size()];for(int i=0;i<a.length;i++)a[i]=ns.get(i).title+" • "+new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.US).format(new Date(ns.get(i).updated));new AlertDialog.Builder(this).setTitle("آرشیو").setItems(a,(d,w)->{DbHelper.Note n=ns.get(w);noteId=n.id;title.setText(n.title);folder.setText(n.folder);body.setText(n.text);}).setNegativeButton("بستن",null).show();}
    private void share(){String x=body.getText().toString().trim();if(x.isEmpty())return;Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,title.getText().toString());i.putExtra(Intent.EXTRA_TEXT,x);startActivity(Intent.createChooser(i,"Share ARNIKA SESSION"));}
    private void translate(){String x=body.getText().toString().trim();if(x.isEmpty())return;String[]n=new String[TARGETS.length];for(int i=0;i<n.length;i++)n[i]=TARGETS[i][0];new AlertDialog.Builder(this).setTitle("ترجمه به زبان").setItems(n,(d,w)->{status.setText("در حال ترجمه / دانلود مدل…");TranslationEngine.translate(x,TARGETS[w][1],(out,src,e)->runOnUiThread(()->{if(e!=null){status.setText(e);toast("ترجمه ناموفق");return;}String nt=(title.getText().toString().trim().isEmpty()?"ARNIKA":title.getText().toString().trim())+"_"+TARGETS[w][1];db.save(0,nt,folder.getText().toString()+"/Translations",out);try{StorageHelper.write(this,nt+".txt","text/plain",out.getBytes(StandardCharsets.UTF_8));StorageHelper.write(this,nt+".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocxWriter.create(nt,out));}catch(Exception ignored){}status.setText("ترجمه آماده شد: "+TARGETS[w][0]);new AlertDialog.Builder(this).setTitle(nt).setMessage(out).setPositiveButton("جایگزینی متن",(z,y)->body.setText(out)).setNegativeButton("بستن",null).show();}));}).show();}
    private void exportDialog(){String[]x={"TXT","DOCX","هر دو در فولدر تنظیم‌شده"};new AlertDialog.Builder(this).setTitle("خروجی").setItems(x,(d,w)->{try{String name=title.getText().toString().trim().isEmpty()?"ARNIKA_SESSION":title.getText().toString().trim();String text=body.getText().toString();if(w==2&&StorageHelper.tree(this)!=null){StorageHelper.write(this,name+".txt","text/plain",text.getBytes(StandardCharsets.UTF_8));StorageHelper.write(this,name+".docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocxWriter.create(name,text));toast("هر دو فایل ذخیره شدند");return;}pending=w==0?text.getBytes(StandardCharsets.UTF_8):DocxWriter.create(name,text);pendingMime=w==0?"text/plain":"application/vnd.openxmlformats-officedocument.wordprocessingml.document";Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType(pendingMime);i.putExtra(Intent.EXTRA_TITLE,name+(w==0?".txt":".docx"));startActivityForResult(i,REQ_SAVE);}catch(Exception e){toast(e.getMessage());}}).show();}
    private void pickAudio(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("audio/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,REQ_AUDIO);}
    private void speak(){String x=body.getText().toString().trim();if(x.isEmpty())return;if(tts==null){tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){String l=language();if(!"auto".equals(l))tts.setLanguage(Locale.forLanguageTag(l));tts.speak(x,TextToSpeech.QUEUE_FLUSH,null,"arnika");}});}else tts.speak(x,TextToSpeech.QUEUE_FLUSH,null,"arnika");}

    private void samples(){List<DbHelper.Sample>ss=db.samples();ArrayList<String>a=new ArrayList<>();a.add("＋ ضبط Sample جدید");for(DbHelper.Sample s:ss)a.add((s.def?"★ ":"")+s.name);new AlertDialog.Builder(this).setTitle("Voice Samples").setItems(a.toArray(new String[0]),(d,w)->{if(w==0){recordSample();return;}DbHelper.Sample s=ss.get(w-1);String[]ops={"▶ Play","قرار دادن به‌عنوان پیش‌فرض","حذف"};new AlertDialog.Builder(this).setTitle(s.name).setItems(ops,(q,z)->{if(z==0)play(s.path);else if(z==1){db.setDef(s.id);toast("پیش‌فرض شد");}else{db.delSample(s.id);new File(s.path).delete();toast("حذف شد");}}).show();}).show();}
    private void recordSample(){if(!mic())return;EditText e=new EditText(this);e.setHint("نام Sample");new AlertDialog.Builder(this).setTitle("Sample جدید").setView(e).setPositiveButton("شروع ضبط",(d,w)->{String n=e.getText().toString().trim();if(n.isEmpty())n="Sample "+(db.samples().size()+1);File dir=new File(getFilesDir(),"samples");dir.mkdirs();File f=new File(dir,"s_"+System.currentTimeMillis()+".wav");String nn=n;status.setText("۴ ثانیه صحبت کنید…");VoiceEngine.capture(this,f,4000,(fp,file,err)->runOnUiThread(()->{if(err!=null){toast(err);return;}new AlertDialog.Builder(this).setTitle("تست Sample").setMessage("می‌توانید ابتدا صدای ذخیره‌شده را Play کنید.").setNeutralButton("▶ Play",(x,y)->play(file.getAbsolutePath())).setPositiveButton("ذخیره",(x,y)->{db.addSample(nn,file.getAbsolutePath(),fp);toast("Sample ذخیره شد");status.setText("آماده");}).setNegativeButton("لغو",(x,y)->file.delete()).show();}));}).setNegativeButton("لغو",null).show();}
    private void play(String path){try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(path);player.prepare();player.start();}catch(Exception e){toast("Play ناموفق");}}
    private void settings(){String[]x={"پوسته: "+p.getString("theme","midnight"),"گوینده: "+("default".equals(p.getString("speaker_mode","all"))?"فقط پیش‌فرض":"همه صداها"),"موتور: "+(p.getBoolean("on_device",true)?"On-device اولویت دارد":"سیستم"),"📁 انتخاب فولدر ذخیره‌سازی","زبان: "+languageName()};new AlertDialog.Builder(this).setTitle("تنظیمات ARNIKA").setItems(x,(d,w)->{if(w==0)theme();else if(w==1){String v="default".equals(p.getString("speaker_mode","all"))?"all":"default";p.edit().putString("speaker_mode",v).apply();settings();}else if(w==2){p.edit().putBoolean("on_device",!p.getBoolean("on_device",true)).apply();settings();}else if(w==3){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,REQ_TREE);}else languageDialog();}).show();}
    private void theme(){String[]n={"Midnight Blue","Classic Gold","Light","Neon"};String[]v={"midnight","classic","light","neon"};new AlertDialog.Builder(this).setTitle("پوسته").setItems(n,(d,w)->{p.edit().putString("theme",v[w]).apply();build();}).show();}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;Uri u=d.getData();if(r==REQ_SAVE&&u!=null&&pending!=null){try(OutputStream o=getContentResolver().openOutputStream(u,"w")){if(o!=null)o.write(pending);toast("فایل ذخیره شد");}catch(Exception e){toast(e.getMessage());}pending=null;}else if(r==REQ_TREE&&u!=null){int flags=d.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);try{getContentResolver().takePersistableUriPermission(u,flags);}catch(Exception ignored){}p.edit().putString("storage_tree",u.toString()).apply();toast("فولدر ذخیره شد");}else if(r==REQ_AUDIO&&u!=null){if(Build.VERSION.SDK_INT<33){toast("تبدیل مستقیم فایل صوتی در Android 13+ فعال است");return;}if(!mic())return;startRecognizer(u);}}
}
