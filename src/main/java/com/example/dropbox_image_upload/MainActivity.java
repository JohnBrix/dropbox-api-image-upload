package com.example.dropbox_image_upload;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.DbxUserUsersRequests;
import com.dropbox.core.v2.users.FullAccount;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Wave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button loadImage, upload, download, displayDownload;
    Uri uri;
    ProgressBar progressBar;
    private static final int PICK_IMAGE = 100;
    private static final String ACCESS_TOKEN = "jMQjMsTtg3IAAAAAAAAAAVObPlY8Yef9C13w75pqpBPGq0EO_sQoek6XX6BxOCdn";
    private int STORAGE_PERMISSION_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //for automatic runnable thread but this is bad practice because its already ASYNC runned
   /*     StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);*/
        progressBar = findViewById(R.id.regLoadingScreen);
        imageView = (ImageView) findViewById(R.id.imageView);
        loadImage = findViewById(R.id.buttonLoadPicture);
        upload = findViewById(R.id.upload);
        download = findViewById(R.id.download);
        displayDownload = findViewById(R.id.displayDownload);
        upload.setEnabled(false);
        Sprite doubleBounce = new Wave();
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressBar.setVisibility(View.GONE);


        loadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "You have already granted this permission!",
                            Toast.LENGTH_SHORT).show();
                    openGallery();
                } else {
                    requestStoragePermission();
                }

            }
        });
        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                progressBar.setVisibility(View.VISIBLE);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            downloadingFile();
                            /*Toast.makeText(v.getContext(), "SUCCESSFULLY DOWNLOADED", Toast.LENGTH_SHORT).show();*/
                        } catch (DbxException e) {
                            e.printStackTrace();
                        }

                        progressBar.post(new Runnable() {
                            public void run() {
                                //WE USE THIS FOR
                                // ISA LANG GAGANA NA UI AT MAGPAPATONG PATONG PARA DI LUMAKE ANG RAM management!
                                progressBar.setVisibility(View.GONE);
                                
                            }
                        });


                    }
                }).start();


            }
        });
        displayDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //NOTES THIS IS OFFLINE METHOD YOU CAN USE
                // SQL LITE TO SAVE THE LINK IF THE IMAGE IS OFFLINE YOU CAN STILL USE OF IT.
                String testFile = "/fb_img_1608366368565.jpg"; //dito magsasave ka ng data from mysql database mo
                //THIS PATH IS WHERE YOU CAN DOWNLOAD THE IMAGE FROM DROPBOX API
                File imgFile = new File("/storage/emulated/0/Android/data/com.example.dropbox_image_upload/files/Pictures" + testFile);
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

                if (imgFile.exists()) {
                    imageView.setImageBitmap(myBitmap); //need bitmap to display local stoage
                } else {
                    Toast.makeText(v.getContext(), "putangna mo di sya nag eexist", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void downloadingFile() throws DbxException {
        DbxRequestConfig config;
        config = new DbxRequestConfig("dropbox/spring-boot-file-upload");
        DbxClientV2 client;
        client = new DbxClientV2(config, ACCESS_TOKEN);
        FullAccount account;
        DbxUserUsersRequests r1 = client.users();
        account = r1.getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        System.out.println("waiting...");

        DbxDownloader<FileMetadata> downloader = client.files().download("/fb_img_1608366368565.jpg"); //from database ito sasave mo sa query
        //example
        // column mo is image = profile.jpeg
        try {
            // MAKIKITA MO LNG TO SA COM.example.dropbox_image_upload folder mo sa android/data/com.example.dropbox_image_upload
            //getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)+fil
            //WORKED ITO NASA TAAS

            String filePath = "/storage/emulated/0/Android/data/com.example.dropbox_image_upload/files/Pictures";
            String imagePath = "/fb_img_1608366368565.jpg"; //dapat manggling sa database to mysql
            FileOutputStream out = new FileOutputStream(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + imagePath);
            System.out.println("your image path: " + out);
            /*  Toast.makeText(this, "your image path: " + out, Toast.LENGTH_SHORT).show();*/
            downloader.download(out);
            out.close();
            System.out.println("Successfully downloaded!");
        } catch (DbxException | IOException ex) {
            System.out.println(ex.getMessage());
        }
    }


    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because of this and that")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }


    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {

            uri = data.getData();
            System.out.println("bRIX" + uri);
            imageView.setImageURI(uri);
            //FILE PATH WITH IMAGE
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = this.getContentResolver().query(uri, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            final String filePath = cursor.getString(columnIndex);
            cursor.close();
            /*System.out.println("my path" +filePath);*/

            final File file = new File(filePath);
            String strFileName = file.getAbsolutePath();
            System.out.println("my path: " + strFileName);

            //ORIGINAL FILE NAME pompom.jpeg
            final String url = filePath;
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            final String originalImageName = URLUtil.guessFileName(url, null, fileExtenstion);
            System.out.println("file original: " + originalImageName);

            //upload button
            upload.setEnabled(true);
            upload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("CLICKED");
                    uploadingFile(originalImageName, file);
                    progressBar.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public void uploadingFile(final String originalImageName, final File file) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Upload "test.txt" to Dropbox
                try (InputStream in = new FileInputStream(file)) {
                    DbxRequestConfig config;
                    config = new DbxRequestConfig("dropbox/spring-boot-file-upload");
                    DbxClientV2 client;
                    client = new DbxClientV2(config, ACCESS_TOKEN);
                    FullAccount account;
                    DbxUserUsersRequests r1 = client.users();
                    account = r1.getCurrentAccount();
                    System.out.println(account.getName().getDisplayName());

                    FileMetadata metadata = client.files().uploadBuilder("/" + originalImageName)
                            .uploadAndFinish(in);

                    in.close();
                    System.out.println("SUccessfully upload!");

                    progressBar.post(new Runnable() {
                        public void run() {
                            //WE USE THIS FOR
                            // ISA LANG GAGANA NA UI AT MAGPAPATONG PATONG PARA DI LUMAKE ANG RAM management!
                            progressBar.setVisibility(View.GONE);

                        }
                    });

                } catch (IOException | DbxException e) {
                    e.printStackTrace();
                }
            }

        }).start();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

}