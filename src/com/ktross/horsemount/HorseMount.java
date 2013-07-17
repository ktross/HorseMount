package com.ktross.horsemount;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class HorseMount extends JavaPlugin implements Listener {
	
	public Map<String, Horse.Variant> mountVariants;
	public Map<String, Horse.Style> mountStyles;
	public Map<String, Horse.Color> mountColors;
	public Map<String, Material> mountArmor;
	public boolean DisableSpawning;
	public boolean DisableItemDrops;
	
	public void onEnable() {
		
		this.saveDefaultConfig();
		
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		
		this.DisableSpawning = ((Boolean) this.getConfig().get("disable-spawning") == true) ? true : false;
		this.DisableItemDrops = ((Boolean) this.getConfig().get("disable-item-drops") == true) ? true : false;
		
		this.mountVariants = new HashMap<String, Horse.Variant>();
		this.mountVariants.put("horse", Horse.Variant.HORSE);
		this.mountVariants.put("mule", Horse.Variant.MULE);
		this.mountVariants.put("donkey", Horse.Variant.DONKEY);
		this.mountVariants.put("skeleton", Horse.Variant.SKELETON_HORSE);
		this.mountVariants.put("zombie", Horse.Variant.UNDEAD_HORSE);
		
		this.mountStyles = new HashMap<String, Horse.Style>();
		this.mountStyles.put("default", Horse.Style.NONE);
		this.mountStyles.put("white", Horse.Style.WHITE);
		this.mountStyles.put("whitefield", Horse.Style.WHITEFIELD);
		this.mountStyles.put("whitedots", Horse.Style.WHITE_DOTS);
		this.mountStyles.put("blackdots", Horse.Style.BLACK_DOTS);
		
		this.mountColors = new HashMap<String, Horse.Color>();
		this.mountColors.put("white", Horse.Color.WHITE);
		this.mountColors.put("creamy", Horse.Color.CREAMY);
		this.mountColors.put("chestnut", Horse.Color.CHESTNUT);
		this.mountColors.put("brown", Horse.Color.BROWN);
		this.mountColors.put("black", Horse.Color.BLACK);
		this.mountColors.put("gray", Horse.Color.GRAY);
		this.mountColors.put("darkbrown", Horse.Color.DARK_BROWN);
		
		this.mountArmor = new HashMap<String, Material>();
		this.mountArmor.put("iron", Material.IRON_BARDING);
		this.mountArmor.put("gold", Material.GOLD_BARDING);
		this.mountArmor.put("diamond", Material.DIAMOND_BARDING);
		
		// Commands
		getCommand("hm").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("horsemount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("mount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("dismount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("setmount").setExecutor(new HorseMountCommandExecutor(this));
		getCommand("setarmor").setExecutor(new HorseMountCommandExecutor(this));
		
		// Plugin Metrics
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
			getLogger().info("Failed to submit stats to MCStats.org");
		}
	}
	
	public void onDisable() {}
	
	public void msgPlayer(CommandSender sender, String message) {
		sender.sendMessage(ChatColor.GOLD+"["+ChatColor.YELLOW+"HorseMount"+ChatColor.GOLD+"]"+ChatColor.GRAY+" "+message);
	}
	
	@EventHandler
	public void onVehicleEnter(VehicleEnterEvent event) {
		Player player = (Player) event.getEntered();
		if (player.getVehicle() instanceof Horse) {
			player.getVehicle().remove();
			msgPlayer(player, "Automatically dismounted due to vehicle change.");
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Horse) {
			boolean eventCancelled = true;
			Player p = (Player) event.getPlayer();
			if (p.hasPermission("horsemount.mount")) {
				eventCancelled = false;
			} else {
				msgPlayer(p, "You do not have permission to mount horses.");
			}
			event.setCancelled(eventCancelled);
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getInventory() instanceof HorseInventory && event.getSlotType() != InventoryType.SlotType.QUICKBAR && event.getSlot() < 9) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onVehicleExit(VehicleExitEvent event) {
		if (event.getVehicle() instanceof Horse) {
			Horse h = (Horse) event.getVehicle();
			h.remove();
		}
	}
	
	@EventHandler
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getEntityType() == EntityType.HORSE && event.getSpawnReason() != SpawnReason.CUSTOM && this.DisableSpawning == true) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.HORSE) {
			Damageable h = (Damageable) event.getEntity();
			if (event.getDamage() >= h.getHealth() && this.DisableItemDrops == true) {
				event.setCancelled(true);
				event.getEntity().remove();
			}
		}
		if (event.getEntityType() == EntityType.PLAYER && event.getEntity().getVehicle() != null) {
			Damageable p = (Damageable) event.getEntity();
			if (event.getDamage() >= p.getHealth()) {
				Horse h = (Horse) event.getEntity().getVehicle();
				h.remove();
			}
		}
	}
}
