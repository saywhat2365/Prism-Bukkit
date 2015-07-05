package me.botsko.prism.bridge;

import org.bukkit.Bukkit;

import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;

public class PrismBlockEditHandler {
	@Subscribe
	public void wrapForLogging(EditSessionEvent event) {
		Actor actor = event.getActor();
		org.bukkit.World world = Bukkit.getWorld(event.getWorld().getName());
		if (actor != null && actor.isPlayer() && world != null) {			
			event.setExtent(new PrismWorldEditLogger(actor, event.getExtent(), world));
		}
	}
}