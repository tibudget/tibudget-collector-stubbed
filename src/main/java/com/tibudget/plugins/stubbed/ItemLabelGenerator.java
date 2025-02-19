package com.tibudget.plugins.stubbed;

import java.util.List;
import java.util.Random;

public class ItemLabelGenerator {

    private static final List<String> PRODUCTS = List.of(
            // High-tech
            "Laptop", "Gaming Laptop", "Wireless Bluetooth Earbuds", "Smartphone",
            "Ultra HD 4K Smart TV", "Gaming Keyboard", "Wireless Mouse",
            "Mechanical Keyboard", "External SSD 1TB", "Professional DSLR Camera",
            "Smartwatch with Heart Monitor", "Portable Bluetooth Speaker",
            "High-Speed WiFi Router", "Digital Drawing Tablet", "Noise Cancelling Headphones",

            // Électroménager
            "Electric Kettle", "Microwave Oven", "Smart LED Light Bulb",
            "Robot Vacuum Cleaner", "Wireless Charging Pad", "Air Fryer XL",
            "Automatic Coffee Machine", "Smart Door Lock with Fingerprint Scanner",

            // Vêtements & Chaussures
            "Men's Leather Jacket", "Women's Summer Dress", "Casual Sneakers",
            "Running Shoes", "Designer Handbag", "Unisex Hoodie", "Formal Suit",
            "Slim Fit Jeans", "Winter Boots", "Athletic T-Shirt", "Cotton Sweatpants",

            // Parfums & Cosmétiques
            "Luxury Eau de Parfum", "Fresh Citrus Cologne", "Rose Scented Body Mist",
            "Vanilla and Musk Perfume", "Men’s Aftershave Balm", "Organic Face Cream",
            "Aloe Vera Moisturizer", "Exfoliating Body Scrub", "Red Matte Lipstick",

            // Jouets & Enfants
            "LEGO City Set", "Remote Control Car", "Plush Teddy Bear",
            "Educational Wooden Puzzle", "Dinosaur Action Figure", "Barbie Doll House",
            "Interactive Talking Robot", "Baby Stroller", "Kids Play Tent",
            "Board Game: Monopoly", "Musical Toy Piano",

            // Nourriture & Boissons
            "Organic Honey 500g", "Italian Roasted Coffee Beans", "Dark Chocolate Bar",
            "Gourmet Olive Oil 1L", "Freshly Baked Croissant Pack", "Almond & Oat Granola",
            "Bottle of Red Wine", "Japanese Matcha Green Tea", "Premium Sushi Rice",
            "Handmade Raspberry Jam", "Spicy BBQ Sauce", "Vegan Protein Powder",

            // Divers
            "Yoga Mat with Carry Strap", "Waterproof Hiking Backpack", "Luxury Bath Towel Set",
            "Portable Camping Stove", "Rechargeable LED Flashlight", "Hardcover Travel Journal"
    );

    private static final Random RANDOM = new Random();

    public static String generateProductName() {
        return PRODUCTS.get(RANDOM.nextInt(PRODUCTS.size()));
    }
}

