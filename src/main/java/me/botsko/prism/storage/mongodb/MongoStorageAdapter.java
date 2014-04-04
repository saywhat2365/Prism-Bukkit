package me.botsko.prism.storage.mongodb;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.storage.StorageAdapter;
import me.botsko.prism.storage.StorageWriteResponse;

public class MongoStorageAdapter implements StorageAdapter {
    
    private MongoClient mongoClient = null;
    private String database;
  

    /**
     * 
     * @param dbName
     * @param collectionName
     * @return
     */
    private DBCollection getMongoCollection( String collectionName ){
        try {
            DB db = getMongoDB();
            return db.getCollection(collectionName);
        } catch( MongoException e ){
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 
     * @param dbName
     * @param collectionName
     * @return
     */
    private DB getMongoDB(){
        DB db = null;
        try {
            db = mongoClient.getDB(database);
            return db;
        } catch( MongoException e ){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 
     */
    @Override
    public boolean connect(FileConfiguration config) {
        
        database = config.getString("prism.mongodb.database");
        
        // Initialize database
        try {
            mongoClient = new MongoClient(config.getString( "prism.mongodb.hostname" ),config.getInt( "prism.mongodb.port" ));
            // boolean auth = db.authenticate(myUserName, myPassword);
            
            // Create indexes
            getMongoCollection("prismData").ensureIndex( new BasicDBObject("epoch",-1).append("world",1).append("x",1).append("z",1) .append("y",1).append("action_id",1) );
            
        } catch ( UnknownHostException e ) {
            e.printStackTrace();
            return false;
        }
        
        if( mongoClient == null ) return false;
        
        return true;

    }

    /**
     * 
     */
    @Override
    public int delete(QueryParameters parameters) {
        int total_rows_affected = 0, cycle_rows_affected;
        try {
            final BasicDBObject query = queryParamsToMongo( parameters );
            WriteResult result = getMongoCollection("prismData").remove( query );
            cycle_rows_affected = result.getN();
            total_rows_affected += cycle_rows_affected;
        } catch( MongoException e ){
            e.printStackTrace();
        }
        return total_rows_affected;
    }
    
    /**
     * 
     */
    @Override
    public List<Handler> query( QueryParameters parameters ){
        return query(parameters,false);
    }

    /**
     * 
     */
    @Override
    public List<Handler> query( QueryParameters parameters, boolean shouldGroup ){

        // Pull results
        final List<Handler> actions = new ArrayList<Handler>();

        // Build conditions based off final args
        final BasicDBObject query = queryParamsToMongo( parameters );

        if( query != null ) {
            try {

                Prism.eventTimer.recordTimedEvent( "query started" );
                
                BasicDBObject matcher = new BasicDBObject("$match",query);
                
//                Prism.debug( query.toString() );
                
                int sortDir = parameters.getSortDirection().equals( "ASC" ) ? 1 : -1;
                BasicDBObject sorter = new BasicDBObject( "$sort", new BasicDBObject("epoch",sortDir).append( "x", 1 ).append( "z", 1 ).append( "y", 1 ) );
                BasicDBObject limit = new BasicDBObject( "$limit", parameters.getLimit() );
//                BasicDBObject group = new BasicDBObject("$group", new BasicDBObject("_id",new BasicDBObject("action","$action").append( "player", "$player" )).append( "count", new BasicDBObject("$sum", 1) ) );
                
//                BasicDBObject group = new BasicDBObject("_id","$action").append( "count", "$sum : 1" );
//                Prism.debug(group.toString());
                
                AggregationOutput aggregated = getMongoCollection("prismData").aggregate( matcher, sorter, limit );
                Prism.debug(aggregated.getCommand().toString());
                
                
                Prism.eventTimer.recordTimedEvent( "query returned, will now build results" );
                
                for (DBObject result : aggregated.results()){

                    if( result.get( "action" ) == null ) continue;

                    // Get the action handler
                    final ActionType actionType = Prism.getActionRegistry().getAction( result.get( "action" ).toString() );

                    if( actionType == null ) continue;

                    try {

                        final Handler baseHandler = Prism.getHandlerRegistry().getHandler( actionType.getHandler() );

                        // Set all shared values
                        baseHandler.setPlugin( Bukkit.getPluginManager().getPlugin( "Prism" ) );
                        baseHandler.setType( actionType );
                        baseHandler.setUnixEpoch( (Long) result.get( "epoch" ) );
                        baseHandler.setPlayerName( (String) result.get( "player" ) );
                        baseHandler.setWorldName( (String) result.get( "world" ) );
                        baseHandler.setX( (Double) result.get( "x" ) );
                        baseHandler.setY( (Double) result.get( "y" ) );
                        baseHandler.setZ( (Double) result.get( "z" ) );
                        baseHandler.setBlockId( (Integer) result.get( "block_id" ) );
                        baseHandler.setBlockSubId( (Integer) result.get( "block_subid" ) );
                        baseHandler.setOldBlockId( (Integer) result.get( "old_block_id" ) );
                        baseHandler.setOldBlockSubId( (Integer) result.get( "old_block_subid" ) );
                        baseHandler.setData( (String) result.get( "data" ) );
                        baseHandler.setMaterialAliases( Prism.getItems() );

                        // Set aggregate counts if a lookup
                        // @todo mongodb
//                        int aggregated = 0;
//                        if( shouldGroup ) {
//                            aggregated = rs.getInt( 14 );
//                        }
//                        baseHandler.setAggregateCount( aggregated );

                        actions.add( baseHandler );

                    } catch ( final Exception e ) {
                        e.printStackTrace();
                    }
                }
            } catch ( final Exception e ) {
                e.printStackTrace();
            }
        }
        return actions;
    }

    /**
     * 
     */
    @Override
    public StorageWriteResponse create(List<Handler> actions) {
        try {

            if( !actions.isEmpty() ) {

                // Begin new batch
                List<DBObject> documents = new ArrayList<DBObject>();

                for( Handler a : actions){

                    if( a == null )
                        break;

                    if( a.isCanceled() )
                        continue;

                    BasicDBObject doc = new BasicDBObject("world", a.getWorldName()).
                            append("action", a.getType().getName()).
                            append("player", a.getPlayerName()).
                            append("block_id",a.getBlockId()).
                            append("block_subid",a.getBlockSubId()).
                            append("old_block_id",a.getOldBlockId()).
                            append("old_block_subid",a.getOldBlockSubId()).
                            append("x",a.getX()).
                            append("y",a.getY()).
                            append("z",a.getZ()).
                            append("epoch",System.currentTimeMillis() / 1000L).
                            append("data",a.getData());
                    
                    documents.add( doc );
    
                }

                DBCollection coll = getMongoCollection("prismData");
                WriteResult res = coll.insert( documents );
                Prism.debug("Recorder logged " + res.getN() + " new actions.");

                // Save the current count to the queue for short historical data
                Prism.queueStats.addRunCount( res.getN() );

            }
        } catch ( final Exception e ) {
            e.printStackTrace();
        }
        
        return new StorageWriteResponse();
        
    }

    
    /**
     * Unused, only needed for foreign-key schemas
     */
    @Override
    public void addActionName(String actionName) {}
    @Override
    public void addWorldName(String worldName) {}
    @Override
    public void cachePlayerPrimaryKey(String playerName){}
    @Override
    public void addPlayerName(String playerName){}

    /**
     * 
     */
    @Override
    public void close() {}
    
    
    /**
     * 
     * @return
     */
    private static BasicDBObject queryParamsToMongo( QueryParameters parameters ){
        
        BasicDBObject query = new BasicDBObject();
        
        // @todo add support for include/excludes
        
        // Time
        if( !parameters.getIgnoreTime() ){
            if( parameters.getBeforeTime() != null && parameters.getBeforeTime() > 0 ){
                query.append( "epoch", new BasicDBObject("$lt", parameters.getBeforeTime()/1000) );
            }
            if( parameters.getSinceTime() != null && parameters.getSinceTime() > 0 ){
                query.append( "epoch", new BasicDBObject("$gte", parameters.getSinceTime()/1000) );
            }
        }
        
        // World
        if( parameters.getWorld() != null && !parameters.getWorld().isEmpty() ){
            query.append( "world", parameters.getWorld() );
        }
        
        // Specific coords
        final ArrayList<Location> locations = parameters.getSpecificBlockLocations();
        if( locations.size() > 0 ){
            BasicDBList or = new BasicDBList();
            for ( final Location loc : locations ){
                or.add( new BasicDBObject("x",loc.getBlockX())
                            .append( "y", loc.getBlockY() )
                            .append( "z", loc.getBlockZ() ) );

            }
            query.append( "$or", or );
        }
        
        // Coordinate bounds
        Vector maxLoc = parameters.getMaxLocation();
        Vector minLoc = parameters.getMinLocation();
        if( minLoc != null && maxLoc != null ) {
            query.append( "x", new BasicDBObject("$gt", minLoc.getBlockX()).append( "$lt", maxLoc.getBlockX() ) );
            query.append( "y", new BasicDBObject("$gt", minLoc.getBlockY()).append( "$lt", maxLoc.getBlockY() ) );
            query.append( "z", new BasicDBObject("$gt", minLoc.getBlockZ()).append( "$lt", maxLoc.getBlockZ() ) );
        }
        
        // Action types
        if( !parameters.getActionTypeNames().isEmpty() ){
            query.append( "action", new BasicDBObject("$in", parameters.getActionTypeNames().keySet()) );
        }
        
        // Players
        if( !parameters.getPlayerNames().isEmpty() ){
            query.append( "player", new BasicDBObject("$in", parameters.getPlayerNames().keySet()) );
        }
        
        // @todo:
//        parameters.getEntities()
//        parameters.getKeyword()
        
        // Blocks
        if( !parameters.getBlockFilters().isEmpty() ){
            BasicDBList or = new BasicDBList();
            for ( final Entry<Integer, Byte> entry : parameters.getBlockFilters().entrySet() ) {
                if( entry.getValue() == 0 ){
                    or.add( new BasicDBObject("block_id",entry.getKey()) );
                } else {
                    or.add( new BasicDBObject("block_id",entry.getKey()).append( "block_subid", entry.getValue() ) );
                }
            }
            query.append( "$or", or );
        }
        
        return query;
        
    }
    
    /**
     * 
     */
    @Override
    public boolean testConnection() throws Exception {
        try {
            if( mongoClient == null ) {
                throw new Exception( "Pool returned NULL instead of a valid connection." );
            }
            mongoClient.getDB("prism");
        } catch ( final MongoException e ) {
            throw new Exception( "[InternalAffairs] Error: " + e.getMessage() );
        }
        return true;
    }
}