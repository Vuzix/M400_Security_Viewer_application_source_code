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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.room.Room;

import com.vuzix.securityviewer.CamerasDatabase;
import com.vuzix.securityviewer.R;
import com.vuzix.securityviewer.model.Camera;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Manage extends Settings{

    private static final String TAG = "ManageCameras";
    public final static String UID = "uid";
    private final static int MENU_BACK = 0;
    private Menu menu;
    private Map<MenuItem, Camera> buttonCameraMap;

    /**
     * Initially set our views and read the database for existing cameras
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pageTitle = R.string.title_cameras;
        updateTitle(pageTitle);
        buttonCameraMap = new HashMap<>();
    }

    /**
     * Override to recreate the action menu for new camera data
     */
    @Override
    protected void onResume() {
        invalidateActionMenu();
        super.onResume();
    }

    /**
     * Create the action menu and initialize the menu items
     * @param menu Menu to inflate
     * @return Menu inflated
     */
    @Override
    protected boolean onCreateActionMenu(Menu menu) {
        super.onCreateActionMenu(menu);
        getMenuInflater().inflate(R.menu.settings_manage, menu);
        this.menu = menu;

        LoadCamerasTask loadCamerasTask = new LoadCamerasTask(this);
        loadCamerasTask.execute();

        return true;
    }

    /**
     * Inform the action menu we are initially starting with an empty menu and populating later
     * @return Zero index of the menu item list
     */
    @Override
    protected int getDefaultAction() {
        return MENU_BACK;
    }

    /**
     * Creates menu items for a list of cameras, uses orderInCategory to assign order
     * @param cameras List of cameras to create menu items for
     */
    private void createCameraMenuItems(List<Camera> cameras){
        Log.d(TAG, "Creating camera menu items");
        buttonCameraMap.clear();

        int orderInCategory = 101;
        for(Camera camera : cameras){
            MenuItem cameraButton = this.menu.add(Menu.NONE, this.menu.hashCode(), orderInCategory, camera.getPresentableName());
            cameraButton.setOnMenuItemClickListener(menuItem -> {
                onEditClicked(menuItem);
                return true;
            });
            buttonCameraMap.put(cameraButton, camera);
            orderInCategory++;
        }
    }

    /**
     * Called when a camera menu item is selected, opens EditCamera
     * @param item Selected menu item
     */
    public void onEditClicked(MenuItem item){
        Log.d(TAG, "Camera selected");
        Camera selectedCamera = buttonCameraMap.get(item);

        Intent openEdit = new Intent(this, EditCamera.class);
        if (selectedCamera != null) {
            openEdit.putExtra(UID, selectedCamera.getUID());
            startActivity(openEdit);
        }else{
            Toast.makeText(this, getString(R.string.toast_error), Toast.LENGTH_LONG).show();
        }

    }

    private static class LoadCamerasTask extends AsyncTask<Void, Void, Void> {

        CamerasDatabase database;
        private WeakReference<Manage> manageReference;
        private List<Camera> camerasFromRoom;

        /**
         * Constructor for LoadCamerasTask, connects to database and creates a weak reference to the
         * Manage activity
         * @param context Context of Manage executing the task
         */
        LoadCamerasTask(Manage context) {
            manageReference = new WeakReference<>(context);
            database = Room.databaseBuilder(context, CamerasDatabase.class, "camerasDB")
                    .build();
        }

        /**
         * Work to be done in the background, need to load existing cameras from database
         * @param voids Not used, null
         * @return Null
         */
        @Override
        protected Void doInBackground(Void... voids) {
            camerasFromRoom = database.getCameraDAO().getCameras();
            return null;
        }

        /**
         * Executed on cameras loaded, initializes the recycler view with our cameras
         * @param aVoid Null
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            Manage manageActivity = manageReference.get();
            if (manageActivity == null || manageActivity.isFinishing()) {
                return;
            }
            database.close();
            manageActivity.createCameraMenuItems(camerasFromRoom);
        }
    }
}