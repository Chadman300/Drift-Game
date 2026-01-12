package physics;

import util.GameConstants;
import util.MathUtils;
import util.Vector2D;

/**
 * Complete vehicle physics simulation
 * Combines engine, tires, and chassis physics for realistic drifting
 */
public class VehiclePhysics {
    
    // Position and orientation
    private Vector2D position;
    private double rotation;        // Radians, 0 = facing right
    
    // Velocity
    private Vector2D velocity;
    private double angularVelocity; // Rotation speed (rad/s)
    
    // Components
    private Engine engine;
    private Tire[] tires;
    
    // Input state
    private double throttleInput;   // 0 to 1
    private double brakeInput;      // 0 to 1
    private double steeringInput;   // -1 to 1
    private boolean handbrakeActive;
    
    // Current steering angle (smoothed)
    private double steeringAngle;   // Radians
    
    // Drift state
    private boolean isDrifting;
    private double driftAngle;      // Degrees
    private double driftTime;       // How long in current drift
    
    // Physics state
    private double speed;           // m/s
    private double speedMph;        // miles per hour
    private double lateralG;        // Lateral acceleration in G's
    
    // Weight distribution
    private double frontWeight;     // Percentage on front
    private double rearWeight;      // Percentage on rear
    
    public VehiclePhysics(double startX, double startY) {
        this.position = new Vector2D(startX, startY);
        this.rotation = 0;
        this.velocity = new Vector2D(0, 0);
        this.angularVelocity = 0;
        
        this.engine = new Engine();
        this.tires = new Tire[] {
            new Tire(Tire.Position.FRONT_LEFT),
            new Tire(Tire.Position.FRONT_RIGHT),
            new Tire(Tire.Position.REAR_LEFT),
            new Tire(Tire.Position.REAR_RIGHT)
        };
        
        this.throttleInput = 0;
        this.brakeInput = 0;
        this.steeringInput = 0;
        this.handbrakeActive = false;
        this.steeringAngle = 0;
        
        this.isDrifting = false;
        this.driftAngle = 0;
        this.driftTime = 0;
        
        this.frontWeight = 0.5;
        this.rearWeight = 0.5;
    }
    
    /**
     * Main physics update
     */
    public void update(double dt) {
        // Update steering with smoothing
        updateSteering(dt);
        
        // Calculate weight distribution
        calculateWeightTransfer();
        
        // Update engine
        double avgRearWheelSpeed = (tires[2].getAngularVelocity() + tires[3].getAngularVelocity()) / 2;
        engine.update(dt, avgRearWheelSpeed);
        
        // Calculate forces
        Vector2D totalForce = new Vector2D(0, 0);
        double totalTorque = 0;
        
        // Get local velocities
        Vector2D forward = Vector2D.fromAngle(rotation);
        Vector2D right = forward.perpendicular();
        double forwardSpeed = velocity.dot(forward);
        double lateralSpeed = velocity.dot(right);
        
        // Update each tire and accumulate forces
        double driveForce = engine.getDriveForce() / 2; // Split between rear wheels
        
        for (int i = 0; i < tires.length; i++) {
            Tire tire = tires[i];
            
            // Calculate tire position relative to car center
            Vector2D tireOffset = getTireOffset(tire.getPosition());
            
            // Calculate lateral velocity at this tire (includes rotation effect)
            double tireLateralVel = lateralSpeed + angularVelocity * tireOffset.x;
            
            // Steering angle (only front wheels steer)
            double tireSteerAngle = tire.isFront() ? steeringAngle : 0;
            
            // Brake force distribution
            double brakeForceTire;
            if (handbrakeActive && tire.isRear()) {
                brakeForceTire = GameConstants.HANDBRAKE_FORCE;
            } else {
                double brakeBias = tire.isFront() ? GameConstants.BRAKE_BIAS_FRONT : 
                                                    (1 - GameConstants.BRAKE_BIAS_FRONT);
                brakeForceTire = brakeInput * GameConstants.MAX_BRAKE_FORCE * brakeBias / 2;
            }
            
            // Drive force (RWD - only rear wheels)
            // Engine's getDriveForce() already includes throttle input through torque calculation
            // When rev limiter is active, this will be 0
            double tireDriveForce = tire.isRear() ? driveForce : 0;
            
            // Update tire physics
            tire.update(dt, forwardSpeed, tireDriveForce, brakeForceTire, 
                       tireSteerAngle, tireLateralVel);
            
            // Get forces from tire and transform to world space
            Vector2D tireForward = Vector2D.fromAngle(rotation + tireSteerAngle);
            Vector2D tireRight = tireForward.perpendicular();
            
            // Only apply tire forces if car is moving - steering shouldn't add force when stationary
            double speed = velocity.magnitude();
            Vector2D tireForce;
            if (speed < 0.5) {
                // When nearly stopped, only allow longitudinal force (engine/brake) - no lateral from steering
                tireForce = tireForward.multiply(tire.getLongitudinalForce());
            } else {
                tireForce = tireForward.multiply(tire.getLongitudinalForce())
                                     .add(tireRight.multiply(-tire.getLateralForce()));
            }
            
            totalForce = totalForce.add(tireForce);
            
            // Calculate torque from this tire (force x distance from center)
            Vector2D worldTirePos = tireOffset.rotate(rotation);
            totalTorque += worldTirePos.cross(tireForce);
        }
        
        // Aerodynamic drag (quadratic - increases with speed squared)
        // Reduced to prevent overly fast deceleration at high speeds
        double quadraticDrag = 0.5 * GameConstants.AIR_DENSITY * GameConstants.DRAG_COEFFICIENT * 
                          GameConstants.FRONTAL_AREA * speed * speed;
        
        // Linear drag for more consistent deceleration feel across all speeds
        double linearDrag = GameConstants.LINEAR_DRAG * speed;
        
        // Combined drag force
        double totalDrag = quadraticDrag + linearDrag;
        Vector2D drag = velocity.normalize().multiply(-totalDrag);
        totalForce = totalForce.add(drag);
        
        // Rolling resistance - always active, proportional to speed
        if (speed > 0.1) {
            double rollingForce = GameConstants.CAR_MASS * GameConstants.GRAVITY * 
                                  GameConstants.ROLLING_RESISTANCE;
            // Rolling resistance increases slightly with speed
            rollingForce *= (1.0 + speed * 0.01);
            Vector2D rolling = velocity.normalize().multiply(-rollingForce);
            totalForce = totalForce.add(rolling);
        }
        
        // Ground friction - brings car to rest when coasting
        // This is the friction between tires and road when not accelerating
        if (speed > 0.1 && throttleInput < 0.1) {
            double groundFriction = GameConstants.CAR_MASS * GameConstants.GRAVITY * 
                                    GameConstants.GROUND_FRICTION;
            // Stronger friction when fully stopped (static vs kinetic)
            if (speed < 2.0) {
                groundFriction *= 2.0; // Extra friction at very low speeds to stop completely
            }
            Vector2D frictionForce = velocity.normalize().multiply(-groundFriction);
            totalForce = totalForce.add(frictionForce);
        }
        
        // Lateral friction - resist sideways sliding (like real tires scrubbing)
        // When sliding without throttle, tires grip the road and slow the car
        double lateralVel = velocity.dot(right);
        if (Math.abs(lateralVel) > 0.1) {
            // Friction coefficient - MUCH higher when not applying throttle (tires can grip better)
            double frictionCoef = (throttleInput < 0.1 && !handbrakeActive) ? 4.0 : 1.0;
            
            // Lateral friction force opposes sideways motion
            // F = μ * N where N is weight on tires
            double lateralFrictionForce = frictionCoef * GameConstants.CAR_MASS * GameConstants.GRAVITY * 0.5;
            
            // Apply friction force opposing lateral velocity - stronger to stop sliding faster
            double lateralDragMag = Math.min(lateralFrictionForce, Math.abs(lateralVel) * GameConstants.CAR_MASS * 5);
            Vector2D lateralDrag = right.multiply(-Math.signum(lateralVel) * lateralDragMag);
            totalForce = totalForce.add(lateralDrag);
        }
        
        // Engine braking when coasting (no throttle, in gear) - like a real manual transmission
        // Apply based on actual speed, not just forward speed, so car slows after drifts
        if (throttleInput < 0.1 && speed > 0.5 && !handbrakeActive) {
            // Engine braking force depends on current gear - lower gears = stronger braking
            int currentGear = engine.getCurrentGear();
            double gearRatio = Math.abs(engine.getGearRatio());
            // More engine braking in lower gears (higher gear ratio)
            double engineBraking = 800 + gearRatio * 600; // Base + gear-dependent
            if (speed < 8) {
                engineBraking *= 1.5; // Extra strong at low speeds to stop the car
            }
            // Apply engine braking in direction opposing velocity (not just forward)
            Vector2D engineBrakingForce = velocity.normalize().multiply(-engineBraking);
            totalForce = totalForce.add(engineBrakingForce);
        }
        
        // Apply forces (F = ma)
        Vector2D acceleration = totalForce.divide(GameConstants.CAR_MASS);
        velocity = velocity.add(acceleration.multiply(dt));
        
        // Force car to stop at very low speeds when no throttle (prevents drifting at crawl speed)
        if (speed < 0.3 && throttleInput < 0.1) {
            velocity = new Vector2D(0, 0);
        }
        
        // Apply rotation (simplified moment of inertia)
        double momentOfInertia = GameConstants.CAR_MASS * GameConstants.CAR_LENGTH * 
                                 GameConstants.CAR_LENGTH / 12;
        double angularAccel = totalTorque / momentOfInertia;
        angularVelocity += angularAccel * dt;
        
        // Damping on angular velocity - much stronger when not on throttle to stop spinning
        if (throttleInput < 0.1) {
            angularVelocity *= 0.90; // Strong damping when coasting - car straightens out
        } else {
            angularVelocity *= 0.97; // Light damping when on throttle
        }
        
        // Low-speed Ackermann steering (geometric steering at low speeds)
        // This provides direct rotation based on steering angle when moving slowly
        if (Math.abs(forwardSpeed) > 0.5 && Math.abs(steeringAngle) > 0.01) {
            double lowSpeedFactor = MathUtils.clamp(1.0 - speed / 15, 0, 1);
            // Turn radius = wheelbase / tan(steeringAngle)
            double turnRadius = GameConstants.WHEELBASE / Math.tan(Math.abs(steeringAngle) + 0.001);
            double geometricAngularVel = forwardSpeed / turnRadius * Math.signum(steeringAngle);
            angularVelocity = angularVelocity * (1 - lowSpeedFactor) + geometricAngularVel * lowSpeedFactor;
        }
        
        // Update position and rotation
        position = position.add(velocity.multiply(dt));
        rotation += angularVelocity * dt;
        rotation = MathUtils.normalizeAngle(rotation);
        
        // Calculate derived values
        speed = velocity.magnitude();
        speedMph = speed * 2.237; // m/s to mph
        lateralG = Math.abs(lateralSpeed * angularVelocity) / GameConstants.GRAVITY;
        
        // Calculate drift state
        updateDriftState(dt, forwardSpeed);
    }
    
    /**
     * Smooth steering input to angle
     */
    private void updateSteering(double dt) {
        double targetAngle = steeringInput * MathUtils.toRadians(GameConstants.MAX_STEERING_ANGLE);
        
        // Speed-dependent steering reduction (less aggressive for better control)
        double speedFactor = 1.0 - MathUtils.clamp(speed / 80, 0, 0.5);
        targetAngle *= speedFactor;
        
        // Countersteer assist when drifting
        if (isDrifting && GameConstants.COUNTERSTEER_ASSIST > 0) {
            double counterSteer = -Math.signum(driftAngle) * 
                                  MathUtils.toRadians(Math.abs(driftAngle) * 0.3);
            targetAngle += counterSteer * GameConstants.COUNTERSTEER_ASSIST;
        }
        
        // Smooth transition
        double steerSpeed = GameConstants.STEERING_SPEED;
        if (Math.abs(steeringInput) < 0.1) {
            steerSpeed = GameConstants.STEERING_RETURN_SPEED;
        }
        
        steeringAngle = MathUtils.approach(steeringAngle, targetAngle, steerSpeed * dt);
    }
    
    /**
     * Calculate weight transfer based on acceleration
     */
    private void calculateWeightTransfer() {
        // Longitudinal weight transfer (acceleration/braking)
        double longitudinalAccel = throttleInput - brakeInput;
        double weightShift = longitudinalAccel * 0.1;
        
        rearWeight = 0.5 + weightShift;
        frontWeight = 0.5 - weightShift;
        
        // Calculate normal force on each tire
        double totalWeight = GameConstants.CAR_MASS * GameConstants.GRAVITY;
        double frontAxleWeight = totalWeight * frontWeight;
        double rearAxleWeight = totalWeight * rearWeight;
        
        tires[0].setNormalForce(frontAxleWeight / 2);
        tires[1].setNormalForce(frontAxleWeight / 2);
        tires[2].setNormalForce(rearAxleWeight / 2);
        tires[3].setNormalForce(rearAxleWeight / 2);
    }
    
    /**
     * Get offset of tire from car center
     */
    private Vector2D getTireOffset(Tire.Position pos) {
        double x = 0, y = 0;
        
        switch (pos) {
            case FRONT_LEFT:
                x = GameConstants.WHEELBASE / 2;
                y = -GameConstants.TRACK_WIDTH / 2;
                break;
            case FRONT_RIGHT:
                x = GameConstants.WHEELBASE / 2;
                y = GameConstants.TRACK_WIDTH / 2;
                break;
            case REAR_LEFT:
                x = -GameConstants.WHEELBASE / 2;
                y = -GameConstants.TRACK_WIDTH / 2;
                break;
            case REAR_RIGHT:
                x = -GameConstants.WHEELBASE / 2;
                y = GameConstants.TRACK_WIDTH / 2;
                break;
        }
        
        return new Vector2D(x, y);
    }
    
    /**
     * Update drift detection
     */
    private void updateDriftState(double dt, double forwardSpeed) {
        // Calculate angle between velocity and car heading
        if (speed > 3) {
            double velocityAngle = velocity.angle();
            double angleDiff = MathUtils.normalizeAngle(velocityAngle - rotation);
            driftAngle = MathUtils.toDegrees(angleDiff);
            
            // Check if drifting - require significant angle AND speed AND throttle input
            // Real drifting requires maintaining throttle to keep rear wheels spinning
            boolean wasDrifting = isDrifting;
            isDrifting = Math.abs(driftAngle) > 15 && speed > 8 && throttleInput > 0.2;
            
            if (isDrifting) {
                if (wasDrifting) {
                    driftTime += dt;
                } else {
                    driftTime = 0;
                }
            } else {
                driftTime = 0;
            }
        } else {
            driftAngle = 0;
            isDrifting = false;
            driftTime = 0;
        }
    }
    
    // Input methods
    public void setThrottle(double value) { 
        this.throttleInput = MathUtils.clamp(value, 0, 1);
        engine.setThrottle(throttleInput);
    }
    
    public void setBrake(double value) { 
        this.brakeInput = MathUtils.clamp(value, 0, 1); 
    }
    
    public void setSteering(double value) { 
        this.steeringInput = MathUtils.clamp(value, -1, 1); 
    }
    
    public void setHandbrake(boolean active) { 
        this.handbrakeActive = active; 
    }
    
    public void shiftUp() { engine.shiftUp(); }
    public void shiftDown() { engine.shiftDown(); }
    
    // Getters
    public Vector2D getPosition() { return position; }
    public double getRotation() { return rotation; }
    public double getRotationDegrees() { return MathUtils.toDegrees(rotation); }
    public Vector2D getVelocity() { return velocity; }
    public double getSpeed() { return speed; }
    public double getSpeedMph() { return speedMph; }
    public double getAngularVelocity() { return angularVelocity; }
    public Engine getEngine() { return engine; }
    public Tire[] getTires() { return tires; }
    public boolean isDrifting() { return isDrifting; }
    public double getDriftAngle() { return driftAngle; }
    public double getDriftTime() { return driftTime; }
    public double getSteeringAngle() { return steeringAngle; }
    public double getSteeringAngleDegrees() { return MathUtils.toDegrees(steeringAngle); }
    public double getLateralG() { return lateralG; }
    public boolean isHandbrakeActive() { return handbrakeActive; }
    
    /**
     * Check if any rear tire is spinning
     */
    public boolean isWheelspinning() {
        return tires[2].isSpinning() || tires[3].isSpinning();
    }
    
    /**
     * Get average rear tire slip
     */
    public double getRearSlip() {
        return (Math.abs(tires[2].getSlipRatio()) + Math.abs(tires[3].getSlipRatio())) / 2;
    }
}
