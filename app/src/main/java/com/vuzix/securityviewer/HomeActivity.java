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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.vuzix.hud.actionmenu.ActionMenuActivity;
import com.vuzix.hud.actionmenu.DefaultActionMenuItemView;
import com.vuzix.securityviewer.model.Camera;
import com.vuzix.securityviewer.settings.DiscoverCameras;
import com.vuzix.securityviewer.settings.Manage;
import com.vuzix.securityviewer.utils.CameraCardAdapter;
import com.vuzix.sdk.speechrecognitionservice.VuzixSpeechClient;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import static android.view.KeyEvent.ACTION_DOWN;
import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_DPAD_CENTER;
import static android.view.KeyEvent.KEYCODE_DPAD_LEFT;
import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;
import static android.view.KeyEvent.KEYCODE_ENTER;
import static android.view.KeyEvent.KEYCODE_MENU;

public class HomeActivity extends ActionMenuActivity {

    private final static int pageTitle = R.string.title_home;
    private final static int MENU_DISCOVER = 1;

    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;


    private TextView networkInfoText;
    private TextView noCamerasTextView;
    private TextView optionsTextView;
    private RecyclerView cameraRecyclerView;

    private MenuItem manageCameras;
    private boolean manageCamerasShouldBeVisible = false;

    private MenuItem allowCameraAudio;
    private SwitchMenuItemView switchAudioMenuItemView;

    private List<Camera> cameraList;
    private int currentCameraIndex;

    private HashMap<String, Camera> cameraSubstitutionMap;

    boolean actionMenuOpen = false;


    /**
     * Initially set our views and read the database for existing cameras
     *
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        networkInfoText = findViewById(R.id.header_network);
        TextView headerPageTitle = findViewById(R.id.header_page_title);
        cameraRecyclerView = findViewById(R.id.rv_home_cameras);
        noCamerasTextView = findViewById(R.id.tv_home_no_cameras);
        optionsTextView = findViewById(R.id.tv_home_options);
        currentCameraIndex = 0;

        cameraSubstitutionMap = new HashMap<>();

        headerPageTitle.setText(pageTitle);


        registerReceiver(voiceCmdReceiver, new IntentFilter(VuzixSpeechClient.ACTION_VOICE_COMMAND));
        registerReceiver(wifiReceiver, intentFilter);
    }

    /**
     * Used to load updated details from the database when the user returns
     */
    @Override
    protected void onResume() {
        Log.d(getString(pageTitle), "OnResume");
        updateCurrentNetwork();
        currentCameraIndex = 0;
        LoadCamerasTask loadCamerasTask = new LoadCamerasTask(this);
        loadCamerasTask.execute();
        super.onResume();
    }

    /**
     * Called when action menu is created, initializes menu items and hide "Done" if not coming from
     * FTUE
     *
     * @param menu Menu object being inflated
     * @return True to inflate menu
     */
    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.home, menu);

        menu.findItem(R.id.menu_item_home_discovery)
                .setIntent(new Intent(this, DiscoverCameras.class));
        manageCameras = menu.findItem(R.id.menu_item_home_manage);
        manageCameras.setEnabled(manageCamerasShouldBeVisible);
        manageCameras.setIntent(new Intent(this, Manage.class));
        allowCameraAudio = menu.findItem(R.id.menu_item_settings_audio);
        allowCameraAudio.setActionView(switchAudioMenuItemView = new SwitchMenuItemView(this));
        menu.findItem(R.id.menu_item_home_help)
                .setIntent(new Intent(this, HelpActivity.class));
        menu.findItem(R.id.menu_item_home_about)
                .setIntent(new Intent(this, AboutActivity.class));
        updateMenuItems();
        return true;
    }

    private void updateMenuItems() {
        setupAudioOption();
    }

    /**
     * Custom Class to allow the view to change on the dynamicly.
     * Notice that we utilize this class on the actual MenuItem for its ActionView class.
     * This will allow us to access internal fields like icon to modify the action view itself.
     * For more information see the Class definition for DefaultActionMenuItemView and for
     * onCreateActionMenu.
     */
    private static class SwitchMenuItemView extends DefaultActionMenuItemView {

        public SwitchMenuItemView(Context context) {
            super(context);
        }

        private void setSwitchState(String state) {
            final String off = getResources().getString(R.string.menu_item_settings_camera_audio_OFF);
            final String on = getResources().getString(R.string.menu_item_settings_camera_audio_ON);
            final String manual = getResources().getString(R.string.menu_item_settings_camera_audio_MANUAL);
            if (off.equals(state)) {
                setIcon(getResources().getDrawable(R.drawable.ic_mic_off_24px));
                setTitle(off);
            } else if (on.equals(state)) {
                setIcon(getResources().getDrawable(R.drawable.ic_mic_24px));
                setTitle(on);
            } else if (manual.equals(state)) {
                setIcon(getResources().getDrawable(R.drawable.ic_mic_none_24px));
                setTitle(manual);
            }

        }
    }

    /**
     * Restores state off audio option from SharedPreferences
     */
    protected void setupAudioOption() {
        final String off = getString(R.string.menu_item_settings_camera_audio_OFF);
        final String on = getString(R.string.menu_item_settings_camera_audio_ON);
        final String manual = getString(R.string.menu_item_settings_camera_audio_MANUAL);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        if (!pref.contains("audioState")) {
            editor.putString("audioState", off);
            switchAudioMenuItemView.setSwitchState(off);
        }
        if (pref.contains("audioState") && pref.getString("audioState", null) != null) {
            String state = pref.getString("audioState", null);
            switchAudioMenuItemView.setSwitchState(state);
        }
        editor.apply();
    }

    /**
     * Handler for swapping audio options for camera
     */
    public void cameraAudioOption(MenuItem item) {
        final String off = getString(R.string.menu_item_settings_camera_audio_OFF);
        final String on = getString(R.string.menu_item_settings_camera_audio_ON);
        final String manual = getString(R.string.menu_item_settings_camera_audio_MANUAL);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        if (pref.contains("audioState") && pref.getString("audioState", null) != null) {
            String state = pref.getString("audioState", null);
            if (off.equals(state)) {
                editor.putString("audioState", manual);
                switchAudioMenuItemView.setSwitchState(manual);
            } else if (manual.equals(state)) {
                editor.putString("audioState", on);
                switchAudioMenuItemView.setSwitchState(on);
            } else if (on.equals(state)) {
                editor.putString("audioState", off);
                switchAudioMenuItemView.setSwitchState(off);
            }
        }
        editor.apply();
    }

    /**
     * Sets the default menu item for the action menu
     *
     * @return 1 for "Discover"
     */
    @Override
    protected int getDefaultAction() {
        return MENU_DISCOVER;
    }

    /**
     * Method to tell us if we should ignore recycler view gestures if the action menu is
     * open/closed
     */
    @Override
    protected void onActionMenuClosed() {
        actionMenuOpen = false;
        super.onActionMenuClosed();
    }

    /**
     * Used to set the "Manage" button to enabled/disabled if there are subscribed cameras
     *
     * @param enabled Value to set "Manage" button enabled to
     */
    private void manageSetEnabled(boolean enabled) {
        if (manageCameras != null) {
            manageCameras.setEnabled(enabled); // can't directly call setEnabled as item may be null
        }
        manageCamerasShouldBeVisible = enabled;
    }

    /**
     * Used to unregister our wifi and SpeechRecognizer receivers
     */
    @Override
    protected void onDestroy() {
        unregisterReceiver(wifiReceiver);
        unregisterReceiver(voiceCmdReceiver);
        super.onDestroy();
    }

    /**
     * Method for navigating the camera recycler view
     *
     * @param event KeyEvent from track-pad
     * @return True for handling event
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KEYCODE_DPAD_CENTER:
                case KEYCODE_ENTER:
                    if (cameraList.size() != 0 && !actionMenuOpen) {
                        cameraList.get(currentCameraIndex).openStream(this);
                    }
                    break;
                case KEYCODE_MENU:
                    this.openActionMenu();
                    actionMenuOpen = true;
                    break;
                case KEYCODE_BACK:
                    finish();
                    break;
                case KEYCODE_DPAD_LEFT:
                    if (cameraList != null && !actionMenuOpen) {
                        if (currentCameraIndex < cameraList.size() - 1) {
                            cameraList.get(currentCameraIndex).setSelected(false);
                            currentCameraIndex++;
                            cameraRecyclerView.smoothScrollToPosition(currentCameraIndex);
                            cameraList.get(currentCameraIndex).setSelected(true);
                            cameraRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }
                    break;
                case KEYCODE_DPAD_RIGHT:
                    if (cameraList != null && !actionMenuOpen) {
                        if (currentCameraIndex > 0) {
                            cameraList.get(currentCameraIndex).setSelected(false);
                            currentCameraIndex--;
                            cameraRecyclerView.smoothScrollToPosition(currentCameraIndex);
                            cameraList.get(currentCameraIndex).setSelected(true);
                            cameraRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    }
                    break;
                default:
                    super.dispatchKeyEvent(event);
            }
        }
        return true;
    }

    /**
     * Determines if there is a current wifi connection
     *
     * @return Boolean for currently connected
     */
    public boolean wifiConnected() {
        for (Network network : connectivityManager.getAllNetworks()) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                return networkInfo.isConnected();
            }
        }

        return false;
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    updateCurrentNetwork();
                }
            }
        }
    };

    /**
     * Fetches the SSID of the current network connected
     */
    private void updateCurrentNetwork() {
        if (wifiConnected()) {
            String currentNetwork = wifiManager.getConnectionInfo().getSSID().replace("\"", "");
            networkInfoText.setText(currentNetwork);
        } else {
            networkInfoText.setText("");
        }
    }

    /**
     * Toggles visibility of no cameras text/recycler view
     */
    private void onCameraListLoaded(boolean camerasAvailable) {
        if (camerasAvailable) {
            noCamerasTextView.setVisibility(View.GONE);
            optionsTextView.setVisibility(View.VISIBLE);
            cameraRecyclerView.setVisibility(View.VISIBLE);
            manageSetEnabled(true);
        } else {
            noCamerasTextView.setVisibility(View.VISIBLE);
            optionsTextView.setVisibility(View.GONE);
            cameraRecyclerView.setVisibility(View.GONE);
            manageSetEnabled(false);
        }
    }

    /**
     * Initializes the SpeechRecognizer, removes all phrases and adds back the wake/sleep phrases
     * After, we add all the camera nicknames/titles to the vocabulary
     */
    private void initVoiceVocabulary() {
        try {
            VuzixSpeechClient vuzixSpeechClient = new VuzixSpeechClient(this);
            vuzixSpeechClient.deletePhrase("*");
            vuzixSpeechClient.insertWakeWordPhrase("hello vuzix");    // Add "hello vuzix" wake-up phrase for consistency
            vuzixSpeechClient.insertWakeWordPhrase("start security viewer");      // Add application specific wake-up phrase

            vuzixSpeechClient.insertVoiceOffPhrase("voice off");      // Add-back the default phrase for consistency
            vuzixSpeechClient.insertVoiceOffPhrase("privacy please"); // Add application specific stop listening phrase
            for (Camera camera : cameraList) {
                // We'll add three trigger words to the beginning of each camera: "View", "Go to", "Open"
                int[] stringIds = {R.string.speech_view, R.string.speech_goto, R.string.speech_open};
                for (int eachStringId : stringIds) {
                    String cameraPhrase = getString(eachStringId, camera.getPresentableName());
                    String cameraSubstitution = cameraPhrase.replace(" ", "_").toLowerCase();
                    cameraSubstitutionMap.put(cameraSubstitution, camera);
                    vuzixSpeechClient.insertPhrase(cameraPhrase, cameraSubstitution);
                }
            }
            Log.d(getString(pageTitle), vuzixSpeechClient.dump());
        } catch (Exception e) {
            Log.e(getString(pageTitle), e.getMessage());
        }
    }

    private BroadcastReceiver voiceCmdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(VuzixSpeechClient.ACTION_VOICE_COMMAND)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    if (extras.containsKey(VuzixSpeechClient.PHRASE_STRING_EXTRA)) {
                        String phrase = intent.getStringExtra(VuzixSpeechClient.PHRASE_STRING_EXTRA);
                        System.out.println(phrase);
                        if (cameraSubstitutionMap.containsKey(phrase)) {
                            Camera summonedCamera = cameraSubstitutionMap.get(phrase);
                            if (summonedCamera != null) {
                                Toast.makeText(getApplicationContext(), getString(R.string.toast_home_speech_open, summonedCamera.getPresentableName()), Toast.LENGTH_SHORT).show();
                                summonedCamera.openStream(getApplicationContext());
                            }
                        }
                    }
                }
            }
        }
    };

    private static class LoadCamerasTask extends AsyncTask<Void, Void, Void> {

        CamerasDatabase database;
        private WeakReference<HomeActivity> homeReference;

        /**
         * Constructor for LoadCamerasTask, connects to database and creates a weak reference to the
         * HomeActivity
         *
         * @param context Context of HomeActivity executing the task
         */
        LoadCamerasTask(HomeActivity context) {
            homeReference = new WeakReference<>(context);
            database = Room.databaseBuilder(context, CamerasDatabase.class, "camerasDB")
                    .build();
        }

        /**
         * Work to be done in the background, need to load existing cameras from database
         *
         * @param voids Not used, null
         * @return Null
         */
        @Override
        protected Void doInBackground(Void... voids) {
            HomeActivity homeActivity = homeReference.get();
            if (homeActivity != null && !homeActivity.isFinishing()) {
                homeActivity.cameraList = database.getCameraDAO().getCameras();
            }
            return null;
        }

        /**
         * Executed on cameras loaded, initializes the recycler view with our cameras
         *
         * @param aVoid Null
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            HomeActivity homeActivity = homeReference.get();
            if (homeActivity != null && !homeActivity.isFinishing()) {
                if (homeActivity.cameraList.size() > 0) {
                    homeActivity.cameraList.get(homeActivity.currentCameraIndex).setSelected(true);
                    CameraCardAdapter cameraCardAdapter = new CameraCardAdapter(homeActivity.cameraList);
                    homeActivity.cameraRecyclerView.setAdapter(cameraCardAdapter);
                    homeActivity.cameraRecyclerView.setLayoutManager(new LinearLayoutManager(homeActivity, LinearLayoutManager.HORIZONTAL, true));
                    homeActivity.onCameraListLoaded(true);
                    homeActivity.initVoiceVocabulary();
                } else {
                    homeActivity.onCameraListLoaded(false);
                }
            }
            database.close();
        }
    }
}