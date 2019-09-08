package com.young.barber;

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
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;

public class DrawActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    Bitmap bit, resized;
    private Paint mPaint;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private FrameLayout layout;
    private int stroke = 20;
    Button completeBtn,backBtn;
    ImageButton insertBtn, deleteBtn;
    ImageView img;
    SeekBar weightBar;
    LinearLayout Linearlay;
    public static Mat markerImage;
    public static Bitmap mBitmap;

    public native void marking_processing(long output,long markerImage,int w,int h);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_draw);
        layout = (FrameLayout) findViewById(R.id.layout1);
        byte[] byteArray = getIntent().getByteArrayExtra("image");
        bit = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        Linearlay = (LinearLayout)findViewById(R.id.lay2);
        resized = null;


        Bitmap bitmap = Bitmap.createBitmap(bit.getWidth(), bit.getHeight(), Bitmap.Config.ARGB_8888);
        Mat markerImage = new Mat();
        Utils.bitmapToMat(bitmap,markerImage);
        Mat outputImage = new Mat();
        Utils.bitmapToMat(bitmap,outputImage);
        marking_processing(outputImage.getNativeObjAddr(),markerImage.getNativeObjAddr(),bit.getWidth(),bit.getHeight());
        Utils.matToBitmap(outputImage,bit);
        int height = bit.getHeight();
        int width = bit.getWidth();
        resized = Bitmap.createScaledBitmap(bit, 1000, (int)(bit.getHeight()*((float)1000/(float)bit.getWidth())), true);

        deleteBtn = (ImageButton) findViewById(R.id.delete);
        insertBtn = (ImageButton) findViewById(R.id.insert);
        completeBtn = (Button) findViewById(R.id.complete);
        backBtn = (Button)findViewById(R.id.back);

        weightBar = (SeekBar)findViewById(R.id.seekbar2);
        weightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    stroke=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteBtn.setImageResource(R.drawable.eraseron);
                insertBtn.setImageResource(R.drawable.penun);
                mPaint.setColor(0xFF010101);
            }
        });
        insertBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteBtn.setImageResource(R.drawable.eraserun);
                insertBtn.setImageResource(R.drawable.penon);
                mPaint.setColor(0xFF888888);
            }
        });
        completeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //사진전송
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                float scale = (float) (1024/(float)resized.getWidth());
                int image_w = (int) (resized.getWidth() * scale);
                int image_h = (int) (resized.getHeight() * scale);
                Bitmap resize = Bitmap.createScaledBitmap(resized, image_w, image_h, true);
                resize.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                final byte[] byteArray = stream.toByteArray();
                ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                float scale2 = (float) (1024/(float)mBitmap.getWidth());
                int image_w2 = (int) (mBitmap.getWidth() * scale);
                int image_h2 = (int) (mBitmap.getHeight() * scale);
                Bitmap resize2 = Bitmap.createScaledBitmap(mBitmap, image_w, image_h, true);
                resize2.compress(Bitmap.CompressFormat.JPEG, 50, stream2);
                final byte[] byteArray2 = stream2.toByteArray();
                Intent intent = new Intent(DrawActivity.this, CompleteActivity.class);
                intent.putExtra("image",byteArray);
                intent.putExtra("image2",byteArray2);
                startActivity(intent);
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                float scale = (float) (1024/(float)bit.getWidth());
                int image_w = (int) (bit.getWidth() * scale);
                int image_h = (int) (bit.getHeight() * scale);
                Bitmap resize = Bitmap.createScaledBitmap(bit, image_w, image_h, true);
                resize.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                final byte[] byteArray = stream.toByteArray();
                Intent intent = new Intent(DrawActivity.this, PhotoActivity.class);
                intent.putExtra("image1",byteArray);
                startActivity(intent);
            }
        });
        //1020 1782
        MyView myView = new MyView(layout.getContext());
        layout.addView(myView);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(0xFF888888);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(12);
        mEmboss = new EmbossMaskFilter(new float[]{1, 1, 1},
                0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
    }

    public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    public class MyView extends View {

        private static final float MINP = 0.25f;
        private static final float MAXP = 0.75f;
        private Canvas mCanvas, mCanvas2;
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
            resized = Bitmap.createScaledBitmap(bit, 1000, (int)(bit.getHeight()*((float)1000/(float)bit.getWidth())), true);
            mBitmap = Bitmap.createBitmap(resized.getWidth(), resized.getHeight(), Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawBitmap(mBitmap,(Linearlay.getWidth()-resized.getWidth())/2, (Linearlay.getHeight()-resized.getHeight())/2,mBitmapPaint);
            mCanvas2 = new Canvas(resized);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // canvas.drawPath(mPath, mPaint);
            //opencv로 mBitmap2 전송
            // abs((int)(layout.getWidth()-img_output.cols())/2)
            canvas.drawBitmap(resized, (Linearlay.getWidth()-resized.getWidth())/2, (Linearlay.getHeight()-resized.getHeight())/2, mBitmapPaint);
        }

        private float mX, mY;
        private static final float TOUCH_TOLERANCE = 4;

        private void touch_start(float x, float y) {
            mPath.reset();
            mPath.moveTo(x, y);
            mX = x;
            mY = y+20;

        }

        private void touch_move(float x, float y) {
            float dx = Math.abs(x - mX);
            float dy = Math.abs(y - mY);
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                mX = x;
                mY = y;
                mPath.lineTo(mX, mY);
                mPaint.setStrokeWidth(stroke);
                mCanvas.drawPath(mPath, mPaint);
                markerImage = new Mat();
                Utils.bitmapToMat(mBitmap,markerImage);
                Mat outputImage = new Mat();
                Bitmap bmp32 = Bitmap.createBitmap(bit.getWidth(), bit.getHeight(), Bitmap.Config.ARGB_8888);
                Utils.bitmapToMat(bmp32,outputImage);
                marking_processing(outputImage.getNativeObjAddr(),markerImage.getNativeObjAddr(),markerImage.width(),markerImage.height());
                Utils.matToBitmap(outputImage,resized);;
                mPath.reset();
                mPath.moveTo(x, y);
            }
            // commit the path to our offscreen
            // kill this so we don't double draw
        }

        private void touch_up() {
            mPath.lineTo(mX, mY);
            // commit the path to our offscreen
            mPaint.setStrokeWidth(stroke);
            mCanvas.drawPath(mPath, mPaint);
            markerImage = new Mat();
            Utils.bitmapToMat(mBitmap,markerImage);
            Mat outputImage = new Mat();
            Bitmap bmp32 = Bitmap.createBitmap(bit.getWidth(), bit.getHeight(), Bitmap.Config.ARGB_8888);
            Utils.bitmapToMat(bmp32,outputImage);
            marking_processing(outputImage.getNativeObjAddr(),markerImage.getNativeObjAddr(),markerImage.width(),markerImage.height());
            Utils.matToBitmap(outputImage,resized);
            // kill this so we don't double draw
            mPath.reset();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touch_start(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_MOVE:
                    touch_move(x, y);
                    invalidate();
                    break;
                case MotionEvent.ACTION_UP:
                    touch_up();
                    invalidate();
                    break;
            }
            return true;
        }
    }
}