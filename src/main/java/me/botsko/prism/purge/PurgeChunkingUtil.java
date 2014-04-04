package me.botsko.prism.purge;

public class PurgeChunkingUtil {

    /**
     * 
     * @param playername
     */
    public static int getMinimumPrimaryKey() {
     // @todo storageadapter move
        return 0;
//        int id = 0;
//        Connection conn = null;
//        PreparedStatement s = null;
//        ResultSet rs = null;
//        try {
//
//            conn = Prism.dbc();
//            s = conn.prepareStatement( "SELECT MIN(id) FROM prism_data" );
//            s.executeQuery();
//            rs = s.getResultSet();
//
//            if( rs.first() ) {
//                id = rs.getInt( 1 );
//            }
//
//        } catch ( final SQLException ignored ) {
//
//        } finally {
//            if( rs != null )
//                try {
//                    rs.close();
//                } catch ( final SQLException ignored ) {}
//            if( s != null )
//                try {
//                    s.close();
//                } catch ( final SQLException ignored ) {}
//            if( conn != null )
//                try {
//                    conn.close();
//                } catch ( final SQLException ignored ) {}
//        }
//        return id;
    }

    /**
     * 
     * @param playername
     */
    public static int getMaximumPrimaryKey() {
     // @todo storageadapter move
        return 0;
//        int id = 0;
//        Connection conn = null;
//        PreparedStatement s = null;
//        ResultSet rs = null;
//        try {
//
//            conn = Prism.dbc();
//            s = conn.prepareStatement( "SELECT id FROM prism_data ORDER BY id DESC LIMIT 1;" );
//            s.executeQuery();
//            rs = s.getResultSet();
//
//            if( rs.first() ) {
//                id = rs.getInt( 1 );
//            }
//
//        } catch ( final SQLException ignored ) {
//
//        } finally {
//            if( rs != null )
//                try {
//                    rs.close();
//                } catch ( final SQLException ignored ) {}
//            if( s != null )
//                try {
//                    s.close();
//                } catch ( final SQLException ignored ) {}
//            if( conn != null )
//                try {
//                    conn.close();
//                } catch ( final SQLException ignored ) {}
//        }
//        return id;
    }
}