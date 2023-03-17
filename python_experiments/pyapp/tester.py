import sys
from PyQt6.QtWidgets import QApplication, QMessageBox

def window():
    app = QApplication(sys.argv)
    msg = QMessageBox()
    msg.setIcon(QMessageBox.Icon.Critical)
    msg.setText("Error")
    msg.setInformativeText("This is an error message.")
    msg.setWindowTitle("Error")
    msg.setDefaultButton(QMessageBox.StandardButton.Ok)
    msg.setDetailedText("Extra details.....")
    msg.setStyleSheet("QDialogButtonBox {min-width: 400px;alignment:left} ");
    msg.exec()

if __name__ == '__main__': 
    window()