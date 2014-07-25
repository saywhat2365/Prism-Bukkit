package me.botsko.prism.storage.mongodb;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import me.botsko.prism.Prism;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.players.PrismPlayer;
import me.botsko.prism.storage.PlayerStorageAdapter;

public class MongoPlayerStorageAdapter implements PlayerStorageAdapter {

    /**
     * 
     * @param player
     * @return
     */
    @Override
    public PrismPlayer addPlayer(Player player){
        PrismPlayer prismPlayer = new PrismPlayer( 0, player.getUniqueId(), player.getName() );
        addPlayer(prismPlayer);
        return prismPlayer;
    }

    /**
     * 
     * @param playerName
     * @return
     */
    @Override
    public PrismPlayer addPlayer(String playerName){
        PrismPlayer fakePlayer = new PrismPlayer( 0, UUID.randomUUID(), playerName );
        addPlayer(fakePlayer);
        return null;
    }
    
    /**
     * 
     * @param player
     */
    protected void addPlayer( PrismPlayer player ){
        MongoStorageAdapter.getCollection("players").insert( new BasicDBObject("uuid",player.getUUID().toString()).append("nickname",player.getName()));
    }

    /**
     * Update nickname for a UUID
     */
    @Override
    public void updatePlayer(PrismPlayer prismPlayer) {
        MongoStorageAdapter.getCollection("players").update( 
           new BasicDBObject("nickname",prismPlayer.getName()),
           new BasicDBObject("uuid",prismPlayer.getUUID().toString())
        );
    }

    /**
     * Lookup player by name
     */
    @Override
    public PrismPlayer lookupByName(String playerName){
        PrismPlayer player = null;
        DBCursor cursor = MongoStorageAdapter.getCollection("players").find( new BasicDBObject("nickname",playerName) ).limit( 1 );
        try {
            while(cursor.hasNext()) {
                DBObject record = cursor.next();
                player = new PrismPlayer( 0, UUID.fromString((String) record.get("uuid")), (String) record.get("nickname") );
            }
        } finally {
            cursor.close();
        }
        return player;
    }

    /**
     * Lookup player by UUID
     */
    @Override
    public PrismPlayer lookupByUUID(UUID uuid){
        PrismPlayer player = null;
        DBCursor cursor = MongoStorageAdapter.getCollection("players").find( new BasicDBObject("uuid",uuid.toString()) ).limit( 1 );
        try {
            while(cursor.hasNext()) {
                DBObject record = cursor.next();
                player = new PrismPlayer( 0, UUID.fromString((String) record.get("uuid")), (String) record.get("nickname") );
            }
        } finally {
            cursor.close();
        }
        return player;
    }

    /**
     * Find all players currently online
     */
    @Override
    public void cacheOnlinePlayerPrimaryKeys(){
        DBCursor cursor = MongoStorageAdapter.getCollection("players").find( new BasicDBObject("nickname",Bukkit.getServer().getOnlinePlayers()) );
        try {
            while(cursor.hasNext()) {
                DBObject record = cursor.next();
                UUID playerUUID = UUID.fromString((String) record.get("uuid"));
                PrismPlayer prismPlayer = new PrismPlayer( 0, playerUUID, (String) record.get("nickname") );
                Prism.debug("Loaded player " + record.get("nickname") + ", id: " + record.get("uuid") + " into the cache.");
                PlayerIdentification.prismPlayers.put( playerUUID, prismPlayer );
            }
        } finally {
            cursor.close();
        }
    }
}