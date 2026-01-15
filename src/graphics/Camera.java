package graphics;

import util.GameConstants;
import util.Vector2D;

/**
 * Camera system for the game
 * Follows the player car with smooth movement
 * 
 * FEATURES:
 * - Smooth position and rotation following
 * - Drift look-ahead (camera shifts in drift direction)
 * - Velocity look-ahead (camera leads in movement direction)
 * - Screen shake effect for impacts
 * 
 * FUTURE ADDITIONS:
 * - Zoom based on speed
 * - Multiple camera modes (chase, fixed, cinematic)
 * - Smooth transitions between modes
 */
public class Camera {
    
    private double x, y;           // Current camera position
    private double targetX, targetY; // Target position
    private double rotation;       // Camera rotation (follows car)
    private double targetRotation;
    private double zoom;           // Pixels per meter (current)
    private double baseZoom;       // Base zoom level
    private double targetZoom;     // Target zoom based on speed
    
    // Speed-based FOV - noticeable but smooth zoom changes
    private static final double MIN_ZOOM = 3.5;    // Zoomed out at high speed
    private static final double MAX_ZOOM = 5.5;    // Zoomed in at low speed
    private static final double ZOOM_SPEED = 3.0;  // Faster zoom transitions
    
    // Shake effect
    private double shakeIntensity;
    private double shakeTimer;
    private double shakeOffsetX, shakeOffsetY;
    
    // Drift offset (look ahead in drift direction)
    private double driftOffsetX, driftOffsetY;
    
    // Velocity look-ahead
    private double velocityOffsetX, velocityOffsetY;
    
    public Camera() {
        this.x = 0;
        this.y = 0;
        this.targetX = 0;
        this.targetY = 0;
        this.rotation = 0;
        this.targetRotation = 0;
        this.baseZoom = GameConstants.CAMERA_ZOOM;
        this.zoom = MAX_ZOOM;
        this.targetZoom = MAX_ZOOM;
        this.shakeIntensity = 0;
        this.shakeTimer = 0;
    }
    
    /**
     * Update camera position and effects
     * @param dt Delta time
     * @param targetPos Player position to follow
     * @param targetRot Player rotation to follow
     * @param isDrifting Whether player is drifting
     * @param driftAngle Current drift angle
     * @param velocity Player velocity for look-ahead
     */
    public void update(double dt, Vector2D targetPos, double targetRot, 
                       boolean isDrifting, double driftAngle, Vector2D velocity) {
        // Set targets
        this.targetX = targetPos.x;
        this.targetY = targetPos.y;
        this.targetRotation = targetRot;
        
        // Calculate velocity look-ahead (camera leads in movement direction)
        double speed = velocity.magnitude();
        if (speed > 1.0) {
            double lookAheadAmount = GameConstants.CAMERA_VELOCITY_LOOKAHEAD * Math.min(speed / 20, 1.5);
            double targetVelOffsetX = velocity.x * lookAheadAmount;
            double targetVelOffsetY = velocity.y * lookAheadAmount;
            // Smooth the velocity offset
            velocityOffsetX += (targetVelOffsetX - velocityOffsetX) * 0.05;
            velocityOffsetY += (targetVelOffsetY - velocityOffsetY) * 0.05;
        } else {
            velocityOffsetX *= 0.95;
            velocityOffsetY *= 0.95;
        }
        
        // Calculate drift look-ahead offset
        if (isDrifting) {
            double driftDir = Math.signum(driftAngle);
            double offsetMag = Math.min(Math.abs(driftAngle) / 45, 1) * GameConstants.CAMERA_DRIFT_OFFSET;
            driftOffsetX = Math.sin(targetRotation) * driftDir * offsetMag;
            driftOffsetY = -Math.cos(targetRotation) * driftDir * offsetMag;
        } else {
            driftOffsetX *= 0.9;
            driftOffsetY *= 0.9;
        }
        
        // Combine all offsets
        double totalOffsetX = driftOffsetX + velocityOffsetX;
        double totalOffsetY = driftOffsetY + velocityOffsetY;
        
        // Smooth follow
        double followSpeed = GameConstants.CAMERA_FOLLOW_SMOOTHNESS;
        x += (targetX + totalOffsetX - x) * followSpeed * 60 * dt;
        y += (targetY + totalOffsetY - y) * followSpeed * 60 * dt;
        
        // Smooth rotation
        double rotSpeed = GameConstants.CAMERA_ROTATION_SMOOTHNESS;
        double rotDiff = targetRotation - rotation;
        // Normalize rotation difference
        while (rotDiff > Math.PI) rotDiff -= 2 * Math.PI;
        while (rotDiff < -Math.PI) rotDiff += 2 * Math.PI;
        rotation += rotDiff * rotSpeed * 60 * dt;
        
        // SPEED-BASED FOV (ZOOM)
        // Zoom out as speed increases for sense of speed
        double speedMph = speed * 2.237; // Convert to mph
        double speedFactor = Math.min(speedMph / 80.0, 1.0); // Max effect at 80mph
        targetZoom = MAX_ZOOM - (MAX_ZOOM - MIN_ZOOM) * speedFactor;
        
        // Extra zoom out when drifting for dramatic effect
        if (isDrifting) {
            double driftZoomOut = Math.min(Math.abs(driftAngle) / 60.0, 0.4);
            targetZoom -= driftZoomOut;
        }
        
        // Clamp target zoom
        targetZoom = Math.max(MIN_ZOOM - 0.5, Math.min(MAX_ZOOM, targetZoom));
        
        // Smoothly interpolate to target zoom
        zoom += (targetZoom - zoom) * ZOOM_SPEED * dt;
        
        // Update shake
        if (shakeTimer > 0) {
            shakeTimer -= dt;
            shakeOffsetX = (Math.random() - 0.5) * 2 * shakeIntensity;
            shakeOffsetY = (Math.random() - 0.5) * 2 * shakeIntensity;
            shakeIntensity *= 0.9;
        } else {
            shakeOffsetX = 0;
            shakeOffsetY = 0;
        }
    }
    
    /**
     * Apply camera shake
     */
    public void shake(double intensity, double duration) {
        this.shakeIntensity = intensity;
        this.shakeTimer = duration;
    }
    
    /**
     * Convert world coordinates to screen coordinates
     */
    public int worldToScreenX(double worldX) {
        double relX = worldX - x;
        double relY = 0; // Y coordinate doesn't affect X for rotation
        
        // Apply camera rotation
        double cos = Math.cos(-rotation);
        double sin = Math.sin(-rotation);
        double rotatedX = relX * cos - (worldX - x) * 0 * sin;
        
        // Actually, for top-down we don't rotate the camera view
        // Just offset and scale
        double screenX = (worldX - x) * zoom + GameConstants.RENDER_WIDTH / 2.0;
        screenX += shakeOffsetX;
        
        return (int) screenX;
    }
    
    public int worldToScreenY(double worldY) {
        double screenY = (worldY - y) * zoom + GameConstants.RENDER_HEIGHT / 2.0;
        screenY += shakeOffsetY;
        return (int) screenY;
    }
    
    /**
     * Convert screen coordinates to world coordinates
     */
    public double screenToWorldX(int screenX) {
        return (screenX - GameConstants.RENDER_WIDTH / 2.0 - shakeOffsetX) / zoom + x;
    }
    
    public double screenToWorldY(int screenY) {
        return (screenY - GameConstants.RENDER_HEIGHT / 2.0 - shakeOffsetY) / zoom + y;
    }
    
    /**
     * Get visible world bounds for culling
     */
    public double[] getVisibleBounds() {
        double halfWidth = GameConstants.RENDER_WIDTH / zoom / 2;
        double halfHeight = GameConstants.RENDER_HEIGHT / zoom / 2;
        
        return new double[] {
            x - halfWidth - 20,  // Left (with margin)
            y - halfHeight - 20, // Top
            x + halfWidth + 20,  // Right
            y + halfHeight + 20  // Bottom
        };
    }
    
    /**
     * Check if world position is visible
     */
    public boolean isVisible(double worldX, double worldY, double margin) {
        double[] bounds = getVisibleBounds();
        return worldX >= bounds[0] - margin && worldX <= bounds[2] + margin &&
               worldY >= bounds[1] - margin && worldY <= bounds[3] + margin;
    }
    
    /**
     * Scale a world distance to screen pixels
     */
    public int scaleToScreen(double worldDistance) {
        return (int)(worldDistance * zoom);
    }
    
    // Getters and setters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
    public double getZoom() { return zoom; }
    
    public void setZoom(double zoom) { 
        this.zoom = Math.max(1, Math.min(20, zoom)); 
    }
    
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
    }
}
