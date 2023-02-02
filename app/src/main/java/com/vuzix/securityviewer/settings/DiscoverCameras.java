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
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.room.Room;

import com.vuzix.securityviewer.CamerasDatabase;
import com.vuzix.securityviewer.HomeActivity;
import com.vuzix.securityviewer.MainActivity;
import com.vuzix.securityviewer.R;
import com.vuzix.securityviewer.model.Camera;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.teletask.onvif.DiscoveryManager;
import be.teletask.onvif.OnvifManager;
import be.teletask.onvif.listeners.DiscoveryListener;
import be.teletask.onvif.listeners.OnvifDeviceInformationListener;
import be.teletask.onvif.listeners.OnvifResponseListener;
import be.teletask.onvif.models.Device;
import be.teletask.onvif.models.OnvifDevice;
import be.teletask.onvif.models.OnvifDeviceInformation;
import be.teletask.onvif.responses.OnvifResponse;

public class DiscoverCameras extends Settings implements DiscoveryListener, OnvifResponseListener, OnvifDeviceInformationListener {

    private final int pageTitle = R.string.title_settings_discovery;

    private DiscoveryManager discoveryManager;
    private OnvifManager onvifManager;
    private WifiManager.MulticastLock multicastLock;
    private CamerasDatabase database;

    private MenuItem wifiMenuItem;
    private MenuItem searchMenuItem;
    private MenuItem manualConnectMenuItem;
    private Menu menu;

    private Map<Camera, MenuItem> cameraMenuItemMap;
    private Map<String, Camera> hostnameCameraMap;

    private List<Camera> discoveredNetworkCameras;
    private List<Camera> camerasFromRoom;

    private boolean from_ftue;
    boolean notSearching = true;

    private int orderInCategory = 101;

    /**
     * Initially set our views and read the database for existing cameras
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int DISCOVERY_TIMEOUT = 10000;

        super.onCreate(savedInstanceState);
        discoveryManager = new DiscoveryManager();
        onvifManager = new OnvifManager();
        multicastLock = wifiManager.createMulticastLock("ONVIF");
        discoveryManager.setDiscoveryTimeout(DISCOVERY_TIMEOUT);
        multicastLock.acquire();
        cameraMenuItemMap = new HashMap<>();
        hostnameCameraMap = new HashMap<>();
        discoveredNetworkCameras = new ArrayList<>();
        database = Room.databaseBuilder(this, CamerasDatabase.class, "camerasDB")
                .build();

        updateTitle(pageTitle);
        onvifManager.setOnvifResponseListener(this);
        Thread loadFromRoomThread = new Thread(() -> camerasFromRoom = database.getCameraDAO().getCameras());
        loadFromRoomThread.start();
    }

    /**
     * Used to release our MulticastLock
     */
    @Override
    protected void onDestroy() {
        if(database != null){
            database.close();
        }
        multicastLock.release();
        super.onDestroy();
    }

    /**
     * Inflate the discovery menu, show/hide "Done" button if we're in FTUE flow
     * @param menu Menu object
     * @return Inflate Menu
     */
    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.settings_discovery, menu);
        this.menu = menu;

        MenuItem doneMenuItem = menu.findItem(R.id.menu_item_settings_discovery_ftue_done);
        wifiMenuItem = menu.findItem(R.id.menu_item_settings_discovery_wifi);
        searchMenuItem = menu.findItem(R.id.menu_item_settings_discovery_auto);
        manualConnectMenuItem = menu.findItem(R.id.menu_item_settings_discovery_manual);
        from_ftue = getIntent().getBooleanExtra(MainActivity.FROM_FTUE, false);

        wifiMenuItem.setIntent(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));

        if(!from_ftue){
            doneMenuItem.setVisible(false);
        }

        updateNetworkInfo();

        return true;
    }

    /**
     * Sets the default selected action menu item (Search)
     * @return 2 if in FTUE flow, 1 otherwise.
     */
    @Override
    protected int getDefaultAction() {
        if(from_ftue){
            return 2;
        }else{
            return 1;
        }
    }

    /**
     * Used to kill this activity after tapping "Done". Prevents back button from re-opening FTUE.
     * @param item MenuItem selected
     */
    public void goHome(MenuItem item){
        Intent homeTransition = new Intent(this, HomeActivity.class);
        homeTransition.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeTransition);
        finish();
    }

    /**
     * Method to execute on "Search" pressed.
     * @param item MenuItem selected
     */
    public void executeSearch(MenuItem item){
        if(camerasFromRoom != null && notSearching){
            hostnameCameraMap.clear();
            discoveredNetworkCameras.clear();
            for(Camera camera : cameraMenuItemMap.keySet()){
                this.menu.removeItem(cameraMenuItemMap.get(camera).getItemId());
            }
            cameraMenuItemMap.clear();

            discoveryManager.discover(this);
            notSearching = false;
        }
    }

    /**
     * ONVIF listener method called when OnvifDiscovery started
     */
    @Override
    public void onDiscoveryStarted() {
        Log.d(getString(pageTitle), "Network discovery started");
        updateSubtitle("Searching...");
    }

    /**
     * ONVIF listener method called when devices discovered
     * @param devices List of OnvifDevice objects
     */
    @Override
    public void onDevicesFound(List<Device> devices) {
        Log.d(getString(pageTitle), "Network search completed; found: " + devices.size() + " devices");
        OnvifDevice onvifDevice;

        for(Device device : devices) {
            boolean alreadyExistsInDB = false;
            for(Camera existingCamera : camerasFromRoom){
                // We want to check to see if the same hostname already exists in our database.
                // If it doesn't we'll convert it to a Camera and show it in the list.
                if(existingCamera.getHostName().equals(device.getHostName())){
                    alreadyExistsInDB = true;
                }
            }
            if(!alreadyExistsInDB){
                Camera convertedCamera = new Camera();
                convertedCamera.setHostName(device.getHostName());
                convertedCamera.setNetwork(wifiManager.getConnectionInfo().getSSID().replace("\"", ""));
                discoveredNetworkCameras.add(convertedCamera);
                hostnameCameraMap.put(convertedCamera.getHostName(), convertedCamera);
            }
        }

        // Create the menu items now and we'll update them when either getDeviceInformation
        // successfully retrieves information or onError throws an authentication issue signaling a
        // locked camera.
        runOnUiThread(this::createCameraMenuItems);

        // getDeviceInformation is used in two ways (1) get device information (2) determine if
        // device is locked. If locked, onError is called and onDeviceInformationReceived is not.
        for(Camera camera : discoveredNetworkCameras){
            onvifDevice = new OnvifDevice(camera.getHostName());
            onvifManager.getDeviceInformation(onvifDevice, this);
        }

        // There's a lot of work going on in the background but we want to update the user on our
        // progress. We can alert them that we've found X many new devices even if we haven't
        // gotten information on them yet.
        runOnUiThread(() -> {
            if(discoveredNetworkCameras.size() == 1){
                updateSubtitle(getString(R.string.subtitle_settings_discovery_cameras_singular, discoveredNetworkCameras.size()));
            }else{
                updateSubtitle(getString(R.string.subtitle_settings_discovery_cameras_plural, discoveredNetworkCameras.size()));
            }
        });

    }

    /**
     * ONVIF listener method for receiving device information.
     * @param onvifDevice
     * @param deviceInformation
     */
    @Override
    public void onDeviceInformationReceived(OnvifDevice onvifDevice, OnvifDeviceInformation deviceInformation) {
        Camera camera = hostnameCameraMap.get(onvifDevice.getHostName());

        notSearching = true;
        if(camera != null){
            camera.setLocked(false);
            camera.setModel(deviceInformation.getModel());
            camera.setFirmwareVersion(deviceInformation.getFirmwareVersion());
            camera.setHardwareID(deviceInformation.getHardwareId());
            camera.setManufacturer(deviceInformation.getManufacturer());
            camera.setSerialNumber(deviceInformation.getSerialNumber());

            runOnUiThread(() -> updateCameraMenuItem(camera));
        }
    }

    /**
     * ONVIF listener method for network responses - not used
     * @param onvifDevice Not used
     * @param onvifResponse Not used
     */
    @Override
    public void onResponse(OnvifDevice onvifDevice, OnvifResponse onvifResponse) {
        // Not used as we do not need to know any response data.
    }

    /**
     * ONVIF listener method for error events. Used to determine if a camera is locked.
     * @param onvifDevice The onvifDevice experiencing the error
     * @param errorCode Error code for the specific error event
     * @param errorMessage Error message relating to the errorCode
     */
    @Override
    public void onError(OnvifDevice onvifDevice, int errorCode, String errorMessage) {
        Camera camera = hostnameCameraMap.get(onvifDevice.getHostName());

        if(camera != null && errorCode == Camera.ERROR_UNAUTHORIZED){
            notSearching = true;
            // Error code ERROR_UNAUTHORIZED(401) indicates an access denied error when trying to get details from an
            // OnvifDevice. With this, we alert the user the camera is locked and requires
            // credentials.
            camera.setLocked(true);
            runOnUiThread(() -> updateCameraMenuItem(camera));
        }
    }

    /**
     * Method to create the individual camera MenuItems
     */
    private void createCameraMenuItems(){
        for(Camera camera : discoveredNetworkCameras){
            MenuItem menuItem = this.menu.add(Menu.NONE, this.menu.hashCode(), orderInCategory, camera.getPresentableName());
            cameraMenuItemMap.put(camera, menuItem);
            menuItem.setIcon(R.drawable.ic_camera);
            menuItem.setEnabled(false);
            orderInCategory++;
        }
    }

    /**
     * Method used to create a MenuItem for a camera
     * @param camera Camera object to make a MenuItem for
     */
    private void updateCameraMenuItem(Camera camera){
        MenuItem menuItem = cameraMenuItemMap.get(camera);

        if(menuItem != null){
            menuItem.setEnabled(true);
            menuItem.setTitle(camera.getPresentableName());
            if(camera.isLocked()){
                menuItem.setIcon(R.drawable.ic_camera_locked);
                menuItem.setOnMenuItemClickListener(menuItem1 -> {
                    authenticateCamera(camera);
                    return true;
                });
            }else{
                menuItem.setIcon(R.drawable.ic_camera);
                menuItem.setOnMenuItemClickListener(menuItem1 -> {
                    subscribeToCamera(camera, menuItem);
                    return true;
                });
            }
        }
    }

    /**
     * Method executed on attempting to subscribe to a locked camera. Displays an AlertDialog for
     * authentication
     * @param camera Camera that needs to be authenticated
     */
    private void authenticateCamera(Camera camera){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ad_settings_discover_unlock));

        final View authenticateLayout = getLayoutInflater().inflate(R.layout.layout_camera_authenticate, null);
        EditText username = authenticateLayout.findViewById(R.id.et_settings_manage_auth_username);
        EditText password = authenticateLayout.findViewById(R.id.et_settings_manage_auth_password);
        builder.setView(authenticateLayout);

        builder.setPositiveButton(getString(R.string.ad_settings_discover_unlock), (dialogInterface, i) -> {
            camera.setCredentials(username.getText().toString(), password.getText().toString());
            OnvifDevice onvifDevice = new OnvifDevice(camera.getHostName(), camera.getUsername(), camera.getPassword());
            onvifManager.getDeviceInformation(onvifDevice, this);
        });

        AlertDialog unlockDialog = builder.create();
        unlockDialog.show();
    }

    /**
     * Method executed on attempting to subscribe to a camera. Changes MenuItem appearance to show
     * new camera addition
     * @param camera Desired camera to subscribe to
     * @param menuItem MenuItem that relates to the chosen camera, used to modify appearance
     */
    private void subscribeToCamera(Camera camera, MenuItem menuItem){
        Thread addCameraThread = new Thread(() -> database.getCameraDAO().add(camera));
        addCameraThread.start();
        menuItem.setTitle(camera.getPresentableName());
        menuItem.setIcon(R.drawable.ic_camera_subscribed);
        menuItem.setEnabled(false);
    }

    /**
     * Method executed on pressing "Manually Add" - opens AlertDialog for camera details
     * @param menuItem MenuItem that was selected
     */
    public void manuallyAddCamera(MenuItem menuItem){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.ad_settings_discover_manual));

        final View authenticateLayout = getLayoutInflater().inflate(R.layout.layout_discovery_manual, null);
        EditText hostname = authenticateLayout.findViewById(R.id.et_settings_discovery_manual_hostname);
        EditText username = authenticateLayout.findViewById(R.id.et_settings_discovery_manual_username);
        EditText password = authenticateLayout.findViewById(R.id.et_settings_discovery_manual_password);
        builder.setView(authenticateLayout);

        builder.setPositiveButton(getString(R.string.ad_add), (dialogInterface, i) -> {
            Camera newCamera =  new Camera();
            newCamera.setHostName((hostname.getText().toString().equals("")) ? null : hostname.getText().toString());
            newCamera.setUsername((username.getText().toString().equals("")) ? null : username.getText().toString());
            newCamera.setPassword((password.getText().toString().equals("")) ? null : password.getText().toString());
            if (!hostname.getText().toString().equals("")){
                MenuItem menuItem1 = menu.add(Menu.NONE, menu.hashCode(), orderInCategory, newCamera.getPresentableName());
                subscribeToCamera(newCamera, menuItem1);
            }

            dialogInterface.cancel();
        });

        AlertDialog unlockDialog = builder.create();
        unlockDialog.show();
    }

    /**
     * Method to modify network TextView if wifi connection is present
     */
    @Override
    protected void updateNetworkInfo() {
        super.updateNetworkInfo();
        if(searchMenuItem == null || manualConnectMenuItem == null){
            return;
        }
        if(wifiConnected()){
            wifiMenuItem.setVisible(false);
            searchMenuItem.setVisible(true);
            manualConnectMenuItem.setVisible(true);
            updateSubtitle(getString(R.string.blank));
        }else{
            wifiMenuItem.setVisible(true);
            searchMenuItem.setVisible(false);
            manualConnectMenuItem.setVisible(false);
            updateSubtitle(getString(R.string.subtitle_settings_discovery_network_disconnected));
        }
    }
}

