package in.codehex.traffic;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.traffic.model.TrafficItem;
import in.codehex.traffic.util.AppController;
import in.codehex.traffic.util.Const;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    Toolbar toolbar;
    ProgressDialog progressDialog;
    GoogleMap googleMap;
    String source, destination, phone, distance;
    double sourceLat, sourceLng, destLat, destLng, lat, lng, userLat, userLng;
    LatLng sourcePos, destinationPos;
    SharedPreferences sharedPreferences;
    String[] points = new String[100];
    int[] weight = new int[100];
    int traffic;
    LinearLayoutManager linearLayoutManager;
    List<TrafficItem> trafficItemList;
    MapRecyclerViewAdapter adapter;
    RecyclerView recyclerView;
    JSONObject jsonObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = (RecyclerView) findViewById(R.id.traffic_list);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        trafficItemList = new ArrayList<>();
        linearLayoutManager = new LinearLayoutManager(getApplicationContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MapRecyclerViewAdapter(getApplicationContext(), trafficItemList);
        recyclerView.setAdapter(adapter);

        jsonObject = new JSONObject();

        sharedPreferences = getSharedPreferences(Const.pref, MODE_PRIVATE);
        phone = sharedPreferences.getString("phone", null);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading..");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        source = getIntent().getStringExtra("source");
        destination = getIntent().getStringExtra("destination");
        traffic = Integer.parseInt(getIntent().getStringExtra("traffic"));
        distance = getIntent().getStringExtra("distance");
        userLat = getIntent().getDoubleExtra("lat", 0);
        userLng = getIntent().getDoubleExtra("lng", 0);
        getPolyline();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.setMyLocationEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        Geocoder geocoder = new Geocoder(getApplicationContext());
        List<Address> sourceAddress = null;
        List<Address> destinationAddress = null;
        try {
            sourceAddress = geocoder.getFromLocationName(source, 1);
            destinationAddress = geocoder.getFromLocationName(destination, 1);
            Address sAddress = sourceAddress.get(0);
            sourceLat = sAddress.getLatitude();
            sourceLng = sAddress.getLongitude();
            Address dAddress = destinationAddress.get(0);
            destLat = dAddress.getLatitude();
            destLng = dAddress.getLongitude();
        } catch (IOException e) {
            e.printStackTrace();
        }
        sourcePos = new LatLng(sourceLat, sourceLng);
        destinationPos = new LatLng(destLat, destLng);

        googleMap.addMarker(new MarkerOptions().position(sourcePos).title(source));
        googleMap.addMarker(new MarkerOptions().position(destinationPos).title(destination));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sourcePos, 11));
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
                try {
                    JSONArray array = response.getJSONArray("routes");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject routes = array.getJSONObject(i);
                        JSONObject polyline = routes.getJSONObject("overview_polyline");
                        points[i] = polyline.getString("points");
                        List<LatLng> decodedPath = PolyUtil.decode(points[i]);
                        googleMap.addPolyline(new PolylineOptions().color(ContextCompat
                                .getColor(getApplicationContext(), R.color.blue)).addAll(decodedPath));
                    }
                    processRoute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
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
                        List<LatLng> decodedPath = PolyUtil.decode(points[0]);
                        googleMap.addPolyline(new PolylineOptions().color(ContextCompat
                                .getColor(getApplicationContext(), R.color.accent)).addAll(decodedPath));
                        processList(response, 0);
                    } else {
                        int[] temp = new int[100];
                        for (int w = 0; w < weight.length; w++)
                            temp[w] = weight[w];
                        Arrays.sort(temp);
                        for (int t = 0; t < weight.length; t++) {
                            if (weight[t] == temp[0]) {
                                List<LatLng> decodedPath = PolyUtil.decode(points[t]);
                                googleMap.addPolyline(new PolylineOptions().color(ContextCompat
                                        .getColor(getApplicationContext(), R.color.accent)).addAll(decodedPath));
                                processList(response, t);
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

    private void processList(String data, int pos) {
        try {
            JSONArray array = jsonObject.getJSONArray("routes");
            JSONObject routes = array.getJSONObject(pos);
            JSONArray legs = routes.getJSONArray("legs");
            JSONObject object = legs.getJSONObject(0);
            JSONArray steps = object.getJSONArray("steps");
            trafficItemList.add(new TrafficItem("Segment", "Bike", "Car", "Truck", "Speed"));
            adapter.notifyDataSetChanged();
            int segment = 0, dist = 0;

            for (int i = 0; i < steps.length(); i++) {
                JSONObject polyObject = steps.getJSONObject(i);
                JSONObject jDistance = polyObject.getJSONObject("distance");
                dist += jDistance.getInt("value");
                if (segment == 0 || dist <= Float.parseFloat(distance)) {
                    JSONObject polyline = polyObject.getJSONObject("polyline");
                    String point = polyline.getString("points");
                    List<LatLng> decodedPath = PolyUtil.decode(point);
                    int bikeCount = 0, carCount = 0, truckCount = 0;
                    List<Double> speedList = new ArrayList<>();

                    JSONArray jsonArray = new JSONArray(data);
                    for (int j = 0; j < jsonArray.length(); j++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(j);
                        LatLng latLng = new LatLng(jsonObject.getDouble("lat"),
                                jsonObject.getDouble("lng"));

                        if (PolyUtil.isLocationOnPath(latLng, decodedPath, true, 10)) {
                            speedList.add(jsonObject.getDouble("speed"));
                            switch (jsonObject.getString("vehicle")) {
                                case "Bike":
                                    bikeCount++;
                                    break;
                                case "Car":
                                    carCount++;
                                    break;
                                case "Truck":
                                    truckCount++;
                                    break;
                            }
                        }
                    }

                    Double speed = 0.00, avgSpeed = 0.00;
                    String mSpeed;
                    for (Double d : speedList)
                        speed += d;
                    if (!speedList.isEmpty())
                        avgSpeed = speed / speedList.size();

                    if (avgSpeed == 0.0)
                        mSpeed = "-";
                    else mSpeed = String.valueOf(avgSpeed);

                    trafficItemList.add(new TrafficItem(String.valueOf(++segment),
                            String.valueOf(bikeCount), String.valueOf(carCount),
                            String.valueOf(truckCount), mSpeed));
                    adapter.notifyDataSetChanged();
                }
            }
            if (trafficItemList.size() < 2)
                Toast.makeText(getApplicationContext(),
                        "No traffic data is available for the given distance",
                        Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private class MapRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView textSegment, textBike, textCar, textTruck, textSpeed;

        public MapRecyclerViewHolder(View view) {
            super(view);
            textSegment = (TextView) view.findViewById(R.id.segment);
            textBike = (TextView) view.findViewById(R.id.bike);
            textCar = (TextView) view.findViewById(R.id.car);
            textTruck = (TextView) view.findViewById(R.id.truck);
            textSpeed = (TextView) view.findViewById(R.id.speed);
        }
    }

    public class MapRecyclerViewAdapter extends RecyclerView.Adapter<MapRecyclerViewHolder> {
        private List<TrafficItem> trafficItemList;
        private Context context;

        public MapRecyclerViewAdapter(Context context, List<TrafficItem> trafficItemList) {
            this.trafficItemList = trafficItemList;
            this.context = context;
        }

        @Override
        public MapRecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_traffic, parent, false);
            return new MapRecyclerViewHolder(layoutView);
        }

        @Override
        public void onBindViewHolder(MapRecyclerViewHolder holder, int position) {
            TrafficItem trafficItem = trafficItemList.get(position);
            holder.textSegment.setText(trafficItem.getSegment());
            holder.textBike.setText(trafficItem.getBike());
            holder.textCar.setText(trafficItem.getCar());
            holder.textTruck.setText(trafficItem.getTruck());
            holder.textSpeed.setText(trafficItem.getSpeed());
        }

        @Override
        public int getItemCount() {
            return this.trafficItemList.size();
        }
    }
}