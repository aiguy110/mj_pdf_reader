LOCAL_PATH := $(call my-dir)

#Prebuilt library (bblanchon/pdfium-binaries, self-contained: statically links
#freetype/libpng/c++_shared, only depends on libc/libm/libdl)
include $(CLEAR_VARS)
LOCAL_MODULE := pdfium

ARCH_PATH = $(TARGET_ARCH_ABI)

LOCAL_SRC_FILES := $(LOCAL_PATH)/lib/$(ARCH_PATH)/libpdfium.so

include $(PREBUILT_SHARED_LIBRARY)

#Main JNI library
include $(CLEAR_VARS)
LOCAL_MODULE := jniPdfium

LOCAL_CFLAGS += -DHAVE_PTHREADS
LOCAL_C_INCLUDES += $(LOCAL_PATH)/include
LOCAL_SHARED_LIBRARIES += pdfium
LOCAL_LDLIBS += -llog -landroid -ljnigraphics
#Force 16 KB page size ELF alignment (see https://developer.android.com/guide/practices/page-sizes)
LOCAL_LDFLAGS += -Wl,-z,max-page-size=16384

LOCAL_SRC_FILES :=  $(LOCAL_PATH)/src/mainJNILib.cpp

include $(BUILD_SHARED_LIBRARY)
