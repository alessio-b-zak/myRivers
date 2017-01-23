package com.bitbusters.android.speproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cp153 on 06/12/2016.
 */

public class InputStreamToImage {
    public List<Image> readImageStream(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        try {
            return readMessagesArray(reader);
        } finally {
            reader.close();
        }
    }

    public List<Image> readMessagesArray(JsonReader reader) throws IOException {
        List<Image> messages = new ArrayList<Image>();
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                messages.add(readMessage(reader));
            }
            reader.endArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    public Image readMessage(JsonReader reader) throws IOException {
        String comment = null;
        double latitude = 0.0, longitude = 0.0;
        List<Byte> imgPixels = new ArrayList<Byte>();
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("image")) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        name = reader.nextName();
                        if (name.equals("data")) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                Byte pixel = (byte) reader.nextInt();
                                imgPixels.add(pixel);
                            }
                            reader.endArray();
                        } else {
                            reader.skipValue();
                        }
                    }
                    reader.endObject();
                } else if (name.equals("comment")) {
                    comment = reader.nextString();
                } else if (name.equals("loc")) {
                    reader.beginArray();
                    longitude = reader.nextDouble();
                    latitude = reader.nextDouble();
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] imgBytePrimitive = arrayListByteToPrimitive(imgPixels);
        Bitmap image = BitmapFactory.decodeByteArray(imgBytePrimitive, 0, imgBytePrimitive.length);
        return new Image(image, latitude, longitude, comment);
    }

    private byte[] arrayListByteToPrimitive(List<Byte> arrayListByte) {
        byte[] primitiveByte = new byte[arrayListByte.size()];
        for (int i = 0; i < primitiveByte.length; i++){
            primitiveByte[i] = arrayListByte.get(i);
        }
        return primitiveByte;
    }
}
