package world;

/**
 * Represents a road segment in the city
 */
public class Road {
    
    private double x1, y1;    // Start point
    private double x2, y2;    // End point
    private double width;     // Road width
    private boolean vertical; // Orientation
    
    public Road(double x1, double y1, double x2, double y2, double width, boolean vertical) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.vertical = vertical;
    }
    
    /**
     * Check if point is on this road
     */
    public boolean containsPoint(double px, double py) {
        if (vertical) {
            return px >= x1 - width / 2 && px <= x1 + width / 2 &&
                   py >= Math.min(y1, y2) && py <= Math.max(y1, y2);
        } else {
            return py >= y1 - width / 2 && py <= y1 + width / 2 &&
                   px >= Math.min(x1, x2) && px <= Math.max(x1, x2);
        }
    }
    
    /**
     * Get the bounding rectangle of this road
     */
    public double[] getBounds() {
        if (vertical) {
            return new double[] {
                x1 - width / 2,
                Math.min(y1, y2),
                width,
                Math.abs(y2 - y1)
            };
        } else {
            return new double[] {
                Math.min(x1, x2),
                y1 - width / 2,
                Math.abs(x2 - x1),
                width
            };
        }
    }
    
    // Getters
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public double getWidth() { return width; }
    public boolean isVertical() { return vertical; }
}
