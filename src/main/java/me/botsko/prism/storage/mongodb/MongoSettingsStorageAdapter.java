package me.botsko.prism.storage.mongodb;

import org.bukkit.entity.Player;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;

import me.botsko.prism.storage.SettingsStorageAdapter;

public class MongoSettingsStorageAdapter implements SettingsStorageAdapter {

    /**
     * Namespaces a key by the player, if provided
     * @param key
     * @param player
     * @return
     */
    private String getNamespacedKey(String key,Player player) {
        if( player != null ) {
            key = player.getName() + "." + key;
        }
        return key;
    }

    /**
     * 
     * @param key
     */
    public void deleteSetting(String key) {
        deleteSetting( key, null );
    }

    /**
     * 
     * @param key
     */
    public void deleteSetting(String key, Player player){
        MongoStorageAdapter.getCollection("meta").remove( new BasicDBObject("k",getNamespacedKey( key, player )) );
    }

    /**
     * 
     * @param key
     * @param value
     * @return
     */
    public void saveSetting(String key, String value) {
        saveSetting( key, value, null );
    }

    /**
     * 
     * @param key
     * @param value
     * @return
     */
    public void saveSetting(String key, String value, Player player) {
        deleteSetting(key,player);
        MongoStorageAdapter.getCollection("meta").insert( new BasicDBObject("k",getNamespacedKey( key, player )).append("v",value));
    }

    /**
     * 
     * @param key
     * @return
     */
    public String getSetting(String key) {
        return getSetting( key, null );
    }

    /**
     * 
     * @param key
     * @return
     */
    public String getSetting(String key, Player player) {
        String v = null;
        DBCursor cursor = MongoStorageAdapter.getCollection("meta").find( new BasicDBObject("k",getNamespacedKey( key, player )) ).limit( 1 );
        try {
            while(cursor.hasNext()) {
               v = (String) cursor.next().get("v");
            }
        } finally {
            cursor.close();
        }
        return v;
    }
}