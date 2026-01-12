package util;

/**
 * Central configuration file for all game constants
 * Organized for easy AI modifications and tuning
 */
public class GameConstants {
    
    // ============== WINDOW SETTINGS (Dynamic for fullscreen) ==============
    public static int WINDOW_WIDTH = 1920;   // Default, updated at runtime
    public static int WINDOW_HEIGHT = 1080;  // Default, updated at runtime
    public static final int TARGET_FPS = 60;
    public static final double DELTA_TIME = 1.0 / TARGET_FPS;
    
    // ============== PIXEL ART SCALING ==============
    public static final int PIXEL_SCALE = 4;  // Upscale factor for retro look
    public static int RENDER_WIDTH = WINDOW_WIDTH / PIXEL_SCALE;
    public static int RENDER_HEIGHT = WINDOW_HEIGHT / PIXEL_SCALE;
    
    /**
     * Update screen dimensions at runtime for fullscreen
     */
    public static void setScreenSize(int width, int height) {
        WINDOW_WIDTH = width;
        WINDOW_HEIGHT = height;
        RENDER_WIDTH = width / PIXEL_SCALE;
        RENDER_HEIGHT = height / PIXEL_SCALE;
    }
    
    // ============== CAR DIMENSIONS (in game units) ==============
    public static final double CAR_LENGTH = 4.5;  // meters
    public static final double CAR_WIDTH = 2.0;   // meters
    public static final double WHEELBASE = 2.7;   // meters (distance between axles)
    public static final double TRACK_WIDTH = 1.8; // meters (distance between left/right wheels)
    
    // ============== ENGINE SPECS ==============
    public static final double ENGINE_HORSEPOWER = 350;  // HP
    public static final double ENGINE_TORQUE = 400;      // Nm at peak
    public static final double IDLE_RPM = 800;
    public static final double REDLINE_RPM = 8000;
    public static final double MAX_RPM = 8500;
    public static final double REV_LIMITER_RPM = 8200;
    
    // ============== TRANSMISSION ==============
    public static final double[] GEAR_RATIOS = {
        0.0,    // Neutral
        3.5,    // 1st gear
        2.2,    // 2nd gear
        1.5,    // 3rd gear
        1.1,    // 4th gear
        0.85,   // 5th gear
        0.7     // 6th gear
    };
    public static final double FINAL_DRIVE_RATIO = 3.7;
    public static final double REVERSE_GEAR_RATIO = -3.2;
    
    // ============== TIRE PHYSICS ==============
    public static final double TIRE_RADIUS = 0.33;         // meters
    public static final double OPTIMAL_TIRE_PRESSURE = 32; // PSI
    public static final double MIN_TIRE_PRESSURE = 20;
    public static final double MAX_TIRE_PRESSURE = 40;
    public static final double BASE_TIRE_GRIP = 1.0;
    public static final double TIRE_GRIP_FALLOFF = 0.3;    // Grip loss when sliding
    public static final double TIRE_TEMP_OPTIMAL = 90;     // Celsius
    public static final double TIRE_TEMP_MIN = 20;
    public static final double TIRE_TEMP_MAX = 150;
    
    // ============== TRACTION & DRIFT PHYSICS ==============
    public static final double STATIC_FRICTION_COEF = 1.2;   // Grip when not sliding
    public static final double KINETIC_FRICTION_COEF = 0.8;  // Grip when sliding
    public static final double SLIP_ANGLE_PEAK = 8.0;        // Degrees for max grip
    public static final double SLIP_ANGLE_DRIFT = 15.0;      // Degrees when drifting starts
    public static final double SLIP_RATIO_THRESHOLD = 0.15;  // When tires start spinning
    public static final double DRIFT_ANGLE_THRESHOLD = 10.0; // Degrees to be considered drifting
    
    // ============== WEIGHT & PHYSICS ==============
    public static final double CAR_MASS = 1400;      // kg
    public static final double GRAVITY = 9.81;       // m/s²
    public static final double AIR_DENSITY = 1.225;  // kg/m³
    public static final double DRAG_COEFFICIENT = 0.25;  // Reduced for less aggressive high-speed slowdown
    public static final double LINEAR_DRAG = 200;    // Linear drag for consistent feel (Newtons per m/s)
    public static final double GROUND_FRICTION = 0.15;   // Ground friction for low-speed stopping
    public static final double FRONTAL_AREA = 2.2;   // m²
    public static final double ROLLING_RESISTANCE = 0.08; // Higher for realistic coasting deceleration
    
    // ============== STEERING ==============
    public static final double MAX_STEERING_ANGLE = 35;  // Degrees
    public static final double STEERING_SPEED = 3.5;     // How fast steering responds
    public static final double STEERING_RETURN_SPEED = 5.0;
    public static final double COUNTERSTEER_ASSIST = 0.3; // 0-1, helps with drifts
    
    // ============== BRAKES ==============
    public static final double MAX_BRAKE_FORCE = 25000;  // Newtons
    public static final double HANDBRAKE_FORCE = 15000;  // Rear wheels only
    public static final double BRAKE_BIAS_FRONT = 0.6;   // 60% front, 40% rear
    
    // ============== WORLD SETTINGS ==============
    public static final int WORLD_SIZE = 2000;  // World size in game units
    public static final int ROAD_WIDTH = 24;    // meters (doubled for easier gameplay)
    public static final int BLOCK_SIZE = 120;   // City block size in meters (increased to match wider roads)
    
    // ============== CAMERA ==============
    public static final double CAMERA_FOLLOW_SMOOTHNESS = 0.08;
    public static final double CAMERA_ROTATION_SMOOTHNESS = 0.05;
    public static final double CAMERA_ZOOM = 5.0;  // Pixels per meter
    public static final double CAMERA_DRIFT_OFFSET = 2.0;  // Look ahead when drifting
    public static final double CAMERA_VELOCITY_LOOKAHEAD = 0.3;  // Look ahead in movement direction
    
    // ============== HUD SETTINGS ==============
    public static final int HUD_BAR_HEIGHT = 45;  // Height of bottom HUD bar in pixels
    
    // ============== VISUAL EFFECTS ==============
    public static final int TIRE_SMOKE_PARTICLES = 50;
    public static final int TIRE_MARKS_LENGTH = 200;
    public static final double SMOKE_THRESHOLD_SLIP = 0.2;  // Slip ratio for smoke
    
    // ============== SCORING ==============
    public static final double DRIFT_SCORE_MULTIPLIER = 100;
    public static final double DRIFT_ANGLE_BONUS = 1.5;
    public static final double SPEED_BONUS_MULTIPLIER = 0.1;
    public static final double COMBO_TIMEOUT = 2.0;  // Seconds to maintain combo
    
    // ============== COLORS (Retro Palette) ==============
    public static final int[] COLOR_PALETTE = {
        0xFF0D0D0D,  // Black
        0xFF1A1A2E,  // Dark blue
        0xFF16213E,  // Navy
        0xFF0F3460,  // Blue
        0xFFE94560,  // Red/Pink
        0xFF533483,  // Purple
        0xFFFF6B6B,  // Light red
        0xFFFFE66D,  // Yellow
        0xFF4ECDC4,  // Cyan
        0xFF95E1D3,  // Light green
        0xFFF38181,  // Salmon
        0xFFAA96DA,  // Lavender
        0xFFFCBF49,  // Orange
        0xFFEAE2B7,  // Cream
        0xFF2D3436,  // Dark gray
        0xFFDFE6E9   // Light gray
    };
}
