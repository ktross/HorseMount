package com.ktross.horsemount;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

public class HorseMountCommandExecutor implements CommandExecutor {
	private HorseMount plugin;
	 
	public HorseMountCommandExecutor(HorseMount plugin) {
		this.plugin = plugin;
	}
 
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		
		if (!(sender instanceof Player)) {
			plugin.getLogger().info("This command cannot be executed via console.");
			return false;
		}
		Player player = (Player) sender;
		
		if (cmd.getName().equalsIgnoreCase("hm") || cmd.getName().equalsIgnoreCase("horsemount")) {
			if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
				if (player.hasPermission("horsemount.reload")) {
					plugin.reloadConfig();
					plugin.msgPlayer(sender, "Config reloaded.");
					return true;
				} else {
					plugin.msgPlayer(sender, "You do not have permission to use this command.");
					return false;
				}
			}
			if (!player.hasPermission("horsemount.help")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			plugin.msgPlayer(sender, "Welcome to HorseMount! Please refer to http://dev.bukkit.org/bukkit-plugins/horsemount/ for a list of commands and documentation.");
			return true;
		}
		
		if ((cmd.getName().equalsIgnoreCase("mount") || cmd.getName().equalsIgnoreCase("mnt"))) {
			
			if (player.isInsideVehicle()) {
				Entity horse = (Entity) player.getVehicle();
				horse.eject();
				return true;
			}
			
			if (!player.hasPermission("horsemount.mount")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			
            String HorseVariant;
            String HorseStyle;
            String HorseColor;
            String HorseArmor;
            World world = player.getWorld();
            Location TargetLocation = player.getLocation();
            
            if (plugin.getConfig().get("players."+player.getUniqueId()) != null) {
            	HorseVariant = (String) plugin.getConfig().get("players."+player.getUniqueId()+".variant");
				HorseStyle = (String) plugin.getConfig().get("players."+player.getUniqueId()+".style");
				HorseColor = (String) plugin.getConfig().get("players."+player.getUniqueId()+".color");
				HorseArmor = (String) plugin.getConfig().get("players."+player.getUniqueId()+".armor");
            } else {
            	HorseVariant = (String) plugin.getConfig().get("players.default.variant"); 
				HorseStyle = (String) plugin.getConfig().get("players.default.style");
				HorseColor = (String) plugin.getConfig().get("players.default.color");
				HorseArmor = (String) plugin.getConfig().get("players.default.armor");
            }
			
			if (plugin.mountVariants.get(HorseVariant) == null || plugin.mountStyles.get(HorseStyle) == null || plugin.mountColors.get(HorseColor) == null) {
				if (!HorseStyle.equalsIgnoreCase("none") && !HorseColor.equalsIgnoreCase("none")) {
					plugin.msgPlayer(sender, "Mount does not exist. Please check your config values.");
					return false;
				}
			}
			if (HorseStyle.equalsIgnoreCase("none") || HorseColor.equalsIgnoreCase("none")) {
				if (!player.hasPermission("horsemount.variant."+HorseVariant)) {
					plugin.msgPlayer(sender, "You do not have permission to use this mount.");
					return false;
				}
			} else {
				if (!player.hasPermission("horsemount.variant."+HorseVariant) || !player.hasPermission("horsemount.style."+HorseStyle) || !player.hasPermission("horsemount.color."+HorseColor)) {
					plugin.msgPlayer(sender, "You do not have permission to use this mount.");
					return false;
				}
			}
			
			Horse horse = (Horse) world.spawnEntity(TargetLocation, EntityType.HORSE);
			HorseInventory inv = horse.getInventory();
			
			horse.setVariant(plugin.mountVariants.get(HorseVariant));
			
			if (HorseVariant.equalsIgnoreCase("horse")) {
				horse.setStyle(plugin.mountStyles.get(HorseStyle));
			}
			if (HorseVariant.equalsIgnoreCase("horse")) {
				horse.setColor(plugin.mountColors.get(HorseColor));
			}
			
			inv.setSaddle(new ItemStack(Material.SADDLE, 1));
			
			if (HorseVariant.equalsIgnoreCase("horse") && HorseArmor.equalsIgnoreCase("none") == false && player.hasPermission("horsemount.armor."+HorseArmor)) {
				inv.setArmor(new ItemStack(plugin.mountArmor.get(HorseArmor), 1));
			}
			
			horse.setOwner(player);
            horse.setPassenger(player);
            return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("dismount")) {
			if (!player.hasPermission("horsemount.dismount")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			if (player.getVehicle() != null) {
				Entity vehicle = (Entity) player.getVehicle();
				vehicle.eject();
			}
			return true;
			
		}
		
		if (cmd.getName().equalsIgnoreCase("setmount")) {
			if (!player.hasPermission("horsemount.setmount")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			if (args.length == 0 || args.length > 3) {
				plugin.msgPlayer(sender, "Invalid command arguments.");
				return false;
			}
			if (args[0].equalsIgnoreCase("horse")) {
				if (plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".armor") == null) {
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".armor", "none");
				}
				if (args.length == 1 && player.hasPermission("horsemount.variant."+args[0])) {
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".variant", args[0]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".style", plugin.getConfig().get("players.default.style"));
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".color", plugin.getConfig().get("players.default.color"));
					plugin.saveConfig();
					plugin.msgPlayer(sender, "Your default mount has been set. Variant: "+args[0]+", Style: "+plugin.getConfig().get("players.default.style")+", Color: "+plugin.getConfig().get("players.default.color"));
					return true;
				}
				if (args.length == 2 && plugin.mountStyles.get(args[1]) != null && player.hasPermission("horsemount.variant."+args[0]) && player.hasPermission("horsemount.style."+args[1])) {
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".variant", args[0]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".style", args[1]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".color", plugin.getConfig().get("players.default.color"));
					plugin.saveConfig();
					plugin.msgPlayer(sender, "Your default mount has been set. Variant: "+args[0]+", Style: "+args[1]+", Color: "+plugin.getConfig().get("players.default.color"));
					return true;
				}
				if (args.length == 3 && plugin.mountStyles.get(args[1]) != null && plugin.mountColors.get(args[2]) != null && player.hasPermission("horsemount.variant."+args[0]) && player.hasPermission("horsemount.style."+args[1]) && player.hasPermission("horsemount.color."+args[2])) {
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".variant", args[0]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".style", args[1]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".color", args[2]);
					plugin.saveConfig();
					plugin.msgPlayer(sender, "Your default mount has been set. Variant: "+args[0]+", Style: "+args[1]+", Color: "+args[2]);
					return true;
				}
			} else {
				if (args.length == 1 && plugin.mountVariants.get(args[0]) != null && player.hasPermission("horsemount.variant."+args[0])) {
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".variant", args[0]);
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".style", "none");
					plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".color", "none");
					plugin.saveConfig();
					plugin.msgPlayer(sender, "Your default mount has been set. Variant: "+args[0]);
					return true;
				}
			}
			plugin.msgPlayer(sender, "Unable to set mount type: mount not found or no permission.");
			return false;
		}
		
		if (cmd.getName().equalsIgnoreCase("setarmor")) {
			if (!player.hasPermission("horsemount.setarmor")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			if (plugin.getConfig().get("players."+player.getUniqueId()+".variant") == null || plugin.getConfig().get("players."+player.getUniqueId()+".style") == null || plugin.getConfig().get("players."+player.getUniqueId()+".color") == null) {
				plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".variant", plugin.getConfig().get("players.default.variant"));
				plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".style", plugin.getConfig().get("players.default.style"));
				plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".color", plugin.getConfig().get("players.default.color"));
			}
			if (args.length == 1 && args[0].equalsIgnoreCase("none")) {
				plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".armor", args[0]);
				plugin.saveConfig();
				plugin.msgPlayer(sender, "Your default mount armor has been set to: "+args[0]);
				return true;
			}
			if (args.length == 1 && plugin.mountArmor.get(args[0]) != null && player.hasPermission("horsemount.armor."+args[0])) {
				plugin.getConfig().set("players."+((Player) sender).getUniqueId()+".armor", args[0]);
				plugin.saveConfig();
				plugin.msgPlayer(sender, "Your default mount armor has been set to: "+args[0]);
				return true;
			}
			plugin.msgPlayer(sender, "Unable to set mount armor: armor not found or no permission.");
			return false;
		}
		
		if (cmd.getName().equalsIgnoreCase("showmount")) {
			if (!player.hasPermission("horsemount.showmount")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			
			String mountVariant = (String) ((plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".variant") != null) ? plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".variant") : plugin.getConfig().get("players.default.variant"));
			String mountStyle = (String) ((plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".style") != null) ? plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".style") : plugin.getConfig().get("players.default.style"));
			String mountColor = (String) ((plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".color") != null) ? plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".color") : plugin.getConfig().get("players.default.color"));
			String mountArmor = (String) ((plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".armor") != null) ? plugin.getConfig().get("players."+((Player) sender).getUniqueId()+".armor") : plugin.getConfig().get("players.default.armor"));
			
			plugin.msgPlayer(sender, "Variant: "+mountVariant+", Style: "+mountStyle+", Color: "+mountColor+", Armor: "+mountArmor);
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("spawnmount")) {
			if (!player.hasPermission("horsemount.spawnmount")) {
				plugin.msgPlayer(sender, "You do not have permission to use this command.");
				return false;
			}
			if (args.length == 0 || args.length > 3) {
				plugin.msgPlayer(sender, "Invalid command arguments.");
				return false;
			}
			
			if (plugin.mountVariants.get(args[0]) != null) {
				Horse horse = (Horse) player.getWorld().spawnEntity(player.getLocation(), EntityType.HORSE);
				HorseInventory inv = horse.getInventory();
				horse.setVariant(plugin.mountVariants.get(args[0]));
				if (args.length >= 2 && plugin.mountStyles.get(args[1]) != null) {
					horse.setStyle(plugin.mountStyles.get(args[1]));
				}
				if (args.length == 3 && plugin.mountColors.get(args[2]) != null) {
					horse.setColor(plugin.mountColors.get(args[2]));
				}
				inv.setSaddle(new ItemStack(Material.SADDLE, 1));
				horse.setOwner(player);
				((LivingEntity) horse).setCustomName("[HM] Display");
				return true;
			}
			
			plugin.msgPlayer(sender, "Unable to spawn mount: mount not found.");
			return false;
		}
		
		//No command matched
		return false;
	}
}
