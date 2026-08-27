package com.arnika.session;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity{
    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(4,12,32));getWindow().setNavigationBarColor(Color.rgb(4,12,32));LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setGravity(Gravity.CENTER);r.setBackgroundColor(Color.rgb(4,12,32));ImageView l=new ImageView(this);l.setImageResource(R.drawable.arnika_logo_vector);r.addView(l,new LinearLayout.LayoutParams(dp(210),dp(210)));TextView t=new TextView(this);t.setText("ARNIKA SESSION");t.setTextColor(Color.rgb(239,185,71));t.setTextSize(29);t.setGravity(Gravity.CENTER);t.setLetterSpacing(.08f);r.addView(t);TextView s=new TextView(this);s.setText("Your Voice • Any Language • Your Session");s.setTextColor(Color.rgb(145,184,225));s.setGravity(Gravity.CENTER);s.setPadding(0,10,0,0);r.addView(s);setContentView(r);l.setAlpha(0f);l.setScaleX(.55f);l.setScaleY(.55f);l.setRotation(-22f);t.setAlpha(0f);s.setAlpha(0f);l.animate().alpha(1).scaleX(1).scaleY(1).rotation(0).setDuration(850).setInterpolator(new AccelerateDecelerateInterpolator()).withEndAction(()->{t.animate().alpha(1).setDuration(350).start();s.animate().alpha(1).setDuration(550).withEndAction(()->r.postDelayed(()->{startActivity(new Intent(this,MainActivity.class));overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);finish();},550)).start();}).start();}
    private int dp(int x){return(int)(x*getResources().getDisplayMetrics().density+.5f);}
}
