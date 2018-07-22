package com.jazibkhan.equalizer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.Virtualizer;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.marcinmoskala.arcseekbar.ArcSeekBar;
import com.marcinmoskala.arcseekbar.ProgressListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    Switch enabled = null;
    Switch enableBass, enableVirtual;
    Spinner spinner;

    Equalizer eq = null;
    BassBoost bb = null;
    Virtualizer virtualizer = null;

    int min_level = 0;
    int max_level = 100;

    static final int MAX_SLIDERS = 5; // Must match the XML layout
    SeekBar sliders[] = new SeekBar[MAX_SLIDERS];
    ArcSeekBar bassSlider, virtualSlider;
    TextView slider_labels[] = new TextView[MAX_SLIDERS];
    int num_sliders = 0;
    boolean canEnable;
    private AdView mAdView;
    ArrayList<String> eqPreset;
    int spinnerPos = 0;
    boolean dontcall=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileAds.initialize(this, "ca-app-pub-3247504109469111~8021644228");
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("9CAE76FEB9BFA8EA6723EEED1660711A").build();
        mAdView.loadAd(adRequest);


        enabled = (Switch) findViewById(R.id.switchEnable);
        enabled.setChecked(true);
        spinner = findViewById(R.id.spinner);
        sliders[0] = (SeekBar) findViewById(R.id.mySeekBar0);
        slider_labels[0] = (TextView) findViewById(R.id.centerFreq0);
        sliders[1] = (SeekBar) findViewById(R.id.mySeekBar1);
        slider_labels[1] = (TextView) findViewById(R.id.centerFreq1);
        sliders[2] = (SeekBar) findViewById(R.id.mySeekBar2);
        slider_labels[2] = (TextView) findViewById(R.id.centerFreq2);
        sliders[3] = (SeekBar) findViewById(R.id.mySeekBar3);
        slider_labels[3] = (TextView) findViewById(R.id.centerFreq3);
        sliders[4] = (SeekBar) findViewById(R.id.mySeekBar4);
        slider_labels[4] = (TextView) findViewById(R.id.centerFreq4);
        bassSlider = (ArcSeekBar) findViewById(R.id.bassSeekBar);
        virtualSlider = (ArcSeekBar) findViewById(R.id.virtualSeekBar);
        enableBass = (Switch) findViewById(R.id.bassSwitch);
        enableVirtual = (Switch) findViewById(R.id.virtualSwitch);
        bassSlider.setMaxProgress(1000);
        virtualSlider.setMaxProgress(1000);
        enableBass.setChecked(true);
        enableVirtual.setChecked(true);

        eqPreset = new ArrayList<String>();
        final ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eqPreset);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        eq = new Equalizer(100, 0);
        bb = new BassBoost(100, 0);
        virtualizer = new Virtualizer(100, 0);


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


        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor myEditor = myPreferences.edit();
        if (!myPreferences.contains("initial")) {
            myEditor.putBoolean("initial", true);
            myEditor.putBoolean("eqswitch", true);
            myEditor.putBoolean("bbswitch", true);
            myEditor.putBoolean("virswitch", true);
            myEditor.putInt("bbslider", (int) bb.getRoundedStrength());
            myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());
            myEditor.putInt("slider0", 100 * eq.getBandLevel((short) 0) / (max_level - min_level) + 50);
            myEditor.putInt("slider1", 100 * eq.getBandLevel((short) 1) / (max_level - min_level) + 50);
            myEditor.putInt("slider2", 100 * eq.getBandLevel((short) 2) / (max_level - min_level) + 50);
            myEditor.putInt("slider3", 100 * eq.getBandLevel((short) 3) / (max_level - min_level) + 50);
            myEditor.putInt("slider4", 100 * eq.getBandLevel((short) 4) / (max_level - min_level) + 50);
            myEditor.putInt("spinnerpos", 0);
            myEditor.apply();
        }

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
                    eq.usePreset((short) i);
                    spinnerPos = i;
                    updateSliders();
                    saveChanges();
                }
                else {
                    dontcall=true;
                    spinnerPos = i;
                    saveChanges();
                    applyChanges();
                    updateSliders();
                    dontcall=false;
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
                        if (enabled.isChecked() || enableBass.isChecked() || enableVirtual.isChecked()) {
                            Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                            startService(startIntent);
                        } else {
                            Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                            startService(stopIntent);
                        }
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
                        if (enabled.isChecked() || enableBass.isChecked() || enableVirtual.isChecked()) {
                            Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                            startService(startIntent);
                        } else {
                            Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                            startService(stopIntent);
                        }
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
                        if (enabled.isChecked() || enableBass.isChecked() || enableVirtual.isChecked()) {
                            Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
                            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
                            startService(startIntent);
                        } else {
                            Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
                            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
                            startService(stopIntent);
                        }

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
        spinnerPos=eqPreset.size()-1;
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
        if (enabled.isChecked() || enableBass.isChecked() || enableVirtual.isChecked()) {
            Intent startIntent = new Intent(MainActivity.this, ForegroundService.class);
            startIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
            startService(startIntent);
        } else {
            Intent stopIntent = new Intent(MainActivity.this, ForegroundService.class);
            stopIntent.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            startService(stopIntent);
        }

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

    public void saveChanges() {
        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor myEditor = myPreferences.edit();
        myEditor.putBoolean("initial", true);
        myEditor.putBoolean("eqswitch", enabled.isChecked());
        myEditor.putBoolean("bbswitch", enableBass.isChecked());
        myEditor.putBoolean("virswitch", enableVirtual.isChecked());
 //       Log.d("WOW", "actual bass level *************************** " + bb.getRoundedStrength());
 //       Log.d("WOW", "actual vir level *************************** " + virtualizer.getRoundedStrength());
        myEditor.putInt("spinnerpos", spinnerPos);
        myEditor.putInt("bbslider", (int) bb.getRoundedStrength());
        myEditor.putInt("virslider", (int) virtualizer.getRoundedStrength());

        if((spinnerPos == eqPreset.size() - 1)&&!dontcall) {
            myEditor.putInt("slider0", 100 * eq.getBandLevel((short) 0) / (max_level - min_level) + 50);
            myEditor.putInt("slider1", 100 * eq.getBandLevel((short) 1) / (max_level - min_level) + 50);
            myEditor.putInt("slider2", 100 * eq.getBandLevel((short) 2) / (max_level - min_level) + 50);
            myEditor.putInt("slider3", 100 * eq.getBandLevel((short) 3) / (max_level - min_level) + 50);
            myEditor.putInt("slider4", 100 * eq.getBandLevel((short) 4) / (max_level - min_level) + 50);
        }

        Log.d("WOW", "spinnerPos *************************** "+spinnerPos);
        Log.d("WOW", "eqPreset *************************** "+(eqPreset.size()-1));
        Log.d("WOW", "BAND LEVEL *************************** "+myPreferences.getInt("slider0",0));
        myEditor.apply();
        Log.d("WOW", "BAND LEVEL *************************** "+myPreferences.getInt("slider0",0));
    }








    public void applyChanges() {
        SharedPreferences myPreferences
                = PreferenceManager.getDefaultSharedPreferences(this);
        spinnerPos = myPreferences.getInt("spinnerpos", 0);
        enabled.setChecked(myPreferences.getBoolean("eqswitch", true));
        enableBass.setChecked(myPreferences.getBoolean("bbswitch", true));
        enableVirtual.setChecked(myPreferences.getBoolean("virswitch", true));
        bb.setStrength((short) myPreferences.getInt("bbslider", 0));
        virtualizer.setStrength((short) myPreferences.getInt("virslider", 0));
        if (spinnerPos != eqPreset.size() - 1){
            eq.usePreset((short) spinnerPos);
        }
        else{
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
        Toast.makeText(this, "Disable other Equalizers before enabling this",
                Toast.LENGTH_LONG).show();
        spinner.setEnabled(false);
        enabled.setChecked(false);
        enableVirtual.setChecked(false);
        enableBass.setChecked(false);
        canEnable = false;
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


}
