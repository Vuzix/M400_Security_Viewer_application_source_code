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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import com.vuzix.hud.actionmenu.ActionMenuActivity;
import com.vuzix.securityviewer.R;

public class Settings extends ActionMenuActivity {

    private ConnectivityManager connectivityManager;
    protected WifiManager wifiManager;

    private TextView headerPageTitle;
    private TextView headerNetwork;
    private TextView subtitleText;


    /**
     * Initially set our views for high-level app data (network, page titles)
     * @param savedInstanceState Not used, null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        headerPageTitle = findViewById(R.id.header_page_title);
        headerNetwork = findViewById(R.id.header_network);
        subtitleText = findViewById(R.id.subtitle_settings);

        startNetworkMonitor();
    }

    /**
     * Override to unregister wifi receiver
     */
    @Override
    protected void onDestroy() {
        unregisterReceiver(wifiReceiver);
        super.onDestroy();
    }

    /**
     * Used to adjust the action menu to center in the display
     * @return
     */
    @Override
    protected int getActionMenuGravity() { return Gravity.CENTER; }

    /**
     * Override the action menu to always be visible
     * @return
     */
    @Override
    protected boolean alwaysShowActionMenu() { return true; }

    /**
     * Override the action menu to default to first option other than the back button
     * @return 1 for first non-back item in list
     */
    @Override
    protected int getDefaultAction() {
        return 1;
    }

    /**
     * Method used by subclasses to update the page title
     * @param string Title of the page
     */
    protected void updateTitle(int string){
        this.headerPageTitle.setText(string);
    }

    /**
     * Method used by subclasses to update the subtitle
     * @param string Subtitle text to display
     */
    protected void updateSubtitle(String string){
        this.subtitleText.setVisibility(View.VISIBLE);
        this.subtitleText.setText(string);
    }

    /**
     * Method to grab the latest network information and any future updates to display to the user
     */
    private void startNetworkMonitor(){
        updateNetworkInfo(); // get latest connection info

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, intentFilter);
    }

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                updateNetworkInfo();
            }
        }
    };

    /**
     * Method used to adjust the network info text displayed to the user
     */
    protected void updateNetworkInfo(){
        if(wifiConnected()){
            headerNetwork.setText(wifiManager.getConnectionInfo().getSSID().replace("\"", ""));
        }else{
            headerNetwork.setText("");
        }
    }

    /**
     * Method used to determine if a wifi network is currently connected
     * @return Current network connection status
     */
    protected boolean wifiConnected(){
        for(Network network : connectivityManager.getAllNetworks()){
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
            if(networkInfo != null){
                if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
                    return networkInfo.isConnected();
                }
            }
        }

        return false;
    }
}