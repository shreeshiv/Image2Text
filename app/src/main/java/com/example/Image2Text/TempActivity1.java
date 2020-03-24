package com.example.Image2Text;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TempActivity1 extends AppCompatActivity {
    ImageView imageView;
    TextView textView;
    CameraSource cameraSource;
    Button btn;
    final int requestCameraPermissionID = 1001;
    TextRecognizer textRecognizer;
    public static final int REQUEST_PERM_WRITE_STORAGE = 102;
    Bitmap bitmap;
    Frame frame;
    String imagePath="";

    static final int REQUEST_TAKE_PHOTO = 1;
    static private final int REQUEST_TAKE_PDF = 2;
    private static final int CAMERA_REQUEST = 1888;
    private static final int STORAGE_CODE = 102;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    TextRecognizer recognizer;
    static final int REQUEST_PICTURE_CAPTURE = 1;
    public static String pictureFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp1);
        textView = (TextView) findViewById(R.id.text);
        imageView = findViewById(R.id.imageView);
        btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // sendTakePictureIntent();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                    }
                    else if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        String permissions[] = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permissions, STORAGE_CODE);
                    }
                    else {
                        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO);
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Your android version not supported.", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    String permissionss[] = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(permissionss, STORAGE_CODE);
                }
                else {
                    dispatchTakePictureIntent(REQUEST_TAKE_PHOTO);
                }
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == STORAGE_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent(REQUEST_TAKE_PHOTO);


            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }

        }


    }

    private File createImageFile() throws IOException {
        // Create an image file name

        String folder = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(folder, "/MoneyWise/images");

        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "";
        imageFileName = "moneyWise_" + timeStamp + "_" + ".jpg";
        File file = new File(myDir, imageFileName);
        imagePath = file.getAbsolutePath();

        return file;
    }

    private void dispatchTakePictureIntent(int state) {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,

                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, state);

            }
        }
    }



    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_TAKE_PHOTO) {

                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

                ExifInterface ei = null;
                try {
                    ei = new ExifInterface(imagePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                Bitmap rotatedBitmap = null;
                switch(orientation) {

                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotatedBitmap = rotateImage(bitmap, 90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotatedBitmap = rotateImage(bitmap, 180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotatedBitmap = rotateImage(bitmap, 270);
                        break;

                    case ExifInterface.ORIENTATION_NORMAL:
                    default:
                        rotatedBitmap = bitmap;
                }

                bitmap= rotatedBitmap;



                imageView.setImageBitmap(bitmap);
                runTextRecognition();


            }
        }

    }
    private void runTextRecognition() {


        Log.d("", "runTextRecognition: " + imagePath);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        recognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        bitmap = rotateImage(bitmap, 90);
        if (recognizer.isOperational()) {
            // bitmap.;
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();

            final SparseArray<TextBlock> items = recognizer.detect(frame);
            if (items.size() != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < items.size(); i++) {
                    TextBlock item = items.valueAt(i);
                    stringBuilder.append(item.getValue());
                    stringBuilder.append("\n");
                }
                textView.setText(stringBuilder.toString());


            }
        }
    }


    //
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_PICTURE_CAPTURE && resultCode == RESULT_OK) {
//            File imgFile = new File(pictureFilePath);
//            bitmap = BitmapFactory.decodeFile(pictureFilePath);
//            if (imgFile.exists()) {
//                imageView.setImageURI(Uri.fromFile(imgFile));
//            }
//        }
//    }
public static Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
            matrix, true);
}


}