package com.example.maov.moverec;

import android.app.Activity;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MainActivity extends Activity implements SensorEventListener {
    private int ID                  = -1;
    private int CAPTURE             = 0;
    private int STATUS              = -1; //STOP
    private long time               = -1;

    public static int MAXCAPTURE    = 30;
    public static int INACTIVE      = -1;
    public static int CAPTURING     = 0;
    public static int SAVING        = 1;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private int sensorSpeed         = SensorManager.SENSOR_DELAY_GAME;

    private Button btnCap;
    private TextView txtCount, accX, accY, accZ;
    private TextView gyrX, gyrY, gyrZ;
    private EditText txtID;

    private ArrayList<Data> DataCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if( sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, sensorSpeed);
            notify("Accelerometer found", Toast.LENGTH_SHORT);
        }
        else{
            notify("No acc detected", Toast.LENGTH_SHORT);
            accelerometer = null;
        }

        if( sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorManager.registerListener(this, gyroscope, sensorSpeed);
            notify("Gyroscope found", Toast.LENGTH_SHORT);

        }
        else{
            notify("No gyro detected", Toast.LENGTH_SHORT);
            gyroscope = null;
        }

        //List
        DataCenter = new ArrayList<>();

        btnCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (STATUS == INACTIVE) {                           //Pre-capture mode
                    txtID.setEnabled(false);                      //Disable the input field
                    ID = Integer.parseInt(txtID.getText().toString());   //Grab ID from field
                    time = System.currentTimeMillis();          //Grab start time
                    CAPTURE = 1;                                    //Set counter to 1 (first capture)
                    txtCount.setText(Integer.toString(CAPTURE) +   //Set text of counter field
                            "/" + Integer.toString(MAXCAPTURE));
                    btnCap.setText("Capturing!");                 //Set text of button
                    DataCenter.add(new Data(ID, CAPTURE, System.currentTimeMillis()));
                    STATUS = CAPTURING;                            //Update capture mode;
                    notify("Capture started", Toast.LENGTH_SHORT);
                } else if (STATUS == CAPTURING) {
                    CAPTURE = CAPTURE + 1;                      //Increment capture counter
                    if (CAPTURE <= MAXCAPTURE) {                      //While capturing Data
                        if (CAPTURE == MAXCAPTURE) {                //Update button text on last capture
                            btnCap.setText("STOP CAPTURING!");    //Update button
                        }
                    } else { //SAVE ETC
                        notify("Capture stopped", Toast.LENGTH_SHORT);
                        btnCap.setText("Saving...");                //Set text of button
                        btnCap.setEnabled(false);
                        STATUS = SAVING;                            //Start Saving data
                        saveData();

                        STATUS = INACTIVE;                         //Deactivate capture
                        ID = ID + 1;                           //Increment ID
                        CAPTURE = 0;                                //Reset Capture counter
                        txtID.setText(Integer.toString(ID));      //Set new ID

                        txtID.setEnabled(true);                   //Enable the input field
                        btnCap.setEnabled(true);                    //Enable the button
                        btnCap.setText("Start Capture");            //Set text of button
                    }

                    txtCount.setText(Integer.toString(CAPTURE) +   //Update textfield for counter
                            "/" + Integer.toString(MAXCAPTURE));
                }
            }

            public void notify(String str, int dur) {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, str, dur);
                toast.show();
            }
        });
    }

    public void initializeViews(){
        //Dynamic text fields
        accX = (TextView) findViewById(R.id.accX);
        accY = (TextView) findViewById(R.id.accY);
        accZ = (TextView) findViewById(R.id.accZ);

        gyrX = (TextView) findViewById(R.id.gyrX);
        gyrY = (TextView) findViewById(R.id.gyrY);
        gyrZ = (TextView) findViewById(R.id.gyrZ);

        txtCount = (TextView) findViewById(R.id.txtCount);
        txtCount.setText("0/" + Integer.toString(MAXCAPTURE));

        //Input fields
        txtID = (EditText) findViewById(R.id.txtID);

        //Buttons
        btnCap = (Button) findViewById(R.id.btnCap);

        File folder = new File("/storage/sdcard0/DATAS");
        boolean success = true;
        if (!folder.exists()) {
            Toast.makeText(MainActivity.this, "Directory Does Not Exist, Create It", Toast.LENGTH_SHORT).show();
            success = folder.mkdir();
        }
        if (success) {
            notify("Directory Created", Toast.LENGTH_SHORT);
        } else {
            notify( "Failed - Error occured. Can't create the directory.", Toast.LENGTH_SHORT);
            btnCap.setEnabled( false );
            txtID.setEnabled( false );
            txtCount.setText( "Save folder wasn't created, I've disabled the application." );
        }
    }

    public void onAccuracyChanged( Sensor s, int i ){
        return;
    }

    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, sensorSpeed);
        sensorManager.registerListener(this, gyroscope, sensorSpeed);
    }

    protected void onPause(){
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    public void onSensorChanged( SensorEvent ev) {
        int evType = ev.sensor.getType();                           //Grab sensortype
        updateView(evType, ev.values);                              //Visually update the GUI

        if( STATUS != CAPTURING ){
            return;                           //Don't bother if not capturing
        }

        Data prevData = DataCenter.get( DataCenter.size() - 1);     //Grab previous element
        if( !prevData.isGyroSet() && evType == Sensor.TYPE_GYROSCOPE ){
            prevData.addGyro(ev.values);                                        //Add gyro data
        }
        else if( !prevData.isAccSet() && evType == Sensor.TYPE_ACCELEROMETER ){
            prevData.addAcc(ev.values);                                         //Add Acc data
        }
        else {                                                                  //Both is set
            Data tmpData = new Data(ID, CAPTURE, System.currentTimeMillis());   //Create object
            if (evType == Sensor.TYPE_ACCELEROMETER)
                tmpData.addAcc(ev.values);                                      //Add accell
            else
                tmpData.addGyro(ev.values);                                     //Add gyro
            DataCenter.add(tmpData);                                            //Add object to list
        }
    }

    public void updateView(int type, float[] values){
        if( type == Sensor.TYPE_ACCELEROMETER ){
            accX.setText( Float.toString( Math.round(values[0]) ) ); //Set text of visual fields
            accY.setText( Float.toString( Math.round(values[1]) ) );
            accZ.setText( Float.toString( Math.round(values[2]) ) );
        }
        else if(type == Sensor.TYPE_GYROSCOPE ){
            gyrX.setText( Float.toString( Math.round(values[0]) ) ); //Set text of visual fields
            gyrY.setText( Float.toString( Math.round(values[1]) ) );
            gyrZ.setText(Float.toString(Math.round(values[2])));
        }
    }


    private void saveData(){
        //String fname =  getFilesDir().getAbsolutePath().toString() + "bio_" +
        //                Integer.toString(ID) + "_" + Long.toString(time) + ".csv";
        String fname =  "/storage/sdcard0/DATAS/bio_" +
                Integer.toString(ID) + "_" + Long.toString(time) + ".csv";

        try{
            File file = new File( fname );
            FileWriter out = new FileWriter( file );
            out.write( "ID,Num,Time,TransX,TransY,TransZ,RotX,RotY,RotZ\n");
            while( !DataCenter.isEmpty() ){
                Data tmp = DataCenter.remove(0);
                out.write( tmp.csv() );
            }
            out.flush();
            out.close();
            notify("Data was stored: " + fname, Toast.LENGTH_LONG );
        }
        catch ( IOException e){
            notify(e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    public void notify( String str, int dur ){
        Context context = getApplicationContext();
        Toast toast = Toast.makeText( context, str, dur );
        toast.show();
    }
}
