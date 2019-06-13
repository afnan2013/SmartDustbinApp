package com.example.anonymous.googlemaps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_GRANTED_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;


    //variables
    private boolean mLocationPermissionGranted = false;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ArrayList<LatLng> listpoints;
    private LatLng currentLatLng=null;
    public String JSON_STRING = "";
    public String flag = "";
    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#3C3F41")));

        getLocationPermission();
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        listpoints = new ArrayList<>();

        flag = getIntent().getStringExtra("flag");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        currentLatLng = getLocation();                          //Get My Location
        //getDeviceLocation();
        Log.d(TAG, "OnMapReady : Current Location : "+currentLatLng);
        if(currentLatLng != null){
            listpoints.add(currentLatLng);
        }
        else{
            Toast.makeText(this, "Cannot find current location", Toast.LENGTH_SHORT).show();
        }


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                                                                            != PackageManager.PERMISSION_GRANTED
                                                                                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                                                                                            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        JsonParser jasonParser = new JsonParser();
        jasonParser.execute();
        Log.d(TAG, "onMapReady: Json Output : "+JSON_STRING);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                //reset marker when already 3
                if(listpoints.size() == 3){
                    listpoints.clear();
                    mMap.clear();
                }
                //save first point select
                listpoints.add(latLng);
                //create marker
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);

                if(listpoints.size() == 2){
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                }
                else{
                    //add marker to the second point
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }
                mMap.addMarker(markerOptions);

                //ToDo : request get direction code bellow
                if(listpoints.size() == 3){
                    //Create the url to get request from first marker to second marker
                    String url = CreateUrl.getRequestUrl(listpoints.get(0), listpoints.get(1),listpoints.get(2));
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);

                }

            }
        });

    }


    private LatLng getLocation(){

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            else{
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location != null){
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    LatLng currentLocation = new LatLng(latitude, longitude);
                    return  currentLocation;
                }
            }
        }catch (SecurityException e){
            e.printStackTrace();
        }
        return null;
    }




    private String requestDirection(String reqUrl){
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;

        try {
            URL url = new URL(reqUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            //Get the response result
            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            StringBuffer stringBuffer = new StringBuffer();
            String line = "";

            while ( (line = bufferedReader.readLine()) != null){
                stringBuffer.append(line);
            }


            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                httpURLConnection.disconnect();
            }
        }
        if(responseString != null)
            Log.d(TAG, "requestDirection: Got the response");
        else
            Log.d(TAG, "requestDirection: Getting Response failed");
        //Toast.makeText(this, responseString , Toast.LENGTH_SHORT).show();
        return  responseString;
    }






    private void getDeviceLocation(){
        Log.i(TAG, "getDeviceLocation: getting the device current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();

                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(),
                                    currentLocation.getLongitude()), DEFAULT_ZOOM);
                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        }catch (SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityExecption"+ e.getMessage() );
        }
    }

    private  void moveCamera(LatLng latlng, float zoom){
        Log.d(TAG, "moveCamera: moving camera to : lat :"+ latlng.latitude + ", lng : "+ latlng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng,zoom));
    }


    private void initMap(){
        Log.d(TAG, "initMap: initialising the map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }


    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: getting location persmission");
        String[] permissions = {FINE_LOCATION,
                COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_GRANTED_CODE);
            }
        }else {
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_GRANTED_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: override onRequestPermissionsResult method called");
        mLocationPermissionGranted = false;

        switch (requestCode){
            case LOCATION_PERMISSION_GRANTED_CODE:{
                if(grantResults.length >0){
                    for(int i =0; i< grantResults.length ; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    //initialise our map
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    initMap();
                }
            }
        }
    }



    public class JsonParser extends AsyncTask<Void, Void, String>{
        String json_url;
        String json_string;
        JSONObject jsonObject;
        JSONArray jsonArray;

        @Override
        protected void onPreExecute() {
            json_url = "http://waste.dgted.com/initandroid.php";
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                URL url = new URL(json_url);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.connect();
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line = "";
                StringBuilder stringBuilder = new StringBuilder();
                while((line = bufferedReader.readLine()) != null){
                    stringBuilder.append(line+"\n");
                }

                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();

                return stringBuilder.toString().trim();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return  null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "onPostExecute: "+result);
            getJson(result);
            json_string = result;
            List<Float> check = new ArrayList<>();
            try {
                jsonObject = new JSONObject(json_string);
                jsonArray = jsonObject.getJSONArray("locations");

                int count = 0;
                float max = 0;
                int maxindex=0;

                while(count < jsonArray.length()){
                    JSONObject JO = jsonArray.getJSONObject(count);
                    //double lat = JO.getDouble("lat");
                    float lat = (float)JO.getDouble("lat");
                    float lon = (float)JO.getDouble("lon");
                    int ratio = JO.getInt("ratio");

                    check.add(lat);
                    check.add(lon);

                    LatLng latLng = new LatLng(lat, lon);

                    if(ratio <=4) {
                        listpoints.add(latLng);             //adding the locations to listpoints array

                    }

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);

                    if(ratio < 10){
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }
                    else if(ratio < 20  && ratio >10){
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                    }
                    else{
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    }
                    mMap.addMarker(markerOptions);

                    count++;
                }
                Log.d(TAG, "onPostExecute: check"+ check);
                Log.d(TAG, "onPostExecute: Listpoints"+ listpoints);

                //Create the url to get request from first marker to second marker
                //String url = CreateUrl.getRequestUrl(listpoints.get(0), listpoints.get(2),listpoints.get(1));
                if(flag.equals("true")) {

                    //Collections.swap(listpoints, 1, maxindex);
                    Log.d(TAG, "onPostExecute: Listpoints after correction : "+ listpoints);

                    String url = CreateUrl.getRequestURL(listpoints);                         // **** URL maker
                    Log.d(TAG, "onPostExecute: Url = " + url);
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);
                }



            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void getJson(String json){
        JSON_STRING = json;
    }


    public class TaskRequestDirections extends AsyncTask<String, Void, String>{


        @Override
        protected String doInBackground(String... params) {
            String responseString = "";

            responseString = requestDirection(params[0]);
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Parse json here
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void , List<List<HashMap<String, String>>> >{

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... params) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jsonObject = new JSONObject(params[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            //Get the list route and display it into the map

            ArrayList points = null;

            PolylineOptions polylineOptions = null;

            for(List<HashMap<String, String>> path : lists) {
                points = new ArrayList();
                polylineOptions = new PolylineOptions();

                for(HashMap<String, String> point : path){
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat,lon));
                }

                polylineOptions.addAll(points);
                polylineOptions.width(10);
                polylineOptions.color(Color.BLUE);
                polylineOptions.geodesic(true);
            }

            if(polylineOptions != null){
                mMap.addPolyline(polylineOptions);
            }
            else
                Toast.makeText(MapActivity.this, "Direction not found!", Toast.LENGTH_SHORT).show();
        }
    }


}
