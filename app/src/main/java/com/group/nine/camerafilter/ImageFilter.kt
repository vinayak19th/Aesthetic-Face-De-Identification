package com.group.nine.camerafilter

import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.ml.SVM


class ImageFilter(ablur_kernel : Int = 9,an_clusters:Int = 8, amin_area :Double = 100.0, apoly_epsilon:Double = 10.0) {
    var n_clusters = an_clusters
    var blur_kernel = ablur_kernel
    var min_area = amin_area
    var poly_epsilon = apoly_epsilon

    fun processImage(face:Bitmap):Bitmap {
        var imageMat = Mat()
        Utils.bitmapToMat(face, imageMat)
        val (centers,labels) = kmeansProcess(imageMat)
        Core.multiply(centers,Scalar(255.0),centers)
        imageMat = reColorImage(imageMat,centers,labels)
        labels.release()
        Log.d("Main","Calling Cont function")
        imageMat = getContours(imageMat,centers)
        val processedFace : Bitmap = face.copy(face.config, true)
        Utils.matToBitmap(imageMat, processedFace)
        return processedFace
    }
    fun kmeansProcess(imageMat:Mat):Pair<Mat,Mat> {
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGRA2BGR)
        val reshaped_image: Mat = imageMat.reshape(1, imageMat.cols() * imageMat.rows())
        val reshaped_image32f = Mat()
        reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0)
        reshaped_image.release()
        val labels = Mat()
        val criteria = TermCriteria(TermCriteria.COUNT, 100, 1.toDouble())
        val centers = Mat()
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
        reshaped_image32f.release()
//        Log.d("KClusters","Labels Size | "+labels.size().toString())
//        Log.d("KClusters","Centers Size | "+centers.size().toString())
//        Log.d("KClusters","Flattened Image | "+reshaped_image.size().height.toString())
//        Log.d("KClusters","Image | "+imageMat.size().toString())

//        reshaped_image= imageMat.reshape(1, imageMat.cols() * imageMat.rows())
        return Pair(centers,labels)
    }

    fun reColorImage(imageMat: Mat,centers: Mat,labels:Mat):Mat{
        var value : DoubleArray = DoubleArray(3)
        var r : Int = 0
        for(i:Int in 0 until imageMat.rows()){
            for(j:Int in 0 until imageMat.cols()){
                var valindex : Int = labels.get(r,0).get(0).toInt()
                value.set(0,centers.get(valindex,0).get(0))
                value.set(1,centers.get(valindex,1).get(0))
                value.set(2,centers.get(valindex,2).get(0))
                imageMat.put(i,j,value.get(0),value.get(1),value.get(2))
                r++
            }
        }
        return imageMat
    }

    fun getContours(imageMat: Mat,centers:Mat): Mat {
        var mask : Mat = Mat()
        var upperB: Mat = Mat()
        var lowerB: Mat = Mat()
        Log.d("ContFunct","Finding Contours")
        val contoursFinal: MutableList<Pair<Pair<Double,Int>,MatOfPoint2f>> = ArrayList()
        for (i : Int in 0 until centers.rows()){
            Log.d("ContFunct","Looking at "+i.toString())
            Core.absdiff(centers.row(i),Scalar(1.0,1.0,1.0),lowerB)
            Core.add(centers.row(i),Scalar(1.0,1.0,1.0),upperB)
            Core.inRange(imageMat, Scalar(lowerB[0,0][0],lowerB[0,1][0],lowerB[0,2][0]),Scalar(upperB[0,0][0],upperB[0,1][0],upperB[0,2][0]),mask)
            val contours: List<MatOfPoint> = ArrayList()
            val hierarchey = Mat()
            Imgproc.findContours(mask,contours,hierarchey,Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_NONE)
            hierarchey.release()
//            Log.d("ContFunct","Counts "+contours.size.toString())
            contours.forEach{cont ->
                val area : Double = Imgproc.contourArea(cont)
//                Log.d("ContFunct","Area = "+area.toString())
                if(area>=min_area){
                    val temp = MatOfPoint2f()
                    cont.convertTo(temp, CvType.CV_32FC1)
                    cont.release()
                    contoursFinal.add(Pair(Pair(area,i),temp))
//                    temp.release()
                }
            }
        }
        contoursFinal.sortBy {
            it.first.first
        }
        mask.release()
        upperB.release()
        lowerB.release()
        return drawFinal(imageMat,centers,contoursFinal)
    }
    fun drawFinal(imageMat: Mat,centers: Mat ,contours: MutableList<Pair<Pair<Double,Int>,MatOfPoint2f>> ):Mat{
        var approx: MatOfPoint2f = MatOfPoint2f()
        approx.convertTo(approx, CvType.CV_32FC1)
        var approx2: MatOfPoint = MatOfPoint()
        Log.d("drawFinal","Started Contour drawing")
        contours.forEach{dataPoint->
            Log.d("drawFinal","n_points:"+dataPoint.second.size())
            Imgproc.approxPolyDP(dataPoint.second,approx,poly_epsilon,true)
            dataPoint.second.release()
            approx.convertTo(approx2,CvType.CV_32S)
            approx.release()
            val row = centers.row(dataPoint.first.second)
            Log.d("drawFinal","Contour row Size:"+row.size())
            Imgproc.drawContours(imageMat,arrayListOf(approx2),-1, Scalar(row[0,0][0],row[0,1][0],row[0,2][0]),-1)
        }
        Log.d("drawFinal","Finished Contour drawing")
        return imageMat
    }
    //Setters
    fun setClusterSize(cluster_size:Int){
        n_clusters = cluster_size
    }
    fun setPolyEpsilon(apoly_epsilon:Double){
        poly_epsilon = apoly_epsilon
    }
}

//def find_contours(im, reps, min_area):
//contours = []
//for rep in reps:
//mask = cv2.inRange(im, rep-1, rep+1)
//# show(mask)
//conts, _ = cv2.findContours(
//mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
//for cont in conts:
    //area = cv2.contourArea(cont)
    //if area >= min_area:
        //contours.append((area, cont, rep.tolist()))
//contours.sort(key=lambda x: x[0], reverse=True)
//return contours

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