package world;

/**
 * Represents a building in the city
 */
public class Building {
    
    private double x, y;           // Position (top-left corner)
    private double width, height;  // Footprint size
    private int floors;            // Number of floors (affects render height)
    private int colorIndex;        // Index into color palette
    
    public Building(double x, double y, double width, double height, int floors, int colorIndex) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.floors = floors;
        this.colorIndex = colorIndex;
    }
    
    /**
     * Check if a circle intersects this building
     */
    public boolean intersectsCircle(double cx, double cy, double radius) {
        // Find closest point on rectangle to circle center
        double closestX = Math.max(x, Math.min(cx, x + width));
        double closestY = Math.max(y, Math.min(cy, y + height));
        
        double dx = cx - closestX;
        double dy = cy - closestY;
        
        return (dx * dx + dy * dy) < (radius * radius);
    }
    
    /**
     * Check if point is inside building
     */
    public boolean containsPoint(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getFloors() { return floors; }
    public int getColorIndex() { return colorIndex; }
    
    public double getCenterX() { return x + width / 2; }
    public double getCenterY() { return y + height / 2; }
    
    public double getRenderHeight() {
        return floors * 3.0; // 3 meters per floor
    }
}
