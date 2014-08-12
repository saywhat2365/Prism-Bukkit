package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.botsko.prism.Prism;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.parameters.PrismParameterHandler;
import me.botsko.prism.storage.StorageAdapter;

import org.bukkit.command.CommandSender;

public class QuerySession {
    
    protected final CommandSender sender;
    protected QueryParameters query;
    protected List<CommandSender> shareWithPlayers = new ArrayList<CommandSender>();
    protected Set<MatchedParam> foundArguments = new HashSet<MatchedParam>();
    
    /**
     * 
     * @param sender
     */
    public QuerySession( CommandSender sender ){
        this.sender = sender;
        shareWithPlayers.add( sender );
    }
    
    /**
     * 
     * @param sender
     * @param call
     */
    public void setQuery( QueryParameters query ){
        this.query = query;
    }
    
    /**
     * 
     * @return
     */
    public CommandSender getSender(){
        return sender;
    }
    
    /**
     * 
     * @return
     */
    public QueryParameters getQuery(){
        return query;
    }
    
    /**
     * 
     * @return
     */
    public QueryParameters newQuery(){
        query = new QueryParameters();
        return query;
    }
    
    /**
     * 
     * @param player
     */
    public void addResultRecipient( CommandSender sender ){
        shareWithPlayers.add( sender );
    }
    
    /**
     * 
     * @return
     */
    public List<CommandSender> getResultRecipients(){
        return shareWithPlayers;
    }
    
//    /**
//     * 
//     * @param foundArgumentNames
//     */
//    public void setFoundArgs( Set<MatchedParam> foundArgumentNames ){
//        this.foundArguments = foundArgumentNames;
//    }
    
    /**
     * Arguments matched in a user command are recorded here
     * @param arg
     */
    public void addFoundArgument( MatchedParam arg ){
        foundArguments.add( arg );
    }
    
    
    public String getOriginalCommand(){
        
    }
    
    public List<String> getDefaultsUsed(){
        
    }
    
    /**
     * 
     * @return
     */
    public String getDefaultsUsedMessage(){
      // determine if defaults were used
//      final ArrayList<String> defaultsUsed
      String defaultsReminder = "";
      if( !defaultsUsed.isEmpty() ) {
          defaultsReminder += "Using defaults:";
          for ( final String d : defaultsUsed ) {
              defaultsReminder += " " + d;
          }
      }
    }
    
    
    /**
     * 
     * @return
     */
    public QueryResult execute(){

        // If lookup, determine if we need to group
        boolean shouldGroup = false;
        if( getQuery().getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            shouldGroup = true;
            // What to default to
            if( !Prism.config.getBoolean( "prism.queries.lookup-auto-group" ) ) {
                shouldGroup = false;
            }
            // Any overriding flags passed?
            if( getQuery().hasFlag( Flag.NO_GROUP ) || getQuery().hasFlag( Flag.EXTENDED ) ) {
                shouldGroup = false;
            }
        }
        
        // Let the adapter query for us
        StorageAdapter adapter = Prism.getStorageAdapter();
        final List<Handler> actions = adapter.query( this, shouldGroup );

        // Build result object
        final QueryResult res = new QueryResult( actions, this );
        res.setPerPage( getQuery().getPerPage() );

        // Cache it if we're doing a lookup. Otherwise we don't
        // need a cache.
        if( getQuery().getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            String keyName = "console";
            if( getSender() != null ) {
                keyName = getSender().getName();
            }
            if( Prism.cachedQueries.containsKey( keyName ) ) {
                Prism.cachedQueries.remove( keyName );
            }
            Prism.cachedQueries.put( keyName, res );
            // We also need to share these results with the -share-with players.
            for ( final CommandSender sharedPlayer : getResultRecipients() ) {
                Prism.cachedQueries.put( sharedPlayer.getName(), res );
            }
        }

        Prism.eventTimer.recordTimedEvent( "results object completed" );

        // Return it
        return res;

    }
    
//    /**
//     * 
//     * @return
//     */
//    public static int delete(QueryParameters parameters) {
//        StorageAdapter adapter = Prism.getStorageAdapter();
//        if( adapter == null ) return 0;
//        return adapter.delete(parameters);
//    }
    
    
    /**
     * 
     */
    public static class MatchedParam {
        
        private final PrismParameterHandler handler;
        private final String arg;

        public MatchedParam(PrismParameterHandler handler, String arg) {
            this.handler = handler;
            this.arg = arg;
        }

        public PrismParameterHandler getHandler() {
            return handler;
        }

        public String getArg() {
            return arg;
        }
    }
}