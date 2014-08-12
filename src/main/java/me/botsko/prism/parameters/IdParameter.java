package me.botsko.prism.parameters;

import me.botsko.elixr.TypeUtils;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;

import org.bukkit.command.CommandSender;

import java.util.regex.Pattern;

public class IdParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public IdParameter() {
        super( "ID", Pattern.compile( "[\\d,]+" ), "id" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {
        if( !TypeUtils.isNumeric( input ) ) { 
            throw new IllegalArgumentException("ID must be a number. Use /prism ? for help." );
        }
        session.getQuery().setId( Integer.parseInt( input ) );
    }
}