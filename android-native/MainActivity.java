package com.studytube.app;

import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

/**
 * StudyTube: when a video is playing and you leave the app (Home button / gesture),
 * keep it going in a Picture-in-Picture pop-up instead of stopping.
 * The web app tells us when a video is playing via window.StudyTubeNative.setPlaying(bool)
 * and we tell it when PiP starts/ends via window.__stPip(bool).
 */
public class MainActivity extends BridgeActivity {
    private volatile boolean playing = false;

    public class PipBridge {
        @JavascriptInterface
        public void setPlaying(final boolean p) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    playing = p;
                    updatePipParams();
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            WebView wv = getBridge().getWebView();
            wv.addJavascriptInterface(new PipBridge(), "StudyTubeNative");
        } catch (Throwable ignored) { }
    }

    private PictureInPictureParams.Builder pipBuilder() {
        PictureInPictureParams.Builder b = new PictureInPictureParams.Builder()
                .setAspectRatio(new Rational(16, 9));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setAutoEnterEnabled(playing);
            b.setSeamlessResizeEnabled(true);
        }
        return b;
    }

    private void updatePipParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        try { setPictureInPictureParams(pipBuilder().build()); } catch (Throwable ignored) { }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter explicitly on every version (Android 12+ also auto-enters; this is the safety net).
        if (playing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isInPictureInPictureMode()) {
            try { enterPictureInPictureMode(pipBuilder().build()); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Keep the WebView (and the video inside it) running while the pop-up is showing.
        if (playing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            try { getBridge().getWebView().onResume(); getBridge().getWebView().resumeTimers(); } catch (Throwable ignored) { }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPip, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPip, newConfig);
        try {
            getBridge().getWebView().evaluateJavascript(
                    "window.__stPip&&window.__stPip(" + (isInPip ? "true" : "false") + ")", null);
            if (isInPip) { getBridge().getWebView().onResume(); getBridge().getWebView().resumeTimers(); }
        } catch (Throwable ignored) { }
    }
}
