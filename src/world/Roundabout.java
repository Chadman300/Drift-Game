package world;

/**
 * Represents a roundabout (traffic circle) in the city
 */
public class Roundabout {
    
    private double x, y;           // Center position
    private double outerRadius;    // Outer edge of roundabout
    private double innerRadius;    // Central island radius
    private int numExits;          // Number of roads connecting
    private double[] exitAngles;   // Angles of each exit road
    
    public Roundabout(double x, double y, double outerRadius, double innerRadius, int numExits) {
        this.x = x;
        this.y = y;
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.numExits = numExits;
        
        // Calculate exit angles (evenly spaced)
        this.exitAngles = new double[numExits];
        for (int i = 0; i < numExits; i++) {
            exitAngles[i] = (2 * Math.PI * i) / numExits;
        }
    }
    
    public Roundabout(double x, double y, double outerRadius, double innerRadius, double[] exitAngles) {
        this.x = x;
        this.y = y;
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.numExits = exitAngles.length;
        this.exitAngles = exitAngles;
    }
    
    /**
     * Check if a point is on the drivable part of the roundabout
     */
    public boolean containsPoint(double px, double py) {
        double dx = px - x;
        double dy = py - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return dist >= innerRadius && dist <= outerRadius;
    }
    
    /**
     * Check if a circle intersects the roundabout (for car collision)
     */
    public boolean intersectsCircle(double cx, double cy, double radius) {
        double dx = cx - x;
        double dy = cy - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        // Check if circle overlaps with drivable ring
        return dist + radius >= innerRadius && dist - radius <= outerRadius;
    }
    
    /**
     * Check if car collides with center island
     */
    public boolean collidesWithIsland(double cx, double cy, double radius) {
        double dx = cx - x;
        double dy = cy - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return dist < innerRadius + radius;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getOuterRadius() { return outerRadius; }
    public double getInnerRadius() { return innerRadius; }
    public int getNumExits() { return numExits; }
    public double[] getExitAngles() { return exitAngles; }
    
    public double getRoadWidth() {
        return outerRadius - innerRadius;
    }
}
