package com.liquidkb.keyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.function.Consumer;

public class LiquidKeyboardService extends InputMethodService implements KeyboardView.Listener {

    private KeyboardView view;
    private Consumer<Boolean> blurListener;

    // живое обновление: меняешь ползунок в настройках — клавиатура перерисовывается сразу
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
                    if (view != null) {
                        view.reload();
                        applyWindowEffects();
                    }
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.sp(this).registerOnSharedPreferenceChangeListener(prefListener);
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                // система может включать и выключать размытие на лету (экономия энергии и т.п.)
                blurListener = new Consumer<Boolean>() {
                    @Override public void accept(Boolean on) { applyWindowEffects(); }
                };
                getSystemService(WindowManager.class)
                        .addCrossWindowBlurEnabledListener(getMainExecutor(), blurListener);
            } catch (Throwable ignored) {
                blurListener = null;
            }
        }
    }

    @Override
    public void onDestroy() {
        Prefs.sp(this).unregisterOnSharedPreferenceChangeListener(prefListener);
        if (Build.VERSION.SDK_INT >= 31 && blurListener != null) {
            try {
                getSystemService(WindowManager.class).removeCrossWindowBlurEnabledListener(blurListener);
            } catch (Throwable ignored) {
                // уже снят
            }
        }
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        view = new KeyboardView(this);
        view.setListener(this);
        return view;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (view == null) return;
        if (!restarting) view.resetPage();
        view.reload();
        applyWindowEffects();
        updateShift();
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        applyWindowEffects();
    }

    /**
     * Настоящее размытие того, что лежит за клавиатурой (Android 12+).
     * Окно клавиатуры прозрачное, а размытие под ним рисует система (setBackgroundBlurRadius).
     * Если система размытие выключила, подложка становится плотнее (настраивается).
     * Итог пишется в отдельный файл status, его показывает экран настроек.
     */
    private void applyWindowEffects() {
        int status = 3; // 1 работает, 2 выключено системой, 3 Android старше 12, 4 ошибка
        boolean blurOk = false;
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                Window w = getWindow() == null ? null : getWindow().getWindow();
                WindowManager wm = getSystemService(WindowManager.class);
                if (w != null && wm != null) {
                    w.setFormat(PixelFormat.TRANSLUCENT);
                    // почти невидимый фон: он задаёт область, под которой система рисует размытие
                    w.setBackgroundDrawable(new ColorDrawable(Color.argb(10, 0, 0, 0)));
                    w.setNavigationBarColor(Color.TRANSPARENT);
                    w.setNavigationBarContrastEnforced(false);
                    blurOk = wm.isCrossWindowBlurEnabled();
                    float density = getResources().getDisplayMetrics().density;
                    int radius = Math.min(150, Math.round(Prefs.get(this, Prefs.BLUR) * density * 0.5f));
                    w.setBackgroundBlurRadius(blurOk ? radius : 0);
                    status = blurOk ? 1 : 2;
                }
            } catch (Throwable t) {
                blurOk = false;
                status = 4;
            }
        }
        SharedPreferences st = getSharedPreferences("status", Context.MODE_PRIVATE);
        if (st.getInt("blur", 0) != status) st.edit().putInt("blur", status).apply();
        if (view != null) view.setNoBlur(!blurOk);
    }

    // ---------- ввод ----------

    @Override
    public void onText(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(s, 1);
        updateShift();
    }

    @Override
    public void onCode(int code) {
        InputConnection ic = getCurrentInputConnection();
        switch (code) {
            case Key.C_DEL:
                if (ic == null) return;
                CharSequence sel = ic.getSelectedText(0);
                if (sel != null && sel.length() > 0) ic.commitText("", 1);
                else sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
                updateShift();
                break;
            case Key.C_ENTER:
                enter(ic);
                break;
            case Key.C_CLIP:
                paste(ic);
                break;
            case Key.C_PICKER:
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
                break;
            default:
                break;
        }
    }

    private void enter(InputConnection ic) {
        if (ic == null) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        int action = ei == null ? EditorInfo.IME_ACTION_NONE : (ei.imeOptions & EditorInfo.IME_MASK_ACTION);
        boolean noAction = ei == null
                || (ei.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
                || action == EditorInfo.IME_ACTION_NONE
                || action == EditorInfo.IME_ACTION_UNSPECIFIED;
        if (noAction) sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        else ic.performEditorAction(action);
        updateShift();
    }

    private void paste(InputConnection ic) {
        if (ic == null) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null || !cm.hasPrimaryClip()) return;
        ClipData d = cm.getPrimaryClip();
        if (d == null || d.getItemCount() == 0) return;
        CharSequence t = d.getItemAt(0).coerceToText(this);
        if (t != null && t.length() > 0) ic.commitText(t, 1);
        updateShift();
    }

    /** Автозаглавная буква в начале предложения. */
    private void updateShift() {
        if (view == null || view.getShift() == 2) return;
        EditorInfo ei = getCurrentInputEditorInfo();
        InputConnection ic = getCurrentInputConnection();
        if (ei == null || ic == null || ei.inputType == InputType.TYPE_NULL) {
            view.setShift(0);
            return;
        }
        view.setShift(ic.getCursorCapsMode(ei.inputType) != 0 ? 1 : 0);
    }
}
