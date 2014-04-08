package me.botsko.prism.storage.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import me.botsko.elixr.TypeUtils;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.RecordingManager;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.storage.StorageAdapter;
import me.botsko.prism.storage.StorageWriteResponse;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class MysqlStorageAdapter implements StorageAdapter {
	
	/**
     * Connection Pool
     */
    private static DataSource pool = new DataSource();
    private SelectQueryBuilder qb;
    /**
     * DB Foreign key caches
     */
    private HashMap<String, Integer> prismWorlds = new HashMap<String, Integer>();
    private HashMap<String, Integer> prismPlayers = new HashMap<String, Integer>();
    private HashMap<String, Integer> prismActions = new HashMap<String, Integer>();
    
	/**
	 * 
	 */
	@Override
	public boolean connect(FileConfiguration config) {
		
        pool = initDbPool( config );
        final Connection test_conn = dbc();
        if( pool == null || test_conn == null ) {
        	return false;
        }
        try {
            test_conn.close();
        } catch ( final SQLException e ) {
        	e.printStackTrace();
        	return false;
        }
        
        // Setup databases
        setupDatabase();

        // Cache world IDs
        cacheWorldPrimaryKeys();
        cacheOnlinePlayerPrimaryKeys();
        
        // ensure current worlds are added
        for ( final World w : Bukkit.getServer().getWorlds() ) {
            if( !prismWorlds.containsKey( w.getName() ) ) {
                addWorldName( w.getName() );
            }
        }
        
        this.qb = new SelectQueryBuilder( prismActions );

        return true;
		
	}
	
    /**
     * 
     * @return
     */
    protected DataSource initDbPool(FileConfiguration config) {

        DataSource pool = null;

        final String dns = "jdbc:mysql://" + config.getString( "prism.mysql.hostname" ) + ":"
                + config.getString( "prism.mysql.port" ) + "/" + config.getString( "prism.mysql.database" );
        pool = new DataSource();
        pool.setDriverClassName( "com.mysql.jdbc.Driver" );
        pool.setUrl( dns );
        pool.setUsername( config.getString( "prism.mysql.username" ) );
        pool.setPassword( config.getString( "prism.mysql.password" ) );
        pool.setInitialSize( config.getInt( "prism.database.pool-initial-size" ) );
        pool.setMaxActive( config.getInt( "prism.database.max-pool-connections" ) );
        pool.setMaxIdle( config.getInt( "prism.database.max-idle-connections" ) );
        pool.setMaxWait( config.getInt( "prism.database.max-wait" ) );
        pool.setRemoveAbandoned( true );
        pool.setRemoveAbandonedTimeout( 60 );
        pool.setTestOnBorrow( true );
        pool.setValidationQuery( "/* ping */SELECT 1" );
        pool.setValidationInterval( 30000 );

        return pool;
    }
    
//  /**
//  * Attempt to rebuild the pool, useful for reloads and failed database
//  * connections being restored
//  */
// public void rebuildPool() {
//     // Close pool connections when plugin disables
//     if( pool != null ) {
//         pool.close();
//     }
//     pool = initDbPool();
// }

    /**
     * 
     */
    protected void setupDatabase() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = dbc();
            if( conn == null )
                return;

            // actions
            String query = "CREATE TABLE IF NOT EXISTS `prism_actions` ("
                    + "`action_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`action` varchar(25) NOT NULL,"
                    + "PRIMARY KEY (`action_id`)," + "UNIQUE KEY `action` (`action`)"
                    + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st = conn.createStatement();
            st.executeUpdate( query );

            // data
            query = "CREATE TABLE IF NOT EXISTS `prism_data` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                    + "`epoch` int(10) unsigned NOT NULL," + "`action_id` int(10) unsigned NOT NULL,"
                    + "`player_id` int(10) unsigned NOT NULL," + "`world_id` int(10) unsigned NOT NULL,"
                    + "`x` int(11) NOT NULL," + "`y` int(11) NOT NULL," + "`z` int(11) NOT NULL,"
                    + "`block_id` mediumint(5) DEFAULT NULL," + "`block_subid` mediumint(5) DEFAULT NULL,"
                    + "`old_block_id` mediumint(5) DEFAULT NULL," + "`old_block_subid` mediumint(5) DEFAULT NULL,"
                    + "PRIMARY KEY (`id`)," + "KEY `epoch` (`epoch`),"
                    + "KEY  `location` (`world_id`, `x`, `z`, `y`, `action_id`)"
                    + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            // extra prism data table (check if it exists first, so we can avoid
            // re-adding foreign key stuff)
            final DatabaseMetaData metadata = conn.getMetaData();
            ResultSet resultSet;
            resultSet = metadata.getTables( null, null, "prism_data_extra", null );
            if( !resultSet.next() ) {

                // extra data
                query = "CREATE TABLE IF NOT EXISTS `prism_data_extra` ("
                        + "`extra_id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                        + "`data_id` int(10) unsigned NOT NULL," + "`data` text NULL," + "`te_data` text NULL,"
                        + "PRIMARY KEY (`extra_id`)," + "KEY `data_id` (`data_id`)"
                        + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
                st.executeUpdate( query );

                // add extra data delete cascade
                query = "ALTER TABLE `prism_data_extra` ADD CONSTRAINT `prism_data_extra_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `prism_data` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;";
                st.executeUpdate( query );
            }

            // meta
            query = "CREATE TABLE IF NOT EXISTS `prism_meta` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                    + "`k` varchar(25) NOT NULL," + "`v` varchar(255) NOT NULL," + "PRIMARY KEY (`id`)"
                    + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            // players
            query = "CREATE TABLE IF NOT EXISTS `prism_players` ("
                    + "`player_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`player` varchar(255) NOT NULL,"
                    + "PRIMARY KEY (`player_id`)," + "UNIQUE KEY `player` (`player`)"
                    + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            // worlds
            query = "CREATE TABLE IF NOT EXISTS `prism_worlds` ("
                    + "`world_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`world` varchar(255) NOT NULL,"
                    + "PRIMARY KEY (`world_id`)," + "UNIQUE KEY `world` (`world`)"
                    + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            // actions
            cacheActionPrimaryKeys(); // Pre-cache, so we know if we need to
                                      // populate db
            final String[] actions = Prism.getActionRegistry().listAll();
            for ( final String a : actions ) {
                addActionName( a );
            }
        } catch ( final SQLException e ) {
            Prism.log( "Database connection error: " + e.getMessage() );
            e.printStackTrace();
        } finally {
            if( st != null )
                try {
                    st.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }
    
    /**
     * 
     * @return
     * @throws SQLException
     */
    protected static Connection dbc() {
        Connection con = null;
        try {
            con = pool.getConnection();
        } catch ( final SQLException e ) {
            Prism.log( "Database connection failed. " + e.getMessage() );
            if( !e.getMessage().contains( "Pool empty" ) ) {
                e.printStackTrace();
            }
        }
        return con;
    }
	
	/**
	 * 
	 * @param parameters
	 * @return
	 */
	@Override
	public List<Handler> query( QueryParameters parameters ) {
		return query(parameters,false);
	}
	
	/**
	 * 
	 */
	@Override
	public List<Handler> query( QueryParameters parameters, boolean shouldGroup ) {
	    
		// Any found actions
        final List<Handler> actions = new ArrayList<Handler>();

        // Build conditions based off final args
        final String query = qb.getQuery( parameters, shouldGroup );

        if( query != null ) {
            Connection conn = null;
            PreparedStatement s = null;
            ResultSet rs = null;
            try {

                Prism.eventTimer.recordTimedEvent( "query started" );

                conn = dbc();

                // Handle dead connections
                if( conn == null || conn.isClosed() ) {
                    if( RecordingManager.failedDbConnectionCount == 0 ) {
                        Prism.log( "Prism database error. Connection should be there but it's not. Leaving actions to log in queue." );
                    }
                    RecordingManager.failedDbConnectionCount++;
                    return null;
                } else {
                    RecordingManager.failedDbConnectionCount = 0;
                }

                s = conn.prepareStatement( query );
                rs = s.executeQuery();

                Prism.eventTimer.recordTimedEvent( "query returned, building results" );

                while ( rs.next() ) {

                    if( rs.getString( "action_id" ) == null ) continue;

                    // Convert action ID to name
                    // Performance-wise this is a lot faster than table joins
                    // and the cache data should always be available
                    String actionName = "";
                    for ( final Entry<String, Integer> entry : prismActions.entrySet() ) {
                        if( entry.getValue() == rs.getInt( "action_id" ) ) {
                            actionName = entry.getKey();
                        }
                    }
                    if( actionName.isEmpty() ) {
                        Prism.log( "Record contains action ID that doesn't exist in cache: " + rs.getInt( "action_id" ) );
                        continue;
                    }

                    // Get the action handler
                    final ActionType actionType = Prism.getActionRegistry().getAction( actionName );

                    if( actionType == null )
                        continue;

                    try {

                        final Handler baseHandler = Prism.getHandlerRegistry().getHandler( actionType.getHandler() );

                        // Convert world ID to name
                        // Performance-wise this is typically a lot faster than
                        // table joins
                        String worldName = "";
                        for ( final Entry<String, Integer> entry : prismWorlds.entrySet() ) {
                            if( entry.getValue() == rs.getInt( "world_id" ) ) {
                                worldName = entry.getKey();
                            }
                        }
                        
                        // Some dependency injection
                        baseHandler.setPlugin( Bukkit.getPluginManager().getPlugin( "Prism" ) );
                        baseHandler.setMaterialAliases( Prism.getItems() );

                        // Set all shared values
                        baseHandler.setType( actionType );
                        baseHandler.setPlayerName( rs.getString( "player" ) );
                        baseHandler.setBlockId( rs.getInt( "block_id" ) );
                        baseHandler.setBlockSubId( rs.getInt( "block_subid" ) );
                        
                        // Group-only fields
                        if( shouldGroup ){
                            baseHandler.setAggregateCount( rs.getInt( "counted" ) );
                        }
                        
                        // Non-grouped fields
                        if( !shouldGroup ){
                            baseHandler.setUnixEpoch( rs.getString( "epoch" ) );
                            baseHandler.setWorldName( worldName );
                            baseHandler.setX( rs.getInt( "x" ) );
                            baseHandler.setY( rs.getInt( "y" ) );
                            baseHandler.setZ( rs.getInt( "z" ) );
                            baseHandler.setOldBlockId( rs.getInt( "old_block_id" ) );
                            baseHandler.setOldBlockSubId( rs.getInt( "old_block_subid" ) );
                            baseHandler.setData( rs.getString( "data" ) );
                        }

                        actions.add( baseHandler );

                    } catch ( final Exception e ) {
                        if( !rs.isClosed() ) {
                            Prism.log( "Ignoring data from record #" + rs.getInt( 1 ) + " because it caused an error:" );
                        }
                        e.printStackTrace();
                    }
                }
            } catch ( final SQLException e ) {
                e.printStackTrace();
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
        }
        return actions;
	}
	
	/**
	 * 
	 */
	@Override
	public StorageWriteResponse create(List<Handler> actions) {
		
        PreparedStatement s = null;
        Connection conn = null;

        int actionsRecorded = 0;
        try {

            if( !actions.isEmpty() ) {

                final ArrayList<Handler> extraDataQueue = new ArrayList<Handler>();
                conn = dbc();

                // Handle dead connections
                if( conn == null || conn.isClosed() ) {
                    if( RecordingManager.failedDbConnectionCount == 0 ) {
                        Prism.log( "Prism database error. Connection should be there but it's not. Leaving actions to log in queue." );
                    }
                    RecordingManager.failedDbConnectionCount++;
                    Prism.debug( "Database connection still missing, incrementing count." );
                    return null;
                } else {
                    RecordingManager.failedDbConnectionCount = 0;
                }

                // Connection valid, proceed
                conn.setAutoCommit( false );
                s = conn.prepareStatement(
                        "INSERT INTO prism_data (epoch,action_id,player_id,world_id,block_id,block_subid,old_block_id,old_block_subid,x,y,z) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS );

                for( Handler a : actions ){

                    if( conn.isClosed() ) {
                        Prism.log( "Prism database error. We have to bail in the middle of building primary bulk insert query." );
                        break;
                    }

                    int world_id = 0;
                    if( prismWorlds.containsKey( a.getWorldName() ) ) {
                        world_id = prismWorlds.get( a.getWorldName() );
                    }

                    int action_id = 0;
                    if( prismActions.containsKey( a.getType().getName() ) ) {
                        action_id = prismActions.get( a.getType().getName() );
                    }

                    final int player_id = getPlayerPrimaryKey( a.getPlayerName() );

                    if( world_id == 0 || action_id == 0 || player_id == 0 ) {
                        // @todo do something, error here
                        Prism.log( "Cache data was empty. Please report to developer: world_id:" + world_id + "/"
                                + a.getWorldName() + " action_id:" + action_id + "/" + a.getType().getName()
                                + " player_id:" + player_id + "/" + a.getPlayerName() );
                        Prism.log( "HOWEVER, this likely means you have a broken prism database installation." );
                        continue;
                    }

                    if( a.isCanceled() )
                        continue;

                    actionsRecorded++;

                    s.setLong( 1, System.currentTimeMillis() / 1000L );
                    s.setInt( 2, action_id );
                    s.setInt( 3, player_id );
                    s.setInt( 4, world_id );
                    s.setInt( 5, a.getBlockId() );
                    s.setInt( 6, a.getBlockSubId() );
                    s.setInt( 7, a.getOldBlockId() );
                    s.setInt( 8, a.getOldBlockSubId() );
                    s.setInt( 9, (int) a.getX() );
                    s.setInt( 10, (int) a.getY() );
                    s.setInt( 11, (int) a.getZ() );
                    s.addBatch();

                    extraDataQueue.add( a );

                }

                s.executeBatch();

                if( conn.isClosed() ) {
                    Prism.log( "Prism database error. We have to bail in the middle of building primary bulk insert query." );
                } else {
                    conn.commit();
                    Prism.debug( "Batch insert was commit: " + System.currentTimeMillis() );
                }

                // Save the current count to the queue for short historical data
                Prism.queueStats.addRunCount( actionsRecorded );

                // Insert extra data
                insertExtraData( extraDataQueue, s.getGeneratedKeys() );

            }
        } catch ( final SQLException e ) {
            e.printStackTrace();
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
        return null;
	}
	
	
	/**
     * 
     * @param keys
     * @throws SQLException
     */
    private void insertExtraData(ArrayList<Handler> extraDataQueue, ResultSet keys) throws SQLException {

        if( extraDataQueue.isEmpty() )
            return;

        PreparedStatement s = null;
        final Connection conn = dbc();

        if( conn == null || conn.isClosed() ) {
            Prism.log( "Prism database error. Skipping extra data queue insertion." );
            return;
        }

        try {
            conn.setAutoCommit( false );
            s = conn.prepareStatement( "INSERT INTO prism_data_extra (data_id,data) VALUES (?,?)" );
            int i = 0;
            while ( keys.next() ) {

                if( conn.isClosed() ) {
                    Prism.log( "Prism database error. We have to bail in the middle of building bulk insert extra data query." );
                    break;
                }

                // @todo should not happen
                if( i >= extraDataQueue.size() ) {
                    Prism.log( "Skipping extra data for prism_data.id " + keys.getInt( 1 )
                            + " because the queue doesn't have data for it." );
                    continue;
                }

                final Handler a = extraDataQueue.get( i );

                if( a.getData() != null && !a.getData().isEmpty() ) {
                    s.setInt( 1, keys.getInt( 1 ) );
                    s.setString( 2, a.getData() );
                    s.addBatch();
                }

                i++;

            }
            s.executeBatch();

            if( conn.isClosed() ) {
                Prism.log( "Prism database error. We have to bail in the middle of building extra data bulk insert query." );
            } else {
                conn.commit();
            }

        } catch ( final SQLException e ) {
            e.printStackTrace();
        } finally {
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException ignored ) {}
            try {
                conn.close();
            } catch ( final SQLException ignored ) {}
        }
    }
	
	
	/**
     * 
     * @param playerName
     * @return
     */
    private int getPlayerPrimaryKey(String playerName) {
        try {
            if( prismPlayers.containsKey( playerName ) ) {
                return prismPlayers.get( playerName );
            } else {
                cachePlayerPrimaryKey( playerName );
                return prismPlayers.get( playerName );
            }
        } catch ( final Exception e ) {
            e.printStackTrace();
            return 0;
        }
    }
	
	/**
	 * 
	 */
	@Override
	public void close(){
        if( pool != null ) {
            pool.close();
        }
	}
	
	/**
	 * 
	 */
    private void cacheActionPrimaryKeys() {

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "SELECT action_id, action FROM prism_actions" );
            rs = s.executeQuery();

            while ( rs.next() ) {
                Prism.debug( "Loaded " + rs.getString( 2 ) + ", id:" + rs.getInt( 1 ) );
                prismActions.put( rs.getString( 2 ), rs.getInt( 1 ) );
            }

            Prism.debug( "Loaded " + prismActions.size() + " actions into the cache." );

        } catch ( final SQLException e ) {
            e.printStackTrace();
        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }

    /**
     * Saves an action name to the database, and adds the id to the cache
     * hashmap
     */
    public void addActionName(String actionName) {

        if( prismActions.containsKey( actionName ) )
            return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "INSERT INTO prism_actions (action) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, actionName );
            s.executeUpdate();

            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                Prism.log( "Registering new action type to the database/cache: " + actionName + " " + rs.getInt( 1 ) );
                prismActions.put( actionName, rs.getInt( 1 ) );
            } else {
                throw new SQLException( "Insert statement failed - no generated key obtained." );
            }
        } catch ( final SQLException e ) {

        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }

    /**
	 * 
	 */
    private void cacheWorldPrimaryKeys() {

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "SELECT world_id, world FROM prism_worlds" );
            rs = s.executeQuery();

            while ( rs.next() ) {
                prismWorlds.put( rs.getString( 2 ), rs.getInt( 1 ) );
            }
            Prism.debug( "Loaded " + prismWorlds.size() + " worlds into the cache." );
        } catch ( final SQLException e ) {
        	e.printStackTrace();
        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }

    /**
     * Saves a world name to the database, and adds the id to the cache hashmap
     */
    public void addWorldName(String worldName) {

        if( prismWorlds.containsKey( worldName ) )
            return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "INSERT INTO prism_worlds (world) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, worldName );
            s.executeUpdate();

            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                Prism.log( "Registering new world to the database/cache: " + worldName + " " + rs.getInt( 1 ) );
                prismWorlds.put( worldName, rs.getInt( 1 ) );
            } else {
                throw new SQLException( "Insert statement failed - no generated key obtained." );
            }
        } catch ( final SQLException e ) {

        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }

    /**
	 * 
	 */
    public void cacheOnlinePlayerPrimaryKeys() {
    	
        Bukkit.getServer().getScheduler().runTaskAsynchronously( Bukkit.getPluginManager().getPlugin("Prism"), new Runnable() {
            @Override
            public void run() {

                String[] playerNames;
                playerNames = new String[Bukkit.getServer().getOnlinePlayers().length];
                int i = 0;
                for ( final Player pl : Bukkit.getServer().getOnlinePlayers() ) {
                    playerNames[i] = pl.getName();
                    i++;
                }

                Connection conn = null;
                PreparedStatement s = null;
                ResultSet rs = null;
                try {

                    conn = dbc();
                    s = conn.prepareStatement( "SELECT player_id, player FROM prism_players WHERE player IN ('"
                            + TypeUtils.join( playerNames, "','" ) + "')" );
                    rs = s.executeQuery();

                    while ( rs.next() ) {
                        Prism.debug( "Loaded player " + rs.getString( 2 ) + ", id: " + rs.getInt( 1 ) + " into the cache." );
                        prismPlayers.put( rs.getString( 2 ), rs.getInt( 1 ) );
                    }
                } catch ( final SQLException e ) {
                	e.printStackTrace();
                } finally {
                    if( rs != null )
                        try {
                            rs.close();
                        } catch ( final SQLException e ) {}
                    if( s != null )
                        try {
                            s.close();
                        } catch ( final SQLException e ) {}
                    if( conn != null )
                        try {
                            conn.close();
                        } catch ( final SQLException e ) {}
                }
            }
        } );
    }

    /**
	 * 
	 */
    public void cachePlayerPrimaryKey(final String playerName) {

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "SELECT player_id FROM prism_players WHERE player = ?" );
            s.setString( 1, playerName );
            rs = s.executeQuery();

            if( rs.next() ) {
                Prism.debug( "Loaded player " + playerName + ", id: " + rs.getInt( 1 ) + " into the cache." );
                prismPlayers.put( playerName, rs.getInt( 1 ) );
            } else {
                addPlayerName( playerName );
            }
        } catch ( final SQLException e ) {
            e.printStackTrace();
        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }

    /**
     * Saves a player name to the database, and adds the id to the cache hashmap
     */
    public void addPlayerName(String playerName) {

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "INSERT INTO prism_players (player) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, playerName );
            s.executeUpdate();

            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                Prism.debug( "Saved and loaded player " + playerName + " into the cache." );
                prismPlayers.put( playerName, rs.getInt( 1 ) );
            } else {
                throw new SQLException( "Insert statement failed - no generated key obtained." );
            }
        } catch ( final SQLException e ) {
            e.printStackTrace();
        } finally {
            if( rs != null )
                try {
                    rs.close();
                } catch ( final SQLException e ) {}
            if( s != null )
                try {
                    s.close();
                } catch ( final SQLException e ) {}
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
    }
    
    
    /**
     * 
     * @param playername
     */
    public long getMinimumChunkingKey() {
        int id = 0;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "SELECT MIN(id) FROM prism_data" );
            s.executeQuery();
            rs = s.getResultSet();

            if( rs.first() ) {
                id = rs.getInt( 1 );
            }

        } catch ( final SQLException ignored ) {

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
        return id;
    }

    /**
     * 
     * @param playername
     */
    public long getMaximumChunkingKey() {
        int id = 0;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {

            conn = dbc();
            s = conn.prepareStatement( "SELECT id FROM prism_data ORDER BY id DESC LIMIT 1;" );
            s.executeQuery();
            rs = s.getResultSet();

            if( rs.first() ) {
                id = rs.getInt( 1 );
            }

        } catch ( final SQLException ignored ) {

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
        return id;
    }
    
    /**
     * 
     */
    @Override
    public int delete(QueryParameters parameters) {
        int total_rows_affected = 0, cycle_rows_affected;
        Connection conn = null;
        Statement s = null;
        try {
            final DeleteQueryBuilder dqb = new DeleteQueryBuilder( prismActions );
            // Build conditions based off final args
            final String query = dqb.getQuery( parameters, false );
            conn = dbc();
            if( conn != null && !conn.isClosed() ) {
                s = conn.createStatement();
                cycle_rows_affected = s.executeUpdate( query );
                total_rows_affected += cycle_rows_affected;
            } else {
                Prism.log( "Prism database error. Purge cannot continue." );
            }
        } catch ( final SQLException e ) {
            e.printStackTrace();
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
        return total_rows_affected;
    }
    
    /**
     * @throws Exception  
     * 
     */
    @Override
    public boolean testConnection() throws Exception {
        Connection conn = null;
        try {

            conn = dbc();
            if( conn == null ) {
                throw new Exception( "Pool returned NULL instead of a valid connection." );
            } else if( conn.isClosed() ) {
                throw new Exception( "Pool returned an already closed connection." );
            } else if( conn.isValid( 5 ) ) {
                return true;
            }
        } catch ( final SQLException e ) {
            throw new Exception( "[InternalAffairs] Error: " + e.getMessage() );
        } finally {
            if( conn != null )
                try {
                    conn.close();
                } catch ( final SQLException e ) {}
        }
        return false;
    }
}