package com.mmdeploy.posetracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.MediaStore;

import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.*;

import com.blankj.utilcode.util.PathUtils;
import com.blankj.utilcode.util.ResourceUtils;

import mmdeploy.*;

import java.io.File;
import java.lang.*;

public class MainActivity extends AppCompatActivity {
    private Button addVideoButton;
    private ImageView videoFrameView;
    private PoseTracker poseTracker;

    private int frameID;

    private VideoCapture videoCapture;

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initMMDeploy();
        setContentView(R.layout.activity_main);
        this.addVideoButton = (Button) findViewById(R.id.addVideoButton);
        this.videoFrameView = (ImageView) findViewById(R.id.videoFrameView);
        this.addVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                        .WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
                else {
                    openAlbum();
                }
            }
        });
    }

    private void openAlbum(){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent,2);
    }

    private void reload(String workDir)
    {
        String detModelPath=workDir + "/rtmdet-nano-ncnn-fp16";
        String poseModelPath=workDir + "/rtmpose-tiny-ncnn-fp16";
        String deviceName="cpu";
        int deviceID = 0;
        Model detModel = new Model(detModelPath);
        Model poseModel = new Model(poseModelPath);
        Device device = new Device(deviceName, deviceID);
        Context context = new Context();
        context.add(device);
        this.poseTracker = new mmdeploy.PoseTracker(detModel, poseModel, context);
    }

    private void initMMDeploy() {
        String workDir = PathUtils.getExternalAppFilesPath() + File.separator
                + "file";
        if (ResourceUtils.copyFileFromAssets("models", workDir)){
            reload(workDir);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                openAlbum();
            else Toast.makeText(MainActivity.this, "Invite memory refused.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2) {
            String path =null;
            Uri uri = data.getData();
            System.out.printf("debugging what is uri scheme: %s\n", uri.getScheme());
            // path = uri.getPath();
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null){
                if (cursor.moveToFirst()){
                    path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                }
                cursor.close();
            }
            System.out.printf("debugging what is path: %s\n", path);
            mmdeploy.PoseTracker.Params params = this.poseTracker.initParams();
            params.detInterval = 5;
            params.poseMaxNumBboxes = 6;
            long stateHandle = this.poseTracker.createState(params);

            this.videoCapture = new VideoCapture(path, org.opencv.videoio.Videoio.CAP_ANDROID);
            if (this.videoCapture.isOpened()) {
                System.out.printf("failed to open video: %s", path);
            }
            Double fps = videoCapture.get(org.opencv.videoio.Videoio.CAP_PROP_FPS);
            frameID = 0;
            Handler handler = new Handler();
            Runnable drawThread = new Runnable() {
                org.opencv.core.Mat frame = new org.opencv.core.Mat();
                public void run() {
                    videoCapture.read(frame);
                    System.out.printf("processing frame %d\n", frameID);
                    if (frame.empty()) {
                        return;
                    }
                    org.opencv.core.Mat cvMat = new org.opencv.core.Mat();
                    Imgproc.cvtColor(frame, cvMat, Imgproc.COLOR_RGB2BGR);
                    Mat mat = Utils.cvMatToMat(cvMat);
                    mmdeploy.PoseTracker.Result[] results = poseTracker.apply(stateHandle, mat, -1);
                    Draw.drawPoseTrackerResult(frame, results);
                    Bitmap bitmap = null;
                    bitmap = Bitmap.createBitmap(frame.width(), frame.height(), Bitmap.Config.ARGB_8888);
                    org.opencv.android.Utils.matToBitmap(frame, bitmap);
                    videoFrameView.setImageBitmap(bitmap);
                    videoFrameView.invalidate();
                    handler.postDelayed(this, (long) (1000 / fps));
                    frameID++;
                }
            };
            handler.post(drawThread);
        }
    }
}
