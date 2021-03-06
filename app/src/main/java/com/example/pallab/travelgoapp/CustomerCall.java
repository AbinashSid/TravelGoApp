package com.example.pallab.travelgoapp;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pallab.travelgoapp.Common.Common;
import com.example.pallab.travelgoapp.Model.FCMResponse;
import com.example.pallab.travelgoapp.Model.Notification;
import com.example.pallab.travelgoapp.Model.Sender;
import com.example.pallab.travelgoapp.Model.Token;
import com.example.pallab.travelgoapp.Remote.IFCMService;
import com.example.pallab.travelgoapp.Remote.IGoogleAPI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CustomerCall extends AppCompatActivity {

    TextView txtTime,txtAddress,txtDistance;
    Button btnCancel,btnAccept;

    //MediaPlayer  mediaPlayer;

    IGoogleAPI mService;
    double lat,lng;

    String customerId;
    IFCMService mFCMService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_call);

        mService = Common.getGoogleAPI();
        mFCMService = Common.getFCMService();

        //Init view
        txtAddress  = (TextView) findViewById(R.id.txtAddress);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtTime     = (TextView) findViewById(R.id.txtTime);

        btnAccept = (Button)findViewById(R.id.btnAccept);
        btnCancel = (Button)findViewById(R.id.btnDecline);

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(customerId)){
                    cancelBooking(customerId);
                }

            }
        });
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CustomerCall.this,DriverTracking.class);
                //sending customer location to new Activity
                intent.putExtra("lat",lat);
                intent.putExtra("lng",lng);
                intent.putExtra("customerId",customerId);

                startActivity(intent);
                finish();
            }
        });

       /* mediaPlayer = MediaPlayer.create(this, R.raw.uber_beep);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();*/

        if (getIntent() != null) {
             lat = getIntent().getDoubleExtra("lat", -1.0);
             lng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customer");
            //just copy direction from welcome activity
            getDirection(lat,lng);
        }
    }


    private void cancelBooking(String customerId) {

        Token token = new Token(customerId);

        Notification notification = new Notification("Cancel","Driver has cancelled your request ");

        Sender sender = new Sender(token.getToken(),notification);

        mFCMService.sendMessage(sender)
                .enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                        if (response.body().success == 1){
                            Toast.makeText(CustomerCall.this, "Cancelled", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {

                    }
                });

    }

    private void getDirection(double lat,double lng) {

        String requestApi = null;
        try{
            requestApi = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+Common.mLastLocation.getLatitude()+","+Common.mLastLocation.getLongitude()+"&"+
                    "destination="+lat+","+lng+"&"+
                    "key="+getResources().getString(R.string.google_direction_api);

            Log.d("Knowsnoalgo",requestApi);//print url for debug

            mService.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {
                                
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                JSONArray routes =jsonObject.getJSONArray("routes");

                                //after get routes ,just get first element of routes
                                JSONObject object = routes.getJSONObject(0);

                                //after get first element , we need get array with name "legs"
                                JSONArray legs = object.getJSONArray("legs");

                                //and get first element of legs array
                                JSONObject legsObject = legs.getJSONObject(0);

                                //now ,get Distance
                                JSONObject distance  = legsObject.getJSONObject("distance");
                                txtDistance.setText(distance.getString("text"));

                                //get Time
                                JSONObject time  = legsObject.getJSONObject("duration");
                                txtTime.setText(time.getString("text"));

                                //Get address
                                String address = legsObject.getString("end_address");
                                txtAddress.setText(address);

                                } catch (JSONException e) {

                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(CustomerCall.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


        }catch (Exception e){
            e.printStackTrace();
        }
    }
/*

   @Override
    protected void onStop() {
        mediaPlayer.release();
        super.onStop();
    }

    @Override
    protected void onStart() {
        mediaPlayer.release();
        super.onStart();
    }

    @Override
    protected void onPause() {
        mediaPlayer.release();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();

    }*/
}
