package me.botsko.prism.parameters;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actionlibs.QueryParameters.MatchRule;
import me.botsko.prism.appliers.PrismProcessType;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class ActionParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public ActionParameter() {
        super( "Action", Pattern.compile( "[~|!]?[\\w,-]+" ), "a" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {

        if( input.startsWith( "!" ) ){
            session.getQuery().setActionTypesMatchRule( MatchRule.EXCLUDE );
        }

        final String[] actions = input.split( "," );
        if( actions.length > 0 ){
            for ( final String action : actions ){
                // Find all actions that match the action provided - whether the
                // full name or short name.
                final ArrayList<ActionType> actionTypes = Prism.getActionRegistry().getActionsByShortname(action.replace( "!", "" ) );
                if( !actionTypes.isEmpty() ){
                    for( ActionType actionType : actionTypes ){

                        // Ensure the action allows this process type
                        if( ( session.getQuery().getProcessType().equals( PrismProcessType.ROLLBACK ) && !actionType.canRollback() )
                                || ( session.getQuery().getProcessType().equals( PrismProcessType.RESTORE ) && !actionType
                                        .canRestore() ) ) {
                            // @todo this is important information but is too
                            // spammy with a:place, because vehicle-place
                            // doesn't support a rollback etc
                            // respond( sender,
                            // Prism.messenger.playerError("Ingoring action '"+actionType.getName()+"' because it doesn't support rollbacks.")
                            // );
                            continue;
                        }

                        session.getQuery().addActionType( actionType.getName() );
                    }
                } else {
                    throw new IllegalArgumentException( "Ignoring action '" + action.replace( "!", "" ) + "' because it's unrecognized." );
                }
            }
            // If none were valid, we end here.
            if( session.getQuery().getActionTypes().size() == 0 ){
                throw new IllegalArgumentException("Action parameter value not recognized. Try /pr ? for help" );
            }
        }
    }
}