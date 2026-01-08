# Physics Package 🔧

This package handles all vehicle physics simulation including engine, tires, and vehicle dynamics.

## Files Overview

### Engine.java
Simulates realistic engine behavior including RPM, torque curves, and power delivery.

**Key Features:**
- **Torque Curve**: Realistic curve with peak around 4500 RPM, tapering at high RPM
- **Gear System**: 6-speed + reverse with proper gear ratios
- **Rev Limiter**: Cuts fuel briefly when hitting max RPM, includes bounce effect
- **Clutch Simulation**: Smooth clutch engagement during gear changes

**Rev Limiter Bounce Effect:**
When the engine hits the rev limiter, the RPM gauge needle bounces back realistically:
```java
// Fields for bounce physics (spring-damper system)
private double revLimiterBounce;     // Current bounce offset
private double revLimiterBounceVel;  // Bounce velocity
private int bounceCount;             // Dampening counter

// When hitting limiter, trigger bounce
revLimiterBounceVel = -8.0; // Initial bounce back velocity

// Use getRevLimiterBounce() in Renderer to offset the needle
```

**How to Modify:**
- Change `peakTorque` in constructor for more/less power
- Adjust gear ratios in `GameConstants.GEAR_RATIOS`
- Modify `calculateTorque()` curve for different power delivery

---

### Tire.java
Implements realistic tire physics using simplified Pacejka "Magic Formula".

**Key Concepts:**
- **Slip Ratio**: Difference between wheel speed and road speed (longitudinal grip)
- **Slip Angle**: Angle between tire direction and travel direction (lateral grip)
- **Temperature**: Affects grip (optimal around 90°C)
- **Pressure**: Affects grip (optimal around 32 PSI)

**Wheel-Road Synchronization:**
When coasting (no throttle), wheels sync to road speed to prevent false wheelspin:
```java
// In calculateSlipRatio():
boolean isCoasting = throttle < 0.05 && !isHandbraking;
if (isCoasting && Math.abs(slipRatio) > 0.05) {
    // Apply correction to sync wheel to road
    double correction = slipRatio * roadFrictionCorrection * dt;
    wheelSpeed -= correction;
}
```

**How to Modify:**
- Adjust `BASE_GRIP` for overall tire grip
- Change temperature/pressure optimal values
- Modify slip curve in `calculateLateralForce()`

---

### VehiclePhysics.java
Complete vehicle dynamics simulation combining all physics elements.

**Key Features:**
- **Weight Transfer**: During acceleration/braking, weight shifts front/rear
- **Lateral Friction**: Resists sideways sliding, stronger when not on throttle
- **Engine Braking**: Natural deceleration when off throttle in gear
- **Handbrake**: Locks rear wheels for drift initiation
- **Ackermann Steering**: Better low-speed turning response

**Lateral Friction System:**
```java
// Higher friction when coasting for more control
double frictionCoef = (throttle < 0.1 && !handbrakeActive) ? 1.5 : 0.8;
double lateralFriction = -lateralVelocity * frictionCoef * mass;
```

**Engine Braking:**
```java
// Apply when off throttle and in gear
if (throttle < 0.1 && currentGear > 0 && speed > 5) {
    double engineBrakeForce = 800.0; // Newtons
    // Apply opposite to velocity direction
}
```

**How to Modify:**
- Adjust `CAR_MASS` for heavier/lighter feel
- Change `DRAG_COEFFICIENT` for top speed
- Modify handbrake `HANDBRAKE_GRIP_MULTIPLIER` for drift ease

---

## Adding New Physics Features

### Example: Add Downforce
```java
// In VehiclePhysics.update():
double speed = getSpeed();
double downforce = 0.5 * speed * speed * DOWNFORCE_COEFFICIENT;
// Increase tire grip based on downforce
for (Tire tire : tires) {
    tire.setExtraGrip(downforce / 4 / CAR_MASS);
}
```

### Example: Add Boost/Nitro
```java
// In Engine.java, add:
private boolean boostActive;
private double boostTimer;

public void activateBoost() {
    boostActive = true;
    boostTimer = 3.0; // 3 seconds
}

// In update(), multiply torque when boosting:
if (boostActive) {
    currentTorque *= 1.5;
}
```

---

## Constants Reference (GameConstants.java)

| Constant | Value | Description |
|----------|-------|-------------|
| `ENGINE_HORSEPOWER` | 280 | Base engine power |
| `ENGINE_TORQUE` | 260 | Peak torque (lb-ft) |
| `MAX_RPM` | 8000 | Engine max RPM |
| `REDLINE_RPM` | 7500 | Redline (visual) |
| `REV_LIMITER_RPM` | 7800 | Fuel cut point |
| `CAR_MASS` | 1400 | Vehicle mass (kg) |
| `TIRE_GRIP` | 1.2 | Base tire grip |
| `HANDBRAKE_GRIP_MULTIPLIER` | 0.3 | Rear grip with handbrake |
