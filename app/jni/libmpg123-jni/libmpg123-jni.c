#include <string.h>
#include <jni.h>
#include "../libmpg123/libmpg123/mpg123.h"
#include <stdio.h>
#include <android/log.h>

#define LOG(msg...) __android_log_print(ANDROID_LOG_DEBUG, "libmpg123-jni", msg)
#define LOGE(msg...) __android_log_print(ANDROID_LOG_ERROR, "libmpg123-jni", msg)

static jmethodID method_onNewFormatCallback;

/**
Init native decoder by initializing mpg123 and Java callbacks.
*/
jint Java_com_denisigo_netradioplayer_Decoder_initNative(JNIEnv* env, jobject thiz){
    int ret;

    // Init mpg123 decoder
    mpg123_init();
    mpg123_handle* hnd = mpg123_new(NULL, &ret);
    if(hnd == NULL) {
        LOGE("Unable to create mpg123 handle: %s\n", mpg123_plain_strerror(ret));
        return 0;
    }

    // Set mpg123 to feed mode since we're providing data ourseves
    ret = mpg123_open_feed(hnd);
    if(ret != MPG123_OK)
    {
        LOGE("Unable open feed: %s\n", mpg123_plain_strerror(ret));
        return 0;
    }

    // Init Java callback
    jclass thisClass = (*env)->GetObjectClass(env, thiz);
    method_onNewFormatCallback =
            (*env)->GetMethodID(env, thisClass, "onNewFormatCallback", "(III)V");

    return (jint) hnd;
}

/**
Closes native decoder
*/
jint Java_com_denisigo_netradioplayer_Decoder_closeNative(JNIEnv* env, jobject thiz, jint handle){
    mpg123_handle* hnd = (mpg123_handle*) handle;
    mpg123_close(hnd);
}

/**
Decodes given buffer of data to PCM
*/
jint Java_com_denisigo_netradioplayer_Decoder_decodeNative(JNIEnv* env, jobject thiz, jint handle,
                            jbyteArray in_buffer, jint size, jbyteArray out_buffer, jint max_size){
    jint ret;

    mpg123_handle* hnd = (mpg123_handle*) handle;
    jbyte* in_buf = (*env)->GetByteArrayElements(env, in_buffer, NULL);
    jbyte* out_buf = (*env)->GetByteArrayElements(env, out_buffer, NULL);

    size_t bytes_decoded;
    // Decode data
    ret = mpg123_decode(hnd, in_buf, size, out_buf, max_size, &bytes_decoded);
    LOG("Decoded %d bytes, ret: %d, max_size: %d", bytes_decoded, ret, max_size);

    // If status if OK, decoder decoded something
    if (ret == MPG123_OK){
        ret = bytes_decoded;
    }
    // This means decoder faced new stream. For us this means that there was sufficient data decoded
    // to determine stream's params and we can notify Java Decoder about it
    else if (ret == MPG123_NEW_FORMAT){
        long rate;
        int channels, encoding;
        // Get actual format params
        mpg123_getformat(hnd, &rate, &channels, &encoding);
        //LOG("mpg123_getformat: rate: %d channels: %d encoding: %d\n", rate, channels, encoding);

        // We support only 16 and 8 bits per sample
        if (encoding == MPG123_ENC_SIGNED_16)
            encoding = 16;
        else if (encoding == MPG123_ENC_SIGNED_8)
            encoding = 8;
        else
            encoding = -1;

        (*env)->CallVoidMethod(env, thiz, method_onNewFormatCallback, (jint)rate, (jint)channels,
                            (jint)encoding);

        ret = 0;
    }
    // This means mpg123 decoder needs more data to provide decoded data
    else if (ret == MPG123_NEED_MORE){
        ret = 0;
    }
    // Other cases usually means some errors
    else {
        // Ensure we will not return positive number which is normally indicating num of decoded bytes
        if (ret > 0)
            ret *= -1;
    }

    (*env)->ReleaseByteArrayElements(env, in_buffer, in_buf, 0);
    (*env)->ReleaseByteArrayElements(env, out_buffer, out_buf, 0);

    return ret;
}
