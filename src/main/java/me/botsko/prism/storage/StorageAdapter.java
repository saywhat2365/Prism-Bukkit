package me.botsko.prism.storage;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.Handler;

public interface StorageAdapter {

	public boolean connect( FileConfiguration config );

	public int delete(QueryParameters parameters);
	
	public List<Handler> query(QueryParameters parameters);
	
	public List<Handler> query(QueryParameters parameters,boolean shouldGroup);
	
	public StorageWriteResponse create( List<Handler> actions );
	
	// Methods needed for foreign-key schemas, some dbs don't use these
	public void addActionName(String actionName);
	public void addWorldName(String worldName);
	public void cachePlayerPrimaryKey(final String playerName);
	public void addPlayerName(String playerName);
	
	public void close();
	
}