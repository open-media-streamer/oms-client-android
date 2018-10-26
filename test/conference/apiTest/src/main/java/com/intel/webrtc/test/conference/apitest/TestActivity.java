package com.intel.webrtc.test.conference.apitest;

import android.app.Activity;
import android.os.Bundle;

import android.os.Process;
import android.util.Log;

import com.intel.webrtc.base.ContextInitialization;

import org.webrtc.EglBase;

public class TestActivity extends Activity {
    private final static String TAG = "ics_conference_test";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        Log.d(TAG, "PID=" + Process.myPid() + "=");
        initConferenceClient();
    }

    private void initConferenceClient() {
        EglBase rootEglBase = EglBase.create();
        ContextInitialization.create().setApplicationContext(this)
                .setCodecHardwareAccelerationEnabled(true)
                .setVideoHardwareAccelerationOptions(rootEglBase.getEglBaseContext(),
                        rootEglBase.getEglBaseContext())
                .initialize();
    }

}
