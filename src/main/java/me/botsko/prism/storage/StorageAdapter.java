package me.botsko.prism.storage;

import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.Handler;

public interface StorageAdapter {

    /**
     * Establishes an active connection to the storage
     * engine. Any setup/initialization code will run
     * here.
     * 
     * @param config
     * @return
     */
	public boolean connect( FileConfiguration config );
	
	/**
	 * Provided a parameters object, this will build any SQL/Query objects
	 * and will return a list of all found results.
	 * 
	 * @param parameters
	 * @return
	 */
	public List<Handler> query(QueryParameters parameters);
	public List<Handler> query(QueryParameters parameters,boolean shouldGroup);
	
	/**
	 * Given a list of actions, will write them to the database. Prism still
	 * handles the batch insert building as that's db independent.
	 * 
	 * @param actions
	 * @return
	 */
	public StorageWriteResponse create( List<Handler> actions );
	
	/**
	 * Prism purges records in chunks. How we find those chunks is determined
	 * by the storage engine. For MySQL it's by primary key, etc.
	 * 
	 * It must be numerical (something we can increment), largest allowed is a long.
	 * 
	 * @return
	 */
	public long getMinimumChunkingKey();
    public long getMaximumChunkingKey();

    /**
     * Given a list of parameters, will remove all matching records.
     * @param parameters
     * @return
     */
    public int delete(QueryParameters parameters);
	
	/**
	 * Convenience methods for storage engines that need to cache primary keys. Not
	 * all databases need this.
	 * @param actionName
	 */
	public void addActionName(String actionName);
	public void addWorldName(String worldName);
	
	
	/**
	 * Close connections.
	 */
	public void close();
	
	
	/**
	 * Test the connection, returns true if valid and ready, false
	 * if error/null.
	 * @return
	 * @throws Exception
	 */
	public boolean testConnection() throws Exception;
	
}