package me.botsko.prism.parameters;

import me.botsko.prism.actionlibs.QuerySession;
import me.botsko.prism.utils.DateUtil;

import java.util.Date;
import java.util.regex.Pattern;

public class BeforeParameter extends SimplePrismParameterHandler {

    /**
	 * 
	 */
    public BeforeParameter() {
        super( "Before", Pattern.compile( "[\\w]+" ), "before" );
    }

    /**
	 * 
	 */
    @Override
    public void process( QuerySession session, String alias, String input ) {
        final Date date = DateUtil.translateTimeStringToDate( input );
        if( date != null ) {
            session.getQuery().setMaximumDate( date );
        } else {
            throw new IllegalArgumentException(
                    "Date/time for 'before' parameter value not recognized. Try /pr ? for help" );
        }
    }
}