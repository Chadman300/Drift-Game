package world;

/**
 * Represents a building in the city
 * Supports rectangular, circular, L-shaped, and other building types
 */
public class Building {
    
    public enum BuildingShape {
        RECTANGLE,
        CIRCLE,
        OCTAGON,
        TRIANGLE
    }
    
    private double x, y;           // Position (center for circles, top-left for rectangles)
    private double width, height;  // Footprint size (radius for circles)
    private int floors;            // Number of floors (affects render height)
    private int colorIndex;        // Index into color palette
    private BuildingShape shape;   // Shape of the building
    private double rotation;       // Rotation angle in radians (for non-circular shapes)
    
    // Rectangle constructor (backwards compatible)
    public Building(double x, double y, double width, double height, int floors, int colorIndex) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.floors = floors;
        this.colorIndex = colorIndex;
        this.shape = BuildingShape.RECTANGLE;
        this.rotation = 0;
    }
    
    // Full constructor with shape
    public Building(double x, double y, double width, double height, int floors, int colorIndex, BuildingShape shape, double rotation) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.floors = floors;
        this.colorIndex = colorIndex;
        this.shape = shape;
        this.rotation = rotation;
    }
    
    /**
     * Check if a circle intersects this building
     */
    public boolean intersectsCircle(double cx, double cy, double radius) {
        if (shape == BuildingShape.CIRCLE) {
            // Circle to circle collision
            double dx = cx - getCenterX();
            double dy = cy - getCenterY();
            double combinedRadius = radius + width / 2;
            return (dx * dx + dy * dy) < (combinedRadius * combinedRadius);
        }
        
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
        if (shape == BuildingShape.CIRCLE) {
            double dx = px - getCenterX();
            double dy = py - getCenterY();
            return (dx * dx + dy * dy) < (width * width / 4);
        }
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public int getFloors() { return floors; }
    public int getColorIndex() { return colorIndex; }
    public BuildingShape getShape() { return shape; }
    public double getRotation() { return rotation; }
    
    public double getCenterX() { 
        return shape == BuildingShape.CIRCLE ? x : x + width / 2; 
    }
    public double getCenterY() { 
        return shape == BuildingShape.CIRCLE ? y : y + height / 2; 
    }
    
    public double getRadius() {
        return width / 2; // For circular buildings
    }
    
    public double getRenderHeight() {
        return floors * 3.0; // 3 meters per floor
    }
}
