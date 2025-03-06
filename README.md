# NYU Bus Tracker

An Android application for tracking NYU's shuttle bus service in real-time.

## Features

- Real-time bus location tracking on an interactive map
- View all NYU bus stops and routes
- Get estimated arrival times for buses at each stop
- Mark favorite stops for quick access
- Support for weekday, Friday, and weekend schedules
- Offline mode capability
- Sliding drawer UI for easy access to bus information
- Safe Ride service information

## Setup

1. Clone the repository
2. Copy `api-keys.xml.sample` to `api-keys.xml` and add required API keys
3. Open project in Android Studio
4. Build and run

## Project Structure

- `activities/` - Main activity and UI logic
- `adapters/` - List adapters for stops and times
- `helpers/` - Network, data management and UI helper classes  
- `models/` - Data models for buses, routes, stops and times

## Key Components

### Bus Manager
Singleton class that manages all bus, route and stop data. Handles parsing JSON responses from the API and maintaining the application state.

### Downloader System 
Network request framework with specialized helpers for downloading:
- Bus locations
- Routes
- Stops  
- Schedule times
- Version information

### Location & Time Tracking
- Real-time bus location updates every few seconds
- Schedule time calculations accounting for weekday/weekend differences
- Distance and ETA calculations between stops

### User Interface
- Google Maps integration for bus tracking
- Sliding drawer for stop information  
- Stop list with favorites support
- Time display with route information
- Safe Ride service information panel

## Testing

The project includes instrumentation tests for:
- Main activity functionality
- Time calculations and comparisons
- Data model validation

## License

See LICENSE file for details.
