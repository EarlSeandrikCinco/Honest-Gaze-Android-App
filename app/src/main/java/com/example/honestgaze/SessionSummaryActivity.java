package com.example.honestgaze;

import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;

public class SessionSummaryActivity extends AppCompatActivity {

    private Button btnExportCsv;
    private TextView tvStudentTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary);

        btnExportCsv = findViewById(R.id.btnExportCsv);
        tvStudentTable = findViewById(R.id.tvStudentTable);

        // Placeholder table for students
        String placeholderTable = "Name      Warnings  Status  Avg Off  Max Off  Disconnections  Session Time\n" +
                "Student A    2       Focused   12       20       0              21:31-22:29\n" +
                "Student B    1       Warned    5        10       1              21:31-22:29";
        tvStudentTable.setText(placeholderTable);

        btnExportCsv.setOnClickListener(v -> exportCsv());
    }

    private void exportCsv() {
        String header = "name,warnings,status,avg_off,max_off,disconnections,session_time\n";
        String row1 = "Student A,2,Focused,12,20,0,21:31-22:29\n";
        String row2 = "Student B,1,Warned,5,10,1,21:31-22:29\n";
        String fullCsv = header + row1 + row2;

        String fileName = "session_data.csv";

        try {
            OutputStream fos;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ scoped storage
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Failed to create file URI");

                fos = getContentResolver().openOutputStream(uri);
            } else {
                // Older Android
                java.io.File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                java.io.File file = new java.io.File(path, fileName);
                fos = new java.io.FileOutputStream(file);
            }

            if (fos != null) {
                fos.write(fullCsv.getBytes());
                fos.close();
            }

            Toast.makeText(this, "CSV exported to Downloads: " + fileName, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error exporting CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
