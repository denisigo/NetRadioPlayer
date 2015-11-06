LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE  := arm
LOCAL_MODULE    := libdecoder-jni
LOCAL_SRC_FILES := libdecoder-jni.c
LOCAL_SHARED_LIBRARIES := libmpg123
LOCAL_CFLAGS += -O4 -std=c99
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)