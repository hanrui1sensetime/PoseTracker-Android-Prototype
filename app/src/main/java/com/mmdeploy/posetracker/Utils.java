package com.mmdeploy.posetracker;

import mmdeploy.PixelFormat;
import mmdeploy.DataType;
import mmdeploy.Mat;

import org.opencv.core.*;

/** @description: this is a util class for java demo. */
public class Utils {

    /** This function changes cvMat to Mat.
     * @param cvMat: the image with opencv Mat format.
     * @return: the image with Mat format.
     */
    public static Mat cvMatToMat(org.opencv.core.Mat cvMat)
    {
        byte[] dataPointer = new byte[cvMat.rows() * cvMat.cols() * cvMat.channels() * (int)cvMat.elemSize()];
        cvMat.get(0, 0, dataPointer);
        return new Mat(cvMat.rows(), cvMat.cols(), cvMat.channels(),
                PixelFormat.BGR, DataType.INT8, dataPointer);
    }
}
