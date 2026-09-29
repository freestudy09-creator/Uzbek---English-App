package com.uzeng.languagebridge;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.*;
import android.text.*;
import android.speech.tts.TextToSpeech;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    static {
        System.loadLibrary("tilmate_translation");
    }

    private static native String nativeTranslate(String modelPath,String spmPath,String text);
    private final Map<String,String> enUz=new LinkedHashMap<>();
    private final Map<String,String> uzEn=new LinkedHashMap<>();
    private EditText input;
    private TextView output,direction,packStatus,aiPackStatus;
    private boolean enToUz=true;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private boolean listening=false;
    private boolean continuousVoice=false;
    private String confirmedSpeech="";
    private String confirmedVoiceTranslation="";
    private int voiceSessionGeneration=0;
    private boolean voiceAiWarningShown=false;
    private final Handler voiceHandler=new Handler(Looper.getMainLooper());
    private final ExecutorService translationExecutor=Executors.newSingleThreadExecutor();
    private int translationGeneration=0;
    private static final int REQ_RECORD_AUDIO=42;
    private SharedPreferences prefs;
    private File packFile;
    private File aiPackDir;
    private float voiceSpeed=0.86f;
    private String currentScreen="home";
    private static final int TEAL=Color.rgb(27,154,132);
    private static final int BLUE=Color.rgb(53,120,212);
    private static final int YELLOW=Color.rgb(255,209,102);
    private static final int BG=Color.rgb(244,250,249);
    private static final String PACK_URL="https://raw.githubusercontent.com/freestudy09-creator/Uzbek---English-App/main/language-packs/uz-en-v1.tsv";
    private static final String AI_PACK_URL="https://github.com/freestudy09-creator/Uzbek---English-App/releases/download/ai-translation-v1/Uzbek-English-AI-Translation-Pack-v1.zip";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("uzeng",MODE_PRIVATE);
        packFile=new File(getFilesDir(),"uz-en-v1.tsv");
        aiPackDir=new File(getFilesDir(),"ai-translation-v1");
        voiceSpeed=prefs.getFloat("voice_speed",0.86f);
        tts=new TextToSpeech(this,this);
        if(SpeechRecognizer.isRecognitionAvailable(this)){
            speechRecognizer=SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener(){
                @Override public void onReadyForSpeech(Bundle params){listening=true;}
                @Override public void onBeginningOfSpeech(){}
                @Override public void onRmsChanged(float rmsdB){}
                @Override public void onBufferReceived(byte[] buffer){}
                @Override public void onEndOfSpeech(){listening=false;}
                @Override public void onError(int error){
                    listening=false;
                    if(continuousVoice && error!=SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                        && error!=SpeechRecognizer.ERROR_CLIENT){
                        voiceHandler.postDelayed(()->beginListeningSession(),450);
                    }
                }
                @Override public void onResults(Bundle results){
                    listening=false;
                    applySpeechResults(results,true);
                    if(continuousVoice) voiceHandler.postDelayed(()->beginListeningSession(),300);
                }
                @Override public void onPartialResults(Bundle partialResults){
                    applySpeechResults(partialResults,false);
                }
                @Override public void onEvent(int eventType,Bundle params){}
            });
        }
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
        b.setBackground(gradient(TEAL,BLUE,32));
        b.setElevation(4);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,132);
        lp.setMargins(0,9,0,9);
        b.setLayoutParams(lp);
        return b;
    }

    private TextView pill(String s,int bg){
        TextView v=text(s,14,true);
        v.setTextColor(Color.rgb(25,36,48));
        v.setGravity(Gravity.CENTER);
        v.setBackground(rounded(bg,40));
        v.setPadding(18,10,18,10);
        return v;
    }

    private LinearLayout sectionCard(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(22,18,22,18);
        box.setBackground(rounded(Color.WHITE,30));
        box.setElevation(5);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,10,0,10);
        box.setLayoutParams(lp);
        return box;
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
        if(continuousVoice) stopVoiceTranslation();
        currentScreen="home";
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(gradient(Color.rgb(240,250,247),Color.rgb(238,244,255),0));

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(18,16,18,12);

        Button menu=button("☰");
        menu.setTextSize(22);
        LinearLayout.LayoutParams menuLp=new LinearLayout.LayoutParams(100,100);
        menu.setLayoutParams(menuLp);
        top.addView(menu);

        TextView brand=text("TilMate",25,true);
        brand.setTextColor(Color.WHITE);
        brand.setGravity(Gravity.CENTER);
        brand.setBackground(gradient(TEAL,BLUE,30));
        LinearLayout.LayoutParams brandLp=new LinearLayout.LayoutParams(0,100,1);
        brandLp.setMargins(12,0,0,0);
        top.addView(brand,brandLp);
        root.addView(top);

        ScrollView sv=new ScrollView(this);
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(22,6,22,120);
        sv.addView(c);

        LinearLayout hero=sectionCard();
        TextView mascot=text("💬",46,true);
        mascot.setGravity(Gravity.CENTER);
        hero.addView(mascot);
        TextView title=text("Ingliz tilini oson o‘rganing\nLearn English easily",28,true);
        title.setGravity(Gravity.CENTER);
        hero.addView(title);
        TextView sub=text("O‘zbekcha + English • Learn • Translate • Speak",15,false);
        sub.setGravity(Gravity.CENTER);
        sub.setTextColor(Color.rgb(95,105,118));
        hero.addView(sub);
        c.addView(hero);

        LinearLayout goal=sectionCard();
        goal.addView(text("🔥 Bugungi maqsad • Today’s goal",19,true));
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);pb.setProgress(prefs.getInt("daily_progress",20));
        goal.addView(pb,new LinearLayout.LayoutParams(-1,28));
        goal.addView(text("10 daqiqa • 5 words • 1 mini quiz",14,false));
        c.addView(goal);

        c.addView(text("Darajangiz • Your level",21,true));
        Button beginner=button("🌱  BEGINNER • 0 DAN BOSHLASH");
        Button medium=button("📘  INTERMEDIATE • O‘RTA DARAJA");
        Button advanced=button("🚀  ADVANCED • YUQORI DARAJA");
        c.addView(beginner);c.addView(medium);c.addView(advanced);

        c.addView(text("Tezkor • Quick access",21,true));
        LinearLayout quick1=new LinearLayout(this);
        quick1.setOrientation(LinearLayout.HORIZONTAL);
        Button translator=button("⇄  Translate");
        Button words=button("📚  Words");
        quick1.addView(translator,new LinearLayout.LayoutParams(0,125,1));
        quick1.addView(words,new LinearLayout.LayoutParams(0,125,1));
        c.addView(quick1);

        LinearLayout quick2=new LinearLayout(this);
        quick2.setOrientation(LinearLayout.HORIZONTAL);
        Button practice=button("🎯  Practice");
        Button progress=button("🏆  Progress");
        quick2.addView(practice,new LinearLayout.LayoutParams(0,125,1));
        quick2.addView(progress,new LinearLayout.LayoutParams(0,125,1));
        c.addView(quick2);

        LinearLayout streak=sectionCard();
        streak.addView(text("⭐  Streak • Ketma-ket kunlar",18,true));
        streak.addView(text(prefs.getInt("streak",1)+" day streak • "+prefs.getInt("xp",0)+" XP",16,false));
        c.addView(streak);

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(8,8,8,8);
        bottom.setBackgroundColor(Color.WHITE);
        String[] labels={"🏠\nHome","📚\nLearn","⇄\nTranslate","🎯\nPractice","👤\nProfile"};
        for(int i=0;i<labels.length;i++){
            TextView item=pill(labels[i],i==0?Color.rgb(228,248,243):Color.WHITE);
            final int idx=i;
            item.setOnClickListener(v->{
                if(idx==0) showHome();
                else if(idx==1) showBeginnerLesson();
                else if(idx==2) showTranslator();
                else if(idx==3) showPractice();
                else showProgress();
            });
            bottom.addView(item,new LinearLayout.LayoutParams(0,90,1));
        }
        root.addView(bottom);
        setContentView(root);

        menu.setOnClickListener(v->showMainMenu());
        beginner.setOnClickListener(v->showBeginnerLesson());
        medium.setOnClickListener(v->showLevelPage("INTERMEDIATE • O‘RTA DARAJA",
            new String[]{"Present & past tenses • Hozirgi va o‘tgan zamon","Travel conversations • Safar suhbatlari","Work & university • Ish va universitet","Listening practice • Tinglab tushunish"}));
        advanced.setOnClickListener(v->showLevelPage("ADVANCED • YUQORI DARAJA",
            new String[]{"Fluent conversation • Ravon suhbat","Academic English • Akademik ingliz tili","Presentations & writing • Taqdimot va yozuv","Idioms & natural speech • Tabiiy nutq"}));
        translator.setOnClickListener(v->showTranslator());
        words.setOnClickListener(v->showWordLesson());
        practice.setOnClickListener(v->showPractice());
        progress.setOnClickListener(v->showProgress());
    }

    private void showMainMenu(){
        String[] items={
            "🏠 Home • Bosh sahifa",
            "📚 Learn • O‘rganish",
            "⇄ Translator • Tarjimon",
            "⬇ Offline packs • Oflayn paketlar",
            "★ Favorites • Saqlanganlar",
            "🔊 Voice settings • Ovoz",
            "🏆 Progress • Natijalar",
            "ℹ About TilMate",
            "🔒 Privacy"
        };
        new AlertDialog.Builder(this)
            .setTitle("TilMate • Menu")
            .setItems(items,(d,which)->{
                if(which==0) showHome();
                else if(which==1) showBeginnerLesson();
                else if(which==2) showTranslator();
                else if(which==3) showOfflinePacks();
                else if(which==4) showSaved();
                else if(which==5) showVoiceSettings();
                else if(which==6) showProgress();
                else if(which==7) showAbout();
                else showPrivacy();
            })
            .setNegativeButton("Close • Yopish",null)
            .show();
    }

    private void showLevelPage(String titleText,String[] lessons){
        currentScreen="level";
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(20,20,20,30);
        r.setBackground(gradient(Color.rgb(240,250,247),Color.rgb(238,244,255),0));
        Button back=button("← Home • Bosh sahifa");
        r.addView(back);
        r.addView(text(titleText,25,true));
        r.addView(text("Choose a lesson • Darsni tanlang",16,false));
        for(String lesson:lessons){
            LinearLayout box=sectionCard();
            box.addView(text("✓ "+lesson,18,true));
            box.addView(text("Tap to open • Ochish uchun bosing",14,false));
            r.addView(box);
        }
        back.setOnClickListener(v->showHome());
        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void showPractice(){
        currentScreen="practice";
        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(20,20,20,30);
        r.setBackground(gradient(Color.rgb(240,250,247),Color.rgb(238,244,255),0));
        Button back=button("← Home • Bosh sahifa");r.addView(back);
        r.addView(text("🎯 Daily Practice • Kundalik mashq",25,true));
        String[][] items={{"Word of the day","confident — ishonchli"},{"Phrase of the day","How can I help you? — Sizga qanday yordam bera olaman?"},{"Listening","Listen and repeat 3 sentences"},{"Quick quiz","5 questions • 2 minutes"}};
        for(String[] x:items){
            LinearLayout box=sectionCard();
            box.addView(text(x[0],18,true));
            box.addView(text(x[1],15,false));
            r.addView(box);
        }
        back.setOnClickListener(v->showHome());
        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void showVoiceSettings(){
        String[] choices={"Slow • Sekin (0.75×)","Clear • Aniq (0.86×)","Normal • Oddiy (1.0×)","Test English voice","Test Uzbek voice"};
        new AlertDialog.Builder(this)
            .setTitle("🔊 Voice settings • Ovoz")
            .setItems(choices,(d,which)->{
                if(which<=2){
                    voiceSpeed=which==0?0.75f:(which==1?0.86f:1.0f);
                    prefs.edit().putFloat("voice_speed",voiceSpeed).apply();
                    if(tts!=null)tts.setSpeechRate(voiceSpeed);
                    toast("Voice speed saved");
                }else if(which==3) speakTest("Welcome to TilMate. Learn English step by step.",Locale.US);
                else speakTest("Assalomu alaykum. TilMate bilan ingliz tilini o‘rganamiz.",new Locale("uz","UZ"));
            })
            .setNegativeButton("Close • Yopish",null).show();
    }

    private void speakTest(String phrase,Locale locale){
        if(tts==null)return;
        int a=tts.isLanguageAvailable(locale);
        if(a==TextToSpeech.LANG_MISSING_DATA||a==TextToSpeech.LANG_NOT_SUPPORTED){
            toast("This voice is not installed on your phone");
            return;
        }
        tts.setLanguage(locale);tts.setSpeechRate(voiceSpeed);tts.setPitch(1.0f);
        tts.speak(phrase,TextToSpeech.QUEUE_FLUSH,null,"voice-test");
    }

    private void showAbout(){
        new AlertDialog.Builder(this)
            .setTitle("TilMate")
            .setMessage("TilMate: English ↔ Uzbek\n\nLearn • Translate • Speak\nO‘rganing • Tarjima qiling • Gapiring\n\nVersion 3.0")
            .setPositiveButton("OK",null).show();
    }

    private void showPrivacy(){
        new AlertDialog.Builder(this)
            .setTitle("Privacy • Maxfiylik")
            .setMessage("TilMate stores learning progress, favorites and downloaded language packs on your device. A full Play Store privacy policy will be linked before public release.")
            .setPositiveButton("OK",null).show();
    }

    private void showBeginnerLesson(){
        currentScreen="lesson";
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
        currentScreen="words";
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,28);
        Button back=button("← BOSH SAHIFA");r.addView(back);
        r.addView(text("Kundalik so‘zlar",24,true));
        String[] words={"water — suv","food — ovqat","book — kitob","teacher — o‘qituvchi","student — talaba","house — uy","friend — do‘st","today — bugun","tomorrow — ertaga","money — pul"};
        for(String w:words)r.addView(text(w,19,false));
        back.setOnClickListener(v->showHome());
        ScrollView sv=new ScrollView(this);sv.addView(r);setContentView(sv);
    }

    private void showProgress(){
        currentScreen="progress";
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(20,20,20,28);
        Button back=button("← BOSH SAHIFA");r.addView(back);
        int p=prefs.getInt("lesson_progress",0);
        r.addView(text("Mening natijam",24,true));
        r.addView(text(p>0?"✓ 1-dars testi bajarildi. Davom eting!":"Hali dars yakunlanmagan. “0 dan boshlash” orqali boshlang.",18,false));
        back.setOnClickListener(v->showHome());
        setContentView(r);
    }

    private void showTranslator(){
        currentScreen="translator";
        if(!continuousVoice){
            confirmedSpeech="";
            confirmedVoiceTranslation="";
        }
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(250,252,252));

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(14,10,14,8);
        top.setBackgroundColor(Color.WHITE);

        TextView menu=text("☰",22,false);
        menu.setGravity(Gravity.CENTER);
        menu.setPadding(8,8,8,8);
        top.addView(menu,new LinearLayout.LayoutParams(72,72));

        TextView head=text("TilMate",20,true);
        head.setGravity(Gravity.CENTER);
        head.setPadding(8,8,8,8);
        top.addView(head,new LinearLayout.LayoutParams(0,72,1));

        TextView home=text("⌂",22,false);
        home.setGravity(Gravity.CENTER);
        home.setPadding(8,8,8,8);
        top.addView(home,new LinearLayout.LayoutParams(72,72));
        root.addView(top);

        ScrollView sv=new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout c=new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(18,8,18,30);
        sv.addView(c);

        TextView sourceTitle=text(enToUz?"English":"Uzbek",15,true);
        sourceTitle.setTextColor(Color.rgb(62,74,88));
        sourceTitle.setPadding(6,10,6,4);
        c.addView(sourceTitle);

        input=new EditText(this);
        input.setHint(enToUz?"Type here":"Shu yerga yozing");
        input.setTextSize(18);
        input.setTextColor(Color.rgb(22,29,36));
        input.setHintTextColor(Color.rgb(148,156,166));
        input.setGravity(Gravity.TOP|Gravity.START);
        input.setPadding(18,16,18,16);
        input.setMinLines(5);
        input.setMaxLines(12);
        input.setMinHeight(230);
        input.setLineSpacing(3f,1.03f);
        input.setBackground(rounded(Color.WHITE,24));
        input.setVerticalScrollBarEnabled(true);
        c.addView(input,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout inputTools=new LinearLayout(this);
        inputTools.setOrientation(LinearLayout.HORIZONTAL);
        inputTools.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        inputTools.setPadding(0,4,0,6);

        TextView mic=text("🎤",21,false);
        mic.setGravity(Gravity.CENTER);
        mic.setBackground(rounded(Color.TRANSPARENT,40));
        inputTools.addView(mic,new LinearLayout.LayoutParams(64,58));
        c.addView(inputTools);

        View divider=new View(this);
        divider.setBackgroundColor(Color.rgb(230,234,238));
        LinearLayout.LayoutParams dividerLp=new LinearLayout.LayoutParams(-1,1);
        dividerLp.setMargins(0,4,0,6);
        c.addView(divider,dividerLp);

        TextView targetTitle=text(enToUz?"Uzbek":"English",15,true);
        targetTitle.setTextColor(Color.rgb(62,74,88));
        targetTitle.setPadding(6,8,6,4);
        c.addView(targetTitle);

        output=text("Translation will appear automatically\nTarjima avtomatik ko‘rinadi",18,false);
        output.setTextColor(Color.rgb(22,29,36));
        output.setBackground(rounded(Color.WHITE,24));
        output.setMinHeight(220);
        output.setPadding(18,16,18,16);
        output.setLineSpacing(3f,1.03f);
        output.setGravity(Gravity.TOP|Gravity.START);
        output.setTextIsSelectable(true);
        c.addView(output,new LinearLayout.LayoutParams(-1,-2));

        LinearLayout outputTools=new LinearLayout(this);
        outputTools.setOrientation(LinearLayout.HORIZONTAL);
        outputTools.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        outputTools.setPadding(0,4,0,8);

        TextView speak=text("🔊",21,false);
        TextView copy=text("⧉",22,false);
        TextView save=text("☆",24,false);
        speak.setGravity(Gravity.CENTER);
        copy.setGravity(Gravity.CENTER);
        save.setGravity(Gravity.CENTER);
        outputTools.addView(speak,new LinearLayout.LayoutParams(64,58));
        outputTools.addView(copy,new LinearLayout.LayoutParams(64,58));
        outputTools.addView(save,new LinearLayout.LayoutParams(64,58));
        c.addView(outputTools);

        LinearLayout languageBar=new LinearLayout(this);
        languageBar.setOrientation(LinearLayout.HORIZONTAL);
        languageBar.setGravity(Gravity.CENTER_VERTICAL);
        languageBar.setPadding(4,8,4,8);
        languageBar.setBackground(rounded(Color.WHITE,24));

        TextView sourceLang=text(enToUz?"English":"Uzbek",16,true);
        sourceLang.setGravity(Gravity.CENTER);
        sourceLang.setTextColor(BLUE);

        TextView directionArrow=text("⇄",23,true);
        directionArrow.setGravity(Gravity.CENTER);
        directionArrow.setTextColor(Color.rgb(72,82,92));

        TextView targetLang=text(enToUz?"Uzbek":"English",16,true);
        targetLang.setGravity(Gravity.CENTER);
        targetLang.setTextColor(BLUE);

        languageBar.addView(sourceLang,new LinearLayout.LayoutParams(0,64,1));
        languageBar.addView(directionArrow,new LinearLayout.LayoutParams(70,64));
        languageBar.addView(targetLang,new LinearLayout.LayoutParams(0,64,1));
        c.addView(languageBar);

        direction=text(enToUz?"English → Uzbek":"Uzbek → English",12,false);
        direction.setGravity(Gravity.CENTER);
        direction.setTextColor(Color.rgb(130,138,148));
        direction.setPadding(4,2,4,10);
        c.addView(direction);

        TextView liveLabel=text("Live translation",13,false);
        liveLabel.setTextColor(Color.rgb(24,128,105));
        liveLabel.setGravity(Gravity.CENTER);
        liveLabel.setPadding(4,2,4,10);
        c.addView(liveLabel);

        TextView engineStatus=text(
            isAiPackReady()?"✓ AI translation ready":"AI translation pack required for full sentences",
            12,false);
        engineStatus.setTextColor(isAiPackReady()?Color.rgb(24,128,105):Color.rgb(130,100,35));
        engineStatus.setGravity(Gravity.CENTER);
        engineStatus.setPadding(4,8,4,8);
        c.addView(engineStatus);

        TextView packSettings=text("Offline packs & settings",13,false);
        packSettings.setTextColor(BLUE);
        packSettings.setGravity(Gravity.CENTER);
        packSettings.setPadding(4,6,4,14);
        c.addView(packSettings);

        root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        final Handler liveHandler=new Handler(Looper.getMainLooper());
        final Runnable[] pending=new Runnable[1];

        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                if(continuousVoice) return;
                if(pending[0]!=null) liveHandler.removeCallbacks(pending[0]);
                pending[0]=()->translateLive();
                liveHandler.postDelayed(pending[0],350);
            }
            @Override public void afterTextChanged(Editable e){}
        });

        directionArrow.setOnClickListener(v->{
            if(continuousVoice) stopVoiceTranslation();
            confirmedSpeech="";
            confirmedVoiceTranslation="";
            voiceSessionGeneration++;
            voiceAiWarningShown=false;
            enToUz=!enToUz;
            sourceLang.setText(enToUz?"English":"Uzbek");
            targetLang.setText(enToUz?"Uzbek":"English");
            sourceTitle.setText(enToUz?"English":"Uzbek");
            targetTitle.setText(enToUz?"Uzbek":"English");
            input.setHint(enToUz?"Type here":"Shu yerga yozing");
            direction.setText(enToUz?"English → Uzbek":"Uzbek → English");
            translateLive();
        });

        menu.setOnClickListener(v->showMainMenu());
        home.setOnClickListener(v->showHome());
        mic.setOnClickListener(v->startVoiceTranslation());
        speak.setOnClickListener(v->speak());
        copy.setOnClickListener(v->{
            ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE))
                .setPrimaryClip(android.content.ClipData.newPlainText("translation",output.getText()));
            toast("Copied");
        });
        save.setOnClickListener(v->saveFavorite());
        packSettings.setOnClickListener(v->showOfflinePacks());
    }

    private void showOfflinePacks(){
        if(continuousVoice) stopVoiceTranslation();
        currentScreen="offline";

        LinearLayout r=new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(20,18,20,30);
        r.setBackgroundColor(Color.rgb(250,252,252));

        TextView back=text("←  Translator",17,true);
        back.setPadding(8,12,8,16);
        r.addView(back);

        r.addView(text("Offline translation",24,true));
        r.addView(text("Manage translation models here. The main translator stays clean and focused.",14,false));

        LinearLayout basic=sectionCard();
        basic.addView(text("Basic Uzbek–English pack",18,true));
        packStatus=text(packFile.exists()
            ?"✓ Ready ("+enUz.size()+" entries)"
            :"Not downloaded yet",14,false);
        basic.addView(packStatus);
        Button download=button(packFile.exists()?"Update basic pack":"Download basic pack");
        download.setTextSize(14);
        basic.addView(download);
        r.addView(basic);

        LinearLayout ai=sectionCard();
        ai.addView(text("AI Translation Pack",18,true));
        aiPackStatus=text(isAiPackReady()
            ?"✓ AI translation ready on this device"
            :"Not installed • about 143 MB",14,false);
        ai.addView(aiPackStatus);
        Button aiDownload=button(isAiPackReady()
            ?"Reinstall AI translation"
            :"Download AI translation");
        aiDownload.setTextSize(14);
        ai.addView(aiDownload);
        ai.addView(text("Recommended for full sentences and live conversation.",13,false));
        r.addView(ai);

        back.setOnClickListener(v->showTranslator());
        download.setOnClickListener(v->downloadPack(download));
        aiDownload.setOnClickListener(v->downloadAiPack(aiDownload));

        ScrollView sv=new ScrollView(this);
        sv.addView(r);
        setContentView(sv);
    }

    private void startVoiceTranslation(){
        if(speechRecognizer==null){
            toast("Speech recognition is not available on this phone");
            return;
        }
        if(continuousVoice){
            stopVoiceTranslation();
            return;
        }
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_RECORD_AUDIO);
            return;
        }
        continuousVoice=true;
        voiceSessionGeneration++;
        voiceAiWarningShown=false;
        confirmedSpeech=input==null?"":input.getText().toString().trim();
        String currentOut=output==null?"":output.getText().toString().trim();
        if(currentOut.startsWith("Translation will appear") || currentOut.startsWith("Translating")
            || currentOut.startsWith("Tarjima avtomatik") || currentOut.startsWith("AI paketini")){
            currentOut="";
        }
        confirmedVoiceTranslation=currentOut;
        toast(enToUz?"Live English listening on":"Jonli o‘zbekcha tinglash yoqildi");
        beginListeningSession();
    }

    private void beginListeningSession(){
        if(!continuousVoice || speechRecognizer==null || input==null || !"translator".equals(currentScreen)) return;
        if(listening) return;
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,enToUz?"en-US":"uz-UZ");
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,1200L);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,700L);
        input.setHint(enToUz?"Listening in English…":"O‘zbekcha tinglanmoqda…");
        try{
            speechRecognizer.cancel();
            speechRecognizer.startListening(intent);
            listening=true;
        }catch(Exception ex){
            listening=false;
            if(continuousVoice) voiceHandler.postDelayed(()->beginListeningSession(),600);
        }
    }

    private void stopVoiceTranslation(){
        continuousVoice=false;
        listening=false;
        voiceSessionGeneration++;
        voiceHandler.removeCallbacksAndMessages(null);
        if(speechRecognizer!=null) speechRecognizer.cancel();
        if(input!=null) input.setHint(enToUz?"Type here":"Shu yerga yozing");
        toast("Live listening off");
    }

    private String joinSpeech(String base,String spoken){
        base=base==null?"":base.trim();
        spoken=spoken==null?"":spoken.trim();
        if(base.isEmpty()) return spoken;
        if(spoken.isEmpty()) return base;
        return base+" "+spoken;
    }

    private String capitalizeSentence(String s){
        if(s==null) return "";
        s=s.trim();
        if(s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0))+s.substring(1);
    }

    private boolean looksLikeQuestion(String spoken,boolean english){
        String s=spoken==null?"":spoken.trim().toLowerCase(Locale.ROOT);
        if(s.isEmpty()) return false;
        if(english){
            String[] q={"who ","what ","when ","where ","why ","how ","which ","whose ",
                "can ","could ","would ","should ","do ","does ","did ","is ","are ","am ",
                "have ","has ","had ","will ","may ","might "};
            for(String p:q) if(s.startsWith(p)) return true;
            return s.equals("who")||s.equals("what")||s.equals("when")||s.equals("where")
                ||s.equals("why")||s.equals("how");
        }else{
            String[] q={"kim ","nima ","qachon ","qayer","nega ","nimaga ","qanday ","qancha ",
                "necha ","qaysi ","qanaqa "};
            for(String p:q) if(s.startsWith(p)) return true;
            return s.endsWith("mi")||s.endsWith("mi?");
        }
    }

    private String punctuateSpeechChunk(String spoken,boolean english){
        String s=spoken==null?"":spoken.trim().replaceAll("\\s+"," ");
        if(s.isEmpty()) return "";
        s=capitalizeSentence(s);
        if(s.matches(".*[.!?…]$")) return s;
        return s+(looksLikeQuestion(s,english)?"?":".");
    }

    private String appendSentence(String base,String sentence){
        base=base==null?"":base.trim();
        sentence=sentence==null?"":sentence.trim();
        if(base.isEmpty()) return sentence;
        if(sentence.isEmpty()) return base;
        return base+" "+sentence;
    }

    private void translateVoiceChunk(String sourceChunk){
        if(sourceChunk==null || sourceChunk.trim().isEmpty() || output==null) return;
        final String chunk=sourceChunk.trim();
        final boolean directionSnapshot=enToUz;
        final int sessionSnapshot=voiceSessionGeneration;

        if(!isAiPackReady()){
            String key=chunk.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
            Map<String,String> map=directionSnapshot?enUz:uzEn;
            String ans=map.get(key);
            if(ans!=null){
                ans=punctuateSpeechChunk(ans,!directionSnapshot);
                confirmedVoiceTranslation=appendSentence(confirmedVoiceTranslation,ans);
                output.setText(confirmedVoiceTranslation);
            }else if(!voiceAiWarningShown){
                voiceAiWarningShown=true;
                String notice=directionSnapshot
                    ?"To‘liq jonli gap tarjimasi uchun AI Translation Pack-ni yuklab oling."
                    :"Download the AI Translation Pack for full live sentence translation.";
                confirmedVoiceTranslation=appendSentence(confirmedVoiceTranslation,notice);
                output.setText(confirmedVoiceTranslation);
            }
            return;
        }

        final String stableBefore=confirmedVoiceTranslation;
        output.setText(stableBefore.isEmpty()
            ?"Translating latest phrase…"
            :stableBefore+"\n\nTranslating latest phrase…");

        translationExecutor.execute(()->{
            try{
                File modelDir=new File(aiPackDir,directionSnapshot?"ai-pack/en-uz":"ai-pack/uz-en");
                File spm=findSpm(modelDir);
                if(spm==null) throw new IOException("Tokenizer file missing");
                String ans=nativeTranslate(modelDir.getAbsolutePath(),spm.getAbsolutePath(),chunk);
                final String polished=(ans==null||ans.trim().isEmpty())
                    ?(directionSnapshot?"Tarjima topilmadi.":"Translation unavailable.")
                    :polishTranslation(ans,directionSnapshot);
                runOnUiThread(()->{
                    if(sessionSnapshot!=voiceSessionGeneration || directionSnapshot!=enToUz) return;
                    confirmedVoiceTranslation=appendSentence(confirmedVoiceTranslation,polished);
                    if(output!=null) output.setText(confirmedVoiceTranslation);
                });
            }catch(Throwable ex){
                runOnUiThread(()->{
                    if(sessionSnapshot!=voiceSessionGeneration || directionSnapshot!=enToUz) return;
                    String key=chunk.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
                    Map<String,String> map=directionSnapshot?enUz:uzEn;
                    String ans=map.get(key);
                    if(ans==null) ans=directionSnapshot
                        ?"Tarjima mavjud emas."
                        :"Translation unavailable.";
                    confirmedVoiceTranslation=appendSentence(confirmedVoiceTranslation,ans);
                    if(output!=null) output.setText(confirmedVoiceTranslation);
                });
            }
        });
    }

    private String normalizeRecognizedSpeech(String text){
        if(text==null) return "";
        String s=text.trim().replaceAll("\\s+"," ");
        if(enToUz){
            s=s.replaceAll("(?i)\\b(team\\s*mate|tell\\s*mate|til\\s*mate|teammate|tellmate|telmate)\\b","TilMate");
        }
        return s;
    }

    private String chooseBestRecognition(Bundle bundle,boolean isFinal){
        ArrayList<String> matches=bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(matches==null||matches.isEmpty()) return "";

        float[] confidences=bundle.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);
        int limit=Math.min(matches.size(),3);
        int best=0;
        float bestScore=-1f;

        for(int i=0;i<limit;i++){
            String candidate=normalizeRecognizedSpeech(matches.get(i));
            if(candidate.isEmpty()) continue;
            float confidence=(confidences!=null && i<confidences.length)?confidences[i]:-1f;
            float score=confidence>=0?confidence:(1f-(i*0.08f));

            // Prefer alternatives that correctly identify the app name.
            if(candidate.contains("TilMate")) score+=0.12f;

            // Very short isolated low-confidence final results are common false starts.
            if(isFinal && confirmedSpeech.isEmpty() && candidate.split("\\s+").length==1
                && confidence>=0 && confidence<0.45f){
                score-=0.60f;
            }

            if(score>bestScore){bestScore=score;best=i;}
        }

        String chosen=normalizeRecognizedSpeech(matches.get(best));
        if(isFinal && confidences!=null && best<confidences.length
            && confidences[best]>=0 && confidences[best]<0.28f
            && chosen.split("\\s+").length<=2){
            return "";
        }
        return chosen;
    }

    private void applySpeechResults(Bundle bundle,boolean isFinal){
        if(bundle==null||input==null)return;
        String spoken=chooseBestRecognition(bundle,isFinal);
        if(spoken.isEmpty())return;

        if(isFinal){
            String chunk=punctuateSpeechChunk(spoken,enToUz);
            confirmedSpeech=appendSentence(confirmedSpeech,chunk);
            input.setText(confirmedSpeech);
            input.setSelection(input.length());
            translateVoiceChunk(chunk);
        }else{
            String partial=capitalizeSentence(spoken);
            String display=joinSpeech(confirmedSpeech,partial);
            input.setText(display);
            input.setSelection(input.length());
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_RECORD_AUDIO){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) startVoiceTranslation();
            else toast("Microphone permission is needed for voice translation");
        }
    }

    private File findSpm(File dir){
        File[] files=dir.listFiles();
        if(files==null) return null;
        for(File f:files) if(f.isFile() && f.getName().endsWith(".spm")) return f;
        return null;
    }

    private String polishTranslation(String text, boolean directionEnToUz){
        if(text==null) return "";
        String out=text.trim().replaceAll("\\s+([,.!?;:])","$1").replaceAll("[ \\t]{2,}"," ");
        if(directionEnToUz){
            // High-confidence Uzbek terminology cleanup for common education/business text.
            out=out.replace("litsenziyalash kurslari","tanlov fanlari");
            out=out.replace("litsenziya kurslari","tanlov fanlari");
            out=out.replace("Biznes boshqaruvi magistri","Biznesni boshqarish magistri");
            out=out.replace("biznes boshqaruvi magistri","biznesni boshqarish magistri");
            out=out.replace("MBA odatda umumiy dastur bo'lishi kerak","MBA odatda umumiy yo'nalishdagi dastur sifatida ko'zda tutilgan");
            out=out.replace("MBA odatda umumiy dastur bo‘lishi kerak","MBA odatda umumiy yo‘nalishdagi dastur sifatida ko‘zda tutilgan");
            out=out.replace("mamlakat sanoatlashgan va kompaniyalar ilmiy boshqaruvga intilganida","AQShda sanoatlashuv jarayoni kechayotgan va kompaniyalar boshqaruvning ilmiy usullariga intilayotgan davrda");
        }
        return out;
    }

    private void translateLive(){
        if(input==null || output==null) return;
        String raw=input.getText().toString().trim();
        if(raw.isEmpty()){
            translationGeneration++;
            output.setText("Translation will appear automatically\nTarjima avtomatik ko‘rinadi");
            return;
        }

        if(isAiPackReady()){
            output.setText("Translating… • Tarjima qilinmoqda…");
            final boolean directionSnapshot=enToUz;
            final String sourceSnapshot=raw;
            final int generation=++translationGeneration;
            translationExecutor.execute(()->{
                if(generation!=translationGeneration) return;
                try{
                    File modelDir=new File(aiPackDir,directionSnapshot?"ai-pack/en-uz":"ai-pack/uz-en");
                    File spm=findSpm(modelDir);
                    if(spm==null) throw new IOException("Tokenizer file missing");
                    String ans=nativeTranslate(modelDir.getAbsolutePath(),spm.getAbsolutePath(),sourceSnapshot);
                    runOnUiThread(()->{
                        if(generation==translationGeneration && input!=null
                            && sourceSnapshot.equals(input.getText().toString().trim())
                            && directionSnapshot==enToUz){
                            output.setText(ans==null||ans.trim().isEmpty()
                                ?(directionSnapshot?"Tarjima topilmadi.":"Translation unavailable.")
                                :polishTranslation(ans,directionSnapshot));
                        }
                    });
                }catch(Throwable ex){
                    runOnUiThread(()->{
                        if(generation==translationGeneration) translateLiveFallback(sourceSnapshot,directionSnapshot);
                    });
                }
            });
            return;
        }

        translateLiveFallback(raw,enToUz);
    }

    private void translateLiveFallback(String raw,boolean directionSnapshot){
        String key=raw.toLowerCase(Locale.ROOT).replaceAll("[.!?]+$","").trim();
        Map<String,String> map=directionSnapshot?enUz:uzEn;
        String ans=map.get(key);
        if(ans==null){
            ans=directionSnapshot
                ?"AI paketini yuklab oling — erkin gaplarni oflayn tarjima qilish uchun."
                :"Download the AI pack for offline open-ended sentence translation.";
        }
        output.setText(ans);
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
        currentScreen="saved";
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
        String s=output==null?"":output.getText().toString();
        if(s.isEmpty()||s.startsWith("Translation")||s.startsWith("Tarjima")){toast("Translate something first");return;}
        Locale locale=enToUz?new Locale("uz","UZ"):Locale.US;
        int a=tts.isLanguageAvailable(locale);
        if(a==TextToSpeech.LANG_MISSING_DATA||a==TextToSpeech.LANG_NOT_SUPPORTED){
            toast("Voice not installed. Open Voice settings.");
            return;
        }
        tts.setLanguage(locale);tts.setSpeechRate(voiceSpeed);tts.setPitch(1.0f);
        tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"translation");
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onInit(int status){if(status==TextToSpeech.SUCCESS){tts.setSpeechRate(voiceSpeed);tts.setPitch(1.0f);}}
    @Override public void onBackPressed(){
        if("home".equals(currentScreen)){
            super.onBackPressed();
        }else if("translator".equals(currentScreen)){
            if(continuousVoice) stopVoiceTranslation();
            showHome();
        }else{
            showTranslator();
        }
    }

    @Override protected void onDestroy(){
        if(speechRecognizer!=null){speechRecognizer.cancel();speechRecognizer.destroy();}
        continuousVoice=false;
        voiceHandler.removeCallbacksAndMessages(null);
        translationExecutor.shutdownNow();
        if(tts!=null)tts.shutdown();
        super.onDestroy();
    }
}
