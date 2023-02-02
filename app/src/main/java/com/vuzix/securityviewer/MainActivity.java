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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.Nullable;

import com.vuzix.hud.actionmenu.ActionMenuActivity;
import com.vuzix.securityviewer.settings.DiscoverCameras;

import static android.view.KeyEvent.KEYCODE_DPAD_RIGHT;

public class MainActivity extends ActionMenuActivity {

    private final static String TAG = "MainActivity";
    private final static String FIRST_LAUNCH = "first_launch";
    public final static String FROM_FTUE = "from_ftue";
    private SharedPreferences sharedPreferences;

    /**
     * Get our shared preferences for seeing if this is first launch
     * @param savedInstanceState Null
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "Starting FTUE");
        super.onCreate(savedInstanceState);
        sharedPreferences = this.getPreferences(MODE_PRIVATE);
    }

    /**
     * Overriding to catch if this is the first launch. If yes, show FTUE; else go home
     */
    @Override
    protected void onResume() {
        boolean firstAppLaunch = sharedPreferences.getBoolean(FIRST_LAUNCH, true);
        if(!firstAppLaunch){
            Log.d(TAG, "Not first launch, going home");
            Intent goHomeIntent = new Intent(this, HomeActivity.class);
            startActivity(goHomeIntent);
            finish();
        }else{
            Log.d(TAG, "First launch, displaying FTUE");
            setContentView(R.layout.activity_welcome);
        }
        super.onResume();
    }

    /**
     * Method to capture onKeyDown events. Used for swipe forward gesture
     * @param keyCode Individual key code
     * @param event Individual KeyEvent object
     * @return super.onKeyDown
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KEYCODE_DPAD_RIGHT){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(FIRST_LAUNCH, false);
            editor.apply();

            Intent continueIntent = new Intent(this, DiscoverCameras.class);
            continueIntent.putExtra(FROM_FTUE, true); // Tell Discover this is FTUE
            this.startActivity(continueIntent);
        }
        return super.onKeyDown(keyCode, event);
    }
}
