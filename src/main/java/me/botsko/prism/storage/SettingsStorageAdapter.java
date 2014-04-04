package me.botsko.prism.storage;

import org.bukkit.entity.Player;

public interface SettingsStorageAdapter {
	
	public String getPlayerKey(Player player, String key);
	
	public void deleteSetting(String key);
	
	public void deleteSetting(String key, Player player);
	
	public void saveSetting(String key, String value);
	
	public void saveSetting(String key, String value, Player player);
	
	public String getSetting(String key);
	
	public String getSetting(String key, Player player);

}