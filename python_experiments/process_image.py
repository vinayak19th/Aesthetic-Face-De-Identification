import cv2
from sklearn.cluster import KMeans
import matplotlib.pyplot as plt
import numpy as np
from PIL import Image
from matplotlib.pyplot import imshow

class ImageFilter:
    def __init__(self,blur_kernel = 9, n_clusters = 10, min_area = 100, poly_epsilon = 10, quiet = False, final_blur = False):
        """_summary_

        Args:
            blur_kernel (int, optional): gaussian blur before clustering. Defaults to 9.
            n_clusters (int, optional): _description_. Defaults to 10.
            min_area (int, optional): _description_. Defaults to 100.
            poly_epsilon (int, optional): _description_. Defaults to 10.
            quiet (bool, optional): _description_. Defaults to False.
            final_blur (bool, optional): _description_. Defaults to False.
        """
