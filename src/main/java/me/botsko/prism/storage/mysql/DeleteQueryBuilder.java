package me.botsko.prism.storage.mysql;

import java.util.HashMap;

public class DeleteQueryBuilder extends SelectQueryBuilder {

    
    /**
     * 
     * @param prismActions
     */
    public DeleteQueryBuilder(HashMap<String, Integer> prismActions) {
        super( prismActions );
    }

    /**
	 * 
	 */
    @Override
    public String select() {
        return "DELETE FROM " + tableNameData + " USING " + tableNameData + 
        " LEFT JOIN " + tableNameDataExtra + " ex ON (" + tableNameData + ".id = ex.data_id) ";
    }

    /**
	 * 
	 */
    @Override
    protected String group() {
        return "";
    }

    /**
	 * 
	 */
    @Override
    protected String order() {
        return "";
    }

    /**
	 * 
	 */
    @Override
    protected String limit() {
        return "";
    }
}