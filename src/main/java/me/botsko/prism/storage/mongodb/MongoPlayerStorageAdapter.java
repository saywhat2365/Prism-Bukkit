package me.botsko.prism.storage.mongodb;

import java.util.UUID;

import org.bukkit.entity.Player;

import me.botsko.prism.players.PrismPlayer;
import me.botsko.prism.storage.PlayerStorageAdapter;

public class MongoPlayerStorageAdapter implements PlayerStorageAdapter {

    @Override
    public PrismPlayer addPlayer(Player player) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrismPlayer addPlayer(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updatePlayer(PrismPlayer prismPlayer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public PrismPlayer lookupByName(String playerName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrismPlayer lookupByUUID(UUID uuid) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void cacheOnlinePlayerPrimaryKeys() {
        // TODO Auto-generated method stub
        
    }
}