LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13


LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/res

LOCAL_CERTIFICATE := platform

include $(LOCAL_PATH)/version.mk
LOCAL_AAPT_FLAGS := \
        --auto-add-overlay \
        --version-name "$(version_name_package)" \
        --version-code $(version_code_package) \

LOCAL_PACKAGE_NAME := WestAlgoFussion

#compile opemcv
LOCAL_SRC_FILES += $(call all-java-files-under, ../../OpenCV/OpenCV-android-sdk/sdk/java/src)
LOCAL_SRC_FILES += ../../OpenCV/OpenCV-android-sdk/sdk/java/src/org/opencv/engine/OpenCVEngineInterface.aidl
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../../OpenCV/OpenCV-android-sdk/sdk/java/res
LOCAL_AAPT_FLAGS += --auto-add-overlay --extra-packages org.opencv.android:org.opencv.core:org.opencv.engine:org.opencv
#

LOCAL_SDK_VERSION := current

#LOCAL_PROGUARD_ENABLED := disabled
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_LDFLAGS += -fuse-ld=bfd
#LOCAL_JNI_SHARED_LIBRARIES := libjni_tinyplanet libjni_jpegutil  libopencv_java3
#LOCAL_JNI_SHARED_LIBRARIES := libjni_dof libdcs_dof
LOCAL_JNI_SHARED_LIBRARIES := libjni_supernight libdcs_rgb_mono
LOCAL_JAVA_LIBRARIES += org.apache.http.legacy

LOCAL_MULTILIB := 32

LOCAL_DEX_PREOPT = false

include $(BUILD_PACKAGE)

include $(call all-makefiles-under, $(LOCAL_PATH))
