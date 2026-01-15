package world;

/**
 * Represents a street light in the city
 */
public class StreetLight {
    
    private double x, y;       // Position
    private double rotation;   // Direction light faces
    private boolean isOn;      // Light state
    private int style;         // Visual style variant
    
    public StreetLight(double x, double y, double rotation, int style) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.isOn = true;
        this.style = style;
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getRotation() { return rotation; }
    public boolean isOn() { return isOn; }
    public int getStyle() { return style; }
    
    public void setOn(boolean on) { this.isOn = on; }
}
