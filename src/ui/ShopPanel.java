package ui;

import game.Shop;
import game.ShopItem;
import java.awt.*;
import java.util.List;

/**
 * Renders the shop menu UI in pixel art style
 * Full screen overlay matching game aesthetic
 */
public class ShopPanel {
    
    private Shop shop;;//
    private boolean visible;
    private ShopItem.Category selectedCategory;
    private int selectedItemIndex;
    private int scrollOffset;
    
    // Retro color palette (matching game style)
    private static final Color BG_COLOR = new Color(0x1A1A2E);
    private static final Color PANEL_COLOR = new Color(0x16213E);
    private static final Color ACCENT_COLOR = new Color(0xE94560);
    private static final Color GOLD_COLOR = new Color(0xFFE66D);
    private static final Color CYAN_COLOR = new Color(0x4ECDC4);
    private static final Color TEXT_COLOR = new Color(0xDFE6E9);
    private static final Color DIM_COLOR = new Color(0x636E72);
    private static final Color MONEY_COLOR = new Color(0x00B894);
    private static final Color WARNING_COLOR = new Color(0xFF6B6B);
    private static final Color OWNED_COLOR = new Color(0x74B9FF);
    
    public ShopPanel(Shop shop) {
        this.shop = shop;
        this.visible = false;
        this.selectedCategory = ShopItem.Category.STEERING;
        this.selectedItemIndex = 0;
        this.scrollOffset = 0;
    }
    
    /**
     * Render directly to the buffer graphics (pixel art resolution)
     */
    public void render(Graphics2D g, int renderWidth, int renderHeight) {
        if (!visible) return;
        
        // Full screen dark overlay
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, renderWidth, renderHeight);
        
        // Main panel area with border
        int margin = 8;
        int panelX = margin;
        int panelY = margin;
        int panelW = renderWidth - margin * 2;
        int panelH = renderHeight - margin * 2;
        
        // Panel background
        g.setColor(PANEL_COLOR);
        g.fillRect(panelX, panelY, panelW, panelH);
        
        // Border (pixel art style - just solid lines)
        g.setColor(ACCENT_COLOR);
        g.drawRect(panelX, panelY, panelW, panelH);
        g.drawRect(panelX + 1, panelY + 1, panelW - 2, panelH - 2);
        
        // Title
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        String title = "=== PARTS SHOP ===";
        drawPixelText(g, title, renderWidth / 2 - getTextWidth(g, title) / 2, panelY + 14, GOLD_COLOR);
        
        // Money display
        String moneyText = "$ " + String.format("%,d", shop.getPlayerMoney());
        drawPixelText(g, moneyText, panelX + panelW - getTextWidth(g, moneyText) - 10, panelY + 14, MONEY_COLOR);
        
        // Category tabs
        int tabY = panelY + 26;
        int tabHeight = 16;
        ShopItem.Category[] categories = ShopItem.Category.values();
        int tabWidth = (panelW - 20) / categories.length;
        
        g.setFont(new Font("Monospaced", Font.BOLD, 9));
        for (int i = 0; i < categories.length; i++) {
            int tabX = panelX + 10 + i * tabWidth;
            
            boolean selected = categories[i] == selectedCategory;
            
            // Tab background
            g.setColor(selected ? ACCENT_COLOR : new Color(0x2D3436));
            g.fillRect(tabX, tabY, tabWidth - 2, tabHeight);
            
            // Tab text
            String catName = getCategoryName(categories[i]);
            int textX = tabX + (tabWidth - getTextWidth(g, catName)) / 2;
            drawPixelText(g, catName, textX, tabY + 10, selected ? Color.WHITE : DIM_COLOR);
        }
        
        // Items list area
        int listX = panelX + 10;
        int listY = tabY + tabHeight + 8;
        int listW = panelW - 20;
        int listH = panelH - 70;
        
        // List background
        g.setColor(new Color(0x0D0D0D));
        g.fillRect(listX, listY, listW, listH);
        g.setColor(CYAN_COLOR);
        g.drawRect(listX, listY, listW, listH);
        
        // Draw items
        List<ShopItem> items = shop.getItemsByCategory(selectedCategory);
        int itemHeight = 36;
        int maxVisible = listH / itemHeight;
        
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        
        for (int i = 0; i < Math.min(items.size(), maxVisible); i++) {
            int actualIndex = i + scrollOffset;
            if (actualIndex >= items.size()) break;
            
            ShopItem item = items.get(actualIndex);
            int itemY = listY + 4 + i * itemHeight;
            
            boolean isSelected = actualIndex == selectedItemIndex;
            
            // Selection highlight
            if (isSelected) {
                g.setColor(new Color(0x2D3436));
                g.fillRect(listX + 2, itemY - 2, listW - 4, itemHeight - 2);
                
                // Selection arrow
                drawPixelText(g, ">", listX + 6, itemY + 12, GOLD_COLOR);
            }
            
            int textStartX = listX + 18;
            
            // Item name
            Color nameColor = item.isEquipped() ? GOLD_COLOR : (item.isOwned() ? OWNED_COLOR : TEXT_COLOR);
            drawPixelText(g, item.getName(), textStartX, itemY + 12, nameColor);
            
            // Status indicator
            int statusX = textStartX + 120;
            if (item.isEquipped()) {
                drawPixelText(g, "[EQP]", statusX, itemY + 12, GOLD_COLOR);
            } else if (item.isOwned()) {
                drawPixelText(g, "[OWN]", statusX, itemY + 12, OWNED_COLOR);
            }
            
            // Description (truncated)
            g.setFont(new Font("Monospaced", Font.PLAIN, 8));
            String desc = item.getDescription().split("\n")[0];
            if (desc.length() > 45) desc = desc.substring(0, 42) + "...";
            drawPixelText(g, desc, textStartX, itemY + 24, DIM_COLOR);
            g.setFont(new Font("Monospaced", Font.BOLD, 10));
            
            // Price (right aligned)
            if (!item.isOwned() && item.getPrice() > 0) {
                String priceStr = "$" + String.format("%,d", item.getPrice());
                boolean canAfford = shop.getPlayerMoney() >= item.getPrice();
                int priceX = listX + listW - getTextWidth(g, priceStr) - 10;
                drawPixelText(g, priceStr, priceX, itemY + 12, canAfford ? MONEY_COLOR : WARNING_COLOR);
            }
            
            // Tire durability for tire items
            if (item.getCategory() == ShopItem.Category.TIRES) {
                String durStr = getDurabilityStars(item.getDurability());
                int durX = listX + listW - getTextWidth(g, durStr) - 10;
                drawPixelText(g, durStr, durX, itemY + 24, CYAN_COLOR);
            }
        }
        
        // Scroll indicators
        g.setFont(new Font("Monospaced", Font.BOLD, 8));
        if (scrollOffset > 0) {
            drawPixelText(g, "^", listX + listW / 2 - 3, listY + 8, GOLD_COLOR);
        }
        if (scrollOffset + maxVisible < items.size()) {
            drawPixelText(g, "v", listX + listW / 2 - 3, listY + listH - 3, GOLD_COLOR);
        }
        
        // Controls help bar at bottom
        int helpY = panelY + panelH - 10;
        g.setFont(new Font("Monospaced", Font.PLAIN, 8));
        String controls = "Q/E:Category  W/S:Select  ENTER:Buy/Equip  ESC/TAB:Close";
        int controlsX = renderWidth / 2 - getTextWidth(g, controls) / 2;
        drawPixelText(g, controls, controlsX, helpY, DIM_COLOR);
        
        // Decorative corners (pixel art style)
        g.setColor(GOLD_COLOR);
        // Top-left
        g.fillRect(panelX, panelY, 4, 1);
        g.fillRect(panelX, panelY, 1, 4);
        // Top-right
        g.fillRect(panelX + panelW - 4, panelY, 4, 1);
        g.fillRect(panelX + panelW - 1, panelY, 1, 4);
        // Bottom-left
        g.fillRect(panelX, panelY + panelH - 1, 4, 1);
        g.fillRect(panelX, panelY + panelH - 4, 1, 4);
        // Bottom-right
        g.fillRect(panelX + panelW - 4, panelY + panelH - 1, 4, 1);
        g.fillRect(panelX + panelW - 1, panelY + panelH - 4, 1, 4);
    }
    
    /**
     * Draw text with pixel art style shadow
     */
    private void drawPixelText(Graphics2D g, String text, int x, int y, Color color) {
        // Shadow
        g.setColor(Color.BLACK);
        g.drawString(text, x + 1, y + 1);
        // Main text
        g.setColor(color);
        g.drawString(text, x, y);
    }
    
    private int getTextWidth(Graphics2D g, String text) {
        return g.getFontMetrics().stringWidth(text);
    }
    
    private String getCategoryName(ShopItem.Category cat) {
        switch (cat) {
            case STEERING: return "STEER";
            case ENGINE: return "ENGINE";
            case TIRES: return "TIRES";
            case SUSPENSION: return "SUSP";
            case BRAKES: return "BRAKE";
            case WEIGHT: return "WEIGHT";
            default: return cat.name();
        }
    }
    
    private String getDurabilityStars(double dur) {
        if (dur >= 1.0) return "*****";
        if (dur >= 0.8) return "****-";
        if (dur >= 0.6) return "***--";
        if (dur >= 0.4) return "**---";
        return "*----";
    }
    
    // Navigation methods
    public void nextCategory() {
        ShopItem.Category[] cats = ShopItem.Category.values();
        int index = selectedCategory.ordinal();
        selectedCategory = cats[(index + 1) % cats.length];
        selectedItemIndex = 0;
        scrollOffset = 0;
    }
    
    public void prevCategory() {
        ShopItem.Category[] cats = ShopItem.Category.values();
        int index = selectedCategory.ordinal();
        selectedCategory = cats[(index - 1 + cats.length) % cats.length];
        selectedItemIndex = 0;
        scrollOffset = 0;
    }
    
    public void selectNext() {
        List<ShopItem> items = shop.getItemsByCategory(selectedCategory);
        if (selectedItemIndex < items.size() - 1) {
            selectedItemIndex++;
            // Scroll if needed
            int maxVisible = 6;
            if (selectedItemIndex >= scrollOffset + maxVisible) {
                scrollOffset++;
            }
        }
    }
    
    public void selectPrev() {
        if (selectedItemIndex > 0) {
            selectedItemIndex--;
            if (selectedItemIndex < scrollOffset) {
                scrollOffset = selectedItemIndex;
            }
        }
    }
    
    /**
     * Try to buy or equip selected item
     * @return message to display
     */
    public String confirmSelection() {
        List<ShopItem> items = shop.getItemsByCategory(selectedCategory);
        if (selectedItemIndex >= items.size()) return "";
        
        ShopItem item = items.get(selectedItemIndex);
        
        if (item.isOwned()) {
            if (item.isEquipped()) {
                return "Already equipped!";
            }
            shop.equipItem(item);
            return "Equipped " + item.getName();
        } else {
            if (shop.purchaseItem(item)) {
                shop.equipItem(item);
                return "Purchased & equipped " + item.getName();
            } else {
                return "Not enough money!";
            }
        }
    }
    
    // Visibility
    public void toggle() { visible = !visible; }
    public void show() { visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }
}
