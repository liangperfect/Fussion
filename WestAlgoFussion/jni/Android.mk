LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := libdcs_rgb_mono
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := libs/armeabi-v7a/$(LOCAL_MODULE).so
LOCAL_MODULE_STEM := $(LOCAL_MODULE)
LOCAL_MODULE_SUFFIX := $(suffix $(LOCAL_SRC_FILES))
LOCAL_MULTILIB := 32
LOCAL_MODULE_CLASS := SHARED_LIBRARIES
include $(BUILD_PREBUILT)

include $(CLEAR_VARS)
LOCAL_MODULE := libjni_supernight
LOCAL_SRC_FILES := com_westalgo_factorycamera_supernight_DcsSupernight.cpp
LOCAL_MULTILIB := 32
LOCAL_LDLIBS :=-llog
LOCAL_MODULE_TAGS := optional
LOCAL_SHARED_LIBRARIES := libdcs_rgb_mono
include $(BUILD_SHARED_LIBRARY)

