package game;

import graphics.ParticleSystem;
import physics.Tire;
import physics.VehiclePhysics;
import util.GameConstants;
import util.Vector2D;

/**
 * Main car entity that combines physics, input, and visual effects
 * SFX NOTE: This class triggers most of the car-related sound effects
 */
public class Car {
    
    private VehiclePhysics physics;
    private ParticleSystem particles;
    
    // Input state
    private double throttle;
    private double brake;
    private double steering;
    private boolean handbrake;
    
    // State flags
    private boolean isAccelerating;
    private boolean isBraking;
    private boolean isReversing;
    
    // Smoke emission timer
    private double smokeTimer;
    private static final double SMOKE_INTERVAL = 0.03;
    
    // Backfire state - tracks throttle changes for exhaust pops
    private double previousThrottle;
    private double backfireTimer;
    private static final double BACKFIRE_COOLDOWN = 0.15; // Min time between backfires
    
    public Car(double x, double y, ParticleSystem particles) {
        this.physics = new VehiclePhysics(x, y);
        this.particles = particles;
        this.smokeTimer = 0;
        this.previousThrottle = 0;
        this.backfireTimer = 0;
    }
    
    /**
     * Update car state
     * SFX NOTE: Engine sound should be continuous, pitch based on RPM
     */
    public void update(double dt, double throttleInput, double brakeInput, 
                       double steeringInput, boolean handbrakeInput) {
        // Store previous throttle for backfire detection
        double throttleDelta = throttleInput - previousThrottle;
        previousThrottle = throttleInput;
        
        // Store inputs
        this.throttle = throttleInput;
        this.brake = brakeInput;
        this.steering = steeringInput;
        this.handbrake = handbrakeInput;
        
        // Update state flags
        this.isAccelerating = throttle > 0.1;
        this.isBraking = brake > 0.1 || handbrake;
        this.isReversing = false; // Reverse disabled - S is now brake only
        
        // S key is brakes only - no automatic reverse
        // Exit reverse gear if in it and pressing throttle
        if (physics.getEngine().getCurrentGear() == -1 && throttle > 0.1) {
            physics.getEngine().shiftUp(); // Go back to 1st gear
        }
        physics.setThrottle(throttle);
        physics.setBrake(brake);
        
        physics.setSteering(steering);
        physics.setHandbrake(handbrake);
        
        // Update physics
        physics.update(dt);
        
        // Manual transmission - player controls gear shifts (no auto shift)
        
        // Emit tire smoke
        updateTireEffects(dt);
        
        // Check for backfire conditions
        updateBackfire(dt, throttleDelta);
    }
    
    /**
     * Check and trigger exhaust backfire effect
     * Backfire occurs when lifting off throttle at high RPM
     * SFX NOTE: Play loud pop/crackle sound when backfire triggers
     */
    private void updateBackfire(double dt, double throttleDelta) {
        backfireTimer -= dt;
        
        double rpm = physics.getEngine().getRpm();
        double rpmPercent = rpm / GameConstants.REDLINE_RPM;
        
        // Conditions for backfire:
        // 1. Quick throttle lift-off (negative throttle delta)
        // 2. High RPM (above 60% of redline)
        // 3. Not on cooldown
        // 4. Moving at decent speed
        boolean quickLiftOff = throttleDelta < -0.3; // Rapid throttle release
        boolean highRpm = rpmPercent > 0.6;
        boolean offCooldown = backfireTimer <= 0;
        boolean hasSpeed = physics.getSpeed() > 5;
        
        if (quickLiftOff && highRpm && offCooldown && hasSpeed) {
            // Calculate exhaust position (behind the car, right side)
            double exhaustOffsetX = -GameConstants.CAR_LENGTH / 2 - 0.1;
            double exhaustOffsetY = GameConstants.CAR_WIDTH / 2 - 0.3; // Right side of car
            
            double cos = Math.cos(physics.getRotation());
            double sin = Math.sin(physics.getRotation());
            double exhaustX = physics.getPosition().x + exhaustOffsetX * cos - exhaustOffsetY * sin;
            double exhaustY = physics.getPosition().y + exhaustOffsetX * sin + exhaustOffsetY * cos;
            
            // Intensity based on RPM and throttle drop speed
            double intensity = rpmPercent * Math.abs(throttleDelta);
            intensity = Math.min(1.0, intensity);
            
            // Spawn backfire effect
            particles.spawnBackfire(exhaustX, exhaustY, physics.getRotation(), intensity);
            
            // Reset cooldown
            backfireTimer = BACKFIRE_COOLDOWN;
            
            // SFX NOTE: Trigger backfire sound here!
            // Sound pitch/volume should scale with 'intensity'
        }
        
        // Also trigger backfire on rev limiter hit
        if (physics.getEngine().isRevLimiterActive() && offCooldown) {
            // Exhaust is at right side of car
            double exhaustOffsetX = -GameConstants.CAR_LENGTH / 2 - 0.1;
            double exhaustOffsetY = GameConstants.CAR_WIDTH / 2 - 0.3; // Right side of car
            
            double cos = Math.cos(physics.getRotation());
            double sin = Math.sin(physics.getRotation());
            double exhaustX = physics.getPosition().x + exhaustOffsetX * cos - exhaustOffsetY * sin;
            double exhaustY = physics.getPosition().y + exhaustOffsetX * sin + exhaustOffsetY * cos;
            
            particles.spawnBackfire(exhaustX, exhaustY, physics.getRotation(), 0.8);
            backfireTimer = BACKFIRE_COOLDOWN * 0.3; // Very short cooldown for continuous pops at rev limiter
            
            // SFX NOTE: Trigger rev limiter pop sound here (slightly different from lift-off backfire)
        }
    }
    
    /**
     * Simple automatic transmission logic
     */
    private void autoShift() {
        double rpm = physics.getEngine().getRpm();
        int gear = physics.getEngine().getCurrentGear();
        
        // Don't auto-shift in reverse or neutral
        if (gear <= 0) return;
        
        // Shift up at high RPM
        if (rpm > GameConstants.REDLINE_RPM * 0.95 && gear < 6) {
            physics.shiftUp();
        }
        
        // Shift down at low RPM (but not if braking hard)
        if (rpm < GameConstants.IDLE_RPM * 1.5 && gear > 1 && brake < 0.5) {
            physics.shiftDown();
        }
    }
    
    /**
     * Handle tire smoke and marks
     */
    private void updateTireEffects(double dt) {
        smokeTimer += dt;
        
        if (smokeTimer >= SMOKE_INTERVAL) {
            smokeTimer = 0;
            
            Tire[] tires = physics.getTires();
            
            for (Tire tire : tires) {
                if (tire.getSmokeIntensity() > 0.1) {
                    // Calculate tire world position
                    Vector2D tirePos = getTireWorldPosition(tire);
                    
                    // Spawn smoke
                    particles.spawnTireSmoke(
                        tirePos.x, tirePos.y,
                        tire.getSmokeIntensity(),
                        physics.getRotation()
                    );
                }
                
                if (tire.isLeavingMarks()) {
                    Vector2D tirePos = getTireWorldPosition(tire);
                    particles.addTireMark(
                        tirePos.x, tirePos.y,
                        physics.getRotation(),
                        0.3 // Mark width
                    );
                }
            }
        }
    }
    
    /**
     * Get world position of a tire
     */
    private Vector2D getTireWorldPosition(Tire tire) {
        double offsetX = 0, offsetY = 0;
        
        switch (tire.getPosition()) {
            case FRONT_LEFT:
                offsetX = GameConstants.WHEELBASE / 2;
                offsetY = -GameConstants.TRACK_WIDTH / 2;
                break;
            case FRONT_RIGHT:
                offsetX = GameConstants.WHEELBASE / 2;
                offsetY = GameConstants.TRACK_WIDTH / 2;
                break;
            case REAR_LEFT:
                offsetX = -GameConstants.WHEELBASE / 2;
                offsetY = -GameConstants.TRACK_WIDTH / 2;
                break;
            case REAR_RIGHT:
                offsetX = -GameConstants.WHEELBASE / 2;
                offsetY = GameConstants.TRACK_WIDTH / 2;
                break;
        }
        
        // Rotate offset by car rotation
        double cos = Math.cos(physics.getRotation());
        double sin = Math.sin(physics.getRotation());
        double worldOffsetX = offsetX * cos - offsetY * sin;
        double worldOffsetY = offsetX * sin + offsetY * cos;
        
        return new Vector2D(
            physics.getPosition().x + worldOffsetX,
            physics.getPosition().y + worldOffsetY
        );
    }
    
    /**
     * Shift up manually
     */
    public void shiftUp() {
        physics.shiftUp();
    }
    
    /**
     * Shift down manually
     */
    public void shiftDown() {
        physics.shiftDown();
    }
    
    /**
     * Trigger clutch kick for drift initiation
     * Creates a sudden torque spike that breaks rear traction
     */
    public void triggerClutchKick() {
        physics.triggerClutchKick();
    }
    
    /**
     * Reset car to position
     */
    public void reset(double x, double y) {
        this.physics = new VehiclePhysics(x, y);
    }
    
    // Getters
    public VehiclePhysics getPhysics() { return physics; }
    public double getThrottle() { return throttle; }
    public double getBrake() { return brake; }
    public double getSteering() { return steering; }
    public boolean isHandbrakeActive() { return handbrake; }
    public boolean isAccelerating() { return isAccelerating; }
    public boolean isBraking() { return isBraking; }
    public boolean isReversing() { return isReversing; }
    
    public Vector2D getPosition() { return physics.getPosition(); }
    public double getRotation() { return physics.getRotation(); }
    public double getSpeed() { return physics.getSpeed(); }
    public double getSpeedMph() { return physics.getSpeedMph(); }
    public boolean isDrifting() { return physics.isDrifting(); }
    public double getDriftAngle() { return physics.getDriftAngle(); }
}
