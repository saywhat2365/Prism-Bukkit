package me.botsko.prism.parameters;

import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.actionlibs.QueryParameters.MatchRule;

import org.bukkit.command.CommandSender;

import java.util.regex.Pattern;

public class EntityParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public EntityParameter() {
        super( "Entity", Pattern.compile( "[~|!]?[\\w,]+" ), "e" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {
        MatchRule match = MatchRule.INCLUDE;
        if( input.startsWith( "!" ) ) {
            match = MatchRule.EXCLUDE;
        }
        final String[] entityNames = input.split( "," );
        if( entityNames.length > 0 ) {
            for ( final String entityName : entityNames ) {
                session.getQuery().addEntity( entityName.replace( "!", "" ), match );
            }
        }
    }
}