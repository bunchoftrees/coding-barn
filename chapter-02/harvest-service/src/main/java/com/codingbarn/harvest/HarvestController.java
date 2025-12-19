package com.codingbarn.harvest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public harvest party endpoints.
 * 
 * Key insight: These endpoints are PUBLIC (no auth required for party guests),
 * but the service internally uses OAuth to fetch music data from shed-service.
 * 
 * This demonstrates the BFF (Backend for Frontend) pattern where:
 * - Guests can access these endpoints without authentication
 * - harvest-service acts as an authenticated client to shed-service
 * - Just like guests at a party can hear the music from the shed speakers
 *   without needing keys to the shed
 */
@RestController
@RequestMapping("/harvest")
public class HarvestController {
    
    private static final Logger log = LoggerFactory.getLogger(HarvestController.class);
    
    private final FoodService foodService;
    private final MusicClient musicClient;
    
    public HarvestController(FoodService foodService, MusicClient musicClient) {
        this.foodService = foodService;
        this.musicClient = musicClient;
    }
    
    /**
     * Get available food at the party.
     * Completely public - no authentication needed.
     */
    @GetMapping("/food")
    public List<FoodItem> getFood() {
        log.info("Guest accessed food menu");
        return foodService.getAllFood();
    }
    
    /**
     * Get what's currently playing at the party.
     * 
     * Public endpoint, but internally:
     * 1. harvest-service requests OAuth token from auth-server
     * 2. Uses that token to call shed-service
     * 3. Returns the data to the guest
     * 
     * The guest never sees the OAuth dance - they just see the music info,
     * like hearing speakers at a party.
     */
    @GetMapping("/nowplaying")
    public NowPlayingInfo getNowPlaying() {
        log.info("Guest accessed now playing info");
        
        Song song = musicClient.getCurrentSong();
        
        return new NowPlayingInfo(
            song.title(),
            song.artist(),
            "♪ Now playing at the harvest party ♪"
        );
    }
    
    @GetMapping("/health")
    public String health() {
        return "Harvest service is running - party's happening!";
    }
}

record NowPlayingInfo(String title, String artist, String message) {}
