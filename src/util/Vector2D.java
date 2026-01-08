package util;

/**
 * 2D Vector class for physics calculations
 * Provides essential vector math operations for game physics
 */
public class Vector2D {
    public double x;
    public double y;
    
    public Vector2D() {
        this.x = 0;
        this.y = 0;
    }
    
    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2D copy() {
        return new Vector2D(x, y);
    }
    
    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }
    
    public Vector2D subtract(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }
    
    public Vector2D multiply(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }
    
    public Vector2D divide(double scalar) {
        if (scalar == 0) return new Vector2D(0, 0);
        return new Vector2D(x / scalar, y / scalar);
    }
    
    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }
    
    public Vector2D normalize() {
        double mag = magnitude();
        if (mag == 0) return new Vector2D(0, 0);
        return divide(mag);
    }
    
    public double dot(Vector2D other) {
        return x * other.x + y * other.y;
    }
    
    public double cross(Vector2D other) {
        return x * other.y - y * other.x;
    }
    
    public Vector2D rotate(double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vector2D(
            x * cos - y * sin,
            x * sin + y * cos
        );
    }
    
    public double angle() {
        return Math.atan2(y, x);
    }
    
    public static Vector2D fromAngle(double angle) {
        return new Vector2D(Math.cos(angle), Math.sin(angle));
    }
    
    public Vector2D perpendicular() {
        return new Vector2D(-y, x);
    }
    
    public Vector2D lerp(Vector2D target, double t) {
        return new Vector2D(
            x + (target.x - x) * t,
            y + (target.y - y) * t
        );
    }
    
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)", x, y);
    }
}
