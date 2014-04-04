package me.botsko.prism.actionlibs;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.botsko.prism.Prism;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.storage.StorageAdapter;

public class ActionsQuery {

    /**
	 * 
	 */
    private final Prism plugin;


    /**
     * 
     * @param plugin
     * @return
     */
    public ActionsQuery(Prism plugin) {
        this.plugin = plugin;
    }

    /**
     * 
     * @return
     */
    public QueryResult lookup(QueryParameters parameters) {
        return lookup( parameters, null );
    }

    /**
     * 
     * @return
     */
    public QueryResult lookup(QueryParameters parameters, CommandSender sender) {

        Player player = null;
        if( sender instanceof Player ) {
            player = (Player) sender;
        }

        // If lookup, determine if we need to group
        boolean shouldGroup = false;
        if( parameters.getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            shouldGroup = true;
            // What to default to
            if( !plugin.getConfig().getBoolean( "prism.queries.lookup-auto-group" ) ) {
                shouldGroup = false;
            }
            // Any overriding flags passed?
            if( parameters.hasFlag( Flag.NO_GROUP ) || parameters.hasFlag( Flag.EXTENDED ) ) {
                shouldGroup = false;
            }
        }
        
        // Let the adapter query for us
        StorageAdapter adapter = Prism.getStorageAdapter();
        final List<Handler> actions = adapter.query( parameters, shouldGroup );

        // Build result object
        final QueryResult res = new QueryResult( actions, parameters );
        res.setPerPage( parameters.getPerPage() );

        // Cache it if we're doing a lookup. Otherwise we don't
        // need a cache.
        if( parameters.getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            String keyName = "console";
            if( player != null ) {
                keyName = player.getName();
            }
            if( plugin.cachedQueries.containsKey( keyName ) ) {
                plugin.cachedQueries.remove( keyName );
            }
            plugin.cachedQueries.put( keyName, res );
            // We also need to share these results with the -share-with players.
            for ( final CommandSender sharedPlayer : parameters.getSharedPlayers() ) {
                plugin.cachedQueries.put( sharedPlayer.getName(), res );
            }
        }

        plugin.eventTimer.recordTimedEvent( "results object completed" );

        // Return it
        return res;

    }

    
    /**
     * 
     * @return
     */
    public int delete(QueryParameters parameters) {
    	StorageAdapter adapter = Prism.getStorageAdapter();
    	if( adapter == null ) return 0;
    	return adapter.delete(parameters);
    }
}