package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.parameters.PrismParameterHandler;

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
    public QuerySession( CommandSender sender, CallInfo call ){
        this(sender);
        PreprocessArgs.extractQueryFromCommand( this, call );
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