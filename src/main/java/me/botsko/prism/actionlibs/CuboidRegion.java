package me.botsko.prism.actionlibs;

import org.bukkit.Location;

public class CuboidRegion {
    
    private final Location min;
    private final Location max;
    
    /**
     * 
     * @param min
     * @param max
     */
    public CuboidRegion( Location min, Location max ){
        this.min = min;
        this.max = max;
    }
    
    /**
     * 
     * @return
     */
    public int getAverageRadius(){
        int x = max.getBlockX()-min.getBlockX();
        int y = max.getBlockY()-min.getBlockY();
        int z = max.getBlockZ()-min.getBlockZ();
        return (int)Math.ceil( (x+y+z)/3 );
    }
}