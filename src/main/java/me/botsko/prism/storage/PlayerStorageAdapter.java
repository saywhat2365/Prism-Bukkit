package me.botsko.prism.storage;

import java.util.UUID;

import me.botsko.prism.players.PrismPlayer;

import org.bukkit.entity.Player;

public interface PlayerStorageAdapter {
    
    /**
     * Adds a new player to the storage cache
     * @param player
     * @return
     */
    public PrismPlayer addPlayer( Player player );
    
    /**
     * Add a player by name only, usually for fake players
     * @param playerName
     * @return
     */
    public PrismPlayer addPlayer( String playerName );
    
    /**
     * Update a player record
     * @param prismPlayer
     */
    public void updatePlayer( PrismPlayer prismPlayer );
    
    /**
     * Find a player by their current name
     * @param playerName
     * @return
     */
    public PrismPlayer lookupByName( String playerName );
    
    /**
     * Find a player by their UUID
     * @param uuid
     * @return
     */
    public PrismPlayer lookupByUUID( UUID uuid );
    
    /**
     * Refresh data from cache for all players currently online
     */
    public void cacheOnlinePlayerPrimaryKeys();
    
}