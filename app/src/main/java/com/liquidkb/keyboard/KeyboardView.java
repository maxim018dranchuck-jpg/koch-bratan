package com.liquidkb.keyboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Сама клавиатура: раскладка, мультитач, долгое нажатие, повтор Backspace, анимация нажатия. */
final class KeyboardView extends View {

    interface Listener {
        void onText(String s);
        void onCode(int code); // C_DEL, C_ENTER, C_CLIP, C_PICKER
    }

    private final KeyPainter painter = new KeyPainter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SparseArray<Key> active = new SparseArray<>();
    private Listener listener;

    private List<Key[]> rows = new ArrayList<>();
    private int lang = Layouts.RU;
    private int langMask = 3;
    private boolean symbols, numRow, haptic;
    private int shift;
    private long lastShiftTap;
    private float heightScale = 1f, gap, rowH;
    private float pushAmt, bounceAmt;
    private boolean langAnim;
    private long lastFrame, flashStart = -1;
    private String flashText;
    private final List<Key> pressedKeys = new ArrayList<>();

    private Bitmap bg;
    private long bgStamp = -2;

    private Key lpKey;
    private int lpPointer = -1;
    private Key altKey;
    private String altChars;
    private int altSel, altPointer = -1;
    private Key repeatKey;

    private final Runnable repeat = new Runnable() {
        @Override public void run() {
            if (repeatKey == null) return;
            emit(Key.C_DEL);
            handler.postDelayed(this, 45);
        }
    };

    private final Runnable longPress = new Runnable() {
        @Override public void run() {
            if (lpKey == null || !lpKey.down) return;
            if (lpKey.code == Key.C_SPACE || lpKey.code == Key.C_LANG) {
                active.remove(lpPointer);
                lpKey.down = false;
                lpKey = null;
                lpPointer = -1;
                emit(Key.C_PICKER);
            } else if (lpKey.alt != null) {
                altKey = lpKey;
                altPointer = lpPointer;
                altChars = shifted(altKey.alt);
                float[] g = painter.altGeom(altKey, altChars.length(), getWidth());
                altSel = Math.max(0, Math.min(altChars.length() - 1,
                        (int) ((altKey.cx() - g[0]) / g[1])));
                buzz();
            }
            invalidate();
        }
    };

    KeyboardView(Context c) {
        super(c);
        reload();
    }

    void setListener(Listener l) { listener = l; }

    int getShift() { return shift; }

    void setNoBlur(boolean b) {
        if (painter.noBlur != b) {
            painter.noBlur = b;
            invalidate();
        }
    }

    void setShift(int s) {
        if (shift != s) {
            shift = s;
            invalidate();
        }
    }

    /** Новое поле ввода — возвращаемся на буквы. */
    void resetPage() {
        if (symbols) {
            symbols = false;
            rebuild();
        }
    }

    /** Читает настройки и перестраивает клавиатуру. */
    void reload() {
        Context c = getContext();
        painter.reload(c);
        numRow = Prefs.get(c, Prefs.NUMROW) == 1;
        haptic = Prefs.get(c, Prefs.HAPTIC) == 1;
        heightScale = Prefs.get(c, Prefs.HEIGHT) / 100f;
        gap = painter.dp(Prefs.get(c, Prefs.GAP));
        pushAmt = Prefs.get(c, Prefs.PUSH) / 100f;
        bounceAmt = Prefs.get(c, Prefs.BOUNCE) / 100f;
        langAnim = Prefs.get(c, Prefs.LANGANIM) == 1;
        langMask = Prefs.get(c, Prefs.LANGS) & 3;
        if (langMask == 0) langMask = 3;
        if ((langMask & (1 << lang)) == 0) lang = (langMask & 1) != 0 ? Layouts.RU : Layouts.EN;
        loadBg();
        rebuild();
    }

    private void loadBg() {
        File f = new File(getContext().getFilesDir(), "bg.jpg");
        long st = f.exists() ? f.lastModified() : -1;
        if (st == bgStamp) return;
        bgStamp = st;
        bg = null;
        if (st >= 0) {
            try {
                bg = BitmapFactory.decodeFile(f.getPath());
            } catch (Throwable t) {
                bg = null;
            }
        }
    }

    private void rebuild() {
        rows = Layouts.build(lang, symbols, numRow);
        layoutKeys();
        requestLayout();
        invalidate();
    }

    private void nextLang() {
        for (int i = 1; i <= 2; i++) {
            int n = (lang + i) % 2;
            if ((langMask & (1 << n)) != 0) {
                lang = n;
                break;
            }
        }
        rebuild();
        flashText = Layouts.NAMES[lang];
        flashStart = SystemClock.uptimeMillis();
        startWave();
    }

    /** Волна: клавиши по очереди «выпрыгивают» слева направо. */
    private void startWave() {
        if (!langAnim) return;
        int w = Math.max(1, getWidth());
        for (int i = 0; i < rows.size(); i++) {
            for (Key k : rows.get(i)) {
                // задержка в секундах переводится в долю длительности (0.30 с)
                k.appear = -(0.14f * (k.cx() / w) + 0.035f * i) / 0.30f;
                k.press = 0f;
                k.vel = 0f;
            }
        }
        invalidate();
    }

    private Key spaceKey() {
        if (rows.isEmpty()) return null;
        for (Key k : rows.get(rows.size() - 1)) {
            if (k.kind == Key.SPACE) return k;
        }
        return null;
    }

    // ---------- размеры ----------

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        rowH = painter.dp(54) * heightScale;
        int h = Math.round(painter.dp(6) + rows.size() * rowH + painter.dp(8));
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        layoutKeys();
    }

    private void layoutKeys() {
        int w = getWidth();
        if (w == 0) return;
        rowH = painter.dp(54) * heightScale;
        float padX = painter.dp(4), padTop = painter.dp(6);
        for (int i = 0; i < rows.size(); i++) {
            Key[] row = rows.get(i);
            float sum = 0;
            for (Key k : row) sum += k.w;
            float avail = w - 2 * padX - gap * (row.length - 1);
            float x = padX;
            for (Key k : row) {
                float kw = avail * k.w / sum;
                k.l = x;
                k.r = x + kw;
                k.t = padTop + i * rowH + gap / 2f;
                k.b = padTop + (i + 1) * rowH - gap / 2f;
                x += kw + gap;
            }
        }
    }

    // ---------- рисование ----------

    private String labelFor(Key k) {
        if (k.kind == Key.SPACE) return Layouts.NAMES[lang];
        if (k.kind == Key.CHAR && shift > 0) return shifted(k.label);
        return k.label;
    }

    private static String shifted(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            String ch = s.substring(i, i + 1);
            String up = ch.toUpperCase();
            sb.append(up.length() == 1 ? up : ch);
        }
        return sb.toString();
    }

    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        boolean animating = step(dt);

        painter.drawPanel(c, getWidth(), getHeight(), bg);
        // сначала спокойные клавиши, потом нажатые — чтобы «набухшая» клавиша была сверху
        for (int pass = 0; pass < 2; pass++) {
            for (Key[] row : rows) {
                for (Key k : row) {
                    boolean raised = Math.abs(k.press) > 0.01f || k.appear < 1f;
                    if (raised == (pass == 1)) {
                        painter.drawKey(c, k, labelFor(k), shift, rowH);
                    }
                }
            }
        }
        if (painter.popup && altKey == null) {
            for (Key[] row : rows) {
                for (Key k : row) {
                    if (k.down && k.kind == Key.CHAR) painter.drawBubble(c, k, labelFor(k), getWidth());
                }
            }
        }
        if (altKey != null) painter.drawAlt(c, altKey, altChars, altSel, getWidth());

        // плашка с названием языка над пробелом
        if (flashText != null) {
            long el = now - flashStart;
            Key sp = spaceKey();
            if (el < 900 && sp != null) {
                float al = Math.min(1f, el / 120f) * (el < 650 ? 1f : 1f - (el - 650) / 250f);
                painter.drawPill(c, sp.cx(), sp.t - painter.dp(28), flashText, al);
                animating = true;
            } else {
                flashText = null;
            }
        }
        if (animating) postInvalidateOnAnimation();
        else lastFrame = 0;
    }

    /** Шаг физики: пружины клавиш, всплеск света, волна появления, расталкивание соседей. */
    private boolean step(float dt) {
        boolean busy = false;
        float zeta = 0.95f - 0.65f * bounceAmt; // 0.95 — без отскока, 0.30 — желе
        float stiff = 520f;
        float damp = 2f * (float) Math.sqrt(stiff) * zeta;
        pressedKeys.clear();
        for (Key[] row : rows) {
            for (Key k : row) {
                float target = k.down ? 1f : 0f;
                k.vel += (stiff * (target - k.press) - damp * k.vel) * dt;
                k.press += k.vel * dt;
                if (Math.abs(target - k.press) < 0.003f && Math.abs(k.vel) < 0.05f) {
                    k.press = target;
                    k.vel = 0f;
                } else {
                    busy = true;
                }
                if (k.ripple < 1f) {
                    k.ripple = Math.min(1f, k.ripple + dt / 0.45f);
                    busy = true;
                }
                if (k.appear < 1f) {
                    k.appear = Math.min(1f, k.appear + dt / 0.30f);
                    busy = true;
                }
                k.dx = 0f;
                k.dy = 0f;
                if (Math.abs(k.press) > 0.01f && k.kind == Key.CHAR) pressedKeys.add(k);
            }
        }
        if (pushAmt > 0f && !pressedKeys.isEmpty()) {
            float push = painter.dp(4) * pushAmt;
            for (Key p : pressedKeys) {
                float reach = (p.r - p.l) * 2.2f;
                for (Key[] row : rows) {
                    for (Key o : row) {
                        if (o == p || o.kind == Key.SPACER) continue;
                        float ddx = o.cx() - p.cx(), ddy = o.cy() - p.cy();
                        float d = (float) Math.sqrt(ddx * ddx + ddy * ddy);
                        if (d < 1f || d >= reach) continue;
                        float f = 1f - d / reach;
                        float m = push * f * f * p.press / d;
                        o.dx += ddx * m;
                        o.dy += ddy * m;
                    }
                }
            }
        }
        return busy;
    }

    // ---------- касания ----------

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int idx = e.getActionIndex();
        int id = e.getPointerId(idx);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                Key k = findKey(e.getX(idx), e.getY(idx));
                if (k != null) press(id, k, e.getX(idx), e.getY(idx));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) {
                    move(e.getPointerId(i), e.getX(i), e.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                release(id, false);
                break;
            case MotionEvent.ACTION_CANCEL:
                for (int i = active.size() - 1; i >= 0; i--) release(active.keyAt(i), true);
                break;
            default:
                break;
        }
        invalidate();
        return true;
    }

    private Key findKey(float x, float y) {
        if (rows.isEmpty()) return null;
        int ri = (int) ((y - painter.dp(6)) / rowH);
        ri = Math.max(0, Math.min(rows.size() - 1, ri));
        Key best = null;
        float bd = Float.MAX_VALUE;
        for (Key k : rows.get(ri)) {
            if (k.kind == Key.SPACER) continue;
            float d = x < k.l ? k.l - x : (x > k.r ? x - k.r : 0f);
            if (d < bd) {
                bd = d;
                best = k;
            }
        }
        return best;
    }

    private void press(int id, Key k, float x, float y) {
        active.put(id, k);
        k.down = true;
        k.tx = x - k.l;
        k.ty = y - k.t;
        k.ripple = 0f;
        buzz();
        if (id == lpPointer) cancelLp();
        if (k.code == Key.C_DEL) {
            emit(Key.C_DEL);
            repeatKey = k;
            handler.removeCallbacks(repeat);
            handler.postDelayed(repeat, 380);
        } else if (k.alt != null || k.code == Key.C_SPACE || k.code == Key.C_LANG) {
            lpKey = k;
            lpPointer = id;
            handler.removeCallbacks(longPress);
            handler.postDelayed(longPress, 400);
        }
    }

    private void move(int id, float x, float y) {
        if (id == altPointer && altKey != null) {
            float[] g = painter.altGeom(altKey, altChars.length(), getWidth());
            int s = (int) ((x - g[0]) / g[1]);
            altSel = Math.max(0, Math.min(altChars.length() - 1, s));
            return;
        }
        Key cur = active.get(id);
        if (cur == null || cur.code == Key.C_DEL) return;
        Key nk = findKey(x, y);
        if (nk != null && nk != cur) {
            cur.down = false;
            if (id == lpPointer) cancelLp();
            press(id, nk, x, y);
        }
    }

    private void release(int id, boolean cancel) {
        Key k = active.get(id);
        active.remove(id);
        if (k == null) return;
        k.down = false;
        if (id == lpPointer) cancelLp();
        if (k == repeatKey) {
            handler.removeCallbacks(repeat);
            repeatKey = null;
        }
        if (id == altPointer && altKey != null) {
            String picked = cancel ? null : altChars.substring(altSel, altSel + 1);
            altKey = null;
            altChars = null;
            altPointer = -1;
            if (picked != null && listener != null) {
                if (shift == 1) shift = 0;
                listener.onText(picked);
            }
            return;
        }
        if (cancel || k.code == Key.C_DEL) return;
        fire(k);
    }

    private void cancelLp() {
        handler.removeCallbacks(longPress);
        lpKey = null;
        lpPointer = -1;
    }

    private void fire(Key k) {
        switch (k.code) {
            case Key.C_SHIFT:
                long now = SystemClock.uptimeMillis();
                if (shift == 2) shift = 0;
                else if (shift == 1 && now - lastShiftTap < 350) shift = 2;
                else shift = shift == 0 ? 1 : 0;
                lastShiftTap = now;
                break;
            case Key.C_SYM:
                symbols = true;
                rebuild();
                startWave();
                break;
            case Key.C_ABC:
                symbols = false;
                rebuild();
                startWave();
                break;
            case Key.C_LANG:
                // один язык включён — глобус открывает выбор клавиатуры
                if (langMask == 3) nextLang();
                else emit(Key.C_PICKER);
                break;
            case Key.C_ENTER:
            case Key.C_CLIP:
                emit(k.code);
                break;
            default:
                if (listener == null) break;
                if (k.kind == Key.SPACE) {
                    listener.onText(" ");
                } else {
                    String t = shift > 0 ? shifted(k.label) : k.label;
                    if (shift == 1) shift = 0; // до onText: сервис может снова включить автозаглавную
                    listener.onText(t);
                }
                break;
        }
    }

    private void emit(int code) {
        if (listener != null) listener.onCode(code);
    }

    private void buzz() {
        if (haptic) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
