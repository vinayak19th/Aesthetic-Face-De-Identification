import sys
import traceback

import cv2
import matplotlib.pyplot as plt
import numpy as np
from PyQt6.QtCore import QObject, QRunnable, pyqtSignal, pyqtSlot
from sklearn.cluster import MiniBatchKMeans


class WorkerSignals(QObject):
    '''
    Defines the signals available from a running worker thread.

    Supported signals are:

    finished
        No data

    error
        tuple (exctype, value, traceback.format_exc() )

    result
        object data returned from processing, anything

    progress
        int indicating % progress

    '''
    finished = pyqtSignal()
    final_image = pyqtSignal(np.ndarray)
    meta_data = pyqtSignal(tuple)
    error = pyqtSignal(str)
    progress = pyqtSignal(int)
    progress_max = pyqtSignal(float)

class Worker(QRunnable):
    '''
    Worker thread

    Inherits from QRunnable to handler worker thread setup, signals and wrap-up.

    :param callback: The function callback to run on this worker thread. Supplied args and
                    kwargs will be passed through to the runner.
    :type callback: function
    :param args: Arguments to pass to the callback function
    :param kwargs: Keywords to pass to the callback function

    '''

    def __init__(self, fn, *args, **kwargs):
        super(Worker, self).__init__()

        # Store constructor arguments (re-used for processing)
        self.fn = fn
        self.args = args
        self.kwargs = kwargs
        self.signals = WorkerSignals()

        # Add the callback to our kwargs
        self.kwargs['progress'] = self.signals.progress
        self.kwargs['meta_data'] = self.signals.meta_data

    @pyqtSlot(np.ndarray)
    def run(self):
        '''
        Initialise the runner function with passed args, kwargs.
        '''

        # Retrieve args/kwargs here; and fire processing using them
        try:
            print("Running run")
            result = self.fn(*self.args, **self.kwargs)
        except:
            traceback.print_exc()
            exctype, value = sys.exc_info()[:2]
            self.signals.error.emit("{}\n{}\n{}".format(str(exctype), str(value), str(traceback.format_exc())))
        else:
            self.signals.final_image.emit(result)  # Return the result of the processing
        finally:
            self.signals.finished.emit()  # Done

class ImageFilter():
    _signal = pyqtSignal(int)
    def __init__(self,blur_kernel = 19, n_clusters = 6, min_area = 200, poly_epsilon = 13,mask="./mask5.png",thresh:int=1):
        """_summary_

        Args:
            blur_kernel (int, optional): Gaussian blur before clustering. Defaults to 9.
            n_clusters (int, optional): Number of colors in filter. Defaults to 10.
            min_area (int, optional): Min area for polygon. Defaults to 100.
            poly_epsilon (int, optional): Similiarity in Contour drawing. Defaults to 10.
            mask (path, optional): Path to mask image. Defaults to 10.
        """
        super(ImageFilter, self).__init__()
        self.blur_kernel = blur_kernel 
        self.n_clusters =n_clusters
        self.min_area =min_area
        self.poly_epsilon = poly_epsilon
        self.mask = cv2.imread(mask)
        self.threshold_factor=thresh
        self.image = None
        self.vector_area = np.vectorize(self.area,signature='(n)->()')
        
    def area(self,bb):
        return bb[2]*bb[3]
    
    def __get_crops(self,image):
        face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
        gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        faces = face_cascade.detectMultiScale(gray, 1.1, 4)
        crop_images = []
        for (x, y, w, h) in faces:
            crop_images.append(image[y:y+h, x:x+w])
        return faces , crop_images
    
    def __cluster(self,im):
        im = im.reshape((im.shape[0] * im.shape[1], 3))
        km = MiniBatchKMeans(n_clusters=n_clusters,batch_size=512, random_state=0,max_iter=50)
        km.fit(im)
        return km.cluster_centers_, km.labels_
    
    def __remap_colors(self,im, reps, labels):
        orig_shape = im.shape
        im = im.reshape((im.shape[0] * im.shape[1], 3))
        for i in range(len(im)):
            im[i] = reps[labels[i]]
        return im.reshape(orig_shape)
    
    def __find_contours(self,im, reps): 
        contours = []
        for rep in reps:
            mask = cv2.inRange(im, rep-1, rep+1)
            # show(mask)
            conts, _ = cv2.findContours(
                mask, cv2.RETR_TREE, cv2.CHAIN_APPROX_NONE)
            for cont in conts:
                area = cv2.contourArea(cont)
                if area >= self.min_area:
                    contours.append((area, cont, rep))
        contours.sort(key=lambda x: x[0], reverse=True)
        return contours
    
    def __black_inpaint(self,fitler:np.ndarray):
        paint_mask = cv2.inRange(fitler, (0,0,0),(5,5,5))
        return cv2.inpaint(fitler, paint_mask, 1, flags=cv2.INPAINT_TELEA)
    
    def __smooth_blend(self,fitler:np.ndarray,orig:np.ndarray,coordinates:tuple):
        """
            coordinates: (y,y+h,x,x+w)
        """
        mask= cv2.resize(self.mask,fitler.shape[:2])/255
        orig[coordinates[1]:coordinates[1]+coordinates[3], coordinates[0]:coordinates[0]+coordinates[2]] = np.add(np.multiply(orig[coordinates[1]:coordinates[1]+coordinates[3], coordinates[0]:coordinates[0]+coordinates[2]],mask),np.multiply(fitler,(1-mask)))
        return orig
    
    def process_image(self,meta_data,progress):
        print("Staring")
        images = self.image.copy()
        # show(im)
        faces,images = self.__get_crops(images)
        threshold= np.max(self.vector_area(faces))/self.threshold_factor
        meta_data.emit((len(faces),threshold))
        for i in range(len(images)):
            progress.emit(i)
            if(faces[i][2]*faces[i][3] <= threshold):
                im = images[i]
                #Blurring
                downsampled:bool = False
                if(faces[i][2]*faces[i][3] < 10000):
                    downsampled = True
                    face_size = im.shape[:2]
                    im = cv2.pyrDown(im)
                im = cv2.GaussianBlur(im,(self.blur_kernel,self.blur_kernel),0)
                #Clustering around {args.n_clusters} colors
                reps, labels = self.__cluster(im)

                #Remapping image to representative colors
                im = self.__remap_colors(im, reps, labels)
                
                #Finding contours with area gate {args.min_area}
                contours = self.__find_contours(im, reps)

                #Drawing
                canvas = np.zeros(im.shape, np.uint8)
                # show(im)
                for _, cont, rep in contours:
                    approx = cv2.approxPolyDP(cont, self.poly_epsilon, True)
                    cv2.drawContours(canvas, [approx], -1, rep, -1)
                
                canvas = self.__black_inpaint(canvas)
                if(downsampled):
                    canvas=cv2.pyrUp(canvas)
                    canvas=cv2.resize(canvas,face_size)

                self.image = self.__smooth_blend(canvas,self.image, faces[i])
        progress.emit(len(faces))
        return self.image
