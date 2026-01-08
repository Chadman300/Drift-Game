# Graphics Package 🎨

This package handles all rendering, camera control, and visual effects.

## Files Overview

### Renderer.java
Main rendering system that draws the pixel-art game world and HUD.

**Key Features:**
- **Pixel Scaling**: Renders at 1/4 resolution then scales up 4x for retro look
- **Double Buffering**: Smooth rendering without flickering
- **HUD System**: Organized layout with black bar at bottom for gauges

**HUD Layout:**
```
┌─────────────────────────────────────────────┐
│ [Controls hint]            SCORE: 999,999   │  <- Top area
│                               x3 [combo]    │
│            [DRIFT GRADE]                    │
│             [score]                         │
│             [angle]                         │
│                                             │
│               [GAME WORLD]                  │
│                                             │
├─────────────────────────────────────────────┤
│[GEAR][RPM GAUGE]    [SPEED MPH]   TIRE/GRIP │  <- HUD Bar
└─────────────────────────────────────────────┘
```

**RPM Gauge with Bounce:**
The tachometer needle bounces when hitting the rev limiter:
```java
// Get bounce offset from engine
double bounce = engine.getRevLimiterBounce();
double displayPercent = rpmPercent + bounce;

// Apply to needle angle calculation
double needleAngle = Math.PI - (displayPercent * Math.PI);
```

**How to Modify:**
- Adjust `HUD_BAR_HEIGHT` in GameConstants for bar size
- Change colors in `drawHUD()` and `drawRPMGauge()`
- Add new HUD elements by adding draw calls in `drawHUD()`

---

### Camera.java
Smooth camera that follows the player car with effects.

**Key Features:**
- **Smooth Following**: Lerps toward car position
- **Drift Offset**: Camera shifts outward during drifts for dramatic effect
- **Velocity Look-Ahead**: Camera leads in the direction of travel
- **Rotation Smoothing**: Camera rotation interpolates smoothly

**Velocity Look-Ahead:**
```java
// Camera moves slightly in the direction of travel
double velocityOffsetX = velocity.getX() * CAMERA_VELOCITY_LOOKAHEAD;
double velocityOffsetY = velocity.getY() * CAMERA_VELOCITY_LOOKAHEAD;

// Combined with drift offset for total offset
double totalOffsetX = driftOffsetX + velocityOffsetX;
double totalOffsetY = driftOffsetY + velocityOffsetY;
```

**Camera Update Method:**
```java
public void update(double targetX, double targetY, 
                   double targetAngle, double driftAngle, 
                   double speed, Vector2D velocity)
```

**How to Modify:**
- Increase `CAMERA_VELOCITY_LOOKAHEAD` (default 0.3) for more look-ahead
- Adjust `CAMERA_DRIFT_OFFSET` for more dramatic drift camera
- Change `CAMERA_SMOOTHING` for faster/slower camera response

---

### ParticleSystem.java
Handles tire smoke, sparks, and tire mark effects.

**Key Features:**
- **Thread-Safe**: Uses synchronized iteration for concurrent safety
- **Tire Smoke**: White puffs when sliding/wheelspin
- **Tire Marks**: Dark lines on ground from sliding
- **Fade Effects**: Particles fade over lifetime

**Adding Particles:**
```java
// Smoke particle
particleSystem.addSmoke(x, y, velocityX, velocityY);

// Tire mark
particleSystem.addTireMark(x, y, angle);
```

**How to Modify:**
- Adjust particle lifetimes in particle creation
- Change colors for different effects
- Add new particle types (sparks, nitro flames, etc.)

---

## Adding New Visual Features

### Example: Add Boost Flames
```java
// In ParticleSystem.java, add:
public void addBoostFlame(double x, double y, double angle) {
    // Orange/red particles behind car
    double offsetX = -Math.cos(angle) * 20;
    double offsetY = -Math.sin(angle) * 20;
    particles.add(new Particle(
        x + offsetX, y + offsetY,
        -Math.cos(angle) * 50, -Math.sin(angle) * 50,
        0.1, // lifetime
        new Color(0xFF6600) // orange
    ));
}
```

### Example: Add Screen Shake
```java
// In Camera.java, add:
private double shakeIntensity = 0;
private double shakeTimer = 0;

public void addShake(double intensity, double duration) {
    shakeIntensity = intensity;
    shakeTimer = duration;
}

// In update(), apply offset:
if (shakeTimer > 0) {
    x += (Math.random() - 0.5) * shakeIntensity;
    y += (Math.random() - 0.5) * shakeIntensity;
    shakeTimer -= dt;
}
```

### Example: Add Speed Lines
```java
// In Renderer.java, during renderWorld():
if (speed > 50) {
    // Draw motion blur lines
    bufferGraphics.setColor(new Color(255, 255, 255, 100));
    for (int i = 0; i < 10; i++) {
        int lineX = (int)(Math.random() * RENDER_WIDTH);
        int lineLength = (int)(speed / 2);
        bufferGraphics.drawLine(lineX, 0, lineX, lineLength);
    }
}
```

---

## Constants Reference (GameConstants.java)

| Constant | Value | Description |
|----------|-------|-------------|
| `WINDOW_WIDTH` | 1280 | Window width (fullscreen auto-adjusts) |
| `WINDOW_HEIGHT` | 720 | Window height |
| `RENDER_WIDTH` | 320 | Internal render resolution |
| `RENDER_HEIGHT` | 180 | (window / 4 for pixel effect) |
| `CAMERA_SMOOTHING` | 0.08 | Camera follow speed |
| `CAMERA_DRIFT_OFFSET` | 50 | Drift camera offset |
| `CAMERA_VELOCITY_LOOKAHEAD` | 0.3 | Look-ahead factor |
| `HUD_BAR_HEIGHT` | 45 | Bottom HUD bar height |
