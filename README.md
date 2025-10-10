# Palm Oil Harvest Optimization App

## Overview

The Palm Oil Harvest Optimization App is an Android application designed to improve the efficiency and accuracy of palm oil fruit harvesting operations in Malaysian plantations. The app addresses critical pain points in the traditional manual harvesting process, reducing the physical strain on workers, optimizing the harvesting workflow, and improving yield management.

## Problem Statement

Traditional palm oil fruit harvesting faces several challenges:

1. **Manual Detection Strain**: Workers suffer neck strain from visually inspecting trees for ripe Fresh Fruit Bunches (FFB)
2. **Inefficient Inspection**: Only about 1 in 3 trees are ready to harvest at any given time
3. **Time-Consuming Process**: Careful inspection of each tree significantly increases harvest time
4. **Inconsistent Assessment**: Quality depends heavily on worker experience
5. **Environmental Challenges**: Tree height and poor lighting conditions affect assessment accuracy
6. **Connectivity Limitations**: GPS and internet signals are limited in plantation environments

## Solution

The app implements a drone-assisted reconnaissance and harvest planning system that works in a challenging offline-first environment, with key features:

### For Reconnaissance Teams

1. **Virtual Mapping**: Create digital maps of plantation plots with precise tree locations
2. **Drone-Assisted Reconnaissance**: Record and grade fruit bunches using HD camera drones
3. **Fruit Classification**: Document number of fruits, ripeness condition, and harvest timing
4. **Offline-First Operation**: Full functionality without internet connection
5. **Data Collection Forms**: Record tree data including fruit count and ripeness status
6. **Bulk Upload**: Synchronize collected data when internet connectivity becomes available

### For Harvester Teams

1. **Optimized Route Planning**: Navigate the most efficient path to harvest-ready trees
2. **Tree Verification**: Scan tree QR codes to confirm location and follow guided navigation
3. **Harvest Proof Collection**: Document completed harvests with photographic evidence
4. **Yield Tracking**: Monitor and verify harvesting output to minimize loss

## Technical Implementation

### Architecture

- **Android Native Application**: Written in Kotlin
- **MVVM Architecture**: Separation of UI, business logic, and data layers
- **Room Database**: Local data persistence for offline operation
- **Retrofit**: API integration for cloud synchronization
- **Camera & Location APIs**: For image capture and GPS positioning

### Key Components

1. **Database Structure**:
   - `ReconFormEntity`: Stores reconnaissance data about trees and fruits
   - `HarvesterProofEntity`: Documents completed harvests with evidence
   - `TreeLocationEntity`: Maps tree positions with coordinates

2. **User Interfaces**:
   - Reconnaissance Team Module
   - Harvester Team Module
   - Virtual Mapping System
   - Data Upload & Synchronization

## Features

### Reconnaissance Module

- **Form Collection**: Record tree ID, plot ID, fruit count, and harvest timeframe
- **Image Capture**: Take up to 3 images per tree for visual documentation
- **Form Management**: View and edit stored forms
- **Virtual Mapping**: Plot and manage tree locations on a virtual canvas
- **Data Synchronization**: Upload collected data to central database

### Harvester Module

- **Proof Collection**: Document completed harvests with images
- **Navigation**: Follow optimized routes between harvest-ready trees
- **Map Downloads**: Access reconnaissance data for assigned plot sections
- **Evidence Collection**: Record completed harvests with validation
- **Data Synchronization**: Upload harvest proofs to central database

### Virtual Map System

- **Interactive Canvas**: Touch-based tree placement with zoom and pan functionality
- **Multi-level Grid**: Adaptive grid system for precise positioning
- **GPS Integration**: Real-time positioning with accuracy visualization
- **Tree Management**: Add, edit, move, and delete tree markers
- **Malaysian Standards**: Support for standard 8.5m × 8.5m triangular plantation pattern

### Upload System

- **Network Detection**: Automatic connectivity monitoring
- **Intelligent Sync**: Track and upload unsynced forms and images
- **Progress Tracking**: Visual feedback during upload operations
- **Error Handling**: Comprehensive error reporting and recovery
- **Image Management**: Process and upload drone-captured images

## Project Setup

### Prerequisites

- Android Studio Electric Eel (2023.3.1) or newer
- JDK 11 or higher
- Android SDK 33+ (Android 13)
- Gradle 8.0+

### Configuration

1. Clone the repository:
   ```
   git clone https://github.com/username/palm_oil_backup.git
   ```

2. Create a `local.properties` file in the project root with the following properties:
   ```
   sdk.dir=/path/to/android/sdk
   VERCEL_BLOB_TOKEN=your_vercel_token
   API_KEY=your_api_key
   BACKEND_URL=your_backend_url
   ```

3. Open the project in Android Studio and sync Gradle.

4. Build the project:
   ```
   ./gradlew build
   ```

5. Run the application on a device or emulator.

### Backend Integration

The app connects to a FastAPI backend for data synchronization. The backend implementation includes:

- RESTful API endpoints for form and image uploads
- Secure token-based authentication
- Data validation and storage
- Image processing and analysis

## Usage Flow

### Reconnaissance Team Workflow

1. **Login** to the reconnaissance team module
2. **Create/Select Plot** to work on
3. **Add Tree Locations** using the virtual map
4. **Collect Tree Data** using the form feature
5. **Capture Images** of fruit bunches using drone
6. **Upload Data** when internet connectivity is available

### Harvester Team Workflow

1. **Login** to the harvester team module
2. **Download Plot Maps** for assigned sections
3. **Navigate** to harvest-ready trees following the suggested route
4. **Verify Location** by checking tree IDs
5. **Record Harvest** with proof images
6. **Upload Proofs** when internet connectivity is available

## Current Limitations

1. **Drone Integration**: Currently requires manual image transfer from drone to app
2. **Offline Duration**: Extended offline operation may lead to storage constraints
3. **Image Processing**: Image analysis is performed in the cloud, not on device
4. **Battery Optimization**: High GPS and camera usage impacts battery life

## Future Enhancements

1. **Direct Drone Integration**: Connect drones directly to the app for real-time image transfer
2. **Machine Learning**: On-device fruit ripeness detection using ML models
3. **Augmented Reality**: Overlay navigation and tree information in camera view
4. **Worker Performance Analytics**: Track and optimize individual worker efficiency
5. **Yield Prediction**: Implement predictive analytics for harvest planning
6. **Multi-Language Support**: Add support for local languages
7. **Cross-Platform Support**: Develop iOS version for broader accessibility

## License

© 2025 Palm Oil Project Team. All Rights Reserved.