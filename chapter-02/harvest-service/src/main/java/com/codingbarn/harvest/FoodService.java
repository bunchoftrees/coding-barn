package com.codingbarn.harvest;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Simple food service - completely public, no authentication needed.
 */
@Service
public class FoodService {
    
    private final List<FoodItem> menu = List.of(
        new FoodItem("Apple Cider", "Beverage", "Fresh pressed from local apples"),
        new FoodItem("Pumpkin Pie", "Dessert", "Made with real pumpkins from the patch"),
        new FoodItem("Corn on the Cob", "Side", "Grilled with butter and herbs"),
        new FoodItem("BBQ Pulled Pork", "Main", "Slow cooked for 12 hours"),
        new FoodItem("Coleslaw", "Side", "Crispy cabbage with tangy dressing"),
        new FoodItem("Cornbread", "Side", "Sweet and buttery"),
        new FoodItem("Apple Fritters", "Dessert", "Warm and cinnamon-dusted")
    );
    
    public List<FoodItem> getAllFood() {
        return menu;
    }
}

record FoodItem(String name, String category, String description) {}
