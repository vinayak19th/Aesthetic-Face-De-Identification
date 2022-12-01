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


class ImageFilter(ablur_kernel : Int = 9,an_clusters:Int = 8, amin_area :Int = 500, apoly_epsilon:Int = 10) {
    var n_clusters = an_clusters
    var blur_kernel = ablur_kernel
    var min_area = amin_area
    var poly_epsilon = apoly_epsilon

    fun processImage(face:Bitmap):Bitmap {
        val imageMat = Mat()
        Utils.bitmapToMat(face, imageMat)
        val (centers,labels) = kmeansProcess(imageMat)
        var value : DoubleArray;
        value = DoubleArray(3)
//        val s :DoubleArray = labels.get(1,0)
//        Log.d("KClusters","Label Array | "+ s.get(0).toString())
//        Log.d("KClusters","Index "+0.toString()+" | ValIndex"+labels.get(1,0).toString())
        var r : Int = 0
        for(i:Int in 0 until imageMat.rows()){
            for(j:Int in 0 until imageMat.cols()){
                var valindex : Int = labels.get(r,0).get(0).toInt()
                value.set(0,centers.get(valindex,0).get(0)*255)
                value.set(1,centers.get(valindex,1).get(0)*255)
                value.set(2,centers.get(valindex,2).get(0)*255)
                imageMat.put(i,j,value.get(0),value.get(1),value.get(2))
                r++
            }
        }
        val processedFace : Bitmap = face.copy(face.config, true)
        Utils.matToBitmap(imageMat, processedFace)
        return processedFace
    }
    fun kmeansProcess(imageMat:Mat):Pair<Mat,Mat> {
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGRA2BGR)
        val reshaped_image: Mat = imageMat.reshape(1, imageMat.cols() * imageMat.rows())
        val reshaped_image32f = Mat()
        reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0)

        val labels = Mat()
        val criteria = TermCriteria(TermCriteria.COUNT, 100, 1.toDouble())
        val centers = Mat()
        val clusterCount = 5
        val attempts = 1
        Log.d("Kmeans","Clustering with Cluster Size "+n_clusters.toString())
        Core.kmeans(
            reshaped_image32f,
            n_clusters,
            labels,
            criteria,
            attempts,
            Core.KMEANS_PP_CENTERS,
            centers
        )
//        Log.d("KClusters","Labels Size | "+labels.size().toString())
//        Log.d("KClusters","Centers Size | "+centers.size().toString())
//        Log.d("KClusters","Flattened Image | "+reshaped_image.size().height.toString())
//        Log.d("KClusters","Image | "+imageMat.size().toString())

//        reshaped_image= imageMat.reshape(1, imageMat.cols() * imageMat.rows())
        return Pair(centers,labels)
    }
    fun findContours(faceMat: Mat,centers:Mat){

    }
    //Setters
    fun setClusterSize(cluster_size:Int){
        n_clusters = cluster_size
    }
    fun setPolyEpsilon(apoly_epsilon:Int){
        poly_epsilon = apoly_epsilon
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