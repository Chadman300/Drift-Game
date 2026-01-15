package game;

/**
 * Represents a purchasable item in the shop
 */
public class ShopItem {
    
    public enum Category {
        STEERING,   // Turn angle upgrades
        ENGINE,     // Power upgrades
        TIRES,      // Tire compounds
        SUSPENSION, // Handling & stability upgrades
        BRAKES,     // Braking power upgrades
        WEIGHT      // Weight reduction
    }
    
    private String id;
    private String name;
    private String description;
    private Category category;
    private int price;
    private double value;        // The stat modifier value
    private double durability;   // For tires: 0.0 = breaks fast, 1.0 = lasts long
    private boolean owned;
    private boolean equipped;
    
    public ShopItem(String id, String name, String description, Category category, 
                    int price, double value) {
        this(id, name, description, category, price, value, 1.0);
    }
    
    public ShopItem(String id, String name, String description, Category category, 
                    int price, double value, double durability) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.value = value;
        this.durability = durability;
        this.owned = false;
        this.equipped = false;
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public int getPrice() { return price; }
    public double getValue() { return value; }
    public double getDurability() { return durability; }
    public boolean isOwned() { return owned; }
    public boolean isEquipped() { return equipped; }
    
    // Setters
    public void setOwned(boolean owned) { this.owned = owned; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
}
