package com.example.maov.moverec;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class MainActivity extends Activity implements SensorEventListener {
    private int ID                  = -1;
    private int CAPTURE             = 0;
    private int STATUS              = -1; //STOP
    private long time               = -1;
    private float lastX, lastY, lastZ;

    public static int MAXCAPTURE    = 5;
    public static int INACTIVE      = -1;
    public static int CAPTURING     = 0;
    public static int SAVING        = 1;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private Button btnCap;
    private TextView txtCount, curX, curY, curZ;
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
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        else{
            return;
        }
        //List
        DataCenter = new ArrayList<>();

        btnCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if( STATUS == INACTIVE ){                           //Pre-capture mode
                        txtID.setEnabled( false );                      //Disable the input field
                        ID      = Integer.parseInt(txtID.getText().toString());   //Grab ID from field
                        time    =  System.currentTimeMillis();          //Grab start time
                        CAPTURE = 1;                                    //Set counter to 1 (first capture)
                        txtCount.setText( Integer.toString(CAPTURE) +   //Set text of counter field
                                "/" + Integer.toString(MAXCAPTURE) );
                        btnCap.setText( "Capturing!" );                 //Set text of button
                        STATUS  = CAPTURING;                            //Update capture mode;
                    }
                    else if( STATUS == CAPTURING ){
                        CAPTURE     = CAPTURE + 1;                      //Increment capture counter
                        if(CAPTURE <= MAXCAPTURE){                      //While capturing Data
                            if( CAPTURE == MAXCAPTURE ){                //Update button text on last capture
                                btnCap.setText( "STOP CAPTURING!" );    //Update button
                            }
                        }
                        else{ //SAVE ETC
                            btnCap.setText("Saving...");                //Set text of button
                            btnCap.setEnabled(false);
                            STATUS = SAVING;                            //Start Saving data
                            saveData();

                            STATUS  = INACTIVE;                         //Deactivate capture
                            ID      = ID + 1;                           //Increment ID
                            CAPTURE = 0;                                //Reset Capture counter
                            txtID.setText( Integer.toString(ID) );      //Set new ID

                            txtID.setEnabled( true );                   //Enable the input field
                            btnCap.setEnabled(true);                    //Enable the button
                            btnCap.setText("Start Capture");            //Set text of button
                        }

                        txtCount.setText( Integer.toString(CAPTURE) +   //Update textfield for counter
                                "/" + Integer.toString(MAXCAPTURE) );
                    }
                }
                catch (Exception e){
                    notify( e.getMessage(), Toast.LENGTH_LONG );
                }
            }
            public void notify( String str, int dur ){
                Context context = getApplicationContext();
                Toast toast = Toast.makeText( context, str, dur );
                toast.show();
            }
        });
    }

    public void initializeViews(){
        //Dynamic text fields
        curX = (TextView) findViewById(R.id.curX);
        curY = (TextView) findViewById(R.id.curY);
        curZ = (TextView) findViewById(R.id.curZ);
        txtCount = (TextView) findViewById(R.id.txtCount);
        txtCount.setText( "0/" + Integer.toString(MAXCAPTURE) );

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
            Toast.makeText(MainActivity.this, "Directory Created", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Failed - Error", Toast.LENGTH_SHORT).show();
        }
    }

    public void onAccuracyChanged( Sensor s, int i ){
        return;
    }

    //Resume
    protected void onResume(){
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause(){
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    public void onSensorChanged( SensorEvent ev ){
        float dX = lastX - ev.values[0];                    //Calculate delta values
        float dY = lastY - ev.values[1];
        float dZ = lastZ - ev.values[2];

        lastX = ev.values[0];                               //Update previous values
        lastY = ev.values[1];
        lastZ = ev.values[2];

        if( dX < 0.5  && dX > -0.5 )  dX = 0.0F;
        if( dY < 0.5  && dY > -0.5 )  dY = 0.0F;
        if( dZ < 0.5  && dZ > -0.5 )  dZ = 0.0F;

        updateView(dX, dY, dZ);                             //Visually update the GUI

        if( STATUS == CAPTURING ){                          //Add data to temporary lis
            try {
                Data d = new Data(ID, CAPTURE, lastX, lastY, lastZ, System.currentTimeMillis());
                DataCenter.add(d);                            //Add object to datacenter
            }
            catch (Exception e){
                notify( e.getMessage(), Toast.LENGTH_LONG );
            }
        }
    }

    public void updateView(float x, float y, float z){
        try{
            curX.setText( Float.toString(x) + "G" );            //Set text of visual fields
            curY.setText( Float.toString(y) + "G" );
            curZ.setText( Float.toString(z) + "G" );
        }
        catch (Exception e){
            notify( e.getMessage(), Toast.LENGTH_LONG );
        }
    }


    private void saveData(){
        notify("STORING DATA to file. JK", Toast.LENGTH_SHORT);
        //String fname =  getFilesDir().getAbsolutePath().toString() + "bio_" +
        //                Integer.toString(ID) + "_" + Long.toString(time) + ".csv";
        String fname =  "/storage/sdcard0/DATAS/bio_" +
                Integer.toString(ID) + "_" + Long.toString(time) + ".csv";

        notify("PATH: " + fname, Toast.LENGTH_LONG);

        try{
            File file = new File( fname );
            FileWriter out = new FileWriter( file );
            out.write( "ID,Num,Time,TransX,TransY,TransZ,RotX,RotY,RotZ\n");
            while( !DataCenter.isEmpty() ){
                Data tmp = DataCenter.remove(0);
                out.write( tmp.csv());
                out.write(",0,0,0\n");
            }

            out.flush();
            out.close();
        }
        catch ( IOException e){
            notify(e.getMessage(), Toast.LENGTH_LONG);
        }

        notify("DATA STORED", Toast.LENGTH_SHORT);
    }

    public void notify( String str, int dur ){
        Context context = getApplicationContext();
        Toast toast = Toast.makeText( context, str, dur );
        toast.show();
    }
}
