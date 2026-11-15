package com.liquidkb.keyboard;

/** Одна клавиша: что на ней написано, что она делает и где лежит на экране. */
final class Key {
    static final int CHAR = 0, FUNC = 1, SPACE = 2, ENTER = 3, SPACER = 4;

    static final int C_SHIFT = -1, C_DEL = -2, C_ENTER = -3, C_SYM = -4, C_LANG = -5,
            C_CLIP = -6, C_ABC = -7, C_PICKER = -8, C_SPACE = 32;

    final String label;
    final String alt;   // символы для долгого нажатия
    final int code;
    final float w;      // вес ширины в ряду
    final int kind;

    // положение, считает KeyboardView
    float l, t, r, b;
    // нажатие как пружина: 0 = покой, 1 = нажата, может «перелетать» за эти значения
    float press, vel;
    boolean down;
    // смещение от соседней нажатой клавиши
    float dx, dy;
    // точка касания внутри клавиши и прогресс всплеска света (1 = закончился)
    float tx, ty, ripple = 1f;
    // появление при смене языка: от отрицательного (задержка) до 1 (готово)
    float appear = 1f;

    Key(String label, String alt, int code, float w, int kind) {
        this.label = label;
        this.alt = alt;
        this.code = code;
        this.w = w;
        this.kind = kind;
    }

    static Key ch(String s, String alt) {
        return new Key(s, alt, s.charAt(0), 1f, CHAR);
    }

    static Key fn(int code, String label, float w) {
        return new Key(label, null, code, w, code == C_ENTER ? ENTER : FUNC);
    }

    static Key space(String label, float w) {
        return new Key(label, null, C_SPACE, w, SPACE);
    }

    static Key gap(float w) {
        return new Key("", null, 0, w, SPACER);
    }

    boolean contains(float x, float y) {
        return x >= l && x < r && y >= t && y < b;
    }

    float cx() { return (l + r) / 2f; }
    float cy() { return (t + b) / 2f; }
}
