package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

/**
 * An API for setting conditions for a records query.
 * 
 * @author botskonet
 * 
 */
public class QueryParameters implements Cloneable {
    
    public enum MatchRule {
        INCLUDE, EXCLUDE, PARTIAL
    }
    
    // Conditions
    protected World world = null;
    protected CuboidRegion selectionRegion = null;
    protected List<Location> specificLocations = new ArrayList<Location>();
    protected List<ActionType> actionTypes = new ArrayList<ActionType>();
    protected MatchRule actionsMatchRule = MatchRule.INCLUDE;
    protected List<BlockFilter> blockFilters = new ArrayList<BlockFilter>();
    protected List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
    protected MatchRule playersMatchRule = MatchRule.INCLUDE;
    protected Date minDate = null;
    protected Date maxDate = null;
    
    // Query configs
    protected PrismProcessType processType;
    protected int limit;
    protected int perPage;
    protected List<Flag> flags = new ArrayList<Flag>();
    
    // WORLDS
    
    /**
     * Set the world condition using the String name of the world.
     * @param worldName
     * @throws IllegalArgumentException
     */
    public void setWorldByName( String worldName ) throws IllegalArgumentException {
        World world = Bukkit.getWorld( worldName );
        if( world == null ){
            throw new IllegalArgumentException("No world found for the name " + worldName);
        }
        setWorld(world);
    }
    
    /**
     * Limit query to a specific world name.
     * @param world
     */
    public void setWorld( World world ){
        this.world = world;
    }
    
    /**
     * Returns the specific world
     * @return
     */
    public World getWorld(){
        return world;
    }
    
    // LOCATIONS
    
    /**
     * Define a region with a set radius around an origin location.
     * @param origin
     * @param blockRadius
     * @throws IllegalArgumentException
     */
    public void setRadius( Location origin, int blockRadius ) throws IllegalArgumentException {
        
        if( blockRadius <= 0 ){
            throw new IllegalArgumentException("Radius must be a positive value");
        }
        if( origin == null ){
            throw new IllegalArgumentException("Origin Location may not be null");
        }
        if( specificLocations.size() > 0 ){
            throw new IllegalArgumentException("Cannot set a region when any specific block locations exist.");
        }
        
        setWorld( origin.getWorld() );
        
        Location min = new Location( origin.getWorld(), origin.getX() - blockRadius, origin.getY() - blockRadius, origin.getZ() - blockRadius );
        Location max = new Location( origin.getWorld(), origin.getX() + blockRadius, origin.getY() + blockRadius, origin.getZ() + blockRadius );
        
        selectionRegion = new CuboidRegion(min,max);
        
    }
    
    /**
     * Directly set a region
     * @param selectionRegion
     * @throws IllegalArgumentException
     */
    public void setSelectedRegion( CuboidRegion selectionRegion ) throws IllegalArgumentException {
        if( selectionRegion == null ){
            throw new IllegalArgumentException("Region may not be null");
        }
        this.selectionRegion = selectionRegion;
    }
    
    /**
     * Returns the region specificed via an origin + radius
     * @return
     */
    public CuboidRegion getSelectedRegion(){
        return selectionRegion;
    }

    /**
     * Include only one or more specific locations
     * @param loc
     * @throws IllegalArgumentException
     */
    public void addLocation( Location loc ) throws IllegalArgumentException {
        if( loc == null ){
            throw new IllegalArgumentException("Location may not be null");
        }
        if( selectionRegion != null ){
            throw new IllegalArgumentException("Cannot add a block location when a region has been provided.");
        }
        specificLocations.add( loc );
    }
    
    /**
     * Returns a list of specific block locations
     * @return
     */
    public List<Location> getLocations(){
        return specificLocations;
    }
    
    // ACTIONS
    
    /**
     * Limit query to specific Prism action types.
     * @param type
     */
    public void addActionType( ActionType type ) throws IllegalArgumentException {
        if( type == null ){
            throw new IllegalArgumentException("Action type may not be null");
        }
        this.actionTypes.add( type );
    }
    
    /**
     * Returns action types specified
     * @return
     */
    public List<ActionType> getActionTypes(){
        return actionTypes;
    }
    
    /**
     * Set a match rule for the list of players
     * @param playersMatchRule
     */
    public void setActionTypesMatchRule( MatchRule playersMatchRule ){
        this.actionsMatchRule = playersMatchRule;
    }
    
    /**
     * Returns the match rule we'll apply to the action types
     * @return
     */
    public MatchRule getActionTypesMatchRule(){
        return actionsMatchRule;
    }
    
    // BLOCKS
    
    /**
     * Filter by block Material type. This method assumes the durability/data value
     * is 0.
     * @param material
     */
    public void addBlock( Material material ) throws IllegalArgumentException {
        if( material == null ){
            throw new IllegalArgumentException("Block filter material may not be null");
        }
        addBlock( new BlockFilter(material,(short)0) );
    }
    
    /**
     * Filter by a custom block filter.
     * @param filter
     */
    public void addBlock( BlockFilter filter ) throws IllegalArgumentException {
        if( filter == null ){
            throw new IllegalArgumentException("Block filter may not be null");
        }
        this.blockFilters.add( filter );
    }
    
    /**
     * Returns the list of block filters
     * @return
     */
    public List<BlockFilter> getBlockFilters(){
        return blockFilters;
    }
    
    // PLAYA's
    
    /**
     * Limit query to specific players. Be sure to use setPlayerMatchRule if you
     * need to exclude or partially match players.
     * @param player
     */
    public void addPlayer( OfflinePlayer player ) throws IllegalArgumentException {
        if( player == null ){
            throw new IllegalArgumentException("Player may not be null");
        }
        this.players.add( player );
    }
    
    /**
     * Set a match rule for the list of players
     * @param playersMatchRule
     */
    public void setPlayerMatchRule( MatchRule playersMatchRule ){
        this.playersMatchRule = playersMatchRule;
    }
    
    /**
     * Returns the list of player conditions
     * @return
     */
    public List<OfflinePlayer> getPlayers(){
        return players;
    }
    
    /**
     * Returns the match rule we'll apply to the player conditions
     * @return
     */
    public MatchRule getPlayerMatchRule(){
        return playersMatchRule;
    }
    
    // DATES
    
    /**
     * Query only records occurring after this date
     * @param since
     */
    public void setMinimumDate( Date since ) throws IllegalArgumentException {
        if( since == null ){
            throw new IllegalArgumentException("Minimum date may not be null");
        }
        this.minDate = since;
    }
    
    /**
     * Returns the minimum "since" date
     * @return
     */
    public Date getMinimumDate(){
        return minDate;
    }
    
    /**
     * Query only records occurring before this date
     * @param before
     */
    public void setMaximumDate( Date before ) throws IllegalArgumentException {
        if( before == null ){
            throw new IllegalArgumentException("Maximum date may not be null");
        }
        this.maxDate = before;
    }
    
    /**
     * Returns the maximum "before" date
     * @return
     */
    public Date getMaximumDate(){
        return maxDate;
    }


//    public HashMap<String, MatchRule> getEntities() {
//        return entity_filters;
//    }
//
//
//    public void addEntity(String entity, MatchRule match) {
//        this.entity_filters.put( entity, match );
//    }



//    public String getKeyword() {
//        return keyword;
//    }
//
// 
//    public void setKeyword(String keyword) {
//        this.keyword = keyword;
//    }


   
    
    // CONFIGS, NOT CONDITIONS

    /**
     * Indicates which applier process this query is for
     * @return
     */
    public PrismProcessType getProcessType(){
        return processType;
    }

    /**
     * Indicate which applier process this query is for
     * @param processType
     */
    public void setProcessType( PrismProcessType processType ){
        this.processType = processType;
    }

    /**
     * LOOKUP = Most recent actions first. ROLLBACK = Newest->Oldest so we can
     * "rewind" the events RESTORE = Oldest->Newest so we can "replay" the
     * events
     * 
     * @return
     */
    public String getSortDirection(){
        if( !this.processType.equals( PrismProcessType.RESTORE ) ) { return "DESC"; }
        return "ASC";
    }
    
    /**
     * Limit the number of records returned
     * @return
     */
    public int getLimit(){
        return limit;
    }

    /**
     * Set the max number of records to return
     * @param limit
     */
    public void setLimit(int limit){
        this.limit = limit;
    }
    
    /**
     * Sets a limit without any assumption of the current offset
     * @return
     */
    public int getPerPage(){
        return perPage;
    }

    /**
     * Returns a limit without any assumption of the current offset
     * @return
     */
    public void setPerPage( int perPage ){
        this.perPage = perPage;
    }

    // FLAGS
    
    /**
     * Add a query flag
     * @param flag
     */
    public void addFlag( Flag flag ){
        this.flags.add( flag );
    }

    /**
     * Whether or not a specific flag was included
     * @param flag
     * @return
     */
    public boolean hasFlag(Flag flag) {
        return flags.contains( flag );
    }
    

//  public Set<String> getFoundArgs() {
//      return foundArgs;
//  }
//  public void setFoundArgs(Set<String> foundArgs) {
//      this.foundArgs = foundArgs;
//  }
 




//    public void addDefaultUsed(String d) {
//        defaultsUsed.add( d );
//    }

//    public ArrayList<String> getDefaultsUsed() {
//        return defaultsUsed;
//    }


//    public void setStringFromRawArgs(String[] args, int start) {
//        String params = "";
//        if( args.length > 0 ) {
//            for ( int i = start; i < args.length; i++ ) {
//                params += " " + args[i];
//            }
//        }
//        original_command = params;
//    }

 
//    public String getOriginalCommand() {
//        return original_command;
//    }

    
//    public ArrayList<CommandSender> getSharedPlayers() {
//        return shared_players;
//    }
//
//    
//    public void addSharedPlayer(CommandSender sender) {
//        this.shared_players.add( sender );
//    }
//    
//
//    
//    
//    public void setMinChunkingKey(long minId) {
//        this.minChunkingId = minId;
//    }
//
//    
//    public long getMinChunkingKey() {
//        return this.minChunkingId;
//    }
//
//    
//    public void setMaxChunkingKey(long maxId) {
//        this.maxChunkingId = maxId;
//    }
//
//    
//    public long getMaxChunkingKey() {
//        return this.maxChunkingId;
//    }

//    /**
//	 * 
//	 */
//    @Override
//    public QueryParameters clone() throws CloneNotSupportedException {
//        final QueryParameters cloned = (QueryParameters) super.clone();
//        cloned.actionTypeRules = new HashMap<String, MatchRule>( actionTypeRules );
//        return cloned;
//    }
}
