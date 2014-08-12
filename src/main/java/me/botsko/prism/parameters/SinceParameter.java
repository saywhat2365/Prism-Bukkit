package me.botsko.prism.parameters;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.utils.DateUtil;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Date;
import java.util.regex.Pattern;

public class SinceParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public SinceParameter() {
        super( "Since", Pattern.compile( "[\\w]+" ), "t", "since" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {
        final Date date = DateUtil.translateTimeStringToDate( input );
        if( date != null ) {
            session.getQuery().setMinimumDate( date );
        } else {
            throw new IllegalArgumentException("Date/time for 'since' parameter value not recognized. Try /pr ? for help" );
        }
    }

    /**
	 * 
	 */
    @Override
    public void defaultTo( QuerySession session ){

        if( session.getQuery().getProcessType().equals( PrismProcessType.DELETE ) ) return;

        // Enforce defaults
        if( session.getQuery().getMinimumDate() == null && session.getQuery().getMaximumDate() == null ) {

            final FileConfiguration config = Bukkit.getPluginManager().getPlugin( "Prism" ).getConfig();

            Date date = DateUtil.translateTimeStringToDate( config.getString( "prism.queries.default-time-since" ) );
            if( date == null ) {
                Prism.log( "Error - date range configuration for prism.time-since is not valid" );
                date = DateUtil.translateTimeStringToDate( "3d" );
            }
            session.getQuery().setMinimumDate( date );
//            query.addDefaultUsed( "t:" + config.getString( "prism.queries.default-time-since" ) );
        }
    }
}