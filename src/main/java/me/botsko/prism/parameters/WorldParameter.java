package me.botsko.prism.parameters;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.appliers.PrismProcessType;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class WorldParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public WorldParameter() {
        super( "World", Pattern.compile( "[^\\s]+" ), "w" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {
        World  world;
        if( input.equalsIgnoreCase( "current" ) ) {
            if( session.getSender() instanceof Player ) {
                world = ( (Player) session.getSender() ).getWorld();
            } else {
                session.getSender().sendMessage( Prism.messenger
                        .playerError( "Can't use the current world since you're not a player. Using default world." ) );
                world = Bukkit.getServer().getWorlds().get( 0 );
            }
        } else {
            world = Bukkit.getWorld( input );
        }
        session.getQuery().setWorld( world );
    }

    /**
	 * 
	 */
    @Override
    public void defaultTo( QuerySession session ){
        if( session.getQuery().getProcessType().equals( PrismProcessType.DELETE ) )
            return;
        if( session.getSender() instanceof Player && !session.getQuery().allowsNoRadius() ) {
            session.getQuery().setWorld( ( (Player) session.getSender() ).getWorld() );
        }
    }
}