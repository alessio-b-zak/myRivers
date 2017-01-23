package com.bitbusters.android.speproject;
import android.graphics.Bitmap;


/**
 * Created by cp153 on 06/12/2016.
 */

class Image {
    private Bitmap image;
    private double latitude;
    private double longitude;
    private String comment;

    Image(Bitmap image, double latitude, double longitude, String comment) {
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
        this.comment = comment;
    }

    public Bitmap getImage() {
        return image;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getComment() {
        return comment;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
