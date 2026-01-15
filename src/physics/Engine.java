package physics;

import util.GameConstants;
import util.MathUtils;

/**
 * Simulates realistic engine behavior
 * Handles RPM, torque curves, power delivery
 * 
 * SFX NOTE: Engine sound implementation guide:
 * - Main engine sound should be a continuous loop with pitch based on RPM
 *   - Pitch formula: basePitch * (0.5 + 0.5 * getRpmPercentage())
 *   - Volume: Louder with throttle, quieter when coasting
 * - Rev limiter: When isRevLimiterActive() is true, add a crackling/popping layer
 * - Gear shifts: shiftUp()/shiftDown() should trigger a brief rev-match sound
 * - Idle: Special low rumble when RPM is near IDLE_RPM
 */
public class Engine {
    
    private double rpm;
    private double throttle;        // 0.0 to 1.0
    private int currentGear;        // 0 = neutral, 1-6 = forward gears, -1 = reverse
    private double clutch;          // 0.0 = disengaged, 1.0 = fully engaged
    
    // Engine state
    private double horsepower;
    private double peakTorque;
    private double currentTorque;
    private double currentPower;
    
    // Bogging state (too low RPM for gear)
    private boolean isBogging;
    private double bogIntensity; // 0-1 how badly bogged
    
    // Rev limiter
    private boolean revLimiterActive;
    private double revLimiterTimer;
    
    // Rev limiter bounce effect - creates visual needle bounce when hitting limiter
    private double revLimiterBounce;     // Current bounce offset (0 to 1)
    private double revLimiterBounceVel;  // Bounce velocity for oscillation
    private int bounceCount;             // Track number of bounces for dampening
    
    public Engine() {
        this.rpm = GameConstants.IDLE_RPM;
        this.throttle = 0;
        this.currentGear = 1;
        this.clutch = 1.0;
        this.horsepower = GameConstants.ENGINE_HORSEPOWER;
        this.peakTorque = GameConstants.ENGINE_TORQUE;
        this.isBogging = false;
        this.bogIntensity = 0;
        this.revLimiterActive = false;
        this.revLimiterTimer = 0;
        this.revLimiterBounce = 0;
        this.revLimiterBounceVel = 0;
        this.bounceCount = 0;
    }
    
    /**
     * Update engine state
     * @param dt delta time
     * @param wheelSpeed current wheel rotation speed (rad/s)
     */
    public void update(double dt, double wheelSpeed) {
        // Handle rev limiter
        if (revLimiterActive) {
            revLimiterTimer -= dt;
            if (revLimiterTimer <= 0) {
                revLimiterActive = false;
            }
        }
        
        // Calculate target RPM from wheel speed (what RPM would be if fully connected)
        double gearRatio = getGearRatio();
        double wheelRpm = Math.abs(wheelSpeed / (2 * Math.PI)) * 60;
        double targetRpmFromWheels = wheelRpm * Math.abs(gearRatio) * GameConstants.FINAL_DRIVE_RATIO;
        
        // When clutch is engaged and in gear
        if (clutch > 0.5 && currentGear != 0) {
            // RPM is directly tied to wheel speed through gearbox (mechanical connection)
            // The wheels report their actual angular velocity including any wheelspin
            // So if wheels are spinning from throttle, RPM will naturally be higher
            rpm = targetRpmFromWheels;
            
            // At very low wheel speeds with throttle, simulate clutch slip to prevent stalling
            if (targetRpmFromWheels < GameConstants.IDLE_RPM && throttle > 0.1) {
                // Clutch slipping - engine can rev independently
                double slipRpm = GameConstants.IDLE_RPM + throttle * 2500;
                rpm = Math.max(rpm, slipRpm);
            }
            
            // Engine can't go below idle when clutch is engaged
            if (rpm < GameConstants.IDLE_RPM) {
                rpm = GameConstants.IDLE_RPM;
            }
        } else {
            // Neutral or clutch disengaged - RPM based on throttle (can rev freely)
            double targetRpm = GameConstants.IDLE_RPM + throttle * (GameConstants.REDLINE_RPM - GameConstants.IDLE_RPM) * 0.85;
            double rpmChangeRate = 12000 * dt; // Fast rev response when not connected
            rpm = MathUtils.approach(rpm, targetRpm, rpmChangeRate);
        }
        
        // Re-engage clutch smoothly after shift
        if (clutch < 1.0) {
            clutch = MathUtils.approach(clutch, 1.0, 3.0 * dt);
        }
        
        // Detect bogging - RPM near idle while trying to accelerate in high gear
        // Made less sensitive to prevent rapid flickering of the indicator
        double bogThreshold = GameConstants.IDLE_RPM + 300; // Closer to idle
        if (rpm < bogThreshold && throttle > 0.5 && currentGear >= 4) {
            // Smoothly increase bog intensity
            double targetBog = (bogThreshold - rpm) / 300.0;
            targetBog = MathUtils.clamp(targetBog, 0, 1);
            bogIntensity = MathUtils.approach(bogIntensity, targetBog, dt * 3.0);
            isBogging = bogIntensity > 0.3; // Only show indicator when significantly bogging
        } else {
            // Smoothly decrease bog intensity
            bogIntensity = MathUtils.approach(bogIntensity, 0, dt * 5.0);
            isBogging = bogIntensity > 0.3;
        }
        
        // Check rev limiter - soft limiter that just holds RPM steady
        if (rpm >= GameConstants.REV_LIMITER_RPM) {
            revLimiterActive = true;
            revLimiterTimer = 0.016; // 16ms - one frame, very smooth
            rpm = GameConstants.REV_LIMITER_RPM - 50; // Hold just below limiter
            
            // No bounce - just hold steady
            revLimiterBounce = 0;
            revLimiterBounceVel = 0;
        }
        
        // Update rev limiter bounce animation (spring-damper system)
        if (revLimiterBounce != 0 || revLimiterBounceVel != 0) {
            // Spring force pulls toward 0, damping slows oscillation
            double springForce = -revLimiterBounce * 150.0; // Spring stiffness
            double dampingForce = -revLimiterBounceVel * 8.0; // Damping
            revLimiterBounceVel += (springForce + dampingForce) * dt;
            revLimiterBounce += revLimiterBounceVel * dt;
            
            // Count bounces and stop when settled
            if (revLimiterBounce < -0.02 && revLimiterBounceVel > 0) {
                bounceCount++;
            }
            if (bounceCount > 3 || (Math.abs(revLimiterBounce) < 0.005 && Math.abs(revLimiterBounceVel) < 0.1)) {
                revLimiterBounce = 0;
                revLimiterBounceVel = 0;
            }
            // Clamp bounce to reasonable range
            revLimiterBounce = MathUtils.clamp(revLimiterBounce, -0.15, 0.05);
        }
        
        // Clamp RPM
        rpm = MathUtils.clamp(rpm, GameConstants.IDLE_RPM, GameConstants.MAX_RPM);
        
        // Calculate torque from torque curve
        currentTorque = calculateTorque();
        
        // Calculate power: P = (Torque * RPM) / 5252
        currentPower = (currentTorque * rpm) / 5252;
    }
    
    /**
     * Realistic torque curve simulation
     * Peak torque around 4500 RPM, falls off at high RPM
     * Torque drastically reduced when bogging or at rev limiter
     */
    private double calculateTorque() {
        // No torque when rev limiter is cutting fuel
        if (revLimiterActive) {
            return 0;
        }
        
        double rpmNormalized = (rpm - GameConstants.IDLE_RPM) / 
                               (GameConstants.REDLINE_RPM - GameConstants.IDLE_RPM);
        
        // Torque curve shape (peaks around 0.5 normalized RPM)
        double torqueCurve;
        if (rpmNormalized < 0.5) {
            // Rising torque
            torqueCurve = 0.6 + 0.4 * (rpmNormalized / 0.5);
        } else {
            // Falling torque after peak
            torqueCurve = 1.0 - 0.25 * ((rpmNormalized - 0.5) / 0.5);
        }
        
        // Apply throttle
        double torque = peakTorque * torqueCurve * throttle;
        
        // Apply clutch
        torque *= clutch;
        
        // Drastically reduce torque when bogging (engine can't produce power at low RPM)
        if (isBogging) {
            torque *= (1.0 - bogIntensity * 0.8); // Up to 80% power loss when badly bogged
        }
        
        return torque;
    }
    
    /**
     * Get current gear ratio
     */
    public double getGearRatio() {
        if (currentGear == -1) {
            return GameConstants.REVERSE_GEAR_RATIO;
        }
        if (currentGear >= 0 && currentGear < GameConstants.GEAR_RATIOS.length) {
            return GameConstants.GEAR_RATIOS[currentGear];
        }
        return 0;
    }
    
    /**
     * Get wheel torque after gearing
     */
    public double getWheelTorque() {
        if (currentGear == 0) return 0;
        return currentTorque * Math.abs(getGearRatio()) * GameConstants.FINAL_DRIVE_RATIO;
    }
    
    /**
     * Get drive force at wheels
     */
    public double getDriveForce() {
        return getWheelTorque() / GameConstants.TIRE_RADIUS;
    }
    
    public void shiftUp() {
        if (currentGear < GameConstants.GEAR_RATIOS.length - 1) {
            currentGear++;
            // Simulate clutch kick
            clutch = 0.3;
        }
    }
    
    public void shiftDown() {
        if (currentGear > 1) {
            currentGear--;
            clutch = 0.3;
        }
    }
    
    public void setReverse() {
        if (currentGear != -1) {
            currentGear = -1;
        }
    }
    
    public void setNeutral() {
        currentGear = 0;
    }
    
    public void setThrottle(double value) {
        this.throttle = MathUtils.clamp(value, 0, 1);
    }
    
    public void setClutch(double value) {
        this.clutch = MathUtils.clamp(value, 0, 1);
    }
    
    // Getters
    public double getRpm() { return rpm; }
    public double getThrottle() { return throttle; }
    public int getCurrentGear() { return currentGear; }
    public double getCurrentTorque() { return currentTorque; }
    public double getCurrentPower() { return currentPower; }
    public double getClutch() { return clutch; }
    public boolean isRevLimiterActive() { return revLimiterActive; }
    public boolean isBogging() { return isBogging; }
    public double getBogIntensity() { return bogIntensity; }
    
    /**
     * Calculate theoretical top speed for current gear (in m/s)
     * This is the speed where engine hits redline in this gear
     */
    public double getGearTopSpeed() {
        if (currentGear <= 0) return 0;
        // At redline RPM, what wheel speed do we get?
        double redlineWheelRpm = GameConstants.REDLINE_RPM / (Math.abs(getGearRatio()) * GameConstants.FINAL_DRIVE_RATIO);
        double redlineWheelRadPerSec = redlineWheelRpm * 2 * Math.PI / 60;
        return redlineWheelRadPerSec * GameConstants.TIRE_RADIUS;
    }
    
    /**
     * Get the rev limiter bounce offset for visual effects.
     * Returns a value typically between -0.15 and 0.05 representing
     * needle bounce when hitting the rev limiter.
     * @return bounce offset to apply to RPM gauge needle
     */
    public double getRevLimiterBounce() { return revLimiterBounce; }
    
    /**
     * Get RPM as percentage of rev limiter (for gauge display)
     */
    public double getRpmPercentage() {
        return (rpm - GameConstants.IDLE_RPM) / (GameConstants.REV_LIMITER_RPM - GameConstants.IDLE_RPM);
    }
}
