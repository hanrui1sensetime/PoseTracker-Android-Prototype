package com.mmdeploy.posetracker;

import mmdeploy.PointF;

import org.opencv.core.*;
import org.opencv.imgproc.*;

/** @description: this is a util class for java demo. */
public class Draw {

    /** This function changes cvMat to Mat.
     * @param frame: the image with opencv Mat format.
     * @param results: the results array of PoseTracker.
     */
    public static void drawPoseTrackerResult(org.opencv.core.Mat frame, mmdeploy.PoseTracker.Result[] results)
    {
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
        else {
            frame = frame.clone();
        }
        for (int i = 0; i < results.length; i++) {
            mmdeploy.PoseTracker.Result pt = results[i];
            for (int j = 0; j < pt.keypoints.length; j++) {
                PointF p = pt.keypoints[j];
                p.x *= scale;
                p.y *= scale;
                pt.keypoints[j] = p;
            }
            float scoreThr = 0.5f;
            int used[] = new int[pt.keypoints.length * 2];
            for (int j = 0; j < skeleton.length; j++) {
                int u = skeleton[j][0];
                int v = skeleton[j][1];
                if (pt.scores[u] > scoreThr && pt.scores[v] > scoreThr) {
                    used[u] = used[v] = 1;
                    Point pointU = new Point(pt.keypoints[u].x, pt.keypoints[u].y);
                    Point pointV = new Point(pt.keypoints[v].x, pt.keypoints[v].y);
                    Imgproc.line(frame, pointU, pointV, palette[linkColor[j]], 4);
                }
            }
            for (int j = 0; j < pt.keypoints.length; j++) {
                if (used[j] == 1) {
                    Point p = new Point(pt.keypoints[j].x, pt.keypoints[j].y);
                    Imgproc.circle(frame, p, 1, palette[pointColor[j]], 2);
                }
            }
            float bbox[] = {pt.bbox.left, pt.bbox.top, pt.bbox.right, pt.bbox.bottom};
            for (int j = 0; j < 4; j++) {
                bbox[j] *= scale;
            }
            Imgproc.rectangle(frame, new Point(bbox[0], bbox[1]),
                    new Point(bbox[2], bbox[3]), new Scalar(0, 255, 0));
        }
    }
}
