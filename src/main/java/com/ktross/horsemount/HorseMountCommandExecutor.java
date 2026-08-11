package com.ktross.horsemount;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;

public final class HorseMountCommandExecutor implements TabExecutor {

    private static final String NO_PERMISSION = "You do not have permission to use this command.";

    private final HorseMount plugin;

    public HorseMountCommandExecutor(HorseMount plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = MountProfile.normalizeKey(command.getName());
        if (name.equals("horsemount") || name.equals("hm")) {
            return handleRootCommand(sender, args);
        }

        if (!(sender instanceof Player player)) {
            plugin.msgPlayer(sender, "This command can only be used by a player.");
            return true;
        }

        return switch (name) {
            case "mount", "mnt" -> handleMount(player);
            case "dismount" -> handleDismount(player);
            case "setmount" -> handleSetMount(player, args);
            case "setarmor" -> handleSetArmor(player, args);
            case "showmount" -> handleShowMount(player);
            case "spawnmount" -> handleSpawnMount(player, args);
            default -> false;
        };
    }

    private boolean handleRootCommand(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("horsemount.reload")) {
                plugin.msgPlayer(sender, NO_PERMISSION);
                return true;
            }
            if (args.length != 1) {
                plugin.msgPlayer(sender, "Usage: /horsemount reload");
                return true;
            }

            List<String> warnings = plugin.reloadPluginConfig();
            if (warnings.isEmpty()) {
                plugin.msgPlayer(sender, "Config reloaded.");
            } else {
                plugin.msgPlayer(sender, "Config reloaded with " + warnings.size()
                        + " warning(s); check the server log.");
            }
            return true;
        }

        if (!sender.hasPermission("horsemount.help")) {
            plugin.msgPlayer(sender, NO_PERMISSION);
            return true;
        }
        plugin.msgPlayer(sender, "HorseMount commands and documentation: "
                + "https://github.com/ktross/horsemount");
        return true;
    }

    private boolean handleMount(Player player) {
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
            return true;
        }
        if (!player.hasPermission("horsemount.mount")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }

        Optional<MountProfile> configured = plugin.profileFor(player.getUniqueId());
        if (configured.isEmpty()) {
            plugin.msgPlayer(player, "Your configured mount is invalid. Use /setmount or check config.yml.");
            return true;
        }

        MountProfile profile = configured.get();
        if (!canUseMount(player, profile)) {
            plugin.msgPlayer(player, "You do not have permission to use this mount.");
            return true;
        }

        MountProfile effectiveProfile = profile;
        if (profile.armor() != MountProfile.Armor.NONE
                && !player.hasPermission("horsemount.armor." + profile.armor().key())) {
            effectiveProfile = profile.withArmor(MountProfile.Armor.NONE);
        }

        try {
            AbstractHorse mount = plugin.spawnMount(player.getLocation(), effectiveProfile, player, false);
            if (!mount.addPassenger(player)) {
                mount.remove();
                plugin.msgPlayer(player, "Unable to mount the summoned horse.");
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Unable to summon a mount for " + player.getName(), exception);
            plugin.msgPlayer(player, "Unable to summon your mount; check the server log.");
        }
        return true;
    }

    private boolean handleDismount(Player player) {
        if (!player.hasPermission("horsemount.dismount")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }
        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        return true;
    }

    private boolean handleSetMount(Player player, String[] args) {
        if (!player.hasPermission("horsemount.setmount")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }
        if (args.length < 1 || args.length > 3) {
            plugin.msgPlayer(player, "Usage: /setmount <variant> [style] [color]");
            return true;
        }

        Optional<MountProfile.Variant> parsedVariant = MountProfile.Variant.parse(args[0]);
        if (parsedVariant.isEmpty()) {
            plugin.msgPlayer(player, "Unable to set mount type: mount not found.");
            return true;
        }

        MountProfile.Variant variant = parsedVariant.get();
        MountProfile.Armor armor = plugin.profileFor(player.getUniqueId())
                .orElse(plugin.defaultProfile())
                .armor();
        MountProfile profile;

        if (variant == MountProfile.Variant.HORSE) {
            MountProfile.Style style = plugin.preferredHorseStyle();
            MountProfile.Color color = plugin.preferredHorseColor();

            if (args.length >= 2) {
                Optional<MountProfile.Style> parsedStyle = MountProfile.Style.parse(args[1]);
                if (parsedStyle.isEmpty() || parsedStyle.get() == MountProfile.Style.NONE) {
                    plugin.msgPlayer(player, "Unable to set mount type: style not found.");
                    return true;
                }
                style = parsedStyle.get();
            }
            if (args.length == 3) {
                Optional<MountProfile.Color> parsedColor = MountProfile.Color.parse(args[2]);
                if (parsedColor.isEmpty() || parsedColor.get() == MountProfile.Color.NONE) {
                    plugin.msgPlayer(player, "Unable to set mount type: color not found.");
                    return true;
                }
                color = parsedColor.get();
            }
            profile = new MountProfile(variant, style, color, armor);
        } else {
            if (args.length != 1) {
                plugin.msgPlayer(player, "Style and color only apply to horse mounts.");
                return true;
            }
            profile = new MountProfile(
                    variant, MountProfile.Style.NONE, MountProfile.Color.NONE, armor);
        }

        if (!canUseMount(player, profile)) {
            plugin.msgPlayer(player, "Unable to set mount type: mount not found or no permission.");
            return true;
        }

        plugin.saveProfile(player.getUniqueId(), profile);
        plugin.msgPlayer(player, "Your default mount has been set. " + profile.description());
        return true;
    }

    private boolean handleSetArmor(Player player, String[] args) {
        if (!player.hasPermission("horsemount.setarmor")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }
        if (args.length != 1) {
            plugin.msgPlayer(player, "Usage: /setarmor <none|iron|gold|diamond>");
            return true;
        }

        Optional<MountProfile.Armor> parsedArmor = MountProfile.Armor.parse(args[0]);
        if (parsedArmor.isEmpty()) {
            plugin.msgPlayer(player, "Unable to set mount armor: armor not found.");
            return true;
        }

        MountProfile.Armor armor = parsedArmor.get();
        if (armor != MountProfile.Armor.NONE
                && !player.hasPermission("horsemount.armor." + armor.key())) {
            plugin.msgPlayer(player, "Unable to set mount armor: armor not found or no permission.");
            return true;
        }

        MountProfile current = plugin.profileFor(player.getUniqueId()).orElse(plugin.defaultProfile());
        plugin.saveProfile(player.getUniqueId(), current.withArmor(armor));
        plugin.msgPlayer(player, "Your default mount armor has been set to: " + armor.key());
        return true;
    }

    private boolean handleShowMount(Player player) {
        if (!player.hasPermission("horsemount.showmount")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }

        Optional<MountProfile> profile = plugin.profileFor(player.getUniqueId());
        if (profile.isEmpty()) {
            plugin.msgPlayer(player, "Your configured mount is invalid. Use /setmount or check config.yml.");
            return true;
        }
        plugin.msgPlayer(player, profile.get().description());
        return true;
    }

    private boolean handleSpawnMount(Player player, String[] args) {
        if (!player.hasPermission("horsemount.spawnmount")) {
            plugin.msgPlayer(player, NO_PERMISSION);
            return true;
        }
        if (args.length < 1 || args.length > 3) {
            plugin.msgPlayer(player, "Usage: /spawnmount <variant> [style] [color]");
            return true;
        }

        Optional<MountProfile.Variant> parsedVariant = MountProfile.Variant.parse(args[0]);
        if (parsedVariant.isEmpty()) {
            plugin.msgPlayer(player, "Unable to spawn mount: mount not found.");
            return true;
        }

        MountProfile.Variant variant = parsedVariant.get();
        MountProfile profile;
        if (variant == MountProfile.Variant.HORSE) {
            MountProfile.Style style = plugin.preferredHorseStyle();
            MountProfile.Color color = plugin.preferredHorseColor();
            if (args.length >= 2) {
                Optional<MountProfile.Style> parsedStyle = MountProfile.Style.parse(args[1]);
                if (parsedStyle.isEmpty() || parsedStyle.get() == MountProfile.Style.NONE) {
                    plugin.msgPlayer(player, "Unable to spawn mount: style not found.");
                    return true;
                }
                style = parsedStyle.get();
            }
            if (args.length == 3) {
                Optional<MountProfile.Color> parsedColor = MountProfile.Color.parse(args[2]);
                if (parsedColor.isEmpty() || parsedColor.get() == MountProfile.Color.NONE) {
                    plugin.msgPlayer(player, "Unable to spawn mount: color not found.");
                    return true;
                }
                color = parsedColor.get();
            }
            profile = new MountProfile(variant, style, color, MountProfile.Armor.NONE);
        } else {
            if (args.length != 1) {
                plugin.msgPlayer(player, "Style and color only apply to horse mounts.");
                return true;
            }
            profile = new MountProfile(
                    variant, MountProfile.Style.NONE, MountProfile.Color.NONE, MountProfile.Armor.NONE);
        }

        try {
            plugin.spawnMount(player.getLocation(), profile, player, true);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create a display mount", exception);
            plugin.msgPlayer(player, "Unable to spawn a display mount; check the server log.");
        }
        return true;
    }

    private static boolean canUseMount(Player player, MountProfile profile) {
        if (!player.hasPermission("horsemount.variant." + profile.variant().key())) {
            return false;
        }
        if (profile.variant() != MountProfile.Variant.HORSE) {
            return true;
        }
        return player.hasPermission("horsemount.style." + profile.style().key())
                && player.hasPermission("horsemount.color." + profile.color().key());
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender, Command command, String alias, String[] args) {
        String name = MountProfile.normalizeKey(command.getName());
        List<String> candidates = new ArrayList<>();

        if ((name.equals("horsemount") || name.equals("hm")) && args.length == 1) {
            candidates.add("reload");
        } else if ((name.equals("setmount") || name.equals("spawnmount")) && args.length == 1) {
            for (MountProfile.Variant variant : MountProfile.Variant.values()) {
                candidates.add(variant.key());
            }
        } else if ((name.equals("setmount") || name.equals("spawnmount"))
                && args.length == 2
                && MountProfile.Variant.parse(args[0]).orElse(null) == MountProfile.Variant.HORSE) {
            for (MountProfile.Style style : MountProfile.Style.values()) {
                if (style != MountProfile.Style.NONE) {
                    candidates.add(style.key());
                }
            }
        } else if ((name.equals("setmount") || name.equals("spawnmount"))
                && args.length == 3
                && MountProfile.Variant.parse(args[0]).orElse(null) == MountProfile.Variant.HORSE) {
            for (MountProfile.Color color : MountProfile.Color.values()) {
                if (color != MountProfile.Color.NONE) {
                    candidates.add(color.key());
                }
            }
        } else if (name.equals("setarmor") && args.length == 1) {
            for (MountProfile.Armor armor : MountProfile.Armor.values()) {
                candidates.add(armor.key());
            }
        }

        String prefix = args.length == 0 ? "" : MountProfile.normalizeKey(args[args.length - 1]);
        return candidates.stream().filter(candidate -> candidate.startsWith(prefix)).toList();
    }
}
