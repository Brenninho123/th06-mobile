package com.brenninho.th06mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.util.SparseArray;
import android.view.DisplayCutout;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

public class TouchControlsView extends View {
    public static final int BUTTON_SHOOT = 1;
    public static final int BUTTON_BOMB = 1 << 1;
    public static final int BUTTON_FOCUS = 1 << 2;
    public static final int BUTTON_MENU = 1 << 3;
    public static final int BUTTON_UP = 1 << 4;
    public static final int BUTTON_DOWN = 1 << 5;
    public static final int BUTTON_LEFT = 1 << 6;
    public static final int BUTTON_RIGHT = 1 << 7;
    public static final int BUTTON_SKIP = 1 << 8;

    private static final float HIT_SCALE = 1.25f;
    private static final float DEAD_ZONE_SCALE = 0.28f;

    private static native void nativeSetButtons(int buttons);

    private static final class VirtualButton {
        final int mask;
        final String label;
        final boolean small;
        float x;
        float y;
        float radius;

        VirtualButton(int mask, String label, boolean small) {
            this.mask = mask;
            this.label = label;
            this.small = small;
        }

        boolean hit(float px, float py) {
            float dx = px - x;
            float dy = py - y;
            float r = radius * HIT_SCALE;
            return dx * dx + dy * dy <= r * r;
        }
    }

    private static final class Stick {
        int pointerId = -1;
        float originX;
        float originY;
        float knobX;
        float knobY;
    }

    private final VirtualButton[] buttons = {
        new VirtualButton(BUTTON_SHOOT, "Z", false),
        new VirtualButton(BUTTON_BOMB, "X", false),
        new VirtualButton(BUTTON_FOCUS, "⇧", false),
        new VirtualButton(BUTTON_MENU, "II", true),
        new VirtualButton(BUTTON_SKIP, "»", true)
    };

    private final Stick stick = new Stick();
    private final SparseArray<float[]> pointers = new SparseArray<>();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect textBounds = new Rect();
    private final Rect safeInsets = new Rect();

    private float stickRadius;
    private float stickHomeX;
    private float stickHomeY;
    private int currentMask;
    private boolean nativeAvailable = true;

    public TouchControlsView(Context context) {
        super(context);
        setClickable(true);
        setHapticFeedbackEnabled(true);

        strokePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.WHITE);
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        safeInsets.setEmpty();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                safeInsets.set(cutout.getSafeInsetLeft(), cutout.getSafeInsetTop(), cutout.getSafeInsetRight(),
                    cutout.getSafeInsetBottom());
            }
        }
        layoutControls(getWidth(), getHeight());
        invalidate();
        return super.onApplyWindowInsets(insets);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutControls(w, h);
    }

    private void layoutControls(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float left = safeInsets.left;
        float right = width - safeInsets.right;
        float top = safeInsets.top;
        float bottom = height - safeInsets.bottom;
        float usableHeight = bottom - top;

        float barWidth = Math.max(0f, ((right - left) - usableHeight * 4f / 3f) / 2f);
        float radius = Math.max(usableHeight * 0.075f, Math.min(usableHeight * 0.11f, barWidth * 0.3f));
        float smallRadius = radius * 0.6f;

        VirtualButton shoot = buttons[0];
        VirtualButton bomb = buttons[1];
        VirtualButton focus = buttons[2];
        VirtualButton menu = buttons[3];
        VirtualButton skip = buttons[4];

        shoot.radius = radius;
        shoot.x = right - radius * 1.5f;
        shoot.y = bottom - radius * 1.7f;

        focus.radius = radius;
        focus.x = shoot.x - radius * 2.4f;
        focus.y = shoot.y + radius * 0.35f;

        bomb.radius = radius;
        bomb.x = shoot.x - radius * 0.35f;
        bomb.y = shoot.y - radius * 2.4f;

        menu.radius = smallRadius;
        menu.x = right - smallRadius * 1.6f;
        menu.y = top + smallRadius * 1.6f;

        skip.radius = smallRadius;
        skip.x = left + smallRadius * 1.6f;
        skip.y = top + smallRadius * 1.6f;

        stickRadius = radius * 1.35f;
        stickHomeX = left + stickRadius * 1.3f;
        stickHomeY = bottom - stickRadius * 1.3f;

        textPaint.setTextSize(radius * 0.7f);
        strokePaint.setStrokeWidth(Math.max(2f, radius * 0.06f));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerDown(pointerId, event.getX(index), event.getY(index));
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    pointerMove(event.getPointerId(i), event.getX(i), event.getY(i));
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                pointerUp(pointerId);
                break;
            case MotionEvent.ACTION_CANCEL:
                pointers.clear();
                stick.pointerId = -1;
                break;
            default:
                break;
        }

        updateMask();
        return true;
    }

    private boolean hitsAnyButton(float x, float y) {
        for (VirtualButton button : buttons) {
            if (button.hit(x, y)) {
                return true;
            }
        }
        return false;
    }

    private void pointerDown(int id, float x, float y) {
        pointers.put(id, new float[] {x, y});

        if (stick.pointerId == -1 && x < getWidth() * 0.5f && !hitsAnyButton(x, y)) {
            stick.pointerId = id;
            stick.originX = x;
            stick.originY = y;
            stick.knobX = x;
            stick.knobY = y;
        }
    }

    private void pointerMove(int id, float x, float y) {
        float[] position = pointers.get(id);
        if (position != null) {
            position[0] = x;
            position[1] = y;
        }

        if (id == stick.pointerId) {
            float dx = x - stick.originX;
            float dy = y - stick.originY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > stickRadius) {
                stick.originX = x - dx / distance * stickRadius;
                stick.originY = y - dy / distance * stickRadius;
            }
            stick.knobX = x;
            stick.knobY = y;
        }
    }

    private void pointerUp(int id) {
        pointers.remove(id);
        if (id == stick.pointerId) {
            stick.pointerId = -1;
        }
    }

    private int stickMask() {
        if (stick.pointerId == -1) {
            return 0;
        }

        float dx = stick.knobX - stick.originX;
        float dy = stick.knobY - stick.originY;
        if (dx * dx + dy * dy < (stickRadius * DEAD_ZONE_SCALE) * (stickRadius * DEAD_ZONE_SCALE)) {
            return 0;
        }

        double angle = Math.toDegrees(Math.atan2(-dy, dx));
        if (angle < 0) {
            angle += 360;
        }

        int sector = (int) Math.floor((angle + 22.5) / 45.0) % 8;
        switch (sector) {
            case 0:
                return BUTTON_RIGHT;
            case 1:
                return BUTTON_RIGHT | BUTTON_UP;
            case 2:
                return BUTTON_UP;
            case 3:
                return BUTTON_LEFT | BUTTON_UP;
            case 4:
                return BUTTON_LEFT;
            case 5:
                return BUTTON_LEFT | BUTTON_DOWN;
            case 6:
                return BUTTON_DOWN;
            default:
                return BUTTON_RIGHT | BUTTON_DOWN;
        }
    }

    private void updateMask() {
        int mask = stickMask();

        for (int i = 0; i < pointers.size(); i++) {
            if (pointers.keyAt(i) == stick.pointerId) {
                continue;
            }
            float[] position = pointers.valueAt(i);
            for (VirtualButton button : buttons) {
                if (button.hit(position[0], position[1])) {
                    mask |= button.mask;
                }
            }
        }

        int buttonBits = BUTTON_SHOOT | BUTTON_BOMB | BUTTON_FOCUS | BUTTON_MENU | BUTTON_SKIP;
        if ((mask & ~currentMask & buttonBits) != 0) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }

        if (mask != currentMask) {
            currentMask = mask;
            sendButtons(mask);
            invalidate();
        }
    }

    public void releaseAll() {
        pointers.clear();
        stick.pointerId = -1;
        currentMask = 0;
        sendButtons(0);
        invalidate();
    }

    private void sendButtons(int mask) {
        if (!nativeAvailable) {
            return;
        }
        try {
            nativeSetButtons(mask);
        } catch (UnsatisfiedLinkError e) {
            nativeAvailable = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        boolean stickActive = stick.pointerId != -1;
        float baseX = stickActive ? stick.originX : stickHomeX;
        float baseY = stickActive ? stick.originY : stickHomeY;

        fillPaint.setColor(Color.argb(stickActive ? 70 : 40, 255, 255, 255));
        canvas.drawCircle(baseX, baseY, stickRadius, fillPaint);
        strokePaint.setColor(Color.argb(stickActive ? 160 : 90, 255, 255, 255));
        canvas.drawCircle(baseX, baseY, stickRadius, strokePaint);

        float knobX = stickActive ? stick.knobX : baseX;
        float knobY = stickActive ? stick.knobY : baseY;
        float dx = knobX - baseX;
        float dy = knobY - baseY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > stickRadius) {
            knobX = baseX + dx / distance * stickRadius;
            knobY = baseY + dy / distance * stickRadius;
        }
        fillPaint.setColor(Color.argb(stickActive ? 170 : 90, 200, 16, 46));
        canvas.drawCircle(knobX, knobY, stickRadius * 0.45f, fillPaint);

        for (VirtualButton button : buttons) {
            boolean pressed = (currentMask & button.mask) != 0;
            fillPaint.setColor(pressed ? Color.argb(170, 200, 16, 46) : Color.argb(60, 255, 255, 255));
            canvas.drawCircle(button.x, button.y, button.radius, fillPaint);
            strokePaint.setColor(Color.argb(pressed ? 220 : 110, 255, 255, 255));
            canvas.drawCircle(button.x, button.y, button.radius, strokePaint);

            float previousSize = textPaint.getTextSize();
            if (button.small) {
                textPaint.setTextSize(previousSize * 0.65f);
            }
            textPaint.setAlpha(pressed ? 255 : 190);
            textPaint.getTextBounds(button.label, 0, button.label.length(), textBounds);
            canvas.drawText(button.label, button.x, button.y + textBounds.height() / 2f, textPaint);
            textPaint.setTextSize(previousSize);
        }
    }
}
