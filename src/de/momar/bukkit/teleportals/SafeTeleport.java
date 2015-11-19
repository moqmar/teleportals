package de.momar.bukkit.teleportals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Find the nearest safe location around a given one.
 * @author Moritz Marquardt
 */
public class SafeTeleport {
	
	/**
	 * Finds the nearest location to l which is safe to teleport to.
	 * @param l		The location to teleport to
	 * @return		If possible: a safe location around l. If not: null.
	 */
	public static Location getSafeLocation(Location l) {
		List<Material> blacklist = new ArrayList<Material>();
		blacklist.add(Material.AIR);
		blacklist.add(Material.LAVA);
		blacklist.add(Material.WATER);
		blacklist.add(Material.CACTUS);
		blacklist.add(Material.FIRE);
		blacklist.add(Material.PORTAL);
		blacklist.add(Material.ENDER_PORTAL);
		
		//Check if the location is safe
		if (isSafe(l, blacklist)) return l; //The given location is already safe.
		
		//When it's not, find the nearest safe block in the surrounding 10x5x10 cuboid.
		Location nearestReplacementBlock = l.clone().add(0,100,0);
		for (int x = -5; x < 5; x++) for (int z = -5; z < 5; z++) for (int y = -2; y < 3; y++) {
			Location replacementBlock = l.clone().add(x, y, z);
			if (isSafe(replacementBlock, blacklist) && l.distance(replacementBlock) < l.distance(nearestReplacementBlock)) nearestReplacementBlock = replacementBlock;
		}
		if (nearestReplacementBlock.distance(l.clone().add(0,100,0)) > 10) return nearestReplacementBlock;
		
		//When the surrounding blocks are not safe, try the highest block at this location (probably the surface).
		Location highestBlock = l.getWorld().getHighestBlockAt(l).getLocation().clone().add(0,1,0);
		if (isSafe(highestBlock, blacklist)) return highestBlock; //The highest block is safe. Last possibility for a safe location ;)
		
		//If it also is not (will probably only happen in the middle of an ocean), return null.
		return null;
	}
	
	/**
	 * Checks if a location is safe to teleport to.
	 * @param l			The location to check
	 * @param blacklist	A list of 'unsafe' blocks, like lava or cacti 
	 * @return			If the location is safe, this function will return true.
	 */
	private static boolean isSafe(Location l, List<Material> blacklist) {
		if (l.getY() < 1) return false;
		if (!blacklist.contains(l.clone().add(0,-1,0).getBlock().getType()) && //   Block to teleport on is not on blacklist
			l                   .getBlock().getType() == Material.AIR       && // + Block where the feet will be is air
			l.clone().add(0,1,0).getBlock().getType() == Material.AIR        ) // + Block where the head will be is air
			return true;                                                       // = Location is safe. Return true.
		return false;                                                          // Location is not safe. Return false.
	}
}
