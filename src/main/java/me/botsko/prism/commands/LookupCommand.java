package me.botsko.prism.commands;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.utils.MiscUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class LookupCommand implements SubHandler {

    /**
	 * 
	 */
    private final Prism plugin;

    /**
     * 
     * @param plugin
     * @return
     */
    public LookupCommand(Prism plugin){
        this.plugin = plugin;
    }

    /**
     * Handle the command
     */
    @Override
    public void handle(final CallInfo call){
        
        // Create a new command/query session
        final QuerySession session = new QuerySession( call.getSender(), call );

        /**
         * Run the lookup itself in an async task so the lookup query isn't done
         * on the main thread
         */
        plugin.getServer().getScheduler().runTaskAsynchronously( plugin, new Runnable() {
            @Override
            public void run(){

                final ActionsQuery aq = new ActionsQuery();
                final QueryResult results = aq.lookup( session );
                
                // No results
                if( results.getActionResults().isEmpty() ){
                    session.getSender().sendMessage( Prism.messenger.playerError( "Nothing found." + ChatColor.GRAY + " Either you're missing something, or we are." ) );
                } else {
                    
                    // PasteBin
                    if( session.getQuery().hasFlag( Flag.PASTE ) ){
                        String paste = "";
                        for ( final Handler a : results.getActionResults() ){
                            paste += new ActionMessage( a ).getRawMessage() + "\r\n";
                        }
                        session.getSender().sendMessage( MiscUtils.paste_results( plugin, paste ) );
                    }
                
                    // Results, share with all recipients
                    for( final CommandSender sender : session.getResultRecipients() ){
    
                        final boolean isSender = sender.getName().equals( call.getSender().getName() );
    
                        if( !isSender ){
                            sender.sendMessage( Prism.messenger.playerHeaderMsg( ChatColor.YELLOW + "" + ChatColor.ITALIC + call.getSender().getName() + ChatColor.GOLD + " shared these Prism lookup logs with you:" ) );
                        }
    
                        // Results headers
                        sender.sendMessage( Prism.messenger.playerHeaderMsg( "Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages() ) );
                        if( !session.getDefaultsUsed().isEmpty() ) {
                            sender.sendMessage( Prism.messenger.playerSubduedHeaderMsg( session.getDefaultsUsedMessage() ) );
                        }
                        
                        final List<Handler> paginated = results.getPaginatedActionResults();
                        if( paginated == null ){
                            sender.sendMessage( Prism.messenger.playerError( "Pagination can't find anything. Do you have the right page number?" ) );
                        } else {
                        
                            int result_count = results.getIndexOfFirstResult();
                            for ( final Handler a : paginated ){
                                // Format and display messages
                                final ActionMessage am = new ActionMessage( a );
                                if( session.getQuery().isGlobal() || session.getQuery().hasFlag( Flag.EXTENDED ) || Prism.config.getBoolean( "prism.messenger.always-show-extended" ) ) {
                                    am.showExtended();
                                }
                                am.setResultIndex( result_count );
                                sender.sendMessage( Prism.messenger.playerMsg( am.getMessage() ) );
                                result_count++;
                            }
                        } 
                    }
                }

                // Flush timed data
                Prism.eventTimer.printTimeRecord();

            }
        } );
    }

    /**
     * 
     */
    @Override
    public List<String> handleComplete(CallInfo call) {
        return PreprocessArgs.complete( call.getSender(), call.getArgs() );
    }
}