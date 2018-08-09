# BLENeoPixelController
Android Application that uses BLE to control a NeoPixel strip

# Installation
Clone repository and open the directory in Android Studio.
With an android device connected to your computer, running the application will download the app to the android device.
Ensure the intended android device has MINIMUM SDK Version 23

# How to use
Once the app is open, it should immediately start scanning for a BLE device named 'Arduino'.
This scan can be changed in MainActivity.java->onScanResult to scan for a MAC Adress or UUID.

THE DEVICE MAY ACCIDENTALLY DISCONNECT. Simply re-open the app and it should re-connect. 

Once the android device is connected to the BLE device:

  Color Wheel: Tap to change the color of the NeoPixel strip
  
  Slider: Tap/drag to change the brightness of the strip
  
  Toggle Lights: Turn strip on/off
  
  Recover Values: When going in/out of the app, the NeoPixel strip will remain at its most recent state
                  (eg. the strip will stay Green & Dim even after closing the app)
                  However, upon re-entering the app, all the values on the android device will revert back to default.
                  IN ORDER TO RECOVER THE IN-APP VALUES, press the Recover Previous Values button
                  
  Scroll View: Small text output at bottom of screen used for debugging.
               THIS CAN BE REMOVED BY REMOVING THE SCROLLVIEW & TEXTVIEW OBJECTS IN ACTIVITY_MAIN.XML
               
# Contributors
Derek Kelley, Panasonic Avionics, Innovations Solutions Intern
