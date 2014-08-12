package me.botsko.prism.commandlibs;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actionlibs.QueryParameters.MatchRule;
import me.botsko.prism.appliers.PrismProcessType;
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
    public static QueryParameters extractQueryFromCommand( QuerySession session, CallInfo call ) throws IllegalArgumentException {

        // Start query
        final QueryParameters parameters = session.newQuery();

        // Define pagination/process type
        parameters.setLimit( Prism.config.getInt( "prism.queries.lookup-max-results" ) );
        parameters.setPerPage( Prism.config.getInt( "prism.queries.default-results-per-page" ) );

        // Load registered parameters
        final HashMap<String, PrismParameterHandler> registeredParams = Prism.getParameters();

        // Store names of matched params/handlers
        final Set<String> foundArgsNames = new HashSet<String>();
        final List<MatchedParam> foundArgsList = new ArrayList<MatchedParam>();

        // Iterate arguments
        for ( int i = 1; i < call.getArgs().length; i++ ) {

            final String arg = call.getArg(i);
            if( arg.isEmpty() ) continue;

            if ( parseParam().equals( ParseResult.NotFound ) ){
                
            }
        }
        session.setFoundArgs( foundArgsNames );

        // Reject no matches
        if( foundArgsList.isEmpty() ) {
            throw new IllegalArgumentException("No valid arguments were found.");
        }

        /**
         * Call default method for handlers *not* used
         */
        for ( final Entry<String, PrismParameterHandler> entry : registeredParams.entrySet() ) {
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

        return parameters;
        
    }

    /**
     * 
     * @param plugin
     * @param sender
     * @param parameters
     * @param registeredParams
     * @param foundArgsNames
     * @param foundArgsList
     * @param arg
     * @return
     */
    private static ParseResult parseParam(){
        ParseResult result = ParseResult.NotFound;

        // Match command argument to parameter handler
        for ( final Entry<String, PrismParameterHandler> entry : registeredParams.entrySet() ) {
            PrismParameterHandler parameterHandler = entry.getValue();
            if (!parameterHandler.applicable(arg, sender)) {
                continue;
            }
            if( !parameterHandler.hasPermission(arg, sender) ) {
                result = ParseResult.NoPermission;
                continue;
            }
            result = ParseResult.Found;
            foundArgsList.add( new MatchedParam(parameterHandler, arg ) );
            foundArgsNames.add( parameterHandler.getName().toLowerCase() );
            break;
        }

        // Reject argument that doesn't match anything
        if( result == ParseResult.NotFound ) {
            // We support an alternate player syntax so that people
            // can use the tab-complete
            // feature of minecraft. Using p: prevents it.
            final Player autoFillPlayer = plugin.getServer().getPlayer( arg );
            if( autoFillPlayer != null ) {
                // Match
                if( arg.startsWith( "!" ) ) {
                    parameters.setPlayerMatchRule( MatchRule.EXCLUDE );
                }
                // Find player
                OfflinePlayer player = Bukkit.getOfflinePlayer( arg.replace( "!", "" ) );
                if( player != null ){
                    result = ParseResult.Found;
                    parameters.addPlayer( player );
                }
            }
        }

        switch (result) {
            case NotFound:
                if (sender != null)
                    sender.sendMessage(Prism.messenger.playerError("Unrecognized parameter '" + arg + "."));
                break;
            case NoPermission:
                if (sender != null)
                    sender.sendMessage(Prism.messenger.playerError("No permission for parameter '" + arg + "', skipped."));
                break;
            default:
                break;
        }
        return result;
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

    /**
	 * 
	 */
    private static class MatchedParam {
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

    /**
     * 
     * @author botskonet
     *
     */
    private enum ParseResult {
        NotFound,
        NoPermission,
        Found
    }
}