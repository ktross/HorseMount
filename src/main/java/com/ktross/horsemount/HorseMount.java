package com.ktross.horsemount;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class HorseMount extends JavaPlugin implements Listener {

    private static final String MESSAGE_PREFIX = ChatColor.GOLD + "[" + ChatColor.YELLOW
            + "HorseMount" + ChatColor.GOLD + "] ";
    private static final String DISPLAY_NAME = ChatColor.AQUA + "[HM] Display";

    private NamespacedKey managedMountKey;
    private NamespacedKey displayMountKey;
    private NamespacedKey mountOwnerKey;
    private boolean disableSpawning;
    private boolean disableItemDrops;
    private MountProfile defaultProfile = MountProfile.defaultProfile();

    @Override
    public void onEnable() {
        managedMountKey = new NamespacedKey(this, "managed_mount");
        displayMountKey = new NamespacedKey(this, "display_mount");
        mountOwnerKey = new NamespacedKey(this, "mount_owner");

        saveDefaultConfig();
        logConfigurationWarnings(loadConfiguration());

        getServer().getPluginManager().registerEvents(this, this);

        HorseMountCommandExecutor executor = new HorseMountCommandExecutor(this);
        registerCommand("horsemount", executor);
        registerCommand("mount", executor);
        registerCommand("dismount", executor);
        registerCommand("setmount", executor);
        registerCommand("setarmor", executor);
        registerCommand("showmount", executor);
        registerCommand("spawnmount", executor);
    }

    @Override
    public void onDisable() {
        // Summoned riding mounts are intentionally transient. Display mounts remain in the world.
        for (World world : getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof AbstractHorse mount
                        && isManagedMount(mount)
                        && !isDisplayMount(mount)) {
                    mount.remove();
                }
            }
        }
    }

    public void msgPlayer(CommandSender sender, String message) {
        sender.sendMessage(MESSAGE_PREFIX + ChatColor.GRAY + message);
    }

    List<String> reloadPluginConfig() {
        reloadConfig();
        List<String> warnings = loadConfiguration();
        logConfigurationWarnings(warnings);
        return warnings;
    }

    MountProfile defaultProfile() {
        return defaultProfile;
    }

    Optional<MountProfile> profileFor(UUID playerId) {
        String basePath = "players." + playerId;
        if (!getConfig().isConfigurationSection(basePath)) {
            return Optional.of(defaultProfile);
        }

        return profileAt(basePath);
    }

    private Optional<MountProfile> profileAt(String basePath) {
        String variantValue = getConfig().getString(basePath + ".variant", defaultProfile.variant().key());
        Optional<MountProfile.Variant> variant = MountProfile.Variant.parse(variantValue);
        if (variant.isEmpty()) {
            return Optional.empty();
        }

        boolean horse = variant.get() == MountProfile.Variant.HORSE;
        String defaultStyle = horse ? preferredHorseStyle().key() : MountProfile.Style.NONE.key();
        String defaultColor = horse ? preferredHorseColor().key() : MountProfile.Color.NONE.key();

        return MountProfile.parse(
                variantValue,
                getConfig().getString(basePath + ".style", defaultStyle),
                getConfig().getString(basePath + ".color", defaultColor),
                getConfig().getString(basePath + ".armor", defaultProfile.armor().key()));
    }

    void saveProfile(UUID playerId, MountProfile profile) {
        String basePath = "players." + playerId;
        getConfig().set(basePath + ".variant", profile.variant().key());
        getConfig().set(basePath + ".style", profile.style().key());
        getConfig().set(basePath + ".color", profile.color().key());
        getConfig().set(basePath + ".armor", profile.armor().key());
        saveConfig();
    }

    MountProfile.Style preferredHorseStyle() {
        return defaultProfile.variant() == MountProfile.Variant.HORSE
                ? defaultProfile.style()
                : MountProfile.Style.DEFAULT;
    }

    MountProfile.Color preferredHorseColor() {
        return defaultProfile.variant() == MountProfile.Variant.HORSE
                ? defaultProfile.color()
                : MountProfile.Color.WHITE;
    }

    AbstractHorse spawnMount(Location location, MountProfile profile, Player owner, boolean display) {
        Entity spawned = location.getWorld().spawnEntity(location, entityType(profile.variant()));
        if (!(spawned instanceof AbstractHorse mount)) {
            spawned.remove();
            throw new IllegalStateException("Configured mount entity is not an AbstractHorse");
        }

        markManagedMount(mount, display, owner.getUniqueId());
        mount.setTamed(true);
        mount.setOwner(owner);
        mount.setRemoveWhenFarAway(false);
        mount.setPersistent(display);
        mount.getInventory().setSaddle(new ItemStack(Material.SADDLE));

        if (mount instanceof Horse horse) {
            horse.setStyle(horseStyle(profile.style()));
            horse.setColor(horseColor(profile.color()));
            Material armor = armorMaterial(profile.armor());
            if (armor != null) {
                horse.getInventory().setArmor(new ItemStack(armor));
            }
        }

        if (display) {
            mount.setCustomName(DISPLAY_NAME);
            mount.setCustomNameVisible(true);
            mount.setInvulnerable(true);
        }

        return mount;
    }

    boolean isManagedMount(Entity entity) {
        return entity.getPersistentDataContainer().has(managedMountKey, PersistentDataType.BYTE);
    }

    boolean isDisplayMount(Entity entity) {
        return entity.getPersistentDataContainer().has(displayMountKey, PersistentDataType.BYTE);
    }

    private void markManagedMount(AbstractHorse mount, boolean display, UUID ownerId) {
        mount.getPersistentDataContainer().set(managedMountKey, PersistentDataType.BYTE, (byte) 1);
        mount.getPersistentDataContainer()
                .set(mountOwnerKey, PersistentDataType.STRING, ownerId.toString());
        if (display) {
            mount.getPersistentDataContainer().set(displayMountKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private boolean isMountOwner(AbstractHorse mount, Player player) {
        String ownerId = mount.getPersistentDataContainer()
                .get(mountOwnerKey, PersistentDataType.STRING);
        if (ownerId != null) {
            return ownerId.equals(player.getUniqueId().toString());
        }

        // Fallback for any managed entities created by an early development build.
        AnimalTamer owner = mount.getOwner();
        return owner != null && player.getUniqueId().equals(owner.getUniqueId());
    }

    private void registerCommand(String name, HorseMountCommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command is missing from plugin.yml: " + name);
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private List<String> loadConfiguration() {
        List<String> warnings = new ArrayList<>();
        FileConfiguration config = getConfig();

        disableSpawning = readBoolean(config, "disable-spawning", true, warnings);
        disableItemDrops = readBoolean(config, "disable-item-drops", true, warnings);

        MountProfile parsedDefault = MountProfile.parse(
                        config.getString("players.default.variant", "horse"),
                        config.getString("players.default.style", "default"),
                        config.getString("players.default.color", "white"),
                        config.getString("players.default.armor", "none"))
                .orElse(null);
        if (parsedDefault == null) {
            defaultProfile = MountProfile.defaultProfile();
            warnings.add("players.default is invalid; using horse/default/white/none");
        } else {
            defaultProfile = parsedDefault;
        }

        ConfigurationSection players = config.getConfigurationSection("players");
        if (players != null) {
            for (String playerKey : players.getKeys(false)) {
                if (playerKey.equals("default")) {
                    continue;
                }
                String playerPath = "players." + playerKey;
                if (!config.isConfigurationSection(playerPath)) {
                    warnings.add(playerPath + " must be a configuration section");
                } else if (profileAt(playerPath).isEmpty()) {
                    warnings.add(playerPath + " contains an invalid mount profile");
                }
            }
        }

        return List.copyOf(warnings);
    }

    private static boolean readBoolean(
            FileConfiguration config, String path, boolean fallback, List<String> warnings) {
        if (!config.contains(path)) {
            warnings.add(path + " is missing; using " + fallback);
            return fallback;
        }
        if (!config.isBoolean(path)) {
            warnings.add(path + " must be true or false; using " + fallback);
            return fallback;
        }
        return config.getBoolean(path);
    }

    private void logConfigurationWarnings(List<String> warnings) {
        for (String warning : warnings) {
            getLogger().warning(warning);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof AbstractHorse target) || !isManagedMount(target)) {
            return;
        }

        if (isDisplayMount(target)) {
            event.setCancelled(true);
            if (event.getEntered() instanceof Player player) {
                msgPlayer(player, "Display mounts cannot be ridden.");
            }
            return;
        }

        if (!(event.getEntered() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        if (!isMountOwner(target, player)) {
            event.setCancelled(true);
            msgPlayer(player, "This mount belongs to another player.");
            return;
        }

        if (!player.hasPermission("horsemount.mount")) {
            event.setCancelled(true);
            msgPlayer(player, "You do not have permission to mount this horse.");
            return;
        }

        Entity currentVehicle = player.getVehicle();
        if (currentVehicle instanceof AbstractHorse currentMount
                && currentMount != target
                && isManagedMount(currentMount)) {
            currentMount.remove();
            msgPlayer(player, "Automatically dismissed your previous mount.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof AbstractHorse mount) || !isManagedMount(mount)) {
            return;
        }

        Player player = event.getPlayer();
        if (isDisplayMount(mount)) {
            event.setCancelled(true);
            msgPlayer(player, "Display mounts cannot be ridden.");
            return;
        }

        if (!isMountOwner(mount, player)) {
            event.setCancelled(true);
            msgPlayer(player, "This mount belongs to another player.");
            return;
        }

        if (mount.isEmpty() && !player.hasPermission("horsemount.mount")) {
            event.setCancelled(true);
            msgPlayer(player, "You do not have permission to mount this horse.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (isManagedHorseInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isManagedHorseInventory(top)) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize())) {
            event.setCancelled(true);
        }
    }

    private boolean isManagedHorseInventory(Inventory inventory) {
        return inventory.getHolder() instanceof AbstractHorse mount && isManagedMount(mount);
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof AbstractHorse mount && isManagedMount(mount)) {
            mount.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Entity vehicle = event.getPlayer().getVehicle();
        if (vehicle instanceof AbstractHorse mount && isManagedMount(mount)) {
            mount.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (disableSpawning
                && isSupportedMountType(event.getEntityType())
                && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof AbstractHorse mount
                && isManagedMount(mount)
                && isDisplayMount(mount)) {
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Player player
                && event.getFinalDamage() >= player.getHealth()
                && player.getVehicle() instanceof AbstractHorse mount
                && isManagedMount(mount)) {
            mount.remove();
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (disableItemDrops
                && event.getEntity() instanceof AbstractHorse mount
                && isManagedMount(mount)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        String header = plainLine(event.getLine(0));
        if (!(header.equalsIgnoreCase("[HM]") || header.equalsIgnoreCase("[HorseMount]"))) {
            return;
        }
        if (!event.getPlayer().hasPermission("horsemount.signs.create")) {
            return;
        }

        String secondLine = MountProfile.normalizeKey(plainLine(event.getLine(1)));
        Optional<MountProfile.Armor> armor = MountProfile.Armor.parse(secondLine)
                .filter(value -> value != MountProfile.Armor.NONE);
        if (armor.isPresent()) {
            formatSignHeader(event);
            event.setLine(1, capitalize(armor.get().key()));
            event.setLine(2, "");
            event.setLine(3, "");
            return;
        }

        Optional<MountProfile.Variant> variant = MountProfile.Variant.parse(secondLine);
        if (variant.isEmpty()) {
            formatInvalidSign(event);
            return;
        }

        if (variant.get() == MountProfile.Variant.HORSE) {
            Optional<MountProfile.Style> style = MountProfile.Style.parse(plainLine(event.getLine(2)))
                    .filter(value -> value != MountProfile.Style.NONE);
            Optional<MountProfile.Color> color = MountProfile.Color.parse(plainLine(event.getLine(3)))
                    .filter(value -> value != MountProfile.Color.NONE);
            if (style.isEmpty() || color.isEmpty()) {
                formatInvalidSign(event);
                return;
            }
            formatSignHeader(event);
            event.setLine(1, capitalize(variant.get().key()));
            event.setLine(2, capitalize(style.get().key()));
            event.setLine(3, capitalize(color.get().key()));
            return;
        }

        formatSignHeader(event);
        event.setLine(1, capitalize(variant.get().key()));
        event.setLine(2, "");
        event.setLine(3, "");
    }

    @EventHandler(ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Sign sign)) {
            return;
        }

        Player player = event.getPlayer();
        SignSide side = sign.getTargetSide(player);
        if (!plainLine(side.getLine(0)).equalsIgnoreCase("[HorseMount]")
                || !player.hasPermission("horsemount.signs.use")) {
            return;
        }

        String secondLine = MountProfile.normalizeKey(plainLine(side.getLine(1)));
        Optional<MountProfile.Armor> armor = MountProfile.Armor.parse(secondLine)
                .filter(value -> value != MountProfile.Armor.NONE);
        if (armor.isPresent()) {
            executeCommand(player, "setarmor", armor.get().key());
            return;
        }

        Optional<MountProfile.Variant> variant = MountProfile.Variant.parse(secondLine);
        if (variant.isEmpty()) {
            return;
        }
        if (variant.get() == MountProfile.Variant.HORSE) {
            executeCommand(
                    player,
                    "setmount",
                    variant.get().key(),
                    MountProfile.normalizeKey(plainLine(side.getLine(2))),
                    MountProfile.normalizeKey(plainLine(side.getLine(3))));
        } else {
            executeCommand(player, "setmount", variant.get().key());
        }
    }

    private void executeCommand(Player player, String name, String... arguments) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command is missing from plugin.yml: " + name);
            return;
        }
        command.execute(player, name, arguments);
    }

    private static String plainLine(String line) {
        return MountProfile.normalizeKey(ChatColor.stripColor(line));
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static void formatSignHeader(SignChangeEvent event) {
        event.setLine(0, ChatColor.AQUA + "[HorseMount]");
    }

    private static void formatInvalidSign(SignChangeEvent event) {
        event.setLine(0, ChatColor.RED + "Error:");
        event.setLine(1, "Invalid");
        event.setLine(2, "Parameters");
        event.setLine(3, "");
    }

    private static boolean isSupportedMountType(EntityType type) {
        return type == EntityType.HORSE
                || type == EntityType.DONKEY
                || type == EntityType.MULE
                || type == EntityType.SKELETON_HORSE
                || type == EntityType.ZOMBIE_HORSE;
    }

    private static EntityType entityType(MountProfile.Variant variant) {
        return switch (variant) {
            case HORSE -> EntityType.HORSE;
            case DONKEY -> EntityType.DONKEY;
            case MULE -> EntityType.MULE;
            case SKELETON -> EntityType.SKELETON_HORSE;
            case ZOMBIE -> EntityType.ZOMBIE_HORSE;
        };
    }

    private static Horse.Style horseStyle(MountProfile.Style style) {
        return switch (style) {
            case DEFAULT -> Horse.Style.NONE;
            case WHITE -> Horse.Style.WHITE;
            case WHITEFIELD -> Horse.Style.WHITEFIELD;
            case WHITEDOTS -> Horse.Style.WHITE_DOTS;
            case BLACKDOTS -> Horse.Style.BLACK_DOTS;
            case NONE -> throw new IllegalArgumentException("Horse style cannot be none");
        };
    }

    private static Horse.Color horseColor(MountProfile.Color color) {
        return switch (color) {
            case WHITE -> Horse.Color.WHITE;
            case CREAMY -> Horse.Color.CREAMY;
            case CHESTNUT -> Horse.Color.CHESTNUT;
            case BROWN -> Horse.Color.BROWN;
            case BLACK -> Horse.Color.BLACK;
            case GRAY -> Horse.Color.GRAY;
            case DARKBROWN -> Horse.Color.DARK_BROWN;
            case NONE -> throw new IllegalArgumentException("Horse color cannot be none");
        };
    }

    private static Material armorMaterial(MountProfile.Armor armor) {
        return switch (armor) {
            case NONE -> null;
            case IRON -> Material.IRON_HORSE_ARMOR;
            case GOLD -> Material.GOLDEN_HORSE_ARMOR;
            case DIAMOND -> Material.DIAMOND_HORSE_ARMOR;
        };
    }
}
