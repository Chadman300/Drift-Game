package physics;

import util.GameConstants;
import util.MathUtils;

/**
 * Simulates realistic tire physics
 * Handles grip, slip, temperature, and pressure
 */
public class Tire {
    
    // Position on car
    public enum Position { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT }
    
    private Position position;
    
    // Tire properties
    private double pressure;        // PSI
    private double temperature;     // Celsius
    private double wear;           // 0.0 = new, 1.0 = worn out
    
    // Current state
    private double grip;           // Current grip coefficient
    private double slipRatio;      // Longitudinal slip (acceleration/braking)
    private double slipAngle;      // Lateral slip angle (degrees)
    private double angularVelocity; // Wheel rotation speed (rad/s)
    
    // Forces
    private double longitudinalForce;
    private double lateralForce;
    private double normalForce;    // Weight on this tire
    
    // Slip state
    private boolean isSlipping;
    private boolean isLocked;      // Brake lockup
    private boolean isSpinning;    // Wheelspin
    
    // Smoke/marks
    private double smokeIntensity;
    private boolean leavingMarks;
    
    // Grip modifier from upgrades
    private double gripModifier = 1.0;
    private double durabilityModifier = 1.0;
    
    // Drift-specific grip reduction (set by VehiclePhysics during drift)
    private double driftGripMultiplier = 1.0;
    
    public Tire(Position position) {
        this.position = position;
        this.pressure = GameConstants.OPTIMAL_TIRE_PRESSURE;
        this.temperature = 40; // Starting cold
        this.wear = 0;
        this.grip = GameConstants.BASE_TIRE_GRIP;
        this.slipRatio = 0;
        this.slipAngle = 0;
        this.angularVelocity = 0;
        this.isSlipping = false;
        this.isLocked = false;
        this.isSpinning = false;
        this.smokeIntensity = 0;
        this.leavingMarks = false;
    }
    
    /**
     * Update tire physics
     * @param dt delta time
     * @param vehicleSpeed current vehicle speed (m/s)
     * @param driveForce force applied to wheel (N)
     * @param brakeForce brake force (N)
     * @param steeringAngle steering angle for this wheel (radians)
     * @param lateralVelocity sideways velocity at this tire (m/s)
     */
    public void update(double dt, double vehicleSpeed, double driveForce, 
                       double brakeForce, double steeringAngle, double lateralVelocity) {
        
        // Update temperature based on slip
        updateTemperature(dt);
        
        // Calculate grip based on conditions
        calculateGrip();
        
        // Calculate slip ratio (longitudinal)
        calculateSlipRatio(vehicleSpeed, driveForce, brakeForce, dt);
        
        // Calculate slip angle (lateral)
        calculateSlipAngle(vehicleSpeed, lateralVelocity, steeringAngle);
        
        // Calculate forces using Pacejka-like tire model
        calculateForces();
        
        // Update visual effects - pass vehicle speed to check if car is moving
        updateEffects(vehicleSpeed);
    }
    
    /**
     * Temperature affects grip - cold tires have less grip
     */
    private void updateTemperature(double dt) {
        // Heat up from slip
        double heatGeneration = (Math.abs(slipRatio) + Math.abs(slipAngle) / 90) * 50;
        
        // Cool down towards ambient
        double ambientTemp = 25;
        double cooling = (temperature - ambientTemp) * 0.02;
        
        temperature += (heatGeneration - cooling) * dt;
        temperature = MathUtils.clamp(temperature, GameConstants.TIRE_TEMP_MIN, 
                                       GameConstants.TIRE_TEMP_MAX);
    }
    
    /**
     * Calculate current grip based on temperature, pressure, wear
     */
    private void calculateGrip() {
        double baseGrip = GameConstants.BASE_TIRE_GRIP * gripModifier;
        
        // Temperature factor - optimal around 90°C
        double tempDiff = Math.abs(temperature - GameConstants.TIRE_TEMP_OPTIMAL);
        double tempFactor = 1.0 - (tempDiff / 100) * 0.3;
        tempFactor = MathUtils.clamp(tempFactor, 0.5, 1.0);
        
        // Pressure factor - optimal at 32 PSI
        double pressureDiff = Math.abs(pressure - GameConstants.OPTIMAL_TIRE_PRESSURE);
        double pressureFactor = 1.0 - (pressureDiff / 20) * 0.2;
        pressureFactor = MathUtils.clamp(pressureFactor, 0.7, 1.0);
        
        // Wear factor - scaled by durability modifier
        double effectiveWear = wear / Math.max(durabilityModifier, 0.1);
        double wearFactor = 1.0 - effectiveWear * 0.4;
        wearFactor = MathUtils.clamp(wearFactor, 0.3, 1.0);
        
        // Apply drift grip multiplier (rear tires lose grip during oversteer/drift)
        grip = baseGrip * tempFactor * pressureFactor * wearFactor * driftGripMultiplier;
    }
    
    /**
     * Calculate longitudinal slip ratio
     * Slip = (wheel speed - vehicle speed) / max(wheel speed, vehicle speed)
     * 
     * In a real car:
     * - Wheels primarily roll at vehicle speed (connected to ground)
     * - Throttle can cause wheelspin (wheel faster than ground)
     * - Braking can cause lockup (wheel slower than ground)
     * - But RPM is determined by how fast wheels are actually rotating
     */
    private void calculateSlipRatio(double vehicleSpeed, double driveForce, 
                                     double brakeForce, double dt) {
        // The wheel's base angular velocity comes from the vehicle rolling on the ground
        // This is the fundamental connection: car moves → wheels rotate
        double groundAngularVel = vehicleSpeed / GameConstants.TIRE_RADIUS;
        
        // Calculate torque that could cause slip
        double wheelTorque = driveForce * GameConstants.TIRE_RADIUS;
        if (angularVelocity > 0) {
            wheelTorque -= brakeForce * GameConstants.TIRE_RADIUS;
        } else if (angularVelocity < 0) {
            wheelTorque += brakeForce * GameConstants.TIRE_RADIUS;
        }
        
        // Wheel inertia for slip calculation
        double wheelInertia = 1.2;
        double potentialSlipAccel = wheelTorque / wheelInertia;
        
        // The wheel speed is primarily determined by ground speed
        // Slip occurs when torque overcomes grip
        // During drifting with throttle, wheels can spin MUCH faster than ground speed
        double maxSlipSpeed = 25.0; // rad/s - allows significant wheelspin for realistic RPM
        
        // Calculate slip velocity based on drive force
        // More throttle = more potential wheelspin
        double slipVelocity = 0;
        if (driveForce > 500) {
            // Wheelspin - wheels spin faster than ground
            // Scale with drive force - more power = more spin
            double spinFactor = (driveForce - 500) / 3000.0; // 0 to 1+ based on power
            spinFactor = MathUtils.clamp(spinFactor, 0, 1.5);
            slipVelocity = spinFactor * maxSlipSpeed;
        } else if (brakeForce > 1500) {
            // Lockup - wheels slower than ground
            // Higher threshold (1500 vs 500) makes lockup harder
            // Lower lock factor makes lockup less severe
            double lockFactor = (brakeForce - 1500) / 6000.0; // More gradual
            lockFactor = MathUtils.clamp(lockFactor, 0, 0.7); // Max 70% lockup
            slipVelocity = -lockFactor * maxSlipSpeed * 0.5; // Less severe
        }
        
        // Set wheel angular velocity: ground speed + wheelspin
        angularVelocity = groundAngularVel + slipVelocity;
        
        // Prevent negative rotation when nearly stopped
        if (Math.abs(vehicleSpeed) < 0.5 && Math.abs(driveForce) < 50) {
            angularVelocity = MathUtils.approach(angularVelocity, 0, 3.0 * dt);
        }
        
        // Calculate slip ratio
        double wheelSpeed = angularVelocity * GameConstants.TIRE_RADIUS;
        double maxSpeed = Math.max(Math.abs(wheelSpeed), Math.abs(vehicleSpeed));
        if (maxSpeed > 0.5) {
            slipRatio = (wheelSpeed - vehicleSpeed) / maxSpeed;
        } else {
            slipRatio = 0;
        }
        
        slipRatio = MathUtils.clamp(slipRatio, -1, 1);
        
        // Detect wheelspin and lockup - only when there's significant slip AND throttle/brake
        isSpinning = slipRatio > GameConstants.SLIP_RATIO_THRESHOLD && driveForce > 100;
        // Higher threshold for lockup detection - brakes work better before locking
        isLocked = slipRatio < -0.25 && brakeForce > 500; // Was -0.12, now -0.25
        isSlipping = isSpinning || isLocked;
    }
    
    /**
     * Calculate slip angle from lateral velocity
     */
    private void calculateSlipAngle(double vehicleSpeed, double lateralVelocity, 
                                     double steeringAngle) {
        if (Math.abs(vehicleSpeed) > 0.1) {
            // Slip angle is the difference between wheel direction and velocity direction
            double velocityAngle = Math.atan2(lateralVelocity, Math.abs(vehicleSpeed));
            slipAngle = MathUtils.toDegrees(steeringAngle - velocityAngle);
        } else {
            // At very low speeds, use steering angle directly for responsiveness
            slipAngle = MathUtils.toDegrees(steeringAngle) * 0.5;
        }
        
        slipAngle = MathUtils.clamp(slipAngle, -90, 90);
    }
    
    /**
     * Calculate tire forces using simplified Pacejka model
     * 
     * DRIFT PHYSICS: When driftGripMultiplier is low (rear sliding),
     * lateral force drops dramatically, allowing the rear to swing out.
     */
    private void calculateForces() {
        // Pacejka Magic Formula parameters (simplified)
        double B = 10;  // Stiffness factor
        double C = 1.9; // Shape factor
        double D = grip * normalForce; // Peak value (grip already includes driftGripMultiplier!)
        double E = 0.97; // Curvature factor
        
        // Longitudinal force from slip ratio
        double slipRatioRad = slipRatio * Math.PI;
        longitudinalForce = D * Math.sin(C * Math.atan(B * slipRatioRad - 
                            E * (B * slipRatioRad - Math.atan(B * slipRatioRad))));
        
        // Lateral force from slip angle - THIS IS KEY FOR DRIFTING
        // When rear tires lose grip, lateral force drops, rear swings out
        double slipAngleRad = MathUtils.toRadians(slipAngle);
        double rawLateralForce = D * Math.sin(C * Math.atan(B * slipAngleRad - 
                       E * (B * slipAngleRad - Math.atan(B * slipAngleRad))));
        
        // DRIFT ENHANCEMENT: Apply extra lateral force reduction for rear tires during slide
        // When driftGripMultiplier is low, lateral force should REALLY drop
        // The grip variable already includes driftGripMultiplier, but we need more reduction
        // for proper rear swing-out behavior
        double lateralReduction = 1.0;
        if (isRear() && driftGripMultiplier < 0.8) {
            // Extra reduction for rear tires that are sliding
            // This makes the rear actually swing out instead of just scrubbing
            lateralReduction = 0.3 + 0.7 * (driftGripMultiplier / 0.8);
        }
        
        lateralForce = rawLateralForce * lateralReduction;
        
        // Friction circle - but less aggressive so you can accelerate while turning
        double totalSlip = Math.sqrt(slipRatio * slipRatio + 
                           Math.pow(slipAngle / 90, 2));
        if (totalSlip > 1.5) {
            double reduction = 1.5 / totalSlip;
            reduction = Math.max(reduction, 0.5);
            longitudinalForce *= reduction;
            lateralForce *= reduction;
        }
        
        // Apply grip falloff when sliding heavily
        if (Math.abs(slipAngle) > GameConstants.SLIP_ANGLE_DRIFT) {
            double falloff = 1.0 - GameConstants.TIRE_GRIP_FALLOFF * 
                            (Math.abs(slipAngle) - GameConstants.SLIP_ANGLE_DRIFT) / 60;
            falloff = MathUtils.clamp(falloff, 0.4, 1.0);
            lateralForce *= falloff;
            // Preserve most longitudinal force during drifts
            longitudinalForce *= (0.7 + 0.3 * falloff);
        }
    }
    
    /**
     * Update visual effects (smoke, tire marks)
     * Smoke is generated based on actual loss of traction:
     * - Rear wheels: smoke when spinning (wheelspin from too much throttle)
     * - Front wheels: smoke when understeering (high slip angle, loss of lateral grip)
     * @param vehicleSpeed current vehicle speed to determine if car is actually moving
     */
    private void updateEffects(double vehicleSpeed) {
        double totalSlip = Math.abs(slipRatio) + Math.abs(slipAngle) / 45;
        
        // Calculate wheel speed for intensity scaling
        double wheelSpeed = Math.abs(angularVelocity * GameConstants.TIRE_RADIUS);
        
        // Only generate smoke if car is actually moving at a reasonable speed
        // Increased threshold - turning wheels while stationary should NOT create smoke
        double minSpeedForSmoke = 2.0; // m/s - need significant movement for smoke
        boolean carIsMoving = Math.abs(vehicleSpeed) > minSpeedForSmoke;
        
        smokeIntensity = 0;
        
        if (carIsMoving) {
            if (isRear()) {
                // REAR WHEELS: Smoke only when wheelspin occurs (isSpinning)
                // isSpinning is true when slip ratio is high AND there's drive force
                if (isSpinning || isLocked) {
                    // Base intensity from slip ratio (how much the wheel is spinning vs ground speed)
                    double slipIntensity = Math.abs(slipRatio);
                    
                    // Scale with wheel speed - faster spin = more smoke
                    double speedScale = MathUtils.clamp(wheelSpeed / 20.0, 0.3, 2.0);
                    
                    smokeIntensity = MathUtils.clamp(slipIntensity * speedScale, 0, 1);
                }
                // Also smoke during heavy drifting (high slip angle on rears)
                else if (Math.abs(slipAngle) > 15 && Math.abs(vehicleSpeed) > 5) {
                    double driftIntensity = (Math.abs(slipAngle) - 15) / 45.0;
                    double speedScale = MathUtils.clamp(Math.abs(vehicleSpeed) / 15.0, 0.3, 1.5);
                    smokeIntensity = MathUtils.clamp(driftIntensity * speedScale, 0, 0.8);
                }
            } else {
                // FRONT WHEELS: Smoke only during understeer (loss of front grip)
                // Understeer = high slip angle on front tires while moving at speed
                double understeerThreshold = 12.0; // degrees - above this = losing front grip
                
                if (Math.abs(slipAngle) > understeerThreshold && Math.abs(vehicleSpeed) > 5) {
                    // Intensity based on how much slip angle exceeds threshold
                    double understeerAmount = (Math.abs(slipAngle) - understeerThreshold) / 40.0;
                    
                    // Scale with vehicle speed - faster = more smoke
                    double speedScale = MathUtils.clamp(Math.abs(vehicleSpeed) / 15.0, 0.3, 1.5);
                    
                    smokeIntensity = MathUtils.clamp(understeerAmount * speedScale, 0, 0.7);
                }
                // Also smoke if front wheels lock up during hard braking
                else if (isLocked && Math.abs(vehicleSpeed) > 3) {
                    double lockIntensity = Math.abs(slipRatio);
                    double speedScale = MathUtils.clamp(Math.abs(vehicleSpeed) / 10.0, 0.3, 1.5);
                    smokeIntensity = MathUtils.clamp(lockIntensity * speedScale, 0, 0.8);
                }
            }
        }
        
        // Leave tire marks whenever smoke is being produced (drifting, wheelspin, lockup, etc.)
        leavingMarks = smokeIntensity > 0.1;
        
        // Increase wear when sliding
        if (totalSlip > 0.2) {
            wear += totalSlip * 0.0001;
            wear = MathUtils.clamp(wear, 0, 1);
        }
    }
    
    // Setters
    public void setNormalForce(double force) { this.normalForce = force; }
    public void setPressure(double psi) { 
        this.pressure = MathUtils.clamp(psi, GameConstants.MIN_TIRE_PRESSURE, 
                                        GameConstants.MAX_TIRE_PRESSURE); 
    }
    public void setAngularVelocity(double omega) { this.angularVelocity = omega; }
    
    // Getters
    public Position getPosition() { return position; }
    public double getPressure() { return pressure; }
    public double getTemperature() { return temperature; }
    public double getWear() { return wear; }
    public double getGrip() { return grip; }
    public double getSlipRatio() { return slipRatio; }
    public double getSlipAngle() { return slipAngle; }
    public double getLongitudinalForce() { return longitudinalForce; }
    public double getLateralForce() { return lateralForce; }
    public double getAngularVelocity() { return angularVelocity; }
    public boolean isSlipping() { return isSlipping; }
    public boolean isLocked() { return isLocked; }
    public boolean isSpinning() { return isSpinning; }
    public double getSmokeIntensity() { return smokeIntensity; }
    public boolean isLeavingMarks() { return leavingMarks; }
    
    public boolean isFront() {
        return position == Position.FRONT_LEFT || position == Position.FRONT_RIGHT;
    }
    
    public boolean isRear() {
        return position == Position.REAR_LEFT || position == Position.REAR_RIGHT;
    }
    
    // Upgrade modifiers
    public void setGripModifier(double mod) { this.gripModifier = mod; }
    public void setDurabilityModifier(double mod) { this.durabilityModifier = mod; }
    public double getGripModifier() { return gripModifier; }
    public double getDurabilityModifier() { return durabilityModifier; }
    public void resetWear() { this.wear = 0; }
    
    // Drift grip control - allows VehiclePhysics to reduce rear grip during oversteer
    public void setDriftGripMultiplier(double mult) { this.driftGripMultiplier = MathUtils.clamp(mult, 0.1, 1.0); }
    public double getDriftGripMultiplier() { return driftGripMultiplier; }
}
