package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the shop inventory and player upgrades
 */
public class Shop {
    
    private List<ShopItem> allItems;
    private Map<ShopItem.Category, ShopItem> equippedItems;
    private int playerMoney;
    
    // Default/stock parts (always owned, baseline stats)
    private Map<ShopItem.Category, ShopItem> stockParts;
    
    public Shop() {
        this.allItems = new ArrayList<>();
        this.equippedItems = new HashMap<>();
        this.stockParts = new HashMap<>();
        this.playerMoney = 0;
        
        initializeItems();
        initializeStockParts();
    }
    
    private void initializeStockParts() {
        // Stock parts - free, baseline stats
        ShopItem stockSteering = new ShopItem("stock_steering", "Stock Steering", 
            "Factory steering rack", ShopItem.Category.STEERING, 0, 1.0);
        stockSteering.setOwned(true);
        stockSteering.setEquipped(true);
        stockParts.put(ShopItem.Category.STEERING, stockSteering);
        equippedItems.put(ShopItem.Category.STEERING, stockSteering);
        
        ShopItem stockEngine = new ShopItem("stock_engine", "Stock Engine", 
            "350 HP Factory engine", ShopItem.Category.ENGINE, 0, 1.0);
        stockEngine.setOwned(true);
        stockEngine.setEquipped(true);
        stockParts.put(ShopItem.Category.ENGINE, stockEngine);
        equippedItems.put(ShopItem.Category.ENGINE, stockEngine);
        
        ShopItem stockTires = new ShopItem("stock_tires", "Street Tires", 
            "Balanced grip and durability", ShopItem.Category.TIRES, 0, 1.0, 1.0);
        stockTires.setOwned(true);
        stockTires.setEquipped(true);
        stockParts.put(ShopItem.Category.TIRES, stockTires);
        equippedItems.put(ShopItem.Category.TIRES, stockTires);
        
        ShopItem stockSuspension = new ShopItem("stock_suspension", "Stock Suspension", 
            "Factory suspension setup", ShopItem.Category.SUSPENSION, 0, 1.0);
        stockSuspension.setOwned(true);
        stockSuspension.setEquipped(true);
        stockParts.put(ShopItem.Category.SUSPENSION, stockSuspension);
        equippedItems.put(ShopItem.Category.SUSPENSION, stockSuspension);
        
        ShopItem stockWeight = new ShopItem("stock_weight", "Stock Body", 
            "Full interior, stock panels", ShopItem.Category.WEIGHT, 0, 1.0);
        stockWeight.setOwned(true);
        stockWeight.setEquipped(true);
        stockParts.put(ShopItem.Category.WEIGHT, stockWeight);
        equippedItems.put(ShopItem.Category.WEIGHT, stockWeight);
    }
    
    private void initializeItems() {
        // ========== STEERING UPGRADES ==========
        allItems.add(new ShopItem("steering_1", "Quick Ratio Rack", 
            "+15% steering angle\nFaster turn-in response", 
            ShopItem.Category.STEERING, 500, 1.15));
        
        allItems.add(new ShopItem("steering_2", "Racing Steering Rack", 
            "+25% steering angle\nPrecision engineered", 
            ShopItem.Category.STEERING, 1500, 1.25));
        
        allItems.add(new ShopItem("steering_3", "Hydraulic Angle Kit", 
            "+40% steering angle\nExtreme lock for big drifts", 
            ShopItem.Category.STEERING, 4000, 1.40));
        
        allItems.add(new ShopItem("steering_4", "Competition Angle Kit", 
            "+60% steering angle\nPro-level steering", 
            ShopItem.Category.STEERING, 8000, 1.60));
        
        // ========== ENGINE UPGRADES ==========
        allItems.add(new ShopItem("engine_1", "Cold Air Intake", 
            "+10% power (385 HP)\nImproved throttle response", 
            ShopItem.Category.ENGINE, 800, 1.10));
        
        allItems.add(new ShopItem("engine_2", "Turbo Kit", 
            "+30% power (455 HP)\nMassive power boost", 
            ShopItem.Category.ENGINE, 3500, 1.30));
        
        allItems.add(new ShopItem("engine_3", "Built Motor + Turbo", 
            "+50% power (525 HP)\nForged internals", 
            ShopItem.Category.ENGINE, 8000, 1.50));
        
        allItems.add(new ShopItem("engine_4", "Stroker Kit + Big Turbo", 
            "+80% power (630 HP)\nMaximum power", 
            ShopItem.Category.ENGINE, 15000, 1.80));
        
        // ========== TIRE COMPOUNDS ==========
        // value = grip multiplier, durability = wear rate (lower = wears faster)
        allItems.add(new ShopItem("tires_sport", "Sport Compound", 
            "+15% grip\nModerate wear rate", 
            ShopItem.Category.TIRES, 600, 1.15, 0.8));
        
        allItems.add(new ShopItem("tires_semi", "Semi-Slick", 
            "+25% grip\nFaster wear", 
            ShopItem.Category.TIRES, 1200, 1.25, 0.6));
        
        allItems.add(new ShopItem("tires_slick", "Racing Slicks", 
            "+40% grip\nWears quickly", 
            ShopItem.Category.TIRES, 2500, 1.40, 0.4));
        
        allItems.add(new ShopItem("tires_drift", "Drift Compound", 
            "-10% grip, super slidey\nVery durable", 
            ShopItem.Category.TIRES, 1000, 0.90, 1.2));
        
        allItems.add(new ShopItem("tires_extreme", "Competition Slicks", 
            "+50% grip\nWears very fast!", 
            ShopItem.Category.TIRES, 5000, 1.50, 0.25));
        
        // ========== SUSPENSION ==========
        allItems.add(new ShopItem("susp_1", "Lowering Springs", 
            "+10% handling\nLower center of gravity", 
            ShopItem.Category.SUSPENSION, 400, 1.10));
        
        allItems.add(new ShopItem("susp_2", "Coilovers", 
            "+20% handling\nAdjustable damping", 
            ShopItem.Category.SUSPENSION, 1500, 1.20));
        
        allItems.add(new ShopItem("susp_3", "Racing Coilovers", 
            "+30% handling\nTrack-ready setup", 
            ShopItem.Category.SUSPENSION, 4000, 1.30));
        
        allItems.add(new ShopItem("susp_4", "Pro Drift Suspension", 
            "+40% handling\nKnuckle & arm kit", 
            ShopItem.Category.SUSPENSION, 7500, 1.40));
        
        // ========== WEIGHT REDUCTION ==========
        allItems.add(new ShopItem("weight_1", "Interior Delete", 
            "-5% weight\nRemove rear seats & carpet", 
            ShopItem.Category.WEIGHT, 300, 0.95));
        
        allItems.add(new ShopItem("weight_2", "Lightweight Panels", 
            "-10% weight\nFiberglass hood & trunk", 
            ShopItem.Category.WEIGHT, 1200, 0.90));
        
        allItems.add(new ShopItem("weight_3", "Carbon Fiber Kit", 
            "-15% weight\nFull carbon body panels", 
            ShopItem.Category.WEIGHT, 4000, 0.85));
        
        allItems.add(new ShopItem("weight_4", "Full Lightweight Build", 
            "-25% weight\nRoll cage, lexan windows", 
            ShopItem.Category.WEIGHT, 10000, 0.75));
    }
    
    /**
     * Attempt to purchase an item
     * @return true if purchase successful
     */
    public boolean purchaseItem(ShopItem item) {
        if (item.isOwned()) {
            return false; // Already owned
        }
        if (playerMoney < item.getPrice()) {
            return false; // Not enough money
        }
        
        playerMoney -= item.getPrice();
        item.setOwned(true);
        return true;
    }
    
    /**
     * Equip an owned item
     */
    public boolean equipItem(ShopItem item) {
        if (!item.isOwned()) {
            return false;
        }
        
        // Unequip current item in same category
        ShopItem current = equippedItems.get(item.getCategory());
        if (current != null) {
            current.setEquipped(false);
        }
        
        // Also check stock parts
        ShopItem stockPart = stockParts.get(item.getCategory());
        if (stockPart != null) {
            stockPart.setEquipped(false);
        }
        
        item.setEquipped(true);
        equippedItems.put(item.getCategory(), item);
        return true;
    }
    
    /**
     * Equip stock part for a category
     */
    public void equipStock(ShopItem.Category category) {
        ShopItem current = equippedItems.get(category);
        if (current != null) {
            current.setEquipped(false);
        }
        
        ShopItem stock = stockParts.get(category);
        if (stock != null) {
            stock.setEquipped(true);
            equippedItems.put(category, stock);
        }
    }
    
    /**
     * Add money (from drift scores)
     */
    public void addMoney(int amount) {
        playerMoney += amount;
    }
    
    /**
     * Get modifier value for a category
     */
    public double getModifier(ShopItem.Category category) {
        ShopItem equipped = equippedItems.get(category);
        return equipped != null ? equipped.getValue() : 1.0;
    }
    
    /**
     * Get tire durability modifier
     */
    public double getTireDurability() {
        ShopItem tires = equippedItems.get(ShopItem.Category.TIRES);
        return tires != null ? tires.getDurability() : 1.0;
    }
    
    /**
     * Get equipped item for category
     */
    public ShopItem getEquipped(ShopItem.Category category) {
        return equippedItems.get(category);
    }
    
    /**
     * Get all items in a category
     */
    public List<ShopItem> getItemsByCategory(ShopItem.Category category) {
        List<ShopItem> result = new ArrayList<>();
        // Add stock part first
        ShopItem stock = stockParts.get(category);
        if (stock != null) {
            result.add(stock);
        }
        // Add purchasable items
        for (ShopItem item : allItems) {
            if (item.getCategory() == category) {
                result.add(item);
            }
        }
        return result;
    }
    
    public List<ShopItem> getAllItems() { return allItems; }
    public int getPlayerMoney() { return playerMoney; }
    public void setPlayerMoney(int money) { this.playerMoney = money; }
    public ShopItem getStockPart(ShopItem.Category category) { return stockParts.get(category); }
}
