package world;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a road segment in the city
 * Supports both straight and curved roads
 */
public class Road {
    
    private double x1, y1;    // Start point
    private double x2, y2;    // End point
    private double width;     // Road width
    private boolean vertical; // Orientation (for straight roads)
    private boolean curved;   // Is this a curved road?
    
    // Curve properties
    private double centerX, centerY; // Arc center
    private double radius;           // Arc radius
    private double startAngle;       // Start angle in radians
    private double endAngle;         // End angle in radians
    private List<double[]> curvePoints; // Pre-calculated curve points
    
    // Road type for visual variety
    private RoadType roadType;
    
    public enum RoadType {
        MAIN_ROAD,      // Wide, major road
        SIDE_STREET,    // Narrower side street
        HIGHWAY,        // Very wide, multi-lane
        ALLEY           // Narrow back alley
    }
    
    // Straight road constructor
    public Road(double x1, double y1, double x2, double y2, double width, boolean vertical) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.width = width;
        this.vertical = vertical;
        this.curved = false;
        this.roadType = RoadType.MAIN_ROAD;
    }
    
    // Curved road constructor
    public Road(double centerX, double centerY, double radius, double startAngle, double endAngle, double width) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.width = width;
        this.curved = true;
        this.roadType = RoadType.MAIN_ROAD;
        
        // Calculate start/end points for bounds
        this.x1 = centerX + Math.cos(startAngle) * radius;
        this.y1 = centerY + Math.sin(startAngle) * radius;
        this.x2 = centerX + Math.cos(endAngle) * radius;
        this.y2 = centerY + Math.sin(endAngle) * radius;
        
        // Pre-calculate curve points for collision and rendering
        generateCurvePoints();
    }
    
    private void generateCurvePoints() {
        curvePoints = new ArrayList<>();
        int segments = 20;
        double angleStep = (endAngle - startAngle) / segments;
        
        for (int i = 0; i <= segments; i++) {
            double angle = startAngle + i * angleStep;
            double px = centerX + Math.cos(angle) * radius;
            double py = centerY + Math.sin(angle) * radius;
            curvePoints.add(new double[]{px, py});
        }
    }
    
    public void setRoadType(RoadType type) {
        this.roadType = type;
    }
    
    /**
     * Check if point is on this road
     */
    public boolean containsPoint(double px, double py) {
        if (curved) {
            return containsPointCurved(px, py);
        }
        
        if (vertical) {
            return px >= x1 - width / 2 && px <= x1 + width / 2 &&
                   py >= Math.min(y1, y2) && py <= Math.max(y1, y2);
        } else {
            return py >= y1 - width / 2 && py <= y1 + width / 2 &&
                   px >= Math.min(x1, x2) && px <= Math.max(x1, x2);
        }
    }
    
    private boolean containsPointCurved(double px, double py) {
        // Check distance from arc center
        double dx = px - centerX;
        double dy = py - centerY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        // Check if within road width of the arc
        if (dist < radius - width / 2 || dist > radius + width / 2) {
            return false;
        }
        
        // Check if within angle range
        double angle = Math.atan2(dy, dx);
        double start = normalizeAngle(startAngle);
        double end = normalizeAngle(endAngle);
        angle = normalizeAngle(angle);
        
        if (start < end) {
            return angle >= start && angle <= end;
        } else {
            return angle >= start || angle <= end;
        }
    }
    
    private double normalizeAngle(double angle) {
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
        return angle;
    }
    
    /**
     * Get the bounding rectangle of this road
     */
    public double[] getBounds() {
        if (curved) {
            // Return bounding box of the arc
            double minX = Math.min(x1, x2) - width;
            double maxX = Math.max(x1, x2) + width;
            double minY = Math.min(y1, y2) - width;
            double maxY = Math.max(y1, y2) + width;
            
            // Check for extreme points on the arc
            double[] angles = {0, Math.PI/2, Math.PI, 3*Math.PI/2};
            for (double a : angles) {
                if (isAngleInRange(a)) {
                    double px = centerX + Math.cos(a) * (radius + width/2);
                    double py = centerY + Math.sin(a) * (radius + width/2);
                    minX = Math.min(minX, px - width);
                    maxX = Math.max(maxX, px + width);
                    minY = Math.min(minY, py - width);
                    maxY = Math.max(maxY, py + width);
                }
            }
            
            return new double[] { minX, minY, maxX - minX, maxY - minY };
        }
        
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
    
    private boolean isAngleInRange(double angle) {
        double start = normalizeAngle(startAngle);
        double end = normalizeAngle(endAngle);
        angle = normalizeAngle(angle);
        
        if (start < end) {
            return angle >= start && angle <= end;
        } else {
            return angle >= start || angle <= end;
        }
    }
    
    // Getters
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    public double getWidth() { return width; }
    public boolean isVertical() { return vertical; }
    public boolean isCurved() { return curved; }
    public double getCenterX() { return centerX; }
    public double getCenterY() { return centerY; }
    public double getRadius() { return radius; }
    public double getStartAngle() { return startAngle; }
    public double getEndAngle() { return endAngle; }
    public RoadType getRoadType() { return roadType; }
    public List<double[]> getCurvePoints() { return curvePoints; }
}
