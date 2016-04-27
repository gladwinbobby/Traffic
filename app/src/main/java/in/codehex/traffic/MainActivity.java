package in.codehex.traffic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import in.codehex.traffic.util.AppController;
import in.codehex.traffic.util.Const;

public class MainActivity extends AppCompatActivity implements
        LocationListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    Toolbar toolbar;
    FloatingActionButton fabSubmit;
    AutoCompleteTextView autoSource, autoDestination;
    EditText editTraffic, editDistance;
    Intent intent;
    String url;
    GoogleApiClient googleApiClient;
    Location location;
    LocationRequest locationRequest;
    double lat, lng;
    ArrayList<String> places;
    ArrayAdapter<String> adapter;
    String phone, source, destination, traffic, distance;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        autoSource = (AutoCompleteTextView) findViewById(R.id.source);
        autoDestination = (AutoCompleteTextView) findViewById(R.id.destination);
        editTraffic = (EditText) findViewById(R.id.traffic);
        editDistance = (EditText) findViewById(R.id.distance);
        fabSubmit = (FloatingActionButton) findViewById(R.id.submit);

        sharedPreferences = getSharedPreferences(Const.pref, MODE_PRIVATE);

        Boolean check = sharedPreferences.contains("phone");
        if (check) {
            phone = sharedPreferences.getString("phone", null);
        } else {
            intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        autoSource.setThreshold(0);
        autoDestination.setThreshold(0);

        autoSource.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() <= 3) {
                    places = new ArrayList<String>();
                    updateList(s.toString(), "source");
                }
            }
        });

        autoDestination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() <= 3) {
                    places = new ArrayList<String>();
                    updateList(s.toString(), "destination");
                }
            }
        });

        fabSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                source = autoSource.getText().toString();
                destination = autoDestination.getText().toString();
                traffic = editTraffic.getText().toString();
                distance = editDistance.getText().toString();
                if (source.length() > 2 && destination.length() > 2
                        && !traffic.isEmpty() && !distance.isEmpty()) {
                    intent = new Intent(getApplicationContext(), MapActivity.class);
                    intent.putExtra("source", source);
                    intent.putExtra("destination", destination);
                    intent.putExtra("traffic", traffic);
                    intent.putExtra("distance", distance);
                    intent.putExtra("lat", lat);
                    intent.putExtra("lng", lng);
                    startActivity(intent);
                }
            }
        });

        createLocationRequest();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (googleApiClient.isConnected())
            startLocationUpdates();
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location loc) {
        location = loc;
        lat = location.getLatitude();
        lng = location.getLongitude();
        updateLocation();
    }

    private void updateLocation() {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Const.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                // parsing json response data
                try {
                    JSONObject jObj = new JSONObject(response);
                    int error = jObj.getInt("error");
                    String message = jObj.getString("message");

                   /* Toast.makeText(getApplicationContext(),
                            message, Toast.LENGTH_SHORT).show();*/
                    System.out.println(message);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(),
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "gps");
                params.put("phone", phone);
                params.put("lat", String.valueOf(lat));
                params.put("lng", String.valueOf(lng));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq);
    }

    private void startLocationUpdates() {
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                .requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(Const.INTERVAL);
        locationRequest.setFastestInterval(Const.FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    void updateList(String place, final String loc) {
        String input = "";
        try {
            input = "input=" + URLEncoder.encode(place, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        final String output = "json";
        String parameter = input + "&types=geocode&sensor=true&key=" + Const.browser_key;
        url = "https://maps.googleapis.com/maps/api/place/autocomplete/"
                + output + "?" + parameter;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                (String) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray array = response.getJSONArray(Const.TAG_RESULT);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        String description = object.getString("description");
                        places.add(description);
                    }

                    adapter = new ArrayAdapter<String>(getApplicationContext(),
                            android.R.layout.simple_list_item_1, places) {
                        @Override
                        public View getView(int position,
                                            View convertView, ViewGroup parent) {
                            View view = super.getView(position,
                                    convertView, parent);
                            TextView text = (TextView) view
                                    .findViewById(android.R.id.text1);
                            text.setTextColor(Color.BLACK);
                            return view;
                        }
                    };
                    if (loc.equals("source")) {
                        autoSource.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    } else if (loc.equals("destination")) {
                        autoDestination.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjectRequest, "req");
    }
}