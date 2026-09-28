package com.uzeng.languagebridge;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private final Map<String,String> enUz=new LinkedHashMap<>();
    private final Map<String,String> uzEn=new LinkedHashMap<>();
    private EditText input;
    private TextView output,direction,packStatus;
    private boolean enToUz=true;
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private File packFile;
    private static final String PACK_URL="https://raw.githubusercontent.com/freestudy09-creator/Uzbek---English-App/main/language-packs/uz-en-v1.tsv";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("uzeng",MODE_PRIVATE);
        packFile=new File(getFilesDir(),"uz-en-v1.tsv");
        tts=new TextToSpeech(this,this);
        loadBuiltIn();
        if(packFile.exists()) loadPack(packFile);
        showTranslator();
    }

    private void pair(String e,String u){
        if(e==null||u==null)return;
        e=e.trim().toLowerCase(Locale.ROOT); u=u.trim().toLowerCase(Locale.ROOT);
        if(!e.isEmpty()&&!u.isEmpty()){enUz.put(e,u);uzEn.put(u,e);}
    }

    private void loadBuiltIn(){
        pair("hello","salom"); pair("thank you","rahmat"); pair("please","iltimos");
        pair("sorry","kechirasiz"); pair("yes","ha"); pair("no","yo'q");
        pair("how are you","qalaysiz"); pair("where are you","qayerdasiz");
        pair("where are you from","qayerdansiz"); pair("i am fine","men yaxshiman");
        pair("what is your name","ismingiz nima"); pair("can you help me","menga yordam bera olasizmi");
        pair("where is the airport","aeroport qayerda"); pair("where is the hotel","mehmonxona qayerda");
        pair("i don't understand","men tushunmayapman"); pair("speak slowly","sekin gapiring");
        pair("water","suv"); pair("food","ovqat"); pair("book","kitob"); pair("school","maktab");
        pair("university","universitet"); pair("teacher","o'qituvchi"); pair("student","talaba");
        pair("today","bugun"); pair("tomorrow","ertaga"); pair("yesterday","kecha");
        pair("one","bir"); pair("two","ikki"); pair("three","uch"); pair("four","to'rt"); pair("five","besh");
    }

    private int loadPack(File file){
        int n=0;
        try(BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"))){
            String line;
            while((line=br.readLine())!=null){
                if(line.trim().isEmpty()||line.startsWith("#"))continue;
                String[] p=line.split("\t",2);
                if(p.length==2){pair(p[0],p[1]);n++;}
            }
        }catch(Exception ignored){}
        return n;
    }

    private TextView text(String s,int sp,boolean bold){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(sp);
        v.setTextColor(Color.rgb(25,35,35)); v.setPadding(12,12,12,12);
        if(bold)v.setTypeface(null,1); return v;
    }
    private Button button(String s){Button b=new Button(this);b.setText(s);return b;}


    private void showHome(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        TextView head=text("INGLIZ TILINI OSON O‘RGANING",24,true);
        head.setTextColor(Color.WHITE);head.setGravity(Gravity.CENTER);
        head.setBackgroundColor(Color.rgb(20,110,90));head.setPadding(18,32,18,32);
        root.addView(head);

        ScrollView sv=new ScrollView(this);
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(22,22,22,30);
        sv.addView(c);

        c.addView(text("Assalomu alaykum!",22,true));
        c.addView(text("Ingliz tilini 0 dan boshlab, o‘zbek tilida, bosqichma-bosqich o‘rganing.",17,false));

        Button start=button("▶ 0 DAN BOSHLASH");
        Button translator=button("⇄ TARJIMON");
        Button words=button("📚 SO‘ZLAR");
        Button progress=button("✓ MENING NATIJAM");
        c.addView(start);c.addView(translator);c.addView(words);c.addView(progress);

        c.addView(text("Boshlang‘ich darslar",20,true));
        c.addView(text("1. Salomlashish\n2. Tanishish\n3. Raqamlar\n4. Oila\n5. Maktab va universitet\n6. Kundalik suhbat\n7. Do‘kon va bozor\n8. Safar va transport",16,false));

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);

        start.setOnClickListener(v->showBeginnerLesson());
        translator.setOnClickListener(v->showTranslator());
        words.setOnClickListener(v->showWordLesson());
        progress.setOnClickListener(v->showProgress());
    }

    private void showBeginnerLesson(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,28);
        Button back=button("← BOSH SAHIFA");r.addView(back);
        r.addView(text("1-DARS: Salomlashish",24,true));
        r.addView(text("Har bir so‘zni o‘qing, ma’nosini ko‘ring va ovoz chiqarib takrorlang.",16,false));

        String[][] items={
            {"Hello","Salom"},
            {"Hi","Salom"},
            {"Good morning","Xayrli tong"},
            {"Good evening","Xayrli kech"},
            {"Goodbye","Xayr"},
            {"Thank you","Rahmat"},
            {"Please","Iltimos"},
            {"Sorry","Kechirasiz"},
            {"How are you?","Qalaysiz?"},
            {"I am fine.","Men yaxshiman."}
        };
        for(String[] x:items){
            TextView card=text(x[0]+"\n"+x[1],20,true);
            card.setBackgroundColor(Color.rgb(245,245,245));card.setPadding(18,18,18,18);
            r.addView(card);
            Button hear=button("🔊 ESHITISH: "+x[0]);
            hear.setOnClickListener(v->{tts.setLanguage(Locale.US);tts.speak(x[0],TextToSpeech.QUEUE_FLUSH,null,x[0]);});
            r.addView(hear);
        }

        r.addView(text("Kichik test",20,true));
        r.addView(text("“Thank you” nimani anglatadi?",18,false));
        Button a=button("A) Salom"), b=button("B) Rahmat"), d=button("C) Xayr");
        r.addView(a);r.addView(b);r.addView(d);
        a.setOnClickListener(v->toast("Yana urinib ko‘ring"));
        b.setOnClickListener(v->{toast("To‘g‘ri!");prefs.edit().putInt("lesson_progress",1).apply();});
        d.setOnClickListener(v->toast("Yana urinib ko‘ring"));
        back.setOnClickListener(v->showHome());

        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void showWordLesson(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,28);
        Button back=button("← BOSH SAHIFA");r.addView(back);
        r.addView(text("Kundalik so‘zlar",24,true));
        String[] words={"water — suv","food — ovqat","book — kitob","teacher — o‘qituvchi","student — talaba","house — uy","friend — do‘st","today — bugun","tomorrow — ertaga","money — pul"};
        for(String w:words)r.addView(text(w,19,false));
        back.setOnClickListener(v->showHome());
        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void showProgress(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,28);
        Button back=button("← BOSH SAHIFA");r.addView(back);
        int p=prefs.getInt("lesson_progress",0);
        r.addView(text("Mening natijam",24,true));
        r.addView(text(p>0?"✓ 1-dars testi bajarildi. Davom eting!":"Hali dars yakunlanmagan. “0 dan boshlash” orqali boshlang.",18,false));
        back.setOnClickListener(v->showHome());
        setContentView(r);
    }

    private void showTranslator(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        TextView head=text("UZBEK ↔ ENGLISH",24,true);head.setTextColor(Color.WHITE);
        head.setBackgroundColor(Color.rgb(20,110,90));head.setGravity(Gravity.CENTER);
        head.setPadding(15,28,15,28);root.addView(head);

        ScrollView sv=new ScrollView(this);
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(20,18,20,30);
        sv.addView(c);

        Button home=button("← BOSH SAHIFA");c.addView(home);
        direction=text("English → Uzbek",20,true);c.addView(direction);
        input=new EditText(this);input.setHint("Type a word or phrase");input.setMinLines(3);
        input.setGravity(Gravity.TOP);c.addView(input,new LinearLayout.LayoutParams(-1,180));

        LinearLayout row=new LinearLayout(this);
        Button swap=button("⇄ SWAP"),translate=button("TRANSLATE");
        row.addView(swap,new LinearLayout.LayoutParams(0,-2,1));row.addView(translate,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);

        output=text("Translation will appear here",22,true);output.setBackgroundColor(Color.rgb(245,245,245));
        output.setMinHeight(150);c.addView(output);

        LinearLayout tools=new LinearLayout(this);
        Button speak=button("🔊 SPEAK"),copy=button("COPY"),save=button("★ SAVE");
        tools.addView(speak,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        tools.addView(save,new LinearLayout.LayoutParams(0,-2,1));c.addView(tools);

        c.addView(text("Offline language pack",19,true));
        packStatus=text(packFile.exists()?"✓ Uzbek–English pack downloaded ("+enUz.size()+" entries)":"Not downloaded yet",15,false);
        c.addView(packStatus);
        Button download=button(packFile.exists()?"UPDATE OFFLINE PACK":"DOWNLOAD UZBEK–ENGLISH PACK");
        c.addView(download);

        c.addView(text("Common phrases",19,true));
        String[] p={"Where are you? — Qayerdasiz?","Hello — Salom","Thank you — Rahmat","How are you? — Qalaysiz?","Please — Iltimos","Sorry — Kechirasiz","Can you help me? — Menga yordam bera olasizmi?","Where is the airport? — Aeroport qayerda?"};
        for(String s:p)c.addView(text(s,16,false));

        Button saved=button("RECENT & FAVORITES");c.addView(saved);
        c.addView(text("After downloading the pack, these translations work without internet.",14,false));

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);

        home.setOnClickListener(v->showHome());\n        swap.setOnClickListener(v->{enToUz=!enToUz;direction.setText(enToUz?"English → Uzbek":"Uzbek → English");input.setText("");output.setText("Translation will appear here");});
        translate.setOnClickListener(v->translate());
        speak.setOnClickListener(v->speak());
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("translation",output.getText()));toast("Copied");});
        save.setOnClickListener(v->saveFavorite());
        saved.setOnClickListener(v->showSaved());
        download.setOnClickListener(v->downloadPack(download));
    }

    private void downloadPack(Button button){
        button.setEnabled(false);button.setText("DOWNLOADING...");
        packStatus.setText("Downloading language pack...");
        new Thread(()->{
            File tmp=new File(getFilesDir(),"uz-en-v1.tmp");
            try{
                HttpURLConnection con=(HttpURLConnection)new URL(PACK_URL).openConnection();
                con.setConnectTimeout(15000);con.setReadTimeout(20000);con.setRequestProperty("User-Agent","UzbekEnglishApp/2.0");
                int code=con.getResponseCode();
                if(code!=200)throw new IOException("HTTP "+code);
                try(InputStream in=con.getInputStream();FileOutputStream out=new FileOutputStream(tmp)){
                    byte[] buf=new byte[8192];int len;while((len=in.read(buf))>0)out.write(buf,0,len);
                }
                if(packFile.exists())packFile.delete();
                if(!tmp.renameTo(packFile))throw new IOException("Could not save pack");
                int count=loadPack(packFile);
                prefs.edit().putBoolean("pack_downloaded",true).apply();
                runOnUiThread(()->{
                    packStatus.setText("✓ Offline pack ready ("+count+" downloaded entries)");
                    button.setText("UPDATE OFFLINE PACK");button.setEnabled(true);toast("Language pack downloaded");
                });
            }catch(Exception e){
                tmp.delete();
                runOnUiThread(()->{
                    packStatus.setText("Download failed. Check internet and try again.");
                    button.setText(packFile.exists()?"UPDATE OFFLINE PACK":"DOWNLOAD UZBEK–ENGLISH PACK");
                    button.setEnabled(true);toast("Download failed");
                });
            }
        }).start();
    }

    private void translate(){
        String raw=input.getText().toString().trim();
        if(raw.isEmpty()){toast("Type something first");return;}
        String key=raw.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
        Map<String,String> map=enToUz?enUz:uzEn;
        String ans=map.get(key);
        if(ans==null){
            StringBuilder sb=new StringBuilder();int found=0,total=0;
            for(String w:key.split("\\s+")){
                total++;String clean=w.replaceAll("^[^\\p{L}']+|[^\\p{L}']+$","");
                String t=map.get(clean);
                if(t!=null){sb.append(t);found++;}else sb.append(w);
                sb.append(" ");
            }
            if(found>0 && found==total) ans=sb.toString().trim();
            else if(found>0) ans=sb.toString().trim()+"\n\n(Some words are not in the downloaded pack.)";
            else ans=packFile.exists()?"Not available in this offline pack yet.":"Download the Uzbek–English offline pack first.";
        }
        output.setText(ans);
        Set<String> r=new LinkedHashSet<>(prefs.getStringSet("recent",Collections.emptySet()));
        r.add(raw+" → "+ans);prefs.edit().putStringSet("recent",r).apply();
    }

    private void saveFavorite(){
        if(input.getText().toString().trim().isEmpty()){toast("Translate something first");return;}
        Set<String> f=new LinkedHashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));
        f.add(input.getText().toString().trim()+" → "+output.getText());prefs.edit().putStringSet("favorites",f).apply();toast("Saved");
    }

    private void showSaved(){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,20);
        Button back=button("← BACK");r.addView(back);r.addView(text("Favorites",20,true));
        Set<String> f=prefs.getStringSet("favorites",Collections.emptySet());
        if(f.isEmpty())r.addView(text("No favorites yet.",16,false));else for(String s:f)r.addView(text("★ "+s,16,false));
        r.addView(text("Recent",20,true));
        Set<String> q=prefs.getStringSet("recent",Collections.emptySet());
        if(q.isEmpty())r.addView(text("No history yet.",16,false));else for(String s:q)r.addView(text("• "+s,16,false));
        Button clear=button("CLEAR HISTORY & FAVORITES");r.addView(clear);
        back.setOnClickListener(v->showTranslator());clear.setOnClickListener(v->{prefs.edit().remove("recent").remove("favorites").apply();showSaved();});
        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void speak(){
        String s=output.getText().toString();
        if(s.startsWith("Translation")){toast("Translate something first");return;}
        tts.setLanguage(enToUz?new Locale("uz","UZ"):Locale.US);tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"translation");
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onInit(int status){}
    @Override protected void onDestroy(){if(tts!=null)tts.shutdown();super.onDestroy();}
}
