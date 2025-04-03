# GUI-Car_Control-Android
# RoboCar Control - Android App Using TCP/IP & HTTP  

This Android application allows users to connect to a Raspberry Pi and control a RoboCar remotely. The app utilizes **TCP/IP** for command transmission and **HTTP** for video streaming.  

## Features  

- **Connection to Raspberry Pi**  
  - Establishes a stable connection via **TCP/IP**.  
  - Sends control commands for movement (forward, backward, left, right, stop).  

- **Live Video Streaming**  
  - Uses **HTTP protocol** to receive real-time video feed from Raspberry Pi's camera.  

- **Secure Communication**  
  - Implements **data encryption and decryption** for enhanced security.  

## Screenshots  

### 1. RoboCar Model  
<img src="Assets/Car_RC.png" width="369" height="745">  

### 2. Connection Interface  
<img src="Assets/Connect_Interface.png" width="369" height="745">  

### 3. Data Encryption & Decryption  
<img src="Assets/EncryptDecryptData.png" width="369" height="745">  

### 4. Landscape Mode Layout  
<img src="Assets/Landscape_Layout.png" width="369" height="745">  

### 5. Portrait Mode Layout  
<img src="Assets/Portrait_layout.png" width="369" height="745">  

### 6. System Schematic Diagram  
<img src="Assets/schematic_Diagram_AppAndroid_RoboCar.png" width="800">  

## Technologies Used  

- **Java/XML** – UI and application logic.  
- **TCP/IP Protocol** – Communication between Android app and Raspberry Pi.  
- **HTTP Protocol** – Real-time video streaming.  
- **Encryption/Decryption** – Secure data exchange.  
- **Android Studio** – Development environment.  

## Installation  

1. Clone the repository:  
   ```sh
   git clone https://github.com/crscristian/GUI-Car_Control-Android.git
