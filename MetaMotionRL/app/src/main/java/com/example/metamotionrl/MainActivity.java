package com.example.metamotionrl;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Led;
import com.mbientlab.metawear.module.SensorFusionBosch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection {

    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard board;
    private MetaWearBoard board2;
    private Accelerometer accelerometer;
    private Accelerometer accelerometer2;
    private GyroBmi160 gyroscope;
    private final String macAddr = "D9:87:32:9B:56:71";  //right
    private final String macAddr2 = "CD:F5:FD:51:BD:0B"; //left
    private TextView ax;
    private TextView ay;
    private TextView az;
    private TextView ax2;
    private TextView ay2;
    private TextView az2;
    private TextView gyroX;
    private TextView gyro2;
    private boolean connection = false;
    private boolean connection2 = false;
    private Led led;
    private Led led2;
    private List<String> resultRight = new ArrayList<>();
    private List<String> resultLeft = new ArrayList<>();
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ax = (TextView) findViewById(R.id.xAxis);
        ay = (TextView) findViewById(R.id.yAxis);
        az = (TextView) findViewById(R.id.zAxis);

        ax2 = (TextView) findViewById(R.id.xAxis2);
        ay2 = (TextView) findViewById(R.id.yAxis2);
        az2 = (TextView) findViewById(R.id.zAxis2);

        gyroX = (TextView) findViewById(R.id.gyro);

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);

//        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                accelerometer.acceleration().start();
//                accelerometer.start();
//            }
//        });
//
//        findViewById(R.id.stop).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                accelerometer.acceleration().stop();
//                accelerometer.stop();
//            }
//        });
//
//        findViewById(R.id.reset).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                board.tearDown();
//                ax.setText("");
//                ay.setText("");
//                az.setText("");
//                ax2.setText("");
//                ay2.setText("");
//                az2.setText("");
//            }
//        });

    }

    public void onStartClicked (View view){

        try {
            accelerometer.acceleration().start();
            accelerometer.start();
            accelerometer2.acceleration().start();
            accelerometer2.start();
//            gyroscope.angularVelocity().start();
            if (connection = connection2 = true) {
                Toast.makeText(this, "Conntected to sensors", Toast.LENGTH_LONG).show();
            }

            led = board.getModule(Led.class);
            led2 = board2.getModule(Led.class);

            led.editPattern(Led.Color.BLUE, Led.PatternPreset.SOLID)
                    .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                    .commit();
            led.play();

            led2.editPattern(Led.Color.RED, Led.PatternPreset.SOLID)
                    .repeatCount(Led.PATTERN_REPEAT_INDEFINITELY)
                    .commit();
            led2.play();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to conntect to sensors", Toast.LENGTH_LONG).show();
        }
    }

    public void onStopClicked (View view){
        try {
            accelerometer.acceleration().stop();
            accelerometer.stop();
            accelerometer2.acceleration().stop();
            accelerometer2.stop();
//        gyroscope.angularVelocity().stop();

            led = board.getModule(Led.class);
            led2 = board2.getModule(Led.class);
            led.stop(true);
            led2.stop(true);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onResetClicked (View view){
        //board.tearDown();
        ax.setText("");
        ay.setText("");
        az.setText("");
        ax2.setText("");
        ay2.setText("");
        az2.setText("");
        resultRight.clear();
        resultLeft.clear();
    }

    public void onSaveClicked (View view){

        if (resultRight.size() != 0 && resultLeft.size() != 0) {
            count++;
            writeFileExternalStorage("Left_" + count + ".csv", resultRight);
            writeFileExternalStorage("Right_" + count + ".csv", resultLeft);
        }
        else {
            Toast.makeText(this, "No data to save", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (BtleService.LocalBinder) service;
        Log.i("messsage", "Service connected");
        retrieveBoard();
        retrieveBoard2();
    }


    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    private void retrieveBoard() {

        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr);

        board = serviceBinder.getMetaWearBoard(remoteDevice);
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {

            @Override
            public Task<Route> then(Task<Void> task) throws Exception {
                Log.i("messsage", "connected to: " + macAddr);
                connection = true;

                accelerometer = board.getModule(Accelerometer.class);
                accelerometer.configure()
                        .odr(15f)
                        .commit();

                return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {

                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
//                                Log.i("data", data.value(Acceleration.class).toString());
                                ax.setText(String.valueOf(data.value(Acceleration.class).x()));
                                ay.setText(String.valueOf(data.value(Acceleration.class).y()));
                                az.setText(String.valueOf(data.value(Acceleration.class).z()));
                                resultRight.add(ax.getText().toString() + "; " + ay.getText().toString() + "; " + az.getText().toString());
//                                Log.i("table", ax.getText().toString() + ", " + ay.getText().toString() + ", " + ay.getText().toString());
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                if (task.isFaulted()) {
                    Log.i("messsage", "Failed to connect", task.getError());
                } else {
                    Log.i("messsage", "App configured");
                    connection = true;
                }
                return null;
            }
        });

    }

//    private void retrieveBoardGyro() {
//
//        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr);
//
//        board = serviceBinder.getMetaWearBoard(remoteDevice);
//        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
//
//            @Override
//            public Task<Route> then(Task<Void> task) throws Exception {
//                Log.i("messsage", "connected to: " + macAddr);
//
//                gyroscope = board.getModule(GyroBmi160.class);
//                gyroscope.configure()
//                        .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
//                        .range(GyroBmi160.Range.FSR_2000)
//                        .commit();
//
//                return gyroscope.angularVelocity().addRouteAsync(new RouteBuilder() {
//
//                    @Override
//                    public void configure(RouteComponent source) {
//                        source.stream(new Subscriber() {
//                            @Override
//                            public void apply(Data data, Object ... env) {
//                                Log.i("data", data.value(AngularVelocity.class).toString());
//                                gyroX.setText(String.valueOf(data.value(AngularVelocity.class).x()));
//                            }
//                        });
//                    }
//                });
//            }
//        }).continueWith(new Continuation<Route, Void>() {
//            @Override
//            public Void then(Task<Route> task) throws Exception {
//                if (task.isFaulted()) {
//                    Log.i("messsage", "Failed to connect", task.getError());
//                } else {
//                    Log.i("messsage", "App configured");
//                }
//                return null;
//            }
//        });
//
//    }
//
//    private void retrieveBoardGyro2() {
//        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr);
//
//        board = serviceBinder.getMetaWearBoard(remoteDevice);
//        final GyroBmi160 gyroBmi160 = board.getModule(GyroBmi160.class);
//        gyroBmi160.configure()
//                .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
//                .range(GyroBmi160.Range.FSR_2000)
//                .commit();
//
//        gyroBmi160.angularVelocity().addRouteAsync(new RouteBuilder() {
//            @Override
//            public void configure(RouteComponent source) {
//                source.stream(new Subscriber() {
//                    @Override
//                    public void apply(Data data, Object ... env) {
//                        Log.i("data", data.value(AngularVelocity.class).toString());
//                    }
//                });
//            }
//        }).continueWith(new Continuation<Route, Void>() {
//            @Override
//            public Void then(Task<Route> task) throws Exception {
//                gyroBmi160.angularVelocity();
//                gyroBmi160.start();
//                return null;
//            }
//        });
//
//    }

    private void retrieveBoard2() {

        final BluetoothManager btManager= (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = btManager.getAdapter().getRemoteDevice(macAddr2);

        board2 = serviceBinder.getMetaWearBoard(remoteDevice);
        board2.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {

            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                Log.i("messsage", "connected to: " + macAddr2);
                connection2 = true;

                accelerometer2 = board2.getModule(Accelerometer.class);
                accelerometer2.configure()
                        .odr(15f)
                        .commit();

                return accelerometer2.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
//                                Log.i("data2", data.value(Acceleration.class).toString());
                                ax2.setText(String.valueOf(data.value(Acceleration.class).x()));
                                ay2.setText(String.valueOf(data.value(Acceleration.class).y()));
                                az2.setText(String.valueOf(data.value(Acceleration.class).z()));
                                resultLeft.add(ax2.getText().toString() + "; " + ay2.getText().toString() + "; " + az2.getText().toString());
//                                Log.i("table", ax2.getText().toString() + ", " + ay2.getText().toString() + ", " + ay2.getText().toString());
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {

                if (task.isFaulted()) {
                    Log.i("messsage", "Failed to connect", task.getError());
                } else {
                    Log.i("messsage", "App configured");
                    connection2 = true;
                }

                return null;
            }
        });
    }

    public void writeFileExternalStorage(String filenameExternal, List textToWrite) {

        //Text of the Document
        String header = "Acceleration:";
        String description = "X; Y; Z";

        //Checking the availability state of the External Storage.
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {

            //If it isn't mounted - we can't write into it.
            return;
        }

        //Create a new file that points to the root directory, with the given name:
        File file = new File(getExternalFilesDir(null), filenameExternal);

        //This point and below is responsible for the write operation
        FileOutputStream outputStream = null;
        try {
            file.createNewFile();
            //second argument of FileOutputStream constructor indicates whether
            //to append or create new file if one exists
            outputStream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);

            writer.append(header);
            writer.append("\n");
            writer.append(description);
            writer.append("\n");

            for (int i = 0; i < textToWrite.size(); i++) {
                writer.append(textToWrite.get(i).toString());
                writer.append("\n");
            }

            writer.close();
            outputStream.flush();
            outputStream.close();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error occurred", Toast.LENGTH_SHORT).show();
        }
    }


}
