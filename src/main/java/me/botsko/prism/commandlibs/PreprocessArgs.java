package me.botsko.prism.commandlibs;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actionlibs.QueryParameters.MatchRule;
import me.botsko.prism.actionlibs.QuerySession.MatchedParam;
import me.botsko.prism.parameters.PrismParameterHandler;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Map.Entry;

public class PreprocessArgs {

    /**
     *
     */
    public static void extractQueryFromCommand( QuerySession session, CallInfo call ) throws IllegalArgumentException {

        // Start query
        final QueryParameters parameters = session.newQuery();

        // Define pagination/process type
        parameters.setLimit( Prism.config.getInt( "prism.queries.lookup-max-results" ) );
        parameters.setPerPage( Prism.config.getInt( "prism.queries.default-results-per-page" ) );

        // Store names of matched params/handlers
        final Set<String> foundArgsNames = new HashSet<String>();
        final List<MatchedParam> foundArgsList = new ArrayList<MatchedParam>();

        // Iterate arguments
        for ( int i = 1; i < call.getArgs().length; i++ ) {
            final String arg = call.getArg(i);
            if( arg.isEmpty() ) continue;
            parseParam( session, arg );
        }

        // Reject no matches
        if( foundArgsList.isEmpty() ) {
            throw new IllegalArgumentException("No valid arguments were found.");
        }

        /**
         * Call default method for handlers *not* used
         */
        for ( final Entry<String, PrismParameterHandler> entry : Prism.getParameters().entrySet() ) {
            if( !foundArgsNames.contains( entry.getKey().toLowerCase() ) ) {
                entry.getValue().defaultTo( session );
            }
        }

        /**
         * Send arguments to parameter handlers
         */
        for ( final MatchedParam matchedParam : foundArgsList ) {
            final PrismParameterHandler handler = matchedParam.getHandler();
            handler.process( session, matchedParam.getArg() );
        }
    }

    /**
     * 
     * @param session
     * @param arg
     */
    private static void parseParam( QuerySession session, String arg ){
        
        // Match command argument to parameter handler
        for ( final Entry<String, PrismParameterHandler> entry : Prism.getParameters().entrySet() ) {
            PrismParameterHandler parameterHandler = entry.getValue();
            if (!parameterHandler.applicable(arg, session.getSender())){
                continue;
            }
            if( !parameterHandler.hasPermission(arg, session.getSender()) ){
                throw new IllegalArgumentException("No permission for parameter '" + arg + "', skipped.");
            }
            session.addFoundArgument( new MatchedParam(parameterHandler, arg ) );
            break;
        }
        
        // We support an alternate player syntax so that people
        // can use the tab-complete
        // feature of minecraft. Using p: prevents it.
        final Player autoFillPlayer = Bukkit.getServer().getPlayer( arg );
        if( autoFillPlayer != null ){
            // Match
            if( arg.startsWith( "!" ) ){
                session.getQuery().setPlayerMatchRule( MatchRule.EXCLUDE );
            }
            // Find player
            OfflinePlayer player = Bukkit.getOfflinePlayer( arg.replace( "!", "" ) );
            if( player != null ){
                session.getQuery().addPlayer( player );
                return;
            }
        }
        
        throw new IllegalArgumentException("Unrecognized parameter '" + arg + ".");
        
    }

    /**
     * 
     * @param sender
     * @param args
     * @param arg
     * @return
     */
    public static List<String> complete(CommandSender sender, String[] args, int arg) {
        // Iterate all command arguments
        if( args == null || args.length <= arg ) { return null; }

        return complete( sender, args[arg] );
    }

    /**
     * 
     * @param sender
     * @param args
     * @return
     */
    public static List<String> complete(CommandSender sender, String[] args) {
        return complete( sender, args, args.length - 1 );
    }

    /**
     * 
     * @param sender
     * @param arg
     * @return
     */
    public static List<String> complete(CommandSender sender, String arg) {
        if( arg.isEmpty() )
            return null;

        // Load registered parameters
        final HashMap<String, PrismParameterHandler> registeredParams = Prism.getParameters();

        // Match command argument to parameter handler
        for ( final Entry<String, PrismParameterHandler> entry : registeredParams.entrySet() ) {
            if( entry.getValue().applicable( arg, sender ) && entry.getValue().hasPermission( arg, sender ) ) { return entry.getValue().tabComplete( arg, sender ); }
        }

        return null;
    }
}