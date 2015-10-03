package com.gimbal.hello_gimbal_android;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;


import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.gimbal.android.BeaconSighting;
import java.util.*;
import android.widget.TextView;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.modifiers.ScaleModifier;

enum GameState {
    READYTOHIT, BALLINFLIGHT, RETURNBALL
}

public class MainActivity extends ActionBarActivity {

    private PlaceManager placeManager;
    private PlaceEventListener placeEventListener;
    private TextView textView;
    private ArrayList<Integer> rssi_log = new ArrayList<Integer>();
    private TextView resultTextView;
    private long last_timeread = 0;
    private long previous_mbr = 0;
    private long hit_start_time = 0;
    private GameState state;

    public void selfDestruct(View view) {
        ParticleSystem ps = new ParticleSystem(this, 100, R.drawable.star_pink, 800);
        ps.setScaleRange(0.7f, 1.3f);
        ps.setSpeedRange(0.1f, 0.25f);
        ps.setRotationSpeedRange(90, 180);
        ps.setFadeOut(200, new AccelerateInterpolator());
        ps.oneShot(view, 70);


        ParticleSystem ps2 = new ParticleSystem(this, 100, R.drawable.star_white, 800);
        ps2.setScaleRange(0.7f, 1.3f);
        ps2.setSpeedRange(0.1f, 0.25f);
        ps.setRotationSpeedRange(90, 180);
        ps2.setFadeOut(200, new AccelerateInterpolator());
        ps2.oneShot(view, 70);
    }

    public void dust() {
        new ParticleSystem(this, 4, R.drawable.dust, 3000)
        .setSpeedByComponentsRange(-0.025f, 0.025f, -0.06f, -0.08f)
                .setAcceleration(0.00001f, 30)
                .setInitialRotationRange(0, 360)
                .addModifier(new AlphaModifier(255, 0, 1000, 3000))
                .addModifier(new ScaleModifier(0.5f, 2f, 0, 1000))
                .oneShot(findViewById(R.id.emiter_bottom), 4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1);
        //listView = (ListView) findViewById(R.id.list);
        //listView.setAdapter(listAdapter);
        textView = (TextView) findViewById(R.id.RSSI);
        textView.setText("text you want to display");

        resultTextView = (TextView) findViewById(R.id.result);
        resultTextView.setText("Waiting for Homerun");
        //listAdapter.add("Setting Gimbal API Key");
        //listAdapter.notifyDataSetChanged();
        Gimbal.setApiKey(this.getApplication(), "c2e44ce5-3f4b-4d3f-8655-3615974371c6");

        state = GameState.READYTOHIT;

        placeEventListener = new PlaceEventListener() {
            @Override
            public void onVisitStart(Visit visit) {
                textView.setText(String.format("Welcome to Homerun Derby! (%s)", visit.getPlace().getName()));
            }

            @Override
            public void onVisitEnd(Visit visit) {
                textView.setText(String.format("End Visit for %s", visit.getPlace().getName()));
            }

            @Override
            public void onBeaconSighting(BeaconSighting sighting, List<Visit> visits) {
                //double dist = 60.978*(Math.log(35.0)-Math.log((sighting.getRSSI()*-1.0)));
                Integer rssi = sighting.getRSSI();
                Integer total = 0;
                long start = sighting.getTimeInMillis();
                long milliseconds_between_readings = start - last_timeread;

                rssi_log.add(rssi);
                if(rssi_log.size() > 3){
                    rssi_log.remove(0);
                }
                Iterator<Integer> log_iter = rssi_log.iterator();
                while(log_iter.hasNext())
                {
                    Integer val = log_iter.next();
                    total += val;
                }
                Integer avg_rssi = total/rssi_log.size();
                textView.setText(String.format("RSSI:%d seconds:%d", avg_rssi, milliseconds_between_readings));

                switch(state) {
                    case READYTOHIT:
                        resultTextView.setText("Ready to Hit");
                        if(avg_rssi < -65) {
                            hit_start_time = start;
                            state = GameState.BALLINFLIGHT;
                        }
                        break;
                    case BALLINFLIGHT:
                        long time_since_hit =  start - hit_start_time;
                        resultTextView.setText(String.format("Ball in Flight (seconds elapsed: %d)", time_since_hit));
                        if(time_since_hit > 5000) {
                            long avg_mbr = (milliseconds_between_readings + previous_mbr)/2;
                            if (avg_rssi <= -97) {
                                //(avg_mbr > 1000){
                                resultTextView.setText(String.format("HomeRun!!!!"));
                            } else {
                                resultTextView.setText(String.format("Out!!!!"));
                                dust();
                            }
                            state = GameState.RETURNBALL;
                        }
                        break;
                    case RETURNBALL:
                        if(avg_rssi > -55){
                            state = GameState.READYTOHIT;
                        }
                        break;
                }

                previous_mbr = milliseconds_between_readings;
                last_timeread = start;
            }
        };

        placeManager = PlaceManager.getInstance();
        placeManager.addListener(placeEventListener);
        placeManager.startMonitoring();

        CommunicationManager.getInstance().startReceivingCommunications();
    }



}
