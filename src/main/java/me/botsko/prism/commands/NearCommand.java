package me.botsko.prism.commands;

import me.botsko.elixr.TypeUtils;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.SubHandler;

import java.util.List;

public class NearCommand implements SubHandler {

    /**
     * Handle the command
     */
    @Override
    public void handle(final CallInfo call){
        
        final QuerySession session = new QuerySession( call.getSender() );

        // Build params
        final QueryParameters query = new QueryParameters();
        query.setPerPage( Prism.config.getInt( "prism.queries.default-results-per-page" ) );
        query.setWorld( call.getPlayer().getWorld() );

        // allow a custom near radius
        int radius = Prism.config.getInt( "prism.near.default-radius" );
        if( call.getArgs().length == 2 ) {
            if( TypeUtils.isNumeric( call.getArg( 1 ) ) ) {
                final int _tmp_radius = Integer.parseInt( call.getArg( 1 ) );
                if( _tmp_radius > 0 ) {
                    radius = _tmp_radius;
                } else {
                    call.getPlayer().sendMessage(Prism.messenger.playerError( "Radius must be greater than zero. Or leave it off to use the default. Use /prism ? for help." ) );
                    return;
                }
            } else {
                call.getPlayer().sendMessage(Prism.messenger.playerError( "Radius must be a number. Or leave it off to use the default. Use /prism ? for help." ) );
                return;
            }
        }
        query.setRadius( call.getPlayer().getLocation(), radius );
        query.setLimit( Prism.config.getInt( "prism.near.max-results" ) );
        
        session.setQuery( query );

        /**
         * Run the lookup itself in an async task so the lookup query isn't done
         * on the main thread
         */
        new Thread(new Runnable(){
            @Override
            public void run(){

                // Query
                final QueryResult results = session.execute();
                if( !results.getActionResults().isEmpty() ){
                    call.getPlayer().sendMessage( Prism.messenger.playerError( "Couldn't find anything." ) );
                    return;
                }
 
                final List<Handler> paginated = results.getPaginatedActionResults();
                if( paginated == null ){
                    call.getPlayer().sendMessage(Prism.messenger.playerError( "Pagination can't find anything. Do you have the right page number?" ) );
                    return;
                }
                
                // Message headers
                call.getPlayer().sendMessage( Prism.messenger.playerSubduedHeaderMsg( "All changes within " + session.getQuery().getSelectedRegion().getAverageRadius() + " blocks of you..." ) );
                call.getPlayer().sendMessage( Prism.messenger.playerHeaderMsg( "Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages() ) );
                
                int result_count = results.getIndexOfFirstResult();
                for ( final Handler a : paginated ) {
                    final ActionMessage am = new ActionMessage( a );
                    if( session.getQuery().isGlobal() || session.getQuery().hasFlag( Flag.EXTENDED ) || Prism.config.getBoolean( "prism.messenger.always-show-extended" ) ){
                        am.showExtended();
                    }
                    am.setResultIndex( result_count );
                    call.getPlayer().sendMessage( Prism.messenger.playerMsg( am.getMessage() ) );
                    result_count++;
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
    public List<String> handleComplete(CallInfo call){
        return null;
    }
}