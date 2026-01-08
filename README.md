# Drift City 🏎️

A retro pixel-art drift racing game built in Java with realistic vehicle physics.

![Java](https://img.shields.io/badge/Java-17+-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

## 🎮 Features

- **Realistic Drifting Physics**: Full simulation including RPM, tire grip, slip angles, and weight transfer
- **Pixel Art Style**: Retro aesthetic with 4x pixel scaling for that classic arcade look
- **Procedural City**: Explore a procedurally-generated city with roads and buildings
- **Scoring System**: Earn points for drifts with combo multipliers
- **Visual Effects**: Tire smoke, tire marks, and dynamic camera
- **Fullscreen Mode**: Runs in fullscreen for immersive gameplay
- **Camera Velocity Look-Ahead**: Camera smoothly leads in the direction of travel
- **Rev Limiter Bounce Effect**: Tachometer needle bounces realistically when hitting rev limiter

## 🕹️ Controls

| Key | Action |
|-----|--------|
| W / ↑ | Accelerate |
| S / ↓ | Brake / Reverse |
| A / ← | Steer Left |
| D / → | Steer Right |
| Space | Handbrake (essential for drifting!) |
| E / Shift | Shift Up |
| Q / Ctrl | Shift Down |
| ESC / P | Pause |
| R | Reset Position |

## 🏁 How to Drift

1. Build up speed using W
2. Turn into a corner with A or D
3. Tap SPACE (handbrake) to break traction on the rear wheels
4. Counter-steer to maintain the drift
5. Modulate throttle to control the drift angle

## ⚙️ Physics System

### Engine Simulation
- Realistic torque curves with peak torque around 4500 RPM
- 6-speed transmission with proper gear ratios
- Rev limiter and clutch simulation
- Power calculation: HP = (Torque × RPM) / 5252

### Tire Physics
- Pacejka "Magic Formula" tire model (simplified)
- Slip ratio (longitudinal) and slip angle (lateral)
- Temperature affects grip (optimal at 90°C)
- Pressure affects grip (optimal at 32 PSI)
- Tire wear over time

### Drift Mechanics
- Static vs kinetic friction transition
- Weight transfer during acceleration/braking
- Handbrake locks rear wheels for drift initiation
- Counter-steer assist (configurable)

## 📁 Project Structure

```
src/
├── App.java              # Main entry point
├── game/                 # Core game logic
│   ├── Car.java          # Car entity combining physics & effects
│   ├── GameLoop.java     # Main game loop
│   └── GameState.java    # Game state management
├── graphics/             # Rendering system
│   ├── Camera.java       # Camera following & effects
│   ├── ParticleSystem.java # Smoke, sparks, tire marks
│   └── Renderer.java     # Main pixel art renderer
├── input/                # Input handling
│   └── InputHandler.java # Keyboard input with smoothing
├── physics/              # Realistic vehicle physics
│   ├── Engine.java       # Engine simulation (RPM, torque, gears)
│   ├── Tire.java         # Tire physics (grip, slip, temperature)
│   └── VehiclePhysics.java # Complete vehicle dynamics
├── scoring/              # Scoring system
│   └── DriftScoring.java # Drift detection & scoring
├── ui/                   # User interface
│   └── GameWindow.java   # Main game window
├── util/                 # Utilities
│   ├── GameConstants.java # All game configuration
│   ├── MathUtils.java    # Math helper functions
│   └── Vector2D.java     # 2D vector class
└── world/                # World generation
    ├── Building.java     # Building entity
    ├── CityWorld.java    # Procedural city generator
    └── Road.java         # Road segment
│   └── CarConfig.java    # Car configuration/tuning
├── world/
│   ├── Track.java        # Track generation and queries
│   └── TrackSegment.java # Individual track segments
├── graphics/
│   ├── Renderer.java     # Main rendering system
│   ├── Camera.java       # Camera that follows car
│   └── PixelColors.java  # Color palette
├── input/
│   └── InputHandler.java # Keyboard input handling
├── scoring/
│   └── ScoreManager.java # Drift scoring system
├── ui/
│   └── HUD.java          # Heads-up display
└── util/
    ├── Vector2D.java     # 2D vector math
    └── MathUtils.java    # Math utilities
```

## 🚀 Running the Game

1. Open the project in VS Code or your IDE
2. Compile all Java files in the `src` directory
3. Run `App.java`

### Command Line
```bash
cd src
javac -d ../out App.java game/*.java physics/*.java world/*.java graphics/*.java input/*.java scoring/*.java ui/*.java util/*.java
cd ../out
java App
```

## 🔧 Customization for AI/Future Development

The code is structured for easy modification:

### Adding New Tracks
Create new track layouts in `Track.java`:
```java
public static Track createMyTrack() {
    Track track = new Track("My Track");
    // Add segments with different curve types
    return track;
}
```

### Tuning Car Physics
Modify `CarConfig.java` to change:
- Grip levels
- Engine power
- Weight distribution
- Steering response

### Adding New Features
- **Power-ups**: Add to `Game.java` update loop
- **Multiple cars**: Extend `Car.java` for AI
- **New visuals**: Modify `Renderer.java`
- **Sound**: Add a `SoundManager` class

## 📊 Scoring System

| Factor | Description |
|--------|-------------|
| Drift Angle | Higher angle = more points (up to ~35°) |
| Speed | Faster drifts score better |
| Duration | Longer drifts accumulate more |
| Combo | Chain drifts for multiplier bonus |
| Near-Spinout | Bonus for drifting at the edge! |

### Ratings
- **S Rank**: 100,000+ points
- **A Rank**: 50,000+ points
- **B Rank**: 25,000+ points
- **C Rank**: 10,000+ points

## 🎨 Visual Style

The game uses a retro pixel art aesthetic with:
- Limited color palette
- Chunky car sprites
- Tire smoke particles
- Skid marks on the track
- Curb striping on corners

## Folder Structure

The workspace contains these folders:

- `src`: the folder with all source code
- `lib`: the folder for dependencies (if needed)
- `out`: compiled class files (created when building)

---

*Built for APCS A - Java Projects*
