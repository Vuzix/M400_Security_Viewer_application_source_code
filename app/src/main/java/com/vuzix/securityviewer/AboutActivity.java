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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.vuzix.hud.actionmenu.ActionMenuActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


public class AboutActivity extends ActionMenuActivity {

    private static final int pageTitle = R.string.title_about;

    private TextView headerPageTitle;
    private TextView versionText;

    public  static final License[]  Licenses = new License[] {
        new License("Vuzix General License, Version 1.0", "VZX", "vuzix_license", "Security Viewer"),
        new License("Apache License, Version 2.0", "ALv2", "apache_license", "ONVIF"),
        new License("Apache License, Version 2.0", "ALv2", "apache_license", "okhttp-digest"),
        new License("GNU General Public License, Version 2.0", "GPLv2", "gpl2_license", "VLC Core Library"),
    };

    /**
     * Initially set the views
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        versionText = findViewById(R.id.about_version);
        headerPageTitle = findViewById(R.id.header_page_title);

        headerPageTitle.setText(pageTitle);

        try {
            versionText.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        legal_ThirdParty();

    }

    public void legal_ThirdParty() {

        //TODO: Add Recursive File reading and add to TextView
        StringBuilder licenses_string = new StringBuilder();
        try{
            AssetManager as = getAssets();


            for (License li : Licenses)
            {
                licenses_string.append("License For: " + li.getSoftwareName() + "\n") ;
                licenses_string.append("License Name: " + li.getName() + "\n") ;

                licenses_string.append(readFromAssets(as,li.getFilename()));
                licenses_string.append("\n\n");

            }
//            for (String path : as.list("legal"))
//            {
//                //PUT IN STRING AND IN TEXT - VIEW
//                licenses_string.append("License For: " + path.substring(0,path.indexOf("_")) + "\n") ;
//
//                licenses_string.append(readFromAssets(as,path));
//                licenses_string.append("\n\n");
//            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            //PUT DID NOT OPEN ERROR and eat the exception
        }

        TextView legal_textview = findViewById(R.id.legal_textView);
        legal_textview.setText(licenses_string.toString());

    }

    public String readFromAssets(AssetManager as, String filename) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(as.open("legal" + File.separator +filename)));

        // do reading, usually loop until end of file reading
        StringBuilder sb = new StringBuilder();
        String mLine = reader.readLine();
        while (mLine != null) {
            sb.append(mLine); // process line
            mLine = reader.readLine();
        }
        reader.close();
        return sb.toString();
    }
}