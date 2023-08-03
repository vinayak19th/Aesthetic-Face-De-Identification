package com.group.nine.camerafilter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import java.io.FileNotFoundException
import java.io.InputStream


class ImageFilter(context: Context,an_clusters:Int = 8, amin_area :Double = 100.0, apoly_epsilon:Double = 10.0) {
    var n_clusters = an_clusters
    var min_area = amin_area
    var poly_epsilon = apoly_epsilon
    val maskMat : Mat = loadMask(context)
    fun loadMask(context: Context):Mat{
        Log.d("MaskLoad","Loading Mask")
        var maskMat : Mat = Mat()
        try {
            val inStream: InputStream = context.assets.open("mask.png")
            val buffer = inStream.readBytes()
            maskMat = Imgcodecs.imdecode(MatOfByte(*buffer),Imgcodecs.IMREAD_GRAYSCALE)
        } catch (e:FileNotFoundException){
            println(e)
            null
        }
        maskMat.convertTo(maskMat, CvType.CV_32FC1, 1.0 / 255.0)
//        val maskMat3channel = Mat(maskMat.size(), CvType.CV_32FC3)
//        val channels = mutableListOf(maskMat, maskMat, maskMat)
//        Core.merge(channels, maskMat3channel)
//        channels.forEach {
//            it.release()
//        }
//        maskMat.release()
        return maskMat
    }

    fun processImage(face:Bitmap):Bitmap {
        var imageMat = Mat()
        Utils.bitmapToMat(face, imageMat)
        var filterMat : Mat = Mat()
        imageMat.copyTo(filterMat)
        var downsampled: Boolean = false;
        Log.d("Main","Image Width: "+filterMat.width().toString()+"| Image Height: "+filterMat.height().toString())
        val face_size: Size = filterMat.size()
        if(filterMat.width()>1000 || filterMat.height()>1000) {
            Imgproc.pyrDown(filterMat, filterMat, Size(filterMat.cols() / 2.0, filterMat.rows() / 2.0))
            downsampled = true
            Log.d("Main","Downsampling")
        }
        val (centers,labels) = kmeansProcess(filterMat)
        Core.multiply(centers,Scalar(255.0),centers)
        filterMat = reColorImage(filterMat,centers,labels)
//        BGR - filterMat
        labels.release()
        Log.d("Main","Calling Cont function")
        filterMat = getContours(filterMat,centers)
        //        BGR - filterMat
        centers.release()
        Imgproc.cvtColor(imageMat,imageMat,Imgproc.COLOR_RGBA2RGB)
        Log.d("Main","imageMat type"+imageMat.toString())
//        filterMat = inPaint(filterMat)
        if(downsampled){
            Imgproc.pyrUp(filterMat,filterMat,Size(filterMat.cols() * 2.0, filterMat.rows() * 2.0))
            Imgproc.resize(filterMat,filterMat,face_size)
        }
        imageMat = blendImages(filterMat,imageMat)
        filterMat.release()
        val processedFace : Bitmap = face.copy(face.config, true)
        Utils.matToBitmap(imageMat, processedFace)
        imageMat.release()
        return processedFace
    }
    fun kmeansProcess(filterMat:Mat):Pair<Mat,Mat> {
        Imgproc.cvtColor(filterMat, filterMat, Imgproc.COLOR_BGRA2BGR)
        val reshaped_image: Mat = filterMat.reshape(1, filterMat.cols() * filterMat.rows())
        val reshaped_image32f = Mat()
        reshaped_image.convertTo(reshaped_image32f, CvType.CV_32F, 1.0 / 255.0)
        reshaped_image.release()
        val labels = Mat()
        val criteria = TermCriteria(TermCriteria.COUNT, 100, 1.toDouble())
        val centers = Mat()
        val attempts = 3
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
//        Log.d("KClusters","Image | "+filterMat.size().toString())

//        reshaped_image= filterMat.reshape(1, filterMat.cols() * filterMat.rows())
        return Pair(centers,labels)
    }

    fun reColorImage(filterMat: Mat,centers: Mat,labels:Mat):Mat{
        val value : DoubleArray = DoubleArray(3)
        var r : Int = 0
        for(i:Int in 0 until filterMat.rows()){
            for(j:Int in 0 until filterMat.cols()){
                var valindex : Int = labels.get(r,0).get(0).toInt()
                value.set(0,centers.get(valindex,0).get(0))
                value.set(1,centers.get(valindex,1).get(0))
                value.set(2,centers.get(valindex,2).get(0))
                filterMat.put(i,j,value.get(0),value.get(1),value.get(2))
                r++
            }
        }
        return filterMat
    }
    fun inPaint(filterMat: Mat):Mat{
        Log.d("Blend","Starting In Paint")
        var paintMask : Mat = Mat()
        Core.inRange(filterMat, Scalar(0.0, 0.0, 0.0),Scalar(5.0, 5.0, 5.0),paintMask)
        Photo.inpaint(filterMat,paintMask,filterMat,1.0,Photo.INPAINT_NS)
        //        cv2.inpaint(fitler, paint_mask, 1, flags=cv2.INPAINT_TELEA)
        paintMask.release()
        return filterMat
    }
    fun printMatToLogcat(tag: String, mat: Mat) {
        val rows = mat.rows()
        val cols = mat.cols()
        val channels = mat.channels()

        val buffer = FloatArray(rows * cols * channels)
        mat.get(0, 0, buffer)

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val index = (i * cols + j) * channels
                val pixel = buffer.slice(index until index + channels).joinToString(", ")
                Log.d(tag, "Pixel ($i, $j): $pixel")
            }
        }
    }
    fun blendImages(filterMat: Mat,imageMat: Mat):Mat{
        filterMat.convertTo(filterMat, CvType.CV_32FC3)
        imageMat.convertTo(imageMat, CvType.CV_32FC3)

        var maskSized : Mat = Mat()
        Imgproc.resize(maskMat,maskSized,imageMat.size())
        maskSized.convertTo(maskSized,CvType.CV_32FC1)
        val  sourceSplit : List<Mat> = ArrayList(3)
        val  filterSplit : List<Mat> = ArrayList(3)
        Core.split(imageMat,sourceSplit)
        Core.split(filterMat,filterSplit)
        Log.d("Blend","Split Mats")
        var maskInverted : Mat = Mat()
        maskInverted.convertTo(maskInverted, CvType.CV_32FC1)
        Core.absdiff(maskSized,Scalar(1.0),maskInverted)
//        Log.d("Blend","maskSized Size "+maskSized.size()+"| Channels:"+maskSized.channels()+"| type:"+maskSized.type())
//        Log.d("Blend","maskInverted Size "+maskInverted.size()+"| Channels:"+maskInverted.channels()+"| type:"+maskInverted.type())
        Log.d("Blend","Subtracted to create maskinverted")
        sourceSplit.forEach {
            Core.multiply(it,maskSized,it)
        }
        filterSplit.forEach {
            Core.multiply(it,maskInverted,it)
        }
        Log.d("Blend","Masks multiplied")
        Core.merge(sourceSplit,maskSized)
        Core.merge(filterSplit,maskInverted)
        Log.d("Blend","Merged images")

        Log.d("Blend","maskSized Size "+maskSized.size()+"| Channels:"+maskSized.channels()+"| type:"+maskSized.toString())
        Log.d("Blend","maskInverted Size "+maskInverted.size()+"| Channels:"+maskInverted.channels()+"| type:"+maskInverted.toString())

        Core.add(maskSized,maskInverted,imageMat)
        Log.d("Blend","Releasing resources")
        maskInverted.release()
        maskSized.release()
        sourceSplit.forEach {
            it.release()
        }
        filterSplit.forEach {
            it.release()
        }
        Log.d("Blend","Returning result")
        imageMat.convertTo(imageMat,CvType.CV_8UC3)
        return imageMat
    }

    fun blendImages2(filterMat: Mat,imageMat: Mat):Mat{
        Log.d("Blend","Starting Blend")
////        Imgproc.cvtColor(filterMat,filterMat,Imgproc.COLOR_RGBA2RGB)
        filterMat.convertTo(filterMat, maskMat.type())
////        Imgproc.cvtColor(imageMat,imageMat,Imgproc.COLOR_RGBA2RGB)
        imageMat.convertTo(imageMat, maskMat.type())

        Log.d("Blend","Filter Size "+filterMat.size()+"| Channels:"+filterMat.channels()+"| Type:"+CvType.depth(filterMat.type()))
        Log.d("Blend","Input Size "+imageMat.size()+"| Channels:"+imageMat.channels()+"| Type:"+CvType.depth(imageMat.type()))
        Log.d("Blend","Mask Size "+maskMat.size()+"| Channels:"+maskMat.channels()+"| Type:"+CvType.depth(maskMat.type()))

        var maskSized : Mat = Mat()
        Imgproc.resize(maskMat,maskSized,imageMat.size())
        maskMat.convertTo(maskMat, maskMat.type())
//        printMatToLogcat("maskedMat",maskSized)
        Log.d("Blend","maskSized Size "+maskSized.size()+"| Channels:"+maskSized.channels())
        Core.multiply(imageMat,maskSized,imageMat)

        var maskInverted : Mat = maskSized.clone()
        Core.subtract(Mat.ones(maskSized.size(),maskSized.type()),maskSized,maskInverted)
        Log.d("Blend","maskInverted Size "+maskInverted.size()+"| Channels:"+maskInverted.channels())
        Core.multiply(filterMat,maskInverted,filterMat)

        maskInverted.release()
        maskSized.release()
        Core.add(imageMat,filterMat,imageMat)

        Log.d("Blend","Returning result")
        imageMat.convertTo(imageMat,CvType.CV_8UC3)
        return imageMat
    }

    fun getContours(filterMat: Mat,centers:Mat): Mat {
        var mask : Mat = Mat()
        var upperB: Mat = Mat()
        var lowerB: Mat = Mat()
        Log.d("ContFunct","Finding Contours")
        val contoursFinal: MutableList<Pair<Pair<Double,Int>,MatOfPoint2f>> = ArrayList()
        for (i : Int in 0 until centers.rows()){
            Log.d("ContFunct","Looking at "+i.toString())
            Core.absdiff(centers.row(i),Scalar(1.0,1.0,1.0),lowerB)
            Core.add(centers.row(i),Scalar(1.0,1.0,1.0),upperB)
            Core.inRange(filterMat, Scalar(lowerB[0,0][0],lowerB[0,1][0],lowerB[0,2][0]),Scalar(upperB[0,0][0],upperB[0,1][0],upperB[0,2][0]),mask)
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
        return drawFinal(filterMat,centers,contoursFinal)
    }
    fun drawFinal(filterMat: Mat,centers: Mat ,contours: MutableList<Pair<Pair<Double,Int>,MatOfPoint2f>> ):Mat{
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
            Imgproc.drawContours(filterMat,arrayListOf(approx2),-1, Scalar(row[0,0][0],row[0,1][0],row[0,2][0]),-1)
        }
        Log.d("drawFinal","Finished Contour drawing")
        return filterMat
    }
    //Setters
    fun setClusterSize(cluster_size:Int){
        n_clusters = cluster_size
    }
    fun setPolyEpsilon(apoly_epsilon:Double){
        poly_epsilon = apoly_epsilon
    }
    fun setMinArea(amin_area: Double){
        min_area = amin_area
    }
}
