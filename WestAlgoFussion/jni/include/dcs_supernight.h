#ifndef __DCS_SUPERNIGHT_H__
#define __DCS_SUPERNIGHT_H__

/****************************************************************
* Color format define
*****************************************************************/
#define COLOR_FORMAT_YUV_NV21   0x16
#define COLOR_FORMAT_YUV_YV12   0x18
////////////////////////////////////////////////////////////////////////////////////////////
//Parameters
//yData: y channel data, size is width * height
//uvData: uv channel data, size is (width * (height >> 1))
//width: Image width
//height: Image height
//format: Now support YUV_NV21
//rotation: no use
//stride[2]: stride[0] is aligned image width, stride[2] is aligned image height
////////////////////////////////////////////////////////////////////////////////////////////
//#ifdef __cplusplus
//extern "C" {
//#endif
typedef struct {
unsigned char *yData;
unsigned char *uvData;
int width;
int height;
int format;
int rotation;
int stride[2];
}dcs_img_Y_UV_buf;

////////////////////////////////////////////////////////////////////////////////////////////
//Image parameters to do super night algorithm.
//We need to input rgb_iso and mono_iso, algorithm will automate handle other parameters.
//
//@rgb_iso:              do rgb denoise according to rgb's iso
//@mono_iso:             do mono denoise according to mono's iso
////////////////////////////////////////////////////////////////////////////////////////////
 struct images_parameters_t {
      int rgb_iso;
      int mono_iso;
 };



////////////////////////////////////////////////////////////////////////////////////////////
    //Init super night algorithm and opencl  environment. Now maxmimum supprot 4160x3136 image size.
    //Only need init one time.
    ////////////////////////////////////////////////////////////////////////////////////////////
    int dcs_supernight_init(int width,int height);

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Set related parameters for super night algorithm.
    //
    //@param:               parameters which super night algorithm needs
    //@return:              0 for success, otherwise failure
    ////////////////////////////////////////////////////////////////////////////////////////////
    int dcs_supernight_setParameters(images_parameters_t* param);

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Get parameters from super night algorithm.
    //
    //@return:               return parameters
    ////////////////////////////////////////////////////////////////////////////////////////////
    images_parameters_t* dcs_supernight_getParameters();

     ////////////////////////////////////////////////////////////////////////////////////////////
     //Update the input image size if the input image width/height changed. Call it before dcs_supernight_process.
     //@param:
     //          width: input image width.
     //          height: input image height.
     //@return:  0 for success, otherwise failure.
     ////////////////////////////////////////////////////////////////////////////////////////////
     int dcs_supernight_updateImageSize(int width, int height);

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Process rgb and mono images to generate fusion images.
    //Because it takes time to complete the image process, it is strongly suggested that
    //the caller of this function be put to a seperated thread to avoid blocking the main thread.
    //
    //@rgb[IN]:             Rgb image buffer
    //@mono[IN]:            Mono image buffer
    //@output[OUT]:         The fusion image buffer
    //@return:              0 for success, otherwise failure
    ////////////////////////////////////////////////////////////////////////////////////////////
    int dcs_supernight_process(const dcs_img_Y_UV_buf* rgb, const dcs_img_Y_UV_buf* mono, dcs_img_Y_UV_buf* output, bool needCutOutput);

    ////////////////////////////////////////////////////////////////////////////////////////////
    //Release super night algorithm environment.
    //
    //@return:              0 for success, otherwise failure
    ////////////////////////////////////////////////////////////////////////////////////////////
    int dcs_supernight_uninit();

    ////////////////////////////////////////////////////////////////////////////////////////////
    //get supernight algorithm version
    //@return:              version char[]
    ////////////////////////////////////////////////////////////////////////////////////////////
    const char* dcs_supernight_getVersion();

    void dcs_symbol();
//#ifdef __cplusplus
//}
//#endif

#endif