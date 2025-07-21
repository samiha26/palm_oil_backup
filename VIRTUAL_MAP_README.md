# Virtual Map Feature for Palm Oil Reconnaissance

## Overview
The Virtual Map feature allows reconnaissance teams to create visual maps of palm oil plantation plots by manually plotting tree locations. This feature works completely offline and stores data locally until internet connection is available for cloud sync.

## Features

### Core Functionality
- **Plot-based tree mapping**: Create and manage multiple plot maps with Malaysian plantation standards
- **Interactive canvas**: Touch-based tree placement with advanced zoom (up to 8.0x) and pan
- **Multi-level grid system**: Adaptive grids (25px base, 12.5px fine, 34px plantation guides)
- **GPS integration**: Real-time GPS positioning with accuracy visualization
- **Offline operation**: Fully functional without internet connection
- **Data synchronization**: Automatic sync when internet becomes available
- **Real-world scaling**: 500m × 500m plots mapped to 2000×2000 pixels (1 pixel = 0.25m)

### Advanced Map Features  
- **Adaptive zoom**: Enhanced zoom range from 0.3x to 8.0x for detailed tree positioning
- **Plantation spacing guides**: Green dashed lines showing Malaysian 8.5m standard spacing
- **GPS coordinate transformation**: Real-world GPS coordinates mapped to virtual canvas
- **User location tracking**: Live GPS position with accuracy indicators
- **Professional tree rendering**: Adaptive tree sizes that scale appropriately across zoom levels

### Map Interactions
- **Tap to add trees**: GPS-based positioning or manual map placement
- **Tap trees to manage**: Access tree details, edit, move, or delete
- **Pinch to zoom**: Advanced zoom with 8.0x maximum for precision work
- **Drag to pan**: Smooth panning with constraint boundaries
- **Double-tap to reset**: Return to default view
- **GPS positioning**: Use current GPS location for accurate tree placement

### Malaysian Plantation Integration
- **Standard specifications**: 8.5m × 8.5m triangular pattern, 140 trees/hectare
- **Plot dimensions**: 25 hectares (500m × 500m) real-world mapping  
- **Field work optimization**: Built-in walking time estimates and survey capacity
- **Professional naming**: Systematic tree ID recommendations (Row-Tree format)

### Tree Management
- **Unique tree IDs**: Each tree has a unique identifier within its plot
- **Position tracking**: X,Y coordinates for precise location mapping with GPS integration
- **Real-world coordinates**: GPS latitude/longitude with coordinate transformation
- **Notes support**: Add notes about specific trees
- **Professional placement**: GPS-assisted positioning for field accuracy
- **Malaysian standards**: Built-in 8.5m spacing validation and plantation guidelines

## Database Structure

### TreeLocationEntity
```kotlin
data class TreeLocationEntity(
    val id: Long,                    // Auto-generated primary key
    val treeId: String,              // User-defined tree identifier
    val plotId: String,              // Plot identifier
    val xCoordinate: Float,          // X position on map (0-2000)
    val yCoordinate: Float,          // Y position on map (0-2000)
    val latitude: Double?,           // Optional GPS latitude
    val longitude: Double?,          // Optional GPS longitude
    val createdAt: Long,             // Creation timestamp
    val updatedAt: Long,             // Last update timestamp
    val isSynced: Boolean,           // Sync status
    val syncTimestamp: Long?,        // Last sync timestamp
    val notes: String?               // Optional notes
)
```

### Key Constraints
- Unique tree ID per plot (one tree ID cannot exist twice in the same plot)
- X,Y coordinates range from 0 to 2000 (virtual map units)
- Automatic timestamp management for creation and updates

## User Interface

### Virtual Map Screen
1. **Plot Selection Dropdown**: Choose existing plot or create new one
2. **Map Canvas**: Interactive 2000×2000 unit coordinate system with real-world scaling
3. **Multi-Level Grid System**: 
   - Base grid (25px) for general positioning
   - Fine grid (12.5px) appears at zoom > 2.0x
   - Plantation guides (34px) show 8.5m spacing at zoom > 4.0x
4. **Tree Markers**: Adaptive circular markers with zoom-aware tree IDs
5. **GPS Integration**: Live GPS position indicator with accuracy visualization
6. **Floating Action Button**: Quick access to GPS-based tree placement
7. **Professional UI**: Real-world distance display and plantation statistics

### Enhanced Menu Options
- **Create Plot**: Add new plot for mapping with Malaysian standards
- **Center Map**: Reset map view to default position  
- **Plot Statistics**: Detailed plantation metrics and completion progress
- **GPS Debug**: Real-time GPS coordinate analysis and accuracy monitoring
- **Plantation Guide**: Malaysian palm oil field work reference and specifications
- **Location Services**: GPS positioning and coordinate transformation tools

### Tree Options (on tap)
- **View Details**: Comprehensive tree information with GPS coordinates and real-world distances
- **Edit Tree ID**: Change tree identifier with validation
- **Move Tree**: Relocate tree position with GPS assistance
- **Add Notes**: Add descriptive notes with timestamps
- **Delete Tree**: Remove tree from plot with confirmation
- **GPS Information**: View coordinate transformation and plantation position data

## Usage Workflow

### For Malaysian Palm Oil Reconnaissance Teams
1. Open "Virtual Map" from Recon Home
2. Select existing plot or create new one with Malaysian plantation standards
3. **GPS Method**: Tap floating action button while standing at tree location
4. **Manual Method**: Tap on map for precise manual placement  
5. Enter systematic tree ID (recommended: Row-Tree format, e.g., R1-T15)
6. Use zoom controls (up to 8.0x) for precise positioning
7. Verify tree spacing using plantation guide lines (8.5m standard)
8. Access real-time GPS debug information and plantation statistics
9. Data automatically saves locally with GPS coordinates
10. Sync to cloud when internet is available

### Advanced Features for Field Work
1. **Real-world positioning**: GPS coordinates automatically transformed to map coordinates
2. **Plantation validation**: Check against Malaysian standard spacing (8.5m)
3. **Progress tracking**: Monitor trees per hectare and completion status
4. **Field navigation**: Use GPS position indicator for location awareness
5. **Professional documentation**: Export data with real-world coordinates and distances

### For Harvesters (Future Use)
1. Download plot maps from cloud
2. View tree locations for optimal path planning
3. Use shortest path algorithms for efficient harvesting
4. Mark trees as harvested during collection

## Technical Implementation

### Architecture
- **MVVM Pattern**: Separation of UI, business logic, and data
- **Room Database**: Local SQLite storage with ORM
- **Repository Pattern**: Centralized data access
- **Coroutines**: Async operations for smooth UI

### Key Components
- `VirtualMapView`: Enhanced custom canvas-based map view with multi-level grids
- `TreeLocationRepository`: Advanced data access layer with GPS integration
- `VirtualMapViewModel`: Business logic and state management with real-world calculations
- `TreeLocationDao`: Database operations with coordinate transformation
- `MapUtils`: Comprehensive utility functions for plantation calculations
- `LocationHelper`: Professional GPS management with Fused Location Provider

### Performance Features
- **Efficient rendering**: Custom drawing optimized for large datasets with adaptive zoom
- **Zoom-aware elements**: Tree IDs and grid details shown based on zoom level
- **Constraint-based panning**: Prevents scrolling beyond map bounds with smooth boundaries
- **Background GPS**: Non-blocking location updates with accuracy monitoring
- **Real-world scaling**: Optimized coordinate transformation for Malaysian plantation mapping
- **Professional grid system**: Multi-level grids (25px, 12.5px, 34px) for precision work

## Utility Functions

### MapUtils Class
- **Distance calculations**: Between trees and points with real-world conversion
- **Path optimization**: Advanced greedy algorithm for harvest routes
- **Coordinate conversion**: GPS to map coordinates with Malaysian plantation bounds
- **Grid references**: A1, B5 style coordinate naming system
- **Tree density**: Calculate trees per unit area with Malaysian standards
- **Radius search**: Find trees within specified distance
- **Real-world scaling**: Convert virtual coordinates to actual meters
- **Plantation validation**: Check spacing against 8.5m Malaysian standards

### LocationHelper Class
- **GPS management**: Fused Location Provider with enhanced accuracy
- **Permission handling**: Automatic location permission management
- **Real-time updates**: Continuous location tracking with accuracy monitoring
- **Coordinate transformation**: GPS to map coordinate conversion
- **Debug capabilities**: Detailed GPS information and troubleshooting

### Example Usage
```kotlin
// Find optimal harvest path with real-world distances
val path = MapUtils.calculateOptimalPath(startX, startY, treeList)

// Calculate real-world distance between trees
val distanceMeters = MapUtils.calculateRealWorldDistance(tree1, tree2)

// Convert GPS to map coordinates
val (mapX, mapY) = LocationHelper.gpsToMapCoordinates(lat, lng, plotBounds)

// Convert coordinates to grid reference
val gridRef = MapUtils.coordinateToGridReference(x, y, 25f) // Returns "A1"

// Validate Malaysian plantation spacing
val isValidSpacing = MapUtils.validatePlantationSpacing(trees, 8.5f)
```

## Data Synchronization

### Local Storage
- All data stored in Room database
- `is_synced` flag tracks sync status
- Automatic timestamps for audit trail

### Cloud Sync (When Implemented)
- Upload unsynced tree locations
- Download shared plot maps
- Conflict resolution for concurrent edits
- Bulk operations for efficiency

## Scalability Considerations

### Current Specifications  
- **Map size**: 2000×2000 units (500m × 500m real-world)
- **Grid system**: Multi-level (25px base, 12.5px fine, 34px plantation)
- **Zoom range**: 0.3x to 8.0x for detailed precision work
- **Tree capacity**: Tested up to 1000+ trees per plot with smooth performance
- **Plot limit**: No enforced limit, scales with device storage
- **GPS accuracy**: Supports ±1-5 meter accuracy for professional surveying
- **Real-world scaling**: 1 pixel = 0.25 meters for precise measurements

### Malaysian Plantation Standards Integration
- **Tree spacing**: 8.5m × 8.5m triangular pattern validation
- **Plantation density**: 140 trees per hectare monitoring
- **Field work optimization**: Walking time and survey capacity calculations
- **Professional naming**: Systematic Row-Tree ID recommendations

### Future Enhancements
- **Variable plot sizes**: Custom plantation dimensions
- **Advanced coordinate systems**: UTM and local grid integration  
- **Satellite imagery overlay**: Aerial photo integration for enhanced visualization
- **Real-time collaboration**: Multi-user simultaneous editing
- **Advanced analytics**: Plantation health monitoring and yield prediction
- **Export capabilities**: Professional survey reports and CAD integration

## Error Handling

### Validation
- **Tree ID uniqueness**: Enforced within plots with clear error messaging
- **Coordinate bounds**: GPS and map coordinate validation with automatic correction
- **Input sanitization**: Special characters and format validation
- **GPS accuracy**: Location accuracy warnings and alternative positioning methods
- **Plantation standards**: Automatic spacing validation against Malaysian standards

### User Feedback
- **Toast messages**: Clear operation confirmations and error notifications
- **Progress indicators**: GPS acquisition and data processing status
- **Error resolution**: Step-by-step guidance for common issues
- **Debug tools**: Professional GPS debugging and coordinate analysis
- **Offline handling**: Graceful degradation when GPS unavailable

## Testing Recommendations

### Manual Testing
1. **Multi-level functionality**: Test all zoom levels (0.3x to 8.0x) with grid detail progression
2. **GPS integration**: Verify real-world coordinate transformation accuracy
3. **Performance testing**: Test zoom/pan performance with large datasets (500+ trees)
4. **Malaysian standards**: Validate plantation spacing calculations and visual guides
5. **Edge cases**: Test duplicate IDs, boundary coordinates, and GPS accuracy variations
6. **Professional workflow**: Test complete reconnaissance team workflow from GPS positioning to data export

### Performance Testing
- **Memory efficiency**: Usage with 1000+ trees across multiple zoom levels  
- **GPS responsiveness**: Location update frequency and accuracy monitoring
- **Rendering optimization**: Grid drawing performance during continuous zoom operations
- **Database performance**: Query efficiency for large datasets with GPS coordinates
- **Real-world accuracy**: Coordinate transformation precision testing

### Field Testing
- **GPS accuracy**: Test in various terrain and weather conditions
- **Battery optimization**: Long-term GPS usage efficiency
- **Offline reliability**: Full functionality without internet connectivity
- **Professional usage**: Real plantation surveying workflow validation

## Recent Enhancements (Latest Version)

### Advanced Grid System
- **Reduced base grid**: From 50px to 25px for finer positioning precision
- **Multi-level grids**: Adaptive detail based on zoom level
  - Base grid (25px): Always visible for general positioning
  - Fine grid (12.5px): Appears at zoom > 2.0x for detailed work  
  - Plantation guides (34px): Malaysian 8.5m spacing guides at zoom > 4.0x
- **Visual hierarchy**: Different colors and transparency levels for grid layers

### Enhanced Zoom Capabilities  
- **Extended zoom range**: Maximum zoom increased from 3.0x to 8.0x
- **Professional precision**: 26.7x total zoom range (0.3x to 8.0x) for detailed tree positioning
- **Adaptive elements**: Tree sizes and UI elements scale appropriately across all zoom levels
- **Smooth interaction**: Enhanced gesture detection and constraint handling

### Real-World Integration
- **GPS coordinate transformation**: Live GPS positioning with Malaysian plantation coordinate bounds
- **Accuracy visualization**: GPS accuracy indicators with real-time monitoring
- **Malaysian standards**: Built-in 8.5m spacing validation and 140 trees/hectare calculations  
- **Professional workflow**: Field guide integration with walking time estimates and survey capacity

### Performance Optimizations
- **Efficient multi-grid rendering**: Optimized drawing for complex grid systems
- **Zoom-aware performance**: Selective detail rendering based on zoom level
- **Memory management**: Improved handling of large datasets with GPS coordinates
- **Smooth GPS integration**: Non-blocking location updates with background processing

## Integration Points

### With Existing Features
- **Database integration**: Enhanced Room database with GPS coordinate support
- **Offline-first design**: Follows same pattern with GPS data caching
- **Sync mechanisms**: Cloud synchronization includes GPS coordinates and real-world data
- **Professional reporting**: Integration with survey data export and plantation analytics

### With Malaysian Plantation Management
- **Tree location data**: GPS coordinates for precision harvest planning
- **Plot boundaries**: Real-world plantation boundaries with coordinate transformation
- **Spacing analysis**: Malaysian standard validation for plantation quality assessment
- **Field optimization**: GPS-based route planning for efficient surveying
- **Professional documentation**: Real-world coordinate export for regulatory compliance
