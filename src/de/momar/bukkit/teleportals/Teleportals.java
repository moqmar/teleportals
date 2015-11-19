package de.momar.bukkit.teleportals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.minecraft.server.v1_8_R2.NBTTagCompound;
import net.minecraft.server.v1_8_R2.NBTTagList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R2.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.adamantdreamer.ui.menu.Menu;
import com.adamantdreamer.ui.noteboard.Note;

public class Teleportals extends JavaPlugin implements Listener {
	public HashMap<String, Location> deathLocation;
	public HashMap<String, Long> teleporting;

	public YamlConfiguration translation, customwarps;
	
	public void onEnable(){
		//Standard-Zeugs!
		saveDefaultConfig();
		getServer().getPluginManager().registerEvents(this, this);
		
		//Todespunkte der Spieler
		deathLocation = new HashMap<String, Location>();
		//Ob sich ein Spieler im Moment teleportiert
		teleporting = new HashMap<String, Long>();
		
		//Create/Read translation file
		try {
			File translationFile = new File(this.getDataFolder().getCanonicalPath() + File.separator + "translation.yml");
			if (translationFile.exists() == false) {
				InputStream defaultFile = getClass().getResourceAsStream("/translation.yml");
				FileOutputStream newFile = new FileOutputStream(translationFile);
				int nextByte = 0;
				while ((nextByte = defaultFile.read()) != -1) {
					newFile.write(nextByte);
				}
				newFile.close();
			}
			//Read translation file
			translation = new YamlConfiguration();
			translation.load(translationFile);
		} catch (IOException | InvalidConfigurationException e) { e.printStackTrace(); }

		//Create/Read custom warps file
		try {
			File warpFile = new File(this.getDataFolder().getCanonicalPath() + File.separator + "customWarps.yml");
			if (warpFile.exists() == false) warpFile.createNewFile();
			customwarps = new YamlConfiguration();
			customwarps.load(warpFile);
		} catch (IOException | InvalidConfigurationException e) { e.printStackTrace(); }
	}
	public void onDisable(){}
	
	//Tempor§r: Befehl
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (sender instanceof Player) {} else { sender.sendMessage(translate("console")); return true; }
		if(cmd.getName().equalsIgnoreCase("teleport")){
			((Player) sender).getInventory().addItem(getPortalItem());
			return true;
		}
		return false;
	}
	
/**HILFSFUNKTIONEN**/

	/**
	  * Erstellt eine Location aus einer target-Angabe.
	  */
	public Location getTarget(String targetString, Player player, String world) {
		if (targetString == null || targetString == "") return null;
		if (world == null || world.equals("")) world = player.getWorld().getName();
		
		Location l;
		if (targetString.equalsIgnoreCase("bed")) l = (player.getBedSpawnLocation() == null ? getServer().getWorlds().get(0).getSpawnLocation() : player.getBedSpawnLocation());
		else if (targetString.equalsIgnoreCase("worldspawn")) l = getServer().getWorld(world).getSpawnLocation();
		else if (targetString.equalsIgnoreCase("death")) l = (deathLocation.containsKey(player.getUniqueId().toString()) ? deathLocation.get(player.getUniqueId().toString()) : null);
		else if (targetString.equalsIgnoreCase("up")) {
			l = new Location(player.getWorld(), player.getLocation().getX(), 255, player.getLocation().getZ()); //Start at the very top
			while (player.getWorld().getBlockAt(l).getType() == Material.AIR) l.setY(l.getY() - 1); //While Air, go down
			l.setY(l.getY() + 2); //Use a position a block up.
		} else l = new Location(getServer().getWorld(world), Integer.parseInt(targetString.split(" ")[0]), Integer.parseInt(targetString.split(" ")[1]), Integer.parseInt(targetString.split(" ")[2]));
		
		return l;
	}
	
	/**
	  * Erstellt einen String aus einer target-Angabe.
	  */
	public String getTargetString(String targetString, Player player, String world) {
		Location l = getTarget(targetString, player, world);
		if (l == null) return "???";
		else return l.getWorld().getName() + "; X" + l.getBlockX() + " Y" + l.getBlockY() + " Z" + l.getBlockZ();
	}
	
	/**
	  * Erstellt ein Portal-Item
	  */
	public ItemStack getPortalItem() {
		ItemStack portal = fromConfig(getConfig().getConfigurationSection("config.portalItem"), "", "", config("config.portalItem.durability"));
		
		ItemMeta meta = portal.getItemMeta();
		List<String> lore = meta.getLore();
		
		//Put durability into the lore
		int durLine = lore.size() - 1;
    	String invisibleDur = "";
    	String dur = getConfig().getString("config.portalItem.durability");
    	for (char chr : dur.toCharArray()) invisibleDur += "§" + chr;
    	lore.set(durLine, lore.get(durLine) + "§#" + invisibleDur);

    	//Put random number into the lore -> portals not stackable
		String invisibleID = "";
		String noStack = "" + Math.round(Math.random() * 899999 + 100000);
    	for (char chr : noStack.toCharArray()) invisibleID += "§" + chr;
    	if (durLine - 1 >= 0) lore.set(durLine - 1, lore.get(durLine - 1) + invisibleID);
    	else lore.set(durLine + 1, invisibleID);
    	
    	meta.setLore(lore);
    	
    	portal.setItemMeta(meta);
		
    	if (config("config.portalItem.ench") == "true") return addGlow(portal);
		else return portal;
	}
	
	/**
	 * Zeigt einen Fehler an
	 */
	public void displayError(Player p, String id, String meta) {
		if (config("config.errorDisplay").equals("scoreboard")) {
			Note e = new Note(translate("error_" + id + "_title").replace("{meta}", meta), null, Note.Priority.NORMAL, 5);
			e.add(translate("error_" + id + "_text").replace("{meta}", meta));
			e.show(p);
		} else {
			p.sendMessage(translate("error_" + id + "_title").replace("{meta}", meta));
			p.sendMessage(translate("error_" + id + "_text").replace("{meta}", meta));
		}
	}
	
	/**
	  * F§gt einem Item einen Enchantment-Glow hinzu.
	  * http://hypixel.net/threads/code-glowing-itemstacks.5197/
	  */
	public static ItemStack addGlow(ItemStack item){ 
		net.minecraft.server.v1_8_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = null;
		if (!nmsStack.hasTag()) {
			tag = new NBTTagCompound();
			nmsStack.setTag(tag);
		}
		if (tag == null) tag = nmsStack.getTag();
		NBTTagList ench = new NBTTagList();
		tag.set("ench", ench);
		nmsStack.setTag(tag);
		return CraftItemStack.asCraftMirror(nmsStack);
	}
	
	/**
	  * Erstellt ein Item aus einer ConfigurationSection
	  */
	public ItemStack fromConfig(ConfigurationSection c, String player, String target, String durability) {
		//Create ItemStack with given material
		String[] itemMat = config("item", c).split(":");
		if (itemMat[0].equals("")) itemMat[0] = "FIRE";
    	ItemStack item = new ItemStack(
    			Material.getMaterial(itemMat[0]),
    			1
    	);
		//Set durability if given
    	if (itemMat.length > 1) item.setDurability(Short.parseShort(itemMat[1]));

		//Change name and lore of the item
    	ItemMeta meta = item.getItemMeta();
    	String name = advancedConfig(config("name", c), player, target, durability);
    	meta.setDisplayName(name);
    	String[] lore = advancedConfig(config("lore", c), player, target, durability).split("\\|");
    	meta.setLore(Arrays.asList(lore));
    	item.setItemMeta(meta);
    	
    	if (config("ench", c) == "true") return addGlow(item);
		else return item;
	}
	public ItemStack getItemStack(String item) {
		String[] mat = item.split(":");
		ItemStack stack = new ItemStack(Material.getMaterial(mat[0]));
		if (mat.length > 1) stack.setDurability(Short.parseShort(mat[1]));
		return stack;
	}
	
	/**
	  * Ersetzt die verf§gbaren Variablen in einem Konfigurations-String
	  */
	public static String advancedConfig(String configInput, String player, String target, String durability) {
		return configInput
				.replace("{durability}", durability)
    			.replace("{player}", player)
    			.replace("{target}", target);
	}
	
	/**
	  * Liest Strings aus der Config
	  */
	public String config(String string, String def) {
		String value = getConfig().getString(string, def).replace("&", "§").replace("§§", "&");
		return value;
	}
	public String config(String string) { return config(string, ""); }
	public String config(String string, ConfigurationSection sect, String def) {
		String value = sect.getString(string, def).replace("&", "§").replace("§§", "&");
		return value;
	}
	public String config(String string, ConfigurationSection sect) { return config(string, sect, ""); }
	
	/**
	 * Gibt einen §bersetzten String zur§ck.
	 */
	public String translate(String string) { return config(string, translation, string); }
	
/** EVENT HANDLERS **/
	
	/**
	  * Update death location on player death
	  */
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		deathLocation.put(event.getEntity().getUniqueId().toString(), event.getEntity().getLocation());
	}
	
	/**
	  * If a monster dies, maybe a portal will be dropped
	  */
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntity().getKiller() != null) { //Only if killed by a player

			double dropChance = Math.random() * 100;
			
			//Get all chance settings:
			Map<String,Object> chances = this.getConfig().getConfigurationSection("config.portalItem.drops").getValues(false);
			
			//Iterate through the map
			Iterator<Map.Entry<String,Object>> cIt = chances.entrySet().iterator();
		    while (cIt.hasNext()) {
		        Map.Entry<String,Object> chance = (Map.Entry<String,Object>) cIt.next();
		        
		        //Check if a mob from the list was killed
		        String[] mobs = chance.getKey().split(",");
		        for (int i = 0; i < mobs.length; i++) {
		        	//if entity from list matches killed entity and the chance from the list is smaller than the random drop chance
		        	if (EntityType.valueOf(mobs[i]) == event.getEntityType() && Double.valueOf(chance.getValue().toString()) > dropChance) {
		        		//drop the item
						event.getDrops().add(getPortalItem());
		        		return;
		        	}
		        }
		        
		        cIt.remove(); //verhindert ConcurrentModificationException
		    }
		    
		}
	}

/** DISPLAY MENU **/
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (!event.getPlayer().hasPermission("teleportals")) return;
		if (	(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
				event.getItem() != null &&
				event.getItem().getType() == Material.getMaterial(
						config("config.portalItem.item").split(":")[0]
				) && event.getItem().hasItemMeta() && event.getItem().getItemMeta().getDisplayName().equals(
						config("config.portalItem.name")
				)) {
			
			Menu portal = new Menu(translate("portalTitle"), new TeleportHandler(this));

			//Set player slots
			int minPlayerSlot = Integer.parseInt(config("config.playerSlots").split("-")[0]);
			int maxPlayerSlot = -1;
			if (config("config.playerSlots").split("-").length > 1) maxPlayerSlot = Integer.parseInt(config("config.playerSlots").split("-")[1]);
			else minPlayerSlot = -1;
			
			//Set custom warp slots
			int minWarpSlot = Integer.parseInt(config("config.mywarpSlots").split("-")[0]);
			int maxWarpSlot = -1;
			if (config("config.mywarpSlots").split("-").length > 1) maxWarpSlot = Integer.parseInt(config("config.mywarpSlots").split("-")[1]);
			else minWarpSlot = -1;

        	String dur = "" + event.getItem().getDurability();
        	String pla = event.getPlayer().getDisplayName();

			//Get all public warps:
			Map<String,Object> publicWarps = this.getConfig().getConfigurationSection("warps").getValues(false);
			//Iterate through the map
			Iterator<Map.Entry<String,Object>> wIt = publicWarps.entrySet().iterator();
		    while (wIt.hasNext()) {
		        Map.Entry<String,Object> warp = (Map.Entry<String,Object>) wIt.next();
		        //warp.getKey()
		        //(ConfigurationSection) warp.getValue()
		        
		        int slot = Integer.parseInt(warp.getKey());
		        //   No warps          OR smaller than Min slot or larger than Max slot
		        if ((minWarpSlot == -1 || (slot < minWarpSlot || slot > maxWarpSlot)) &&
		        	(minPlayerSlot == -1 || (slot < minPlayerSlot || slot > maxPlayerSlot))) {
		        	ConfigurationSection c = (ConfigurationSection) warp.getValue();
		        	
		        	String tar = "???";
		        	if (c.getString("target") != null) tar = getTargetString(c.getString("target"), event.getPlayer(), c.getString("world"));
		        	
		        	ItemStack item = fromConfig(c, pla, tar, dur);
		        	String name = advancedConfig(config("name", c), pla, tar, dur);
		        	String[] lore = advancedConfig(config("lore", c), pla, tar, dur).split("\\|");

		        	portal.set(slot, item, name, lore);
		        } else getLogger().log(Level.WARNING, "Slot " + slot + " is already a warp or player slot and has not been overwritten!");
		        
		        wIt.remove(); // avoids a ConcurrentModificationException
		    }
		    
		    //Get all Players
		    int i = 0;
		    int n = 0;
			Object[] players = getServer().getOnlinePlayers().toArray();
		    while (i < players.length && n < (maxPlayerSlot - minPlayerSlot + 1)) {
		    	if (!(players[i] instanceof Player) || players[i] == event.getPlayer()) {
		    		i++;
		    	} else {
			    	ConfigurationSection c = getConfig().getConfigurationSection("config.playerItem");
			    	
			    	String tar = ((Player) players[i]).getDisplayName();
			    	
			    	ItemStack item = fromConfig(c, pla, tar, dur);
		        	String name = advancedConfig(config("name", c), pla, tar, dur);
		        	String[] lore = advancedConfig(config("lore", c), pla, tar, dur).split("\\|");
		        	
		        	int nameLine = lore.length - 1;
		        	String invisibleName = "";
		        	String uuid = ((Player) players[i]).getUniqueId().toString();
		        	for (char chr : uuid.toCharArray()) invisibleName += "§" + chr;
		        	lore[nameLine] = lore[nameLine] + "§#" + invisibleName;
		        	
		        	portal.set(minPlayerSlot + i, item, name, lore);
			    	
			    	i++; n++;
		    	}
		    }

			//Get all custom warps:
			List<String> customWarps = customwarps.getStringList(event.getPlayer().getUniqueId().toString());
			for (int j = 0; j < customWarps.size(); j++) {
		        String[] warp = customWarps.get(j).split(",");
		        
		        int slot = minWarpSlot + j;
	        	ConfigurationSection c = getConfig().getConfigurationSection("config.mywarpItem");
	        	
	        	String tar = getServer().getWorld(warp[1]).getName() + "; X" + warp[2] + " Y" + warp[3] + " Z" + warp[4];
	        	
	        	ItemStack item = fromConfig(c, pla, tar, dur);
	        	String name = advancedConfig(config("name", c), pla, tar, dur);
	        	String[] lore = advancedConfig(config("lore", c), pla, tar, dur).split("\\|");
	        	
	        	String[] mat = getConfig().getStringList("icons").get(Integer.parseInt(warp[0])).split(":");
	    		item.setType(Material.getMaterial(mat[0]));
	    		if (mat.length > 1) item.setDurability(Short.parseShort(mat[1]));

	        	portal.set(slot, item, name, lore);
		    }

        	ConfigurationSection c = getConfig().getConfigurationSection("config.mywarpNewItem");
	        int slot = Integer.parseInt(config("config.mywarpNewSlot"));
        	ItemStack item = fromConfig(c, pla, "", dur);
        	String name = advancedConfig(config("name", c), pla, "", dur);
        	String[] lore = advancedConfig(config("lore", c), pla, "", dur).split("\\|");
			portal.set(slot, item, name, lore);
			
			portal.show(event.getPlayer());
			
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onPlayerItemHeld(PlayerItemHeldEvent event) {
		if (	event.getPlayer().getItemInHand() != null &&
				event.getPlayer().getItemInHand().getType() == Material.getMaterial(
						config("config.portalItem.item").split(":")[0]
				) && event.getPlayer().getItemInHand().hasItemMeta() && event.getPlayer().getItemInHand().getItemMeta().getDisplayName().equals(
						config("config.portalItem.name")
				)) {
			event.getPlayer().setItemInHand(addGlow(event.getPlayer().getItemInHand()));
		}
	}
}
