# Scalable Aesthetic-Preserving Face De-Identification

<b><i>Tech Stack</i></b>
<p align="left">
<img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/android/android-original-wordmark.svg" alt="android" width="40" height="40"/> 
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/androidstudio/androidstudio-original-wordmark.svg" width="50" height="50"/>
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/kotlin/kotlin-original-wordmark.svg" width="50" height="50" />
<img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/opencv/opencv-original-wordmark.svg" width="40" height="40"/>
</p>
   
## Build Instructions
### 1. <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/opencv/opencv-original.svg" width="20" height="20" /> OpenCV 
Need to set up OpenCV yourself, here are the instructions [link](https://philipplies.medium.com/setting-up-latest-opencv-for-android-studio-and-kotlin-2021-edition-259be404b133)
          
### 2. <img src="https://cdn.jsdelivr.net/gh/devicons/devicon/icons/androidstudio/androidstudio-original.svg" width="20" height="20" /> Android Compile 
You can use the standard Android studio build methods to run the application on a phone or the emulator. **Instructions** can be found [here](https://developer.android.com/studio/run).

## App Description:

### 1. <img src="https://raw.githubusercontent.com/FortAwesome/Font-Awesome/6.x/svgs/solid/mobile.svg" width="20" height="20"> App UI

<table>
    <tbody>
    <tr>
        <td rowspan=5 style="border:0px;">
            <img src="./readme_images/UI.png"><br>
            <i>Image Process Screen</i>
        </td>
         <td>
            <b>Button Set</b>: There is a set of 3 buttons which from top to button have the following uses:
            <ul>
                <li><b>Save</b>: Saves the filtered iamge to the gallery</li>
                <li><b>Process</b>: Processes the Image and de-identifies the faces</li>
                <li><b>Back</b>: Goes to previous screen</li>
            </ul>
        </td>
    </tr>
    <tr>
        <td><b>Faces Detected</b> : Will display the number of faces detected in the image</td>
    </tr>
    <tr>
        <td>
            <b>Threshhold Scale</b> : Correspondes to the threshhold value used in the adaptive thresholding algorithm.<br>The higher the value, the fewer faces will be de-identified. This is due to the threshhold value being lowered as it is divided by the scale value.
        </td>
    </tr>    
    <tr>
        <td>
            <b>Cluser Size</b> : Defines the number of clusters or the number of colors the images is quantized to. The lower the value the more colors in the image. 
        </td>
    </tr>
    <tr>
        <td>
            <b>Face Detail</b> : Defines the "approximation accuracy" or the maximum distance between the original curve and its approximation.<br>
            You can read more <a href="https://docs.opencv.org/3.4/dc/dcf/tutorial_js_contour_features.html">here</a>
        </td>
    </tr>
    </tbody>
</table>

### 2. Compilation demo of our application
Watch the video [link](https://drive.google.com/file/d/1YOQFlHQubktzrKQH43BQKHww-Pqae8wl/view?usp=sharing)
