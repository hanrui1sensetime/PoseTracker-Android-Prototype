package com.mmdeploy.posetracker;

import static java.lang.Math.round;

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

import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
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
                    int skeleton[][] = {{15, 13}, {13, 11}, {16, 14}, {14, 12}, {11, 12}, {5, 11}, {6, 12},
                            {5, 6}, {5, 7}, {6, 8}, {7, 9}, {8, 10}, {1, 2}, {0, 1},
                            {0, 2}, {1, 3}, {2, 4}, {3, 5}, {4, 6}};
                    Scalar palette[] = {new Scalar(255, 128, 0), new Scalar(255, 153, 51), new Scalar(255, 178, 102),
                            new Scalar(230, 230, 0), new Scalar(255, 153, 255), new Scalar(153, 204, 255),
                            new Scalar(255, 102, 255), new Scalar(255, 51, 255), new Scalar(102, 178, 255),
                            new Scalar(51, 153, 255), new Scalar(255, 153, 153), new Scalar(255, 102, 102),
                            new Scalar(255, 51, 51), new Scalar(153, 255, 153), new Scalar(102, 255, 102),
                            new Scalar(51, 255, 51), new Scalar(0, 255, 0), new Scalar(0, 0, 255),
                            new Scalar(255, 0, 0), new Scalar(255, 255, 255)};
                    int linkColor[] = {
                            0, 0, 0, 0, 7, 7, 7, 9, 9, 9, 9, 9, 16, 16, 16, 16, 16, 16, 16
                    };
                    int pointColor[] = {16, 16, 16, 16, 16, 9, 9, 9, 9, 9, 9, 0, 0, 0, 0, 0, 0};

                    float scale = 1280 / (float)Math.max(frame.cols(), frame.rows());
                    if (scale != 1) {
                        Imgproc.resize(frame, frame, new Size(), scale, scale);
                    }
                    else
                    {
                        frame = frame.clone();
                    }
                    for (int i = 0; i < results.length; i++)
                    {
                        mmdeploy.PoseTracker.Result pt = results[i];
                        for (int j = 0; j < pt.keypoints.length; j++)
                        {
                            PointF p = pt.keypoints[j];
                            p.x *= scale;
                            p.y *= scale;
                            pt.keypoints[j] = p;
                        }
                        float scoreThr = 0.5f;
                        int used[] = new int[pt.keypoints.length * 2];
                        for (int j = 0; j < skeleton.length; j++)
                        {
                            int u = skeleton[j][0];
                            int v = skeleton[j][1];
                            if (pt.scores[u] > scoreThr && pt.scores[v] > scoreThr)
                            {
                                used[u] = used[v] = 1;
                                Point pointU = new Point(pt.keypoints[u].x, pt.keypoints[u].y);
                                Point pointV = new Point(pt.keypoints[v].x, pt.keypoints[v].y);
                                Imgproc.line(frame, pointU, pointV, palette[linkColor[j]], 4);
                            }
                        }
                        for (int j = 0; j < pt.keypoints.length; j++)
                        {
                            if (used[j] == 1)
                            {
                                Point p = new Point(pt.keypoints[j].x, pt.keypoints[j].y);
                                Imgproc.circle(frame, p, 1, palette[pointColor[j]], 2);
                            }
                        }
                        float bbox[] = {pt.bbox.left, pt.bbox.top, pt.bbox.right, pt.bbox.bottom};
                        for (int j = 0; j < 4; j++)
                        {
                            bbox[j] *= scale;
                        }
                        Imgproc.rectangle(frame, new Point(bbox[0], bbox[1]),
                                new Point(bbox[2], bbox[3]), new Scalar(0, 255, 0));
                    }
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