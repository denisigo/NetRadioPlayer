LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE  := arm
LOCAL_MODULE    := libmpg123-jni
LOCAL_SRC_FILES := libmpg123-jni.c
LOCAL_SHARED_LIBRARIES := libmpg123
LOCAL_CFLAGS += -O4 -std=c99
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)