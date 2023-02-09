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
package com.vuzix.securityviewer.settings;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.room.Room;

import com.vuzix.hud.actionmenu.ActionMenuActivity;
import com.vuzix.securityviewer.CamerasDatabase;
import com.vuzix.securityviewer.R;
import com.vuzix.securityviewer.model.Camera;

import java.lang.ref.WeakReference;

import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.OnvifDeviceInformationListener;
import be.teletask.onvif.listeners.OnvifResponseListener;
import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifDeviceInformation;
import be.teletask.onvif.responses.OnvifResponse;

public class EditCamera extends ActionMenuActivity implements OnvifDeviceInformationListener, OnvifResponseListener {

    private final static String TAG = "EditCamera";
    private final static int MENU_NICKNAME = 1;

    private Menu menu;
    private CamerasDatabase database;
    private OnvifManager onvifManager;

    private TextView pageTitle;
    private TextView hostName;
    private TextView nickname;
    private TextView macAddress;
    private TextView manufacturer;
    private TextView model;
    private TextView serialNumber;
    private TextView firmwareVersion;
    private TextView username;
    private TextView password;

    private Camera camera;

    /**
     * Initially set our views and read the database for existing camera given supplied UID
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_camera);
        onvifManager = new OnvifManager();
        onvifManager.setOnvifResponseListener(this);
        pageTitle = findViewById(R.id.header_page_title);
        hostName = findViewById(R.id.tv_settings_manage_edit_hostname);
        nickname = findViewById(R.id.tv_settings_manage_edit_nickname);
        macAddress = findViewById(R.id.tv_settings_manage_edit_mac);
        manufacturer = findViewById(R.id.tv_settings_manage_edit_manufacturer);
        model = findViewById(R.id.tv_settings_manage_edit_model);
        serialNumber = findViewById(R.id.tv_settings_manage_edit_serial);
        firmwareVersion = findViewById(R.id.tv_settings_manage_edit_firmware);
        username = findViewById(R.id.tv_settings_manage_edit_username);
        password = findViewById(R.id.tv_settings_manage_edit_password);

        database = Room.databaseBuilder(this, CamerasDatabase.class, "camerasDB")
                .build();

        int cameraUID = getIntent().getIntExtra(Manage.UID, -1);
        if(cameraUID == -1){
            Toast.makeText(this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
            finish();
        }

        LoadDetailsTask loadDetailsTask = new LoadDetailsTask(this);
        loadDetailsTask.execute(cameraUID);

    }

    /**
     * Override to close database connection
     */
    @Override
    protected void onDestroy() {
        if(database != null){
            database.close();
        }
        super.onDestroy();
    }

    /**
     * Updates the UI with the pulled camera details
     */
    private void fetchCameraDetails(){
        pageTitle.setText(camera.getPresentableName());
        hostName.setText((camera.getHostName()!=null) ? camera.getHostName() : getString(R.string.tv_settings_not_available));
        nickname.setText((camera.getCameraNickname()!=null) ? camera.getCameraNickname() : getString(R.string.tv_settings_not_available));
        macAddress.setText((camera.getMacAddress()!=null) ? camera.getMacAddress() : getString(R.string.tv_settings_not_available));
        manufacturer.setText((camera.getManufacturer()!=null) ? camera.getManufacturer() : getString(R.string.tv_settings_not_available));
        model.setText((camera.getModel()!=null) ? camera.getModel() : getString(R.string.tv_settings_not_available));
        serialNumber.setText((camera.getSerialNumber()!=null) ? camera.getSerialNumber() : getString(R.string.tv_settings_not_available));
        firmwareVersion.setText((camera.getFirmwareVersion()!=null) ? camera.getFirmwareVersion() : getString(R.string.tv_settings_not_available));
        username.setText((camera.getUsername()!=null) ? camera.getUsername() : getString(R.string.tv_settings_not_available));
        password.setText((camera.getPassword()!=null) ? getString(R.string.tv_settings_password_placeholder) : getString(R.string.tv_settings_not_available));
    }

    /**
     * Opens a menu for adjusting nickname, credentials and remove camera
     * @param menu Menu object
     * @return Inflate Menu
     */
    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.settings_edit, menu);
        this.menu = menu;
        return true;
    }

    /**
     * Sets the default selected action menu item (Nickname)
     * @return 1 for "Nickname"
     */
    @Override
    protected int getDefaultAction() {
        return MENU_NICKNAME;
    }

    /**
     * Triggered when "Set Nickname" pressed
     * @param menuItem menu item selected
     */
    public void onNicknameClicked(MenuItem menuItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ad_settings_manage_edit_nickname));

        final View nicknameLayout = getLayoutInflater().inflate(R.layout.layout_camera_nickname, null);
        EditText nickname = nicknameLayout.findViewById(R.id.et_settings_manage_edit_nickname);
        builder.setView(nicknameLayout);

        builder.setPositiveButton(getString(R.string.ad_set), (dialogInterface, i) -> {
            if(!nickname.getText().toString().equals("")){
                camera.setCameraNickname(nickname.getText().toString());
                updateCamera(camera);
                fetchCameraDetails();
            }
        });

        AlertDialog setNicknameDialog = builder.create();
        setNicknameDialog.show();
    }

    /**
     * Triggered when "Set Credentials" pressed
     * @param menuItem menu item selected
     */
    public void onCredentialsClicked(MenuItem menuItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ad_settings_manage_edit_credentials));

        final View authenticateLayout = getLayoutInflater().inflate(R.layout.layout_camera_authenticate, null);
        EditText username = authenticateLayout.findViewById(R.id.et_settings_manage_auth_username);
        EditText password = authenticateLayout.findViewById(R.id.et_settings_manage_auth_password);
        builder.setView(authenticateLayout);

        builder.setPositiveButton(getString(R.string.ad_set), (dialogInterface, i) -> {
            camera.setCredentials(username.getText().toString(), password.getText().toString());
            updateCamera(camera);
            fetchCameraDetails();
            OnvifDevice onvifDevice = new OnvifDevice(camera.getHostName(), camera.getUsername(), camera.getPassword());
            onvifManager.getDeviceInformation(onvifDevice, this);
        });

        AlertDialog unlockDialog = builder.create();
        unlockDialog.show();
    }

    /**
     * Triggered when "Remove" pressed
     * @param menuItem menu item selected
     */
    public void onRemoveClicked(MenuItem menuItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ad_settings_manage_edit_remove_cam, camera.getPresentableName()));

        builder.setNegativeButton(getString(R.string.ad_settings_manage_edit_remove), (dialogInterface, i) -> {
            deleteCamera(camera);
            finish();
        });

        builder.setNeutralButton(getString(R.string.ad_settings_manage_edit_cancel), (dialogInterface, i) -> dialogInterface.cancel());

        AlertDialog removeDialog = builder.create();
        removeDialog.show();
    }

    /**
     * Method for deleting camera from database
     * @param camera Camera to delete
     */
    private void deleteCamera(Camera camera){
        Thread deleteCameraThread = new Thread(() -> database.getCameraDAO().delete(camera));
        deleteCameraThread.start();
    }

    /**
     * Method for updating camera from database
     * @param camera Camera to update
     */
    private void updateCamera(Camera camera){
        Thread updateCameraThread = new Thread(() -> database.getCameraDAO().update(camera));
        updateCameraThread.start();
    }

    /**
     * ONVIF listener method triggered when information received from a camera
     * @param onvifDevice onvifDevice that received information, we use host name to match with a
     *                    camera
     * @param deviceInformation The OnvifDeviceInformation object containing device information
     */
    @Override
    public void onDeviceInformationReceived(OnvifDevice onvifDevice, OnvifDeviceInformation deviceInformation) {
        if(onvifDevice.getHostName().equals(camera.getHostName())){
            camera.setLocked(false);
            camera.setModel(deviceInformation.getModel());
            camera.setFirmwareVersion(deviceInformation.getFirmwareVersion());
            camera.setHardwareID(deviceInformation.getHardwareId());
            camera.setManufacturer(deviceInformation.getManufacturer());
            updateCamera(camera);
            runOnUiThread(this::fetchCameraDetails);
        }
    }

    /**
     * ONVIF listener method triggered when response received from a camera, not used
     * @param onvifDevice onvifDevice that received a response, we use host name to match with a
     *                    camera
     * @param onvifResponse The OnvifResponse object containing response information
     */
    @Override
    public void onResponse(OnvifDevice onvifDevice, OnvifResponse onvifResponse) {

    }

    /**
     * ONVIF listener method triggered when error received, used to determine if camera is still
     * locked
     * @param onvifDevice onvifDevice that is tied to the error
     * @param errorCode Error code tied to the error
     * @param errorMessage Error message relating to the error code
     */
    @Override
    public void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
        System.out.println(errorCode);
        if(onvifDevice.getHostName().equals(camera.getHostName()) && errorCode == 401){
            runOnUiThread(() -> {
                camera.setLocked(true);
                AlertDialog.Builder builder1 = new AlertDialog.Builder(EditCamera.this);
                builder1.setTitle(getString(R.string.ad_settings_manage_edit_incorrect_credentials));
                builder1.setPositiveButton(getString(R.string.ad_ok), (dialogInterface1, i1)
                        -> dialogInterface1.dismiss());
                AlertDialog failedDialog = builder1.create();
                failedDialog.show();
            });
        }
    }

    private static class LoadDetailsTask extends AsyncTask<Integer, Void, Void> {


        private WeakReference<EditCamera> editReference;
        private Camera camera;

        /**
         * Constructor for LoadDetailsTask, creates a weak reference to EditCamera
         * @param context Context for EditCamera
         */
        LoadDetailsTask(EditCamera context) {
            editReference = new WeakReference<>(context);
        }

        /**
         * Method for loading camera details from the database
         * @param uid Unique ID for a camera, auto-generated by Room
         * @return Null
         */
        @Override
        protected Void doInBackground(Integer... uid) {
            EditCamera editActivity = editReference.get();
            if (editActivity == null || editActivity.isFinishing()) {
                return null;
            }

            camera = editActivity.database.getCameraDAO().getCamera(uid[0]);
            return null;
        }

        /**
         * Update the UI with details from the camera
         * @param aVoid Null
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            EditCamera editActivity = editReference.get();
            if (editActivity == null || editActivity.isFinishing()) {
                return;
            }

            editActivity.camera = camera;
            editActivity.fetchCameraDetails();
        }
    }
}