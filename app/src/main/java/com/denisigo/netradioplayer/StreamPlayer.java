package com.denisigo.netradioplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class responsible for connecting to station host,
 * decode and write to AudioTrack.
 */
public class StreamPlayer extends Thread{
    private static final String TAG = StreamPlayer.class.getSimpleName();

    // Size of the buffer to read from input stream at once, bytes
    private static final int IN_BUFFER_SIZE = 1024;
    // Size of the buffer to store decoded data, bytes
    private static final int OUT_BUFFER_SIZE = 1024*10;

    private URL mURL;
    private Decoder mDecoder;
    private AudioTrack mAudioTrack;
    private HttpURLConnection mConnection;
    private boolean mIsInterrupted = false;
    private IStreamPlayerListener mListener;
    private BufferedInputStream mInputStream;

    /**
     * Listener for StreamPlayer events
     */
    public interface IStreamPlayerListener{
        /**
         * If some error occurred
         * @param message String message
         */
        void onError(String message);

        /**
         * On player status update
         * @param totalBytesRead amount of total bytes read
         * @param bytesCached amount of bytes cached
         */
        void onStatus(long totalBytesRead, int bytesCached);
    }

    /**
     * Private constructor for StreamPlayer. Use launch() instead.
     * @param url URL instance to play from
     * @param listener IStreamPlayerListener instance
     */
    private StreamPlayer(URL url, IStreamPlayerListener listener){
        mURL = url;
        mListener = listener;
    }

    /**
     * Launches the StreamPlayer to play specified url.
     * @param url URL instance
     * @param listener IStreamPlayerListener instance to receive player events
     * @return StreamPlayer instance
     */
    public static StreamPlayer launch(URL url, IStreamPlayerListener listener){
        StreamPlayer streamPlayer = new StreamPlayer(url, listener);
        streamPlayer.start();
        return streamPlayer;
    }

    /**
     * Closes player.
     */
    public void close(){
        mIsInterrupted = true;
        interrupt();
    }

    /**
     * Init AudioTrack with given parameters.
     * @param sampleRate int sample rate
     * @param channelCount int channels count
     * @param bitsPerSample int bits per sample - either 8 or 16
     */
    private void initAudioTrack(int sampleRate, int channelCount, int bitsPerSample){
        int channels = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        int bps = bitsPerSample == 8 ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT;

        // Determine minimum buffer size to handle this kind of stream
        int minBufSize = AudioTrack.getMinBufferSize(sampleRate, channels, bps);

        // Create AudioTrack with given params to play to MUSIC stream and in STREAM mode to be
        // able to feed it with data as it arrives from socket
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate,
                channels,
                bps,
                minBufSize,
                AudioTrack.MODE_STREAM);
    }

    /**
     * Frees all the player's data
     */
    private void free(){
        if (mConnection != null)
            mConnection.disconnect();
        mConnection = null;
        if (mInputStream != null)
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        mInputStream = null;
        if (mDecoder != null)
            mDecoder.close();
        mDecoder = null;
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.stop();
        }
        mAudioTrack = null;
    }

    /**
     * Handy method to handle errors
     * @param message String message
     * @param exception Exception instance
     */
    private void onError(String message, Exception exception){
        if (exception != null) {
            message += ":" + exception;
            exception.printStackTrace();
        }
        Log.e(TAG, message);
        mListener.onError(message);
    }

    /**
     * Handly method to handle errors
     * @param message String message
     */
    private void onError(String message){
        onError(message, null);
    }

    /**
     * Main method where all the stuff takes place.
     * We added this method to be able to nicely handle errors in run()
     */
    private void main(){
        // At first try to establish a HTTPUrlConnection to the host
        int responseCode;
        String responseMessage;
        try {
            mConnection = (HttpURLConnection) mURL.openConnection();
            responseCode = mConnection.getResponseCode();
            responseMessage = mConnection.getResponseMessage();
        } catch (IOException e) {
            onError("Unable to open HttpURLConnection", e);
            return;
        }

        // Check if server returned 200 OK status
        if (responseCode != 200){
            onError("URL returned not 200 OK HTTP status: " + responseCode + " " + responseMessage);
            return;
        }

        // Currently we support only audio/mpeg format
        String contentType = mConnection.getContentType();
        if (!contentType.equals("audio/mpeg")){
            onError("Unsupported content type: " + contentType);
            return;
        }

        // NOTE: in this version of app, we don't use any buffer so if your internet connection
        // is not good enough, you might face interruptions in playback

        // Create Buffered input stream to optimise stream reads. BufferedInputStream attempts to read
        // as many data as possible in source stream even if you're reading single byte.
        try {
            mInputStream = new BufferedInputStream(mConnection.getInputStream(), IN_BUFFER_SIZE * 10);
        } catch (IOException e) {
            onError("Unable to create BufferedInputStream", e);
            return;
        }

        // Create decoder to decode stream data to PCM which is suitable for AudioTrack
        mDecoder = new Decoder();

        int bytesRead = 0;
        int bytesDecoded = 0;
        long bytesReadTotal = 0;
        int bytesCached = 0;
        byte[] inBuffer = new byte[IN_BUFFER_SIZE];
        byte[] outBuffer = new byte[OUT_BUFFER_SIZE];

        // Main loop where we read data from mInputStream, decode it and write to AudioTrack
        while (!mIsInterrupted){
            // Read data from mInputStream to inBuffer
            try {
                bytesRead = mInputStream.read(inBuffer, 0, IN_BUFFER_SIZE);
                bytesCached = mInputStream.available();
            } catch (IOException e) {
                onError("Unable to read stream", e);
                return;
            }

            Log.d(TAG, "bytesRead: " + bytesRead + " cached: " + bytesCached);

            // We can get less bytes than we actually want, handle all the cases
            // If we didn't received any data, it means we might need some time to wait
            if (bytesRead == 0){
                continue;
            }
            // If stream returned -1 it means there will be no more data since socket is closed
            else if (bytesRead == -1) {
                onError("Reached the end of the stream");
                return;
            }
            // If we received some bytes, go and decode them
            else if (bytesRead > 0){
                bytesReadTotal += bytesRead;

                // Write received data to the decoder
                bytesDecoded = mDecoder.decode(inBuffer, bytesRead, outBuffer, OUT_BUFFER_SIZE);

                // Decoder also may not give us any data as we want it
                // If bytes decoded is zero, it means decoder want more data, so just continue reading
                if (bytesDecoded == 0){
                    continue;
                }
                // If decoder returned negative number, it means error occurred
                else if (bytesDecoded < 0){
                    onError("Error while decoding the stream: " + bytesDecoded);
                    return;
                }
                // Elsewhere we have decoded some real audio data, write it to audio track
                else {
                    // If StreamPlayer just started we need to init AudioTrack.
                    // We can't create it before since we have
                    // correct stream params only after we decoded some data
                    if (mAudioTrack == null) {
                        // Check if we if have supported stream
                        if (mDecoder.getBitsPerSample() != -1) {
                            initAudioTrack(mDecoder.getRate(), mDecoder.getChannels(),
                                    mDecoder.getBitsPerSample());
                            mAudioTrack.play();
                        } else {
                            onError("Stream has invalid bits per sample");
                            return;
                        }
                    } else {
                        // Finally write PCM buffer to AudioTrack. This call will block until
                        // all the data is written
                        mAudioTrack.write(outBuffer, 0, bytesDecoded);
                        mListener.onStatus(bytesReadTotal, bytesCached);
                    }
                }
            }
        }
    }

    @Override
    public void run() {
        // We must handle all possible errors and free resources
        try{
            main();
        } finally {
            Log.d(TAG, "StationPlayer thread is stopping, free resources...");
            free();
        }
    }
}
