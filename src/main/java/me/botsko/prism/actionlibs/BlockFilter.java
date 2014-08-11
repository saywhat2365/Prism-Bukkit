package me.botsko.prism.actionlibs;

import org.bukkit.Material;

public class BlockFilter {
    
    private final Material material;
    private final short data;
    
    /**
     * A wrapper for a Material/data value. We can't use MaterialData because
     * it expects a byte, when mod servers use short.
     * @param material
     * @param data
     */
    public BlockFilter( Material material, short data ){
        this.material = material;
        this.data = data;
    }
}