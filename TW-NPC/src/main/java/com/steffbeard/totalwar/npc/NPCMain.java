package com.steffbeard.totalwar.npc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.steffbeard.totalwar.npc.cargo.CargoTrait;
import com.steffbeard.totalwar.npc.cargo.LoadTask;
import com.steffbeard.totalwar.npc.cargo.ProcessingTask;
import com.steffbeard.totalwar.npc.cargo.UnloadTask;
import com.steffbeard.totalwar.npc.utils.CargoUtils;

import net.citizensnpcs.api.npc.NPC;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.milkbowl.vault.economy.Economy;
import nl.thewgbbroz.dtltraders.Main;
import nl.thewgbbroz.dtltraders.citizens.TraderTrait;
import nl.thewgbbroz.dtltraders.guis.AGUI;
import nl.thewgbbroz.dtltraders.guis.tradegui.TradeGUI;
import nl.thewgbbroz.dtltraders.guis.tradegui.TradeGUIPage;
import nl.thewgbbroz.dtltraders.guis.tradegui.items.AGUIItem;
import nl.thewgbbroz.dtltraders.guis.tradegui.items.TradableGUIItem;

public class NPCMain extends JavaPlugin implements Listener {

	public static final String ERROR_TAG = ChatColor.RED + "Error: " + ChatColor.DARK_RED;
	public static final String SUCCESS_TAG = ChatColor.DARK_AQUA + "Cargo: " + ChatColor.WHITE;
	public static Logger logger;
	private static Economy economy;
	private static ArrayList<Player> playersInQue;
	private static double unloadTax, loadTax;
	private static NPCMain instance;
	private static int delay;// ticks
	private static Material SIGN_POST = Material.getMaterial("SIGN_POST");
	private static Main dtlTradersPlugin;
	public Config config;
	private CraftManager craftManager;
	private boolean cardinalDistance;
	private static boolean debug;
	private double scanRange;

	public void onEnable() {
		logger = this.getLogger();
		this.getServer().getPluginManager().registerEvents(this, this);
		playersInQue = new ArrayList<Player>();
		instance = this;

		// ************************
		// * Configs *
		// ************************
		final File dataFolder = this.getDataFolder();
		this.config = new Config(dataFolder);
		try {
			this.config.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.saveDefaultConfig();

		// ************************
		// * Load Movecraft *
		// ************************
		if (getServer().getPluginManager().getPlugin("Movecraft") == null
				|| !getServer().getPluginManager().getPlugin("Movecraft").isEnabled()) {
			logger.log(Level.SEVERE, "Movecraft not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		craftManager = CraftManager.getInstance();
		// ************************
		// * Load Citizens *
		// ************************
		if (getServer().getPluginManager().getPlugin("Citizens") == null
				|| !getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
			logger.log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		if (net.citizensnpcs.api.CitizensAPI.getTraitFactory().getTrait(CargoTrait.class) == null)
			net.citizensnpcs.api.CitizensAPI.getTraitFactory()
					.registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(CargoTrait.class));
		// ************************
		// * Load Vault *
		// ************************
		if (getServer().getPluginManager().getPlugin("Vault") == null
				|| !getServer().getPluginManager().getPlugin("Vault").isEnabled()) {
			logger.log(Level.SEVERE, "Vault not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		Plugin traders = getServer().getPluginManager().getPlugin("dtlTraders");
		if (traders == null || !(traders instanceof Main)) {
			getServer().getPluginManager().disablePlugin(this);
		}
		dtlTradersPlugin = (Main) traders;
		economy = getServer().getServicesManager().getRegistration(Economy.class).getProvider();
	}

	public void onDisable() {
		logger = null;
		economy = null;
		instance = null;
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) { // Plugin
		if (command.getName().equalsIgnoreCase("unload")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
				return true;
			}
			unload((Player) sender);
			return true;
		}

		if (command.getName().equalsIgnoreCase("load")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ERROR_TAG + "You need to be a player to execute that command!");
				return true;
			}
			load((Player) sender);
			return true;
		}

		if (command.getName().equalsIgnoreCase("cargo")) {
			if (!sender.hasPermission("Cargo.cargo")) {
				sender.sendMessage(ERROR_TAG + "You don't have permission to do that!");
				return true;
			}
			sender.sendMessage(
					ChatColor.WHITE + "--[ " + ChatColor.DARK_AQUA + "  Movecraft Cargo " + ChatColor.WHITE + " ]--");
			sender.sendMessage(ChatColor.DARK_AQUA + "Scan Range: " + ChatColor.WHITE + scanRange + " Blocks");
			sender.sendMessage(ChatColor.DARK_AQUA + "Transfer Delay: " + ChatColor.WHITE + delay + " ticks");
			sender.sendMessage(ChatColor.DARK_AQUA + "Unload Tax: " + ChatColor.WHITE
					+ String.format("%.2f", 100 * unloadTax) + "%");
			sender.sendMessage(
					ChatColor.DARK_AQUA + "Load Tax: " + ChatColor.WHITE + String.format("%.2f", 100 * loadTax) + "%");
			if (cardinalDistance)
				sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + "Cardinal");
			else
				sender.sendMessage(ChatColor.DARK_AQUA + "Distance Type: " + ChatColor.WHITE + "Direct");
			return true;
		}
		return false;

	}

	@EventHandler
	public void onSignClick(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (e.getClickedBlock().getState() instanceof Sign) {
				Sign sign = (Sign) e.getClickedBlock().getState();
				if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[UnLoad]")) {
					unload(e.getPlayer());
					return;
				}
				if (sign.getLine(0).equals(ChatColor.DARK_AQUA + "[Load]")) {
					load(e.getPlayer());
				}
			}
		}
	}

	@EventHandler
	public void onSignPlace(SignChangeEvent e) {
		if (e.getBlock().getState() instanceof Sign) {
			if (ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[Load]")
					|| ChatColor.stripColor(e.getLine(0)).equalsIgnoreCase("[UnLoad]")) {
				e.setLine(0, ChatColor.DARK_AQUA
						+ (ChatColor.stripColor(e.getLine(0))).replaceAll("u", "U").replaceAll("l", "L"));
			}
		}
	}

	public static boolean isDebug() {
		return debug;
	}

	public static Economy getEconomy() {
		return economy;
	}

	public static List<Player> getQue() {
		return playersInQue;
	}

	public static double getLoadTax() {
		return loadTax;
	}

	public static double getUnloadTax() {
		return unloadTax;
	}

	public static int getDelay() {
		return delay;
	}

	public static NPCMain getInstance() {
		return instance;
	}

	private void unload(Player player) {

		if (!player.hasPermission("Cargo.unload")) {
			player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
			return;
		}
		Craft playerCraft = craftManager.getCraftByPlayer(player);
		if (playersInQue.contains(player)) {
			player.sendMessage(ERROR_TAG + "You're already moving cargo!");
			return;
		}

		if (playerCraft == null) {
			player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
			return;
		}
		// NPC cargoMerchant=null;
		List<NPC> nearbyMerchants = new ArrayList<>();
		double distance;// , lastScan = scanRange;
		MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
		for (NPC npc : CargoUtils.getNPCsWithTrait(CargoTrait.class)) {
			if (!npc.isSpawned())
				continue;
			distance = cardinalDistance
					? Math.abs(loc.getX() - npc.getEntity().getLocation().getX())
							+ Math.abs(loc.getZ() - npc.getEntity().getLocation().getZ())
					: Math.sqrt(Math.pow(loc.getX() - npc.getEntity().getLocation().getX(), 2)
							+ Math.pow(loc.getZ() - npc.getEntity().getLocation().getZ(), 2));
			if (distance <= scanRange) {
				nearbyMerchants.add(npc);
			}
		}
		if (nearbyMerchants.size() == 0) {
			player.sendMessage(
					ERROR_TAG + "You need to be within " + scanRange + " blocks of a merchant to use that command!");
			return;
		}

		if (player.getInventory().getItemInMainHand() == null
				|| player.getInventory().getItemInMainHand().getType() == Material.AIR) {
			player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
			return;
		}
		String guiName;
		TradableGUIItem finalItem = null;
		for (NPC cargoMerchant : nearbyMerchants) {
			if (finalItem != null)
				break;
			guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
			AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
			TradeGUI tradeGUI = (TradeGUI) gui;
			ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
			finalItem = null;
			for (TradeGUIPage page : tradeGUI.getPages()) {
				if (page == null)
					continue;
				for (AGUIItem tempItem : page.getItems("sell")) {
					if (tempItem == null)
						continue;
					if (tempItem.getMainItem().isSimilar(compareItem)) {
						if (tempItem.getMainItem().getAmount() > 1)
							continue;
						finalItem = (TradableGUIItem) tempItem;
						break;
					}
				}
				if (finalItem == null || finalItem.getTradePrice() == 0.0) {
					player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
					return;
				}

			}
		}
		assert finalItem != null;
		String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null
				&& finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0
						? finalItem.getMainItem().getItemMeta().getDisplayName()
						: finalItem.getMainItem().getType().name().toLowerCase();
		int size = CargoUtils
				.getInventories(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST).size();
		if (size <= 0) {
			player.sendMessage(NPCMain.ERROR_TAG + "You have no " + itemName + " on this craft!");
			return;
		}
		player.sendMessage(SUCCESS_TAG + "Started unloading cargo");
		playersInQue.add(player);
		new UnloadTask(craftManager.getCraftByPlayer(player), finalItem).runTaskTimer(this, delay, delay);
		new ProcessingTask(player, finalItem, size).runTaskTimer(this, 0, 20);
	}

	private void load(Player player) {
		if (!player.hasPermission("Cargo.load")) {
			player.sendMessage(ERROR_TAG + "You don't have permission to do that!");
			return;
		}
		Craft playerCraft = craftManager.getCraftByPlayer(player);
		if (playersInQue.contains(player)) {
			player.sendMessage(ERROR_TAG + "You're already moving cargo!");
			return;
		}

		if (playerCraft == null) {
			player.sendMessage(ERROR_TAG + "You need to be piloting a craft to do that!");
			return;
		}
		// NPC cargoMerchant=null;
		List<NPC> nearbyMerchants = new ArrayList<>();
		double distance;// , lastScan = scanRange;
		MovecraftLocation loc = playerCraft.getHitBox().getMidPoint();
		for (NPC npc : CargoUtils.getNPCsWithTrait(CargoTrait.class)) {
			if (!npc.isSpawned())
				continue;
			distance = cardinalDistance
					? Math.abs(loc.getX() - npc.getEntity().getLocation().getX())
							+ Math.abs(loc.getZ() - npc.getEntity().getLocation().getZ())
					: Math.sqrt(Math.pow(loc.getX() - npc.getEntity().getLocation().getX(), 2)
							+ Math.pow(loc.getZ() - npc.getEntity().getLocation().getZ(), 2));
			if (distance <= scanRange) {
				nearbyMerchants.add(npc);
			}
		}
		if (nearbyMerchants.size() == 0) {
			player.sendMessage(
					ERROR_TAG + "You need to be within " + scanRange + " blocks of a merchant to use that command!");
			return;
		}

		if (player.getInventory().getItemInMainHand() == null
				|| player.getInventory().getItemInMainHand().getType() == Material.AIR) {
			logger.info(player.getInventory().getItemInMainHand().getType().name());
			player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
			return;
		}
		String guiName;
		TradableGUIItem finalItem = null;
		for (NPC cargoMerchant : nearbyMerchants) {
			if (finalItem != null)
				break;
			guiName = cargoMerchant.getTrait(TraderTrait.class).getGUIName();
			AGUI gui = dtlTradersPlugin.getGuiListService().getGUI(guiName);
			TradeGUI tradeGUI = (TradeGUI) gui;
			ItemStack compareItem = player.getInventory().getItemInMainHand().clone();
			for (TradeGUIPage page : tradeGUI.getPages()) {
				if (page == null)
					continue;
				for (AGUIItem tempItem : page.getItems("buy")) {
					if (tempItem == null)
						continue;
					if (tempItem.getMainItem().isSimilar(compareItem)) {
						if (tempItem.getMainItem().getAmount() > 1)
							continue;
						finalItem = (TradableGUIItem) tempItem;
						break;
					}
				}
				if (finalItem != null)
					break;
			}
			if (finalItem == null || finalItem.getTradePrice() == 0.0) {
				player.sendMessage(ERROR_TAG + "You need to be holding a cargo item to do that!");
				return;
			}
		}
		assert finalItem != null;
		String itemName = finalItem.getMainItem().getItemMeta().getDisplayName() != null
				&& finalItem.getMainItem().getItemMeta().getDisplayName().length() > 0
						? finalItem.getMainItem().getItemMeta().getDisplayName()
						: finalItem.getMainItem().getType().name().toLowerCase();
		if (!economy.has(player, finalItem.getTradePrice() * (1 + loadTax))) {
			player.sendMessage(ERROR_TAG + "You don't have enough money to buy any " + itemName + "!");
			return;
		}

		int size = CargoUtils
				.getInventoriesWithSpace(playerCraft, finalItem.getMainItem(), Material.CHEST, Material.TRAPPED_CHEST)
				.size();
		if (size <= 0) {
			player.sendMessage(NPCMain.ERROR_TAG + "You don't have any space for " + itemName + " on this craft!");
			return;
		}
		playersInQue.add(player);
		new LoadTask(craftManager.getCraftByPlayer(player), finalItem).runTaskTimer(this, delay, delay);
		new ProcessingTask(player, finalItem, size).runTaskTimer(this, 0, 20);
		player.sendMessage(SUCCESS_TAG + "Started loading cargo");
	}

}
