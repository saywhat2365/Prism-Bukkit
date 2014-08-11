package me.botsko.prism.storage.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.entity.Player;

import me.botsko.prism.Prism;
import me.botsko.prism.storage.SettingsStorageAdapter;

public class MysqlSettingsStorageAdapter implements SettingsStorageAdapter {
    
    protected final String prefix;
    
    /**
     * 
     */
    public MysqlSettingsStorageAdapter(){
        prefix = Prism.config.getString("prism.mysql.prefix");
    }

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
        Connection conn = null;
        PreparedStatement s = null;
        try {

            conn = MysqlStorageAdapter.dbc();
            s = conn.prepareStatement( "DELETE FROM " + prefix + "meta WHERE k = ?" );
            s.setString( 1, getNamespacedKey( key, player ) );
            s.executeUpdate();

        } catch ( final SQLException e ) {
            // plugin.logDbError( e );
        } finally {
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException ignored ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException ignored ) {}
        }
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
        String prefix = Prism.config.getString("prism.mysql.prefix");
        Connection conn = null;
        PreparedStatement s = null;
        try {
            
            String finalKey = getNamespacedKey( key, player );

            conn = MysqlStorageAdapter.dbc();
            s = conn.prepareStatement( "DELETE FROM " + prefix + "meta WHERE k = ?" );
            s.setString( 1, finalKey );
            s.executeUpdate();

            s = conn.prepareStatement( "INSERT INTO " + prefix + "meta (k,v) VALUES (?,?)" );
            s.setString( 1, finalKey );
            s.setString( 2, value );
            s.executeUpdate();

        } catch ( final SQLException e ) {
            // plugin.logDbError( e );
        } finally {
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException ignored ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException ignored ) {}
        }
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
        String value = null;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = MysqlStorageAdapter.dbc();
            s = conn.prepareStatement( "SELECT v FROM " + prefix + "meta WHERE k = ? LIMIT 0,1" );
            s.setString( 1, getNamespacedKey( key, player ) );
            rs = s.executeQuery();

            while ( rs.next() ) {
                value = rs.getString( "v" );
            }

        } catch ( final SQLException e ) {
            // plugin.logDbError( e );
        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException ignored ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException ignored ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException ignored ) {}
        }
        return value;
    }
}