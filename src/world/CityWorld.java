package world;

import util.GameConstants;
import util.Vector2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents the city environment
 * Generates procedural city blocks with roads
 */
public class CityWorld {
    
    // Building data
    private List<Building> buildings;
    private List<Road> roads;
    
    // World bounds
    private double worldSize;
    private int blockSize;
    private int roadWidth;
    
    // Random seed for procedural generation
    private long seed;
    private Random random;
    
    public CityWorld() {
        this(System.currentTimeMillis());
    }
    
    public CityWorld(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
        this.worldSize = GameConstants.WORLD_SIZE;
        this.blockSize = GameConstants.BLOCK_SIZE;
        this.roadWidth = GameConstants.ROAD_WIDTH;
        
        this.buildings = new ArrayList<>();
        this.roads = new ArrayList<>();
        
        generateCity();
    }
    
    /**
     * Generate the city layout
     */
    private void generateCity() {
        int numBlocks = (int)(worldSize / blockSize);
        
        // Generate grid of roads
        for (int i = 0; i <= numBlocks; i++) {
            // Vertical roads
            double x = i * blockSize - worldSize / 2;
            roads.add(new Road(x, -worldSize / 2, x, worldSize / 2, roadWidth, true));
            
            // Horizontal roads
            double y = i * blockSize - worldSize / 2;
            roads.add(new Road(-worldSize / 2, y, worldSize / 2, y, roadWidth, false));
        }
        
        // Generate buildings in each block
        for (int bx = 0; bx < numBlocks; bx++) {
            for (int by = 0; by < numBlocks; by++) {
                generateBlock(bx, by);
            }
        }
    }
    
    /**
     * Generate buildings for a single city block
     */
    private void generateBlock(int blockX, int blockY) {
        double startX = blockX * blockSize - worldSize / 2 + roadWidth / 2;
        double startY = blockY * blockSize - worldSize / 2 + roadWidth / 2;
        double availableSize = blockSize - roadWidth;
        
        // Skip some blocks for variety (parking lots, parks)
        if (random.nextDouble() < 0.1) {
            return; // Empty lot
        }
        
        // Determine block type
        int blockType = random.nextInt(4);
        
        switch (blockType) {
            case 0:
                // Single large building
                generateLargeBuilding(startX, startY, availableSize);
                break;
            case 1:
                // Four medium buildings
                generateQuadBuildings(startX, startY, availableSize);
                break;
            case 2:
                // Many small buildings
                generateSmallBuildings(startX, startY, availableSize);
                break;
            default:
                // Mix of sizes
                generateMixedBuildings(startX, startY, availableSize);
                break;
        }
    }
    
    private void generateLargeBuilding(double x, double y, double size) {
        double margin = 5;
        double buildingSize = size - margin * 2;
        int height = 5 + random.nextInt(15);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        
        buildings.add(new Building(
            x + margin, y + margin,
            buildingSize, buildingSize,
            height, colorIndex
        ));
    }
    
    private void generateQuadBuildings(double x, double y, double size) {
        double margin = 3;
        double gap = 6;
        double buildingSize = (size - gap) / 2 - margin;
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 3 + random.nextInt(10);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                buildings.add(new Building(bx, by, buildingSize, buildingSize, height, colorIndex));
            }
        }
    }
    
    private void generateSmallBuildings(double x, double y, double size) {
        double margin = 2;
        double gap = 4;
        int count = 3;
        double buildingSize = (size - gap * (count - 1)) / count - margin;
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (random.nextDouble() < 0.2) continue; // Some gaps
                
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 2 + random.nextInt(6);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                buildings.add(new Building(bx, by, buildingSize, buildingSize, height, colorIndex));
            }
        }
    }
    
    private void generateMixedBuildings(double x, double y, double size) {
        // One large building and several small ones
        double margin = 3;
        double largeSize = size * 0.6 - margin * 2;
        
        int height = 8 + random.nextInt(12);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        buildings.add(new Building(x + margin, y + margin, largeSize, largeSize, height, colorIndex));
        
        // Small buildings on the sides
        double smallSize = size * 0.35 - margin;
        for (int i = 0; i < 2; i++) {
            double bx = x + size * 0.65 + margin;
            double by = y + margin + i * (smallSize + margin);
            height = 2 + random.nextInt(5);
            colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
            buildings.add(new Building(bx, by, smallSize, smallSize, height, colorIndex));
        }
    }
    
    /**
     * Check if a point is on a road
     */
    public boolean isOnRoad(double x, double y) {
        for (Road road : roads) {
            if (road.containsPoint(x, y)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check collision with buildings
     */
    public Building checkBuildingCollision(double x, double y, double radius) {
        for (Building building : buildings) {
            if (building.intersectsCircle(x, y, radius)) {
                return building;
            }
        }
        return null;
    }
    
    /**
     * Get surface type at position
     */
    public SurfaceType getSurfaceAt(double x, double y) {
        if (isOnRoad(x, y)) {
            return SurfaceType.ASPHALT;
        }
        return SurfaceType.CONCRETE;
    }
    
    // Getters
    public List<Building> getBuildings() { return buildings; }
    public List<Road> getRoads() { return roads; }
    public double getWorldSize() { return worldSize; }
    
    /**
     * Get nearby buildings for rendering optimization
     */
    public List<Building> getNearbyBuildings(double x, double y, double radius) {
        List<Building> nearby = new ArrayList<>();
        for (Building b : buildings) {
            double dx = b.getCenterX() - x;
            double dy = b.getCenterY() - y;
            if (dx * dx + dy * dy < radius * radius) {
                nearby.add(b);
            }
        }
        return nearby;
    }
    
    /**
     * Surface types affect tire grip
     */
    public enum SurfaceType {
        ASPHALT(1.0),
        CONCRETE(0.9),
        GRAVEL(0.6),
        GRASS(0.4);
        
        public final double gripMultiplier;
        
        SurfaceType(double grip) {
            this.gripMultiplier = grip;
        }
    }
}
