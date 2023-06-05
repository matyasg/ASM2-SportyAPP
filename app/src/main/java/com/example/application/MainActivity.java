package com.example.application;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Flow;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    // Defining TextViews to show values of probabilities for each activity
    private TextView walkTV;
    private TextView sitTV;
    private TextView standTV;
    // Defining TableRows for each activity
    private TableRow walkTR;
    private TableRow sitTR;
    private TableRow standTR;
    // Defining the ImageView to set an image with the activity
    private ImageView activityIV;
    // Defining Lists to keep all signals from sensors
    private static List<Float> bacc_x, bacc_y, bacc_z; // body component of acc.
    private static List<Float> gyr_x, gyr_y, gyr_z; // gyr. signal
    private static List<Float> all_data; // list for keeping all signals
    protected float[] gravity = new float[3]; // gravity component
    protected float[] filt_acc = new float[3]; // filtered acc. signal
    protected float[] filt_gyr = new float[3]; // filtered gyr. signal
    // Defining the Sensor Manager for acc. and gyr. sensors
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope;
    // Defining the Interpreter for the TensorFlow Lite model
    private Interpreter tflite;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 5*60*1000;
    int walkingTimes = 0;
    int standingTimes = 0;
    int sittingTimes = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Button creation
        Button exit_button = (Button) findViewById(R.id.button_exit);
        // Initializing TextViews with id
        walkTV = (TextView) findViewById(R.id.prob_walk);
        sitTV = (TextView) findViewById(R.id.prob_sit);
        standTV = (TextView) findViewById(R.id.prob_stand);
        // Initializing TableRows with id
        walkTR = (TableRow) findViewById(R.id.row_walk);
        sitTR = (TableRow) findViewById(R.id.row_sit);
        standTR = (TableRow) findViewById(R.id.row_stand);
        // Initializing the ImageView with id
        activityIV = (ImageView) findViewById(R.id.act_img);
        // Initializing Lists
        bacc_x = new ArrayList<>(); bacc_y = new ArrayList<>(); bacc_z = new ArrayList<>();
        gyr_x = new ArrayList<>(); gyr_y = new ArrayList<>(); gyr_z = new ArrayList<>();
        all_data = new ArrayList<>();
        // Activating the Sensor Manager for accelerometer and gyroscope
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // Activating the Listener to get signals from sensors with a sampling frequency 50Hz (20000us)
        mSensorManager.registerListener(this, mAccelerometer, 20000);
        mSensorManager.registerListener(this, mGyroscope, 20000);
        // Loading the Interpreter with a CNN model
        try{
            tflite = new Interpreter(loadModelFile());
        }catch (Exception ex){
            ex.printStackTrace();
        }
        // Loading the animation from the SpinKitView
        ProgressBar progressBar = (ProgressBar)findViewById(R.id.progress);
        Sprite doubleBounce = new DoubleBounce();
        progressBar.setIndeterminateDrawable(doubleBounce);
        // Initializing the ClickListener for the exit button
        exit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Designing a dialog window with answers
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("HAR Application");
                builder.setIcon(R.mipmap.ic_launcher);
                builder.setMessage("Do you want to close the app?")
                        .setCancelable(false)
                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        })
                        .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
        this.createNotificationChannel();
    }
    // Signals registration
    @Override
    public void onSensorChanged(SensorEvent event) {
        // Accelerometer signals
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            // Low-pass filter with a cutoff 20Hz to reduce noises from the acc. signals
            filt_acc[0] = ConstValues.ALPHA_1 * filt_acc[0] + (1 - ConstValues.ALPHA_1) * event.values[0]/10;
            filt_acc[1] = ConstValues.ALPHA_1 * filt_acc[1] + (1 - ConstValues.ALPHA_1) * event.values[1]/10;
            filt_acc[2] = ConstValues.ALPHA_1 * filt_acc[2] + (1 - ConstValues.ALPHA_1) * event.values[2]/10;

            // Using low-pass filter to separate gravity from the total acc. signals
            gravity[0] = ConstValues.ALPHA_2 * gravity[0] + (1 - ConstValues.ALPHA_2) * filt_acc[0];
            gravity[1] = ConstValues.ALPHA_2 * gravity[1] + (1 - ConstValues.ALPHA_2) * filt_acc[1];
            gravity[2] = ConstValues.ALPHA_2 * gravity[2] + (1 - ConstValues.ALPHA_2) * filt_acc[2];
            // Body acc. signals (sub gravity forces)
            bacc_x.add(filt_acc[0] - gravity[0]);
            bacc_y.add(filt_acc[1] - gravity[1]);
            bacc_z.add(filt_acc[2] - gravity[2]);
        }
        // Gyroscope signals
        if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Low-pass filter with a cutoff 20Hz to reduce noise from the gyr. signals
            filt_gyr[0] = ConstValues.ALPHA_1 * filt_gyr[0] + (1 - ConstValues.ALPHA_1) * event.values[0];
            filt_gyr[1] = ConstValues.ALPHA_1 * filt_gyr[1] + (1 - ConstValues.ALPHA_1) * event.values[1];
            filt_gyr[2] = ConstValues.ALPHA_1 * filt_gyr[2] + (1 - ConstValues.ALPHA_1) * event.values[2];
            // Gyr. signals after filtering
            gyr_x.add(filt_gyr[0]);
            gyr_y.add(filt_gyr[1]);
            gyr_z.add(filt_gyr[2]);
        }
        // Using the signals window in size 64
        if (bacc_x.size() >= ConstValues.NUM_SAMPLES && bacc_y.size() >= ConstValues.NUM_SAMPLES && bacc_z.size() >= ConstValues.NUM_SAMPLES &&
                gyr_x.size() >= ConstValues.NUM_SAMPLES && gyr_y.size() >= ConstValues.NUM_SAMPLES && gyr_z.size() >= ConstValues.NUM_SAMPLES) {

            // Putting values into all_data
            all_data.addAll(bacc_x.subList(0,ConstValues.NUM_SAMPLES));
            all_data.addAll(bacc_y.subList(0,ConstValues.NUM_SAMPLES));
            all_data.addAll(bacc_z.subList(0,ConstValues.NUM_SAMPLES));
            all_data.addAll(gyr_x.subList(0,ConstValues.NUM_SAMPLES));
            all_data.addAll(gyr_y.subList(0,ConstValues.NUM_SAMPLES));
            all_data.addAll(gyr_z.subList(0,ConstValues.NUM_SAMPLES));
            // Using a model to get results
            giveTheResult(all_data);
            // Clearing all Lists
            clearData();
        }
    }
    @Override // Default parameters for the SensorManager
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, 20000);
        mSensorManager.registerListener(this, mGyroscope, 20000);
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                Toast.makeText(MainActivity.this, ""+checkWalkRate(),
                        Toast.LENGTH_SHORT).show();
                if(checkWalkRate() < 0.05){

                    sendNotification();
                }
                walkingTimes=0;
                standingTimes=0;
                sittingTimes=0;
            }
        }, delay);
    }
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    // Loading the TensorFlow Lite model from the file
    private MappedByteBuffer loadModelFile() throws IOException{
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("newmodel.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel  =inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    // Function to convert all_data to the 2D Float list (128, 9)
    private float[][] sizeConversion(List<Float> all_data) {
        float[][] two_dim_list = new float[ConstValues.NUM_SAMPLES][ConstValues.NUM_INPUT];
        for (int counter = 0; counter < all_data.size(); counter ++){
            int integer_division = counter / ConstValues.NUM_SAMPLES;
            int remainder = counter % ConstValues.NUM_SAMPLES;
            Float value = all_data.get(counter);
            two_dim_list[remainder][integer_division] = (value != null ? value: Float.NaN);
        }
        return two_dim_list;
    }
    // Function to set values of probabilities in TextViews
    private void propabilities(float[][] results){
        sitTV.setText(String.format("%.02f", results[0][0]));
        standTV.setText(String.format("%.02f", results[0][1]));
        walkTV.setText(String.format("%.02f", results[0][2]));
    }
    // Function to set the image with the most likely activity into ImageView
    private void setActivity(float[][] results){
        int index = 0;
        float max = 0;
        // Finding the highest probability and saving the index
        for(int i = 0; i < results[0].length; i++){
            if(results[0][i]>=max){
                index = i;
                max = results[0][i];}
        }
        // Setting the default background color for TextViews
        walkTR.setBackgroundColor(Color.rgb(243,249,249));
        sitTR.setBackgroundColor(Color.rgb(243,249,249));
        standTR.setBackgroundColor(Color.rgb(243,249,249));
        // Setting the yellow background color to the most likely activity
        if(index==0){
            sittingTimes++;
            activityIV.setImageResource(R.drawable.sitting);
            sitTR.setBackgroundColor(Color.rgb(247,255,147));}
        if(index==1){
            standingTimes++;
            activityIV.setImageResource(R.drawable.standing);
            standTR.setBackgroundColor(Color.rgb(247,255,147));}
        if(index==2){
            walkingTimes++;
            activityIV.setImageResource(R.drawable.walking);
            walkTR.setBackgroundColor(Color.rgb(247,255,147));
        }
    }

    private float checkWalkRate(){
        return (float)this.walkingTimes/(this.walkingTimes + this.sittingTimes + this.standingTimes);
    }
    private void sendNotification(){
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, getString(R.string.CHANNEL_ID))
                .setSmallIcon(R.drawable.walking)
                .setContentTitle("Let's walk!")
                .setContentText("We are taking care of your health, let's walk for a few minutes!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        long[] pattern = {500,500,500,500,500,500,500,500,500};
        builder.setVibrate(pattern);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(0, builder.build());
    }

    // Function to run the model and get results
    private void giveTheResult(List<Float> all_data){
        // Changing the size of the all_data into (128,9)
        float[][] two_dim_list = sizeConversion(all_data);
        // Fitting the size of the input data to the model (1,128,9)
        float [][][] input = new float[1][ConstValues.NUM_SAMPLES][ConstValues.NUM_INPUT];
        input[0] = two_dim_list;
        // Initializing the output list (1,6)
        float [][] prediction_results = new float[1][ConstValues.NUM_OUTPUT];
        // Running the tf_lite model, the output will be written in prediction_results
        tflite.run(input, prediction_results);
        // Probabilities visualisation
        propabilities(prediction_results);
        setActivity(prediction_results);
    }
    // Function to clear Lists with data from sensors to make a new window with samples
    private void clearData(){
        bacc_x.clear(); bacc_y.clear(); bacc_z.clear();
        gyr_x.clear(); gyr_y.clear(); gyr_z.clear();
        all_data.clear();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.CHANNEL_ID), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}