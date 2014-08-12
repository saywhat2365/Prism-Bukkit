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
     * @return
     */
    public QueryResult lookup( QuerySession session ){

        // If lookup, determine if we need to group
        boolean shouldGroup = false;
        if( session.getQuery().getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            shouldGroup = true;
            // What to default to
            if( !Prism.config.getBoolean( "prism.queries.lookup-auto-group" ) ) {
                shouldGroup = false;
            }
            // Any overriding flags passed?
            if( session.getQuery().hasFlag( Flag.NO_GROUP ) || session.getQuery().hasFlag( Flag.EXTENDED ) ) {
                shouldGroup = false;
            }
        }
        
        // Let the adapter query for us
        StorageAdapter adapter = Prism.getStorageAdapter();
        final List<Handler> actions = adapter.query( session, shouldGroup );

        // Build result object
        final QueryResult res = new QueryResult( actions, session );
        res.setPerPage( session.getQuery().getPerPage() );

        // Cache it if we're doing a lookup. Otherwise we don't
        // need a cache.
        if( session.getQuery().getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            String keyName = "console";
            if( session.getSender() != null ) {
                keyName = session.getSender().getName();
            }
            if( Prism.cachedQueries.containsKey( keyName ) ) {
                Prism.cachedQueries.remove( keyName );
            }
            Prism.cachedQueries.put( keyName, res );
            // We also need to share these results with the -share-with players.
            for ( final CommandSender sharedPlayer : session.getResultRecipients() ) {
                Prism.cachedQueries.put( sharedPlayer.getName(), res );
            }
        }

        Prism.eventTimer.recordTimedEvent( "results object completed" );

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