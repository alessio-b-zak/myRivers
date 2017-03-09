
package com.bitbusters.android.speproject;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
//import static com.google.android.gms.internal.zznu.is;

public class ImageUploader extends AsyncTask<Image, Void, String> {
    private static final String DEBUG_TAG = "IMAGES_UPLOADER";
//    private OnTaskCompleted listener;

//    public ImagesDownloader(OnTaskCompleted listener) {
//        this.listener = listener;
//    }

    @Override
    protected String doInBackground(Image...params) {
        try {
            String comment = params[0].getComment();
            Bitmap bitmap = params[0].getImage();
            Double latitude = params[0].getLatitude();
            Double longitude = params[0].getLongitude();
            PhotoTag tag = params[0].getPhotoTag();
            String attachmentName = "bitmap";
            String attachmentFileName = "bitmap.bmp";
            String crlf = "\r\n";
            String twoHyphens = "--";
            String boundary =  "";

            HttpURLConnection httpUrlConnection = null;
            URL url = new URL("http://139.59.184.70:8080/uploadImage");
            //URL url = new URL("http://172.23.215.243:3000/uploadImage");
            Log.d(DEBUG_TAG, " URL UPLOAD : " + url.toString());
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setUseCaches(false);
            httpUrlConnection.setDoOutput(true);
            Log.d(DEBUG_TAG, "Set request body to true");
            httpUrlConnection.setRequestMethod("POST");
            httpUrlConnection.setRequestProperty("Connection", "Keep-Alive");
            httpUrlConnection.setRequestProperty("Comment", comment);
            httpUrlConnection.setRequestProperty("Tag", tag.name());
            httpUrlConnection.setRequestProperty("Latitude", String.valueOf(latitude));
            httpUrlConnection.setRequestProperty("Longitude", String.valueOf(longitude));
            httpUrlConnection.setRequestProperty("Cache-Control", "no-cache");
            httpUrlConnection.setRequestProperty("Content-Type", "image/jpeg");

            // Gets stuck here, though Not being used.
            //int response = httpUrlConnection.getResponseCode();

            Log.d(DEBUG_TAG, "Url is: " + url);
            //Log.d(DEBUG_TAG, "The response is: " + response);

            OutputStream request = httpUrlConnection.getOutputStream();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, request);

            byte[] byteArray = stream.toByteArray();
            request.write(byteArray);
            Log.d(DEBUG_TAG, "Image Converted to JPEG ");
            stream.close();
            Log.d(DEBUG_TAG, "log 1 ");
            request.close();
            Log.d(DEBUG_TAG, "log 2 ");

            // Get response:
            // Gets stuck here...
            InputStream responseStream =
                    new BufferedInputStream(httpUrlConnection.getInputStream());
            Log.d(DEBUG_TAG, "log 3 ");

            BufferedReader responseStreamReader =
                    new BufferedReader(new InputStreamReader(responseStream));
            Log.d(DEBUG_TAG, "log 4 ");

            String line = "";
            Log.d(DEBUG_TAG, "log 5 ");

            StringBuilder stringBuilder = new StringBuilder();
            Log.d(DEBUG_TAG, "log 6 ");

            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            Log.d(DEBUG_TAG, "log 7 ");

            responseStreamReader.close();
            Log.d(DEBUG_TAG, "log 8 ");

            responseStream.close();
            Log.d(DEBUG_TAG, "log 9 ");

            // Close the connection:
            httpUrlConnection.disconnect();
            Log.d(DEBUG_TAG, "log 10 ");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Upload Successful";

    }
    // onPostExecute displays the results of the AsyncTask.

    protected void onPostExecute(int result) {
        System.out.println("ImageUploader onPostExecute called.");

    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

}
