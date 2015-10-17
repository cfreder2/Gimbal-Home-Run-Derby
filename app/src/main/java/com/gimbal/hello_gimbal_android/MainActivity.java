package com.gimbal.hello_gimbal_android;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;

import com.gimbal.android.CommunicationManager;
import com.gimbal.android.Gimbal;
import com.gimbal.android.PlaceEventListener;
import com.gimbal.android.PlaceManager;
import com.gimbal.android.Visit;
import com.gimbal.android.BeaconSighting;
import java.util.*;
import android.os.Handler;

import android.widget.ImageView;
import android.widget.TextView;
import android.view.*;
import android.view.animation.AccelerateInterpolator;
import com.plattysoft.leonids.modifiers.AlphaModifier;
import com.plattysoft.leonids.ParticleSystem;
import com.plattysoft.leonids.modifiers.ScaleModifier;

import android.media.MediaPlayer;

enum GameState {
    READYTOHIT, BALLINFLIGHT, RETURNBALL
}

public class MainActivity extends ActionBarActivity {

    private PlaceManager placeManager;
    private PlaceEventListener placeEventListener;
    private TextView textView;
    private long hit_start_time = 0;
    private GameState state;
    private GameState prev_state;
    private MySightingManager sightingManager = new MySightingManager(3);
    private ImageView imageView;

    public void selfDestruct(View view) {
        ParticleSystem ps = new ParticleSystem(this, 100, R.drawable.star_blue, 800);
        ps.setScaleRange(0.7f, 1.3f);
        ps.setSpeedRange(0.1f, 0.25f);
        ps.setRotationSpeedRange(90, 180);
        ps.setFadeOut(200, new AccelerateInterpolator());
        ps.oneShot(view, 70);

        ParticleSystem ps2 = new ParticleSystem(this, 100, R.drawable.star_white, 800);
        ps2.setScaleRange(0.7f, 1.3f);
        ps2.setSpeedRange(0.1f, 0.25f);
        ps2.setRotationSpeedRange(90, 180);
        ps2.setFadeOut(200, new AccelerateInterpolator());
        ps2.oneShot(view, 70);

        ParticleSystem ps3 = new ParticleSystem(this, 100, R.drawable.star_gold, 800);
        ps3.setScaleRange(0.7f, 1.3f);
        ps3.setSpeedRange(0.1f, 0.25f);
        ps3.setRotationSpeedRange(90, 180);
        ps3.setFadeOut(200, new AccelerateInterpolator());
        ps3.oneShot(view, 70);
    }

    public void dust(View view) {
        new ParticleSystem(this, 4, R.drawable.dust, 3000)
        .setSpeedByComponentsRange(-0.025f, 0.025f, -0.06f, -0.08f)
                .setAcceleration(0.00001f, 30)
                .setInitialRotationRange(0, 360)
                .addModifier(new AlphaModifier(255, 0, 1000, 3000))
                .addModifier(new ScaleModifier(0.5f, 2f, 0, 1000))
                .oneShot(view, 4);

        new ParticleSystem(this, 4, R.drawable.dust, 3000)
                .setSpeedByComponentsRange(0.034f, 0.020f, 0.06f, -0.08f)
                .setAcceleration(0.00001f, 30)
                .setInitialRotationRange(0, 270)
                .addModifier(new AlphaModifier(255, 0, 1000, 3000))
                .addModifier(new ScaleModifier(0.5f, 2f, 0, 1000))
                .oneShot(view, 4);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.RSSI);
        imageView = (ImageView) findViewById(R.id.imgStatus);

        Gimbal.setApiKey(this.getApplication(), "c2e44ce5-3f4b-4d3f-8655-3615974371c6");

        state = GameState.RETURNBALL; //probably needs to be in onVisitStart?

        placeEventListener = new PlaceEventListener() {
            @Override
            public void onVisitStart(Visit visit) {
                textView.setText(String.format("Welcome to Homerun Derby! (%s)", visit.getPlace().getName()));
            }

            @Override
            public void onVisitEnd(Visit visit) {
                MediaPlayer.create(MainActivity.this, R.raw.fireworks).start();
                textView.setText(String.format("End Visit for %s", visit.getPlace().getName()));
                imageView.setImageResource(R.drawable.homerun);
                selfDestruct(imageView); //particle effect
                state = GameState.RETURNBALL;
            }

            @Override
            public void onBeaconSighting(BeaconSighting sighting, List<Visit> visits) {
                sightingManager.addSighting(sighting.getRSSI(), sighting.getTimeInMillis());
            }
        };

        Timer timer = new Timer();
        timer.schedule( new TimerTask() {
            @Override
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Integer avgRssi = sightingManager.avgRssi();
                        long startTime = sightingManager.lastSightingTime();
                        float latency = (float)(System.currentTimeMillis() - startTime ) / 1000.0f;
                        textView.setText(String.format("Avg RSSI:%d, latency(sec):%.1f", avgRssi, latency));

                        Boolean playNewSound = false;
                        int audioID = -1;
                        switch (state) {
                            case READYTOHIT:
                                imageView.setImageResource(R.drawable.batterup);

                                if (avgRssi <= -60) {
                                    hit_start_time =  sightingManager.lastSightingTime();
                                    sightingManager.setHitStarted();
                                    state = GameState.BALLINFLIGHT;
                                    audioID = R.raw.homerun;
                                    playNewSound = true;
                                }
                                break;
                            case BALLINFLIGHT:
                                long time_since_hit = System.currentTimeMillis() - hit_start_time;
                                textView.setText(String.format("Avg RSSI:%d, latency(sec):%.1f, bif(sec): %.1f)", avgRssi, latency, (float) time_since_hit / 1000f));
                                imageView.setImageResource(R.drawable.baseball);

                                if(time_since_hit >=5000) {
                                    if (avgRssi <= -90) {
                                        audioID = R.raw.fireworks;
                                        imageView.setImageResource(R.drawable.homerun);
                                        selfDestruct(imageView); //particle effect
                                        state = GameState.RETURNBALL;
                                        playNewSound = true;
                                    } else if (sightingManager.isReadingCompleted()) {
                                        imageView.setImageResource(R.drawable.yourout);
                                        audioID = R.raw.yourout;
                                        dust(imageView); //particle effect
                                        state = GameState.RETURNBALL;
                                        playNewSound = true;
                                    }
                                }

                                break;
                            case RETURNBALL:
                                if (avgRssi > -55) {
                                    playNewSound = true;

                                    if((avgRssi*-1)%3 == 0) {
                                        audioID = R.raw.uptobat2;
                                    }
                                    else {
                                        audioID = R.raw.uptobat;
                                    }

                                    state = GameState.READYTOHIT;

                                }
                                sightingManager.clearHitStarted();
                                break;
                            default:
                                break;
                        }
                        if(playNewSound){
                            MediaPlayer.create(MainActivity.this, audioID).start();
                        }
                    }
                });
            }
        }, 0, 500); //delay 0, wait every 500 then call yourself again.

        placeManager = PlaceManager.getInstance();
        placeManager.addListener(placeEventListener);
        placeManager.startMonitoring();

        CommunicationManager.getInstance().startReceivingCommunications();
    }



}
