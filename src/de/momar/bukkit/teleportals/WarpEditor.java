package de.momar.bukkit.teleportals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import com.adamantdreamer.ui.menu.Menu;
import com.adamantdreamer.ui.menu.MenuHandler;

public class WarpEditor {
	private List<String> icons;

	//warp[0]: Welt
	//warp[1]: x-Koordinate
	//warp[2]: y-Koordinate
	//warp[3]: z-Koordinate
	
	public WarpEditor(Teleportals plugin, Player player, String[] warp, boolean edit, int oldItem) {
		icons = plugin.getConfig().getStringList("icons");

		String name = (edit ? plugin.translate("changeIcon") : plugin.translate("selectIcon"));
		
		Menu menu = new Menu(plugin.translate("iconMenu"), new ClickHandler(plugin, player, warp, edit, oldItem));
		for (int i = 0; i < icons.size(); i++) {
			menu.add(plugin.getItemStack(icons.get(i)), name);
		}

		if (edit) {
			menu.set(29, new ItemStack(Material.COMPASS), plugin.translate("changeTarget"));
			menu.set(33, new ItemStack(Material.FIRE), plugin.translate("deleteWarp"));
		}
		
		menu.show(player);
	}
	
	public class ClickHandler implements MenuHandler {
		private Teleportals plugin;
		private Player player;
		private String[] warp;
		private boolean edit;
		private int oldItem;
		
		public ClickHandler(Teleportals plugin, Player player, String[] warp, boolean edit, int oldItem) {
			this.plugin = plugin;
			this.player = player;
			this.warp = warp;
			this.edit = edit;
			this.oldItem = oldItem;
		}
		
		@Override
		public Event.Result onClick(InventoryClickEvent event) {
			String cost = plugin.getConfig().getString("config.mywarpNewCost");
			if (!edit && !TeleportHandler.pay_test((Player) event.getWhoClicked(), cost)) {
				plugin.displayError((Player) event.getWhoClicked(), "cost", cost);
				event.getView().close();
				return Event.Result.DENY;
			}
			//Es folgt: ein Riesenhaufen Magie! ;)
			List<String> l = plugin.customwarps.getStringList(player.getUniqueId().toString());
			if (event.getSlot() < 27) {
				if (edit) { //Icon bearbeiten
					int index = plugin.customwarps.getStringList(player.getUniqueId().toString()).indexOf(warp[0] + "," + warp[1] + "," + warp[2] + "," + warp[3] + "," + warp[4]);
					l.add(index, event.getSlot() + "," + warp[1] + "," + warp[2] + "," + warp[3] + "," + warp[4]);
					l.remove(index + 1);
				} else { //Warp erstellen
					l.add(event.getSlot() + "," +
							player.getWorld().getName() + "," + 
							((int) Math.floor(player.getLocation().getX())) + "," +
							((int) Math.floor(player.getLocation().getY())) + "," +
							((int) Math.floor(player.getLocation().getZ())));
				}
			} else if (event.getSlot() == 29) { //Ändern
				int index = plugin.customwarps.getStringList(player.getUniqueId().toString()).indexOf(warp[0] + "," + warp[1] + "," + warp[2] + "," + warp[3] + "," + warp[4]);
				l.add(index, oldItem + "," +
						player.getWorld().getName() + "," +
						((int) Math.floor(player.getLocation().getX())) + "," +
						((int) Math.floor(player.getLocation().getY())) + "," +
						((int) Math.floor(player.getLocation().getZ())));
				l.remove(index + 1);
			} else if (event.getSlot() == 33) { //Löschen
				int index = plugin.customwarps.getStringList(player.getUniqueId().toString()).indexOf(warp[0] + "," + warp[1] + "," + warp[2] + "," + warp[3] + "," + warp[4]);
				if (index > -1) l.remove(index);
			}
			plugin.customwarps.set(player.getUniqueId().toString(), l); //Liste zurückschreiben
			if (!edit) TeleportHandler.pay((Player) event.getWhoClicked(), cost);
			
			try {
				plugin.customwarps.save(plugin.getDataFolder().getCanonicalPath() + File.separator + "customWarps.yml");
			} catch (IOException e) { e.printStackTrace(); }
			event.getView().close();
			return Event.Result.DENY;
		}

		@Override
		public void onClose(InventoryCloseEvent event) {}
	}
	
}
