package com.vuzix.securityviewer;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class License implements Parcelable {

    public static final Creator<License> CREATOR = new Creator<License>() {
        @Override
        public License createFromParcel(Parcel source) {
            return new License(source);
        }

        @Override
        public License[] newArray(int size) {
            return new License[size];
        }
    };

    //
    private final String license_abbreviation;
    private final String license_name;
    private String software_name;
    private String filename;

    public License(String license_name, String license_abbreviation, String filename, String software_name) {
        if(license_name == null) throw new NullPointerException("name is null");
        if(license_abbreviation == null) throw new NullPointerException("abbreviation is null");
        if(filename == null) throw new NullPointerException("filename is null");
        if(software_name == null) throw new NullPointerException("software_name is null");
        this.license_name = license_name;
        this.filename = filename;
        this.license_abbreviation = license_abbreviation;
        this.software_name = software_name;
    }

    protected License(Parcel in) {
        this.filename = in.readString();
        this.license_abbreviation = in.readString();
        this.license_name = in.readString();
        this.software_name = in.readString();
    }

    public Uri getContentUri() {
        return new Uri.Builder()
                .scheme("file")
                .path("/android_asset")
                .appendPath(filename)
                .build();
    }

    public String getAbbreviation() {
        return license_abbreviation;
    }

    public String getFilename() {
        return filename;
    }

    public String getName() {
        return license_name;
    }

    public String getSoftwareName() {
        return software_name;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filename);
        dest.writeString(this.license_abbreviation);
        dest.writeString(this.license_name);
        dest.writeString(this.software_name);
    }
}
