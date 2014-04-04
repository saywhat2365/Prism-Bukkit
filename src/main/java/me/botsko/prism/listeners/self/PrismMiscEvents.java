package me.botsko.prism.listeners.self;

import me.botsko.prism.events.PrismBlocksDrainEvent;
import me.botsko.prism.events.PrismBlocksExtinguishEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PrismMiscEvents implements Listener {

    /**
     * 
     * @param event
     */
    @EventHandler
    public void onPrismBlocksDrainEvent(final PrismBlocksDrainEvent event) {

//        // Get all block changes for this event
//        final ArrayList<BlockStateChange> blockStateChanges = event.getBlockStateChanges();
//        if( !blockStateChanges.isEmpty() ) {
//
//            // Create an entry for the rollback as a whole
//            final Handler primaryAction = ActionFactory.create( "prism-process", PrismProcessType.DRAIN,
//                    event.onBehalfOf(), "" + event.getRadius() );
//            final int id = RecordingTask.insertActionIntoDatabase( primaryAction );
//            if( id == 0 ) { return; }
//            for ( final BlockStateChange stateChange : blockStateChanges ) {
//
//                final BlockState orig = stateChange.getOriginalBlock();
//                final BlockState newBlock = stateChange.getNewBlock();
//
//                // Build the action
//                RecordingQueue.addToQueue( ActionFactory.create( "prism-drain", orig, newBlock, event.onBehalfOf()
//                        .getName(), id ) );
//
//            }
//            // ActionQueue.save();
//        }
    }

    /**
     * 
     * @param event
     */
    @EventHandler
    public void onPrismBlocksExtinguishEvent(final PrismBlocksExtinguishEvent event) {

//        // Get all block changes for this event
//        final ArrayList<BlockStateChange> blockStateChanges = event.getBlockStateChanges();
//        if( !blockStateChanges.isEmpty() ) {
//
//            // Create an entry for the rollback as a whole
//            final Handler primaryAction = ActionFactory.create( "prism-process", PrismProcessType.EXTINGUISH,
//                    event.onBehalfOf(), "" + event.getRadius() );
//            final int id = RecordingTask.insertActionIntoDatabase( primaryAction );
//            if( id == 0 ) { return; }
//            for ( final BlockStateChange stateChange : blockStateChanges ) {
//
//                final BlockState orig = stateChange.getOriginalBlock();
//                final BlockState newBlock = stateChange.getNewBlock();
//
//                // Build the action
//                RecordingQueue.addToQueue( ActionFactory.create( "prism-extinguish", orig, newBlock, event.onBehalfOf()
//                        .getName(), id ) );
//
//            }
//            // ActionQueue.save();
//        }
    }
}