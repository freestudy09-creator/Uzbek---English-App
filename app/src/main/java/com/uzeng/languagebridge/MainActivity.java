package com.uzeng.languagebridge;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private final Map<String,String> enUz=new LinkedHashMap<>();
    private final Map<String,String> uzEn=new LinkedHashMap<>();
    private EditText input;
    private TextView output,direction,packStatus,aiPackStatus;
    private boolean enToUz=true;
    private TextToSpeech tts;
    private SharedPreferences prefs;
    private File packFile;
    private File aiPackDir;
    private static final String PACK_URL="https://raw.githubusercontent.com/freestudy09-creator/Uzbek---English-App/main/language-packs/uz-en-v1.tsv";
    private static final String AI_PACK_URL="https://github.com/freestudy09-creator/Uzbek---English-App/releases/download/ai-translation-v1/Uzbek-English-AI-Translation-Pack-v1.zip";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("uzeng",MODE_PRIVATE);
        packFile=new File(getFilesDir(),"uz-en-v1.tsv");
        aiPackDir=new File(getFilesDir(),"ai-translation-v1");
        tts=new TextToSpeech(this,this);
        loadBuiltIn();
        if(packFile.exists()) loadPack(packFile);
        showHome();
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
        pair("what are you doing","nima qilyapsiz"); pair("what do you do","nima ish qilasiz");
        pair("where are you going","qayerga ketyapsiz"); pair("what are you looking for","nima qidiryapsiz");
        pair("what do you want","nima xohlaysiz"); pair("what do you need","sizga nima kerak");
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

    private GradientDrawable rounded(int color,float radius){
        GradientDrawable g=new GradientDrawable();
        g.setColor(color); g.setCornerRadius(radius);
        return g;
    }

    private GradientDrawable gradient(int c1,int c2,float radius){
        GradientDrawable g=new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{c1,c2}
        );
        g.setCornerRadius(radius);
        return g;
    }

    private TextView text(String s,int sp,boolean bold){
        TextView v=new TextView(this);
        v.setText(s); v.setTextSize(sp); v.setTextColor(Color.rgb(24,34,48));
        v.setPadding(14,12,14,12);
        if(bold)v.setTypeface(null,1);
        return v;
    }

    private Button button(String s){
        Button b=new Button(this);
        b.setText(s); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setTypeface(null,1);
        b.setPadding(18,8,18,8);
        b.setBackground(gradient(Color.rgb(24,128,105),Color.rgb(41,98,173),28));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,142);
        lp.setMargins(0,10,0,10);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView card(String title,String subtitle){
        TextView v=text(title+"\n"+subtitle,18,true);
        v.setBackground(rounded(Color.WHITE,28));
        v.setElevation(6);
        v.setPadding(22,18,22,18);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,10,0,10);
        v.setLayoutParams(lp);
        return v;
    }

    private void showHome(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(gradient(Color.rgb(239,248,246),Color.rgb(236,242,252),0));

        ScrollView sv=new ScrollView(this);
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(24,26,24,40);
        sv.addView(c);

        TextView logo=text("UZ  •  EN",18,true);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(gradient(Color.rgb(18,139,113),Color.rgb(37,93,170),50));
        logo.setPadding(18,14,18,14);
        LinearLayout.LayoutParams logoLp=new LinearLayout.LayoutParams(250,80);
        logoLp.gravity=Gravity.CENTER_HORIZONTAL;
        logo.setLayoutParams(logoLp);
        logo.setElevation(8);
        c.addView(logo);

        TextView title=text("Ingliz tilini oson o‘rganing",30,true);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8,28,8,6);
        c.addView(title);

        TextView subtitle=text("O‘zbek tilida • Oflayn • Bosqichma-bosqich",16,false);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setTextColor(Color.rgb(90,102,118));
        c.addView(subtitle);

        TextView welcome=card("Assalomu alaykum 👋","Bugun 10 daqiqa o‘rganishdan boshlang.");
        c.addView(welcome);

        c.addView(text("Darajangizni tanlang",21,true));

        Button beginner=button("🌱  0 DAN BOSHLASH");
        Button medium=button("📘  O‘RTA DARAJA");
        Button advanced=button("🚀  YUQORI DARAJA");
        c.addView(beginner);c.addView(medium);c.addView(advanced);

        c.addView(text("Tezkor imkoniyatlar",21,true));
        Button translator=button("⇄  TARJIMON");
        Button words=button("📚  KUNDALIK SO‘ZLAR");
        Button progress=button("🏆  MENING NATIJAM");
        c.addView(translator);c.addView(words);c.addView(progress);

        TextView daily=card("Bugungi maqsad","5 ta yangi so‘z • 1 ta mini test • 5 daqiqa tinglash");
        c.addView(daily);

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        beginner.setOnClickListener(v->showBeginnerLesson());
        medium.setOnClickListener(v->toast("O‘rta daraja darslari tayyorlanmoqda"));
        advanced.setOnClickListener(v->toast("Yuqori daraja darslari tayyorlanmoqda"));
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

        c.addView(text("AI Translation Pack",19,true));
        aiPackStatus=text(isAiPackReady()
            ?"✓ AI pack installed — neural model files are ready"
            :"Not installed • about 143 MB download",15,false);
        c.addView(aiPackStatus);
        Button aiDownload=button(isAiPackReady()
            ?"REINSTALL AI TRANSLATION PACK"
            :"DOWNLOAD AI TRANSLATION PACK");
        c.addView(aiDownload);
        c.addView(text("This installs the English↔Uzbek neural model files for offline AI translation.",14,false));

        c.addView(text("Common phrases",19,true));
        String[] p={"Where are you? — Qayerdasiz?","Hello — Salom","Thank you — Rahmat","How are you? — Qalaysiz?","Please — Iltimos","Sorry — Kechirasiz","Can you help me? — Menga yordam bera olasizmi?","Where is the airport? — Aeroport qayerda?"};
        for(String s:p)c.addView(text(s,16,false));

        Button saved=button("RECENT & FAVORITES");c.addView(saved);
        c.addView(text("After downloading the pack, these translations work without internet.",14,false));

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);

        home.setOnClickListener(v->showHome());
        swap.setOnClickListener(v->{enToUz=!enToUz;direction.setText(enToUz?"English → Uzbek":"Uzbek → English");input.setText("");output.setText("Translation will appear here");});
        translate.setOnClickListener(v->translate());
        speak.setOnClickListener(v->speak());
        copy.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("translation",output.getText()));toast("Copied");});
        save.setOnClickListener(v->saveFavorite());
        saved.setOnClickListener(v->showSaved());
        download.setOnClickListener(v->downloadPack(download));
        aiDownload.setOnClickListener(v->downloadAiPack(aiDownload));
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

    private boolean isAiPackReady(){
        File enModel=new File(aiPackDir,"ai-pack/en-uz/model.bin");
        File uzModel=new File(aiPackDir,"ai-pack/uz-en/model.bin");
        File enConfig=new File(aiPackDir,"ai-pack/en-uz/config.json");
        File uzConfig=new File(aiPackDir,"ai-pack/uz-en/config.json");
        return enModel.isFile() && uzModel.isFile() && enConfig.isFile() && uzConfig.isFile();
    }

    private void deleteRecursive(File f){
        if(f==null || !f.exists()) return;
        if(f.isDirectory()){
            File[] children=f.listFiles();
            if(children!=null) for(File child:children) deleteRecursive(child);
        }
        f.delete();
    }

    private void unzipSafely(File zipFile, File destination) throws IOException {
        String root=destination.getCanonicalPath()+File.separator;
        try(ZipInputStream zin=new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))){
            ZipEntry entry;
            byte[] buffer=new byte[16384];
            while((entry=zin.getNextEntry())!=null){
                File out=new File(destination,entry.getName());
                String outPath=out.getCanonicalPath();
                if(!outPath.startsWith(root)) throw new IOException("Unsafe ZIP entry");
                if(entry.isDirectory()){
                    if(!out.exists() && !out.mkdirs()) throw new IOException("Could not create folder");
                }else{
                    File parent=out.getParentFile();
                    if(parent!=null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create folder");
                    try(FileOutputStream fos=new FileOutputStream(out)){
                        int n;
                        while((n=zin.read(buffer))>0) fos.write(buffer,0,n);
                    }
                }
                zin.closeEntry();
            }
        }
    }

    private void downloadAiPack(Button button){
        button.setEnabled(false);
        button.setText("AI PACK DOWNLOADING...");
        aiPackStatus.setText("Downloading about 143 MB… Keep the app open.");

        new Thread(()->{
            File zipFile=new File(getCacheDir(),"ai-translation-v1.zip");
            File tempDir=new File(getFilesDir(),"ai-translation-v1.tmp");
            try{
                deleteRecursive(tempDir);
                if(!tempDir.mkdirs()) throw new IOException("Could not prepare model folder");

                HttpURLConnection con=(HttpURLConnection)new URL(AI_PACK_URL).openConnection();
                con.setInstanceFollowRedirects(true);
                con.setConnectTimeout(20000);
                con.setReadTimeout(60000);
                con.setRequestProperty("User-Agent","UzbekEnglishApp/3.0");
                int code=con.getResponseCode();
                if(code!=200) throw new IOException("HTTP "+code);

                long total=con.getContentLengthLong();
                long received=0;
                try(InputStream in=new BufferedInputStream(con.getInputStream());
                    FileOutputStream out=new FileOutputStream(zipFile)){
                    byte[] buf=new byte[32768];
                    int len;
                    int lastPercent=-1;
                    while((len=in.read(buf))>0){
                        out.write(buf,0,len);
                        received+=len;
                        if(total>0){
                            int percent=(int)(received*100/total);
                            if(percent>=lastPercent+5){
                                lastPercent=percent;
                                final int p=percent;
                                runOnUiThread(()->aiPackStatus.setText("Downloading AI pack… "+p+"%"));
                            }
                        }
                    }
                }

                runOnUiThread(()->{
                    aiPackStatus.setText("Download complete. Installing model…");
                    button.setText("INSTALLING AI PACK...");
                });

                unzipSafely(zipFile,tempDir);
                if(!new File(tempDir,"ai-pack/en-uz/model.bin").isFile()
                    || !new File(tempDir,"ai-pack/uz-en/model.bin").isFile()){
                    throw new IOException("Model files missing after extraction");
                }

                deleteRecursive(aiPackDir);
                if(!tempDir.renameTo(aiPackDir)) throw new IOException("Could not activate AI pack");
                prefs.edit().putBoolean("ai_pack_installed",true).apply();

                runOnUiThread(()->{
                    aiPackStatus.setText("✓ AI pack installed — neural model files are ready");
                    button.setText("REINSTALL AI TRANSLATION PACK");
                    button.setEnabled(true);
                    toast("AI Translation Pack installed");
                });
            }catch(Exception e){
                deleteRecursive(tempDir);
                runOnUiThread(()->{
                    aiPackStatus.setText("AI pack installation failed. Check connection/storage and try again.");
                    button.setText(isAiPackReady()?"REINSTALL AI TRANSLATION PACK":"DOWNLOAD AI TRANSLATION PACK");
                    button.setEnabled(true);
                    toast("AI pack install failed");
                });
            }finally{
                zipFile.delete();
            }
        }).start();
    }

    private void translate(){
        String raw=input.getText().toString().trim();
        if(raw.isEmpty()){toast("Avval so‘z yoki gap yozing");return;}
        String key=raw.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
        Map<String,String> map=enToUz?enUz:uzEn;
        String ans=map.get(key);

        if(ans==null){
            if(!key.contains(" ")){
                ans=enToUz
                    ?"Bu so‘z hozirgi oflayn lug‘atda yo‘q."
                    :"This word is not in the current offline dictionary.";
            }else{
                ans=enToUz
                    ?"Bu gap hozirgi oflayn paketda yo‘q. Noto‘g‘ri so‘zma-so‘z tarjima ko‘rsatilmaydi."
                    :"This sentence is not in the current offline pack. The app will not show an unsafe word-by-word translation.";
            }
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
