package com.example.terminalbluetooth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class MonitoringScreen extends Activity {
    private static final String TAG = "BlueTest5-MainActivity";

    //  for wake up phone
    private int incrementator;
    private android.view.Window window;
    private WindowManager.LayoutParams params;
    private int mMaxChars = 50000;//Default
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;
    private boolean mIsUserInitiatedDisconnect = false;

    // All controls here
    private TextView id_stup;
    private TextView id_stupina;
    private TextView nr_rame;
    private Button btnModify;
    private TextView mTxtReceive;
    private Button mBtnClearInput;
    private ScrollView scrollView;
    private CheckBox chkScroll;
    private CheckBox chkReceiveText;

    private String nr_rame_json;
    private String id_stupina_json;
    private String id_stup_json;

    private boolean mIsBluetoothConnected = false;
    private boolean activated = false;

    private BluetoothDevice mDevice;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_screen);

        ActivityHelper.initialize(this);
//        for wake up screen
        window = this.getWindow();
        params = this.getWindow().getAttributes();
        Button btn = findViewById(R.id.buttonActivateAntifurt);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MonitoringScreen.this, "Am activat sistemul de antifurt.\nVa rog nu mai miscati dispozitivul " +
                        "pentru a evita alarme false", Toast.LENGTH_LONG).show();
                activated = !activated;
            }
        });
        Intent intent = getIntent();

        Bundle b = intent.getExtras();

        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);

        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);
        Log.d(TAG, "Ready");
        id_stup = findViewById(R.id.id_stup_send);
        String var_test = "1a";
        id_stup_json = var_test;
        id_stup.setText(var_test);

        id_stupina = findViewById(R.id.id_stupina_send);
        var_test = "3";
        id_stupina_json = var_test;
        id_stupina.setText(var_test);

        nr_rame = findViewById(R.id.nr_rame_send);
        var_test = "10";
        nr_rame_json = var_test;
        nr_rame.setText(var_test);


        btnModify = findViewById(R.id.button);
        mTxtReceive = (TextView) findViewById(R.id.txtReceive);
        chkScroll = (CheckBox) findViewById(R.id.chkScroll);
        chkReceiveText = (CheckBox) findViewById(R.id.chkReceiveText);
        scrollView = (ScrollView) findViewById(R.id.viewScroll);
        mBtnClearInput = (Button) findViewById(R.id.btnClearInput);
        mTxtReceive.setMovementMethod(new ScrollingMovementMethod());


        btnModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nr_rame.onEditorAction(EditorInfo.IME_ACTION_DONE);
                id_stup.onEditorAction(EditorInfo.IME_ACTION_DONE);
                id_stupina.onEditorAction(EditorInfo.IME_ACTION_DONE);
                if (!nr_rame.getText().toString().equals("")) {
                    if (!id_stup.getText().toString().equals("")) {
                        if (!id_stupina.getText().toString().equals("")) {
                            nr_rame_json = nr_rame.getText().toString();
                            id_stupina_json = id_stupina.getText().toString();
                            id_stup_json = nr_rame.getText().toString();
                        } else {
                            Toast.makeText(MonitoringScreen.this, "Nu ați completat câmpul 'ID Stupina'", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(MonitoringScreen.this, "Nu ați completat câmpul 'ID Stup'", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(MonitoringScreen.this, "Nu ați completat câmpul 'Nr rame'", Toast.LENGTH_LONG).show();
                }
            }
        });

        mBtnClearInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTxtReceive.setText("");
            }
        });
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        Log.d(TAG, "Resumed");
        super.onResume();
    }

//    @Override
//    protected void onStop() {
//        Log.d(TAG, "Stopped");
//        super.onStop();
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
// TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    private class ReadInput implements Runnable, SensorEventListener {

        float x, y, z, x1, y1, z1;
        private boolean bStop = false;
        private Thread t;
        private String[] words;
        private Sensor mySensor;
        private SensorManager SM;
        private int contorShow1 = 1;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            SM = (SensorManager) getSystemService(SENSOR_SERVICE);

            // Accelerometer Sensor
            assert SM != null;
            mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            // Register sensor Listener
            SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
            t.start();
        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream inputStream;
            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    System.out.println("------ " + inputStream.read());
                    if (inputStream.available() > 0) {
                        System.out.println("------ " + inputStream.read());
                        inputStream.read(buffer);
                        int i = 0;
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);


                        if (strInput.length() < 19) {
                            System.out.println("He come to short!");
                        } else {
                            words = strInput.split(",");
                            System.out.println("Afisam cuvintele: " + Arrays.toString(words));
                            System.out.println("NOU: " + x + "; " + y + "; " + z);
                            System.out.println("VECHI: " + x1 + "; " + y1 + "; " + z1);
                            SendServerAsyncT sendServer = new SendServerAsyncT();
                            if (activated) {
                                System.out.println("We send real data about position of phone");
                                if ((x - x1) > 4 || (y - y1) > 4 || (z - z1) > 4) {
                                    sendServer.sendPost("http://192.168.1.83:5000/save_data/1", words, id_stup_json, id_stupina_json, nr_rame_json, false);
                                    x1 = x;
                                    y1 = y;
                                    z1 = z;
                                } else {
                                    x1 = x;
                                    y1 = y;
                                    z1 = z;
                                    sendServer.sendPost("http://192.168.1.83:5000/save_data/1", words, id_stup_json, id_stupina_json, nr_rame_json, true);
                                }
                            } else {
                                sendServer.sendPost("http://192.168.1.83:5000/save_data/1", words, id_stup_json, id_stupina_json, nr_rame_json, true);
                            }
                        }

                        incrementator++;
                        System.out.println(incrementator);
                        if (incrementator == 5) {
                            onPause();
                            onResume();
                            incrementator = 0;
                        }
                        /*
                         * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
                         */
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                        assert powerManager != null;
                        if (powerManager.isScreenOn()) {
                            if (chkReceiveText.isChecked()) {
                                mTxtReceive.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("Al doilea string pe care l-am primit: " + strInput);
                                        if (strInput.length() >= 15) {
                                            String toShow = mTxtReceive.getText() + "\n" + strInput;
                                            mTxtReceive.setText(toShow);
                                        }

                                        int txtLength = 0;
//                                    int txtLength = mTxtReceive.getEditableText().length();
                                        if (txtLength > mMaxChars) {
                                            mTxtReceive.getEditableText().delete(0, txtLength - mMaxChars);
                                            System.out.println("Mai afisam si asta: " + mTxtReceive.getText());
                                        }

                                        if (chkScroll.isChecked()) { // Scroll only if this is checked
                                            scrollView.post(new Runnable() { // Snippet from http://stackoverflow.com/a/4612082/1287554
                                                @Override
                                                public void run() {
                                                    scrollView.fullScroll(View.FOCUS_DOWN);
                                                }
                                            });
                                        }
                                    }
                                });
                            }

                        }
                    } else {
                        Thread.sleep(500);
                    }
                }
            } catch (IOException e) {
//                TODO Auto-generated catch block
//                Toast.makeText(this, "Eroare: " + e.toString(), Toast.LENGTH_LONG).show();
                System.out.println("Eroare: " + e.toString());
                e.printStackTrace();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            x = sensorEvent.values[0];
            y = sensorEvent.values[1];
            z = sensorEvent.values[2];
            if (contorShow1 == 1) {
                x1 = x;
                y1 = y;
                z1 = z;
                contorShow1++;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
// Don't need that.
        }

//        public void stop() {
//            bStop = true;
//        }

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (mReadThread != null) {
//                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }

    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(MonitoringScreen.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
        }

        @Override
        protected Void doInBackground(Void... devices) {

//            try {
            if (mBTSocket == null || !mIsBluetoothConnected) {
                try {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    mConnectSuccessful = false;
                    System.out.println("Nu merge");
                    return null;
                }
            }
//            } catch (IOException e) {
// Unable to connect to device
//            e.printStackTrace();
//            mConnectSuccessful = false;
//            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Nu s-a putut conecta la dizpozitiv." +
                        " Este un dispozitiv serial? Verifică dacă UUID este corect în setări", Toast.LENGTH_LONG).show();
                finish();
            } else {
                msg("Conectat cu dispozitivul");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }

            progressDialog.dismiss();
        }
    }

}
