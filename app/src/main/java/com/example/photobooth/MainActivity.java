package com.example.photobooth;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int PERMISSION_CODE = 101;

    ImageView imgPhoto;
    Button btnCamera;
    RadioGroup rgFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rgFrame = findViewById(R.id.rgFrame);
        // ImageView harus draw open camera ke imgFrame1 atau imgFrame2
        ImageView imgFrame1 = findViewById(R.id.imgFrame1);
        ImageView imgFrame2 = findViewById(R.id.imgFrame2);

        rgFrame.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbFrame2) {
                imgFrame1.setVisibility(View.GONE);
                imgFrame2.setVisibility(View.VISIBLE);
            } else {
                imgFrame1.setVisibility(View.VISIBLE);
                imgFrame2.setVisibility(View.GONE);
            }
        });
        imgPhoto = findViewById(R.id.imgPhoto);
        btnCamera = findViewById(R.id.btnCamera);

        // IZIN KAMERA
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_CODE
            );
        }

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, CAMERA_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getExtras() != null) {

                Bitmap photo = (Bitmap) data.getExtras().get("data");

                // GABUNG FOTO + FRAME
                Bitmap finalBitmap = combineWithFrame(photo);

                imgPhoto.setImageBitmap(finalBitmap);

                // SIMPAN KE GALERI
                saveToGallery(finalBitmap);
            }
        }
    }


    // (LANJUTAN)GABUNG FOTO + FRAME
    private Bitmap combineWithFrame(Bitmap photo) {
        // 1. Cek tombol frame mana yang aktif saat ini
        int selectedFrameId = rgFrame.getCheckedRadioButtonId();
        int frameRes = (selectedFrameId == R.id.rbFrame2) ? R.drawable.frame2 : R.drawable.frame1;

        // 2. Load frame sesuai pilihan
        Bitmap frame = BitmapFactory.decodeResource(getResources(), frameRes);

        // 3. Buat canvas baru setara ukuran foto dari kamera
        Bitmap result = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // 4. Gambar foto di dasar
        canvas.drawBitmap(photo, 0, 0, null);

        // 5. Resize frame agar pas presisi dengan ukuran foto kamera
        Bitmap scaledFrame = Bitmap.createScaledBitmap(frame, photo.getWidth(), photo.getHeight(), true);

        // 6. Tempel frame di atasnya
        canvas.drawBitmap(scaledFrame, 0, 0, null);

        return result;
    }


    // SIMPAN KE GALERI
    private void saveToGallery(Bitmap bitmap) {

        String filename = "Photobooth_"
                + new SimpleDateFormat("bbbbMMdd_HHmmss", Locale.getDefault())
                .format(new Date())
                + ".jpg";

        OutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/Photobooth");

                Uri imageUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                fos = getContentResolver().openOutputStream(imageUri);

            } else {
                fos = MediaStore.Images.Media.insertImage(
                        getContentResolver(),
                        bitmap,
                        filename,
                        "Photobooth Image"
                ) != null ? null : null;
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            }

            Toast.makeText(this,
                    "Foto tersimpan di Galeri",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this,
                    "Gagal menyimpan foto",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
