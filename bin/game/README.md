# Game Package 🎮

This package contains the core game logic, main loop, and car entity.

## Files Overview

### GameLoop.java
The main game loop that coordinates all game systems.

**Key Features:**
- **Fixed Timestep**: Uses `TARGET_FPS` for consistent physics
- **Delta Time**: Passed to all update methods for smooth physics
- **System Coordination**: Updates physics, input, camera, particles in order

**Game Loop Flow:**
```
1. Process Input (InputHandler)
2. Update Car Physics (VehiclePhysics)
3. Update Particles (ParticleSystem)
4. Update Camera (Camera with velocity look-ahead)
5. Check Scoring (DriftScoring)
6. Render Frame (Renderer)
7. Sleep for remaining frame time
```

**Camera Update with Velocity:**
```java
// Camera now receives velocity for look-ahead effect
camera.update(
    car.getX(), car.getY(),           // Position
    car.getAngle(),                    // Rotation
    scoring.getCurrentDriftAngle(),    // Drift offset
    car.getPhysics().getSpeed(),       // Speed
    car.getPhysics().getVelocity()     // Velocity for look-ahead
);
```

**How to Modify:**
- Add new systems by calling their `update()` in the main loop
- Change `TARGET_FPS` in GameConstants for different tick rate
- Add pause/menu states in game state handling

---

### Car.java
Car entity that combines physics, effects, and position.

**Key Features:**
- **Physics Integration**: Contains `VehiclePhysics` instance
- **Effect Triggers**: Spawns smoke/tire marks during slides
- **Position/Rotation**: Exposes getters for rendering

**Effect Spawning:**
```java
// During update, spawn effects when sliding
if (physics.isSliding()) {
    // Spawn smoke from rear tires
    particleSystem.addSmoke(rearLeftX, rearLeftY, ...);
    particleSystem.addSmoke(rearRightX, rearRightY, ...);
    
    // Add tire marks
    particleSystem.addTireMark(rearLeftX, rearLeftY, angle);
}
```

**How to Modify:**
- Add new effects by checking conditions in `update()`
- Change effect spawn rates
- Add car customization (colors, stats)

---

### GameState.java
Manages different game states (playing, paused, menu, etc.)

**States:**
```java
MENU      // Main menu
PLAYING   // Active gameplay
PAUSED    // Game paused
GAME_OVER // Game ended
```

**How to Add States:**
```java
// Add new state enum value
SETTINGS,
GARAGE,

// Handle in GameLoop:
switch (gameState.getCurrentState()) {
    case SETTINGS:
        updateSettings();
        renderSettings();
        break;
}
```

---

## Adding New Game Features

### Example: Add Collectibles
```java
// Create Collectible.java in game package:
public class Collectible {
    private double x, y;
    private boolean collected;
    private int points;
    
    public boolean checkCollection(Car car) {
        double dist = MathUtils.distance(x, y, car.getX(), car.getY());
        if (dist < 20 && !collected) {
            collected = true;
            return true;
        }
        return false;
    }
}

// In GameLoop, add list and update:
List<Collectible> collectibles = new ArrayList<>();

// In update:
for (Collectible c : collectibles) {
    if (c.checkCollection(car)) {
        scoring.addPoints(c.getPoints());
        // Play sound, spawn particles, etc.
    }
}
```

### Example: Add AI Cars
```java
// Create AICar.java extending Car:
public class AICar extends Car {
    private List<Vector2D> waypoints;
    private int currentWaypoint;
    
    @Override
    public void update(double dt) {
        // Calculate steering toward waypoint
        Vector2D target = waypoints.get(currentWaypoint);
        double angleToTarget = Math.atan2(
            target.getY() - getY(),
            target.getX() - getX()
        );
        
        // Apply steering
        double steerAmount = MathUtils.normalizeAngle(angleToTarget - getAngle());
        physics.setSteering(MathUtils.clamp(steerAmount, -1, 1));
        
        // Apply throttle
        physics.setThrottle(0.8);
        
        super.update(dt);
    }
}
```

---

## Game Architecture

```
┌─────────────┐
│  GameLoop   │ ← Main coordinator
├─────────────┤
│   update()  │ → Car.update() → VehiclePhysics
│             │ → ParticleSystem.update()
│             │ → Camera.update()
│             │ → DriftScoring.update()
├─────────────┤
│   render()  │ → Renderer.render()
└─────────────┘
```

---

## Constants Reference

| Constant | Value | Description |
|----------|-------|-------------|
| `TARGET_FPS` | 60 | Game tick rate |
| `FIXED_TIMESTEP` | 1/60 | Physics timestep |
