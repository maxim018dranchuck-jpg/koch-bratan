package com.liquidkb.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Экран настроек: включение клавиатуры, пресеты, ползунки, своя картинка-фон. */
public class SettingsActivity extends Activity {
    private static final int REQ_IMAGE = 7;

    private LinearLayout root;
    private ScrollView scroll;
    private TextView blurStatus;
    private final List<Runnable> swatchers = new ArrayList<>();
    private final List<Runnable> syncers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        scroll = sv;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(380)); // запас снизу, чтобы можно было прокрутить выше клавиатуры
        sv.addView(root);
        setContentView(sv);

        title("Liquid Keyboard", 24);
        text("Стеклянная клавиатура с размытием фона. Все настройки применяются сразу — "
                + "нажми на поле проверки и двигай ползунки.");
        blurStatus = new TextView(this);
        blurStatus.setPadding(0, 0, 0, dp(12));
        root.addView(blurStatus);

        button("1. Включить клавиатуру в системе", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        button("2. Выбрать Liquid Keyboard для ввода", new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });

        EditText test = new EditText(this);
        test.setHint("Поле для проверки клавиатуры");
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(test);

        section("Пресеты");
        LinearLayout pr = new LinearLayout(this);
        pr.setOrientation(LinearLayout.VERTICAL);
        root.addView(pr);
        glass(pr, "Liquid Light", new String[]{Prefs.THEME, "1", Prefs.BLUR, "60", Prefs.PANEL, "55",
                Prefs.KEY_ALPHA, "75", Prefs.BORDER, "80", Prefs.SHINE, "65", Prefs.RADIUS, "14",
                Prefs.SWELL, "55", Prefs.SAT, "0"});
        glass(pr, "Liquid Dark", new String[]{Prefs.THEME, "2", Prefs.BLUR, "70", Prefs.PANEL, "45",
                Prefs.KEY_ALPHA, "55", Prefs.BORDER, "70", Prefs.SHINE, "55", Prefs.RADIUS, "14",
                Prefs.SWELL, "55", Prefs.SAT, "0"});
        glass(pr, "Clear Glass (почти прозрачная)", new String[]{Prefs.THEME, "0", Prefs.BLUR, "40",
                Prefs.PANEL, "10", Prefs.KEY_ALPHA, "35", Prefs.BORDER, "90", Prefs.SHINE, "75",
                Prefs.RADIUS, "16", Prefs.SWELL, "60", Prefs.SAT, "0"});
        glass(pr, "Frosted (матовое стекло)", new String[]{Prefs.BLUR, "100", Prefs.PANEL, "60",
                Prefs.KEY_ALPHA, "60", Prefs.BORDER, "50", Prefs.SHINE, "40", Prefs.RADIUS, "10",
                Prefs.SWELL, "30", Prefs.SAT, "0"});
        glass(pr, "Aurora (цветное)", new String[]{Prefs.THEME, "2", Prefs.BLUR, "80", Prefs.PANEL, "35",
                Prefs.KEY_ALPHA, "55", Prefs.BORDER, "85", Prefs.SHINE, "70", Prefs.SAT, "70",
                Prefs.HUE, "260", Prefs.RADIUS, "14", Prefs.SWELL, "60", Prefs.KEY_HUE, "260",
                Prefs.KEY_SAT, "40", Prefs.EDGE_HUE, "280", Prefs.EDGE_SAT, "50"});

        section("Стекло");
        slider("Размытие фона за клавиатурой", Prefs.BLUR, 0, 100);
        slider("Плотность подложки", Prefs.PANEL, 0, 100);
        slider("Плотность стекла клавиш", Prefs.KEY_ALPHA, 0, 100);
        slider("Яркость светлой кромки", Prefs.BORDER, 0, 100);
        slider("Блик на клавишах", Prefs.SHINE, 0, 100);
        slider("«Набухание» клавиши при нажатии", Prefs.SWELL, 0, 100);

        section("Цвета");
        colorGroup("Клавиши с буквами", Prefs.KEY_HUE, Prefs.KEY_SAT, Prefs.KEY_VAL);
        colorGroup("Функциональные клавиши (Shift, Backspace, !#1…)", Prefs.FUNC_HUE, Prefs.FUNC_SAT, Prefs.FUNC_VAL);
        colorGroup("Края (кромка) клавиш", Prefs.EDGE_HUE, Prefs.EDGE_SAT, Prefs.EDGE_VAL);
        slider("Толщина кромки (десятые доли dp)", Prefs.EDGE_W, 3, 40);
        colorGroup("Кромка нажатой клавиши", Prefs.PEDGE_HUE, Prefs.PEDGE_SAT, Prefs.PEDGE_VAL);
        colorGroup("Цвет клавиши при нажатии (заливка и всплеск света)", Prefs.PRESS_HUE, Prefs.PRESS_SAT, Prefs.PRESS_VAL);
        slider("Сила цвета при нажатии", Prefs.PRESS_A, 0, 100);
        colorGroup("Кнопка Enter", Prefs.ENTER_HUE, Prefs.ENTER_SAT, Prefs.ENTER_VAL);
        slider("Плотность заливки Enter", Prefs.ENTER_A, 0, 100);
        slider("Подложка клавиатуры: оттенок", Prefs.HUE, 0, 360);
        slider("Подложка клавиатуры: насыщенность (0 = без цвета)", Prefs.SAT, 0, 100);

        section("Анимации нажатия");
        LinearLayout an = new LinearLayout(this);
        an.setOrientation(LinearLayout.VERTICAL);
        root.addView(an);
        preset(an, "Мягкие", new String[]{Prefs.RIPPLE, "30", Prefs.PUSH, "15", Prefs.BOUNCE, "20",
                Prefs.SWELL, "30"});
        preset(an, "Жидкие", new String[]{Prefs.RIPPLE, "60", Prefs.PUSH, "40", Prefs.BOUNCE, "50",
                Prefs.SWELL, "55"});
        preset(an, "Желе (максимум)", new String[]{Prefs.RIPPLE, "85", Prefs.PUSH, "85", Prefs.BOUNCE, "95",
                Prefs.SWELL, "90"});
        slider("Всплеск света от пальца", Prefs.RIPPLE, 0, 100);
        slider("Соседние клавиши расходятся", Prefs.PUSH, 0, 100);
        slider("Пружина: отскок после нажатия", Prefs.BOUNCE, 0, 100);
        toggle("Волна клавиш при смене языка и страницы", Prefs.LANGANIM);

        section("Форма");
        slider("Скругление клавиш", Prefs.RADIUS, 0, 24);
        slider("Зазор между клавишами", Prefs.GAP, 0, 10);
        slider("Высота клавиатуры, %", Prefs.HEIGHT, 70, 140);
        slider("Размер букв", Prefs.TEXT, 14, 32);

        section("Тема");
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        final String[] tn = {"Авто (как в системе)", "Светлая", "Тёмная"};
        for (int i = 0; i < 3; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(tn[i]);
            rg.addView(rb);
        }
        root.addView(rg);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) {
                if (id >= 100) Prefs.sp(SettingsActivity.this).edit().putInt(Prefs.THEME, id - 100).apply();
            }
        });
        final RadioGroup rgf = rg;
        syncers.add(new Runnable() {
            @Override public void run() { rgf.check(100 + Prefs.get(SettingsActivity.this, Prefs.THEME)); }
        });

        section("Поведение");
        toggle("Вибро при нажатии", Prefs.HAPTIC);
        toggle("Ряд цифр сверху", Prefs.NUMROW);
        toggle("Буква над клавишей при нажатии", Prefs.POPUP);
        toggle("Маленькие подсказки на клавишах (ё, ъ, цифры)", Prefs.HINTS);
        toggle("Плотная подложка, если система выключила размытие", Prefs.FALLBACK);
        toggle("Яркий фон этой страницы (чтобы увидеть размытие)", Prefs.TESTBG);

        section("Языки");
        langBox("Русский", 1);
        langBox("English", 2);

        section("Своя картинка под клавишами");
        button("Выбрать картинку", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, REQ_IMAGE);
            }
        });
        button("Убрать картинку", new View.OnClickListener() {
            @Override public void onClick(View v) {
                new File(getFilesDir(), "bg.jpg").delete();
                bump();
            }
        });
        slider("Видимость картинки", Prefs.BG_ALPHA, 0, 100);

        section("Сброс");
        button("Вернуть настройки по умолчанию", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.sp(SettingsActivity.this).edit().clear().apply();
                sync();
            }
        });
        sync();
    }

    // ---------- элементы интерфейса ----------

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void title(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setPadding(0, 0, 0, dp(8));
        root.addView(t);
    }

    private void section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(18);
        t.setPadding(0, dp(22), 0, dp(6));
        root.addView(t);
    }

    private void text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 0, 0, dp(12));
        root.addView(t);
    }

    private void button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b);
    }

    private void preset(LinearLayout parent, String name, final String[] kv) {
        Button b = new Button(this);
        b.setText(name);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SharedPreferences.Editor e = Prefs.sp(SettingsActivity.this).edit();
                for (int i = 0; i + 1 < kv.length; i += 2) e.putInt(kv[i], Integer.parseInt(kv[i + 1]));
                e.apply();
                sync();
            }
        });
        parent.addView(b);
    }

    private void slider(final String title, final String key, final int min, final int max) {
        final TextView label = new TextView(this);
        label.setPadding(0, dp(8), 0, 0);
        root.addView(label);
        final SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        root.addView(sb);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                label.setText(title + ": " + (p + min));
                if (fromUser) {
                    Prefs.sp(SettingsActivity.this).edit().putInt(key, p + min).apply();
                    for (Runnable r : swatchers) r.run();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        syncers.add(new Runnable() {
            @Override public void run() { sb.setProgress(Prefs.get(SettingsActivity.this, key) - min); }
        });
    }

    private void toggle(String title, final String key) {
        final Switch sw = new Switch(this);
        sw.setText(title);
        sw.setPadding(0, dp(8), 0, dp(8));
        root.addView(sw);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) {
                Prefs.sp(SettingsActivity.this).edit().putInt(key, on ? 1 : 0).apply();
                if (key.equals(Prefs.TESTBG)) applyTestBg();
            }
        });
        syncers.add(new Runnable() {
            @Override public void run() { sw.setChecked(Prefs.get(SettingsActivity.this, key) == 1); }
        });
    }

    private void langBox(String name, final int bit) {
        final CheckBox cb = new CheckBox(this);
        cb.setText(name);
        root.addView(cb);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) {
                int m = Prefs.get(SettingsActivity.this, Prefs.LANGS);
                m = on ? (m | bit) : (m & ~bit);
                if ((m & 3) == 0) m = 3 ^ bit; // хотя бы один язык должен остаться
                Prefs.sp(SettingsActivity.this).edit().putInt(Prefs.LANGS, m).apply();
                sync();
            }
        });
        syncers.add(new Runnable() {
            @Override public void run() {
                cb.setChecked((Prefs.get(SettingsActivity.this, Prefs.LANGS) & bit) != 0);
            }
        });
    }

    private static final String[] COLOR_RESET = {
            Prefs.KEY_SAT, "0", Prefs.KEY_VAL, "100", Prefs.EDGE_SAT, "0", Prefs.EDGE_VAL, "100",
            Prefs.FUNC_HUE, "222", Prefs.FUNC_SAT, "25", Prefs.FUNC_VAL, "47", Prefs.EDGE_W, "11",
            Prefs.PRESS_HUE, "215", Prefs.PRESS_SAT, "35", Prefs.PRESS_VAL, "100", Prefs.PRESS_A, "40",
            Prefs.PEDGE_SAT, "0", Prefs.PEDGE_VAL, "100",
            Prefs.ENTER_HUE, "219", Prefs.ENTER_SAT, "76", Prefs.ENTER_VAL, "100", Prefs.ENTER_A, "60"};

    /** Пресет стекла: сначала сбрасывает цвета клавиш и кромки, потом применяет свои значения. */
    private void glass(LinearLayout parent, String name, String[] kv) {
        String[] all = new String[COLOR_RESET.length + kv.length];
        System.arraycopy(COLOR_RESET, 0, all, 0, COLOR_RESET.length);
        System.arraycopy(kv, 0, all, COLOR_RESET.length, kv.length);
        preset(parent, name, all);
    }

    private void sync() {
        for (Runnable r : syncers) r.run();
        for (Runnable r : swatchers) r.run();
        applyTestBg();
    }

    // ---------- цвета, статус размытия, фон для проверки ----------

    private void colorGroup(final String title, final String hk, final String sk, final String vk) {
        final TextView sw = new TextView(this);
        sw.setText(title);
        sw.setTextSize(16);
        sw.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(14);
        root.addView(sw, lp);
        slider("Оттенок", hk, 0, 360);
        slider("Насыщенность (0 = белый или серый)", sk, 0, 100);
        slider("Яркость", vk, 0, 100);
        swatchers.add(new Runnable() {
            @Override public void run() {
                int c = Color.HSVToColor(new float[]{
                        Prefs.get(SettingsActivity.this, hk),
                        Prefs.get(SettingsActivity.this, sk) / 100f,
                        Prefs.get(SettingsActivity.this, vk) / 100f});
                sw.setBackgroundColor(c);
                sw.setTextColor(Color.luminance(c) > 0.5f ? 0xFF000000 : 0xFFFFFFFF);
            }
        });
    }

    private void applyTestBg() {
        if (scroll == null) return;
        if (Prefs.get(this, Prefs.TESTBG) == 1) {
            scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFFFF7A7A, 0xFFFFC857, 0xFF6BCB77, 0xFF4D96FF, 0xFFB983FF}));
        } else {
            scroll.setBackground(null);
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener statusListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    updateStatus();
                }
            };

    private void updateStatus() {
        if (blurStatus == null) return;
        int st = getSharedPreferences("status", Context.MODE_PRIVATE).getInt("blur", 0);
        String t;
        switch (st) {
            case 1:
                t = "Размытие: система его включила, должно работать.";
                break;
            case 2:
                t = "Размытие: ВЫКЛЮЧЕНО системой. Проверь режим энергосбережения и в параметрах "
                        + "разработчика пункт «Отключить размытие окон».";
                break;
            case 3:
                t = "Размытие: нужен Android 12 или новее.";
                break;
            case 4:
                t = "Размытие: не удалось применить (ошибка окна клавиатуры).";
                break;
            default:
                t = "Размытие: нажми на поле проверки, чтобы клавиатура показалась, и тут появится статус.";
                break;
        }
        blurStatus.setText(t);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences("status", Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(statusListener);
        updateStatus();
    }

    @Override
    protected void onPause() {
        getSharedPreferences("status", Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(statusListener);
        super.onPause();
    }

    // ---------- картинка ----------

    private void bump() {
        // клавиатура следит за изменениями настроек, так она подхватит новую картинку
        Prefs.sp(this).edit().putInt("bgStamp", (int) (System.currentTimeMillis() / 1000)).apply();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMAGE || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            int sample = 1;
            while (o.outWidth / sample > 1600) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, o2);
            if (in != null) in.close();
            if (bmp == null) throw new IllegalStateException("decode");
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "bg.jpg"));
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            bump();
            Toast.makeText(this, "Картинка установлена", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть картинку", Toast.LENGTH_LONG).show();
        }
    }
}
