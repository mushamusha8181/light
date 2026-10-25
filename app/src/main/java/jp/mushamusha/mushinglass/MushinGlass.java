package jp.mushamusha.mushinglass;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shared visual identity for Mushamusha apps. */
public final class MushinGlass implements Application.ActivityLifecycleCallbacks {
    public static final String COLOR_PREFS = "color_settings";
    public static final String KEY_PALETTE = "palette";
    public static final String KEY_TEXT_COLOR = "text_color";
    public static final String KEY_GLASS_OPACITY = "glass_opacity";
    public static final String DEFAULT_PALETTE = "green_light";
    public static final String DEFAULT_TEXT_COLOR = "black";
    public static final int DEFAULT_GLASS_OPACITY = 72;

    public static final class Palette {
        public final String id, label;
        public final int primary, foreground;
        Palette(String id, String label, String primary, String foreground) {
            this.id=id; this.label=label; this.primary=Color.parseColor(primary); this.foreground=Color.parseColor(foreground);
        }
    }
    public static final class NamedColor {
        public final String id, label;
        public final int color;
        NamedColor(String id, String label, String color) { this.id=id; this.label=label; this.color=Color.parseColor(color); }
    }
    public static final class Config {
        public final Palette palette;
        public final int textColor, glassOpacity, background, band, listBackground, rowBackground, middle, auxiliary;
        Config(Palette p, int text, int opacity) {
            palette=p; textColor=text; glassOpacity=opacity;
            background=mix(p.primary, Color.WHITE, .82f);
            band=mix(p.primary, background, .70f);
            listBackground=mix(p.primary, Color.WHITE, .90f);
            rowBackground=mix(p.primary, Color.WHITE, .92f);
            middle=mix(p.primary, background, .45f);
            auxiliary=mix(p.foreground, background, .45f);
        }
    }

    private static final LinkedHashMap<String, Palette> PALETTES = new LinkedHashMap<>();
    private static final LinkedHashMap<String, NamedColor> TEXT = new LinkedHashMap<>();
    private static final LinkedHashMap<String, NamedColor> ICONS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, String> ALIASES = new LinkedHashMap<>();
    private static final List<WeakReference<Activity>> ACTIVITIES = new ArrayList<>();
    private static volatile MushinGlass instance;
    private static volatile Typeface mushinTypeface;

    static {
        palette("mono_light","ブラック","#1A1A1A","#1A1A1A"); palette("white","ホワイト","#FFFFFF","#333333");
        palette("mono_dark","グレー","#8C8C8C","#262626"); palette("green","グリーン","#5E8C6E","#2F5A44");
        palette("green_light","薄い緑","#7FA98C","#2F5A44"); palette("red_rose","ピンク","#B2798C","#3A2A31");
        palette("olive_gray","モスグレー","#5E6455","#2B2E27"); palette("teal_gray","モスグリーン","#556B3E","#2E3524");
        palette("moss_pink","モスピンク","#8E5A6B","#3A2A31"); palette("moss_red","モスレッド","#8E4F4A","#3A2B29");
        palette("moss_black","モスブラック","#3A3A3A","#262626"); palette("red_night","セージ","#8DA08B","#2B3129");
        palette("graphite","チャコール","#4F555C","#262626"); palette("slate","スレート","#5A6672","#262626");
        palette("blue_sky","ブルーグレー","#6B7C93","#2A323D"); palette("blue_night","ネイビー","#4B5566","#262C36");
        palette("mauve_gray","モーヴ","#7E7286","#2E2A33"); palette("sand_gray","サンド","#A99A82","#38322A");
        palette("brown_latte","テラコッタ","#A8796A","#3A2E28"); palette("brown_cocoa","ブラウン","#7A6558","#332A25");

        text("black","黒","#000000"); text("white","白","#FFFFFF"); text("gray_light","薄いグレー","#9E9E9E");
        text("gray_dark","濃いグレー","#424242"); text("red_dark","濃い赤","#8E2F2F"); text("red_light","薄い赤","#C97B7B");
        text("pink_dark","濃いピンク","#A3435F"); text("pink_light","薄いピンク","#D89AAC"); text("green_dark","濃い緑","#2F6B45");
        text("green_light","薄い緑","#7FA98C"); text("moss_gray_dark","濃いモスグレー","#5E6455"); text("moss_gray_light","薄いモスグレー","#9AA08F");
        text("moss_green_dark","濃いモスグリーン","#556B3E"); text("moss_green_light","薄いモスグリーン","#8FA277");
        text("brown_dark","濃い茶色","#6F4A2A"); text("brown_light","薄い茶色","#B08A63"); text("blue_dark","濃い青","#2F4F8E");
        text("blue_light","薄い青","#7B90C4"); text("sky_dark","濃い空色","#2F6E8E"); text("sky_light","薄い空色","#7FAFC4");

        icon("default","標準（モスグリーン）","#436E6A","IconDefault"); icon("green","グリーン","#4E8F6B","IconGreen");
        icon("green_light","薄い緑","#7FA98C","IconGreenLight"); icon("white","ホワイト","#FFFFFF","IconWhite");
        icon("moss_pink","モスピンク","#8E5A6B","IconMossPink"); icon("moss_red","モスレッド","#8E4F4A","IconMossRed");
        icon("moss_black","モスブラック","#3A3A3A","IconMossBlack"); icon("mono_light","ブラック","#000000","IconMonoLight");
        icon("mono_dark","グレー","#828385","IconMonoDark"); icon("graphite","チャコール","#546E7A","IconGraphite");
        icon("slate","スレート","#455A64","IconSlate"); icon("teal_gray","モスグリーン","#436E6A","IconTealGray");
        icon("olive_gray","モスグレー","#666B4D","IconOliveGray"); icon("mauve_gray","モーヴ","#6D5A70","IconMauveGray");
        icon("sand_gray","サンド","#756A5C","IconSandGray"); icon("blue_sky","ブルーグレー","#3568B8","IconBlueSky");
        icon("blue_night","ネイビー","#4F78C8","IconBlueNight"); icon("red_rose","ピンク","#C05278","IconRedRose");
        icon("red_night","セージ","#A23B48","IconRedNight"); icon("brown_latte","テラコッタ","#806353","IconBrownLatte");
        icon("brown_cocoa","ブラウン","#795548","IconBrownCocoa");
    }

    private MushinGlass() {}
    private static void palette(String id,String label,String a,String b){ PALETTES.put(id,new Palette(id,label,a,b)); }
    private static void text(String id,String label,String c){ TEXT.put(id,new NamedColor(id,label,c)); }
    private static void icon(String id,String label,String c,String alias){ ICONS.put(id,new NamedColor(id,label,c)); ALIASES.put(id,alias); }
    public static Map<String,Palette> palettes(){ return Collections.unmodifiableMap(PALETTES); }
    public static Map<String,NamedColor> textColors(){ return Collections.unmodifiableMap(TEXT); }

    public static void install(Context context) {
        if (instance != null || !(context.getApplicationContext() instanceof Application)) return;
        synchronized (MushinGlass.class) {
            if (instance != null) return;
            instance=new MushinGlass();
            ((Application)context.getApplicationContext()).registerActivityLifecycleCallbacks(instance);
            IconManager.apply(context.getApplicationContext());
        }
    }

    public static Config config(Context context) {
        SharedPreferences p=context.getSharedPreferences(COLOR_PREFS,Context.MODE_PRIVATE);
        Palette palette=PALETTES.get(p.getString(KEY_PALETTE,DEFAULT_PALETTE));
        if(palette==null) palette=PALETTES.get(DEFAULT_PALETTE);
        NamedColor tc=TEXT.get(p.getString(KEY_TEXT_COLOR,DEFAULT_TEXT_COLOR));
        int opacity=Math.max(35,Math.min(95,p.getInt(KEY_GLASS_OPACITY,DEFAULT_GLASS_OPACITY)));
        return new Config(palette,tc==null?Color.BLACK:tc.color,opacity);
    }

    public static void refreshAll() {
        synchronized(ACTIVITIES){ for(int i=ACTIVITIES.size()-1;i>=0;i--){ Activity a=ACTIVITIES.get(i).get(); if(a==null||a.isFinishing()) ACTIVITIES.remove(i); else a.runOnUiThread(a::recreate); } }
    }

    private static void restyle(Activity a) {
        Config c=config(a); Window w=a.getWindow();
        w.setStatusBarColor(withAlpha(c.band,220)); w.setNavigationBarColor(withAlpha(c.background,245));
        if(Build.VERSION.SDK_INT>=30){ WindowInsetsController ctl=w.getInsetsController(); if(ctl!=null) ctl.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS); }
        else if(Build.VERSION.SDK_INT>=26) w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        else if(Build.VERSION.SDK_INT>=23) w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        // Do a few bounded passes only. This catches views added just after setContentView/render
        // without keeping a permanent layout observer or background monitor alive.
        applyActivity(a,c);
        w.getDecorView().postDelayed(() -> applyActivity(a,config(a)),120);
        w.getDecorView().postDelayed(() -> applyActivity(a,config(a)),520);
    }

    private static void applyActivity(Activity a, Config c) {
        if(a.isFinishing() || (Build.VERSION.SDK_INT>=17 && a.isDestroyed())) return;
        View content=a.findViewById(android.R.id.content);
        if(content!=null){ content.setBackgroundColor(c.background); styleTree(a,content,c); }
        addAppearanceEntry(a,c);
    }

    /** Apply the current MUSHIN appearance to a newly-created row/dialog subtree. */
    public static void applyToView(Activity a, View v) {
        if(a==null || v==null) return;
        styleTree(a,v,config(a));
    }

    private static void styleTree(Activity a, View v, Config c) {
        String cn=v.getClass().getName();
        if(v instanceof WebView||v instanceof ImageView||cn.contains("PreviewView")||cn.contains("SurfaceView")||cn.contains("TextureView")||cn.contains("GLSurfaceView")) return;
        String simple=v.getClass().getSimpleName(), id=resName(a,v.getId());
        ColorStateList accent=ColorStateList.valueOf(c.palette.primary);
        if(v instanceof TextView){
            TextView t=(TextView)v; Typeface f=typeface(a); if(f!=null) t.setTypeface(f,t.getTypeface()!=null&&t.getTypeface().isBold()?Typeface.BOLD:Typeface.NORMAL);
            if(!keepColor(a,t)) t.setTextColor(c.textColor); t.setIncludeFontPadding(false);
            if(t instanceof CompoundButton){
                CompoundButton cb=(CompoundButton)t;
                if(Build.VERSION.SDK_INT>=21) cb.setButtonTintList(accent);
                if(t instanceof Switch && Build.VERSION.SDK_INT>=21){ Switch sw=(Switch)t; sw.setThumbTintList(accent); sw.setTrackTintList(ColorStateList.valueOf(withAlpha(c.palette.primary,92))); }
            } else if(t instanceof Button){
                Button b=(Button)t; b.setAllCaps(false); b.setElevation(0); if(Build.VERSION.SDK_INT>=21)b.setStateListAnimator(null);
                int color=(b.isSelected()||b.isActivated())?c.palette.primary:Color.WHITE;
                int opacity=(b.isSelected()||b.isActivated())?Math.max(44,c.glassOpacity-16):Math.max(48,c.glassOpacity-8);
                b.setBackground(glassDrawable(a,color,opacity,14)); int x=dp(a,14),y=dp(a,9); b.setPadding(x,y,x,y);
            } else if(t instanceof EditText){
                t.setBackground(glassDrawable(a,Color.WHITE,Math.max(42,c.glassOpacity-16),12)); int x=dp(a,12),y=dp(a,9); t.setPadding(x,y,x,y);
            }
        }
        if(v instanceof SeekBar && Build.VERSION.SDK_INT>=21){ ((SeekBar)v).setProgressTintList(accent); ((SeekBar)v).setThumbTintList(accent); }
        if(v instanceof ProgressBar && Build.VERSION.SDK_INT>=21){ ((ProgressBar)v).setProgressTintList(accent); ((ProgressBar)v).setIndeterminateTintList(accent); }
        if(v instanceof Spinner){ v.setBackground(glassDrawable(a,Color.WHITE,Math.max(42,c.glassOpacity-16),12)); }
        String key=id.toLowerCase(java.util.Locale.ROOT);
        boolean glass=simple.contains("CardView")||simple.contains("Toolbar")||simple.contains("BottomNavigation")||simple.contains("NavigationBar")||simple.contains("AppBar")||key.contains("card")||key.contains("panel")||key.contains("toolbar")||key.contains("bottom_bar")||key.contains("bottomnav")||key.contains("bottom_nav")||key.contains("top_bar")||key.contains("controls")||key.contains("searchbox")||key.contains("search_box")||key.contains("settingssection")||key.contains("settings_section");
        if(glass && !(v instanceof TextView)){ v.setBackground(glassDrawable(a,Color.WHITE,Math.max(44,c.glassOpacity-5),16)); v.setElevation(0); }
        if(v instanceof ViewGroup){ ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) styleTree(a,g.getChildAt(i),c); }
    }

    private static boolean keepColor(Context c, TextView v) {
        Object tag=v.getTag(); if(tag!=null&&"keep_text_color".equals(String.valueOf(tag))) return true;
        String id=resName(c,v.getId()); if("file_name".equals(id)||id.contains("operation_row")||id.contains("now_playing")||id.contains("playing_title")){ v.setTextColor(Color.BLACK); return true; }
        return false;
    }

    private static void addAppearanceEntry(Activity a, Config c) {
        String n=a.getClass().getSimpleName(); if(!("MainActivity".equals(n)||n.contains("Settings"))) return;
        View d=a.getWindow().getDecorView(); if(!(d instanceof FrameLayout)) return; FrameLayout f=(FrameLayout)d;
        String tag="mushin_glass_appearance"; if(f.findViewWithTag(tag)!=null)return;
        TextView gear=new TextView(a); gear.setTag(tag); gear.setText("⚙"); gear.setTextSize(20); gear.setGravity(Gravity.CENTER); gear.setContentDescription("外観設定"); gear.setTextColor(c.textColor);
        Typeface tf=typeface(a); if(tf!=null)gear.setTypeface(tf); gear.setBackground(glassDrawable(a,Color.WHITE,Math.max(58,c.glassOpacity),999)); gear.setElevation(dp(a,2));
        gear.setOnClickListener(v -> a.startActivity(new Intent(a,AppearanceActivity.class)));
        int s=dp(a,46); FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(s,s,Gravity.TOP|Gravity.END); lp.topMargin=dp(a,12)+statusInset(a); lp.rightMargin=dp(a,12); f.addView(gear,lp);
    }

    private static int statusInset(Activity a){ if(Build.VERSION.SDK_INT>=30&&a.getWindow().getDecorView().getRootWindowInsets()!=null)return a.getWindow().getDecorView().getRootWindowInsets().getInsets(WindowInsets.Type.statusBars()).top; int id=a.getResources().getIdentifier("status_bar_height","dimen","android"); return id>0?a.getResources().getDimensionPixelSize(id):0; }
    public static GradientDrawable glassDrawable(Context c,int color,int opacity,float radius){ GradientDrawable d=new GradientDrawable(); d.setColor(withAlpha(color,Math.round(255f*Math.max(0,Math.min(100,opacity))/100f))); d.setCornerRadius(dp(c,radius)); d.setStroke(dp(c,1),Color.argb(165,255,255,255)); return d; }
    public static int mix(int a,int b,float t){ t=Math.max(0,Math.min(1,t)); return Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*t),Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*t),Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t)); }
    public static int withAlpha(int c,int a){ return Color.argb(Math.max(0,Math.min(255,a)),Color.red(c),Color.green(c),Color.blue(c)); }
    public static int dp(Context c,float v){ return Math.round(v*c.getResources().getDisplayMetrics().density); }
    private static String resName(Context c,int id){ if(id==View.NO_ID||id==0)return ""; try{return c.getResources().getResourceEntryName(id);}catch(Throwable e){return "";} }
    public static Typeface typeface(Context c){ if(mushinTypeface!=null)return mushinTypeface; synchronized(MushinGlass.class){ if(mushinTypeface==null){ try{mushinTypeface=Typeface.createFromAsset(c.getAssets(),"mushin.otf");}catch(Throwable e){ if(Build.VERSION.SDK_INT>=26)try{int id=c.getResources().getIdentifier("mushin","font",c.getPackageName());if(id!=0)mushinTypeface=c.getResources().getFont(id);}catch(Throwable ignored){} } } return mushinTypeface; } }

    @Override public void onActivityCreated(Activity a,Bundle b){ synchronized(ACTIVITIES){ACTIVITIES.add(new WeakReference<>(a));} restyle(a); }
    @Override public void onActivityResumed(Activity a){restyle(a);} @Override public void onActivityStarted(Activity a){} @Override public void onActivityPaused(Activity a){} @Override public void onActivityStopped(Activity a){} @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    @Override public void onActivityDestroyed(Activity a){ synchronized(ACTIVITIES){for(int i=ACTIVITIES.size()-1;i>=0;i--){Activity x=ACTIVITIES.get(i).get();if(x==null||x==a)ACTIVITIES.remove(i);}} }

    /** Runs before activities without replacing the app's own Application class. */
    public static final class InitProvider extends ContentProvider {
        @Override public boolean onCreate(){ if(getContext()!=null)install(getContext()); return true; }
        @Override public Cursor query(Uri u,String[]p,String s,String[]a,String o){return null;} @Override public String getType(Uri u){return null;}
        @Override public Uri insert(Uri u,ContentValues v){return null;} @Override public int delete(Uri u,String s,String[]a){return 0;} @Override public int update(Uri u,ContentValues v,String s,String[]a){return 0;}
    }

    public static final class IconManager {
        public static final String PREFS="app_icon", KEY_MODE="mode", KEY_APPLIED="applied", DEFAULT_MODE="teal_gray";
        private IconManager(){}
        public static String currentMode(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_MODE,DEFAULT_MODE);}
        public static void setMode(Context c,String m){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_MODE,m).apply();apply(c);}
        public static void apply(Context c){SharedPreferences p=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE);String m=p.getString(KEY_MODE,DEFAULT_MODE),r=resolve(c,m),suffix=ALIASES.get(r);if(suffix==null){r=DEFAULT_MODE;suffix=ALIASES.get(r);} PackageManager pm=c.getPackageManager(); try{pm.setComponentEnabledSetting(component(c,suffix),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);for(String x:ALIASES.values())if(!x.equals(suffix))pm.setComponentEnabledSetting(component(c,x),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);p.edit().putString(KEY_APPLIED,r).apply();}catch(Throwable ignored){}}
        private static String resolve(Context c,String m){if("follow".equals(m)){String p=c.getSharedPreferences(COLOR_PREFS,Context.MODE_PRIVATE).getString(KEY_PALETTE,DEFAULT_PALETTE);return ALIASES.containsKey(p)?p:DEFAULT_MODE;}return ALIASES.containsKey(m)?m:DEFAULT_MODE;}
        private static ComponentName component(Context c,String suffix){return new ComponentName(c.getPackageName(),"jp.mushamusha.mushinglass."+suffix);}
    }

    /** Unified appearance settings UI used by every Android app. */
    public static final class AppearanceActivity extends Activity {
        private LinearLayout body; private Typeface font;
        @Override protected void onCreate(Bundle b){super.onCreate(b);font=typeface(this);build();}
        private void build(){Config cfg=config(this);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(cfg.background);body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);int side=dp(this,18);body.setPadding(side,dp(this,18),side,dp(this,40));scroll.addView(body,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));setContentView(scroll);
            TextView title=txt("MUSHIN Glass",30,Color.BLACK);title.setTypeface(font,Typeface.BOLD);body.addView(title,lp(0,10));body.addView(txt("全アプリ共通の外観設定",14,cfg.auxiliary),lp(0,24));
            SharedPreferences prefs=getSharedPreferences(COLOR_PREFS,Context.MODE_PRIVATE);
            section("キーカラー");String selected=prefs.getString(KEY_PALETTE,DEFAULT_PALETTE);for(Palette p:PALETTES.values())choice(p.label,p.id,p.primary,p.id.equals(selected),()->{prefs.edit().putString(KEY_PALETTE,p.id).apply();if("follow".equals(IconManager.currentMode(this)))IconManager.apply(this);refreshAll();});
            section("文字色");String st=prefs.getString(KEY_TEXT_COLOR,DEFAULT_TEXT_COLOR);for(NamedColor n:TEXT.values())choice(n.label,n.id,n.color,n.id.equals(st),()->{prefs.edit().putString(KEY_TEXT_COLOR,n.id).apply();refreshAll();});
            section("半透明");opacity(prefs,cfg);
            section("アプリアイコン");String im=IconManager.currentMode(this);choice("キーカラー準拠","follow",cfg.palette.primary,"follow".equals(im),()->{IconManager.setMode(this,"follow");recreateSoon();});for(NamedColor n:ICONS.values())choice(n.label,n.id,n.color,n.id.equals(im),()->{IconManager.setMode(this,n.id);recreateSoon();});
            section("設計");TextView note=txt("白を基調に、薄い緑の光・半透明の面・細い境界線・大きめの余白を共通化しています。Webページ本文やカメラ映像そのものの色は変更しません。",14,cfg.textColor);note.setPadding(dp(this,16),dp(this,14),dp(this,16),dp(this,14));note.setBackground(glassDrawable(this,Color.WHITE,cfg.glassOpacity,22));body.addView(note,lp(0,12));
        }
        private void opacity(SharedPreferences p,Config c){LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);int pad=dp(this,16);panel.setPadding(pad,pad,pad,pad);panel.setBackground(glassDrawable(this,Color.WHITE,c.glassOpacity,22));TextView label=txt("ガラスの濃さ  "+c.glassOpacity+"%",15,c.textColor);panel.addView(label,lp(0,8));SeekBar seek=new SeekBar(this);seek.setMax(60);seek.setProgress(c.glassOpacity-35);panel.addView(seek,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));panel.addView(txt("左ほど透明、右ほど濃く表示します。",12,c.auxiliary),lp(8,0));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){int v=c.glassOpacity;public void onProgressChanged(SeekBar s,int x,boolean u){v=35+x;label.setText("ガラスの濃さ  "+v+"%");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){p.edit().putInt(KEY_GLASS_OPACITY,v).apply();refreshAll();}});body.addView(panel,lp(0,12));}
        private void section(String s){TextView t=txt(s,18,Color.BLACK);t.setTypeface(font,Typeface.BOLD);body.addView(t,lp(20,10));}
        private void choice(String label,String id,int color,boolean selected,Runnable action){Config c=config(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);int p=dp(this,14);row.setPadding(p,dp(this,11),p,dp(this,11));row.setBackground(glassDrawable(this,Color.WHITE,Math.max(48,c.glassOpacity-5),20));row.setClickable(true);row.setFocusable(true);View swatch=new View(this);swatch.setBackground(glassDrawable(this,color,100,999));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(this,24),dp(this,24));sp.rightMargin=dp(this,12);row.addView(swatch,sp);LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(txt(label,15,c.textColor));labels.addView(txt(id,11,c.auxiliary));row.addView(labels,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));TextView check=txt(selected?"✓":"",20,c.palette.foreground);check.setGravity(Gravity.CENTER);row.addView(check,new LinearLayout.LayoutParams(dp(this,30),ViewGroup.LayoutParams.WRAP_CONTENT));row.setOnClickListener(v->action.run());body.addView(row,lp(0,8));}
        private TextView txt(String s,float size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0,1.08f);t.setIncludeFontPadding(false);if(font!=null)t.setTypeface(font);return t;}
        private LinearLayout.LayoutParams lp(int top,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);p.topMargin=dp(this,top);p.bottomMargin=dp(this,bottom);return p;}
        private void recreateSoon(){body.postDelayed(this::recreate,100);}
    }
}
