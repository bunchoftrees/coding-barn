package com.codingbarn.shed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Protected music endpoints.
 * All endpoints require a valid Bearer token with appropriate scope.
 */
@RestController
@RequestMapping("/music")
public class MusicController {
    
    private static final Logger log = LoggerFactory.getLogger(MusicController.class);
    
    private final TokenValidator tokenValidator;
    private final MusicService musicService;
    private final EquipmentService equipmentService;
    
    public MusicController(TokenValidator tokenValidator, 
                          MusicService musicService,
                          EquipmentService equipmentService) {
        this.tokenValidator = tokenValidator;
        this.musicService = musicService;
        this.equipmentService = equipmentService;
    }
    
    /**
     * Get currently playing song.
     * Requires scope: read:nowplaying
     */
    @GetMapping("/nowplaying")
    public Song getNowPlaying(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenInfo token = validateTokenWithScope(authHeader, "read:nowplaying");
        
        Song song = musicService.getCurrentSong();
        log.info("Client {} accessed now playing: {}", token.clientId(), song.title());
        
        return song;
    }
    
    /**
     * Get the full playlist.
     * Requires scope: read:nowplaying
     */
    @GetMapping("/playlist")
    public List<Song> getPlaylist(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenInfo token = validateTokenWithScope(authHeader, "read:nowplaying");
        
        log.info("Client {} accessed full playlist", token.clientId());
        return musicService.getPlaylist();
    }
    
    /**
     * Play a specific song.
     * Requires scope: write:music
     */
    @PostMapping("/play")
    public Song playSong(@RequestHeader(value = "Authorization", required = false) String authHeader,
                        @RequestBody PlayRequest request) {
        TokenInfo token = validateTokenWithScope(authHeader, "write:music");
        
        Song song = musicService.playSong(request.songId());
        log.info("Client {} changed song to: {}", token.clientId(), song.title());
        
        return song;
    }
    
    /**
     * Skip to next song.
     * Requires scope: write:music
     */
    @PostMapping("/next")
    public Song nextSong(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenInfo token = validateTokenWithScope(authHeader, "write:music");
        
        Song song = musicService.nextSong();
        log.info("Client {} skipped to next song: {}", token.clientId(), song.title());
        
        return song;
    }
    
    /**
     * List all equipment in the shed.
     * Requires scope: admin:equipment
     */
    @GetMapping("/equipment")
    public EquipmentList getEquipment(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenInfo token = validateTokenWithScope(authHeader, "admin:equipment");
        
        List<Equipment> items = equipmentService.getAllEquipment();
        int totalValue = equipmentService.getTotalValue();
        
        log.info("Client {} accessed equipment list (total value: ${})", 
            token.clientId(), totalValue);
        
        return new EquipmentList(items, totalValue);
    }
    
    /**
     * Remove all equipment (simulates theft/damage).
     * Requires scope: admin:equipment
     * 
     * This is the dangerous endpoint we DON'T want harvest-service to access!
     */
    @DeleteMapping("/equipment")
    public String removeAllEquipment(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        TokenInfo token = validateTokenWithScope(authHeader, "admin:equipment");
        
        int totalValue = equipmentService.getTotalValue();
        equipmentService.removeAllEquipment();
        
        log.error("⚠️ Client {} DELETED ALL EQUIPMENT (value: ${})", 
            token.clientId(), totalValue);
        
        return "All equipment removed. Total loss: $" + totalValue;
    }
    
    @GetMapping("/health")
    public String health() {
        return "Shed service is running";
    }
    
    // Helper methods
    
    private TokenInfo validateTokenWithScope(String authHeader, String requiredScope) {
        String token = extractToken(authHeader);
        TokenInfo tokenInfo = tokenValidator.validate(token);
        
        if (!tokenInfo.hasScope(requiredScope)) {
            log.warn("Client {} attempted to access endpoint requiring scope '{}' but only has: {}", 
                tokenInfo.clientId(), requiredScope, tokenInfo.scopes());
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Token missing required scope: " + requiredScope
            );
        }
        
        return tokenInfo;
    }
    
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Missing or invalid Authorization header"
            );
        }
        return authHeader.substring(7);
    }
}

record PlayRequest(String songId) {}
record EquipmentList(List<Equipment> equipment, int totalValueUSD) {}
