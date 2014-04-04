package me.botsko.prism.storage.mysql;

import java.util.ArrayList;
import java.util.HashMap;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;

public class BlockReportQueryBuilder extends SelectQueryBuilder {

    /**
     * 
     * @param prismActions
     */
    public BlockReportQueryBuilder(HashMap<String, Integer> prismActions) {
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

        parameters.addActionType( "block-place" );

        // block-place query
        String sql = "" + "SELECT block_id, SUM(placed) AS placed, SUM(broken) AS broken " + "FROM (("
                + "SELECT block_id, COUNT(id) AS placed, 0 AS broken " + "FROM prism_data " + where() + " "
                + "GROUP BY block_id) ";

        conditions.clear();
        parameters.getActionTypes().clear();
        parameters.addActionType( "block-break" );

        sql += "UNION ( " + "SELECT block_id, 0 AS placed, count(id) AS broken " + "FROM prism_data " + where() + " "
                + "GROUP BY block_id)) " + "AS PR_A " + "GROUP BY block_id ORDER BY (SUM(placed) + SUM(broken)) DESC";

        return sql;

    }
}