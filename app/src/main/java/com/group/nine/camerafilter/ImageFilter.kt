package com.group.nine.camerafilter

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.core.TermCriteria
import org.opencv.imgproc.Imgproc


class ImageFilter(blur_kernel : Int = 9,n_clusters:Int = 8, min_area :Int = 500, poly_epsilon:Int = 10) {
    fun processFace(face:Bitmap):Bitmap{
        Log.d("OPENCV:","Started Face Processing")
        val mat = Mat()
        Utils.bitmapToMat(face, mat)
        Log.d("OPENCV:","Converted Bitmap to Mat")
        // Convert to grayscale
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
        Log.d("OPENCV:","Converted Mat to grayscale")
        // Make a mutable face to copy grayscale image
        val grayBitmap = face.copy(face.config, true)
        Utils.matToBitmap(mat, grayBitmap)
        Log.d("OPENCV:","Converted Mat to Bitmap")
        return grayBitmap
    }
    fun kmeansProcess(face:Bitmap):Bitmap {
        val imageMat = Mat()
        Utils.bitmapToMat(face, imageMat)
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGRA2BGR)
        val reshaped_image: Mat = imageMat.reshape(1, imageMat.cols() * imageMat.rows())
        val reshaped_image32f = Mat()
        reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0)

        val labels = Mat()
        val criteria = TermCriteria(TermCriteria.COUNT, 100, 1.toDouble())
        val centers = Mat()
        val clusterCount = 5
        val attempts = 1
        Core.kmeans(
            reshaped_image32f,
            clusterCount,
            labels,
            criteria,
            attempts,
            Core.KMEANS_PP_CENTERS,
            centers
        )
        Log.d("KClusters","Labels Size"+labels.size().toString())
        Log.d("KClusters","Centers Size"+centers.size().toString())
        Log.d("KClusters","Flattened Image"+reshaped_image.size().height.toString())
        for(i in 0 until reshaped_image.height()){
            reshaped_image[1,i] = centers[labels[1,i]]
        }
        return face
    }
}


// Python Easy
//Z = orig.reshape((-1,3))
//Z = np.float32(Z)
//criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 10, 1.0)
//ret,label,center=cv2.kmeans(Z,5,None,criteria,10,cv2.KMEANS_RANDOM_CENTERS)
//center = np.uint8(center)
//res = center[label.flatten()]
//res2 = res.reshape((orig.shape))

//def cluster(im, n_clusters):
//    Z = im.reshape((-1,3))
//    Z = np.float32(Z)
//    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 10, 1.0)
//    ret,label,center=cv2.kmeans(Z,n_clusters,None,criteria,10,cv2.KMEANS_RANDOM_CENTERS)
//    label = label.flatten().tolist()
//    counts = {}
//    reps = center
//    # count colors per label
//    for i in range(len(Z)):
//    if label[i] not in counts:
//    counts[label[i]] = {}
//    rgb = tuple(Z[i])
//
//    if rgb not in counts[label[i]]:
//    counts[label[i]][rgb] = 0
//    counts[label[i]][rgb] += 1
//
//    # remap representative to most prominent color for ea label
//    for curr_label, hist in counts.items():
//    flat = sorted(hist.items(), key=lambda x: x[1], reverse=True)
//    reps[curr_label] = flat[0][0]
//    return center.astype('int32'), label