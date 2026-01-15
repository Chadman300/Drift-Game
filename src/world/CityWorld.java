package world;

import util.GameConstants;
import util.Vector2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents the city environment
 * Generates procedural city blocks with roads, parking lots, and street lights
 */
public class CityWorld {
    
    // World elements
    private List<Building> buildings;
    private List<Road> roads;
    private List<ParkingLot> parkingLots;
    private List<StreetLight> streetLights;
    
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
        this.parkingLots = new ArrayList<>();
        this.streetLights = new ArrayList<>();
        
        generateCity();
    }
    
    /**
     * Generate the city layout with curved roads and variety
     */
    private void generateCity() {
        int numBlocks = (int)(worldSize / blockSize);
        
        // Generate main grid of roads
        for (int i = 0; i <= numBlocks; i++) {
            // Vertical roads
            double x = i * blockSize - worldSize / 2;
            Road vRoad = new Road(x, -worldSize / 2, x, worldSize / 2, roadWidth, true);
            // Main roads are wider
            if (i % 3 == 0) {
                vRoad = new Road(x, -worldSize / 2, x, worldSize / 2, roadWidth * 1.3, true);
                vRoad.setRoadType(Road.RoadType.HIGHWAY);
            }
            roads.add(vRoad);
            
            // Horizontal roads
            double y = i * blockSize - worldSize / 2;
            Road hRoad = new Road(-worldSize / 2, y, worldSize / 2, y, roadWidth, false);
            if (i % 3 == 0) {
                hRoad = new Road(-worldSize / 2, y, worldSize / 2, y, roadWidth * 1.3, false);
                hRoad.setRoadType(Road.RoadType.HIGHWAY);
            }
            roads.add(hRoad);
            
            // Add street lights along roads
            generateStreetLightsForRoad(x, y, i == numBlocks);
        }
        
        // Add curved roads at some intersections for variety
        generateCurvedRoads(numBlocks);
        
        // Generate buildings and parking lots in each block
        for (int bx = 0; bx < numBlocks; bx++) {
            for (int by = 0; by < numBlocks; by++) {
                generateBlock(bx, by);
            }
        }
    }
    
    /**
     * Generate street lights along roads
     */
    private void generateStreetLightsForRoad(double roadX, double roadY, boolean isLast) {
        double spacing = blockSize / 2.0;
        
        // Lights along vertical road (on both sides)
        for (double y = -worldSize / 2; y < worldSize / 2; y += spacing) {
            if (random.nextDouble() < 0.7) {
                streetLights.add(new StreetLight(roadX - roadWidth/2 - 2, y, 0, random.nextInt(3)));
            }
            if (random.nextDouble() < 0.7) {
                streetLights.add(new StreetLight(roadX + roadWidth/2 + 2, y, Math.PI, random.nextInt(3)));
            }
        }
    }
    
    /**
     * Generate curved roads at select locations
     */
    private void generateCurvedRoads(int numBlocks) {
        // Add curved connector roads in some blocks
        for (int i = 1; i < numBlocks - 1; i += 2) {
            for (int j = 1; j < numBlocks - 1; j += 2) {
                if (random.nextDouble() < 0.3) {
                    // Create a curved road connecting two straight roads
                    double centerX = (i + 0.5) * blockSize - worldSize / 2;
                    double centerY = (j + 0.5) * blockSize - worldSize / 2;
                    double radius = blockSize * 0.4;
                    
                    // Random quarter-circle curve
                    double startAngle = random.nextInt(4) * Math.PI / 2;
                    double endAngle = startAngle + Math.PI / 2;
                    
                    Road curvedRoad = new Road(centerX, centerY, radius, startAngle, endAngle, roadWidth * 0.8);
                    curvedRoad.setRoadType(Road.RoadType.SIDE_STREET);
                    roads.add(curvedRoad);
                }
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
        
        // Determine block type based on location and random
        double distFromCenter = Math.sqrt(
            Math.pow(startX + availableSize/2, 2) + 
            Math.pow(startY + availableSize/2, 2)
        );
        
        // Central area has more commercial buildings
        boolean isCentral = distFromCenter < worldSize * 0.3;
        
        // Random block type
        double roll = random.nextDouble();
        
        if (roll < 0.12) {
            // Parking lot
            generateParkingLot(startX, startY, availableSize);
        } else if (roll < 0.18) {
            // Empty lot / park (no buildings)
            return;
        } else if (isCentral && roll < 0.45) {
            // Downtown: tall single building
            generateSkyscraper(startX, startY, availableSize);
        } else if (roll < 0.55) {
            // Four medium buildings
            generateQuadBuildings(startX, startY, availableSize);
        } else if (roll < 0.70) {
            // Many small buildings
            generateSmallBuildings(startX, startY, availableSize);
        } else if (roll < 0.85) {
            // L-shaped or U-shaped building
            generateShapedBuilding(startX, startY, availableSize);
        } else {
            // Mix of sizes
            generateMixedBuildings(startX, startY, availableSize);
        }
    }
    
    /**
     * Generate a parking lot in a block
     */
    private void generateParkingLot(double x, double y, double size) {
        double margin = 5;
        ParkingLot lot = new ParkingLot(
            x + margin, y + margin, 
            size - margin * 2, size - margin * 2, 
            random
        );
        parkingLots.add(lot);
        
        // Maybe add a small building next to parking
        if (random.nextDouble() < 0.4) {
            double bSize = size * 0.25;
            int height = 2 + random.nextInt(3);
            int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
            buildings.add(new Building(x + margin, y + margin, bSize, bSize, height, colorIndex));
        }
    }
    
    /**
     * Generate a tall downtown building
     */
    private void generateSkyscraper(double x, double y, double size) {
        double margin = 8;
        double buildingSize = size - margin * 2;
        int height = 12 + random.nextInt(18); // Tall buildings
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        
        // Main tower
        buildings.add(new Building(
            x + margin, y + margin,
            buildingSize, buildingSize,
            height, colorIndex
        ));
        
        // Maybe add a smaller attached structure
        if (random.nextDouble() < 0.5) {
            double smallSize = buildingSize * 0.3;
            buildings.add(new Building(
                x + margin + buildingSize - smallSize, 
                y + margin + buildingSize - smallSize,
                smallSize, smallSize,
                height / 2, colorIndex
            ));
        }
    }
    
    /**
     * Generate L-shaped or U-shaped buildings
     */
    private void generateShapedBuilding(double x, double y, double size) {
        double margin = 4;
        int height = 4 + random.nextInt(8);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        double thickness = size * 0.3;
        
        if (random.nextBoolean()) {
            // L-shape
            buildings.add(new Building(x + margin, y + margin, size - margin * 2, thickness, height, colorIndex));
            buildings.add(new Building(x + margin, y + margin + thickness, thickness, size - margin * 2 - thickness, height, colorIndex));
        } else {
            // U-shape
            buildings.add(new Building(x + margin, y + margin, thickness, size - margin * 2, height, colorIndex));
            buildings.add(new Building(x + size - margin - thickness, y + margin, thickness, size - margin * 2, height, colorIndex));
            buildings.add(new Building(x + margin, y + margin, size - margin * 2, thickness, height, colorIndex));
        }
    }
    
    private void generateQuadBuildings(double x, double y, double size) {
        double margin = 3;
        double gap = 8;
        double buildingSize = (size - gap) / 2 - margin;
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 3 + random.nextInt(10);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                // Slight size variation
                double sizeVar = buildingSize * (0.8 + random.nextDouble() * 0.2);
                buildings.add(new Building(bx, by, sizeVar, sizeVar, height, colorIndex));
            }
        }
    }
    
    private void generateSmallBuildings(double x, double y, double size) {
        double margin = 2;
        double gap = 5;
        int count = 3;
        double buildingSize = (size - gap * (count - 1)) / count - margin;
        
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (random.nextDouble() < 0.15) continue; // Some gaps
                
                double bx = x + margin + i * (buildingSize + gap);
                double by = y + margin + j * (buildingSize + gap);
                int height = 2 + random.nextInt(6);
                int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
                
                // Varying sizes
                double w = buildingSize * (0.7 + random.nextDouble() * 0.3);
                double h = buildingSize * (0.7 + random.nextDouble() * 0.3);
                
                buildings.add(new Building(bx, by, w, h, height, colorIndex));
            }
        }
    }
    
    private void generateMixedBuildings(double x, double y, double size) {
        double margin = 3;
        double largeSize = size * 0.55 - margin * 2;
        
        // One large building
        int height = 8 + random.nextInt(12);
        int colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
        buildings.add(new Building(x + margin, y + margin, largeSize, largeSize, height, colorIndex));
        
        // Small buildings on the sides
        double smallSize = size * 0.35 - margin;
        for (int i = 0; i < 2; i++) {
            double bx = x + size * 0.60 + margin;
            double by = y + margin + i * (smallSize + margin * 2);
            height = 2 + random.nextInt(5);
            colorIndex = random.nextInt(GameConstants.COLOR_PALETTE.length);
            
            double w = smallSize * (0.8 + random.nextDouble() * 0.2);
            double h = smallSize * (0.7 + random.nextDouble() * 0.3);
            buildings.add(new Building(bx, by, w, h, height, colorIndex));
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
        // Check if on parking lot
        for (ParkingLot lot : parkingLots) {
            if (x >= lot.getX() && x <= lot.getX() + lot.getWidth() &&
                y >= lot.getY() && y <= lot.getY() + lot.getHeight()) {
                return SurfaceType.CONCRETE;
            }
        }
        return SurfaceType.CONCRETE;
    }
    
    // Getters
    public List<Building> getBuildings() { return buildings; }
    public List<Road> getRoads() { return roads; }
    public List<ParkingLot> getParkingLots() { return parkingLots; }
    public List<StreetLight> getStreetLights() { return streetLights; }
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
