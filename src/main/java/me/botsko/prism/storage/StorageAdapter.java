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
	
	public void close();
	
}