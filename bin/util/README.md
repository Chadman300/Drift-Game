# Utility Package 🛠️

This package contains constants, math utilities, and helper classes used throughout the game.

## Files Overview

### GameConstants.java
Central configuration file for ALL game constants. Modify values here to tune the game.

**Categories:**

#### Window & Rendering
```java
WINDOW_WIDTH = 1280        // Base window width
WINDOW_HEIGHT = 720        // Base window height
RENDER_WIDTH = 320         // Internal render width (1/4 for pixel art)
RENDER_HEIGHT = 180        // Internal render height
TARGET_FPS = 60            // Target framerate
```

#### Car Dimensions
```java
CAR_LENGTH = 24            // Car length in pixels
CAR_WIDTH = 12             // Car width in pixels
CAR_MASS = 1400            // Mass in kg
```

#### Engine
```java
ENGINE_HORSEPOWER = 280    // Peak horsepower
ENGINE_TORQUE = 260        // Peak torque (lb-ft)
IDLE_RPM = 800             // Idle RPM
REDLINE_RPM = 7500         // Visual redline
REV_LIMITER_RPM = 7800     // Fuel cut RPM
MAX_RPM = 8000             // Maximum RPM
GEAR_RATIOS = {...}        // 6-speed gearbox ratios
FINAL_DRIVE_RATIO = 3.7    // Differential ratio
```

#### Tire Physics
```java
TIRE_GRIP = 1.2            // Base grip multiplier
TIRE_RADIUS = 0.31         // Tire radius (meters)
OPTIMAL_TEMP = 90          // Best grip temperature
OPTIMAL_PRESSURE = 32      // Best grip pressure (PSI)
HANDBRAKE_GRIP_MULTIPLIER = 0.3  // Rear grip with handbrake
```

#### Camera
```java
CAMERA_SMOOTHING = 0.08    // Camera follow smoothness
CAMERA_DRIFT_OFFSET = 50   // Offset during drifts
CAMERA_VELOCITY_LOOKAHEAD = 0.3  // Look-ahead factor
```

#### HUD
```java
HUD_BAR_HEIGHT = 45        // Bottom HUD bar height
```

**How to Modify:**
Simply change the constant values. The game will use the new values on next compile.

---

### MathUtils.java
Common math functions used for physics and rendering.

**Key Functions:**
```java
// Clamp value between min and max
double clamp(double value, double min, double max)

// Smoothly approach a target value
double approach(double current, double target, double maxDelta)

// Linear interpolation
double lerp(double a, double b, double t)

// Normalize angle to -PI to PI range
double normalizeAngle(double angle)

// Distance between two points
double distance(double x1, double y1, double x2, double y2)
```

**How to Add Functions:**
```java
// Example: Add easing function
public static double easeOutQuad(double t) {
    return 1 - (1 - t) * (1 - t);
}
```

---

### Vector2D.java
2D vector class for positions, velocities, and forces.

**Key Methods:**
```java
// Creation
new Vector2D(x, y)
Vector2D.zero()
Vector2D.fromAngle(angle, magnitude)

// Operations
add(other)           // Returns new vector
subtract(other)      // Returns new vector
multiply(scalar)     // Returns new vector
normalize()          // Returns unit vector
dot(other)           // Dot product
cross(other)         // 2D cross product (returns scalar)

// Properties
magnitude()          // Length of vector
angle()              // Angle in radians
getX(), getY()       // Component access
```

**How to Use:**
```java
// Physics example
Vector2D velocity = new Vector2D(10, 5);
Vector2D force = new Vector2D(0, -9.8); // gravity
velocity = velocity.add(force.multiply(deltaTime));
```

---

## Adding New Constants

When adding new features, add constants to `GameConstants.java` with clear naming:

```java
// In GameConstants.java, add with comment section:

// ======== BOOST SYSTEM ========
public static final double BOOST_POWER_MULTIPLIER = 1.5;
public static final double BOOST_DURATION = 3.0;      // seconds
public static final double BOOST_COOLDOWN = 10.0;     // seconds
```

Then use in your code:
```java
if (boostActive) {
    power *= GameConstants.BOOST_POWER_MULTIPLIER;
}
```

---

## Best Practices

1. **Always use GameConstants** - Never hardcode magic numbers
2. **Document new constants** - Add comments explaining what they do
3. **Group related constants** - Use comment headers for organization
4. **Use meaningful names** - `HANDBRAKE_GRIP_MULTIPLIER` not `HGM`
