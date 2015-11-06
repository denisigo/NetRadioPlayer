package com.denisigo.netradioplayer;

import android.util.Log;

/**
 * Decoder used to decode mp3 stream to PCM data
 */
public class Decoder {
    private static final String TAG = Decoder.class.getSimpleName();

    // Parameters of the stream. Become available only after some data is decoded
    private int mSampleRate = -1;
    private int mChannels = -1;
    private int mBitsPerSample = -1;

    // Handle of underlying mpg123 decoder
    private int mHandle;

    /**
     * Decoder constructor.
     */
    public Decoder(){
        // We use native decoder library
        System.loadLibrary("decoder-jni");

        // Init native decoder
        mHandle = initNative();
        if (mHandle == 0)
            throw new IllegalStateException("Unable to create native decoder.");
    }

    /**
     * Close decoder and free resources
     */
    public void close(){
        closeNative(mHandle);
        mHandle = -1;
    }

    /**
     * Decode data to PCM
     * @param inBuffer byte[] buffer of raw data
     * @param size int size of actual data in inBuffer
     * @param outBuffer byte[] buffer where to place decoded data
     * @param maxSize int max size of decoded data wanted
     * @return int amount of bytes actually decoded
     */
    public int decode(byte[] inBuffer, int size, byte[] outBuffer, int maxSize){
        return decodeNative(mHandle, inBuffer, size, outBuffer, maxSize);
    }

    /**
     * Get sample rate of the stream.
     * Note: Actual only after some data decoded.
     * @return int sample rate
     */
    public int getRate(){
        return mSampleRate;
    }

    /**
     * Get number of channels of the stream.
     * Note: Actual only after some data decoded.
     * @return int mumber of channels
     */
    public int getChannels(){
        return mChannels;
    }

    /**
     * Get bits per sample of the stream.
     * Note: Actual only after some data decoded.
     * @return int bits per sample
     */
    public int getBitsPerSample(){
        return mBitsPerSample;
    }

    /**
     * Native callback. Being called from native library when stream's params were decoded
     * @param sample_rate int sample rate
     * @param channels int channels number
     * @param bits_per_sample int bits per sample
     */
    private void onNewFormatCallback(int sample_rate, int channels, int bits_per_sample){
        Log.d(TAG, "onNewFormatCallback: sample_rate:"+sample_rate+", channels: "+channels+", bits_per_sample: "+bits_per_sample);
        mSampleRate = sample_rate;
        mChannels = channels;
        mBitsPerSample = bits_per_sample;
    }

    /**
     * Init native decoder
     * @return int if positive - native decoder handle, zero if error
     */
    private native int initNative();

    /**
     * Close native decoder
     * @param handle native decoder handle
     */
    private native void closeNative(int handle);

    /**
     * Decode data on native decoder
     * @param handle native decoder handle
     * @param inBuffer see decode()
     * @param size see decode()
     * @param outBuffer see decode()
     * @param maxSize see decode()
     * @return see decode()
     */
    private native int decodeNative(int handle, byte[] inBuffer, int size, byte[] outBuffer, int maxSize);
}
