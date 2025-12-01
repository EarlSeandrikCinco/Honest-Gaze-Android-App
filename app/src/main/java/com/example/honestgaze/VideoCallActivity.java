package com.example.honestgaze;

import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class VideoCallActivity extends AppCompatActivity {

    private RtcEngine rtcEngine;
    private FrameLayout localContainer;
    private FrameLayout remoteContainer;

    private final String APP_ID = "ab0a820cff88487fbe29bfaec76cdb4d";
    private final String CHANNEL_NAME = "HonestGaze";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        localContainer = findViewById(R.id.local_video_view);
        remoteContainer = findViewById(R.id.remote_video_view);

        initAgoraEngine();
        setupLocalVideo();
        joinChannel();
    }

    private void initAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(getBaseContext(), APP_ID, rtcEventHandler);
            rtcEngine.enableVideo();
            rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

            rtcEngine.setVideoEncoderConfiguration(
                    new VideoEncoderConfiguration(
                            VideoEncoderConfiguration.VD_1280x720,
                            VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                            VideoEncoderConfiguration.STANDARD_BITRATE,
                            VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocalVideo() {
        // NEW WAY IN v4: create SurfaceView manually
        SurfaceView localView = new SurfaceView(this);
        localContainer.addView(localView);
        rtcEngine.setupLocalVideo(new VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.publishCameraTrack = true;
        options.autoSubscribeAudio = true;
        options.autoSubscribeVideo = true;
        rtcEngine.joinChannel(null, CHANNEL_NAME, 0, options);
    }

    private final IRtcEngineEventHandler rtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> remoteContainer.removeAllViews());
        }
    };

    private void setupRemoteVideo(int uid) {
        SurfaceView remoteView = new SurfaceView(this);
        remoteContainer.addView(remoteView);
        rtcEngine.setupRemoteVideo(new VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }
}
