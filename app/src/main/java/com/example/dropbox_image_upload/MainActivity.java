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
import android.os.StrictMode;
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
import com.dropbox.core.v2.files.GetTemporaryLinkResult;
import com.dropbox.core.v2.files.GetThumbnailBuilder;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.ThumbnailFormat;
import com.dropbox.core.v2.files.ThumbnailSize;
import com.dropbox.core.v2.users.DbxUserUsersRequests;
import com.dropbox.core.v2.users.FullAccount;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.Wave;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okio.Okio;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button loadImage, upload, download, displayDownload, links;
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
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        progressBar = findViewById(R.id.regLoadingScreen);
        imageView = (ImageView) findViewById(R.id.imageView);
        loadImage = findViewById(R.id.buttonLoadPicture);
        upload = findViewById(R.id.upload);
        download = findViewById(R.id.download);
        displayDownload = findViewById(R.id.displayDownload);
        links = findViewById(R.id.links);
        upload.setEnabled(false);
        Sprite doubleBounce = new Wave();
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressBar.setVisibility(View.GONE);
        //https://api.dropboxapi.com/2/files/get_temporary_link
        //DESCRIPTION
        //Get a temporary link to stream content of a file. This link will expire in four hours and afterwards you will get 410 Gone.
        // This URL should not be used to display content directly in the browser.
        // The Content-Type of the link is determined automatically by the file's mime type.
        //https://www.dropbox.com/developers/documentation/http/documentation#files-get_temporary_link
//        String temporaryLink = "https://content.dropboxapi.com/apitl/1/AsjKXEn4yVJki0kjTQz7GHFIp7HDELveBgvqtLCrl5Eq17XvVdD-04BHcRoYczmjbTXMUXurukUNyAYU1CP6kepFJso18vLXvg3ITuBLis9tFdNRNn5JjqK-qdw4QpsD9AkEZbjrijUrEK5TFqCqu9glLpe76ebClc662yJFiGzxA2i3CZOSyICI4sV7SY4FRiFQvufGp5X1ZrqaLkuElnr0gtG9fz7T8yoA9MQqCU9wo1RYqDi9KtT36sza8CPqLqJYa1-oD3KTld77ZkZo_O0nLsLO1hlxy6_zsqHcdogKCrT7AalnteSDsLX5MagyVYFbq20D-gshw4L2b6nmSpftiIa6-wmo7FNfxzCIMDOcuA";
//        Picasso.get().load(temporaryLink).into(imageView);

        try {
            getThumbnailBatch();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        links.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            gettingFiles();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        progressBar.post(new Runnable() {
                            public void run() {
                                //WE USE THIS FOR
                                // ISA LANG GAGANA NA UI AT MAGPAPATONG PATONG PARA DI LUMAKE ANG RAM management!
                                progressBar.setVisibility(View.GONE);
                                Picasso.get().load("/fb_img_1608366368565.jpg").into(imageView);
                            }
                        });

                    }
                }).start();

            }
        });

    }

    public void getThumbnailBatch() throws DbxException, IOException {
        //GET TEMPORARY LINK IS ONE FILE RETURN ONLY NOT FOR LIST DATA.
        DbxRequestConfig config;
        config = new DbxRequestConfig("dropbox/spring-boot-file-upload");
        DbxClientV2 client;
        client = new DbxClientV2(config, ACCESS_TOKEN);
        FullAccount account;
        DbxUserUsersRequests r1 = client.users();
        account = r1.getCurrentAccount();
        System.out.println(account.getName().getDisplayName());
        System.out.println("waiting...");

        ListFolderResult result = client.files().listFolder("/images");

        //list initialize
        result.getEntries().forEach(e -> {
            System.out.println("meta: " + e.getPathLower());
            e.getPathLower();


         /*   GetThumbnailBuilder data = client.files().getThumbnailBuilder("" + e.getPathLower());*/

            try {
                DbxDownloader<FileMetadata> downloader =
                        client.files().getThumbnailBuilder(e.getPathLower())
                                .withFormat(ThumbnailFormat.JPEG)
                                .withSize(ThumbnailSize.W1024H768)
                                .start();
                //temporary link is list image
                GetTemporaryLinkResult temporaryLink = client.files().getTemporaryLink(e.getPathLower());
                System.out.println("temporay: "+temporaryLink.getLink());
                Picasso.get().load(temporaryLink.getLink()).into(imageView);
            } catch (DbxException ex) {
                ex.printStackTrace();
            }
        });


    }

    public void gettingFiles() throws IOException {
        System.out.println("Hi");
        //getting files
        try {


            DbxRequestConfig config;
            config = new DbxRequestConfig("dropbox/spring-boot-file-upload");
            DbxClientV2 client;
            client = new DbxClientV2(config, ACCESS_TOKEN);
            FullAccount account;
            DbxUserUsersRequests r1 = client.users();
            account = r1.getCurrentAccount();
            System.out.println(account.getName().getDisplayName());

            // Get files and folder metadata from Dropbox root directory
            ListFolderResult result = client.files().listFolder("");
            while (true) {

                for (Metadata metadata : result.getEntries()) {
                    System.out.println(metadata.getPathLower());
                    //ito kukuha ng list file mo example /pom.jpeg
                    // fhernand.jpg

                }

               /* result.getEntries().forEach(customer->
                        System.out.println("for each mo"+));*/
                if (!result.getHasMore()) {
                    break;
                }

                result = client.files().listFolderContinue(result.getCursor());

            }


        } catch (DbxException ex1) {
            ex1.printStackTrace();
        }
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

        DbxDownloader<FileMetadata> downloader = client.files().download("/1600990055141.jpg"); //from database ito sasave mo sa query
        //example
        // column mo is image = profile.jpeg
        try {
            // MAKIKITA MO LNG TO SA COM.example.dropbox_image_upload folder mo sa android/data/com.example.dropbox_image_upload
            //getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)+fil
            //WORKED ITO NASA TAAS

            String filePath = "/storage/emulated/0/Android/data/com.example.dropbox_image_upload/files/Pictures";
            String imagePath = "/1600990055141.jpg"; //dapat manggling sa database to mysql
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

                    FileMetadata metadata = client.files().uploadBuilder("/images/" + originalImageName).withAutorename(true)
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