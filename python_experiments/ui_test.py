from tkinter import *
from tkinter import filedialog
from PIL import ImageTk, Image

root = Tk()
root.title('Image Loader')
root.geometry("800x600")

def resize_image(event):
    new_width = event.width
    new_height = event.height - button.winfo_height()
    img = Image.open(filename)
    img_ratio = img.width / img.height
    new_ratio = new_width / new_height
    if new_ratio > img_ratio:
        new_width = int(new_height * img_ratio)
    else:
        new_height = int(new_width / img_ratio)
    img = img.resize((new_width, new_height), Image.ANTIALIAS)
    img = ImageTk.PhotoImage(img)
    panel.configure(image=img)
    panel.image = img

def open_image():
    global filename
    filename = filedialog.askopenfilename(initialdir="./", title="Select an image", filetypes=(("jpeg files", "*.jpg"), ("png files", "*.png"), ("all files", "*.*")))
    resize_image(root)

def update_variable(val):
    global variable
    variable = int(val)

frame = Frame(root)
frame.pack(side="left", fill="both", expand="yes")

panel = Label(frame)
panel.pack(side="top", fill="both", expand="yes")

button = Button(frame, text="Open Image", command=open_image)
button.pack(side="bottom")

variable = 10
slider = Scale(root, from_=10, to=500, orient=VERTICAL, resolution=10, command=update_variable)
slider.set(variable)
slider.pack(side="right")

filename = ""
root.bind("<Configure>", resize_image)

root.mainloop()