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
        
        // Update visual effects
        updateEffects();
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
        double baseGrip = GameConstants.BASE_TIRE_GRIP;
        
        // Temperature factor - optimal around 90°C
        double tempDiff = Math.abs(temperature - GameConstants.TIRE_TEMP_OPTIMAL);
        double tempFactor = 1.0 - (tempDiff / 100) * 0.3;
        tempFactor = MathUtils.clamp(tempFactor, 0.5, 1.0);
        
        // Pressure factor - optimal at 32 PSI
        double pressureDiff = Math.abs(pressure - GameConstants.OPTIMAL_TIRE_PRESSURE);
        double pressureFactor = 1.0 - (pressureDiff / 20) * 0.2;
        pressureFactor = MathUtils.clamp(pressureFactor, 0.7, 1.0);
        
        // Wear factor
        double wearFactor = 1.0 - wear * 0.4;
        
        grip = baseGrip * tempFactor * pressureFactor * wearFactor;
    }
    
    /**
     * Calculate longitudinal slip ratio
     * Slip = (wheel speed - vehicle speed) / max(wheel speed, vehicle speed)
     * 
     * In a real car:
     * - No throttle = wheels match vehicle speed (road friction syncs them)
     * - Throttle = wheels can spin faster than vehicle (wheelspin)
     * - Braking = wheels can spin slower than vehicle (lockup)
     */
    private void calculateSlipRatio(double vehicleSpeed, double driveForce, 
                                     double brakeForce, double dt) {
        // Target wheel speed should match vehicle speed when coasting
        double targetWheelAngularVel = vehicleSpeed / GameConstants.TIRE_RADIUS;
        
        // Calculate torque on wheel from engine
        double wheelTorque = driveForce * GameConstants.TIRE_RADIUS;
        
        // Brake torque opposes rotation
        if (angularVelocity > 0) {
            wheelTorque -= brakeForce * GameConstants.TIRE_RADIUS;
        } else if (angularVelocity < 0) {
            wheelTorque += brakeForce * GameConstants.TIRE_RADIUS;
        }
        
        // Check if coasting (no significant drive force or brake)
        boolean isCoasting = Math.abs(driveForce) < 50 && Math.abs(brakeForce) < 50;
        
        if (isCoasting) {
            // When coasting, wheels naturally sync to road speed due to friction
            // This is what happens in a real car - the road "pulls" the wheels
            double syncRate = 5.0; // How fast wheels sync to road (higher = faster)
            angularVelocity = MathUtils.approach(angularVelocity, targetWheelAngularVel, syncRate * dt);
        } else {
            // Wheel inertia (simplified)
            double wheelInertia = 1.5; // kg*m² - slightly higher for more realistic response
            double angularAccel = wheelTorque / wheelInertia;
            
            // Update angular velocity
            angularVelocity += angularAccel * dt;
            
            // Road friction tries to sync wheel to vehicle speed (opposes slip)
            // This prevents runaway wheelspin
            double slipCorrection = (targetWheelAngularVel - angularVelocity) * grip * 2.0;
            angularVelocity += slipCorrection * dt;
        }
        
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
        isLocked = slipRatio < -GameConstants.SLIP_RATIO_THRESHOLD && brakeForce > 100;
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
     */
    private void calculateForces() {
        // Pacejka Magic Formula parameters (simplified)
        double B = 10;  // Stiffness factor
        double C = 1.9; // Shape factor
        double D = grip * normalForce; // Peak value
        double E = 0.97; // Curvature factor
        
        // Longitudinal force from slip ratio
        double slipRatioRad = slipRatio * Math.PI;
        longitudinalForce = D * Math.sin(C * Math.atan(B * slipRatioRad - 
                            E * (B * slipRatioRad - Math.atan(B * slipRatioRad))));
        
        // Lateral force from slip angle
        double slipAngleRad = MathUtils.toRadians(slipAngle);
        lateralForce = D * Math.sin(C * Math.atan(B * slipAngleRad - 
                       E * (B * slipAngleRad - Math.atan(B * slipAngleRad))));
        
        // Reduce forces when heavily sliding (friction circle)
        double totalSlip = Math.sqrt(slipRatio * slipRatio + 
                           Math.pow(slipAngle / 90, 2));
        if (totalSlip > 1) {
            double reduction = 1.0 / totalSlip;
            longitudinalForce *= reduction;
            lateralForce *= reduction;
        }
        
        // Apply grip falloff when sliding heavily
        if (Math.abs(slipAngle) > GameConstants.SLIP_ANGLE_DRIFT) {
            double falloff = 1.0 - GameConstants.TIRE_GRIP_FALLOFF * 
                            (Math.abs(slipAngle) - GameConstants.SLIP_ANGLE_DRIFT) / 45;
            falloff = MathUtils.clamp(falloff, 0.4, 1.0);
            lateralForce *= falloff;
        }
    }
    
    /**
     * Update visual effects (smoke, tire marks)
     */
    private void updateEffects() {
        double totalSlip = Math.abs(slipRatio) + Math.abs(slipAngle) / 45;
        
        // Smoke when slipping
        if (totalSlip > GameConstants.SMOKE_THRESHOLD_SLIP) {
            smokeIntensity = MathUtils.clamp(totalSlip - GameConstants.SMOKE_THRESHOLD_SLIP, 0, 1);
        } else {
            smokeIntensity = 0;
        }
        
        // Leave tire marks when sliding
        leavingMarks = totalSlip > 0.15;
        
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
}
