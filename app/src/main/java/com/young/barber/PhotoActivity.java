package com.young.barber;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;


public class PhotoActivity extends AppCompatActivity {


    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }


    ImageView imageVIewInput;
    ImageView imageVIewOuput;
    SeekBar seekBar;
    private Mat img_input;
    private Mat img_output;
    static final int PICK_FROM_CAMERA = 2;
    static final int PICK_FROM_ALBUM=1;
    Bitmap bitmapOutput;
    File tempFile;
    private static final String TAG = "opencv";
    private final int GET_GALLERY_IMAGE = 200;

    private Canvas mCanvas,mCanvas2;
    private Bitmap mBitmap,mBitmap2;
    private Paint       mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;

    private int stroke=20;
    boolean isReady = false;
    static boolean photoOn = false;
    ImageView img;
    FrameLayout layout;
    LinearLayout Linearlay;
    Bitmap resized,bit;
    Boolean isCamera;
    int th=45;
    String shape[]={"계란형","원형","직사각형","다이아몬드형","역삼각형","사각형"};
    public int sha;
    public native void progress_processing(long outputImage,int resizeW,int resizeH, int th);

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        Bundle extras = getIntent().getExtras();
//        if (extras.getInt("image") == 1)
//        {
//            isCamera=true;
//            takePhoto();
//        }
//        else if(extras.getInt("image")==2)
//        {
//            isCamera=false;
//            goToAlbum();
//        }
        byte[] byteArray = getIntent().getByteArrayExtra("image1");
        bit = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        resized = null;
        layout = (FrameLayout) findViewById(R.id.layout1);
        Linearlay = (LinearLayout)findViewById(R.id.lay1);
        resized = Bitmap.createScaledBitmap(bit, 1000, (int)(bit.getHeight()*((float)1000/(float)bit.getWidth())), true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        float scale = (float) (1024/(float)bit.getWidth());
        int image_w = (int) (bit.getWidth() * scale);
        int image_h = (int) (bit.getHeight() * scale);
        Bitmap resize = Bitmap.createScaledBitmap(bit, image_w, image_h, true);
        resize.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        final byte[] byteArray2 = stream.toByteArray();

        Button markerbtn = (Button) findViewById(R.id.markerBtn);
        markerbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoActivity.this, DrawActivity.class);
                intent.putExtra("image",byteArray2);
                startActivity(intent);
            }
        });
        Button backbtn = (Button)findViewById(R.id.backBtn);
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        final MyView mvView = new MyView(layout.getContext());
        layout.addView(mvView);
        mPaint = new Paint();
        final Paint nPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFFFFFFFF);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1},
                0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
        seekBar = (SeekBar)findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            String color="#ff0000";
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                th=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Mat output= new Mat();
                progress_processing(output.getNativeObjAddr(),bit.getWidth(),bit.getHeight(),th);
                Bitmap bmp = Bitmap.createBitmap(bit.getWidth(),bit.getHeight(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(output,bmp);
                resized = Bitmap.createScaledBitmap(bmp, 1000, (int)(bmp.getHeight()*((float)1000/(float)bmp.getWidth())), true);
                mCanvas2.drawBitmap(resized,0 , 0, mPaint);
            }
        });
    }

    public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    public class MyView extends View {

        private static final float MINP = 0.25f;
        private static final float MAXP = 0.75f;
        private Path mPath;
        private Paint mBitmapPaint;

        public MyView(Context c) {
            super(c);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        public MyView(Context c, AttributeSet att) {
            super(c, att);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        public MyView(Context c, AttributeSet att, int ref) {
            super(c, att, ref);
            mPath = new Path();
            mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            mBitmap = Bitmap.createBitmap(resized.getWidth(), resized.getHeight(), Bitmap.Config.ALPHA_8);
            mCanvas = new Canvas(mBitmap);
            mCanvas2 = new Canvas(resized);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(0xFFFFFFFF);
            canvas.drawBitmap(resized, (Linearlay.getWidth() - resized.getWidth()) / 2, (Linearlay.getHeight() - resized.getHeight()) / 2, mBitmapPaint);
        }
    }
}
