package com.example.honestgaze;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

public class ReplayViewerActivity extends AppCompatActivity {

    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_replay_viewer);

        videoView = findViewById(R.id.videoView);

        String url = getIntent().getStringExtra("videoUrl");

        if (url == null) {
            Toast.makeText(this, "Replay not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri uri = Uri.parse(url);
        videoView.setVideoURI(uri);

        MediaController controller = new MediaController(this);
        videoView.setMediaController(controller);

        videoView.start();
    }
}
