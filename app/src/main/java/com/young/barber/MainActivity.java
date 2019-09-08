/*
 *  Copyright (C) 2015-present TzuTaLin
 */

package com.young.barber;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.PedestrianDet;
import com.tzutalin.dlib.VisionDetRet;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity{
    private static final int RESULT_LOAD_IMG = 1;
    private static final int REQUEST_CODE_PERMISSION = 2;

    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");

    }

    static final int PICK_FROM_CAMERA = 2;

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    // UI
    private ProgressDialog mDialog;
    private MaterialListView mListView;
    private FloatingActionButton mFabActionBt;
    private FloatingActionButton mFabCamActionBt;
    private FloatingActionButton mFabNextActionBt;
    private Toolbar mToolbar;

    File tempFile;

    private String mTestImgPath;
    private FaceDet mFaceDet;
    private PedestrianDet mPersonDet;
    private List<Card> mCard = new ArrayList<>();
    Bitmap bm,bmp;

    Mat img_input, img_output;
    int pointx[]=new int[68];
    int pointy[]=new int[68];

    Rect crabImage;
    public static int sha=-1;
    Boolean resizes = false;
    static int cnt=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        crabImage = new Rect();
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        setSupportActionBar(mToolbar);
        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }
        setupUI();
    }

    protected void setupUI() {
        mFabActionBt = (FloatingActionButton) findViewById(R.id.fab);
        mFabCamActionBt = (FloatingActionButton) findViewById(R.id.fab_cam);
        mFabCamActionBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        mFabActionBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // launch Gallery
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
            }
        });
    }

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_persmission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_persmission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    protected void demoStaticImage() {
        if (mTestImgPath != null) {
            Timber.tag(TAG).d("demoStaticImage() launch a task to det");
            runDemosAsync(mTestImgPath);
        } else {
            Timber.tag(TAG).d("demoStaticImage() mTestImgPath is null, go to gallery");
            // Create intent to Open Image applications like Gallery, Google Photos
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSION) {
            demoStaticImage();
        }
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            tempFile = createImageFile();
        } catch (IOException e) {
            finish();
            e.printStackTrace();
        }
        if (tempFile != null) {

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {

                Uri photoUri = FileProvider.getUriForFile(this,
                        "Barber.provider", tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, 2);

            } else {
                Uri photoUri = Uri.fromFile(tempFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, 2);
            }
        }
    }
    private File createImageFile() throws IOException {

        // 이미지 파일 이름 ( blackJin_{시간}_ )
        String timeStamp = new SimpleDateFormat("HHmmss").format(new Date());
        String imageFileName = "barber_" + timeStamp + "_";

        // 이미지가 저장될 폴더 이름 ( blackJin )
        File storageDir = new File(Environment.getExternalStorageDirectory() + "/barber/");
        if (!storageDir.exists()) storageDir.mkdirs();

        // 빈 파일 생성
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        return image;
    }
    public  void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath,
                                       String filename) {

        File file = new File(strFilePath);
        // If no folders
        if (!file.exists()) {
            file.mkdirs();
            // Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
        }

        File fileCacheItem = new File(strFilePath + filename);
        OutputStream out = null;

        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage=data.getData();
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if(bitmap.getWidth()>1080) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 1080, (int)(bitmap.getHeight()/((float)bitmap.getWidth()/1080)), true);
                    if(bitmap.getWidth()>bitmap.getHeight())
                        bitmap = rotate(bitmap, 90);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "Title", null);
                    selectedImage=Uri.parse(path);
                }
                else
                {
                    if(bitmap.getWidth()>bitmap.getHeight())
                        bitmap = rotate(bitmap, 90);
                }

                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mTestImgPath = cursor.getString(columnIndex);

                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                tempFile = new File(cursor.getString(column_index));
                cursor.close();
                if (mTestImgPath != null) {
                    runDemosAsync(mTestImgPath);
                }
            } else {
                Uri imageUri = Uri.fromFile(tempFile);
                Bitmap bitmap=null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(bitmap.getWidth()>1080) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, 1080, (int)(bitmap.getHeight()/((float)bitmap.getWidth()/1080)), true);
                    if(bitmap.getWidth()>bitmap.getHeight())
                        bitmap = rotate(bitmap, 90);
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
                    imageUri=Uri.parse(path);
                }
                else
                {
                    if(bitmap.getWidth()>bitmap.getHeight())
                        bitmap = rotate(bitmap, 90);
                }
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                // Get the cursor
                Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mTestImgPath = cursor.getString(columnIndex);

                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                tempFile = new File(cursor.getString(column_index));
                cursor.close();
                if (mTestImgPath != null) {
                    runDemosAsync(mTestImgPath);
                }
            }
        } catch (Exception e) {
            //Toast.makeText(this, "사진을 선택하지 않았습니다." + e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    public int exifOrientationToDegrees(int exifOrientation)
    {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
        {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
        {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
        {
            return 270;
        }
        return 0;
    }

    public Bitmap rotate(Bitmap bitmap, int degrees)
    {
        if(degrees != 0 && bitmap != null)
        {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try
            {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted)
                {
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex)
            {
            }
        }
        return bitmap;
    }
    // ==========================================================
    // Tasks inner class
    // ==========================================================
    @NonNull
    private void runDemosAsync(@NonNull final String imgPath) {
        // demoPersonDet(imgPath);
        demoFaceDet(imgPath);
    }

    private void demoPersonDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(List<VisionDetRet> personList) {
                super.onPostExecute(personList);
                if (personList.size() > 0) {
                    Card card = new Card.Builder(MainActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, personList, Color.BLUE))
                            .setTitle("Person det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    Toast.makeText(getApplicationContext(), "No person", Toast.LENGTH_LONG).show();
                }
                // updateCardListView();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mPersonDet == null) {
                    mPersonDet = new PedestrianDet();
                }

                Timber.tag(TAG).d("Image path: " + imgPath);

                List<VisionDetRet> personList = mPersonDet.detect(imgPath);
                return personList;
            }
        }.execute();
    }
    private void demoFaceDet(final String imgPath) {
        new AsyncTask<Void, Void, List<VisionDetRet>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                showDiaglog("Detecting faces");
            }
            @SuppressLint("WrongThread")
            @Override
            protected void onPostExecute(List<VisionDetRet> faceList) {
                super.onPostExecute(faceList);
                if (faceList.size() > 0) {
                    Card card = new Card.Builder(MainActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(drawRect(imgPath, faceList, Color.GREEN))
                            .setTitle("Face det")
                            .endConfig()
                            .build();
                    mCard.add(card);
                } else {
                    if(!resizes)
                    {
                        Uri imageUri = Uri.fromFile(new File(imgPath));
                        Bitmap bitmap=null;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imageUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        while (cnt<3){
                            bitmap = rotate(bitmap,90);
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Title", null);
                            imageUri=Uri.parse(path);
                            String[] filePathColumn = {MediaStore.Images.Media.DATA};
                            // Get the cursor
                            Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
                            cursor.moveToFirst();
                            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                            mTestImgPath = cursor.getString(columnIndex);

                            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                            cursor.moveToFirst();
                            tempFile = new File(cursor.getString(column_index));
                            cursor.close();
                            if (mTestImgPath != null) {
                                resizes=true;
                                demoFaceDet(mTestImgPath);
                            }
                            cnt++;
                        }
                    }
                    else{
                        Toast.makeText(getApplicationContext(), "No face", Toast.LENGTH_LONG).show();
                        resizes=false;
                    }
                }
                // updateCardListView();
                if(!resizes)
                    dismissDialog();
            }

            @Override
            protected List<VisionDetRet> doInBackground(Void... voids) {
                // Init
                if (mFaceDet == null) {
                    mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
                }

                final String targetPath = Constants.getFaceShapeModelPath();
                if (!new File(targetPath).exists()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Toast.makeText(MainActivity.this, "Copy landmark model to " + targetPath, Toast.LENGTH_SHORT).show();
                        }
                    });
                    FileUtils.copyFileFromRawToOthers(getApplicationContext(), R.raw.shape_predictor_68_face_landmarks, targetPath);
                }
                List<VisionDetRet> faceList = mFaceDet.detect(imgPath);
                return faceList;
            }
        }.execute();
    }
//
//    private void updateCardListView() {
//        mListView.clearAll();
//        for (Card each : mCard) {
//            mListView.add(each);
//        }
//    }

    private void showDiaglog(String title) {
        dismissDialog();
        mDialog = ProgressDialog.show(MainActivity.this, title, "process..", true);
    }

    private void dismissDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private BitmapDrawable drawRect(String path, List<VisionDetRet> results, int color) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        bm = BitmapFactory.decodeFile(path, options);

        img_input = new Mat();
        Bitmap bmp32 = bm.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, img_input);

        Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
        // so we need to convert it to mutable one
        Bitmap bmg = bm.copy(bitmapConfig, true);
//        int width = bm.getWidth();
//        int height = bm.getHeight();
//        // By ratio scale
//        float aspectRatio = bm.getWidth() / (float) bm.getHeight();
//
//        final int MAX_SIZE = 512;
//        int newWidth = MAX_SIZE;
//        int newHeight = MAX_SIZE;
//        float resizeRatio = 1;
//        newHeight = Math.round(newWidth / aspectRatio);
//        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
//            Timber.tag(TAG).d("Resize Bitmap");
//            bm = getResizedBitmap(bm, newWidth, newHeight);
//            resizeRatio = (float) bm.getWidth() / (float) width;
//            Timber.tag(TAG).d("resizeRatio " + resizeRatio);
//        }

        // Create canvas to draw
        Canvas canvas = new Canvas(bmg);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);
        // Loop result list
        for (VisionDetRet ret : results) {
            Rect bounds = new Rect();
            bounds.left = (int) ret.getLeft();
            bounds.top = (int) ret.getTop();
            bounds.right = (int) ret.getRight();
            bounds.bottom = (int) ret.getBottom();

            if(bounds.left-bounds.width()/4>0)
                crabImage.left = ret.getLeft()-bounds.width()/4;
            else
                crabImage.left = 0;

            if(bounds.top-bounds.height()/3>0)
                crabImage.top = ret.getTop()-bounds.height()/3;
            else
                crabImage.top = 0;

            if(bounds.right+bounds.width()/4<bm.getWidth())
                crabImage.right = ret.getRight()+bounds.width()/4;
            else
                crabImage.right = bm.getWidth();

            if(bounds.bottom+bounds.height()/3<bm.getHeight())
                crabImage.bottom = ret.getBottom()+bounds.height()/3;
            else
                crabImage.bottom = bm.getHeight();

            canvas.drawRect(bounds, paint);
            // Get landmark
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            int idx=0;
            for (Point point : landmarks) {
                pointx[idx]=point.x;
                pointy[idx++]=point.y;
                canvas.drawCircle(point.x, point.y, 2, paint);
            }
            imageprocess_and_showResult();
        }
        return new BitmapDrawable(getResources(), bmg);
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }
    public native int imageprocessing(long inputImage, long outputImage, int Ptx, int Pty, int w, int h, int[] x, int[] y);
    private void imageprocess_and_showResult() {

        if (img_output == null)
            img_output = new Mat();
        sha=imageprocessing(img_input.getNativeObjAddr(), img_output.getNativeObjAddr(),
                crabImage.left,crabImage.top,crabImage.width(),crabImage.height(),
                pointx,pointy);//t값에따라 대가리형태
        if(sha<0) {
            Intent intents = new Intent(MainActivity.this,MainActivity.class);
            startActivity(intents);
        }
        bmp = Bitmap.createBitmap(crabImage.width(),crabImage.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_output,bmp);
        Intent intent = new Intent(MainActivity.this, PhotoActivity.class);
//                ImageResizeUtils.resizeFile(tempFile, tempFile, 1000, true);
//                BitmapFactory.Options options = new BitmapFactory.Options();
//                Bitmap originalBm = BitmapFactory.decodeFile(tempFile.getAbsolutePath(), options);
        resizes=false;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        float scale = (float) (1024/(float)bmp.getWidth());
        int image_w = (int) (bmp.getWidth() * scale);
        int image_h = (int) (bmp.getHeight() * scale);
        Bitmap resize = Bitmap.createScaledBitmap(bmp, image_w, image_h, true);
        resize.compress(Bitmap.CompressFormat.JPEG, 50, stream);
        final byte[] byteArray = stream.toByteArray();
        intent.putExtra("image1",byteArray);
        startActivity(intent);
    }
}