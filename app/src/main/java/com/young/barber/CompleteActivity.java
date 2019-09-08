package com.young.barber;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;


public class CompleteActivity  extends AppCompatActivity implements ViewFlipperAction.ViewFlipperCallback{
    public native int beardshape(long outputImage, long beardImage,long markingMat);

    MainActivity mains;
    String recommend[];
    String point[];
    TextView facetext, beardtext, recommendtext,sc1,sc2,sc3,sc4;
    ImageView reco1,reco2,reco3,reco4;

    String faceShapeArr[] = {"계란형","원형","직사각형","다이아몬드","역삼각형","사각형"};
    String recommedShapeArr[] = {"GOATEE","CHIN CURTAIN","PETITE GOATEE","HOLLYWOOD","HIPSTER","NORRIS KIPPER","FULL BEARD","MUTTONCHOPS","GOATTER","CHIN STRAP"};
    String alpha[] = {"a","b","c","d","e","f","g","h","i","j"};
    //뷰플리퍼
    ViewFlipper flipper;
    //인덱스
    List<ImageView> indexes;

    Bitmap bits, bits2;
    int idxs=0;
    int idxt=0;
    Button recobtn1,recobtn2,recobtn3,recobtn4;
    Mat outputImage2;
    Mat beardImage2;

    ImageView img;

    public native int recommend(long outputImage, long inputImage, long recoImage);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete);
    //    ImageView img = (ImageView) findViewById(R.id.imageView1);
        recommend = new String[10];
        point = new String[10];
        point[0]="0";
        point[1]="0";
        byte[] byteArray = getIntent().getByteArrayExtra("image");
        byte[] byteArray2 = getIntent().getByteArrayExtra("image2");
        bits = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        bits2 = BitmapFactory.decodeByteArray(byteArray2, 0, byteArray2.length);

        Mat markingMat = new Mat();
        Utils.bitmapToMat(bits2,markingMat);
        Mat outputImage = new Mat();
        Utils.bitmapToMat(bits,outputImage);
        Mat beardImage = new Mat();
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.beard1);
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/3, bitmap.getHeight()/3, true);
        Utils.bitmapToMat(bitmap,beardImage);
        int beardshapes =  beardshape(outputImage.getNativeObjAddr(),beardImage.getNativeObjAddr(),markingMat.getNativeObjAddr());
        Log.i("beardssssss", Integer.toString(beardshapes));

         Bitmap completeImage = Bitmap.createBitmap(outputImage.cols(),outputImage.rows(), Bitmap.Config.ARGB_8888);
         Utils.matToBitmap(outputImage,completeImage);

        img = (ImageView)findViewById(R.id.img1);
        img.setImageBitmap(completeImage);

         mains = new MainActivity();
        int faceshpaes = mains.sha;
        //Toast.makeText(this,Integer.toString(mains.sha),Toast.LENGTH_LONG).show();
        try {
            InputStream is = getBaseContext().getResources().getAssets().open("beardscore.xls");
            Workbook wb = Workbook.getWorkbook(is);
            int cnt=0;
            if(wb != null) {
                Sheet sheet = wb.getSheet(0);   // 시트 불러오기
                if(sheet != null) {
                    int RowStart = faceshpaes*60+(beardshapes-2)*10+1;
                    int RowEnd = RowStart+10;
                    int idx=0;
                    int ptidx=0;
                    for(int i=RowStart;i<RowEnd;i++)
                    {
                        String contents = sheet.getCell(2,i).getContents();
                        recommend[idx++]=contents;
                        String contents2 = sheet.getCell(3,i).getContents();
                        point[ptidx++]=contents2;
                        Log.i("recommend jang", recommend[idx-1]);
                    }
                   // Toast.makeText(this,recommend[0]+" "+recommend[1]+" "+recommend[2]+"   "+Integer.toString(completeImage.getWidth())+"  "+Integer.toString(completeImage.getHeight()),Toast.LENGTH_LONG).show();
                   }
                }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
        //UI
        flipper = (ViewFlipper)findViewById(R.id.flipper);
        ImageView index0 = (ImageView)findViewById(R.id.imgIndex0);
        ImageView index1 = (ImageView)findViewById(R.id.imgIndex1);
        ImageView index2 = (ImageView)findViewById(R.id.imgIndex2);

        //인덱스리스트
        indexes = new ArrayList<>();
        indexes.add(index0);
        indexes.add(index1);
        indexes.add(index2);

        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
        View view1 = inflater.inflate(R.layout.viewflipper1, flipper, false);
        View view2 = inflater.inflate(R.layout.viewflipper2, flipper, false);
        View view3 = inflater.inflate(R.layout.viewflipper3, flipper, false);
        View view4 = inflater.inflate(R.layout.viewflipper4, flipper, false);

//       reco1 = (ImageView) view1.findViewById(R.id.reco1);
//       reco2 = (ImageView) view2.findViewById(R.id.reco2);
//       reco3= (ImageView) view3.findViewById(R.id.reco3);
//       reco4 = (ImageView) view4.findViewById(R.id.reco4);

        sc1 = (TextView)view1.findViewById(R.id.sc1);
        sc2 = (TextView)view2.findViewById(R.id.sc2);
        sc3 = (TextView)view3.findViewById(R.id.sc3);
        sc4 = (TextView)view4.findViewById(R.id.sc4);


        for(int i=0;i<10;i++)
        {
            if(recommend[0].equals(alpha[i]))
            {
                idxs=i;
                if(i==0)
                {
//                    reco1.setImageResource(R.drawable.a1);
//                    reco2.setImageResource(R.drawable.a2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[0]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[0]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==1)
                {
//                    reco1.setImageResource(R.drawable.b1);
//                    reco2.setImageResource(R.drawable.b2);
                    int sec = Integer.valueOf(point[0]) -1;

                    sc1.setText("1st: "+recommedShapeArr[1]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[1]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==2)
                {
//                    reco1.setImageResource(R.drawable.c1);
//                    reco2.setImageResource(R.drawable.c2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[2]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[2]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==3)
                {
//                    reco1.setImageResource(R.drawable.d1);
//                    reco2.setImageResource(R.drawable.d2);

                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[3]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[3]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==4)
                {
//                    reco1.setImageResource(R.drawable.e1);
//                    reco2.setImageResource(R.drawable.e2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[4]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[4]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==5)
                {
//                    reco1.setImageResource(R.drawable.f1);
//                    reco2.setImageResource(R.drawable.f2);

                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[5]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[5]+"\n\n추천점수: "+Integer.toString(sec)+".5");                }
                if(i==6)
                {
//                    reco1.setImageResource(R.drawable.g1);
//                    reco2.setImageResource(R.drawable.g2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[6]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[6]+"\n\n추천점수: "+Integer.toString(sec)+".5");                }
                if(i==7)
                {
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[7]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[7]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==8)
                {
//                    reco1.setImageResource(R.drawable.i1);
//                    reco2.setImageResource(R.drawable.i2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[8]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[8]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
                if(i==9)
                {
//                    reco1.setImageResource(R.drawable.j1);
//                    reco2.setImageResource(R.drawable.j2);
                    int sec = Integer.valueOf(point[0]) -1;
                    sc1.setText("1st: "+recommedShapeArr[9]+"\n\n추천점수: "+point[0]);
                    sc2.setText("1st: "+recommedShapeArr[9]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                }
            }
        }

        if(Integer.valueOf(point[1])>0)
            for(int i=0;i<10;i++)
            {
                if(recommend[1].equals(alpha[i]))
                {
                    idxt=i;
                    if(i==0)
                    {
//                        reco3.setImageResource(R.drawable.a1);
//                        reco4.setImageResource(R.drawable.a2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[0]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[0]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==1)
                    {
//                        reco3.setImageResource(R.drawable.b1);
//                        reco4.setImageResource(R.drawable.b2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[1]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[1]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==2)
                    {
//                        reco3.setImageResource(R.drawable.c1);
//                        reco4.setImageResource(R.drawable.c2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[2]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[2]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==3)
                    {
//                        reco3.setImageResource(R.drawable.d1);
//                        reco4.setImageResource(R.drawable.d2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[3]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[3]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==4)
                    {
//                        reco3.setImageResource(R.drawable.e1);
//                        reco4.setImageResource(R.drawable.e2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[4]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[4]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==5)
                    {
//                        reco3.setImageResource(R.drawable.f1);
//                        reco4.setImageResource(R.drawable.f2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[5]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[5]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==6)
                    {
//                        reco3.setImageResource(R.drawable.g1);
//                        reco4.setImageResource(R.drawable.g2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[6]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[6]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==7)
                    {
//                        reco3.setImageResource(R.drawable.h1);
//                        reco4.setImageResource(R.drawable.h2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[7]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[7]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==8)
                    {
//                        reco3.setImageResource(R.drawable.i1);
//                        reco4.setImageResource(R.drawable.i2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[8]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[8]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                    if(i==9)
                    {
//                        reco3.setImageResource(R.drawable.j1);
//                        reco4.setImageResource(R.drawable.j2);
                        int sec = Integer.valueOf(point[1]) -1;
                        sc3.setText("2nd: "+recommedShapeArr[9]+"\n\n추천점수: "+point[1]);
                        sc4.setText("2nd: "+recommedShapeArr[9]+"\n\n추천점수: "+Integer.toString(sec)+".5");
                    }
                }
            }
        outputImage2 = new Mat();
        Utils.bitmapToMat(bits,outputImage2);
        beardImage2 = new Mat();
        Bitmap bitmaps = BitmapFactory.decodeResource(getResources(),R.drawable.beard1);
        bitmaps = Bitmap.createScaledBitmap(bitmaps, bitmaps.getWidth()/3, bitmaps.getHeight()/3, true);
        Utils.bitmapToMat(bitmaps,beardImage2);
        recobtn1=(Button)view1.findViewById(R.id.recoBtn1);
        recobtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat recoImage = new Mat();
                if(idxs==0)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be0);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                else if(idxs==1)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be1);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==2)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be2);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==3)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be3);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==4)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be4);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==5)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be5);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                } else if(idxs==6)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be6);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==7)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==8)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be9);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                int a = recommend(outputImage2.getNativeObjAddr(),beardImage2.getNativeObjAddr(),recoImage.getNativeObjAddr());
                Bitmap bit = Bitmap.createBitmap(outputImage2.cols(),outputImage2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(outputImage2,bit);
                img.setImageBitmap(bit);            }
        });
        recobtn2=(Button)view2.findViewById(R.id.recoBtn2);
        recobtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat recoImage = new Mat();
                if(idxs==0)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be0);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                else if(idxs==1)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be1);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==2)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be2);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==3)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be3);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==4)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be4);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==5)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be5);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==6)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be6);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==7)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxs==8)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be9);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                int a = recommend(outputImage2.getNativeObjAddr(),beardImage2.getNativeObjAddr(),recoImage.getNativeObjAddr());
                Bitmap bit = Bitmap.createBitmap(outputImage2.cols(),outputImage2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(outputImage2,bit);
                img.setImageBitmap(bit);            }
        });
        recobtn3=(Button)view3.findViewById(R.id.recoBtn3);
        recobtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat recoImage = new Mat();
                if(idxt==0)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be0);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                else if(idxt==1)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be1);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==2)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be2);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==3)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be3);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==4)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be4);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==5)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be5);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==6)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be6);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==7)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==8)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be9);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                int a = recommend(outputImage2.getNativeObjAddr(),beardImage2.getNativeObjAddr(),recoImage.getNativeObjAddr());
                Bitmap bit = Bitmap.createBitmap(outputImage2.cols(),outputImage2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(outputImage2,bit);
                img.setImageBitmap(bit);
            }
        });
        recobtn4=(Button)view4.findViewById(R.id.recoBtn4);
        recobtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Mat recoImage = new Mat();
                if(idxt==0)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be0);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                else if(idxt==1)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be1);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==2)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be2);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==3)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be3);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==4)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be4);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==5)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be5);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==6)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be6);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==7)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else if(idxt==8)
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be7);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                } else
                {
                    Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(),R.drawable.be9);
                    bitmap2 = Bitmap.createScaledBitmap(bitmap2, bitmap2.getWidth()/3, bitmap2.getHeight()/3, true);
                    Utils.bitmapToMat(bitmap2,recoImage);
                }
                int a = recommend(outputImage2.getNativeObjAddr(),beardImage2.getNativeObjAddr(),recoImage.getNativeObjAddr());
                Bitmap bit = Bitmap.createBitmap(outputImage2.cols(),outputImage2.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(outputImage2,bit);
                img.setImageBitmap(bit);
            }
        });
        flipper.addView(view1);
        //flipper.addView(view2);
        flipper.addView(view3);
       // flipper.addView(view4);

        flipper.setOnTouchListener(new ViewFlipperAction(this, flipper));
    }
    //인덱스 업데이트
    @Override
    public void onFlipperActionCallback(int position) {
        Log.d("ddd", ""+position);
        for(int i=0; i<indexes.size(); i++){
            ImageView index = indexes.get(i);
            //현재화면의 인덱스 위치면 녹색
            if(i == position){
                index.setBackgroundColor(255);
            }
            //그외
            else{
                index.setBackgroundColor(1);
            }
        }
    }
}
