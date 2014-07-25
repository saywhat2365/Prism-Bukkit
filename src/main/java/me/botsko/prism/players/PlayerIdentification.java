package me.botsko.prism.players;

import java.util.HashMap;
import java.util.UUID;

import me.botsko.prism.Prism;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerIdentification {
    
    public static HashMap<UUID, PrismPlayer> prismPlayers = new HashMap<UUID, PrismPlayer>();


    /**
     * Loads `prism_players` ID for a real player into our cache.
     *
     * Runs during PlayerJoin events, so it will never be for a fake/null
     * player.
     *
     * @param player
     */
    public static PrismPlayer cachePrismPlayer( final Player player ){

        // Lookup the player
        PrismPlayer prismPlayer = getPrismPlayer( player );
        if( prismPlayer != null ){
            prismPlayer = comparePlayerToCache( player, prismPlayer );
            Prism.debug("Loaded player " + player.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
            prismPlayers.put( player.getUniqueId(), prismPlayer );
            return prismPlayer;
        }

        // Player is new, create a record for them
        prismPlayer = Prism.getPlayerStorageAdapter().addPlayer( player );

        return prismPlayer;

    }

    /**
     * Loads `prism_players` ID for a real player into our cache.
     *
     * Runs during PlayerJoin events, so it will never be for a fake/null
     * player.
     *
     * @param player
     */
    public static PrismPlayer cachePrismPlayer( final String playerName ){

        // Lookup the player
        PrismPlayer prismPlayer = getPrismPlayer( playerName );
        if( prismPlayer != null ){
//            prismPlayer = comparePlayerToCache( player, prismPlayer );
            Prism.debug("Loaded player " + prismPlayer.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
//            prismPlayers.put( player.getUniqueId(), prismPlayer );
            return prismPlayer;
        }

        // Player is new, create a record for them
        prismPlayer = Prism.getPlayerStorageAdapter().addPlayer( playerName );

        return prismPlayer;

    }

    /**
     * Returns a `prism_players` ID for the described player name. If
     * one cannot be found, returns 0.
     *
     * Used by the recorder in determining proper foreign key
     *
     * @param playerName
     * @return
     */
    private static PrismPlayer getPrismPlayer( String playerName ){

        Player player = Bukkit.getPlayer(playerName);

        if( player != null ) return getPrismPlayer( player );

        // Player not online, we need to go to cache
        PrismPlayer prismPlayer = Prism.getPlayerStorageAdapter().lookupByName( playerName );

        // Player found! Return the id
        if( prismPlayer != null ) return prismPlayer;

        // No player exists! We must create one
        return null;

    }

    /**
     * Returns a `prism_players` ID for the described player object. If
     * one cannot be found, returns 0.
     *
     * Used by the recorder in determining proper foreign key
     *
     * @param playerName
     * @return
     */
    private static PrismPlayer getPrismPlayer( Player player ){

        if( player.getUniqueId() == null ){
            // If they have a name, we can attempt to find them that way
            if( player.getName() != null && !player.getName().trim().isEmpty() ){
                return getPrismPlayer( player.getName() );
            }
            // No name, no UUID, no service.
            return null;
        }

        PrismPlayer prismPlayer = null;

        // Are they in the cache?
        prismPlayer = prismPlayers.get( player.getUniqueId() );
        if( prismPlayer != null ) return prismPlayer;

        // Lookup by UUID
        prismPlayer = Prism.getPlayerStorageAdapter().lookupByUUID( player.getUniqueId() );
        if( prismPlayer != null ) return prismPlayer;

        // Still not found, try looking them up by name
        prismPlayer = Prism.getPlayerStorageAdapter().lookupByName( player.getName() );
        if( prismPlayer != null ) return prismPlayer;

        return null;

    }

    /**
     * Compares the known player to the cached data. If there's a difference
     * we need to handle it.
     *
     * If usernames are different: Update `prism_players` with new name
     * (@todo track historical?)
     *
     * If UUID is different, log an error.
     *
     * @param player
     * @param prismPlayer
     * @return
     */
    private static PrismPlayer comparePlayerToCache( Player player, PrismPlayer prismPlayer ){

        // Compare for username differences, update database
        if( !player.getName().equals( prismPlayer.getName() ) ){
            prismPlayer.setName( player.getName() );
            Prism.getPlayerStorageAdapter().updatePlayer(prismPlayer);
        }

        // Compare UUID
        if( !player.getUniqueId().equals( prismPlayer.getUUID() ) ){
            Prism.log("Player UUID for " +player.getName() + " does not match our cache! " +player.getUniqueId()+ " versus cache of " + prismPlayer.getUUID());

            // Update anyway...
            prismPlayer.setUUID( player.getUniqueId() );
            Prism.getPlayerStorageAdapter().updatePlayer(prismPlayer);

        }

        return prismPlayer;

    }
}