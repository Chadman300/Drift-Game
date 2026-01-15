package world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a parking lot in the city
 */
public class ParkingLot {
    
    private double x, y;           // Position (top-left)
    private double width, height;  // Size
    private int rows, cols;        // Parking space layout
    private List<ParkingSpace> spaces;
    
    public ParkingLot(double x, double y, double width, double height, Random random) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        // Calculate parking layout
        double spaceWidth = 8;
        double spaceHeight = 14;
        
        this.cols = (int)(width / spaceWidth);
        this.rows = (int)(height / (spaceHeight * 2 + 6)); // Two rows with driving lane
        
        this.spaces = new ArrayList<>();
        generateSpaces(random);
    }
    
    private void generateSpaces(Random random) {
        double spaceWidth = 8;
        double spaceHeight = 14;
        double laneWidth = 6;
        
        for (int row = 0; row < rows; row++) {
            double rowY = y + row * (spaceHeight * 2 + laneWidth);
            
            // Top row of spaces
            for (int col = 0; col < cols; col++) {
                double spaceX = x + col * spaceWidth;
                boolean hasCar = random.nextDouble() < 0.6; // 60% occupied
                int carColor = random.nextInt(6);
                spaces.add(new ParkingSpace(spaceX, rowY, spaceWidth, spaceHeight, hasCar, carColor));
            }
            
            // Bottom row of spaces (flipped)
            for (int col = 0; col < cols; col++) {
                double spaceX = x + col * spaceWidth;
                double spaceY = rowY + spaceHeight + laneWidth;
                boolean hasCar = random.nextDouble() < 0.6;
                int carColor = random.nextInt(6);
                spaces.add(new ParkingSpace(spaceX, spaceY, spaceWidth, spaceHeight, hasCar, carColor));
            }
        }
    }
    
    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public List<ParkingSpace> getSpaces() { return spaces; }
    
    /**
     * A single parking space
     */
    public static class ParkingSpace {
        public double x, y;
        public double width, height;
        public boolean hasCar;
        public int carColor;
        
        public ParkingSpace(double x, double y, double w, double h, boolean hasCar, int carColor) {
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
            this.hasCar = hasCar;
            this.carColor = carColor;
        }
    }
}
