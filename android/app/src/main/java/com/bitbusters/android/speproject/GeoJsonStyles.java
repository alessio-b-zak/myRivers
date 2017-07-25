package com.bitbusters.android.speproject;

import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import static com.bitbusters.android.speproject.CDEPoint.GOOD;
import static com.bitbusters.android.speproject.CDEPoint.MODERATE;
import static com.bitbusters.android.speproject.CDEPoint.OVERALL;
import static com.bitbusters.android.speproject.CDEPoint.POOR;

/**
 * Created by mihajlo on 03/07/17.
 */

public class GeoJsonStyles {

    public static GeoJsonLineStringStyle geoJsonLineStringStyle() {
        GeoJsonLineStringStyle style = new GeoJsonLineStringStyle();
        style.setColor(0xCC19A1F9);
        style.setClickable(true);
        return style;
    }

    public static GeoJsonPolygonStyle geoJsonPolygonStyle() {
        GeoJsonPolygonStyle style = new GeoJsonPolygonStyle();
        style.setFillColor(0x664FF2EA);
        style.setStrokeColor(0xCC38B7B1);
        return style;
    }

}
