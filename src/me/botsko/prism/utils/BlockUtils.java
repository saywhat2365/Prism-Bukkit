package me.botsko.prism.utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class BlockUtils {
	
	
	/**
	 * Returns whether or not the plugin may place a material,
	 * so that we can avoid including dangerous items with an
	 * applier.
	 * 
	 * @param block
	 * @return
	 */
	public static boolean mayEverPlace( Material m ){
		if( m.equals(Material.TNT) || m.equals(Material.FIRE) || m.equals(Material.LAVA) ){
			return false;
		}
		return true;
	}
	
	
	/**
	 * Recursively grabs a list of all blocks directly above Block
	 * that are anticipated to fall.
	 * 
	 * @param block
	 * @return
	 */
	public static ArrayList<Block> findFallingBlocksAboveBlock( Block block ){
		
		ArrayList<Block> falling_blocks = new ArrayList<Block>();
		
		// Get block above
		Block above = block.getRelative(BlockFace.UP);
		if(BlockUtils.isFallingBlock(above)){
			falling_blocks.add(above);
			ArrayList<Block> fallingBlocksAbove = findFallingBlocksAboveBlock( above );
			if(fallingBlocksAbove.size() > 0){
				for(Block _temp : fallingBlocksAbove){
					falling_blocks.add(_temp);
				}
			}
		}
		return falling_blocks;
	}
	
	
	/**
	 * Determine whether or not a block is capable of falling.
	 * 
	 * Seems like there's got to be another way to do this...
	 * @param block
	 * @return
	 */
	public static boolean isFallingBlock( Block block ){
		Material m = block.getType();
		if( m.equals(Material.SAND) || m.equals(Material.GRAVEL) || m.equals(Material.ANVIL) ){
			return true;
		}
		return false;
	}
	
	
	/**
	 * Searches for detachable blocks on the four acceptable sides of a block.
	 * 
	 * @param block
	 * @return
	 */
	public static ArrayList<Block> findAttachedBlocks( Block block ){
		
		ArrayList<Block> falling_blocks = new ArrayList<Block>();
		
		// Check each of the four sides
		Block blockToCheck = block.getRelative(BlockFace.EAST);
		if(BlockUtils.isDetachableBlock(blockToCheck)){
			falling_blocks.add(blockToCheck);
		}
		blockToCheck = block.getRelative(BlockFace.WEST);
		if(BlockUtils.isDetachableBlock(blockToCheck)){
			falling_blocks.add(blockToCheck);
		}
		blockToCheck = block.getRelative(BlockFace.NORTH);
		if(BlockUtils.isDetachableBlock(blockToCheck)){
			falling_blocks.add(blockToCheck);
		}
		blockToCheck = block.getRelative(BlockFace.SOUTH);
		if(BlockUtils.isDetachableBlock(blockToCheck)){
			falling_blocks.add(blockToCheck);
		}
		
		return falling_blocks;
		
	}
	
	
	/**
	 * Determine whether or not a block is capable of falling.
	 * 
	 * Seems like there's got to be another way to do this...
	 * @param block
	 * @return
	 */
	public static boolean isDetachableBlock( Block block ){
		Material m = block.getType();
		if( m.equals(Material.WALL_SIGN) || m.equals(Material.TORCH) ){
			return true;
		}
		return false;
	}
	
	
	/**
	 * Searches for detachable entities in a
	 * 
	 * @param block
	 * @return
	 */
	public static ArrayList<Entity> findHangingEntities( Block block ){
		
		ArrayList<Entity> entities = new ArrayList<Entity>();
		
		Entity[] foundEntities = block.getChunk().getEntities();
		if(foundEntities.length > 0){
			for(Entity e : foundEntities){
				// Let's limit this to only entities within 1 block of the current.
				if( block.getLocation().distance( e.getLocation() ) < 2 && isHangingEntity(e) ){
					entities.add(e);
				}
			}
		}
		
		return entities;
		
	}
	
	
	/**
	 * Is an entity a hanging type, attachable to a block.
	 * @param block
	 * @return
	 */
	public static boolean isHangingEntity( Entity entity ){
		EntityType e = entity.getType();
		if( e.equals(EntityType.ITEM_FRAME) || e.equals(EntityType.PAINTING) ){
			return true;
		}
		return false;
	}
	
	
	/**
	 * 
	 * @param mat
	 * @param loc
	 * @param radius
	 */
	public static int removeMaterialFromRadius(Material mat, Location loc, int radius){
		Material[] materials = { mat };
		return removeMaterialsFromRadius(materials, loc, radius);
	}
	
	
	/**
	 * 
	 * @param mat
	 * @param loc
	 * @param radius
	 */
	public static int removeMaterialsFromRadius(Material[] materials, Location loc, int radius){
		int blocks_removed = 0;
		int x1 = loc.getBlockX();
		int y1 = loc.getBlockY();
		int z1 = loc.getBlockZ();
		World world = loc.getWorld();
		for(int x = x1-radius; x <= x1+radius; x++){
			for(int y = y1-radius; y <= y1+radius; y++){
				for(int z = z1-radius; z <= z1+radius; z++){
					loc = new Location(world, x, y, z);
					Block b = loc.getBlock();
					if(b.getType().equals(Material.AIR)) continue;
					if( Arrays.asList(materials).contains(loc.getBlock().getType()) ){
						loc.getBlock().setType(Material.AIR);
						blocks_removed++;
					}
				}
			}
		}
		return blocks_removed;
	}
	
	
	/**
	 * Extinguish all the fire in a radius
	 * @param loc The location you want to extinguish around
	 * @param radius The radius around the location you are extinguish
	 */
	public static int extinguish(Location loc, int radius){
		return removeMaterialFromRadius(Material.FIRE, loc, radius);
	}
	
	
	/**
	 * Drains lava and water within (radius) around (loc).
	 * @param loc
	 * @param radius
	 */
	public static int drain(Location loc, int radius){
		Material[] materials = { Material.LAVA, Material.STATIONARY_LAVA, Material.WATER };
		return removeMaterialsFromRadius(materials, loc, radius);
	}
	
	
	/**
	 * Drains lava blocks (radius) around player's loc.
	 * @param loc
	 * @param radius
	 */
	public static int drainlava(Location loc, int radius){
		Material[] materials = { Material.LAVA, Material.STATIONARY_LAVA };
		return removeMaterialsFromRadius(materials, loc, radius);
	}
	
	
	/**
	 * Drains water blocks (radius) around player's loc.
	 * @param loc
	 * @param radius
	 */
	public static int drainwater(Location loc, int radius){
		return removeMaterialFromRadius(Material.WATER, loc, radius);
	}
}