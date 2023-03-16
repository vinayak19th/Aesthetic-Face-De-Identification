import cv2
from sklearn.cluster import KMeans
import matplotlib.pyplot as plt
import numpy as np
from PIL import Image
from tqdm import tqdm
from matplotlib.pyplot import imshow

class ImageFilter:
    def __init__(self,blur_kernel = 9, n_clusters = 10, min_area = 100, poly_epsilon = 10,mask="./masks/mask5.png"):
        """_summary_

        Args:
            blur_kernel (int, optional): Gaussian blur before clustering. Defaults to 9.
            n_clusters (int, optional): Number of colors in filter. Defaults to 10.
            min_area (int, optional): Min area for polygon. Defaults to 100.
            poly_epsilon (int, optional): Similiarity in Contour drawing. Defaults to 10.
            mask (path, optional): Path to mask image. Defaults to 10.
        """
        self.blur_kernel = blur_kernel 
        self.n_clusters =n_clusters
        self.min_area =min_area
        self.poly_epsilon = poly_epsilon
        self.mask = cv2.imread(mask)
        self.processed_image = None
        self.vector_area = np.vectorize(self.area,signature='(n)->()')

    def area(self,bb):
        return bb[2]*bb[3]
    
    def show(self):
        im = cv2.cvtColor(self.processed_image, cv2.COLOR_BGR2RGB)
        plt.figure()
        plt.axis("off")
        plt.imshow(im)
        plt.show()
    
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
        km = KMeans(n_clusters=self.n_clusters, random_state=0)
        km.fit(im)

        counts = {}
        reps = km.cluster_centers_

        # count colors per label
        for i in range(len(im)):
            if km.labels_[i] not in counts:
                counts[km.labels_[i]] = {}
            rgb = tuple(im[i])
            if rgb not in counts[km.labels_[i]]:
                counts[km.labels_[i]][rgb] = 0
            counts[km.labels_[i]][rgb] += 1

        # remap representative to most prominent color for ea label
        for label, hist in counts.items():
            flat = sorted(hist.items(), key=lambda x: x[1], reverse=True)
            col = 0
            # # Soften Edges
            # if sum(flat[0][0])>600:
            #     reps[label] = reps[label-1]
            # else:
            reps[label] = flat[0][0]
        # print(reps)
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
        return np.add(np.multiply(orig[coordinates[1]:coordinates[1]+coordinates[3], coordinates[0]:coordinates[0]+coordinates[2]],mask),np.multiply(fitler,(1-mask)))
    
    def process_image(self,in_file,thresh:int=1):
        self.processed_image = cv2.imread(in_file)
        images = self.processed_image.copy()
        # show(im)
        faces,images = self.__get_crops(images)
        threshold= self.vector_area(faces)/thresh
        print("Threshold=",threshold)
        for i in tqdm(range(len(images))):
            if(faces[i][2]*faces[i][3] < threshold):
                im = images[i]
                #Blurring
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
                self.processed_image = self.__smooth_blend(canvas,self.processed_image, faces[i])
        self.show()