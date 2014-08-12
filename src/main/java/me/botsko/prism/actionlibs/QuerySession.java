package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;

public class QuerySession {
    
    protected final CommandSender sender;
    protected QueryParameters query;
    protected List<CommandSender> shareWithPlayers = new ArrayList<CommandSender>();
    protected Set<String> foundArgumentNames = new HashSet<String>();
    
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
    
    /**
     * 
     * @param foundArgumentNames
     */
    public void setFoundArgs( Set<String> foundArgumentNames ){
        this.foundArgumentNames = foundArgumentNames;
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
}