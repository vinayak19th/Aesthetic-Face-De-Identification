import sys
from PyQt6 import QtWidgets
from PyQt6.QtWidgets import QFileDialog, QLabel, QPushButton, QSlider,QVBoxLayout, QHBoxLayout, QGridLayout, QProgressBar
from PyQt6.QtGui import QPixmap,QImage
from PyQt6.QtCore import Qt
from PyQt6.QtWidgets import QMessageBox
import cv2
from process_image import ImageFilter

class Ui(QtWidgets.QMainWindow):
    def __init__(self):
        super(Ui, self).__init__()

        # self.error_msg = self.create_error_box()

        self.image_filter = ImageFilter()

        self.image = None
        self.image_processed = None
        self.imageLoaded = False

        # Resize window and label to fit desired size.
        window_width = 1280 
        window_height = 720
        
        self.centralWidget = QtWidgets.QWidget(self)
        self.setCentralWidget(self.centralWidget)

        self.loadBtn = QPushButton('Load Image', self)
        self.loadBtn.clicked.connect(self.loadImage)

        self.processBtn = QPushButton('processImage', self)
        self.processBtn.clicked.connect(self.processImage)

        self.saveBtn = QPushButton('Save Image', self)
        self.saveBtn.clicked.connect(self.saveImage)

        uiBox = QVBoxLayout()
        button_box = QHBoxLayout()
        button_box.addWidget(self.loadBtn)
        button_box.addWidget(self.processBtn)
        button_box.addWidget(self.saveBtn)

        button_box.setContentsMargins(10,0,10,0)       
        button_box.setSpacing(10) 
        uiBox.addLayout(button_box, stretch=1)

        meta_data_box = QGridLayout()
        self.sliders = {} 
        self.slider_labels = {}

        self.meta_data = {}
        for i,label in enumerate(["Faces Detected","Threshold Area"]):
            meta_label = QLabel(label,self)
            meta_label.adjustSize()
            meta_label.setFixedSize(meta_label.size())
            meta_data_box.addWidget(meta_label,i,0) 

            self.meta_data[label] = QLabel("0",self)
            self.meta_data[label].adjustSize()
            self.meta_data[label].setBaseSize(self.meta_data[label].size())
            meta_data_box.addWidget(self.meta_data[label] ,i,1) 

        # self.meta_data['progbar'] = QProgressBar(self)
        meta_data_box.setSpacing(20)
        meta_data_box.setContentsMargins(0,0,0,0)
        uiBox.addLayout(meta_data_box, stretch=1)
        
        slider_box=  QGridLayout()
        ranges = {"n_clusters":(1,20,1),'min_area':(5,500,5),'poly_epsilon':(1,25,1),'threshold_factor':(1,7,1)}
        for i,label in enumerate(['n_clusters','min_area','poly_epsilon','threshold_factor']):
            temp_label = QLabel(label)
            self.slider_labels[label] = QLabel('{}'.format(ranges[label][0]))
            self.sliders[label] = QSlider(Qt.Orientation.Horizontal,self)
            self.sliders[label].setMinimum(ranges[label][0])
            self.sliders[label].setMaximum(ranges[label][1])
            self.sliders[label].setSingleStep(ranges[label][2])
            slider_box.addWidget(temp_label,i,0)
            slider_box.addWidget(self.sliders[label],i,1)
            slider_box.addWidget(self.slider_labels[label],i,2)
            i+=1
        del temp_label
        del ranges
        
        self.sliders['n_clusters'].setValue(self.image_filter.n_clusters)
        self.slider_labels['n_clusters'].setNum(self.image_filter.n_clusters)
        self.sliders['n_clusters'].valueChanged.connect(self.update_clusers)

        self.sliders['min_area'].setValue(self.image_filter.min_area)
        self.slider_labels['min_area'].setNum(self.image_filter.min_area)
        self.sliders['min_area'].valueChanged.connect(self.update_min_area)
        
        self.sliders['poly_epsilon'].setValue(self.image_filter.poly_epsilon)
        self.slider_labels['poly_epsilon'].setNum(self.image_filter.poly_epsilon)
        self.sliders['poly_epsilon'].valueChanged.connect(self.update_poly_epsilon)

        self.sliders['threshold_factor'].setValue(self.image_filter.threshold_factor)
        self.slider_labels['threshold_factor'].setNum(self.image_filter.threshold_factor)
        self.sliders['threshold_factor'].valueChanged.connect(self.update_threshold_factor)

        slider_box.setSpacing(50)
        # slider_box.setContentsMargins(0,0,0,0)
        uiBox.addLayout(slider_box, stretch=1)
        uiBox.setSpacing(0)
        hbox = QHBoxLayout()
        
        # Add label to left side of horizontal layout.
        self.label = QLabel(self) 
        self.label.setFixedSize(window_width // 2 ,window_height)
        hbox.addWidget(self.label) 
        # Add vertical layout with buttons and sliders to right side of horizontal layout.
        hbox.addLayout(uiBox) 
        # Set central widget layout to horizontal layout.
        hbox.setSpacing(50)
        hbox.setContentsMargins(20,0,20,0)
        
        self.centralWidget.setLayout(hbox) 
        self.resize(window_width, window_height)


    # def create_error_box(self):
    #     msg = QMessageBox()
    #     msg.setIcon(QMessageBox.warning)
    #     msg.setText("This is a message box")
    #     msg.setInformativeText("This is additional information")
    #     msg.setWindowTitle("MessageBox demo")
    #     msg.setDetailedText("The details are as follows:")
    #     msg.setStandardButtons(QMessageBox.Ok)
    #     return msg
    
    def loadImage(self):
        fileName, _ = QFileDialog.getOpenFileName(None,"Select Image", "","Image Files (*.png *.jpg)")
        if fileName:
            print("Found image")
            print(self.label.size())
            pixmap = QPixmap(fileName).scaled(self.label.size(), Qt.AspectRatioMode.KeepAspectRatio,
                                            Qt.TransformationMode.SmoothTransformation) 
            # Scale image to fit within label while maintaining aspect ratio
            # and using smooth transformation for better quality.
            self.label.setPixmap(pixmap)
            self.image = cv2.imread(filename=fileName)
            self.imageLoaded = True
        
    def saveImage(self):
        if self.imageLoaded:
            fileName, _ = QFileDialog.getSaveFileName(
                None,
                "Save Image",
                "",
                "Images (*.png *.xpm *.jpg)"
            )
            cv2.imwrite(fileName,self.image_processed)
        else:
            self.error_msg.setText("FUCK OFF")
            print("Fuck Off")

    def processImage(self):
        if self.imageLoaded:   
            self.image_processed = self.image_filter.process_image_array(self.image,self.meta_data) 
            im = cv2.cvtColor(self.image_processed, cv2.COLOR_BGR2RGB)
            height, width, _ = im.shape
            bytesPerLine = 3 * width
            qImg = QImage(im.data, width, height, bytesPerLine, QImage.Format.Format_RGB888)
            
            pixmap = QPixmap(qImg).scaled(self.label.size(), Qt.AspectRatioMode.KeepAspectRatio,
                                            Qt.TransformationMode.SmoothTransformation) 
            # Scale image to fit within label while maintaining aspect ratio
            # and using smooth transformation for better quality.
            self.label.setPixmap(pixmap)
        else:
            print("Fuck Off")

    def update_clusers(self,value):
        self.slider_labels['n_clusters'].setNum(value)
        self.image_filter.n_clusters=value
    
    def update_min_area(self,value):
        self.slider_labels['min_area'].setNum(value)
        self.image_filter.min_area=value
    
    def update_poly_epsilon(self,value):
        self.slider_labels['poly_epsilon'].setNum(value)
        self.image_filter.poly_epsilon=value
    
    def update_threshold_factor(self,value):
        self.slider_labels['threshold_factor'].setNum(value)
        self.image_filter.threshold_factor=value

if __name__ == '__main__':
    app = QtWidgets.QApplication(sys.argv)
    win = Ui()
    win.show()
    sys.exit(app.exec())

