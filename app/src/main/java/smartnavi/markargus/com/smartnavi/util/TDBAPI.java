package smartnavi.markargus.com.smartnavi.util;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import smartnavi.markargus.com.smartnavi.models.Action;
import smartnavi.markargus.com.smartnavi.models.Distance;
import smartnavi.markargus.com.smartnavi.models.Place;
import smartnavi.markargus.com.smartnavi.models.Route;

/**
 * Class that interchanges messages via API with Context Service.
 * Created by mc on 11/13/14.
 */
public class TDBAPI {

    public String MODEL_SERVER_ADDRESS = "";
    private String deviceName;
    private boolean connected = false;

    private static TDBAPI modelServer;

    public TDBAPI() {

    }

    @Deprecated
    public TDBAPI(String ipAddress, int port){
        MODEL_SERVER_ADDRESS = "http://" + ipAddress + ":" + port;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public static TDBAPI getModelServer() {
        return modelServer;
    }

    public static void setModelServer(TDBAPI modelServer) {
        TDBAPI.modelServer = modelServer;
    }

    public String getServerAddress() {
        return MODEL_SERVER_ADDRESS;
    }

    public void setServerAddress(String ipAddress, int port){
        MODEL_SERVER_ADDRESS = "http://" + ipAddress + ":" + port;
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    /**
     * Must be called inside an AsyncTask.
     * @return an ordered route.
     */
    public Route getOrderedRoute(){
//        Route route;
        try {
            JSONObject object = new JSONObject(getJSON(MODEL_SERVER_ADDRESS));
            return Route.parseJSONObject(object);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Obtains a JSON String from a server given an address.
     *
     * @param address the address of the model server.
     * @return a string representation of the response.
     */
    public String getJSON(String address){
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(address);
        httpGet.setHeader("Accept", "application/json");
        try{
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode == 200){
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while((line = reader.readLine()) != null){
                    builder.append(line);
                }
            } else {
                Log.e("ModelServer", "Failed to get JSON object");
            }
        }catch(ClientProtocolException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return builder.toString();
    }

    /**
     * Obtains a JSON String from a server given an address.
     *
     * @param beaconId the reference beacon.
     * @return a string representation of the response.
     */
    public JSONObject getPlace(String beaconId){
        StringBuilder builder = new StringBuilder();
        JSONObject jsonResponse = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(getServerAddress() + "/?beacon-id=" + beaconId);
        httpGet.setHeader("Accept", "application/json");
        try{
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if(statusCode == 200){
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while((line = reader.readLine()) != null){
                    builder.append(line);
                }
            } else {
                Log.e("ModelServer", "Failed to get JSON object");
            }
            jsonResponse = new JSONObject(builder.toString());
        }catch(ClientProtocolException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonResponse;
    }

    public void postJson(String url){
        HttpClient client = new DefaultHttpClient();
        String responseBody;
        JSONObject jsonObject = new JSONObject();

        try {
            HttpPost post = new HttpPost(url);
            jsonObject.put("field1", "");
            jsonObject.put("field2", "");

            StringEntity se = new StringEntity(jsonObject.toString());

            post.setEntity(se);
            post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setHeader("Content-type", "application/json");
            Log.e("webservice request","executing");

            client.execute(post);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void postLocation(Place place){
        HttpClient client = new DefaultHttpClient();
        JSONObject placeJsonObject = place.asJSONObject();
        JSONObject jsonObject = new JSONObject();
        try {
            HttpPost post = new HttpPost(MODEL_SERVER_ADDRESS + "/locations/");
            jsonObject.put("device_name", getDeviceName());
            jsonObject.put("type", "smartphone");
            jsonObject.put("uid", "mc-galaxy-alpha");
            jsonObject.put("place", placeJsonObject);
            StringEntity se = new StringEntity(jsonObject.toString());
            post.setEntity(se);
            post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            post.setHeader("Content-type", "application/json");
            Log.e("webservice request","executing");
            client.execute(post);
        } catch(JSONException ex){
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Makes a POST request to /locations/ URI from server.
     * @param beaconId is the Id of the found BLE Beacon.
     */
    public void postLocation(String beaconId){
        if(isConnected()) {
            HttpClient client = new DefaultHttpClient();
            JSONObject jsonObject = new JSONObject();
            try {
                HttpPost post = new HttpPost(MODEL_SERVER_ADDRESS + "/locations/");
                jsonObject.put("device_name", getDeviceName());
                jsonObject.put("type", "smartphone");
                jsonObject.put("uid", "mc-galaxy-alpha");
                jsonObject.put("beacon-id", beaconId);
                StringEntity se = new StringEntity(jsonObject.toString());
                post.setEntity(se);
                post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setHeader("Content-type", "application/json");
                Log.e("postLocation", "executing with Beacon ID...");
                client.execute(post);
            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            Log.e("postLocation", "Not connected to server.");
        }
    }

    /**
     * Makes a POST request to /proxemics/ URI from server.
     * @param beaconId is the Id of the found BLE Beacon.
     */
    public void postProxemicDistance(String beaconId, Distance distance, double value){
        if(isConnected()) {
            HttpClient client = new DefaultHttpClient();
            JSONObject jsonObject = new JSONObject();
            try {
                HttpPost post = new HttpPost(MODEL_SERVER_ADDRESS + "/proxemics/");
                jsonObject.put("device_name", getDeviceName());
                jsonObject.put("type", "smartphone");
                jsonObject.put("uid", "mc-galaxy-alpha");
                jsonObject.put("beacon-id", beaconId);
                jsonObject.put("proxemic-distance", distance.name());
                jsonObject.put("proxemic-distance-value", value);
                StringEntity se = new StringEntity(jsonObject.toString());
                post.setEntity(se);
                post.setHeader(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                post.setHeader("Content-type", "application/json");
                Log.e("postProxemicDistance", "executing with Beacon ID...");
                client.execute(post);
            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else{
            Log.e("postLocation", "Not connected to server.");
        }
    }

    public Action getAction() {
        try {
            JSONObject jsonObject = new JSONObject(getJSON(MODEL_SERVER_ADDRESS + "/locations/"));
            return Action.parseJSONObject(jsonObject);
        } catch (JSONException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public static TDBAPI getInstance() {
        if(modelServer == null){
            modelServer = new TDBAPI();
        }
        return modelServer;
    }

}
