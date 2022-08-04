package ggonzalezm.aire;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String ADDRESS = "192.168.0.48";
    private static final String ADDRESS_GENERAL = "192.168.0.34";
    private static final String ADDRESS_VENT = "192.168.0.51";
    private static final int MIN_TEMP = 17;
    private static final int MAX_TEMP = 30;
    private static final int INIT_TEMP = 25;
    private static final int T_TEMP_UPDATE = 5000;
    private static final int T_TIME_UPDATE = 1000;
    private static final int T_GEN_UPDATE = 30000;
    private static final int[] TIMES = {0, 1, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 300};

    RelativeLayout lyo_fan, lyo_set_temp, lyo_set_time;
    Button btn_on, btn_off, btn_fan_auto, btn_fan1, btn_fan2, btn_fan3, btn_temp_minus, btn_temp_plus, btn_time_minus, btn_time_plus, btn_gen_on, btn_gen_off;
    TextView txv_color, txv_temp, txv_temp_set, txv_time_set, txv_vent, txv_gen_color, txv_log, txv_sent, txv_gen_sent,  txv_vent_sent, txv_received, txv_gen_received, txv_vent_received;
    SeekBar sb_vent;

    Handler handler_update = new Handler();
    Handler handler_gen_update = new Handler();

    public static String address = ADDRESS;
    public static String gen_address = ADDRESS_GENERAL;
    public static String vent_address = ADDRESS_VENT;
    boolean is_on = false;
    boolean is_gen_on = false;
    boolean is_timer = false;
    boolean statusRec = true;
    int temp_set = INIT_TEMP;
    int time_set = 0;
    int i_times = 0;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lyo_fan = findViewById(R.id.lyo_fan);
        lyo_set_temp = findViewById(R.id.lyo_set_temp);
        lyo_set_time = findViewById(R.id.lyo_set_time);

        btn_on = findViewById(R.id.btn_on);
        btn_off = findViewById(R.id.btn_off);
        txv_color = findViewById(R.id.txv_color);
        txv_temp = findViewById(R.id.txv_temp);

        btn_fan_auto = findViewById(R.id.btn_fan_auto);
        btn_fan1 = findViewById(R.id.btn_fan1);
        btn_fan2 = findViewById(R.id.btn_fan2);
        btn_fan3 = findViewById(R.id.btn_fan3);

        btn_temp_minus = findViewById(R.id.btn_temp_minus);
        txv_temp_set = findViewById(R.id.txv_temp_set);
        btn_temp_plus = findViewById(R.id.btn_temp_plus);

        btn_time_minus = findViewById(R.id.btn_time_minus);
        txv_time_set = findViewById(R.id.txv_time_set);
        btn_time_plus = findViewById(R.id.btn_time_plus);

        sb_vent = findViewById(R.id.sb_vent);
        txv_vent = findViewById(R.id.txv_vent);

        btn_gen_on = findViewById(R.id.btn_gen_on);
        btn_gen_off = findViewById(R.id.btn_gen_off);
        txv_gen_color = findViewById(R.id.txv_gen_color);

        txv_log = findViewById(R.id.txv_log);
        txv_sent = findViewById(R.id.txv_sent);
        txv_gen_sent = findViewById(R.id.txv_gen_sent);
        txv_vent_sent = findViewById(R.id.txv_vent_sent);
        txv_received = findViewById(R.id.txv_received);
        txv_gen_received = findViewById(R.id.txv_gen_received);
        txv_vent_received = findViewById(R.id.txv_vent_received);

        btn_on.setOnClickListener(ClickListener);
        btn_off.setOnClickListener(ClickListener);
        btn_fan_auto.setOnClickListener(ClickListener);
        btn_fan1.setOnClickListener(ClickListener);
        btn_fan2.setOnClickListener(ClickListener);
        btn_fan3.setOnClickListener(ClickListener);
        btn_temp_minus.setOnClickListener(ClickListener);
        btn_temp_plus.setOnClickListener(ClickListener);
        btn_time_minus.setOnClickListener(ClickListener);
        btn_time_plus.setOnClickListener(ClickListener);
        btn_gen_on.setOnClickListener(ClickListener);
        btn_gen_off.setOnClickListener(ClickListener);

        sb_vent.setMin(30);
        sb_vent.setMax(145);
        sb_vent.setOnSeekBarChangeListener(SeekBarListener);

        handler_update.postDelayed(update, 0);
        handler_gen_update.postDelayed(gen_update, 0);

        txv_temp_set.setText(getString(R.string.temp_set, temp_set));
        txv_vent.setText(getString(R.string.vent_str, 0));

        lyo_fan.setVisibility(View.GONE);
        lyo_set_temp.setVisibility(View.GONE);
        lyo_set_time.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        statusRec = false;
    }

    SeekBar.OnSeekBarChangeListener SeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            txv_vent.setText(getString(R.string.vent_str, progress));
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // unused
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            String progress = txv_vent.getText().toString();
            MyClientTask my_task = new MyClientTask(vent_address);
            my_task.execute("a" + progress);
            Log.i(TAG, progress);
        }
    };

    View.OnClickListener ClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MyClientTask my_task = new MyClientTask(address);
            MyClientTask my_task_gen = new MyClientTask(gen_address);
            if (v == btn_gen_on) {
                my_task_gen.execute("gen_on");
            }
            if (v == btn_gen_off) {
                my_task_gen.execute("gen_off");
            }
            if (v == btn_on) {
                setFocusButton(btn_fan3);
                my_task.execute("on");
            }
            if (v == btn_off) {
                my_task.execute("off");
            }
            if (is_on) {
                if (v == btn_fan_auto) {
                    my_task.execute("fauto");
                }
                if (v == btn_fan1) {
                    my_task.execute("f1");
                }
                if (v == btn_fan2) {
                    my_task.execute("f2");
                }
                if (v == btn_fan3) {
                    my_task.execute("f3");
                }
                if (v == btn_temp_minus) {
                    temp_set = (temp_set <= MIN_TEMP) ? MIN_TEMP : temp_set - 1;
                    txv_temp_set.setText(getString(R.string.temp_set, temp_set));
                    my_task.execute("t" + temp_set);
                }
                if (v == btn_temp_plus) {
                    temp_set = (temp_set >= MAX_TEMP) ? MAX_TEMP : temp_set + 1;
                    txv_temp_set.setText(getString(R.string.temp_set, temp_set));
                    my_task.execute("t" + temp_set);
                }
                if (v == btn_time_minus) {
                    if (i_times > 0) {
                        i_times--;
                        time_set = TIMES[i_times];
                    }
                    txv_time_set.setText(getString(R.string.time_set, (time_set/60)%60, time_set%60, 0));
                    handler_update.removeCallbacks(update);
                    handler_update.postDelayed(update, 3000);
                    is_timer = true;
                    my_task.execute("d" + time_set);
                }
                if (v == btn_time_plus) {
                    if (i_times < TIMES.length-1) {
                        i_times++;
                        time_set = TIMES[i_times];
                    }
                    txv_time_set.setText(getString(R.string.time_set, (time_set/60)%60, time_set%60, 0));
                    handler_update.removeCallbacks(update);
                    handler_update.postDelayed(update, 3000);
                    is_timer = true;
                    my_task.execute("d" + time_set);
                }
            }
        }
    };

    public class MyClientTask extends AsyncTask<String,Void,String> {
        String server;
        MyClientTask(String server) {
            this.server = server;
        }
        @Override
        protected String doInBackground(String... params) {
            final String msg = params[0];
            final String url = "http://" + server + "/" + msg;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (server.equals(address)) {
                        txv_sent.setText(url);
                    } else if (server.equals(gen_address)) {
                        txv_gen_sent.setText(url);
                    } else if (server.equals(vent_address)) {
                        txv_vent_sent.setText(url);
                    }
                }
            });
            return Conection.getData(url);
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                if (result == null) {
                    return;
                }
                String[] result_split = result.split(",");
                int device =  Integer.parseInt(result_split[0]);
                if (device == 0) { // General
                    String info = result_split[1];
                    txv_gen_received.setText(getString(R.string.gen_info, info));
                    checkGenInfo(info);
                } else if (device == 10) { // Rejilla
                    String info = result_split[1];
                    txv_vent_received.setText(getString(R.string.vent_info, info));
                } else {
                    String info = result_split[1];
                    String data = result_split[2];

                    txv_received.setText(getString(R.string.info, info, data));
                    checkInfo(info, data);
                }
                txv_log.setText(getString(R.string.txv_log));
            } catch (Exception e) {
                txv_log.setText(getString(R.string.offline));
                Log.e(TAG, e.toString());
            }
        }
    }

    void checkGenInfo(String info) {
        switch (info) {
            case "off":
                is_gen_on = false;
                txv_gen_color.setBackgroundColor(getColor(R.color.off));
                setFocusButton(btn_gen_off);
                break;
            case "on":
                is_gen_on = true;
                txv_gen_color.setBackgroundColor(getColor(R.color.on));
                setFocusButton(btn_gen_on);
                break;
        }
    }

    String getTemp(String temp) {
        try {
            Float.parseFloat(temp);
            return temp + " ºC";
        } catch (NumberFormatException e) {
            return getString(R.string.temp_nan);
        }
    }

    void checkInfo(String info, String data) {
        if (info.equals("update")) {
            String[] data_split = data.split(";");
            info = data_split[0];   // on/off
            data = data_split[1];   // temp
            int timeout = Integer.parseInt(data_split[2]);
            txv_time_set.setText(getString(R.string.time_set, timeout/(60*60), (timeout/60)%60, timeout%60));
            if (timeout > 0) {
                is_timer = true;
            }
            if (is_timer && timeout == 0) {
                btn_off.performClick();
                is_timer = false;
            }
        }

        switch (info) {
            case "off":
                is_on = false;
                i_times = 0;
                lyo_fan.setVisibility(View.GONE);
                lyo_set_temp.setVisibility(View.GONE);
                lyo_set_time.setVisibility(View.GONE);
                setFocusButton(btn_off);
                txv_color.setBackgroundColor(getColor(R.color.off));
                break;
            case "on":
                is_on = true;
                lyo_fan.setVisibility(View.VISIBLE);
                lyo_set_time.setVisibility(View.VISIBLE);
                setFocusButton(btn_on);
                txv_color.setBackgroundColor(getColor(R.color.on));
                break;
            case "fauto":
                if (getTemp(data).equals(R.string.temp_nan)) {
                    Toast.makeText(getApplicationContext(), getString(R.string.temp_err), Toast.LENGTH_SHORT).show();
                } else {
                    lyo_set_temp.setVisibility(View.VISIBLE);
                    setFocusButton(btn_fan_auto);
                }
                break;
            case "f1":
                lyo_set_temp.setVisibility(View.GONE);
                setFocusButton(btn_fan1);
                break;
            case "f2":
                lyo_set_temp.setVisibility(View.GONE);
                setFocusButton(btn_fan2);
                break;
            case "f3":
                lyo_set_temp.setVisibility(View.GONE);
                setFocusButton(btn_fan3);
                break;
        }
        txv_temp.setText(getTemp(data));
    }

    void setFocusButton(Button b) {
        if (b == btn_off) {
            btn_fan_auto.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan1.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan2.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan3.setBackgroundColor(getColor(R.color.colorPrimary));
        }
        if (b == btn_on || b == btn_off) {
            btn_on.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_off.setBackgroundColor(getColor(R.color.colorPrimary));
            b.setBackgroundColor(getColor(R.color.colorPrimaryDark));
        }
        if (b == btn_fan_auto || b == btn_fan1 || b == btn_fan2 || b == btn_fan3) {
            btn_fan_auto.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan1.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan2.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_fan3.setBackgroundColor(getColor(R.color.colorPrimary));
            b.setBackgroundColor(getColor(R.color.colorPrimaryDark));
        }
        if (b == btn_gen_on || b == btn_gen_off) {
            btn_gen_on.setBackgroundColor(getColor(R.color.colorPrimary));
            btn_gen_off.setBackgroundColor(getColor(R.color.colorPrimary));
            b.setBackgroundColor(getColor(R.color.colorPrimaryDark));
        }
    }

    private Runnable update = new Runnable() {
        @Override
        public void run() {
        if (statusRec) {
            MyClientTask my_task = new MyClientTask(address);
            my_task.execute("update");
            if (is_timer) {
                handler_update.postDelayed(this, T_TIME_UPDATE);
            } else {
                handler_update.postDelayed(this, T_TEMP_UPDATE);
            }
        } else {
            handler_update.removeCallbacks(update);
        }
        }
    };

    private Runnable gen_update = new Runnable() {
        @Override
        public void run() {
        if (statusRec) {
            MyClientTask my_task = new MyClientTask(gen_address);
            my_task.execute("gen_update");
            handler_gen_update.postDelayed(this, T_GEN_UPDATE);
        } else {
            handler_gen_update.removeCallbacks(gen_update);
        }
        }
    };
}