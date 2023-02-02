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
package com.vuzix.securityviewer.model;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.vuzix.securityviewer.StreamActivity;

@Entity(tableName = "cameras")
public class Camera{

    @PrimaryKey(autoGenerate = true)
    private int UID;
    private String macAddress;
    private String hostName;
    private String model;
    private String firmwareVersion;
    private String hardwareID;
    private String manufacturer;
    private String serialNumber;
    private String cameraNickname;
    private String username;
    private String password;
    private String network;
    @Ignore
    private boolean isLocked;
    @Ignore
    private boolean selected = false;
    @Ignore
    public final static int ERROR_UNAUTHORIZED = 401;

    /**
     * Set the UID of this camera
     * @param UID UID to set camera to
     */
    public void setUID(int UID) { this.UID = UID; }

    /**
     * Set the MAC address for this camera
     * @param macAddress MAC Address for the camera
     */
    public void setMacAddress(String macAddress){
        this.macAddress = macAddress;
    }

    /**
     * Set the host name for this camera
     * @param hostName Host name of the camera
     */
    public void setHostName(String hostName){ this.hostName = hostName; }

    /**
     * Set the model for this camera
     * @param model Model of the camera
     */
    public void setModel(String model){ this.model = model; }

    /**
     * Set the firmware version for this camera
     * @param firmwareVersion Firmware version for the camera
     */
    public void setFirmwareVersion(String firmwareVersion){ this.firmwareVersion = firmwareVersion; }

    /**
     * Set the hardware ID for this camera
     * @param hardwareID Hardware ID for the camera
     */
    public void setHardwareID(String hardwareID){ this.hardwareID = hardwareID; }

    /**
     * Set the manufacturer for this camera
     * @param manufacturer Manufacturer for the camera
     */
    public void setManufacturer(String manufacturer){ this.manufacturer = manufacturer; }

    /**
     * Set the serial number for this camera
     * @param serialNumber Serial number for the camera
     */
    public void setSerialNumber(String serialNumber){
        this.serialNumber = serialNumber;
    }

    /**
     * Set the nickname for the camera
     * @param nickname Nickname for the camera
     */
    public void setCameraNickname(String nickname){
        this.cameraNickname = nickname;
    }

    /**
     * Set the credentials for this camera
     * @param username Username for the camera
     * @param password Password for the camera
     */
    public void setCredentials(String username, String password){
        this.setUsername(username);
        this.setPassword(password);
    }

    /**
     * Set the username for this camera
     * @param username Username for the camera
     */
    public void setUsername(String username){
        this.username = username;
    }

    /**
     * Set the password for this camera
     * @param password Password for the camera
     */
    public void setPassword(String password){
        this.password = password;
    }

    /**
     * Set network for this camera
     * @param network Network for the camera
     */
    public void setNetwork(String network){
        this.network = network;
    }

    /**
     * Set locked status for this camera
     * @param locked Camera authentication required
     */
    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    /**
     * Set camera as the selected camera in the recycler view
     * @param selected Selected status for the camera
     */
    public void setSelected(boolean selected){
        this.selected = selected;
    }

    /**
     * Get the UID for this camera
     * @return UID of this camera
     */
    public int getUID() { return this.UID; }

    /**
     * Get MAC Address for this camera
     * @return MAC Address of this camera
     */
    public String getMacAddress() {
        return this.macAddress;
    }

    /**
     * Get host name for this camera
     * @return Host name of this camera
     */
    public String getHostName() { return this.hostName; }

    /**
     * Get model for this camera
     * @return Model of this camera
     */
    public String getModel() { return this.model; }

    /**
     * Get firmware version of this camera
     * @return Firmware version of this camera
     */
    public String getFirmwareVersion() { return this.firmwareVersion; }

    /**
     * Get hardware ID of this camera
     * @return Hardware ID of this camera
     */
    public String getHardwareID() { return this.hardwareID; }

    /**
     * Get manufacturer of this camera
     * @return Manufacturer of this camera
     */
    public String getManufacturer() { return this.manufacturer; }

    /**
     * Get serial number of this camera
     * @return Serial number of this camera
     */
    public String getSerialNumber() { return this.serialNumber; }

    /**
     * Get the nickname of this camera
     * @return Nickname of this camera
     */
    public String getCameraNickname(){ return this.cameraNickname; }

    /**
     * Get authentication username of this camera
     * @return Authentication username for this camera
     */
    public String getUsername(){ return this.username; }

    /**
     * Get authentication password for this camera
     * @return Authentication password for this camera
     */
    public String getPassword(){ return this.password; }

    /**
     * Get network for this camera
     * @return Network this camera was registered under
     */
    public String getNetwork() { return this.network; }

    /**
     * Get authentication status for this camera
     * @return Locked status for this camera
     */
    public boolean isLocked() {
        return isLocked;
    }

    /**
     * Get selected status for the recycler view
     * @return Selected status for the recycler view
     */
    public boolean isSelected() { return selected; }

    /**
     * Check to see if an object is equal to this camera
     * @param obj An object that can be a camera
     * @return
     */
    public boolean equals(Object obj){
        if(getClass() == obj.getClass()){
            if(((Camera) obj).getUID() == this.getUID()){
                return true;
            }else if(((Camera) obj).getHostName().equals(this.getHostName())){
                return true;
            }else return ((Camera) obj).getSerialNumber().equals(this.getSerialNumber());

        }
        return false;
    }

    /**
     * Get the best name to present to the user
     * @return Best possible name for this camera to present to the user
     */
    public String getPresentableName(){
        if(getCameraNickname() != null){
            return getCameraNickname();
        }else{
            return getHostName();
        }
    }

    /**
     * Method for opening a new StreamActivity for viewing the camera
     * @param context Context that is looking to open the camera
     */
    public void openStream(Context context){
        Log.d(this.hostName, "Opening stream");
        final Intent intent = new Intent(context, StreamActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(StreamActivity.EXTRA_HOSTNAME, this.getHostName());
        intent.putExtra(StreamActivity.EXTRA_CAMERA_NAME, this.getPresentableName());
        intent.putExtra(StreamActivity.EXTRA_USERNAME, this.getUsername());
        intent.putExtra(StreamActivity.EXTRA_PASSWORD, this.getPassword());
        context.startActivity(intent);
    }
}
