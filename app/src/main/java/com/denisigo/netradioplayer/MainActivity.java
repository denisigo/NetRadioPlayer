package com.denisigo.netradioplayer;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {
    private EditText mUrlView;
    private TextView mErrorView;
    private TextView mStatusView;

    private StreamPlayer mStreamPlayer;
    private StreamPlayer.IStreamPlayerListener mStreamPlayerListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUrlView = (EditText) findViewById(R.id.url);
        mErrorView = (TextView) findViewById(R.id.error);
        mStatusView = (TextView) findViewById(R.id.status);

        // Set default station URL
        //mUrlView.setText("http://pub5.radiotunes.com:80/radiotunes_hit70s");
        mUrlView.setText("http://pub7.rockradio.com:80/rr_60srock");

        // Create StreamPlayer listener to update interface
        mStreamPlayerListener = new StreamPlayer.IStreamPlayerListener() {
            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mErrorView.setText(message);
                    }
                });
            }

            @Override
            public void onStatus(final long totalBytesRead, final int bytesCached) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStatusView.setText(String.format("Total bytes read: %d\r\nBytes cached: %d",
                                totalBytesRead, bytesCached));
                    }
                });
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop player if still playing
        if (mStreamPlayer != null)
            mStreamPlayer.close();
    }

    /**
     * Handle on Play button click
     * @param view View instance
     */
    public void onPlayButtonClick(View view){
        // Stop previous player if exists
        if (mStreamPlayer != null)
            mStreamPlayer.close();

        mErrorView.setText("");
        mStatusView.setText("");

        // Prepare URL to play from
        URL url = null;
        try {
            url = new URL(mUrlView.getText().toString());
        } catch (MalformedURLException e) {
            mErrorView.setText("Malformed URL");
            return;
        }

        // Launch a new player.
        // Note: We just launch a new player for the sake of simplicity.
        mStreamPlayer = StreamPlayer.launch(url, mStreamPlayerListener);
    }

    /**
     * Handle on Stop button click
     * @param view View instance
     */
    public void onStopButtonClick(View view){
        if (mStreamPlayer != null)
            mStreamPlayer.close();
        mStreamPlayer = null;
    }
}
