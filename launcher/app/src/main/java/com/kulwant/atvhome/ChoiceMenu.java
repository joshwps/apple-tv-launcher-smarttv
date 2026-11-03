package com.kulwant.atvhome;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Menu de opções: centralizado (padrão) ou ancorado abaixo de uma pill. */
final class ChoiceMenu {

    static final class Choice {
        final String label;
        final Runnable action;
        final boolean keepOpen;

        Choice(String label, Runnable action) { this(label, action, false); }
        Choice(String label, Runnable action, boolean keepOpen) {
            this.label = label; this.action = action; this.keepOpen = keepOpen;
        }
    }

    final FrameLayout panel;
    private final LinearLayout list;
    private final Context c;
    private final List<Choice> choices = new ArrayList<>();
    private final List<TextView> rows = new ArrayList<>();
    private int sel;

    ChoiceMenu(Context c) {
        this.c = c;
        panel = new FrameLayout(c);
        panel.setBackground(Ui.rounded(0xF01C1C1E, Ui.dp(c, 18), Ui.dp(c, 1), 0x24FFFFFF));
        panel.setPadding(Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8));
        panel.setElevation(Ui.dp(c, 16));
        panel.setVisibility(View.GONE);
        list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        panel.addView(list);
    }

    boolean isOpen() { return panel.getVisibility() == View.VISIBLE; }

    /** Menu centralizado (context menus do long-press). */
    void open(String title, List<Choice> cs) {
        build(title, cs);
        panel.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        panel.setPivotY(0.5f);
        panel.setAlpha(0f);
        panel.setScaleX(0.96f);
        panel.setScaleY(0.96f);
        panel.setVisibility(View.VISIBLE);
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start();
    }

    /**
     * Menu ancorado abaixo de uma View (pill do topo). Mantém a largura natural
     * (260dp + padding), só ajusta a posição: alinha o canto direito do menu
     * com o canto direito da âncora e limita para não sair da tela.
     */
    void openAt(View anchor, String title, List<Choice> cs) {
        build(title, cs);

        int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        int pillRight = loc[0] + anchor.getWidth();
        int pillBottom = loc[1] + anchor.getHeight();

        View rootView = anchor.getRootView();
        int screenW = rootView.getWidth();
        int screenH = rootView.getHeight();
        if (screenW <= 0) screenW = anchor.getResources().getDisplayMetrics().widthPixels;
        if (screenH <= 0) screenH = anchor.getResources().getDisplayMetrics().heightPixels;

        panel.measure(
                View.MeasureSpec.makeMeasureSpec(screenW, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(screenH, View.MeasureSpec.AT_MOST));
        int menuW = panel.getMeasuredWidth();
        int menuH = panel.getMeasuredHeight();
        if (menuW <= 0) menuW = Ui.dp(c, 276);
        if (menuH <= 0) menuH = Ui.dp(c, 360);

        int left = pillRight - menuW;
        if (left < 0) left = 0;
        if (left + menuW > screenW) left = screenW - menuW;

        int top = pillBottom + Ui.dp(c, 8);
        int maxTop = screenH - menuH - Ui.dp(c, 8);
        if (maxTop > 0 && top > maxTop) top = maxTop;

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                menuW, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = left;
        lp.topMargin = top;
        panel.setLayoutParams(lp);
        panel.setPivotY(0f);
        panel.setAlpha(0f);
        panel.setScaleX(1f);
        panel.setScaleY(0.94f);
        panel.setVisibility(View.VISIBLE);
        panel.animate().alpha(1f).scaleY(1f).setDuration(160).start();
    }

    private void build(String title, List<Choice> cs) {
        choices.clear();
        choices.addAll(cs);
        list.removeAllViews();
        rows.clear();
        TextView t = Ui.text(c, 500, 13, 0x99FFFFFF);
        t.setText(title);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 14), 0);
        list.addView(t, new LinearLayout.LayoutParams(Ui.dp(c, 260), Ui.dp(c, 34)));
        for (Choice ch : choices) {
            TextView r = Ui.text(c, 400, 15, 0xFFFFFFFF);
            r.setText(ch.label);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.dp(c, 14), 0, Ui.dp(c, 14), 0);
            list.addView(r, new LinearLayout.LayoutParams(Ui.dp(c, 260), Ui.dp(c, 40)));
            rows.add(r);
        }
        sel = 0;
        paint();
    }

    void close() {
        panel.animate().alpha(0f).setDuration(110).withEndAction(() -> panel.setVisibility(View.GONE)).start();
    }

    void move(int d) {
        int n = sel + d;
        if (n < 0 || n >= choices.size()) return;
        sel = n;
        paint();
    }

    Choice selected() { return choices.isEmpty() ? null : choices.get(sel); }

    private void paint() {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setBackground(i == sel
                    ? Ui.rounded(0x2EFFFFFF, Ui.dp(c, 12), Ui.dp(c, 1), 0x40FFFFFF) : null);
        }
    }
}
