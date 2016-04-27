package in.codehex.traffic;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.codehex.traffic.util.AppController;
import in.codehex.traffic.util.Const;

public class LoginActivity extends AppCompatActivity {

    Toolbar toolbar;
    EditText editPhone;
    Spinner spinVehicle;
    FloatingActionButton fabLogin;
    SharedPreferences sharedPreferences;
    Intent intent;
    String phone;
    String vehicle;
    int weight;
    CoordinatorLayout coordinatorLayout;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator);
        editPhone = (EditText) findViewById(R.id.phone);
        spinVehicle = (Spinner) findViewById(R.id.vehicle);
        fabLogin = (FloatingActionButton) findViewById(R.id.login);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in..");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        sharedPreferences = getSharedPreferences(Const.pref, MODE_PRIVATE);

        fabLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phone = editPhone.getText().toString();
                vehicle = spinVehicle.getSelectedItem().toString();
                switch (vehicle) {
                    case "Bike":
                        weight = 1;
                        break;
                    case "Car":
                        weight = 2;
                        break;
                    case "Truck":
                        weight = 3;
                        break;
                    default:
                        weight = 0;
                        break;
                }
                if (!phone.equals("") && phone.length() >= 10) {
                    showProgressDialog();
                    processLogin();
                }
            }
        });

        if (!checkPermission())
            requestPermission();
    }

    boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    void requestPermission() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Const.PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Const.PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(coordinatorLayout, "Location access permission granted",
                            Snackbar.LENGTH_SHORT).show();
                }
        }
    }

    void processLogin() {
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Const.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                try {
                    JSONObject jObj = new JSONObject(response);
                    int error = jObj.getInt("error");
                    String message = jObj.getString("message");

                    Toast.makeText(getApplicationContext(),
                            message, Toast.LENGTH_SHORT).show();

                    if (error == 0) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("phone", phone);
                        editor.putString("vehicle", vehicle);
                        editor.putInt("weight", weight);
                        editor.apply();

                        intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                    }
                } catch (JSONException e) {
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
                params.put("tag", "login");
                params.put("phone", phone);
                params.put("vehicle", vehicle);
                params.put("weight", String.valueOf(weight));

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