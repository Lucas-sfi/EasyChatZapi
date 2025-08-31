package com.example.easychat;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;

public class FullSizeImageActivity extends AppCompatActivity {

    ImageView fullSizeImageView;
    ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_size_image);

        fullSizeImageView = findViewById(R.id.full_size_image_view);
        backButton = findViewById(R.id.back_btn);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(Uri.parse(imageUrl)).into(fullSizeImageView);
        }

        backButton.setOnClickListener(v -> onBackPressed());
    }
}