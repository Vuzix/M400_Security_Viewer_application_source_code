/*
Copyright (c) 2019, Vuzix Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

*  Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

*  Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

*  Neither the name of Vuzix Corporation nor the names of
   its contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.vuzix.securityviewer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vuzix.hud.actionmenu.ActionMenuActivity;
import com.vuzix.hud.actionmenu.DefaultActionMenuItemView;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.OnvifMediaProfilesListener;
import be.teletask.onvif.listeners.OnvifMediaStreamURIListener;
import be.teletask.onvif.listeners.OnvifResponseListener;
import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifMediaProfile;
import be.teletask.onvif.responses.OnvifResponse;

import static android.view.KeyEvent.KEYCODE_DPAD_DOWN;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_UP;

public class StreamActivity extends ActionMenuActivity implements MediaPlayer.EventListener, IVLCVout.Callback {

    private static final String TAG = "StreamActivity";
    private String cameraName;

    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private TextView headerPageTitle;
    private View displayShield;
    private ProgressBar pbLoading;
    private LibVLC mLibVLC;
    private boolean screenOn = true;

    private MenuItem displayBrightnessMenuItem;
    private MenuItem aspectRationMenuItem;
    private MenuItem audioControlOption;

    private float screenBrightness;
    private boolean screenBrightnessAdjustable = true;

    private final int ASPECT_FIT = 0;
    private final int ASPECT_FULL = 1;
    private final int ASPECT_ZOOM = 2;
    private int nextAspect = ASPECT_FULL;

    private final static int MAX_DISPLAY_BRIGHTNESS = 255;
    private final static int MENU_BRIGHTNESS = 1;

    public final static String EXTRA_HOSTNAME = "hostname";
    public final static String EXTRA_USERNAME = "username";
    public final static String EXTRA_PASSWORD = "password";
    public final static String EXTRA_CAMERA_NAME = "cameraName";

    private String hostName;
    private String username;
    private String password;

    /**
     * Called to initially set the views and begin initialize VLC
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        try{
            int settingsScreenBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            screenBrightness = MAX_DISPLAY_BRIGHTNESS/ settingsScreenBrightness;
            Settings.System.putInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);



        }catch (Settings.SettingNotFoundException e){
            screenBrightnessAdjustable = false;
        }

        Log.d(TAG, "Screen brightness: " + screenBrightness);

        setContentView(R.layout.activity_stream);

        mSurfaceView = findViewById(R.id.sv_stream);
        headerPageTitle = findViewById(R.id.header_page_title);
        displayShield = findViewById(R.id.stream_displayshield);
        pbLoading = findViewById(R.id.pb_stream);

        mSurfaceView.setKeepScreenOn(true);
        hostName = getIntent().getStringExtra(EXTRA_HOSTNAME);
        username = getIntent().getStringExtra(EXTRA_USERNAME);
        password = getIntent().getStringExtra(EXTRA_PASSWORD);
        cameraName = getIntent().getStringExtra(EXTRA_CAMERA_NAME);
        headerPageTitle.setText(cameraName);

        registerReceiver(voiceCmdReceiver, new IntentFilter(VuzixSpeechClient.ACTION_VOICE_COMMAND));
        this.initVoiceVocabulary();
        LoadStreamURLTask loadStreamURLTask = new LoadStreamURLTask(this);
        loadStreamURLTask.execute();
        initVLC();
    }

    /**
     * Used to show VLC if screen fell asleep while hiding
     */
    @Override
    protected void onResume() {
        toggleDisplay(true);
        super.onResume();
    }

    /**
     * Used to gracefully stop VLC when activity is being destroyed
     */
    @Override
    protected void onDestroy() {
        mMediaPlayer.stop();
        unregisterReceiver(voiceCmdReceiver);
        super.onDestroy();
    }

    /**
     * Called to create the action menu with our viewer menu
     * @param menu Menu to inflate menu to
     * @return True to inflate
     */
    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.stream, menu);

        audioControlOption = menu.findItem(R.id.menu_item_stream_audio);
        displayBrightnessMenuItem = menu.findItem(R.id.menu_item_stream_brightness);
        aspectRationMenuItem = menu.findItem(R.id.menu_item_stream_aspect);
        updateBrightnessMenuItem();
        updateAudioMenuItem(); //called to check global audio setting
        return true;
    }

    /**
     * mutes mediaplayer
     */
    private void muteAudioPlayer(SharedPreferences.Editor editor, MenuItem item) {
        final String mute = getString(R.string.menu_item_settings_stream_audio_mute);
        mMediaPlayer.setVolume(0);
        editor.putString("volState", "0");
        item.setTitle(mute);
        item.setIcon(R.drawable.ic_mic_off_24px);
        editor.apply();
    }

    /**
     * un-mute mediaplayer
     */
    private void unmuteAudioPlayer(SharedPreferences.Editor editor, MenuItem item) {
        final String listen = getString(R.string.menu_item_settings_stream_audio_listen);
        mMediaPlayer.setVolume(100);
        editor.putString("volState", "1");
        item.setTitle(listen);
        item.setIcon(R.drawable.ic_mic_24px);
        editor.apply();
    }

    /**
     * Handler for audio control settings
     */
    public void streamAudioOnClick(MenuItem item) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        if (pref.contains("volState") && pref.getString("volState", null) != null) {
            String opt = pref.getString("volState", null);
            if ("0".equals(opt)) {
                unmuteAudioPlayer(editor, item);
            } else {
                muteAudioPlayer(editor, item);
            }
        }
        editor.apply();
    }

    /**
     * Checks audio settings to show/hide controls
     */
    protected void updateAudioMenuItem() {
        final String off = getString(R.string.menu_item_settings_camera_audio_OFF);
        final String on = getString(R.string.menu_item_settings_camera_audio_ON);
        final String manual = getString(R.string.menu_item_settings_camera_audio_MANUAL);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        if (pref.contains("audioState") && pref.getString("audioState", null) != null) {
            String opt = pref.getString("audioState", null);
            if (off.equals(opt)) {
                muteAudioPlayer(editor, audioControlOption);
                audioControlOption.setVisible(false);
            } else if (manual.equals(opt)) {
                muteAudioPlayer(editor, audioControlOption);
                audioControlOption.setVisible(true);
            } else {
                unmuteAudioPlayer(editor, audioControlOption);
                audioControlOption.setVisible(true);
            }
        }
        editor.apply();
    }

    /**
     * Overrides the default selected action menu item
     * @return 1 for display brightness
     */
    @Override
    protected int getDefaultAction() {
        return MENU_BRIGHTNESS;
    }

    /**
     * Called when "Refresh" is pressed, initializes VLC and replays video
     * @param item Menu item that was pressed
     */
    public void refreshVideoPlayer(MenuItem item){
        mMediaPlayer.stop();
        mSurfaceView.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
        initVLC();
        LoadStreamURLTask loadStreamURLTask = new LoadStreamURLTask(this);
        loadStreamURLTask.execute();
    }

    /**
     * Called when an aspect ratio adjustment has been pressed
     * @param item Menu item that was pressed
     */
    public void onAspectRatioClicked(MenuItem item){
        switch (nextAspect){
            case ASPECT_FIT:
                aspectRationMenuItem.setTitle(R.string.menu_item_stream_aspect_fit);
                aspectRationMenuItem.setIcon(R.drawable.ic_aspect_fit);
                nextAspect = ASPECT_FULL;
                adjustAspectRatio();
                break;
            case ASPECT_FULL:
                aspectRationMenuItem.setTitle(R.string.menu_item_stream_aspect_full);
                aspectRationMenuItem.setIcon(R.drawable.ic_aspect_full);
                nextAspect = ASPECT_ZOOM;
                adjustAspectRatio();
                break;
            case ASPECT_ZOOM:
                aspectRationMenuItem.setTitle(R.string.menu_item_stream_aspect_zoom);
                aspectRationMenuItem.setIcon(R.drawable.ic_aspect_zoom);
                nextAspect = ASPECT_FIT;
                adjustAspectRatio();
                break;
        }
    }

    /**
     * Initially sets aspect ratio with previously stored value
     */
    private void adjustAspectRatio(){
        switch (nextAspect){
            case ASPECT_FIT:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(0.4f);
                break;
            case ASPECT_FULL:
                mMediaPlayer.setAspectRatio(null);
                mMediaPlayer.setScale(0);
                break;
            case ASPECT_ZOOM:
                mMediaPlayer.setAspectRatio("1:1");
                break;
        }
    }

    /**
     * Called when display brightness toggle has been pressed
     * @param item Menu item that was pressed
     */
    public void toggleDisplayBrightness(MenuItem item){
        if(screenBrightness >= 0.5){
            maxDisplayBrightness(false);
        }else{
            maxDisplayBrightness(true);
        }
    }

    /**
     * Updates the display brightness menu item after it was pressed or a gesture was recognized
     */
    private void updateBrightnessMenuItem(){
        if(screenBrightness >= 0.5){
            displayBrightnessMenuItem.setTitle(R.string.menu_item_stream_brightness_high);
            displayBrightnessMenuItem.setIcon(R.drawable.ic_brightness_high);
        }else{
            displayBrightnessMenuItem.setTitle(R.string.menu_item_stream_brightness_low);
            displayBrightnessMenuItem.setIcon(R.drawable.ic_brightness_low);
        }
    }

    /**
     * Called to initialize the VLC display with pre-set settings
     */
    private void initVLC() {
        ArrayList<String> options = new ArrayList<>();
        options.add("-vvv"); // Used to enable verbose logging for VLC

        // authentication for locked cameras
        if(username != null && password != null){
            options.add("--rtsp-user=" + username);
            options.add("--rtsp-pwd=" + password);
        }

        mLibVLC = new LibVLC(getApplicationContext(), options);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);

        final IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        adjustAspectRatio();

        ViewGroup.LayoutParams videoParams = mSurfaceView.getLayoutParams();
        videoParams.width = displayMetrics.widthPixels;
        videoParams.height = displayMetrics.heightPixels;
        vlcVout.detachViews();
        vlcVout.setVideoView(mSurfaceView);
        vlcVout.setWindowSize(videoParams.width, videoParams.height);
        vlcVout.addCallback(this);
        vlcVout.attachViews();
    }

    /**
     * Method for telling VLC to start playing a specific stream
     * @param path Stream URL to play
     */
    private void play(String path) {
        if (!path.isEmpty()) {
            Log.d(TAG, "Playing URL");
            try {
                Media media = new Media(mLibVLC, Uri.parse(path));
                media.setHWDecoderEnabled(true, false);
                media.addOption(":file-caching=2000");
                media.addOption(":network-caching=2000");
                media.addOption(":clock-jitter=0");
                media.addOption(":clock-synchro=0");

                mMediaPlayer.setMedia(media);
                mMediaPlayer.play();
            } catch (Exception e) {
                Log.e("Media Player", e.getMessage());
            }
        } else {
            Log.d(TAG, "Could not find URL to open");
            Toast.makeText(getApplicationContext(), getString(R.string.toast_missing_url), Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Initializes the SpeechRecognizer, removes all phrases and adds back the wake/sleep phrases
     * add the listen/mute speech commands
     */
    private void initVoiceVocabulary() {
        try {
            VuzixSpeechClient vuzixSpeechClient = new VuzixSpeechClient(this);
            vuzixSpeechClient.deletePhrase("*");
            vuzixSpeechClient.insertWakeWordPhrase("hello vuzix");    // Add "hello vuzix" wake-up phrase for consistency
            vuzixSpeechClient.insertWakeWordPhrase("hey blade");      // Add application specific wake-up phrase

            vuzixSpeechClient.insertVoiceOffPhrase("voice off");      // Add-back the default phrase for consistency
            vuzixSpeechClient.insertVoiceOffPhrase("privacy please"); // Add application specific stop listening phrase
            vuzixSpeechClient.insertPhrase("listen in");
            vuzixSpeechClient.insertPhrase("mute camera");
            Log.d("StreamActivity", vuzixSpeechClient.dump());
        } catch (Exception e) {
            Log.e("StreamActivity", e.getMessage());
        }
    }

    private BroadcastReceiver voiceCmdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = pref.edit();
            if (intent.getAction().equals(VuzixSpeechClient.ACTION_VOICE_COMMAND)) {
                String phrase = intent.getStringExtra(VuzixSpeechClient.PHRASE_STRING_EXTRA);
                System.out.println(phrase);
                if (phrase != null) {
                    if (phrase.equals("listen in")) {
                        unmuteAudioPlayer(editor, audioControlOption);
                    } else if (phrase.equals("mute camera")) {
                        muteAudioPlayer(editor, audioControlOption);
                    }
                }
            }
        }
    };

    /**
     * VLC event listener called when an event happens, used to display loading and error messages
     * @param event VLC event that has occurred
     */
    @Override
    public void onEvent(MediaPlayer.Event event) {
        if(event.type == MediaPlayer.Event.Opening){
            Toast.makeText(this, getString(R.string.toast_opening), Toast.LENGTH_SHORT).show();
        }else if(event.type == MediaPlayer.Event.EncounteredError){
            Toast.makeText(this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * VLC listener - not used
     * @param vlcVout Not used
     */
    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {

    }

    /**
     * VLC listener - not used
     * @param vlcVout Not used
     */
    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {

    }

    /**
     * Used to toggle the visibility of a "shield" view to mock display being turned off, surface
     * view that displays the video cannot be made invisible as VLC breaks when it is abandoned
     * @param showDisplay Toggle for showing/hiding the display
     */
    private void toggleDisplay(boolean showDisplay) {
        if (showDisplay) {
            displayShield.setVisibility(View.GONE);
            mSurfaceView.setKeepScreenOn(true);
            screenOn = true;
        } else {
            displayShield.setVisibility(View.VISIBLE);
            mSurfaceView.setKeepScreenOn(false);
            screenOn = false;
        }

    }

    /**
     * Method for setting the display brightness to max/min
     * @param maxBrightness Toggle for setting the display to max or min brightness
     */
    private void maxDisplayBrightness(boolean maxBrightness) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if(maxBrightness){
            Log.d(TAG, "Maxing display brightness");
            layoutParams.screenBrightness = 1f;
            screenBrightness = 1;
        }else{
            Log.d(TAG, "Zeroing display brightness");
            layoutParams.screenBrightness = 0f;
            screenBrightness = 0;
        }
        getWindow().setAttributes(layoutParams);
        updateBrightnessMenuItem();
    }

    /**
     * Method to capture which key was pressed on the track-pad, used for display brightness and
     * video visibility toggle
     * @param keyCode Key code of the event
     * @param event Event registered with track-pad
     * @return Super.onKeyDown to handle default key presses
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KEYCODE_DPAD_LEFT:
                if (screenOn) {
                    toggleDisplay(false);
                }
                break;
            case KEYCODE_DPAD_UP:
                if(screenBrightnessAdjustable){
                    maxDisplayBrightness(true);
                }
                break;
            case KEYCODE_DPAD_DOWN:
                if(screenBrightnessAdjustable){
                    maxDisplayBrightness(false);
                }
            default:
                if(!screenOn){
                    toggleDisplay(true);
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Sets the visibility of the progress bar
     * @param visible Progress bar visibility
     */
    private void setPbLoading(boolean visible){
        runOnUiThread(() -> pbLoading.setVisibility((visible) ? View.VISIBLE : View.GONE));
    }

    private void showUnauthorizedToast(){
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_stream_access_denied), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private static class LoadStreamURLTask extends AsyncTask<Void, Void, Void> implements OnvifMediaProfilesListener, OnvifMediaStreamURIListener, OnvifResponseListener {

        private static final String TAG = "StreamActivity";

        private WeakReference<StreamActivity> streamReference;
        private OnvifManager onvifManager;

        /**
         * Constructor for LoadStreamURLTask, creates a weak reference to StreamActivity
         * @param context StreamActivity context
         */
        LoadStreamURLTask(StreamActivity context){
            streamReference = new WeakReference<>(context);
            onvifManager = new OnvifManager();
            onvifManager.setOnvifResponseListener(this);
        }

        /**
         * Used to fetch the Stream URL and tell the user work is being done
         */
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "Starting fetch stream process");
            StreamActivity streamActivity = streamReference.get();
            if (streamActivity != null && !streamActivity.isFinishing()) {
                streamActivity.setPbLoading(true);
            }
        }

        /**
         * Used to get the media profiles and stream URL.
         * @param voids Not used, null
         * @return Null
         */
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "Requesting media profiles");
            StreamActivity streamActivity = streamReference.get();
            if (streamActivity != null && !streamActivity.isFinishing()) {
                OnvifDevice onvifDevice = new OnvifDevice(streamActivity.hostName,
                        streamActivity.username,
                        streamActivity.password);
                onvifManager.getMediaProfiles(onvifDevice, this);
            }

            return null;
        }

        /**
         * ONVIF listener method called when media profiles are received
         * @param onvifDevice OnvifDevice relating to the media profiles
         * @param list List of OnvifMediaProfile objects
         */
        @Override
        public void onMediaProfilesReceived(OnvifDevice onvifDevice, List<OnvifMediaProfile> list) {
            Log.d(TAG, "Media profiles received");
            onvifManager.getMediaStreamURI(onvifDevice, list.get(0), this);
        }

        /**
         * ONVIF listener method called when stream URI is received
         * @param onvifDevice OnvifDevice related to the stream URI
         * @param onvifMediaProfile OnvifMediaProfile related to the OnvifDevice
         * @param uriPath URI path for the OnvifDevice
         */
        @Override
        public void onMediaStreamURIReceived(OnvifDevice onvifDevice, OnvifMediaProfile onvifMediaProfile, String uriPath) {
            Log.d(TAG, "Media stream URI received");
            StreamActivity streamActivity = streamReference.get();
            if (streamActivity != null && !streamActivity.isFinishing()) {
                streamActivity.play(uriPath);
                streamActivity.setPbLoading(false);
            }
        }

        /**
         * ONVIF listener method trigger on response received, not used
         * @param onvifDevice OnvifDevice related to the response
         * @param onvifResponse OnvifResponse object containing response data
         */
        @Override
        public void onResponse(OnvifDevice onvifDevice, OnvifResponse onvifResponse) {
            // Not used
        }

        /**
         * ONVIF listener method triggered when error received, used to detect authentication issues
         * @param onvifDevice OnvifDevice related to the error
         * @param errorCode Error code
         * @param errorMessage Error message relating to the error code
         */
        @Override
        public void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
            StreamActivity streamActivity = streamReference.get();
            if (streamActivity != null && !streamActivity.isFinishing()) {
                if(onvifDevice.getHostName().equals(streamActivity.hostName)){
                    streamActivity.showUnauthorizedToast();
                }
            }
        }
    }
}