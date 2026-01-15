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
    private boolean isUndersteering; // Front tires losing grip
    private boolean isOversteering;  // Rear tires sliding out (drifting)
    
    // Per-axle slip angles (key to drift physics)
    private double frontSlipAngle;  // Average front axle slip angle (degrees)
    private double rearSlipAngle;   // Average rear axle slip angle (degrees)
    
    // Yaw rate tracking for counter-steer
    private double yawRate;         // Current rotation rate (rad/s)
    private double targetYawRate;   // What the car "wants" to do
    
    // Feint detection (Scandinavian flick)
    private double previousSteeringInput = 0;
    private double steeringChangeRate = 0;
    private double feintTimer = 0;
    private boolean feintActive = false;
    
    // Clutch kick simulation
    private boolean clutchKickActive = false;
    private double clutchKickTimer = 0;
    
    // Physics state
    private double speed;           // m/s
    private double speedMph;        // miles per hour
    private double lateralG;        // Lateral acceleration in G's
    
    // Weight distribution
    private double frontWeight;     // Percentage on front
    private double rearWeight;      // Percentage on rear
    
    // Throttle lift tracking (for lift-off oversteer)
    private double previousThrottle = 0;
    private double throttleLiftAmount = 0; // How quickly throttle was released
    
    // Upgrade modifiers (set by shop)
    private double steeringModifier = 1.0;
    private double powerModifier = 1.0;
    private double gripModifier = 1.0;
    private double handlingModifier = 1.0;
    private double weightModifier = 1.0;
    private double tireDurability = 1.0;
    private double brakeModifier = 1.0;  // Brake effectiveness
    
    // ABS (Anti-lock Braking System)
    private boolean absActive = false;
    private double absTimer = 0;
    private static final double ABS_CYCLE_TIME = 0.05; // 50ms ABS pulse
    
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
        this.isUndersteering = false;
        this.isOversteering = false;
        
        this.frontSlipAngle = 0;
        this.rearSlipAngle = 0;
        this.yawRate = 0;
        this.targetYawRate = 0;
        
        this.frontWeight = 0.5;
        this.rearWeight = 0.5;
    }
    
    /**
     * REALISTIC DRIFT PHYSICS - Based on Tire Slip Angle Differential
     * 
     * Core concepts:
     * 1. Slip angle = angle between tire heading and velocity direction
     * 2. Rear slip angle > Front slip angle = OVERSTEER (drift)
     * 3. Front slip angle > Rear slip angle = UNDERSTEER
     * 4. Weight transfer affects grip per axle
     * 5. Counter-steering reduces yaw rate to control drift
     * 6. Throttle modulation maintains rear wheel spin during drift
     */
    public void update(double dt) {
        // Update steering with counter-steer physics
        updateSteering(dt);
        
        // Detect feint (Scandinavian flick) for drift initiation
        detectFeint(dt);
        
        // Update clutch kick timer
        updateClutchKick(dt);
        
        // Update ABS timer
        if (absActive) {
            absTimer += dt;
            if (absTimer > ABS_CYCLE_TIME) {
                absTimer = 0;
                absActive = false;
            }
        }
        
        // Calculate weight distribution
        calculateWeightTransfer();
        
        // Update engine
        double avgRearWheelSpeed = (tires[2].getAngularVelocity() + tires[3].getAngularVelocity()) / 2;
        engine.update(dt, avgRearWheelSpeed);
        
        // Get local velocities in car's reference frame
        Vector2D forward = Vector2D.fromAngle(rotation);
        Vector2D right = forward.perpendicular();
        double forwardSpeed = velocity.dot(forward);
        double lateralSpeed = velocity.dot(right);
        speed = velocity.magnitude();
        
        // ==================== CALCULATE PER-AXLE SLIP ANGLES ====================
        // This is the KEY to realistic drift physics
        calculateAxleSlipAngles(forwardSpeed, lateralSpeed);
        
        // ==================== TIRE FORCES ====================
        Vector2D totalForce = new Vector2D(0, 0);
        double totalTorque = 0;
        
        // Drive force with clutch kick boost
        double driveForce = (engine.getDriveForce() * powerModifier) / 2;
        if (clutchKickActive) {
            driveForce *= 1.8; // Torque spike from clutch kick
        }
        
        for (int i = 0; i < tires.length; i++) {
            Tire tire = tires[i];
            Vector2D tireOffset = getTireOffset(tire.getPosition());
            
            // Lateral velocity at this tire includes rotational component
            double tireLateralVel = lateralSpeed + angularVelocity * tireOffset.x;
            
            double tireSteerAngle = tire.isFront() ? steeringAngle : 0;
            
            // Brake force with ABS anti-lock system
            double brakeForceTire;
            if (handbrakeActive && tire.isRear()) {
                // Handbrake bypasses ABS - intentional lockup for drifting
                brakeForceTire = GameConstants.HANDBRAKE_FORCE;
            } else {
                double brakeBias = tire.isFront() ? GameConstants.BRAKE_BIAS_FRONT : (1 - GameConstants.BRAKE_BIAS_FRONT);
                double baseBrakeForce = brakeInput * GameConstants.MAX_BRAKE_FORCE * brakeModifier * brakeBias / 2;
                
                // ABS: Modulate brake force to prevent wheel lockup
                // Check if this tire is about to lock (negative slip ratio)
                if (brakeInput > 0.3 && tire.getSlipRatio() < -0.15 && speed > 3) {
                    // Wheel is locking - reduce brake force (ABS pulse)
                    absActive = true;
                    brakeForceTire = baseBrakeForce * 0.3; // Reduce to 30% during ABS pulse
                } else {
                    // Normal braking - full force available
                    brakeForceTire = baseBrakeForce;
                }
            }
            
            double tireDriveForce = tire.isRear() ? driveForce : 0;
            
            tire.update(dt, forwardSpeed, tireDriveForce, brakeForceTire, tireSteerAngle, tireLateralVel);
            
            // Transform tire forces to world space
            Vector2D tireForward = Vector2D.fromAngle(rotation + tireSteerAngle);
            Vector2D tireRight = tireForward.perpendicular();
            
            Vector2D tireForce;
            if (speed < 0.5) {
                tireForce = tireForward.multiply(tire.getLongitudinalForce());
            } else {
                tireForce = tireForward.multiply(tire.getLongitudinalForce())
                                     .add(tireRight.multiply(-tire.getLateralForce()));
            }
            
            totalForce = totalForce.add(tireForce);
            
            // Torque from this tire
            Vector2D worldTirePos = tireOffset.rotate(rotation);
            totalTorque += worldTirePos.cross(tireForce);
        }
        
        // ==================== DIRECTIONAL FRICTION ====================
        // Wheels roll easily forward/backward but resist lateral movement
        
        // Aerodynamic drag (same in all directions)
        double quadraticDrag = 0.5 * GameConstants.AIR_DENSITY * GameConstants.DRAG_COEFFICIENT * 
                          GameConstants.FRONTAL_AREA * speed * speed;
        if (speed > 0.1) {
            Vector2D drag = velocity.normalize().multiply(-quadraticDrag);
            totalForce = totalForce.add(drag);
        }
        
        // LONGITUDINAL friction (rolling direction) - LOW because wheels roll
        if (Math.abs(forwardSpeed) > 0.1) {
            double rollingFriction = GameConstants.CAR_MASS * GameConstants.GRAVITY * 0.015; // Very low
            Vector2D longitudinalFriction = forward.multiply(-Math.signum(forwardSpeed) * rollingFriction);
            totalForce = totalForce.add(longitudinalFriction);
        }
        
        // LATERAL friction (sideways) - HIGH because tires scrub against road
        if (Math.abs(lateralSpeed) > 0.1) {
            // Strong lateral grip - tires resist sliding sideways
            double lateralFrictionCoef = 0.8; // High friction coefficient
            double lateralFriction = GameConstants.CAR_MASS * GameConstants.GRAVITY * lateralFrictionCoef;
            
            // Scale with lateral speed but cap it
            double lateralDrag = Math.min(lateralFriction, Math.abs(lateralSpeed) * GameConstants.CAR_MASS * 3);
            Vector2D lateralFrictionForce = right.multiply(-Math.signum(lateralSpeed) * lateralDrag);
            totalForce = totalForce.add(lateralFrictionForce);
        }
        
        // Engine braking when coasting
        if (throttleInput < 0.1 && speed > 0.5 && !handbrakeActive) {
            double gearRatio = Math.abs(engine.getGearRatio());
            double engineBraking = 400 + gearRatio * 300;
            if (speed < 5) engineBraking *= 1.5;
            totalForce = totalForce.add(forward.multiply(-Math.signum(forwardSpeed) * engineBraking));
        }
        
        // ==================== APPLY LINEAR FORCES ====================
        double effectiveMass = GameConstants.CAR_MASS * weightModifier;
        Vector2D acceleration = totalForce.divide(effectiveMass);
        velocity = velocity.add(acceleration.multiply(dt));
        
        if (speed < 0.3 && throttleInput < 0.1) {
            velocity = new Vector2D(0, 0);
        }
        
        // ==================== YAW DYNAMICS - THE HEART OF DRIFTING ====================
        double momentOfInertia = effectiveMass * GameConstants.CAR_LENGTH * GameConstants.CAR_LENGTH / 12;
        
        // Base angular acceleration from tire torques
        double angularAccel = totalTorque / momentOfInertia;
        
        // Get grip differential (positive = rear sliding more = oversteer)
        double rearGrip = (tires[2].getDriftGripMultiplier() + tires[3].getDriftGripMultiplier()) / 2;
        double frontGrip = (tires[0].getDriftGripMultiplier() + tires[1].getDriftGripMultiplier()) / 2;
        
        // SLIP ANGLE DIFFERENTIAL EFFECT
        // When rear slip angle > front slip angle, the rear wants to swing out
        double slipAngleDiff = rearSlipAngle - frontSlipAngle;
        
        if (slipAngleDiff > 3 && speed > 4) {
            // Rear is sliding more than front - OVERSTEER!
            // Add rotation based on lateral velocity direction
            double oversteerTorque = Math.signum(lateralSpeed) * slipAngleDiff * speed * 0.004;
            angularAccel += oversteerTorque;
        } else if (slipAngleDiff < -5 && speed > 6) {
            // Front sliding more - UNDERSTEER
            // Reduce turn rate
            angularAccel *= 0.7;
        }
        
        // COUNTER-STEER EFFECT - This is how you CONTROL a drift!
        // When steering opposite to the slide direction, it reduces yaw rate
        if (isDrifting || isOversteering) {
            double slideDirection = Math.signum(driftAngle);
            double steerDirection = Math.signum(steeringInput);
            
            if (slideDirection != 0 && steerDirection == slideDirection) {
                // Counter-steering (steering into the slide)
                // This creates a stabilizing force that controls the drift angle
                double counterSteerEffect = Math.abs(steeringInput) * speed * 0.015;
                angularAccel -= slideDirection * counterSteerEffect;
            }
        }
        
        // THROTTLE MODULATION DURING DRIFT
        // More throttle = more rear slip = maintains/increases drift angle
        // Less throttle = rear regains grip = drift recovers
        if (isDrifting && throttleInput > 0.3 && rearGrip < 0.7) {
            // Throttle is maintaining the drift by keeping rear wheels spinning
            double throttleSpin = (throttleInput - 0.3) * 0.02 * speed;
            angularAccel += Math.signum(driftAngle) * throttleSpin;
        }
        
        // Apply angular acceleration
        angularVelocity += angularAccel * dt;
        yawRate = angularVelocity;
        
        // YAW DAMPING - Different rates for different states
        double yawDamping;
        if (isDrifting || isOversteering) {
            // Light damping during drift - allow rotation
            yawDamping = 0.985;
        } else if (throttleInput < 0.1 && Math.abs(steeringInput) < 0.1) {
            // Strong damping when coasting straight - car wants to straighten
            yawDamping = 0.92;
        } else {
            // Normal damping
            yawDamping = 0.97;
        }
        angularVelocity *= yawDamping;
        
        // STEERING-BASED TURNING - Essential for normal driving!
        // This ensures the car turns when you steer, not just from tire forces
        if (speed > 0.5 && Math.abs(steeringAngle) > 0.01) {
            // Use total speed for turn calculation, not just forward speed
            // This allows steering to work even when sideways (drifting)
            double effectiveSpeed = Math.max(Math.abs(forwardSpeed), speed * 0.5);
            double turnRadius = GameConstants.WHEELBASE / Math.tan(Math.abs(steeringAngle) + 0.001);
            double desiredAngularVel = effectiveSpeed / turnRadius * Math.signum(steeringAngle);
            
            // When drifting, steering should still have effect to control the drift
            // Counter-steering (into the slide) reduces rotation, steering away increases it
            double directSteerFactor;
            if (rearGrip > 0.7) {
                // Normal grip - blend based on speed
                directSteerFactor = MathUtils.clamp(1.0 - speed / 25, 0.4, 1.0);
            } else {
                // Low rear grip (drifting) - steering still works but less direct
                // This allows counter-steering to control the drift
                directSteerFactor = 0.35;
            }
            
            // Blend current angular velocity with desired
            angularVelocity = angularVelocity * (1 - directSteerFactor) + desiredAngularVel * directSteerFactor;
        }
        
        // ==================== UPDATE POSITION ====================
        position = position.add(velocity.multiply(dt));
        rotation += angularVelocity * dt;
        rotation = MathUtils.normalizeAngle(rotation);
        
        speed = velocity.magnitude();
        speedMph = speed * 2.237;
        lateralG = Math.abs(lateralSpeed * angularVelocity) / GameConstants.GRAVITY;
        
        updateDriftState(dt, forwardSpeed);
    }
    
    /**
     * Calculate slip angles for front and rear axles separately
     * slipAngle = atan2(lateralVelocity, longitudinalVelocity)
     * 
     * This is the fundamental measurement for drift physics!
     */
    private void calculateAxleSlipAngles(double forwardSpeed, double lateralSpeed) {
        // Front axle slip angle (includes steering effect)
        double frontLateralVel = lateralSpeed + angularVelocity * (GameConstants.WHEELBASE / 2);
        if (Math.abs(forwardSpeed) > 0.5) {
            double frontVelAngle = Math.atan2(frontLateralVel, Math.abs(forwardSpeed));
            frontSlipAngle = Math.abs(MathUtils.toDegrees(frontVelAngle - steeringAngle));
        } else {
            frontSlipAngle = 0;
        }
        
        // Rear axle slip angle
        double rearLateralVel = lateralSpeed - angularVelocity * (GameConstants.WHEELBASE / 2);
        if (Math.abs(forwardSpeed) > 0.5) {
            double rearVelAngle = Math.atan2(rearLateralVel, Math.abs(forwardSpeed));
            rearSlipAngle = Math.abs(MathUtils.toDegrees(rearVelAngle));
        } else {
            rearSlipAngle = 0;
        }
        
        // Clamp to reasonable values
        frontSlipAngle = MathUtils.clamp(frontSlipAngle, 0, 90);
        rearSlipAngle = MathUtils.clamp(rearSlipAngle, 0, 90);
    }
    
    /**
     * Detect Scandinavian Flick (Feint) for drift initiation
     * Quick steering from one direction to the other transfers weight and breaks traction
     */
    private void detectFeint(double dt) {
        // Track steering change rate
        double steeringDelta = steeringInput - previousSteeringInput;
        steeringChangeRate = steeringDelta / dt;
        previousSteeringInput = steeringInput;
        
        // Detect rapid steering direction change
        if (Math.abs(steeringChangeRate) > 8 && speed > 10) {
            // Quick flick detected!
            feintActive = true;
            feintTimer = 0.3; // Effect lasts 0.3 seconds
        }
        
        // Decay feint effect
        if (feintTimer > 0) {
            feintTimer -= dt;
            if (feintTimer <= 0) {
                feintActive = false;
            }
        }
    }
    
    /**
     * Update clutch kick timer
     */
    private void updateClutchKick(double dt) {
        if (clutchKickActive) {
            clutchKickTimer -= dt;
            if (clutchKickTimer <= 0) {
                clutchKickActive = false;
            }
        }
    }
    
    /**
     * Trigger a clutch kick - sudden torque spike to break rear traction
     * Call this when player presses clutch kick button
     */
    public void triggerClutchKick() {
        if (speed > 5 && throttleInput > 0.3 && !clutchKickActive) {
            clutchKickActive = true;
            clutchKickTimer = 0.15; // 150ms torque spike
        }
    }
    
    /**
     * Smooth steering input to angle
     */
    private void updateSteering(double dt) {
        // Apply steering modifier from upgrades
        double maxAngle = GameConstants.MAX_STEERING_ANGLE * steeringModifier;
        double targetAngle = steeringInput * MathUtils.toRadians(maxAngle);
        
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
     * Calculate weight transfer based on acceleration, braking, and cornering.
     * 
     * REAL DRIFT PHYSICS:
     * - Acceleration shifts weight REARWARD (rear tires get more grip)
     * - Braking/throttle lift shifts weight FORWARD (rear tires lose grip - lift-off oversteer!)
     * - Cornering shifts weight to outside tires
     * - Less weight on rear = less rear grip = easier to initiate drift
     */
    private void calculateWeightTransfer() {
        // Track throttle changes for lift-off oversteer
        double throttleDelta = throttleInput - previousThrottle;
        previousThrottle = throttleInput;
        
        // Detect sudden throttle lift (causes snap oversteer in real cars)
        if (throttleDelta < -0.1 && speed > 8) {
            // Quick throttle release - weight rushes forward
            throttleLiftAmount = Math.min(1.0, throttleLiftAmount + Math.abs(throttleDelta) * 2);
        } else {
            // Decay the lift effect
            throttleLiftAmount *= 0.92;
        }
        
        // LONGITUDINAL weight transfer
        double longitudinalShift = 0;
        
        if (throttleInput > 0.1) {
            // Accelerating - weight shifts BACK (rear has MORE grip)
            longitudinalShift = throttleInput * 0.15;
        } else if (brakeInput > 0.1) {
            // Braking - weight shifts FORWARD (rear loses grip)
            longitudinalShift = -brakeInput * 0.25;
        } else {
            // Coasting - slight forward shift
            longitudinalShift = -0.05;
        }
        
        // Add throttle lift effect (sudden forward weight transfer)
        longitudinalShift -= throttleLiftAmount * 0.2;
        
        // LATERAL weight transfer during cornering
        Vector2D forward = Vector2D.fromAngle(rotation);
        Vector2D right = forward.perpendicular();
        double lateralVelocity = velocity.dot(right);
        double lateralGForce = Math.abs(lateralVelocity * angularVelocity) / GameConstants.GRAVITY;
        double lateralShift = MathUtils.clamp(lateralGForce * 0.1, 0, 0.15);
        
        // Apply weight distribution
        rearWeight = 0.5 + longitudinalShift;
        frontWeight = 0.5 - longitudinalShift;
        
        // Clamp weights
        rearWeight = MathUtils.clamp(rearWeight, 0.3, 0.7);
        frontWeight = MathUtils.clamp(frontWeight, 0.3, 0.7);
        
        // Calculate normal force on each tire
        double totalWeight = GameConstants.CAR_MASS * GameConstants.GRAVITY;
        double frontAxleWeight = totalWeight * frontWeight;
        double rearAxleWeight = totalWeight * rearWeight;
        
        // Distribute weight with lateral transfer
        double innerFrontWeight = frontAxleWeight / 2 * (1 - lateralShift);
        double outerFrontWeight = frontAxleWeight / 2 * (1 + lateralShift);
        double innerRearWeight = rearAxleWeight / 2 * (1 - lateralShift);
        double outerRearWeight = rearAxleWeight / 2 * (1 + lateralShift);
        
        // Apply based on turn direction
        if (steeringInput > 0) {
            tires[0].setNormalForce(outerFrontWeight);
            tires[1].setNormalForce(innerFrontWeight);
            tires[2].setNormalForce(outerRearWeight);
            tires[3].setNormalForce(innerRearWeight);
        } else {
            tires[0].setNormalForce(innerFrontWeight);
            tires[1].setNormalForce(outerFrontWeight);
            tires[2].setNormalForce(innerRearWeight);
            tires[3].setNormalForce(outerRearWeight);
        }
        
        calculateDriftGrip();
    }
    
    /**
     * REALISTIC GRIP CALCULATION - Based on Slip Angle & Weight Transfer
     * 
     * Key physics:
     * - Grip increases with slip angle up to a PEAK (around 8-12 degrees)
     * - Beyond the peak, grip DECREASES (tire is sliding)
     * - This creates natural oversteer/understeer based on per-axle slip angles
     * - Weight transfer affects available grip per axle
     */
    private void calculateDriftGrip() {
        Vector2D forward = Vector2D.fromAngle(rotation);
        Vector2D right = forward.perpendicular();
        double lateralSpeed = velocity.dot(right);
        
        // ===== GRIP CURVES BASED ON SLIP ANGLE =====
        // Using simplified Pacejka concept: grip peaks then falls off
        double slipAnglePeak = GameConstants.SLIP_ANGLE_PEAK; // ~6-8 degrees
        double slipAngleFalloff = 25.0; // Degrees where grip is significantly reduced
        
        // Front axle grip
        double frontGripMult = calculateGripFromSlipAngle(frontSlipAngle, slipAnglePeak, slipAngleFalloff);
        
        // Rear axle grip
        double rearGripMult = calculateGripFromSlipAngle(rearSlipAngle, slipAnglePeak, slipAngleFalloff);
        
        // ===== WEIGHT TRANSFER EFFECTS =====
        // More weight = more grip available
        frontGripMult *= (0.6 + 0.4 * (frontWeight / 0.5));
        rearGripMult *= (0.4 + 0.6 * (rearWeight / 0.5));
        
        // ===== DRIFT INITIATION METHODS =====
        // Handbrake is the primary drift tool, others require extreme inputs
        
        // 1. HANDBRAKE - Main drift initiation (still effective)
        if (handbrakeActive && speed > 3) {
            rearGripMult *= 0.5; // 50% grip loss - enough to slide
        }
        
        // 2. POWER OVERSTEER - Only at redline with full throttle
        double rpm = engine.getRpm();
        double rpmPercent = rpm / GameConstants.REDLINE_RPM;
        if (throttleInput > 0.98 && rpmPercent > 0.9 && speed > 10 && Math.abs(steeringInput) > 0.7) {
            // Requires even more extreme inputs to break traction with power
            double powerLoss = (throttleInput - 0.95) * (rpmPercent - 0.85) * 0.2;
            powerLoss = MathUtils.clamp(powerLoss, 0, 0.1);
            rearGripMult -= powerLoss;
        }
        
        // 3. CLUTCH KICK - Moderate effect
        if (clutchKickActive) {
            rearGripMult *= 0.6;
        }
        
        // 4. FEINT - Very mild
        if (feintActive) {
            rearGripMult *= 0.85;
        }
        
        // 5. LIFT-OFF OVERSTEER - Only at very high speed
        if (throttleLiftAmount > 0.6 && speed > 20 && Math.abs(steeringInput) > 0.5) {
            rearGripMult -= throttleLiftAmount * 0.1;
        }
        
        // 6. BRAKING DRIFT - Very hard braking while turning fast
        if (brakeInput > 0.8 && Math.abs(steeringInput) > 0.6 && speed > 20) {
            rearGripMult -= brakeInput * 0.08;
        }
        
        // ===== DRIFT STABILITY - Throttle maintains the slide =====
        // When already drifting, throttle keeps rear wheels spinning
        if (isDrifting && rearSlipAngle > 25 && throttleInput > 0.7) {
            // More throttle = maintains grip loss = maintains drift
            double driftMaintenance = (throttleInput - 0.7) * 0.1;
            rearGripMult -= driftMaintenance;
        }
        
        // ===== UNDERSTEER DETECTION =====
        // Front loses grip when pushed too hard
        isUndersteering = frontSlipAngle > 15 && frontGripMult < 0.7 && 
                          speed > 12 && !handbrakeActive && rearGripMult > 0.6;
        
        // ===== OVERSTEER DETECTION =====
        // Rear sliding more than front = oversteer/drift
        isOversteering = rearSlipAngle > frontSlipAngle + 8 && 
                         rearGripMult < 0.55 && speed > 5;
        
        // ===== CLAMP AND APPLY =====
        // Very high minimum grip - car should feel planted when not drifting
        // Only reduce grip significantly during intentional drift actions
        rearGripMult = MathUtils.clamp(rearGripMult, 0.65, 1.0);  // Was 0.5
        frontGripMult = MathUtils.clamp(frontGripMult, 0.85, 1.0); // Was 0.8
        
        // Apply to tires
        tires[0].setDriftGripMultiplier(frontGripMult);
        tires[1].setDriftGripMultiplier(frontGripMult);
        tires[2].setDriftGripMultiplier(rearGripMult);
        tires[3].setDriftGripMultiplier(rearGripMult);
    }
    
    /**
     * Calculate grip multiplier based on slip angle using Pacejka-like curve
     * Grip increases up to peak angle, then decreases
     * 
     * @param slipAngle Current slip angle in degrees
     * @param peakAngle Angle of maximum grip
     * @param falloffAngle Angle where grip is significantly reduced
     * @return Grip multiplier 0.0 to 1.0
     */
    private double calculateGripFromSlipAngle(double slipAngle, double peakAngle, double falloffAngle) {
        // Very forgiving grip curve - tires maintain grip even at high slip angles
        if (slipAngle <= peakAngle * 2) {
            // Up to double the peak angle - full grip
            return 1.0;
        } else if (slipAngle <= falloffAngle) {
            // Gradual reduction
            double beyondPeak = slipAngle - peakAngle * 2;
            double maxBeyond = falloffAngle - peakAngle * 2;
            double falloff = beyondPeak / maxBeyond;
            // Only lose 15% grip max in this range
            return 1.0 - (falloff * 0.15);
        } else {
            // Beyond falloff - still maintain good grip
            return 0.85;
        }
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
    public boolean isUndersteering() { return isUndersteering; }
    public boolean isOversteering() { return isOversteering; }
    public double getDriftAngle() { return driftAngle; }
    public double getDriftTime() { return driftTime; }
    public double getSteeringAngle() { return steeringAngle; }
    public double getSteeringAngleDegrees() { return MathUtils.toDegrees(steeringAngle); }
    public double getLateralG() { return lateralG; }
    public boolean isHandbrakeActive() { return handbrakeActive; }
    
    // New drift physics getters
    public double getFrontSlipAngle() { return frontSlipAngle; }
    public double getRearSlipAngle() { return rearSlipAngle; }
    public double getYawRate() { return yawRate; }
    public boolean isFeintActive() { return feintActive; }
    public boolean isClutchKickActive() { return clutchKickActive; }
    
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
    
    // Modifier setters for shop upgrades
    public void setSteeringModifier(double mod) { this.steeringModifier = mod; }
    public void setPowerModifier(double mod) { this.powerModifier = mod; }
    public void setGripModifier(double mod) { this.gripModifier = mod; }
    public void setHandlingModifier(double mod) { this.handlingModifier = mod; }
    public void setWeightModifier(double mod) { this.weightModifier = mod; }
    public void setTireDurability(double dur) { this.tireDurability = dur; }
    
    // Modifier getters
    public double getSteeringModifier() { return steeringModifier; }
    public double getPowerModifier() { return powerModifier; }
    public double getGripModifier() { return gripModifier; }
    public double getHandlingModifier() { return handlingModifier; }
    public double getWeightModifier() { return weightModifier; }
    public double getTireDurability() { return tireDurability; }
    
    /**
     * Apply grip and durability modifiers to all tires
     */
    public void applyTireModifiers(double grip, double durability) {
        this.gripModifier = grip;
        this.tireDurability = durability;
        for (Tire tire : tires) {
            tire.setGripModifier(grip);
            tire.setDurabilityModifier(durability);
        }
    }
    
    /**
     * Apply all shop upgrades at once
     */
    public void applyUpgrades(double steering, double power, double grip, 
                              double handling, double weight, double tireDur, double brakes) {
        this.steeringModifier = steering;
        this.powerModifier = power;
        this.gripModifier = grip;
        this.handlingModifier = handling;
        this.weightModifier = weight;
        this.tireDurability = tireDur;
        this.brakeModifier = brakes;
        
        // Apply to tires
        for (Tire tire : tires) {
            tire.setGripModifier(grip);
            tire.setDurabilityModifier(tireDur);
        }
    }
    
    // Brake modifier setter/getter
    public void setBrakeModifier(double mod) { this.brakeModifier = mod; }
    public double getBrakeModifier() { return brakeModifier; }
    public boolean isAbsActive() { return absActive; }
    
    // ==================== COLLISION RESPONSE ====================
    
    /**
     * Set car position (used for collision response)
     */
    public void setPosition(Vector2D newPos) {
        this.position = newPos;
    }
    
    /**
     * Set car position by components
     */
    public void setPosition(double x, double y) {
        this.position = new Vector2D(x, y);
    }
    
    /**
     * Apply collision impulse - bounces car off obstacle
     * @param normal The collision normal (direction to push car)
     * @param restitution Bounciness (0 = no bounce, 1 = full bounce)
     */
    public void applyCollisionImpulse(Vector2D normal, double restitution) {
        // Calculate velocity component in collision direction
        double velAlongNormal = velocity.dot(normal);
        
        // Only apply if moving towards the obstacle
        if (velAlongNormal < 0) {
            // Calculate impulse magnitude
            double impulseMag = -(1 + restitution) * velAlongNormal;
            
            // Apply impulse
            velocity = velocity.add(normal.multiply(impulseMag));
            
            // Reduce speed on collision (energy loss)
            velocity = velocity.multiply(0.7);
            
            // Add some angular velocity from impact
            angularVelocity += (Math.random() - 0.5) * 2.0;
        }
    }
    
    /**
     * Get the collision radius (half the car's diagonal)
     */
    public double getCollisionRadius() {
        double halfLength = GameConstants.CAR_LENGTH / 2;
        double halfWidth = GameConstants.CAR_WIDTH / 2;
        return Math.sqrt(halfLength * halfLength + halfWidth * halfWidth);
    }
}
