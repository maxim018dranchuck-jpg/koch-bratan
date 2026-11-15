package com.liquidkb.keyboard;

import java.util.ArrayList;
import java.util.List;

/** Раскладки: как на Samsung — ряд цифр, три ряда букв, нижний ряд с !#1, глобусом, буфером и пробелом. */
final class Layouts {
    static final int RU = 0, EN = 1;
    static final String[] NAMES = {"Русский", "English"};

    private Layouts() {}

    static List<Key[]> build(int lang, boolean symbols, boolean numRow) {
        List<Key[]> rows = new ArrayList<>();
        if (numRow) rows.add(chars("1234567890", null, lang));

        if (symbols) {
            rows.add(chars("[]{}#%^*+=", null, lang));
            rows.add(chars("-/:;()$&@\"", null, lang));
            List<Key> r3 = new ArrayList<>();
            for (Key k : chars(".,?!'_~|\\", null, lang)) r3.add(k);
            r3.add(Key.fn(Key.C_DEL, "", 1.5f));
            rows.add(r3.toArray(new Key[0]));
        } else {
            // без ряда цифр цифры прячутся в долгое нажатие верхнего ряда
            String digits = numRow ? null : "1234567890";
            if (lang == RU) {
                rows.add(chars("йцукенгшщзх", digits, lang));
                rows.add(chars("фывапролджэ", null, lang));
                List<Key> r3 = new ArrayList<>();
                r3.add(Key.fn(Key.C_SHIFT, "", 1.4f));
                for (Key k : chars("ячсмитьбю", null, lang)) r3.add(k);
                r3.add(Key.fn(Key.C_DEL, "", 1.4f));
                rows.add(r3.toArray(new Key[0]));
            } else {
                rows.add(chars("qwertyuiop", digits, lang));
                List<Key> r2 = new ArrayList<>();
                r2.add(Key.gap(0.5f));
                for (Key k : chars("asdfghjkl", null, lang)) r2.add(k);
                r2.add(Key.gap(0.5f));
                rows.add(r2.toArray(new Key[0]));
                List<Key> r3 = new ArrayList<>();
                r3.add(Key.fn(Key.C_SHIFT, "", 1.4f));
                for (Key k : chars("zxcvbnm", null, lang)) r3.add(k);
                r3.add(Key.fn(Key.C_DEL, "", 1.4f));
                rows.add(r3.toArray(new Key[0]));
            }
        }

        rows.add(new Key[]{
                Key.fn(symbols ? Key.C_ABC : Key.C_SYM, symbols ? "ABC" : "!#1", 1.4f),
                Key.fn(Key.C_LANG, "", 1.1f),
                Key.fn(Key.C_CLIP, "", 1.1f),
                Key.space(NAMES[lang], 4.4f),
                Key.ch(".", ",!?-:;'\""),
                Key.fn(Key.C_ENTER, "", 1.5f)
        });
        return rows;
    }

    private static Key[] chars(String s, String digits, int lang) {
        Key[] out = new Key[s.length()];
        for (int i = 0; i < s.length(); i++) {
            String c = String.valueOf(s.charAt(i));
            String alt = accent(c, lang);
            if (digits != null) alt = digits.charAt(i) + (alt == null ? "" : alt);
            out[i] = Key.ch(c, alt);
        }
        return out;
    }

    private static String accent(String c, int lang) {
        if (lang == RU) {
            switch (c) {
                case "е": return "ё";
                case "ь": return "ъ";
                default: return null;
            }
        }
        switch (c) {
            case "a": return "àáâäãå";
            case "e": return "éèêë";
            case "i": return "íìîï";
            case "o": return "óòôöõ";
            case "u": return "úùûü";
            case "c": return "ç";
            case "n": return "ñ";
            case "s": return "ß";
            default: return null;
        }
    }
}
