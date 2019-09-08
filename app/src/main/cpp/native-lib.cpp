#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

//#include <string>
//
//extern "C" JNIEXPORT jstring
//
//JNICALL
//Java_com_tistory_webnautes_useopencvwithcmake_MainActivity_stringFromJNI(
//        JNIEnv *env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_barber_PhotoActivity_imageprocessing(JNIEnv *env,
        jobject instance,
        jlong inputImage,
        jlong outputImage,
        jint th1,
        jint th2) {
    Mat &img_input = *(Mat *) inputImage;
    Mat &img_output = *(Mat *) outputImage;
    cvtColor( img_input, img_output, COLOR_RGB2GRAY);
    //blur( img_output, img_output, Size(5,5) );
    //Canny( img_output, img_output, th1, th2);
}

//extern "C"
//JNIEXPORT void JNICALL
//Java_com_example_barber_PhotoActivity_ConvertRGBtoGray(JNIEnv *env,
//                                                                            jobject instance,
//                                                                            jlong matAddrInput,
//                                                                            jlong matAddrResult) {
//
//
//
//    // 입력 RGBA 이미지를 GRAY 이미지로 변환
//
//    Mat &matInput = *(Mat *)matAddrInput;
//
//    Mat &matResult = *(Mat *)matAddrResult;
//
//
//    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
//
//
//
//}
