#include <stdio.h>
#include "jni.h"
#include <android/log.h>
#include "include/dcs_supernight.h"
#include "include/com_westalgo_factorycamera_supernight_DcsSupernight.h"

#define TAG "dcs_supernight_jni"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__)


dcs_img_Y_UV_buf mainBuf;
dcs_img_Y_UV_buf auxBuf;
images_parameters_t p;

JNIEXPORT jint JNICALL  Java_com_westalgo_factorycamera_supernight_DcsSupernight_init(JNIEnv *env, jclass thiz,jint width,jint height){
    LOGE("dcs_supernight_jni init");
	return dcs_supernight_init(width,height);
}

JNIEXPORT jint JNICALL Java_com_westalgo_factorycamera_supernight_DcsSupernight_setParameters(JNIEnv *env,jclass thiz,jint rgbIso,jint monoIso){
	LOGE("dcs_supernight_jni  setParameters");
	p.rgb_iso = rgbIso;
	p.mono_iso = monoIso;
	LOGE("dcs_supernight_jni  setParameters:ret %d",dcs_supernight_setParameters(&p));
	return dcs_supernight_setParameters(&p);
}

JNIEXPORT jint JNICALL Java_com_westalgo_factorycamera_supernight_DcsSupernight_setImagePair(JNIEnv *env,jclass thiz,jbyteArray mainY,jbyteArray mainUV,jint mainW,jint mainH,jint mainFormat,jint mainRotation,jint main_s0,jint main_s1,
jbyteArray auxY,jbyteArray auxUV,jint auxW,jint auxH,jint auxForamt,jint auxRotation,jint aux_s0,jint aux_s1){
	//rgb data
	LOGE("dcs_supernight_jni  setImagePair");
	unsigned char* mainYdata = (unsigned char*)env->GetByteArrayElements(mainY,0);
	unsigned char* mainUVdata = (unsigned char*)env->GetByteArrayElements(mainUV,0);
	mainBuf.yData = mainYdata;
	mainBuf.uvData = mainUVdata;
	mainBuf.width = mainW;
	mainBuf.height = mainH;
	mainBuf.format = mainFormat;
	mainBuf.rotation = mainRotation;
	mainBuf.stride[0] = main_s0;
	mainBuf.stride[1] = main_s1;


	//mono data
	unsigned char* auxYdata = (unsigned char*)env->GetByteArrayElements(auxY,0);
	unsigned char* auxUVdata = (unsigned char*)env->GetByteArrayElements(auxUV,0);


	auxBuf.yData = auxYdata;
	auxBuf.uvData = auxUVdata;
	auxBuf.width = auxW;
	auxBuf.height = auxH;
	auxBuf.format = auxForamt;
	auxBuf.rotation = auxRotation;
	auxBuf.stride[0] = aux_s0;
	auxBuf.stride[1] = aux_s1;
	//LOGD("dcs_supernight_setImagePair %p %d %d %p %d %d",mainYdata,mainW,mainH,auxYdata,auxW,auxH);
	env->ReleaseByteArrayElements(mainY, (jbyte *)mainYdata, 0);
    env->ReleaseByteArrayElements(mainUV, (jbyte *)mainUVdata, 0);
	env->ReleaseByteArrayElements(auxY, (jbyte *)auxYdata, 0);
    env->ReleaseByteArrayElements(auxUV, (jbyte *)auxUVdata, 0);


	return 0;
}


JNIEXPORT jint JNICALL Java_com_westalgo_factorycamera_supernight_DcsSupernight_generate(JNIEnv *env,jclass thiz,jbyteArray supernightY,jbyteArray supernightUV){
	dcs_img_Y_UV_buf supernightBuf;
	unsigned char* supernightYData = (unsigned char*)env->GetByteArrayElements(supernightY, 0);
	unsigned char* supernightUVData = (unsigned char*)env->GetByteArrayElements(supernightUV, 0);
	supernightBuf.yData = supernightYData;
	LOGE("dcs_supernight_jni generate");
	supernightBuf.uvData = supernightUVData;
	supernightBuf.width = mainBuf.width;
	supernightBuf.height = mainBuf.height;
	supernightBuf.format = COLOR_FORMAT_YUV_NV21;
	supernightBuf.rotation = 0;
	supernightBuf.stride[0] = -1;
	supernightBuf.stride[1] = -1;
	LOGE("dcs_supernight_jni  generate");
	//LOGI("dcs_supernight_jni start to process");
	int ret = dcs_supernight_process(&mainBuf,&auxBuf,&supernightBuf,false);
	//LOGI("dcs_supernight_jni end to process");
	env->ReleaseByteArrayElements(supernightY, (jbyte *)supernightYData, 0);
	env->ReleaseByteArrayElements(supernightUV, (jbyte *)supernightUVData, 0);
	return ret;
}

JNIEXPORT jint JNICALL Java_com_westalgo_factorycamera_supernight_DcsSupernight_unInit(JNIEnv *env, jclass thiz){
    LOGE("dcs_supernight_jni  unInit");
    return dcs_supernight_uninit();
}

JNIEXPORT jstring JNICALL Java_com_westalgo_factorycamera_supernight_DcsSupernight_getVersion(JNIEnv *env, jclass thiz){
    LOGE("dcs_supernight_jni  getVersion");
    return  env->NewStringUTF(dcs_supernight_getVersion());
}

/*
static const char *classPathName = "com/westalgo/factorycamera/supernight/DcsSupernight";
static JNINativeMethod methods[] = {
  {"init", "(II)I", (void*)init },
  {"setParameters", "(II)I", (void*)setParameters },
  {"setImagePair", "([B[BIIIIII[B[BIIIIII)I", (void*)setImagePair },
  {"generate", "([B[B)I", (void*)generate },
  //{"unDcsInit", "()I", (void*)unInit },
  {"getVersion", "()Ljava/lang/String;", (void*)getVersion },
};
//public static native int unInit();
/*
 * Register several native methods for one class.
 */
 /*
static int registerNativeMethods(JNIEnv* env, const char* className,
        JNINativeMethod* gMethods, int numMethods) {
    jclass clazz;
    LOGE("westalgo:jni registerNativeMethods className:%s ,numMethods:%d",className,numMethods);
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        LOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Register native methods for all classes we know about.
 *
 * returns JNI_TRUE on success.
 */
 /*
static int registerNatives(JNIEnv* env) {
  LOGE("westalgo:jni registerNatives ");
  if (!registerNativeMethods(env, "com/westalgo/factorycamera/supernight/DcsSupernight",
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

// ----------------------------------------------------------------------------

/*
 * This is called by the VM when the shared library is first loaded.
 */

typedef union {
    JNIEnv* env;
    void* venv;
} UnionJNIEnvToVoid;

jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    UnionJNIEnvToVoid uenv;
    uenv.venv = NULL;
    jint result = -1;
    JNIEnv* env = NULL;

    LOGI("JNI_OnLoad");

    if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_4) != JNI_OK) {
        LOGE("ERROR: GetEnv failed");
        goto bail;
    }
    env = uenv.env;

   // if (registerNatives(env) != JNI_TRUE) {
   //     LOGE("ERROR: registerNatives failed");
   //     goto bail;
  //  }

    result = JNI_VERSION_1_4;

bail:
    return result;
}
