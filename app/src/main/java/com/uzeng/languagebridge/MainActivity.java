package com.uzeng.languagebridge;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private final Map<String,String> enUz = new LinkedHashMap<>();
    private final Map<String,String> uzEn = new LinkedHashMap<>();
    private EditText input;
    private TextView output, direction;
    private boolean enToUz = true;
    private TextToSpeech tts;
    private android.content.SharedPreferences prefs;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("uzeng",MODE_PRIVATE);
        tts=new TextToSpeech(this,this);
        loadDictionary();
        showTranslator();
    }

    private void pair(String e,String u){
        enUz.put(e.toLowerCase(Locale.ROOT),u);
        uzEn.put(u.toLowerCase(Locale.ROOT),e);
    }

    private void loadDictionary(){
        pair("hello","salom"); pair("hi","salom"); pair("thank you","rahmat");
        pair("please","iltimos"); pair("sorry","kechirasiz"); pair("yes","ha"); pair("no","yo'q");
        pair("good morning","xayrli tong"); pair("good evening","xayrli kech"); pair("goodbye","xayr");
        pair("how are you","qalaysiz"); pair("i am fine","men yaxshiman");
        pair("what is your name","ismingiz nima"); pair("my name is","mening ismim");
        pair("where are you from","qayerdansiz"); pair("i am from","men");
        pair("water","suv"); pair("food","ovqat"); pair("tea","choy"); pair("coffee","qahva");
        pair("bread","non"); pair("milk","sut"); pair("market","bozor"); pair("shop","do'kon");
        pair("school","maktab"); pair("university","universitet"); pair("teacher","o'qituvchi");
        pair("student","talaba"); pair("book","kitob"); pair("pen","ruchka"); pair("house","uy");
        pair("room","xona"); pair("door","eshik"); pair("window","deraza"); pair("road","yo'l");
        pair("car","mashina"); pair("bus","avtobus"); pair("train","poyezd"); pair("airport","aeroport");
        pair("hospital","kasalxona"); pair("doctor","shifokor"); pair("friend","do'st");
        pair("family","oila"); pair("mother","ona"); pair("father","ota"); pair("brother","aka");
        pair("sister","opa"); pair("today","bugun"); pair("tomorrow","ertaga"); pair("yesterday","kecha");
        pair("now","hozir"); pair("morning","ertalab"); pair("evening","kechqurun"); pair("night","tun");
        pair("one","bir"); pair("two","ikki"); pair("three","uch"); pair("four","to'rt"); pair("five","besh");
        pair("six","olti"); pair("seven","yetti"); pair("eight","sakkiz"); pair("nine","to'qqiz"); pair("ten","o'n");
        pair("how much is this","bu qancha turadi"); pair("can you help me","menga yordam bera olasizmi");
        pair("where is the airport","aeroport qayerda"); pair("where is the hotel","mehmonxona qayerda");
        pair("i don't understand","men tushunmayapman"); pair("speak slowly","sekin gapiring");
        pair("english","ingliz tili"); pair("uzbek","o'zbek tili"); pair("language","til");
        pair("love","sevgi"); pair("work","ish"); pair("money","pul"); pair("time","vaqt");
        pair("day","kun"); pair("week","hafta"); pair("month","oy"); pair("year","yil");
        pair("beautiful","chiroyli"); pair("good","yaxshi"); pair("bad","yomon"); pair("big","katta");
        pair("small","kichik"); pair("fast","tez"); pair("slow","sekin"); pair("hot","issiq"); pair("cold","sovuq");
    }

    private TextView text(String s,int sp,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp);
        v.setTextColor(Color.rgb(25,35,35)); v.setPadding(12,12,12,12);
        if(bold) v.setTypeface(null,1); return v;
    }
    private Button button(String s){ Button b=new Button(this); b.setText(s); return b; }

    private void showTranslator(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView head=text("UZBEK ↔ ENGLISH",24,true); head.setTextColor(Color.WHITE);
        head.setBackgroundColor(Color.rgb(20,110,90)); head.setGravity(Gravity.CENTER);
        head.setPadding(15,28,15,28); root.addView(head);

        ScrollView sv=new ScrollView(this);
        LinearLayout c=new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(20,18,20,30);
        sv.addView(c);

        direction=text("English → Uzbek",20,true); c.addView(direction);
        input=new EditText(this); input.setHint("Type a word or common phrase"); input.setMinLines(3);
        input.setGravity(Gravity.TOP); c.addView(input,new LinearLayout.LayoutParams(-1,180));

        LinearLayout buttons=new LinearLayout(this);
        Button swap=button("⇄ Swap"); Button translate=button("Translate");
        buttons.addView(swap,new LinearLayout.LayoutParams(0,-2,1));
        buttons.addView(translate,new LinearLayout.LayoutParams(0,-2,1)); c.addView(buttons);

        output=text("Translation will appear here",22,true); output.setBackgroundColor(Color.rgb(245,245,245));
        output.setMinHeight(150); c.addView(output);

        LinearLayout tools=new LinearLayout(this);
        Button speak=button("🔊 Speak"), copy=button("Copy"), save=button("★ Save");
        tools.addView(speak,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(save,new LinearLayout.LayoutParams(0,-2,1)); c.addView(tools);

        c.addView(text("Common phrases",19,true));
        String[] p={"Hello — Salom","Thank you — Rahmat","How are you? — Qalaysiz?","Please — Iltimos","Sorry — Kechirasiz","How much is this? — Bu qancha turadi?","Can you help me? — Menga yordam bera olasizmi?","Where is the airport? — Aeroport qayerda?"};
        for(String s:p)c.addView(text(s,16,false));

        Button saved=button("Recent & Favorites"); c.addView(saved);
        c.addView(text("Offline dictionary • No account • No ads",14,false));

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);

        swap.setOnClickListener(v->{enToUz=!enToUz; direction.setText(enToUz?"English → Uzbek":"Uzbek → English"); input.setText(""); output.setText("Translation will appear here");});
        translate.setOnClickListener(v->translate());
        speak.setOnClickListener(v->speak());
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("translation",output.getText())); toast("Copied");});
        save.setOnClickListener(v->saveFavorite());
        saved.setOnClickListener(v->showSaved());
    }

    private void translate(){
        String raw=input.getText().toString().trim();
        if(raw.isEmpty()){toast("Type something first");return;}
        String key=raw.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
        Map<String,String> map=enToUz?enUz:uzEn; String ans=map.get(key);
        if(ans==null){
            StringBuilder sb=new StringBuilder(); boolean any=false;
            for(String w:key.split("\\s+")){String t=map.get(w); if(t!=null){sb.append(t).append(" "); any=true;} else sb.append("[").append(w).append("] ");}
            ans=any?sb.toString().trim():"Not in the offline dictionary yet.";
        }
        output.setText(ans);
        Set<String> r=new LinkedHashSet<>(prefs.getStringSet("recent",Collections.emptySet()));
        r.add(raw+" → "+ans); prefs.edit().putStringSet("recent",r).apply();
    }

    private void saveFavorite(){
        if(input.getText().toString().trim().isEmpty()){toast("Translate something first");return;}
        Set<String> f=new LinkedHashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));
        f.add(input.getText().toString().trim()+" → "+output.getText()); prefs.edit().putStringSet("favorites",f).apply(); toast("Saved");
    }

    private void showSaved(){
        LinearLayout r=new LinearLayout(this); r.setOrientation(LinearLayout.VERTICAL); r.setPadding(20,20,20,20);
        Button back=button("← Back"); r.addView(back); r.addView(text("Favorites",20,true));
        Set<String> f=prefs.getStringSet("favorites",Collections.emptySet()); if(f.isEmpty())r.addView(text("No favorites yet.",16,false)); else for(String s:f)r.addView(text("★ "+s,16,false));
        r.addView(text("Recent",20,true)); Set<String> q=prefs.getStringSet("recent",Collections.emptySet()); if(q.isEmpty())r.addView(text("No history yet.",16,false)); else for(String s:q)r.addView(text("• "+s,16,false));
        Button clear=button("Clear local data"); r.addView(clear); back.setOnClickListener(v->showTranslator()); clear.setOnClickListener(v->{prefs.edit().clear().apply();showSaved();});
        ScrollView sv=new ScrollView(this); sv.addView(r); setContentView(sv);
    }

    private void speak(){
        String s=output.getText().toString(); if(s.startsWith("Translation")){toast("Translate something first");return;}
        tts.setLanguage(enToUz?new Locale("uz","UZ"):Locale.US); tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"translation");
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onInit(int status){}
    @Override protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
}
