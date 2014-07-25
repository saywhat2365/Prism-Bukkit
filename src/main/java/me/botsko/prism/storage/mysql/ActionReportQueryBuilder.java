package me.botsko.prism.storage.mysql;

import java.util.ArrayList;
import java.util.HashMap;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;

public class ActionReportQueryBuilder extends SelectQueryBuilder {

    /**
     * 
     * @param prismActions
     */
    public ActionReportQueryBuilder(HashMap<String, Integer> prismActions) {
        super( prismActions );
    }

    /**
     * 
     * @param parameters
     * @param shouldGroup
     * @return
     */
    @Override
    public String getQuery(QueryParameters parameters, boolean shouldGroup) {

        this.parameters = parameters;
        this.shouldGroup = shouldGroup;

        // Reset
        columns = new ArrayList<String>();
        conditions = new ArrayList<String>();

        String query = select();

        query += ";";

        Prism.debug( query );

        return query;

    }

    /**
	 * 
	 */
    @Override
    public String select() {
        String prefix = Prism.config.getString("prism.mysql.prefix");

        final String sql = "SELECT COUNT(*), a.action " + "FROM " + prefix + "data "
                + "INNER JOIN " + prefix + "actions a ON a.action_id = " + prefix + "data.action_id " + where() + " "
                + "GROUP BY a.action_id " + "ORDER BY COUNT(*) DESC";

        return sql;

    }
}