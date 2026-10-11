package com.brenninho.th06mobile;

import android.os.Bundle;
import android.os.Process;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.libsdl.app.SDLActivity;

public class GameActivity extends SDLActivity {
    public static final String PROCESS_SUFFIX = ":game";

    private TouchControlsView touchControls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mLayout == null) {
            return;
        }

        touchControls = new TouchControlsView(this);
        mLayout.addView(touchControls, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        touchControls.bringToFront();
        touchControls.requestApplyInsets();
    }

    private static boolean isPhysicalController(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
            || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            || (source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
            || (source & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD;
    }

    private void setTouchControlsVisible(boolean visible) {
        if (touchControls == null) {
            return;
        }
        int visibility = visible ? View.VISIBLE : View.GONE;
        if (touchControls.getVisibility() != visibility) {
            touchControls.releaseAll();
            touchControls.setVisibility(visibility);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean systemKey = keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_VOLUME_UP
            || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE;
        if (!systemKey && event.getDevice() != null && !event.getDevice().isVirtual()
            && isPhysicalController(event.getSource())) {
            setTouchControlsVisible(false);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (isPhysicalController(event.getSource())) {
            setTouchControlsVisible(false);
        }
        return super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            setTouchControlsVisible(true);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onPause() {
        if (touchControls != null) {
            touchControls.releaseAll();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            Process.killProcess(Process.myPid());
        }
    }
}
