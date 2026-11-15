package com.liquidkb.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Все настройки хранятся как целые числа в SharedPreferences. */
final class Prefs {
    static final String FILE = "liquidkb";

    static final String BLUR = "blur";          // радиус размытия фона за клавиатурой
    static final String PANEL = "panel";        // непрозрачность подложки клавиатуры
    static final String KEY_ALPHA = "keyAlpha"; // непрозрачность стекла клавиш
    static final String RADIUS = "radius";      // скругление клавиш, dp
    static final String GAP = "gap";            // зазор между клавишами, dp
    static final String HEIGHT = "height";      // высота рядов, %
    static final String TEXT = "text";          // размер букв, sp
    static final String BORDER = "border";      // яркость светлой кромки
    static final String SHINE = "shine";        // блик на клавише
    static final String SWELL = "swell";        // «набухание» клавиши при нажатии
    static final String HUE = "hue";            // оттенок, 0..360
    static final String SAT = "sat";            // насыщенность оттенка, 0..100
    static final String THEME = "theme";        // 0 авто, 1 светлая, 2 тёмная
    static final String HAPTIC = "haptic";      // вибро
    static final String NUMROW = "numRow";      // ряд цифр
    static final String POPUP = "popup";        // всплывающая буква над клавишей
    static final String LANGS = "langs";        // битовая маска: 1 русский, 2 английский
    static final String BG_ALPHA = "bgAlpha";   // видимость своей картинки-фона
    static final String HINTS = "hints";        // маленькие подсказки (ё, ъ, цифры)
    static final String RIPPLE = "ripple";      // всплеск света от пальца
    static final String PUSH = "push";          // соседние клавиши расходятся от нажатой
    static final String BOUNCE = "bounce";      // пружина: отскок после нажатия
    static final String LANGANIM = "langAnim";  // волна при смене языка / страницы
    // цвета: оттенок 0..360, насыщенность 0..100, яркость 0..100
    static final String KEY_HUE = "keyHue", KEY_SAT = "keySat", KEY_VAL = "keyVal";
    static final String FUNC_HUE = "funcHue", FUNC_SAT = "funcSat", FUNC_VAL = "funcVal";
    static final String EDGE_HUE = "edgeHue", EDGE_SAT = "edgeSat", EDGE_VAL = "edgeVal";
    static final String EDGE_W = "edgeW";       // толщина кромки, десятые доли dp
    static final String FALLBACK = "fallback";  // плотная подложка, если система выключила размытие
    static final String TESTBG = "testBg";      // яркий фон страницы настроек для проверки размытия
    // цвет клавиши в момент нажатия (заливка и световое пятно) и его сила
    static final String PRESS_HUE = "pressHue", PRESS_SAT = "pressSat", PRESS_VAL = "pressVal";
    static final String PRESS_A = "pressA";
    // цвет кромки нажатой клавиши
    static final String PEDGE_HUE = "pedgeHue", PEDGE_SAT = "pedgeSat", PEDGE_VAL = "pedgeVal";
    // кнопка Enter: свой цвет, не зависит от подложки
    static final String ENTER_HUE = "enterHue", ENTER_SAT = "enterSat", ENTER_VAL = "enterVal";
    static final String ENTER_A = "enterA";

    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int def(String k) {
        switch (k) {
            case BLUR: return 70;
            case PANEL: return 35;
            case KEY_ALPHA: return 70;
            case RADIUS: return 12;
            case GAP: return 4;
            case HEIGHT: return 100;
            case TEXT: return 22;
            case BORDER: return 70;
            case SHINE: return 60;
            case SWELL: return 50;
            case HUE: return 210;
            case SAT: return 0;
            case THEME: return 0;
            case HAPTIC: return 1;
            case NUMROW: return 1;
            case POPUP: return 1;
            case LANGS: return 3;
            case BG_ALPHA: return 100;
            case HINTS: return 1;
            case RIPPLE: return 60;
            case PUSH: return 40;
            case BOUNCE: return 50;
            case LANGANIM: return 1;
            case KEY_HUE: return 210;
            case KEY_SAT: return 0;
            case KEY_VAL: return 100;
            case FUNC_HUE: return 222;
            case FUNC_SAT: return 25;
            case FUNC_VAL: return 47;
            case EDGE_HUE: return 210;
            case EDGE_SAT: return 0;
            case EDGE_VAL: return 100;
            case EDGE_W: return 11;
            case FALLBACK: return 1;
            case TESTBG: return 1;
            case PRESS_HUE: return 215;
            case PRESS_SAT: return 35;
            case PRESS_VAL: return 100;
            case PRESS_A: return 40;
            case PEDGE_HUE: return 210;
            case PEDGE_SAT: return 0;
            case PEDGE_VAL: return 100;
            case ENTER_HUE: return 219;
            case ENTER_SAT: return 76;
            case ENTER_VAL: return 100;
            case ENTER_A: return 60;
            default: return 0;
        }
    }

    static int get(Context c, String k) {
        return sp(c).getInt(k, def(k));
    }
}
