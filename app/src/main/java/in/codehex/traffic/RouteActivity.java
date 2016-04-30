package in.codehex.traffic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.util.AppController;
import in.codehex.traffic.util.Const;

public class RouteActivity extends AppCompatActivity {

    Toolbar toolbar;
    Spinner spinRoute;
    FloatingActionButton fab;
    SharedPreferences sharedPreferences;
    String[] points = new String[100];
    int[] weight = new int[100];
    int traffic;
    double lat, lng, userLat, userLng;
    Intent intent;
    int mPosition;
    List<String> routeList;
    String source, destination, phone, distance;
    ArrayAdapter<String> spinnerAdapter;
    ProgressDialog progressDialog;
    JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        spinRoute = (Spinner) findViewById(R.id.route);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        sharedPreferences = getSharedPreferences(Const.pref, MODE_PRIVATE);
        phone = sharedPreferences.getString("phone", null);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        routeList = new ArrayList<>();

        jsonObject = new JSONObject();

        spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, routeList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinRoute.setAdapter(spinnerAdapter);

        source = getIntent().getStringExtra("source");
        destination = getIntent().getStringExtra("destination");
        traffic = Integer.parseInt(getIntent().getStringExtra("traffic"));
        distance = getIntent().getStringExtra("distance");
        userLat = getIntent().getDoubleExtra("lat", 0);
        userLng = getIntent().getDoubleExtra("lng", 0);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(getApplicationContext(), MapActivity.class);
                intent.putExtra("position", mPosition);
                intent.putExtra("route", String.valueOf(jsonObject));
                startActivity(intent);
            }
        });
        getPolyline();
    }

    void getPolyline() {
        progressDialog.setMessage("retrieving path..");
        showProgressDialog();
        String url = null;
        try {
            url = Const.poly_url + "origin=" + URLEncoder.encode(source, "utf-8") + "&destination="
                    + URLEncoder.encode(destination, "utf-8") + "&alternatives=true&key=" + Const.browser_key;
            System.out.println(url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        final JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                (String) null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                hideProgressDialog();
                jsonObject = response;
                processRoute();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                error.printStackTrace();
            }
        });
        AppController.getInstance().addToRequestQueue(jsonObjectRequest, "req");
    }

    void processRoute() {
        progressDialog.setMessage("processing route");
        showProgressDialog();
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Const.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                try {
                    JSONArray array = new JSONArray(response);
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        lat = object.getDouble("lat");
                        lng = object.getDouble("lng");
                        LatLng latLng = new LatLng(lat, lng);
                        for (int j = 0; j < 100; j++) {
                            if (points[j] != null) {
                                List<LatLng> decodedPath = PolyUtil.decode(points[j]);
                                if (PolyUtil.isLocationOnPath(latLng, decodedPath, true, 10))
                                    weight[j] += object.getInt("weight");
                            }
                        }
                    }

                    if (weight[0] <= traffic) {
                        mPosition = 0;
                    } else {
                        int[] temp = new int[100];
                        for (int w = 0; w < weight.length; w++)
                            temp[w] = weight[w];
                        Arrays.sort(temp);
                        for (int t = 0; t < weight.length; t++) {
                            if (weight[t] == temp[0]) {
                                mPosition = t;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(),
                        "Network error", Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "traffic");
                params.put("phone", phone);

                return params;
            }

        };

        AppController.getInstance().addToRequestQueue(strReq);
    }

    private void showProgressDialog() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
