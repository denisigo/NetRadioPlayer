LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

# Pried at https://www.assembla.com/spaces/libmpg123-android and
# https://github.com/tulskiy/camomile

LOCAL_MODULE    := libmpg123
LOCAL_ARM_MODE  := arm
LOCAL_CFLAGS    += -O4 -Wall -DHAVE_CONFIG_H  \
	-fomit-frame-pointer -funroll-all-loops -finline-functions -ffast-math

LOCAL_CFLAGS += \
	-DACCURATE_ROUNDING \
	-DNO_REAL \
	-DNO_32BIT

LOCAL_SRC_FILES := \
	libmpg123/check_neon.S \
	libmpg123/compat.c \
	libmpg123/dct64.c \
	libmpg123/dither.c \
	libmpg123/equalizer.c \
	libmpg123/feature.c \
	libmpg123/format.c \
	libmpg123/frame.c \
	libmpg123/icy.c \
	libmpg123/icy2utf8.c \
	libmpg123/id3.c \
	libmpg123/index.c \
	libmpg123/layer1.c \
	libmpg123/layer2.c \
	libmpg123/layer3.c \
	libmpg123/libmpg123.c \
	libmpg123/ntom.c \
	libmpg123/optimize.c \
	libmpg123/parse.c \
	libmpg123/readers.c \
	libmpg123/stringbuf.c \
	libmpg123/synth.c \
	libmpg123/synth_8bit.c \
	libmpg123/tabinit.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_CFLAGS += -mfloat-abi=softfp -mfpu=neon -DOPT_NEON -DREAL_IS_FLOAT
	LOCAL_SRC_FILES += 	libmpg123/synth_neon.S \
	                    libmpg123/synth_neon_accurate.S \
						libmpg123/synth_stereo_neon.S \
						libmpg123/synth_stereo_neon_accurate.S \
						libmpg123/dct64_neon.S \
						libmpg123/dct64_neon_float.S \
						libmpg123/dct36_neon.S
else
	LOCAL_CFLAGS += -DOPT_ARM -DREAL_IS_FIXED
	LOCAL_SRC_FILES +=  libmpg123/getcpuflags_arm.c \
	                    libmpg123/synth_arm.S \
	                    libmpg123/synth_arm_accurate.S
endif

include $(BUILD_STATIC_LIBRARY)