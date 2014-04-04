package me.botsko.prism;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import me.botsko.elixr.MaterialAliases;
import me.botsko.prism.actionlibs.*;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.bridge.PrismBlockEditSessionFactory;
import me.botsko.prism.commands.PrismCommands;
import me.botsko.prism.commands.WhatCommand;
import me.botsko.prism.listeners.*;
import me.botsko.prism.listeners.self.PrismMiscEvents;
import me.botsko.prism.measurement.Metrics;
import me.botsko.prism.measurement.QueueStats;
import me.botsko.prism.measurement.TimeTaken;
import me.botsko.prism.monitors.OreMonitor;
import me.botsko.prism.monitors.UseMonitor;
import me.botsko.prism.parameters.*;
import me.botsko.prism.purge.PurgeManager;
import me.botsko.prism.storage.StorageAdapter;
import me.botsko.prism.storage.mysql.MysqlStorageAdapter;
import me.botsko.prism.wands.Wand;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Prism extends JavaPlugin {

    /**
     * Protected/private
     */
    private static String plugin_name;
    private String plugin_version;
    private static MaterialAliases items;
    private Language language;
    private static Logger log = Logger.getLogger( "Minecraft" );
    private final ArrayList<String> enabledPlugins = new ArrayList<String>();
    private static ActionRegistry actionRegistry;
    private static HandlerRegistry<?> handlerRegistry;
    private static Ignore ignore;
    private static ArrayList<Integer> illegalBlocks;
    private static ArrayList<String> illegalEntities;
    private static HashMap<String, String> alertedOres = new HashMap<String, String>();
    private static HashMap<String, PrismParameterHandler> paramHandlers = new HashMap<String, PrismParameterHandler>();
    private final ScheduledThreadPoolExecutor schedulePool = new ScheduledThreadPoolExecutor( 1 );
    private final ScheduledThreadPoolExecutor recordingMonitorTask = new ScheduledThreadPoolExecutor( 1 );
    // private ScheduledFuture<?> scheduledPurgeExecutor;
    private PurgeManager purgeManager;
    private static StorageAdapter storageAdapter = null;

    /**
     * Public
     */
    public Prism prism;
    public static Messenger messenger;
    public static FileConfiguration config;
    public static WorldEditPlugin plugin_worldEdit = null;
    public ActionsQuery actionsQuery;
    public OreMonitor oreMonitor;
    public UseMonitor useMonitor;
    public static ConcurrentHashMap<String, Wand> playersWithActiveTools = new ConcurrentHashMap<String, Wand>();
    public ConcurrentHashMap<String, PreviewSession> playerActivePreviews = new ConcurrentHashMap<String, PreviewSession>();
    public ConcurrentHashMap<String, ArrayList<Block>> playerActiveViews = new ConcurrentHashMap<String, ArrayList<Block>>();
    public ConcurrentHashMap<String, QueryResult> cachedQueries = new ConcurrentHashMap<String, QueryResult>();
    public ConcurrentHashMap<Location, Long> alertedBlocks = new ConcurrentHashMap<Location, Long>();
    public TimeTaken eventTimer;
    public QueueStats queueStats;
    public BukkitTask recordingTask;
    public int total_records_affected = 0;


    /**
     * We store a basic index of blocks we anticipate will fall, so that when
     * they do fall we can attribute them to the player who broke the original
     * block.
     * 
     * Once the block fall is registered, it's removed from here, so data should
     * not remain here long.
     */
    public ConcurrentHashMap<String, String> preplannedBlockFalls = new ConcurrentHashMap<String, String>();

    /**
     * VehicleCreateEvents do not include the player/entity that created it, so
     * we need to track players right-clicking rails with minecart vehicles, or
     * water for boats
     */
    public ConcurrentHashMap<String, String> preplannedVehiclePlacement = new ConcurrentHashMap<String, String>();

    /**
     * Enables the plugin and activates our player listeners
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void onEnable() {

        plugin_name = this.getDescription().getName();
        plugin_version = this.getDescription().getVersion();

        prism = this;

        log( "Initializing Prism " + plugin_version + ". By Viveleroi." );

        // Load configuration, or install if new
        loadConfig();

        if( getConfig().getBoolean( "prism.allow-metrics" ) ) {
            try {
                final Metrics metrics = new Metrics( this );
                metrics.start();
            } catch ( final IOException e ) {
                log( "MCStats submission failed." );
            }
        }

        // Load current database storage engine
        if( config.getString("").equals("mysql") ){
        	storageAdapter = new MysqlStorageAdapter();
        }

        // Establish connection to database server
        if( storageAdapter == null || !storageAdapter.connect( config )){
        	final String[] dbDisabled = new String[3];
        	dbDisabled[0] = "Prism will disable itself because your database configuration is invalid";
            dbDisabled[1] = "Make sure any database servers are online, credentials are correct, firewalls open.";
            dbDisabled[2] = "For help - try http://discover-prism.com/wiki/view/troubleshooting/";
            logSection( dbDisabled );
            disablePlugin();
        }


        if( isEnabled() ) {

            // Info needed for setup, init these here
            handlerRegistry = new HandlerRegistry();
            actionRegistry = new ActionRegistry();

            // Apply any updates
            final Updater up = new Updater( this );
            up.apply_updates();

            eventTimer = new TimeTaken( this );
            queueStats = new QueueStats();
            ignore = new Ignore( this );

            // Plugins we use
            checkPluginDependancies();

            // Assign event listeners
            getServer().getPluginManager().registerEvents( new PrismBlockEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismEntityEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismWorldEvents(), this );
            getServer().getPluginManager().registerEvents( new PrismPlayerEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismInventoryEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismVehicleEvents( this ), this );

            // BlockPhysics
            if( getConfig().getBoolean( "prism.bukkit.listeners.blockphysicsevent" ) ) {
                getServer().getPluginManager().registerEvents( new PrismBlockPhysicsEvent( this ), this );
            } else {
                log( "You've configured prism to never listen to the BlockPhysicsEvent." );
                log( "Prism will not be able to track block-fall, and block detachments, i.e. torches and signs." );
            }

            // InventoryMoveItem
            if( getConfig().getBoolean( "prism.track-hopper-item-events" ) && Prism.getIgnore().event( "item-insert" ) ) {
                getServer().getPluginManager().registerEvents( new PrismInventoryMoveItemEvent(), this );
            }

            if( getConfig().getBoolean( "prism.tracking.api.enabled" ) ) {
                getServer().getPluginManager().registerEvents( new PrismCustomEvents( this ), this );
            }

            // Assign Plugin listeners if enabled
            if( dependencyEnabled( "Herochat" ) && getConfig().getBoolean( "prism.tracking.player-chat" ) ) {
                getServer().getPluginManager().registerEvents( new PrismChannelChatEvents(), this );
            }

            // Assign listeners to our own events
            // getServer().getPluginManager().registerEvents(new
            // PrismRollbackEvents(), this);
            getServer().getPluginManager().registerEvents( new PrismMiscEvents(), this );

            // Add commands
            getCommand( "prism" ).setExecutor( new PrismCommands( this ) );
            getCommand( "prism" ).setTabCompleter( new PrismCommands( this ) );
            getCommand( "what" ).setExecutor( new WhatCommand( this ) );

            // Register official parameters
            registerParameter( new ActionParameter() );
            registerParameter( new BeforeParameter() );
            registerParameter( new BlockParameter() );
            registerParameter( new EntityParameter() );
            registerParameter( new FlagParameter() );
            registerParameter( new IdParameter() );
            registerParameter( new KeywordParameter() );
            registerParameter( new PlayerParameter() );
            registerParameter( new RadiusParameter() );
            registerParameter( new SinceParameter() );
            registerParameter( new WorldParameter() );

            // Init re-used classes
            messenger = new Messenger( plugin_name );
            actionsQuery = new ActionsQuery( this );
            oreMonitor = new OreMonitor( this );
            useMonitor = new UseMonitor( this );

            // Init async tasks
            actionRecorderTask();

            // Init scheduled events
            endExpiredQueryCaches();
            endExpiredPreviews();
            removeExpiredLocations();

            // Delete old data based on config
            launchScheduledPurgeManager();

            // Keep watch on db connections, other sanity
            launchInternalAffairs();

        }
    }

    /**
     * 
     * @return
     */
    public static String getPrismName() {
        return plugin_name;
    }

    /**
     * 
     * @return
     */
    public String getPrismVersion() {
        return this.plugin_version;
    }

    /**
     * Load configuration and language files
     */
    @SuppressWarnings("unchecked")
    public void loadConfig() {
        final PrismConfig mc = new PrismConfig( this );
        config = mc.getConfig();

        // Cache config arrays we check constantly
        illegalBlocks = (ArrayList<Integer>) getConfig().getList( "prism.appliers.never-place-block" );
        illegalEntities = (ArrayList<String>) getConfig().getList( "prism.appliers.never-spawn-entity" );

        final ConfigurationSection alertBlocks = getConfig().getConfigurationSection( "prism.alerts.ores.blocks" );
        alertedOres.clear();
        if( alertBlocks != null ) {
            for ( final String key : alertBlocks.getKeys( false ) ) {
                alertedOres.put( key, alertBlocks.getString( key ) );
            }
        }

        // Load language files
        // language = new Language( mc.getLang() );
        // Load items db
        items = new MaterialAliases();
    }

    /**
     * 
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
    }
    
    /**
     * 
     */
    public static StorageAdapter getStorageAdapter(){
    	return storageAdapter;
    }

    /**
     * 
     * @return
     */
    public Language getLang() {
        return this.language;
    }

    /**
	 * 
	 */
    public void checkPluginDependancies() {

        // HeroChat
        final Plugin herochat = getServer().getPluginManager().getPlugin( "Herochat" );
        if( herochat != null ) {
            enabledPlugins.add( "Herochat" );
            log( "HeroChat found. Switching chat listener to HC events" );
        }

        // WorldEdit
        final Plugin we = getServer().getPluginManager().getPlugin( "WorldEdit" );
        if( we != null ) {
            plugin_worldEdit = (WorldEditPlugin) we;
            PrismBlockEditSessionFactory.initialize();
            log( "WorldEdit found. Associated features enabled." );
        } else {
            log( "WorldEdit not found. Certain optional features of Prism disabled." );
        }
    }

    /**
     * 
     * @return
     */
    public boolean dependencyEnabled(String pluginName) {
        return enabledPlugins.contains( pluginName );
    }

    /**
     * 
     * @return
     */
    public static ArrayList<Integer> getIllegalBlocks() {
        return illegalBlocks;
    }

    /**
	 * 
	 */
    public static ArrayList<String> getIllegalEntities() {
        return illegalEntities;
    }

    /**
	 * 
	 */
    public static HashMap<String, String> getAlertedOres() {
        return alertedOres;
    }

    /**
     * 
     * @return
     */
    public static MaterialAliases getItems() {
        return items;
    }

    /**
     * 
     * @return
     */
    public static ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    /**
     * 
     * @return
     */
    public static HandlerRegistry<?> getHandlerRegistry() {
        return handlerRegistry;
    }

    /**
     * 
     * @return
     */
    public static Ignore getIgnore() {
        return ignore;
    }

    /**
     * 
     * @return
     */
    public PurgeManager getPurgeManager() {
        return purgeManager;
    }

    /**
     * Registers a parameter and a handler. Example:
     * 
     * pr l a:block-break. The "a" is an action, and the action handler will
     * process what "block-break" refers to.
     * 
     * @param handler
     */
    public static void registerParameter(PrismParameterHandler handler) {
        paramHandlers.put( handler.getName().toLowerCase(), handler );
    }

    /**
     * 
     * @return
     */
    public static HashMap<String, PrismParameterHandler> getParameters() {
        return paramHandlers;
    }

    /**
     * 
     * @return
     */
    public static PrismParameterHandler getParameter(String name) {
        return paramHandlers.get( name );
    }

    /**
	 * 
	 */
    public void endExpiredQueryCaches() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {

            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                for ( final Map.Entry<String, QueryResult> query : cachedQueries.entrySet() ) {
                    final QueryResult result = query.getValue();
                    final long diff = ( date.getTime() - result.getQueryTime() ) / 1000;
                    if( diff >= 120 ) {
                        cachedQueries.remove( query.getKey() );
                    }
                }
            }
        }, 2400L, 2400L );
    }

    /**
	 * 
	 */
    public void endExpiredPreviews() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {

            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                for ( final Map.Entry<String, PreviewSession> query : playerActivePreviews.entrySet() ) {
                    final PreviewSession result = query.getValue();
                    final long diff = ( date.getTime() - result.getQueryTime() ) / 1000;
                    if( diff >= 60 ) {
                        // inform player
                        final Player player = prism.getServer().getPlayer( result.getPlayer().getName() );
                        if( player != null ) {
                            player.sendMessage( Prism.messenger.playerHeaderMsg( "Canceling forgotten preview." ) );
                        }
                        playerActivePreviews.remove( query.getKey() );
                    }
                }
            }
        }, 1200L, 1200L );
    }

    /**
	 * 
	 */
    public void removeExpiredLocations() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {

            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                // Remove locations logged over five minute ago.
                for ( final Entry<Location, Long> entry : alertedBlocks.entrySet() ) {
                    final long diff = ( date.getTime() - entry.getValue() ) / 1000;
                    if( diff >= 300 ) {
                        alertedBlocks.remove( entry.getKey() );
                    }
                }
            }
        }, 1200L, 1200L );
    }

    /**
	 * 
	 */
    public void actionRecorderTask() {
        int recorder_tick_delay = getConfig().getInt( "prism.queue-empty-tick-delay" );
        if( recorder_tick_delay < 1 ) {
            recorder_tick_delay = 3;
        }
        // we schedule it once, it will reschedule itself
        recordingTask = getServer().getScheduler().runTaskLaterAsynchronously( this, new RecordingTask( prism ),
                recorder_tick_delay );
    }

    /**
	 * 
	 */
    public void launchScheduledPurgeManager() {
        final List<String> purgeRules = getConfig().getStringList( "prism.db-records-purge-rules" );
        purgeManager = new PurgeManager( this, purgeRules );
        // scheduledPurgeExecutor =
        schedulePool.scheduleAtFixedRate( purgeManager, 0, 12, TimeUnit.HOURS );
        // scheduledPurgeExecutor.cancel();
    }

    /**
	 * 
	 */
    public void launchInternalAffairs() {
        final InternalAffairs recordingMonitor = new InternalAffairs( this );
        recordingMonitorTask.scheduleAtFixedRate( recordingMonitor, 0, 5, TimeUnit.MINUTES );
    }

    /**
     * 
     * @param msg
     */
    public void alertPlayers(Player player, String msg) {
        for ( final Player p : getServer().getOnlinePlayers() ) {
            if( !p.equals( player ) || getConfig().getBoolean( "prism.alerts.alert-player-about-self" ) ) {
                if( p.hasPermission( "prism.alerts" ) ) {
                    p.sendMessage( messenger.playerMsg( ChatColor.RED + "[!] " + msg ) );
                }
            }
        }
    }

    /**
     * 
     * @return
     */
    public String msgMissingArguments() {
        return messenger.playerError( "Missing arguments. Check /prism ? for help." );
    }

    /**
     * 
     * @return
     */
    public String msgInvalidArguments() {
        return messenger.playerError( "Invalid arguments. Check /prism ? for help." );
    }

    /**
     * 
     * @return
     */
    public String msgInvalidSubcommand() {
        return messenger.playerError( "Prism doesn't have that command. Check /prism ? for help." );
    }

    /**
     * 
     * @return
     */
    public String msgNoPermission() {
        return messenger.playerError( "You don't have permission to perform this action." );
    }

    /**
     * 
     * @param player
     * @param msg
     */
    public void notifyNearby(Player player, int radius, String msg) {
        if( !getConfig().getBoolean( "prism.appliers.notify-nearby.enabled" ) ) { return; }
        for ( final Player p : player.getServer().getOnlinePlayers() ) {
            if( !p.equals( player ) ) {
                if( player.getWorld().equals( p.getWorld() ) ) {
                    if( player.getLocation().distance( p.getLocation() ) <= ( radius + config
                            .getInt( "prism.appliers.notify-nearby.additional-radius" ) ) ) {
                        p.sendMessage( messenger.playerHeaderMsg( msg ) );
                    }
                }
            }
        }
    }

    /**
     * 
     * @param message
     */
    public static void log(String message) {
        log.info( "[" + getPrismName() + "]: " + message );
    }

    /**
     * 
     * @param messages
     */
    public static void logSection(String[] messages) {
        if( messages.length > 0 ) {
            log( "--------------------- ## Important ## ---------------------" );
            for ( final String msg : messages ) {
                log( msg );
            }
            log( "--------------------- ## ========= ## ---------------------" );
        }
    }

    /**
     * 
     * @param message
     */
    public static void debug(String message) {
        if( config.getBoolean( "prism.debug" ) ) {
            log.info( "[" + plugin_name + "]: " + message );
        }
    }

    /**
     * 
     * @param loc
     */
    public static void debug(Location loc) {
        debug( "Location: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() );
    }

    /**
     * Disable the plugin
     */
    public void disablePlugin() {
        this.setEnabled( false );
    }

    /**
     * Shutdown
     */
    @Override
    public void onDisable() {

        if( getConfig().getBoolean( "prism.database.force-write-queue-on-shutdown" ) ) {
            final QueueDrain drainer = new QueueDrain( this );
            drainer.forceDrainQueue();
        }

        if( storageAdapter != null ){
        	storageAdapter.close();
        }

        log( "Closing plugin." );

    }
}
