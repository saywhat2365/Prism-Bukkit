package me.botsko.prism.storage;

import org.bukkit.entity.Player;

public interface SettingsStorageAdapter {
	
    /**
     * Remove a meta setting by its key
     * @param key
     */
	public void deleteSetting(String key);
	
	/**
	 * Retrieve a meta setting by its key
	 * @param key
	 * @return
	 */
	public String getSetting(String key);
	
	/**
	 * Save a new key/value
	 * @param key
	 * @param value
	 */
	public void saveSetting(String key, String value);
	
	
	/**
     * Remove a player's meta setting by its key
     * @param key
     */
	public void deleteSetting(String key, Player player);
	
	
	/**
     * Retrieve a player's meta setting by its key
     * @param key
     * @return
     */
	public void saveSetting(String key, String value, Player player);
	
	
	/**
     * Save a new key/value for a player
     * @param key
     * @param value
     */
	public String getSetting(String key, Player player);

}