import sys
from PyQt5.QtWidgets import QApplication, QMessageBox

def window():
    app = QApplication(sys.argv)
    msg = QMessageBox()
    msg.setIcon(QMessageBox.Critical)
    msg.setText("Error")
    msg.setInformativeText("This is an error message.")
    msg.setWindowTitle("Error")
    msg.setDefaultButton(QMessageBox.Ok)
    msg.setDetailedText("Extra details.....")
    # msg.setFixedSize(900, 700)
    msg.exec_()

if __name__ == '__main__': 
    window()