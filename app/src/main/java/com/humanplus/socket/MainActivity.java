package com.humanplus.socket;

import android.Manifest;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.humanplus.socket.BuildConfig.DEBUG;


/**
 * Getting media path was inspired by author below.
 * Original source: https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 * Thank you.
 *
 * @version 2009-07-03
 * @author Peli
 * @version 2013-12-11
 * @author paulburke (ipaulpro)
 */

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 1;
    private static final int STORAGE_PERMISSION = 2;
    private static final int SELECT_VIDEO = 3;

    private String[] selectedImagePath;
    private String selectedVideoPath;

    // 2 Sockets for video and imgs.
    private Socket socket, socket2;

    private int i = 0;
    private int count = 0;
    // Use your IP and Port. port must be between 1024 ~ 49151
    private static final String ip = "192.168.0.45";
    private static final int port = 1157;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        selectedImagePath = new String[20];

        // For denied permission.
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Checks external storage read/write
        checkPermission();

    }

    public void onClickButton(View v) {
        switch(v.getId()) {

            // Select Image
            case R.id.button:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(Intent.createChooser(intent, "Select img"), SELECT_PICTURE);
                break;

            // Select Video
            case R.id.button4:
                Intent intent2 = new Intent();
                intent2.setType("video/*");
                intent2.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent2, "Select video"), SELECT_VIDEO);
                break;

            // Start transferring image
            // Supports only jpg files
            case R.id.button2:

                for(i = 0 ; i < count ;i++) {

                    try {
                        socket = new Socket(ip, port);
                        System.out.println("Connecting...");

                        File file = new File(selectedImagePath[i]);
                        byte[] bytes = new byte[(int) file.length()];
                        FileInputStream fis = new FileInputStream(file);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        bis.read(bytes, 0, bytes.length);
                        OutputStream os = socket.getOutputStream();
                        System.out.println("Sending...");

                        os.write(bytes, 0, bytes.length);
                        os.flush();

                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                break;

            // Start transferring video
            // Supports only mp4 files
            case R.id.button3:

                Thread thread2 = new Thread() {
                    @Override
                    public void run() {
                        try {
                            socket2 = new Socket(ip, port+1);
                            System.out.println("Connecting2...");

                            File file2 = new File(selectedVideoPath);
                            byte[] bytes2 = new byte[(int) file2.length()];
                            FileInputStream fis2 = new FileInputStream(file2);
                            BufferedInputStream bis2 = new BufferedInputStream(fis2);
                            bis2.read(bytes2, 0, bytes2.length);
                            OutputStream os2 = socket2.getOutputStream();
                            System.out.println("Sending2...");

                            os2.write(bytes2, 0, bytes2.length);
                            os2.flush();

                            socket2.close();
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread2.start();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_OK) {
            if(requestCode == SELECT_PICTURE) {

                // Put data to clipData.
                // Because Android Intent cannot get the array type of Strings.
                ClipData clipData = data.getClipData();
                count = clipData.getItemCount();

                for(int i = 0; i<count;i++) {
                    System.out.println(clipData.getItemAt(i).getUri().toString());
                    selectedImagePath[i] = getPath(this, clipData.getItemAt(i).getUri());
                    System.out.println("Image Path: " + selectedImagePath[i]);
                }
            }

            else if(requestCode == SELECT_VIDEO) {
                Uri selectedImageUri = data.getData();
                selectedVideoPath = getPath(this, selectedImageUri);
                System.out.println("Media Path: " + selectedVideoPath);
            }
        }
    }

    private void fileTransfer() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ip, port);
                    System.out.println("Connecting...");

                    File file = new File(selectedImagePath[i]);

                    byte[] bytes = new byte[(int)file.length()];
                    FileInputStream fis = new FileInputStream(file);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    bis.read(bytes, 0, bytes.length);
                    OutputStream os = socket.getOutputStream();

                    System.out.println("Sending...");

                    os.write(bytes, 0, bytes.length);
                    os.flush();

                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.<br>
     * <br>
     * Callers should check whether the path is local before assuming it
     * represents a local file.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @see #isLocal(String)
     * @see #getFile(Context, Uri)
     * @author paulburke
     */

    public String getPath(final Context context, final Uri uri) {

        if (DEBUG)
            Log.d(" File -",
                    "Authority: " + uri.getAuthority() +
                            ", Fragment: " + uri.getFragment() +
                            ", Port: " + uri.getPort() +
                            ", Query: " + uri.getQuery() +
                            ", Scheme: " + uri.getScheme() +
                            ", Host: " + uri.getHost() +
                            ", Segments: " + uri.getPathSegments().toString()
            );

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }

            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                if (DEBUG)
                    DatabaseUtils.dumpCursor(cursor);

                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "외부 저장소 사용을 위해 읽기/쓰기가 필요합니다", Toast.LENGTH_SHORT).show();
                }

                requestPermissions(new String[]
                                {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION);

            } else {
                Toast.makeText(this, "권한 승인되었음", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
