package com.arnika.session;

import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import com.google.mlkit.nl.translate.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public final class TranslationEngine{
    public interface Callback{void done(String text,String source,String error);}
    private TranslationEngine(){}
    public static void translate(String text,String target,Callback cb){
        if(text==null||text.trim().isEmpty()){cb.done("","und",null);return;}
        LanguageIdentifier id=LanguageIdentification.getClient();
        id.identifyLanguage(text).addOnSuccessListener(code->{String s=norm(code),t=norm(target);String sm=TranslateLanguage.fromLanguageTag(s),tm=TranslateLanguage.fromLanguageTag(t);if(sm==null||tm==null){id.close();cb.done(null,s,"Language not supported by on-device translator");return;}if(sm.equals(tm)){id.close();cb.done(text,s,null);return;}Translator tr=Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(sm).setTargetLanguage(tm).build());tr.downloadModelIfNeeded(new DownloadConditions.Builder().build()).addOnSuccessListener(v->tr.translate(text).addOnSuccessListener(out->{tr.close();id.close();cb.done(out,s,null);}).addOnFailureListener(e->{tr.close();id.close();cb.done(null,s,e.getMessage());})).addOnFailureListener(e->{tr.close();id.close();cb.done(null,s,"Model download: "+e.getMessage());});}).addOnFailureListener(e->{id.close();cb.done(null,"und",e.getMessage());});
    }
    private static String norm(String c){if(c==null)return"und";c=c.toLowerCase();int p=c.indexOf('-');return p>0?c.substring(0,p):c;}
}
