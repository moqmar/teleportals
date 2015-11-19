package de.momar.bukkit.teleportals;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.adamantdreamer.ui.menu.MenuHandler;

public class TeleportHandler implements MenuHandler {
	Teleportals plugin;
	
	public TeleportHandler(Teleportals plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public Event.Result onClick(InventoryClickEvent event) {
		Player p = (Player) event.getWhoClicked();
		
		if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return Event.Result.ALLOW;
		
		//Set player slots
		int minPlayerSlot = Integer.parseInt(plugin.config("config.playerSlots").split("-")[0]);
		int maxPlayerSlot = -1;
		if (plugin.config("config.playerSlots").split("-").length > 1) maxPlayerSlot = Integer.parseInt(plugin.config("config.playerSlots").split("-")[1]);
		else minPlayerSlot = -1;
		
		//Set custom warp slots
		int minWarpSlot = Integer.parseInt(plugin.config("config.mywarpSlots").split("-")[0]);
		int maxWarpSlot = -1;
		if (plugin.config("config.mywarpSlots").split("-").length > 1) maxWarpSlot = Integer.parseInt(plugin.config("config.mywarpSlots").split("-")[1]);
		else minWarpSlot = -1;
		
		Date d = new Date();
		if (!(event.getSlot() >= minWarpSlot && event.getSlot() <= maxWarpSlot && event.isShiftClick()) && event.getSlot() != Integer.parseInt(plugin.config("config.mywarpNewSlot")))
			if (	plugin.teleporting.get(p.getUniqueId().toString()) != null &&
					d.getTime()/1000 - plugin.teleporting.get(p.getUniqueId().toString()) < plugin.getConfig().getLong("config.cooldown")) {
				String timeLeft = Long.toString(plugin.getConfig().getLong("config.cooldown") - (d.getTime()/1000 - plugin.teleporting.get(p.getUniqueId().toString())));
				plugin.displayError(p, "teleporting", timeLeft);
				event.getView().close();
				return Event.Result.DENY;
		}
		
		if (event.getSlot() >= minPlayerSlot && event.getSlot() <= maxPlayerSlot) {
			//Teleport to a player!
			
			//Get player from hidden UUID
			List<String> lore = event.getCurrentItem().getItemMeta().getLore();
			String uuid = lore.get(lore.size() - 1).split("§#")[1].replace("§", "");
			Player tar = null;
			for(Player pl : plugin.getServer().getOnlinePlayers()) {
				if(pl.getUniqueId().toString().equals(uuid)) tar = pl;
			}
			
			if (tar != null) tp(p, event, plugin.getConfig().getString("config.playerCost"), tar.getLocation());
			else System.out.println("No target!");
		} else if (event.getSlot() >= minWarpSlot && event.getSlot() <= maxWarpSlot) {
			//Warp to a custom warp point!
			String[] warp = plugin.customwarps.getStringList(event.getWhoClicked().getUniqueId().toString()).get(event.getSlot() - minWarpSlot).split(",");
			if (event.isShiftClick()) {
				new WarpEditor(plugin, p, warp, true, Integer.parseInt(warp[0]));
			} else {
				Location l = new Location(plugin.getServer().getWorld(warp[1]), Integer.parseInt(warp[2]), Integer.parseInt(warp[3]), Integer.parseInt(warp[4]));
				tp(p, event, plugin.getConfig().getString("config.playerCost"), l);
			}
		} else if (event.getSlot() == Integer.parseInt(plugin.config("config.mywarpNewSlot"))) {
			//Create new Warp point!
			new WarpEditor(plugin, p, new String[0], false, 0);
		} else if (plugin.getConfig().getConfigurationSection("warps." + event.getSlot()) != null) {
			//Public warp!
			ConfigurationSection config = plugin.getConfig().getConfigurationSection("warps." + event.getSlot());
			Location l = plugin.getTarget(config.getString("target"), p, config.getString("world"));
			if (l != null) tp(p, event, config.getString("cost"), l);
			else System.out.println("No target!");
		} else {
			//Display error: Something went terribly wrong
			if (event.getCurrentItem() != null && event.getCurrentItem().getItemMeta() != null)
				plugin.getLogger().log(Level.WARNING, p.getDisplayName() + " clicked an invalid teleport item: Slot " + event.getSlot() + " (" + event.getCurrentItem().getItemMeta().getDisplayName() + ")");
		}
		event.getView().close();
		return Event.Result.DENY;
	}

	@Override
	public void onClose(InventoryCloseEvent event) {}
	
	private boolean tp(Player p, InventoryClickEvent event, String cost, Location l) {
		
		System.out.println(l.toString());
		
		Location safeLocation = SafeTeleport.getSafeLocation(l);
		if (safeLocation == null) {
			plugin.displayError(p, "unsafe", "");
			event.getView().close();
			return false;
		}
		safeLocation.add(0.5, 0, 0.5);
		
		if (!pay_test(p, cost)) {
			plugin.displayError(p, "cost", cost);
			event.getView().close();
			return false;
		} else {
			pay(p, cost);
		}
		
		ItemStack compass = event.getWhoClicked().getItemInHand();
		if (!compass.getItemMeta().getDisplayName().equals(plugin.config("config.portalItem.name"))) return false;

		ItemMeta meta = compass.getItemMeta();
		List<String> lore = meta.getLore();

		short dur = (short) (Short.parseShort(lore.get(lore.size() - 1).split("§#")[1].replace("§", "")) - 1);
		
    	String invisibleDur = "";
    	for (char chr : Short.toString(dur).toCharArray()) invisibleDur += "§" + chr;
		
    	String[] nLore = Teleportals.advancedConfig(plugin.config("config.portalItem.lore"), p.getDisplayName(), "", Short.toString(dur)).split("\\|");
    	nLore[nLore.length - 1] = nLore[nLore.length - 1] + "§#" + invisibleDur;
		meta.setLore(Arrays.asList(nLore));
		compass.setItemMeta(meta);
		if (dur < 1) {
			event.getWhoClicked().setItemInHand(null);
			p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1, 0);
		} else if (plugin.config("config.portalItem.ench") == "true") event.getWhoClicked().setItemInHand(Teleportals.addGlow(compass));
		else event.getWhoClicked().setItemInHand(compass);

		p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 5*20, 5));
		p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 10*20, 1));
		p.playSound(p.getLocation(), Sound.PORTAL_TRAVEL, (float) 0.25, 0);
		p.playSound(safeLocation, Sound.PORTAL_TRAVEL, (float) 0.25, 0);
		
		Date d = new Date();
		plugin.teleporting.put(p.getUniqueId().toString(), d.getTime()/1000);

		if (plugin.getConfig().getBoolean("config.logTeleports")) plugin.getLogger().log(Level.INFO, p.getName() + " teleported to " + l.getWorld().getName() + "; X" + l.getBlockX() + " Y" + l.getBlockY() + " Z" + l.getBlockZ() + " [=> " + safeLocation.getWorld().getName() + "; X" + safeLocation.getBlockX() + " Y" + safeLocation.getBlockY() + " Z" + safeLocation.getBlockZ() + "] (" + event.getCurrentItem().getItemMeta().getDisplayName().replaceAll("\\§.", "") + ")");
		
		class Run implements Runnable {
	        Location l;
	        Player p;
	        Run(Player p, Location l) { this.p = p; this.l = l; }
	        public void run() {
	        	try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {}

				plugin.teleporting.remove(p);
				
				p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0);
				p.getWorld().playEffect(p.getLocation(), Effect.SMOKE, 0);
				p.getWorld().playEffect(p.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
				
				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() { @Override public void run() {
					p.teleport(l);
					//Fix for Multiverse-Inventories: Teleport again after 2 ticks!
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
						@Override
						public void run() {
							p.teleport(l);
						}
					}, 2);
				}});
				
				p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0);
				p.getWorld().playEffect(p.getLocation(), Effect.SMOKE, 0);
				p.getWorld().playEffect(p.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
				
	        }
	    }

		new Thread(new Run(p, safeLocation)).start();
		return true;
	}

	/**
	 * Testet, ob ein Spieler sich bestimmte Kosten leisten kann.
	 * @param player Der zu testende Spieler
	 * @param cost Der zu testende Kosten-String
	 * @return True, wenn der Spieler genug Level hat.
	 */
	public static boolean pay_test(Player player, String cost) {
		int costN = Integer.parseInt(cost.substring(0, cost.length() - 2));
		if (cost.endsWith("xp")) {
			if (cost.startsWith("-")) return true;
			else return player.getLevel() >= costN;
		
		} else if (cost.endsWith("hp")) {
			return true;
			
		} else if (cost.endsWith("sp")) {
			return true;

		} else if (cost.endsWith("go")) {
			int playerGold = 0;
			HashMap<Integer, ? extends ItemStack> goldStacks = player.getInventory().all(Material.GOLD_INGOT);
			for (Integer i : goldStacks.keySet()) {
				playerGold += goldStacks.get(i).getAmount();
			}
			return playerGold >= costN;
		} else {
			System.out.println("No cost unit defined!");
			return false;
			//Vault integration, NYI.
		}
	}
	
	/**
	 * Zieht einem Spieler bestimmte Kosten ab.
	 * @param player Der zu testende Spieler
	 * @param cost Der zu testende Kosten-String
	 * @return True, wenn der Spieler genug Level hat.
	 */
	public static void pay(Player player, String cost) {
		int costN = Integer.parseInt(cost.substring(0, cost.length() - 2));
		if (cost.endsWith("xp")) {
			if (cost.startsWith("-")) player.setLevel(Math.abs(costN)); //Wenn negativ: setze auf den entsprechenden positiven Wert.
			else player.setLevel(player.getLevel() - costN); //Wenn positiv: ziehe vom aktuellen Stand ab.
		
		} else if (cost.endsWith("hp")) {
			Damageable d = (Damageable) player; //Damit ich die Gesundheit bekommen kann ;)
			
			if (cost.startsWith("-")) player.setHealth((double) Math.abs(costN)); //Wenn negativ: setze auf den entsprechenden positiven Wert.
			else {
				if (d.getHealth() - costN < 0) player.setHealth(0.0);
				else player.setHealth(d.getHealth() - costN); //Wenn positiv: ziehe vom aktuellen Stand ab.
			}
		
		} else if (cost.endsWith("sp")) {
			if (cost.startsWith("-")) player.setFoodLevel(Math.abs(costN)); //Wenn negativ: setze auf den entsprechenden positiven Wert.
			else {
				if (player.getFoodLevel() - costN < 0) player.setFoodLevel(0);
				else player.setFoodLevel(player.getFoodLevel() - costN); //Wenn positiv: ziehe vom aktuellen Stand ab.
			}

		} else if (cost.endsWith("go")) {
			int goldLeft = costN;
			HashMap<Integer, ? extends ItemStack> goldStacks = player.getInventory().all(Material.GOLD_INGOT);
			for (Integer i : goldStacks.keySet()) {
				int amTake = (goldLeft > goldStacks.get(i).getAmount() ? goldStacks.get(i).getAmount() : goldLeft);
				int amNew = goldStacks.get(i).getAmount() - amTake;
				if (amNew == 0) player.getInventory().clear(i);
				else goldStacks.get(i).setAmount(amNew);
				goldLeft -= amTake;
			}
		} else {
			//Vault integration, NYI.
		}
	}
}
