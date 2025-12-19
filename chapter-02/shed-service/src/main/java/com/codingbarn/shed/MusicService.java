package com.codingbarn.shed;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple music service with a playlist.
 * In production, this would be a real music service with database, streaming, etc.
 */
@Service
public class MusicService {
    
    private final List<Song> playlist = List.of(
        new Song("1", "Harvest Moon", "Neil Young", "Harvest Moon"),
        new Song("2", "Fields of Gold", "Sting", "Ten Summoner's Tales"),
        new Song("3", "Autumn Leaves", "Bill Evans", "Portrait in Jazz"),
        new Song("4", "September", "Earth, Wind & Fire", "The Best of Earth, Wind & Fire, Vol. 1"),
        new Song("5", "Watermelon Sugar", "Harry Styles", "Fine Line")
    );
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    public Song getCurrentSong() {
        int index = currentIndex.get() % playlist.size();
        return playlist.get(index);
    }
    
    public Song playSong(String songId) {
        for (int i = 0; i < playlist.size(); i++) {
            if (playlist.get(i).id().equals(songId)) {
                currentIndex.set(i);
                return playlist.get(i);
            }
        }
        throw new IllegalArgumentException("Song not found: " + songId);
    }
    
    public List<Song> getPlaylist() {
        return playlist;
    }
    
    public Song nextSong() {
        currentIndex.incrementAndGet();
        return getCurrentSong();
    }
}

record Song(String id, String title, String artist, String album) {}
