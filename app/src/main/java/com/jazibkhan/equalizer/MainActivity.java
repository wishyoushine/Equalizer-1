package com.jazibkhan.equalizer;

import android.media.audiofx.LoudnessEnhancer;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
//import com.google.android.gms.ads.AdRequest;
//import com.google.android.gms.ads.AdView;
//import com.google.android.gms.ads.MobileAds;
import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    Switch enabled = null;
    Switch enableBass, enableVirtual,enableLoud;
    Spinner spinner;

    Equalizer eq = null;
    BassBoost bb = null;
    Virtualizer virtualizer = null;
    LoudnessEnhancer loudnessEnhancer=null;

    int min_level = 0;
    int max_level = 100;

    static final int MAX_SLIDERS = 5; // Must match the XML layout
    SeekBar sliders[] = new SeekBar[MAX_SLIDERS];
    ArcSeekBar bassSlider, virtualSlider,loudSlider;
    TextView slider_labels[] = new TextView[MAX_SLIDERS];
    int num_sliders = 0;
    boolean canEnable;
//    private AdView mAdView;
    ArrayList<String> eqPreset;
    int spinnerPos = 0;
    boolean dontcall = false;
    boolean canPreset;
    LinearLayout presetView,loudnessView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        MobileAds.initialize(this, "ca-app-pub-3247504109469111~8021644228");
//        mAdView = (AdView) findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().addTestDevice("9CAE76FEB9BFA8EA6723EEED1660711A").build();
//        mAdView.loadAd(adRequest);


        enabled = findViewById(R.id.switchEnable);
        enabled.setChecked(true);
        spinner = findViewById(R.id.spinner);
        sliders[0] = findViewById(R.id.mySeekBar0);
        slider_labels[0] = findViewById(R.id.centerFreq0);
        sliders[1] = findViewById(R.id.mySeekBar1);
        slider_labels[1] = findViewById(R.id.centerFreq1);
        sliders[2] = findViewById(R.id.mySeekBar2);
        slider_labels[2] = findViewById(R.id.centerFreq2);
        sliders[3] = findViewById(R.id.mySeekBar3);
        slider_labels[3] = findViewById(R.id.centerFreq3);
        sliders[4] = findViewById(R.id.mySeekBar4);
        slider_labels[4] = findViewById(R.id.centerFreq4);
        bassSlider = findViewById(R.id.bassSeekBar);
        virtualSlider = findViewById(R.id.virtualSeekBar);
        enableBass = findViewById(R.id.bassSwitch);
        enableVirtual = findViewById(R.id.virtualSwitch);
        enableLoud=findViewById(R.id.volSwitch);
        loudSlider=findViewById(R.id.volSeekBar);
        presetView = findViewById(R.id.presetView);
        loudnessView=findViewById(R.id.loudnessView);
        bassSlider.setMaxProgress(1000);
        virtualSlider.setMaxProgress(1000);
        loudSlider.setMaxProgress(10000);
        enableLoud.setChecked(true);
        enableBass.setChecked(true);
        enableVirtual.setChecked(true);
        eqPreset = new ArrayList<>();
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eqPreset);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 以下代码在pixel3上能正常运行（测试： qreq bd sesstionId），下面的 sesstionId 是根据正在播放的音乐session id写死的，其实可以通过注册一个静态广播接收器来接收系统发出的：AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION 和 AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION 二个广播来获取当前track的session id（具体见：https://stackoverflow.com/questions/9404776/preferred-way-to-attach-audioeffect-to-global-mix）
        int sesstionId = 33;
        eq = new Equalizer(0, sesstionId);
        bb = new BassBoost(0, sesstionId);
        virtualizer = new Virtualizer(0, sesstionId);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
             loudnessEnhancer=new LoudnessEnhancer(0);
        else {
            enableLoud.setChecked(false);
            loudnessView.setVisibility(View.GONE);

        }


        if (eq != null) {
            int num_bands = eq.getNumberOfBands();
            num_sliders = num_bands;
            short r[] = eq.getBandLevelRange();
            min_level = r[0];
            max_level = r[1];
            for (int i = 0; i < num_sliders && i < MAX_SLIDERS; i++) {
                int freq_range = eq.getCenterFreq((short) i);
                sliders[i].setOnSeekBarChangeListener(this);
                slider_labels[i].setText(milliHzToString(freq_range));
            }
            for (short i = 0; i < eq.getNumberOfPresets(); i++) {
                eqPreset.add(eq.getPresetName(i));
            }
            eqPreset.add("Custom");
            spinner.setAdapter(spinnerAdapter);
        }

        initialize();

        try {
            updateUI();
            canEnable = true;
        } catch (Throwable e) {
            disableEvery();
            e.printStackTrace();
        }


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < eqPreset.size() - 1) {
                    //      Crashlytics.log(1, "OnItemSelectedListener", ""+i);
                    try {
                        eq.usePreset((short) i);
                        canPreset = true;
                        spinnerPos = i;
                        updateSliders();
                        saveChanges();
                    } catch (Throwable e) {
                        disablePreset();
                        e.printStackTrace();
                    }
                } else {
                    //    Crashlytics.log(1, "OnItemSelectedListener", ""+i);
                    dontcall = true;
                    spinnerPos = i;
                    saveChanges();
                    applyChanges();
                    updateSliders();
                    dontcall = false;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        virtualSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int j) {
                if (canEnable) {
                    virtualizer.setStrength((short) j);
                    saveChanges();
                } else disableEvery();
            }
        });

        bassSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int i) {
                if (canEnable) {
                    //           Log.d("WOW", "level bass slider*************************** " + (short) i);
                    bb.setStrength((short) i);
                    //           Log.d("WOW", "set progress actual bass level *************************** " + bb.getRoundedStrength());
                    saveChanges();
                } else disableEvery();
            }
        });
        loudSlider.setOnProgressChangedListener(new ProgressListener() {
            @Override
            public void invoke(int j) {
                if (canEnable) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    loudnessEnhancer.setTargetGain(j);
               //     Log.d("WOW", "Loudness Target gain *************************** " + loudnessEnhancer.getTargetGain());
                    }
                    saveChanges();
                } else disableEvery();
            }
        });
        if (virtualizer != null) {
            enableVirtual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (canEnable) {
                        if (enableVirtual.isChecked()) {
                            virtualizer.setEnabled(true);
                            virtualSlider.setEnabled(true);
                            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
                            saveChanges();
                        } else {
                            virtualizer.setEnabled(false);
                            virtualSlider.setEnabled(false);
                            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
                            saveChanges();
                        }
                        serviceChecker();
                    } else disableEvery();
                }
            });
        }
        if (bb != null) {
            enableBass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (canEnable) {
                        if (enableBass.isChecked()) {
                            bb.setEnabled(true);
                            bassSlider.setEnabled(true);
                            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
                            saveChanges();
                        } else {
                            bb.setEnabled(false);
                            bassSlider.setEnabled(false);
                            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
                            saveChanges();
                        }
                        serviceChecker();
                    } else disableEvery();
                }
            });
        }
        if (loudnessEnhancer != null) {
            enableLoud.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (canEnable) {
                        if (enableLoud.isChecked()) {
                            Toast.makeText(getApplicationContext(), R.string.warning,
                                    Toast.LENGTH_SHORT).show();
                            loudnessEnhancer.setEnabled(true);
                            loudSlider.setEnabled(true);
                            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
                            saveChanges();
                        } else {
                            loudnessEnhancer.setEnabled(false);
                            loudSlider.setEnabled(false);
                            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
                            saveChanges();
                        }
                        serviceChecker();
                    } else disableEvery();
                }
            });
        }
        if (eq != null) {
            enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (canEnable) {
                        if (enabled.isChecked()) {
                            spinner.setEnabled(true);
                            eq.setEnabled(true);
                            saveChanges();
                            for (int i = 0; i < 5; i++) {
                                sliders[i].setEnabled(true);
                            }
                        } else {
                            spinner.setEnabled(false);
                            eq.setEnabled(false);
                            saveChanges();
                            for (int i = 0; i < 5; i++) {
                                sliders[i].setEnabled(false);
                            }
                        }
                        serviceChecker();

                    } else disableEvery();
                }
            });
        }
    }


    public String milliHzToString(int milliHz) {
        if (milliHz < 1000) return "";
        if (milliHz < 1000000)
            return "" + (milliHz / 1000) + "Hz";
        else
            return "" + (milliHz / 1000000) + "kHz";
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int level, boolean b) {

        if (eq != null && canEnable) {
            int new_level = min_level + (max_level - min_level) * level / 100;

            for (int i = 0; i < num_sliders; i++) {
                if (sliders[i] == seekBar) {
                    eq.setBandLevel((short) i, (short) new_level);
                    saveChanges();
                    break;
                }
            }
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        spinner.setSelection(eqPreset.size() - 1);
        spinnerPos = eqPreset.size() - 1;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(MainActivity.this, AboutActivity.class);
            MainActivity.this.startActivity(myIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void updateUI() {
        applyChanges();
        serviceChecker();

        if (enableBass.isChecked()) {
            bassSlider.setEnabled(true);
            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            bb.setEnabled(true);
        } else {
            bassSlider.setEnabled(false);
            bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
            bb.setEnabled(false);
        }
        if (enableVirtual.isChecked()) {
            virtualizer.setEnabled(true);
            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            virtualSlider.setEnabled(true);
        } else {
            virtualizer.setEnabled(false);
            virtualSlider.setEnabled(false);
            virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        }
        if (enableLoud.isChecked()) {
            loudnessEnhancer.setEnabled(true);
            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.colorAccent));
            loudSlider.setEnabled(true);
        } else {
            loudnessEnhancer.setEnabled(false);
            loudSlider.setEnabled(false);
            loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        }

        if (enabled.isChecked()) {
            spinner.setEnabled(true);
            for (int i = 0; i < 5; i++)
                sliders[i].setEnabled(true);
            eq.setEnabled(true);
        } else {
            spinner.setEnabled(false);
            for (int i = 0; i < 5; i++)
                sliders[i].setEnabled(false);
            eq.setEnabled(false);
        }
        spinner.setSelection(spinnerPos);
        updateSliders();
        updateBassBoost();
        updateVirtualizer();
        updateLoudness();

    }

    public void updateSliders() {
        for (int i = 0; i < num_sliders; i++) {
            int level;
            if (eq != null)
                level = eq.getBandLevel((short) i);
            else
                level = 0;
            int pos = 100 * level / (max_level - min_level) + 50;
            sliders[i].setProgress(pos);
        }
    }

    public void updateBassBoost() {
        if (bb != null)
            bassSlider.setProgress(bb.getRoundedStrength());
        else
            bassSlider.setProgress(0);
    }

    public void updateVirtualizer() {
        if (virtualizer != null)
            virtualSlider.setProgress(virtualizer.getRoundedStrength());
        else
            virtualSlider.setProgress(0);
    }
    public void updateLoudness() {
        if (loudnessEnhancer != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            loudSlider.setProgress((int)(loudnessEnhancer.getTargetGain()));
        }
        else
            loudSlider.setProgress(0);
    }

    public void saveChanges() {
        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor myEditor = myPreferences.edit();
        myEditor.putBoolean("initial", true);
        myEditor.putBoolean("eqswitch", enabled.isChecked());
        myEditor.putBoolean("bbswitch", enableBass.isChecked());
        myEditor.putBoolean("virswitch", enableVirtual.isChecked());
        myEditor.putBoolean("loudswitch", enableLoud.isChecked());
        //       Log.d("WOW", "actual bass level *************************** " + bb.getRoundedStrength());
        //       Log.d("WOW", "actual vir level *************************** " + virtualizer.getRoundedStrength());
        myEditor.putInt("spinnerpos", spinnerPos);
        try {
            if (bb != null)
                myEditor.putInt("bbslider", (int) bb.getRoundedStrength());
            else {
                bb = new BassBoost(100, 0);
                myEditor.putInt("bbslider", (int) bb.getRoundedStrength());

            }
            if (virtualizer != null)
                myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
            else {
                virtualizer = new Virtualizer(100, 0);
                myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
            }
            if (loudnessEnhancer != null){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                    myEditor.putFloat("loudslider",  loudnessEnhancer.getTargetGain());
            }
            else {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    loudnessEnhancer = new LoudnessEnhancer( 0);
                    myEditor.putFloat("loudslider",  loudnessEnhancer.getTargetGain());
                }
            }
        } catch (Throwable e) {
            myEditor.putInt("bbslider", (int) 0);
            myEditor.putInt("virslider", (int) 0);
            myEditor.putFloat("loudslider",0);
            e.printStackTrace();
        }


        if ((spinnerPos == eqPreset.size() - 1) && !dontcall) {
            myEditor.putInt("slider0", 100 * eq.getBandLevel((short) 0) / (max_level - min_level) + 50);
            myEditor.putInt("slider1", 100 * eq.getBandLevel((short) 1) / (max_level - min_level) + 50);
            myEditor.putInt("slider2", 100 * eq.getBandLevel((short) 2) / (max_level - min_level) + 50);
            myEditor.putInt("slider3", 100 * eq.getBandLevel((short) 3) / (max_level - min_level) + 50);
            myEditor.putInt("slider4", 100 * eq.getBandLevel((short) 4) / (max_level - min_level) + 50);
        }

     //   Log.d("WOW", "spinnerPos *************************** " + spinnerPos);
     //   Log.d("WOW", "eqPreset *************************** " + (eqPreset.size() - 1));
    //    Log.d("WOW", "BAND LEVEL *************************** " + myPreferences.getInt("slider0", 0));
        myEditor.apply();
     //   Log.d("WOW", "BAND LEVEL *************************** " + myPreferences.getInt("slider0", 0));
    }


    public void applyChanges() {
        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        spinnerPos = myPreferences.getInt("spinnerpos", 0);
        enabled.setChecked(myPreferences.getBoolean("eqswitch", true));
        enableBass.setChecked(myPreferences.getBoolean("bbswitch", true));
        enableVirtual.setChecked(myPreferences.getBoolean("virswitch", true));
        enableLoud.setChecked(myPreferences.getBoolean("loudswitch",false));

        try {
            if (bb != null)
                bb.setStrength((short) myPreferences.getInt("bbslider", 0));
            else {
                bb = new BassBoost(100, 0);
                bb.setStrength((short) myPreferences.getInt("bbslider", 0));
            }
            if (virtualizer != null)
                virtualizer.setStrength((short) myPreferences.getInt("virslider", 0));
            else {
                virtualizer = new Virtualizer(100, 0);
                virtualizer.setStrength((short) myPreferences.getInt("virslider", 0));
            }
            if (loudnessEnhancer != null){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                loudnessEnhancer.setTargetGain((int)myPreferences.getFloat("loudslider", 0));
            }
            else {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                loudnessEnhancer = new LoudnessEnhancer( 0);
                loudnessEnhancer.setTargetGain((int)myPreferences.getFloat("loudslider", 0));
                }
            }
        } catch (Throwable e) {

            e.printStackTrace();
        }
        if (spinnerPos != eqPreset.size() - 1) {

            try {
                eq.usePreset((short) spinnerPos);
            } catch (Throwable e) {
                disablePreset();
                e.printStackTrace();
            }
        } else {
            eq.setBandLevel((short) 0, (short) (min_level + (max_level - min_level) * myPreferences.getInt("slider0", 0) / 100));
            eq.setBandLevel((short) 1, (short) (min_level + (max_level - min_level) * myPreferences.getInt("slider1", 0) / 100));
            eq.setBandLevel((short) 2, (short) (min_level + (max_level - min_level) * myPreferences.getInt("slider2", 0) / 100));
            eq.setBandLevel((short) 3, (short) (min_level + (max_level - min_level) * myPreferences.getInt("slider3", 0) / 100));
            eq.setBandLevel((short) 4, (short) (min_level + (max_level - min_level) * myPreferences.getInt("slider4", 0) / 100));
        }

        //       Log.d("WOW", "bass level *************************** " + (short) myPreferences.getInt("bbslider", 0));
        //       Log.d("WOW", "virtualizer level *************************** " + (short) myPreferences.getInt("virslider", 0));
    }

    public void disableEvery() {
        Toast.makeText(this, R.string.disableOther,
                Toast.LENGTH_LONG).show();
        spinner.setEnabled(false);
        enabled.setChecked(false);
        enableVirtual.setChecked(false);
        enableBass.setChecked(false);
        enableLoud.setChecked(false);
        canEnable = false;
        loudnessEnhancer.setEnabled(false);
        loudSlider.setEnabled(false);
        loudSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        virtualizer.setEnabled(false);
        virtualSlider.setEnabled(false);
        virtualSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        bassSlider.setEnabled(false);
        bassSlider.setProgressColor(ContextCompat.getColor(getBaseContext(), R.color.progress_gray));
        bb.setEnabled(false);
        for (int i = 0; i < 5; i++)
            sliders[i].setEnabled(false);
        eq.setEnabled(false);
    }

    public void disablePreset() {
        presetView.setVisibility(View.GONE);
        canPreset = false;
    }

public void initialize(){
    SharedPreferences myPreferences
            = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor myEditor = myPreferences.edit();
    if (!myPreferences.contains("initial")) {
        myEditor.putBoolean("initial", true);
        myEditor.putBoolean("eqswitch", false);
        myEditor.putBoolean("bbswitch", false);
        myEditor.putBoolean("virswitch", false);
        myEditor.putInt("bbslider", (int) bb.getRoundedStrength());
        myEditor.putBoolean("loudswitch", false);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        myEditor.putFloat("loudslider",  loudnessEnhancer.getTargetGain());
        myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
        myEditor.putInt("slider0", 100 * eq.getBandLevel((short) 0) / (max_level - min_level) + 50);
        myEditor.putInt("slider1", 100 * eq.getBandLevel((short) 1) / (max_level - min_level) + 50);
        myEditor.putInt("slider2", 100 * eq.getBandLevel((short) 2) / (max_level - min_level) + 50);
        myEditor.putInt("slider3", 100 * eq.getBandLevel((short) 3) / (max_level - min_level) + 50);
        myEditor.putInt("slider4", 100 * eq.getBandLevel((short) 4) / (max_level - min_level) + 50);
        myEditor.putInt("spinnerpos", 0);
        myEditor.apply();
    }
}

public void serviceChecker(){
    if (enabled.isChecked() || enableBass.isChecked() || enableVirtual.isChecked()||enableLoud.isChecked()) {
        Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
        startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        startService(startIntent);
    } else {
        Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
        stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
        startService(stopIntent);
    }

}
}
