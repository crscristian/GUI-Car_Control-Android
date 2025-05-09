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

## Architecture  

- **MVM (Model-View-Model)**  
  - The application follows the **MVM architecture**, ensuring a clean separation of concerns between the UI (View), the application logic (Model), and the data interaction (Model). This structure promotes scalability, maintainability, and testability

## Screenshots  

### 1. RoboCar Model  
<img src="Assets/Car_RC.png" width="442" height="480">  

### 2. Connection Interface  
![Connect_Interface](Assets/Connect_Interface.png)  

### 3. Data Encryption & Decryption  
![EncryptDecryptData](Assets/EncryptDecryptData.png)  

### 4. Landscape Mode Layout  
![Landscape_Layout](Assets/Landscape_Layout.png)  

### 5. Portrait Mode Layout  
![Portrait_layout](Assets/Portrait_Layout.png)  

### 6. System Schematic Diagram  
![Schematic_Diagram_AppAndroid_RoboCar](Assets/Schematic_Diagram_AppAndroid_RoboCar.png)  

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
