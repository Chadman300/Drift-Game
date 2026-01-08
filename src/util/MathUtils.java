package util;

/**
 * Utility functions used throughout the game
 */
public class MathUtils {
    
    /**
     * Clamp a value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    /**
     * Linear interpolation between two values
     */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
    
    /**
     * Smooth interpolation (ease in/out)
     */
    public static double smoothstep(double t) {
        t = clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
    
    /**
     * Map a value from one range to another
     */
    public static double map(double value, double inMin, double inMax, double outMin, double outMax) {
        return outMin + (outMax - outMin) * ((value - inMin) / (inMax - inMin));
    }
    
    /**
     * Convert degrees to radians
     */
    public static double toRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }
    
    /**
     * Convert radians to degrees
     */
    public static double toDegrees(double radians) {
        return radians * 180.0 / Math.PI;
    }
    
    /**
     * Normalize an angle to [-PI, PI]
     */
    public static double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }
    
    /**
     * Get sign of a number (-1, 0, or 1)
     */
    public static double sign(double value) {
        if (value > 0) return 1;
        if (value < 0) return -1;
        return 0;
    }
    
    /**
     * Approach a target value at a given speed
     */
    public static double approach(double current, double target, double speed) {
        double diff = target - current;
        if (Math.abs(diff) <= speed) {
            return target;
        }
        return current + sign(diff) * speed;
    }
    
    /**
     * Check if value is nearly zero
     */
    public static boolean nearZero(double value, double epsilon) {
        return Math.abs(value) < epsilon;
    }
    
    /**
     * Random double in range
     */
    public static double random(double min, double max) {
        return min + Math.random() * (max - min);
    }
    
    /**
     * Random int in range (inclusive)
     */
    public static int randomInt(int min, int max) {
        return min + (int)(Math.random() * (max - min + 1));
    }
}
