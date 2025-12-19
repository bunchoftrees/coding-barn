package com.codingbarn.shed;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages expensive music equipment in the shed.
 * This is what we DON'T want harvest-service to have access to.
 */
@Service
public class EquipmentService {
    
    private final List<Equipment> equipment = new CopyOnWriteArrayList<>(List.of(
        new Equipment("1", "MacBook Pro", "Computer", 3000),
        new Equipment("2", "Focusrite Scarlett 2i2", "Audio Interface", 180),
        new Equipment("3", "KRK Rokit 5", "Studio Monitor (Pair)", 400),
        new Equipment("4", "Shure SM58", "Microphone", 100),
        new Equipment("5", "Audio-Technica AT-LP120", "Turntable", 300),
        new Equipment("6", "Behringer X32", "Mixer", 2500)
    ));
    
    public List<Equipment> getAllEquipment() {
        return List.copyOf(equipment);
    }
    
    public void removeEquipment(String id) {
        equipment.removeIf(e -> e.id().equals(id));
    }
    
    public void removeAllEquipment() {
        equipment.clear();
    }
    
    public int getTotalValue() {
        return equipment.stream()
            .mapToInt(Equipment::valueUSD)
            .sum();
    }
}

record Equipment(String id, String name, String type, int valueUSD) {}
