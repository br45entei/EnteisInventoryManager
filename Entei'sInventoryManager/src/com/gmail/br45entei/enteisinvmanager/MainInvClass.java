package com.gmail.br45entei.enteisinvmanager;

import java.io.File;
import java.util.ArrayList;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.br45entei.enteispluginlib.EPLib;
import com.gmail.br45entei.enteispluginlib.FileMgmt;
import com.gmail.br45entei.enteispluginlib.InvalidYamlException;
//import org.bukkit.scheduler.BukkitTask;

public class MainInvClass extends JavaPlugin implements Listener {
	private final MainInvClass plugin = this;
	public static PluginDescriptionFile pdffile;
	public static ConsoleCommandSender console;
	public static Server server = null;
	public static String pluginName = EPLib.rwhite + "["+ EPLib.green + "Entei's Inventory Manager" + EPLib.rwhite + "] ";
	public static String dataFolderName = "";
	public static boolean YamlsAreLoaded = false;
	public static FileConfiguration config;
	public static File configFile = null;
	public static String configFileName = "config.yml";
	public static boolean updateInvScreensDebounce = false;
	private static ArrayList<String> playersUsingInvsInfo = new ArrayList<String>();

	// TODO To be loaded from config.yml
	public static boolean showDebugMsgs = false;
	public static String noPerm = "";
	public static boolean worldsHaveSeparateInventories = false;
	public static boolean manageExp = false;
	public static boolean loadByGameMode = false;
	static final boolean forceDebugMsgs = false;
	// TODO Functions
	public void LoginListener(MainInvClass JavaPlugin) {
		getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@Override
	public void onDisable() {
		sendConsoleMessage(pluginName + "&eSaving all online players' inventories...");
		for(Player curPlayer : server.getOnlinePlayers()) {
			if(worldsHaveSeparateInventories) {
				savePlayerInventory(curPlayer, curPlayer.getWorld());
			} else {
				updatePlayerInventory(curPlayer, curPlayer.getWorld(), null, "save");
			}
			curPlayer.getOpenInventory().close();
		}
		//saveYamls();
		sendConsoleMessage(pluginName + "&eVersion " + pdffile.getVersion() + " is now disabled.");
	}
	
	@Override
	public void onEnable() {pdffile = this.getDescription();
		server = Bukkit.getServer();
		server.getPluginManager().registerEvents(this, this);
		console = server.getConsoleSender();
		File dataFolder = getDataFolder();
		if(!(dataFolder.exists())) {
			dataFolder.mkdir();
		}
		try{dataFolderName = getDataFolder().getAbsolutePath();} catch (SecurityException e) {FileMgmt.LogCrash(e, "onEnable()", "Failed to get the full directory of this plugin's folder(\"" + dataFolderName + "\")!", true, dataFolderName);}
		EPLib.showDebugMsg(pluginName + "The dataFolderName variable is: \"" + dataFolderName + "\"!", showDebugMsgs);
		// TODO Loading Files
		LoadConfig();
		// TODO End of Loading Files
		sendConsoleMessage(pluginName + "&aVersion " + pdffile.getVersion() + " is now enabled!");
	}
	public static void loadPlayerInventory(Player player, World world, boolean wipeInvs) {loadPlayerInventory(player, world, player.getGameMode(), wipeInvs);}
	public static void savePlayerInventory(Player player, World world) {savePlayerInventory(player, world, player.getGameMode());}
	public static void loadPlayerInventory(Player player, World world, GameMode gm, boolean wipeInvs) {
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = player.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		Inventory blankInv = server.createInventory(player, InventoryType.PLAYER);
		String invFileName = "";
		String armorFileName = "";
		String enderFileName = "";
		String expFileName = "";
		if(loadByGameMode == false) {
			invFileName = (worldName + ".inv");
			armorFileName = (worldName + ".armorInv");
			enderFileName = (worldName + ".enderInv");
			expFileName = (worldName + ".exp");
		} else {
			String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
			invFileName = (worldName + gamemode + ".inv");
			armorFileName = (worldName + gamemode + ".armorInv");
			enderFileName = (worldName + gamemode + ".enderInv");
			expFileName = (worldName + gamemode + ".exp");
		}
		try{
			if(wipeInvs) {
				player.getInventory().setContents(blankInv.getContents());
				player.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1)});
				player.getEnderChest().setContents(server.createInventory(player, InventoryType.ENDER_CHEST).getContents());
				if(manageExp) {
					player.setLevel(0);
					player.setExp(0);
				}
			}
			String invTitle = player.getName() + (loadByGameMode ? "'s " + ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Inventory") : "'s Inventory");
			String enderTitle = player.getName() + (loadByGameMode ? "'s " + ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Ender Chest") : "'s Ender Chest");
			//String extraTitle = player.getName() + (loadByGameMode ? "'s " + ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Extra Inventory") : "'s Extra Inventory");



			try{player.getInventory().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player).getContents());
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + invFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the gamemode-specific version of this file; if unsuccessful, will save over it from the player's current inventory instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					invFileName = (worldName + "" + player.getGameMode().name().toLowerCase() + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.setTitle(invTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + invFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						invFileName = (worldName + ".inv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
				} else {
					invFileName = (worldName + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.setTitle(invTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + invFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						invFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".inv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}



			try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
				player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + armorFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the gamemode-specific version of this file; if unsuccessful, will save over it from the player's current armor instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					armorFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".armorInv"); //Intentional swappage.
					try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
						player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + armorFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						armorFileName = (worldName + ".armorInv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
				} else {
					armorFileName = (worldName + ".armorInv"); //Intentional swappage.
					try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
						player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + armorFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						armorFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".armorInv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}



			try{player.getEnderChest().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player).getContents());
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + enderFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the gamemode-specific version of this file; if unsuccessful, will save over it from the player's current enderchest instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					enderFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".enderInv"); //Intentional swappage
					try{player.getEnderChest().setContents(InventoryAPI.setTitle(enderTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = (worldName + ".enderInv"); //Intentional swappage
					}
					FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
				} else {
					enderFileName = (worldName + ".enderInv"); //Intentional swappage
					try{player.getEnderChest().setContents(InventoryAPI.setTitle(enderTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".enderInv"); //Intentional swappage
					}
					FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}
			if(loadByGameMode) {// Pre-load the extra inventory for the player.
				getPlayerExtraChest(player, server.getWorld(worldName), GameMode.SURVIVAL);
				getPlayerExtraChest(player, server.getWorld(worldName), GameMode.CREATIVE);
				getPlayerExtraChest(player, server.getWorld(worldName), GameMode.ADVENTURE);
			} else {
				getPlayerExtraChest(player, server.getWorld(worldName));
			}
			if(manageExp) {
				try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
					player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
				} catch (Exception e) {
					EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + expFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the gamemode-specific version of this file; if unsuccessful, will save over it from the player's current exp instead.", true);
					//Start 'smart' loading
					if(loadByGameMode == false) {
						expFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".exp"); //Intentional swappage
						try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
							player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
							EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + expFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
						} catch (Exception e1) {
							expFileName = (worldName + ".exp"); //Intentional swappage
						}
						FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);
					} else {
						expFileName = (worldName + ".exp"); //Intentional swappage
						try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
							player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
							EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + expFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
						} catch (Exception e1) {
							expFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".exp"); //Intentional swappage
						}
						FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);
					}
					//End smart loading.
				}
			} else {
				EPLib.sendOneTimeMessage(pluginName + "&eThe var \"&fmanageExp&e\" was set to false in the config.yml; not managing player experience levels.", "console");
			}
		} catch (Exception e) {e.printStackTrace();/*savePlayerInventory(player, world);*/}
	}
	
	public static void savePlayerInventory(Player player, World world, GameMode gm) {
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = player.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invFileName = "";
		String armorFileName = "";
		String enderFileName = "";
		String expFileName = "";
		if(loadByGameMode == false) {
			invFileName = (worldName + ".inv");
			armorFileName = (worldName + ".armorInv");
			enderFileName = (worldName + ".enderInv");
			expFileName = (worldName + ".exp");
		} else {
			String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
			invFileName = (worldName + gamemode + ".inv");
			armorFileName = (worldName + gamemode + ".armorInv");
			enderFileName = (worldName + gamemode + ".enderInv");
			expFileName = (worldName + gamemode + ".exp");
		}
		FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
		FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
		FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
		if(manageExp) {FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);} else {EPLib.sendOneTimeMessage(pluginName + "&eThe var \"&fmanageExp&e\" was set to false in the config.yml; not managing player experience levels.", "console");}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerJoinEvent(PlayerJoinEvent evt) {
		Player newPlayer = evt.getPlayer();
		if(worldsHaveSeparateInventories) {
			loadPlayerInventory(newPlayer, newPlayer.getWorld(), false);
		} else {
			EPLib.showDebugMsg(pluginName + "&eThe var \"worldsHaveSeparateInventories\" equals false; not managing the player \"&a" + newPlayer.getName() + "&e\"'s inventory for world \"&a" + newPlayer.getWorld().getName() + "&e\" as an individual world inventory. Instead, managing as a grouped world, if applicable.", showDebugMsgs);
			updatePlayerInventory(newPlayer, null, newPlayer.getWorld(), "load");
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerQuit(PlayerQuitEvent evt) {
		Player oldPlayer = evt.getPlayer();
		if(worldsHaveSeparateInventories) {
			savePlayerInventory(oldPlayer, oldPlayer.getWorld());
		} else {
			EPLib.showDebugMsg(pluginName + "&eThe var \"worldsHaveSeparateInventories\" equals false; not managing the player \"&a" + oldPlayer.getName() + "&e\"'s inventory for world \"&a" + oldPlayer.getWorld().getName() + "&e\" as an individual world inventory. Instead, managing as a grouped world, if applicable.", showDebugMsgs);
			updatePlayerInventory(oldPlayer, oldPlayer.getWorld(), null, "save");
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerGameModeChangeEvent(PlayerGameModeChangeEvent evt) {
		final Player player = evt.getPlayer();
		GameMode oldGameMode = player.getGameMode();
		final GameMode newGameMode = evt.getNewGameMode();
		if(evt.isCancelled() == false) {
			savePlayerInventory(player, player.getWorld(), oldGameMode);
			server.getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					loadPlayerInventory(player, player.getWorld(), newGameMode, true);
					sendMessage(player, pluginName + "&aYour inventory has been updated to your current gamemode!");
				}
			}, 2);//Two ticks later(To let the player's inventory get saved before wiping it)!
		}
	}
	
	/**@param evt PlayerChangedWorldEvent
	 */
	@EventHandler(priority=EventPriority.LOWEST) 
	private void onPlayerChangedWorldEvent(PlayerChangedWorldEvent evt) {
		Player player = evt.getPlayer();
		World newWorld = (worldsHaveSeparateInventories ? player.getWorld() : getWhatWorldToUseFromWorld(player.getWorld()));
		World oldWorld = evt.getFrom();
		if(worldsHaveSeparateInventories) {
			//Save the old inventory to disk
/**//**//**/savePlayerInventory(player, oldWorld);
			//Load the new inventory from disk
/**//**//**/loadPlayerInventory(player, newWorld, false);
		} else {
			EPLib.showDebugMsg(pluginName + "&eThe var \"worldsHaveSeparateInventories\" equals false; not managing the player \"&a" + player.getName() + "&e\"'s inventory for world \"&a" + player.getWorld().getName() + "&e\" as an individual world inventory. Instead, managing as a grouped world, if applicable.", showDebugMsgs);
			updatePlayerInventory(player, oldWorld, newWorld, "both");
		}
	}
	
	/**@param player Player
	 * @param oldWorld World
	 * @param newWorld World
	 * @param loadSaveOrBoth Boolean 
	 */
	public void updatePlayerInventory(Player player, World oldWorld, World newWorld, String loadSaveOrBoth) {
		String oldWorldName = "";
		if(loadSaveOrBoth.equalsIgnoreCase("save") || loadSaveOrBoth.equalsIgnoreCase("both")) {
			oldWorldName = oldWorld.getName();
		}
		EPLib.showDebugMsg(pluginName + "&a'WorldTo': \"&6" + player.getWorld().getName() + "&a\"", showDebugMsgs);
		EPLib.showDebugMsg(pluginName + "&a'WorldFrom': \"&6" + oldWorldName + "&a\"", showDebugMsgs);
		if(loadSaveOrBoth.equalsIgnoreCase("save") || loadSaveOrBoth.equalsIgnoreCase("both")) {
			World worldToSaveTo = getWhatWorldToUseFromWorld(oldWorld);
/**//**//**/savePlayerInventory(player, worldToSaveTo);
			sendConsoleMessage(pluginName + (worldToSaveTo.getName().equalsIgnoreCase(oldWorld.getName()) ? "&1&nSAVING:&r &aSaved player \"&f" + player.getName() + "&a\"'s inventory for world: \"&6" + worldToSaveTo.getName() + "&a\"." : "&1&nSAVING:&r &aSaved player \"&f" + player.getName() + "&a\"'s inventory to world: \"&6" + worldToSaveTo.getName() + "&a\" instead of saving to world \"&6" + oldWorld.getName() + "&a\"."));
		} else {
			sendDebugMsg("&aThis is a player join event, isn't it?");
		}
		if(loadSaveOrBoth.equalsIgnoreCase("load") || loadSaveOrBoth.equalsIgnoreCase("both")) {
			World worldToLoadFrom = getWhatWorldToUseFromWorld(newWorld);
/**//**//**/loadPlayerInventory(player, worldToLoadFrom, true);
			sendConsoleMessage(pluginName + (worldToLoadFrom.getName().equalsIgnoreCase(newWorld.getName()) == false ? "&1&nLOADING:&r &aLoaded player \"&f" + player.getName() + "&a\"'s inventory from world: \"&6" + worldToLoadFrom.getName() + "&a\" instead of loading from world \"&6" + newWorld.getName() + "&a\"." : "&1&nLOADING:&r &aLoaded player \"&f" + player.getName() + "&a\"'s inventory for world: \"&6" + worldToLoadFrom.getName() + "&a\"."));
		} else {
			sendDebugMsg("&aThis is a player quit event, isn't it?");
		}
	}
	
	static String sendConsoleMessage(String msg) {
		return EPLib.sendConsoleMessage(msg);
	}
	
	static String sendMessage(CommandSender target, String msg) {
		return EPLib.sendMessage(target,  msg);
	}
	
	static String sendMessage(Player target, String msg) {
		return EPLib.sendMessage(target, msg);
	}
	
	private boolean LoadConfig() {
		this.saveDefaultConfig();
		configFile = new File(dataFolderName, configFileName);
		config = new YamlConfiguration();
		//NEWCONFIGFile = new File(dataFolderName, NEWCONFIGFileName);
		//NEWCONFIG = new YamlConfiguration();
		try {loadResourceFiles();} catch (Exception e) {e.printStackTrace();}
		YamlsAreLoaded = reloadFiles(true);
		if(YamlsAreLoaded == true) {EPLib.showDebugMsg(pluginName + "&aAll YAML Configration Files loaded successfully!", showDebugMsgs);
		} else {sendConsoleMessage(pluginName + "&cError: Some YAML Files failed to load successfully! Check the server log or \"" + dataFolderName + "\\crash-reports.txt\" to solve the problem.");}
		return YamlsAreLoaded;
	}
	
	private void loadResourceFiles() throws Exception {
		if(!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			FileMgmt.copy(getResource(configFileName), configFile, dataFolderName);
		}
		/*if(!NEWCONFIGFile.exists()) {
			NEWCONFIGFile.getParentFile().mkdirs();
			FileMgmt.copy(getResource(NEWCONFIGFileName), NEWCONFIGFile, dataFolderName);
		}*/
	}
	
	private boolean reloadFiles(boolean ShowStatus) {
		YamlsAreLoaded = false;
		boolean loadedAllVars = false;
		String unloadedFiles = "\"";
		Exception e1 = null;try{config.load(configFile);} catch (Exception e) {e1 = e;unloadedFiles = unloadedFiles + configFileName + "\" ";}
		//Exception e2 = null;try{NEWCONFIG.load(NEWCONFIGFile);} catch (Exception e) {e2 = e;unloadedFiles = unloadedFiles + NEWCONFIGFileName + "\" ";}
		try {
			if(unloadedFiles.equals("\"")) {
				YamlsAreLoaded = true;
				loadedAllVars = loadYamlVariables();
				if(loadedAllVars == true) {
					EPLib.showDebugMsg(pluginName + "&aAll of the yaml configuration files loaded successfully!", ShowStatus);
				} else {
					EPLib.showDebugMsg(pluginName + "&aSome of the settings did not load correctly from the configuration files! Check the server log to solve the problem.", ShowStatus);
				}
				return true;
			}
			String Causes = "";
			if(e1 != null) {Causes = Causes.concat(Causes + "\r" + e1.toString());}
			//if(e2 != null) {Causes = Causes.concat(Causes + "\r" + e2.toString());}
			throw new InvalidYamlException(Causes);
		} catch (InvalidYamlException e) {
			FileMgmt.LogCrash(e, "reloadYamls()", "Failed to load one or more of the following YAML files: " + unloadedFiles, false, dataFolderName);
			EPLib.showDebugMsg(pluginName + "&cThe following YAML files failed to load properly! Check the server log or \"" + dataFolderName + "\\crash-reports.txt\" to solve the problem: (" + unloadedFiles + ")", true);
			//MainCommandClass.logger.severe(e.toString());//A test
			return false;
		}
	}
	
	private boolean saveYamls() {
		String unSavedFiles = "\"";
		//The following tries to save the FileConfigurations to their Files:
		Exception e1 = null;try{config.save(configFile);} catch (Exception e) {e1 = e;unSavedFiles = unSavedFiles + configFileName + "\" ";}
		//Exception e2 = null;try{NEWCONFIG.save(NEWCONFIGFile);} catch (Exception e) {e2 = e;unSavedFiles = unSavedFiles + NEWCONFIGFileName + "\" ";}
		try {
			if(unSavedFiles.equals("\"")) {EPLib.showDebugMsg(pluginName + "&aAll of the yaml configuration files were saved successfully!", true);return true;}
			String Causes = "";
			if(e1 != null) {Causes = Causes.concat(Causes + "\r" + e1.toString());}
			//if(e2 != null) {Causes = Causes.concat(Causes + "\r" + e2.toString());}
			throw new InvalidYamlException(Causes);
		} catch (InvalidYamlException e) {
			FileMgmt.LogCrash(e, "saveYamls()", "Failed to save one or more of the following YAML files: (" + unSavedFiles + ")", false, dataFolderName);
			EPLib.showDebugMsg(pluginName + "&cThe following YAML files failed to get saved properly! Check the server log or \"" + dataFolderName + "\\crash-reports.txt\" to solve the problem: (" + unSavedFiles + ")", true);
			//MainCommandClass.logger.severe(e.toString());//A test
			return false;
		}
	}
	
	@SuppressWarnings("boxing")
	private static boolean loadYamlVariables() {
		boolean loadedAllVars = true;
		try{showDebugMsgs = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("showDebugMsgs")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("showDebugMsgs", "config.yml", pluginName);}
		try{noPerm = EPLib.formatColorCodes(config.getString("noPermission"));
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("noPermission", "config.yml", pluginName);}
		try{worldsHaveSeparateInventories = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("worldsHaveSeparateInventories")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("worldsHaveSeparateInventories", "config.yml", pluginName);}
		
		try{manageExp = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("manageExp")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("manageExp", "config.yml", pluginName);}
		
		try{loadByGameMode = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("loadByGameMode")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("loadByGameMode", "config.yml", pluginName);}
		
		return loadedAllVars;
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, final Command cmd, final String command, final String[] args) {
		String strArgs = "";
		if(!(args.length == 0)) {
			strArgs = "";
			int x = 0;
			do{strArgs = strArgs.concat(args[x] + " "); x++; }while( x < args.length );
		}
		strArgs = strArgs.trim();
		Player user = server.getPlayer(sender.getName());
		String userName = sender.getName();
		if(user != null) {
			userName = user.getDisplayName();
		}
		if(userName.equals("") == true) {
			userName = sender.getName();
		}
		if(command.equalsIgnoreCase("enteisinventorymanager")||command.equalsIgnoreCase("eim")) {
			if(args.length >= 1) {
				if(args[0].equalsIgnoreCase("reload")) {
					boolean userHasPerm = false;
					if(user != null) {userHasPerm = (user.hasPermission("eim.reload") || user.hasPermission("eim.*"));} else {userHasPerm = true;}
					if(userHasPerm == true || user == null) {
						boolean reloaded = LoadConfig();
						if(reloaded == true) {
							if(sender.equals(console) == false) {
								sendMessage(sender, pluginName + "&2Configuration files successfully reloaded!");
							} else {
								EPLib.showDebugMsg(pluginName + "&aYaml configuration files reloaded successfully!", showDebugMsgs);
							}
						} else {
							if(sender.equals(console) == false) {
								sendMessage(sender, pluginName + "&cThere was an error when reloading the configuration files.");
							} else {
								EPLib.showDebugMsg(pluginName + "&eSome of the yaml configuration files failed to load successfully, check the server log for more information.", showDebugMsgs);
							}
						}
					} else {
						sendMessage(sender, noPerm);
					}
					return true;
				} else if(args[0].equalsIgnoreCase("save")) {
					boolean userHasPerm = false;
					if(user != null) {userHasPerm = (user.hasPermission("eim.save") || user.hasPermission("eim.*"));} else {userHasPerm = true;}
					if(userHasPerm == true || user == null) {
						boolean saved = saveYamls();
						if(saved == true) {
							if(sender.equals(console) == false) {
								sendMessage(sender, pluginName + "&2The configuration files saved successfully!");
							} else {
								EPLib.showDebugMsg(pluginName + "&aThe yaml configuration files were saved successfully!", showDebugMsgs);
							}
						} else {
							if(sender.equals(console) == false) {
								sendMessage(sender, pluginName + "&cThere was an error when saving the configuration files.");
							} else {
								EPLib.showDebugMsg(pluginName + "&eSome of the yaml configuration files failed to save successfully, check the crash-reports.txt file for more information.", showDebugMsgs);
							}
						}
					} else {
						sendMessage(sender, noPerm);
					}
					return true;
				} else if(args[0].equalsIgnoreCase("info")) {
					boolean userHasPerm = false;
					if(user != null) {userHasPerm = (user.hasPermission("eim.info")||user.hasPermission("eim.*"));} else {userHasPerm = true;}
					if(userHasPerm || user == null) {
						if(args.length == 1) {
							String authors = "\"";
							for(String curAuthor : pdffile.getAuthors()) {authors = authors + curAuthor + "\", \"";}
							if(authors.equals("\"") == false) {authors = authors + ".";authors = authors.replace("\", \".", "\"");
							} else {authors = "&oNone specified in plugin.yml!&r";}
							sendMessage(sender, EPLib.green + pdffile.getPrefix() + " " + pdffile.getVersion() + "; Main class: " + pdffile.getMain() + "; Author(s): (" + authors + "&2).");
						} else {
							sendMessage(sender, pluginName + "&eUsage: /" + command + " info");
						}
						//return true;
					} else {
						sendMessage(sender, pluginName + noPerm);
					}
					return true;
				} else {
					EPLib.sendMessage(sender, pluginName + "&eUsage: \"/" + command + " info\" or use an admin command.");
				}
				return true;
			}
			EPLib.sendMessage(sender, pluginName + "&eUsage: \"/" + command + " info\" or use an admin command.");
			return true;
		} else if(command.equalsIgnoreCase("view")) {// TODO /view
			if(user != null) {
				if(user.hasPermission("eim.cmd.use.view") == false && user.hasPermission("eim.*") == false) {
					sendMessage(user, pluginName + noPerm);
					return true;
				}
				String invToLoad = null;
				Player target = null;
				GameMode gm = null;
				World targetWorld = null;
				if(args.length == 1) {
					invToLoad = args[0];
					target = user;
					gm = user.getGameMode();
					targetWorld = user.getWorld();
				} else if(args.length == 2) {// TODO /view {playerName|worldName|gamemode} {inv|ender|extra}
					invToLoad = args[1];
					target = server.getPlayer(args[0]);
					gm = GameMode.getByValue(args[0].equalsIgnoreCase("survival") || args[0].equals("0") ? 0 : (args[0].equalsIgnoreCase("creative") || args[0].equals("1") ? 1 : (args[0].equalsIgnoreCase("adventure") || args[0].equals("2") ? 2 : 42)));
					targetWorld = server.getWorld(args[0]);
					if(target == null && gm == null && targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName, worldName, or gameMode: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName|worldName|gamemode} {inv|ender|extra}");
						return true;
					}
				} else if(args.length == 3) {
					invToLoad = args[2];
					// TODO /view {worldName} {gameMode} {inv|ender|extra}
					gm = GameMode.getByValue(args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? 0 : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? 1 : (args[1].equalsIgnoreCase("adventure") || args[1].equals("2") ? 2 : 42)));
					targetWorld = server.getWorld(args[0]);
					if(targetWorld != null && gm != null) {
						target = user;
					}
					if(server.getPlayer(args[0]) != null) {// TODO /view {playerName} {worldName|gamemode} {inv|ender|extra}
						target = server.getPlayer(args[0]);
						gm = GameMode.getByValue(args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? 0 : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? 1 : (args[1].equalsIgnoreCase("adventure") || args[1].equals("2") ? 2 : 42)));
						targetWorld = server.getWorld(args[1]);
						if(gm == null && targetWorld == null) {
							/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[1] + "&r&e\", is not a valid gameMode or a valid worldName."/*)*/);
							/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName|gamemode} {inv|ender|extra} or /" + command + " {worldName} {gameMode} {inv|ender|extra}"/*)*/);
							return true;
						}
						if(gm == null) {gm = (target != null ? target.getGameMode() : null);}
						if(targetWorld == null) {targetWorld = (target != null ? target.getWorld() : null);}
					} else if(targetWorld == null) {// TODO /view {worldName} {gamemode} {inv|ender|extra}
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[0] + "&r&e\", is not a valid worldName.")/*)*/;
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {worldName} {gamemode} {inv|ender|extra}")/*)*/;
						return true;
					} else if(gm == null) {// TODO /view {worldName} {gamemode} {inv|ender|extra}
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[1] + "&r&e\", is not a valid gameMode.")/*)*/;
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {worldName} {gamemode} {inv|ender|extra}")/*)*/;
						return true;
					}
					if(target == null && gm == null && targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName, worldName, or gameMode: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName|worldName|gamemode} {inv|ender|extra}");
						return true;
					}
				} else if(args.length == 4) {// TODO /view {playerName} {worldName} {gamemode} {inv|ender|extra}
					invToLoad = args[3];
					target = server.getPlayer(args[0]);
					gm = GameMode.getByValue(args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? 0 : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? 1 : (args[1].equalsIgnoreCase("adventure") || args[1].equals("2") ? 2 : 42)));
					targetWorld = server.getWorld(args[2]);
					if(target == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra}");
						return true;
					} else if(gm == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid gameMode: \"&f" + args[1] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra}");
						return true;
					} else if(targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid worldName: \"&f" + args[2] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra}");
						return true;
					}
				} else {
					sendMessage(user, pluginName + "&eThe arguments that you entered were not compatable for this command. Here is a list of all possible usages:");
					showUsageForCmd("view", "all", user);
					return true;
				}
				if(target == null) {target = user;}
				if(gm == null) {gm = target.getGameMode();}
				if(targetWorld == null) {targetWorld = target.getWorld();}
				if(invToLoad.equalsIgnoreCase("inv")) {
					if(user.getName().equals(target.getName()) && user.getGameMode().equals(gm)) {
						sendMessage(user, pluginName + "&eYou can view this inventory by pressing 'e', or whatever your inventory button is set to!");
						return true;
					}
					String success = openPlayerInventory(user, target, gm, targetWorld, "inv");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your" : " &f" + target.getDisplayName() + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " inventory.");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("ender") || invToLoad.equalsIgnoreCase("enderchest")) {
					String success = openPlayerInventory(user, target, gm, targetWorld, "ender");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your" : " &f" + target.getDisplayName() + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " ender chest.");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("extra")) {
					String success = openPlayerInventory(user, target, gm, targetWorld, "extra");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your extra" : " &f" + target.getDisplayName() + "&r&e's extra") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " chest.");
					}
					return true;
				} else {
					sendMessage(sender, pluginName + "&eThe argument you entered, \"&f" + invToLoad + "&r&e\", must be one of the following: &finv ender enderchest extra&e.");
					return true;
				}
			}
			// /view for console
			sendMessage(sender, pluginName + "&e/" + command + " is used to display a player's inventory. When used by the console, it is used to display the targeted players' inventory on their screen. This command is, however, currently NYI for console use.(Not Yet Implemented)");
			return true;
		} else if(command.equalsIgnoreCase("invperm")) {
			RegisteredServiceProvider<Permission> permProvider = null;
			@SuppressWarnings("unused")Permission permission = null;
			if(EPLib.vaultIsAvailable) {
				permProvider = server.getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
				if(permProvider != null) {
					permission = permProvider.getProvider();
					EPLib.showDebugMsg(pluginName + "&aInternal variable \"permission\" is not null!", showDebugMsgs);
				} else {
					EPLib.showDebugMsg(pluginName + "&4Could not load permission service...(No Vault Plugin, or coding issue?)", showDebugMsgs);
					sendMessage(sender, pluginName + "&eWhoops! There was an error when trying to load &bVault&e's permission service... Please let the server owner know so he/she can fix it!");
				}
			} else {
				sendMessage(sender, pluginName + "&eSorry, this command is only available with &bVault&e installed! Please ask the server owner about installing &bVault&e.");
				return true;
			}
			if(user != null) {
				//Used by Player
				//MAKE THIS!
				return true;
			}
			//Used by Console
			//MAKE THIS!
			sendInvPermUsageForConsole(sender, command);
			return true;
		} else {
			return false;
		}
	}
	
	void showUsageForCmd(String cmd, String mode, CommandSender target) {
		if(cmd.equalsIgnoreCase("view")) {
			if(mode.equalsIgnoreCase("all")) {
				sendMessage(target, pluginName + "&e/" + cmd + " &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &6{gamemode} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &d{worldName} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &d{worldName} &6{gamemode} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &6{gamemode} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &d{worldName} &3{inv|ender|extra}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &d{worldName} &6{gamemode} &3{inv|ender|extra}");
				return;
			}
		}
	}
	
	World getWhatWorldToUseFromWorld(World oldWorld) {
		World rtrn = oldWorld;
		String primaryWorldName = "";
		int numOfLists = 0;
		boolean Continue = true;
		boolean worldNameWasRetrieved = false;
		try{numOfLists = config.getInt("numberOfLists");} catch (Exception e) {Continue = false;e.printStackTrace();}
		if(Continue) {
			for(int num = 1; num <= numOfLists; num++) {
				String worldNameList = "";
				try{worldNameList = config.getString("list_" + num);
				} catch (Exception e) {
					EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
					return oldWorld;
				}
				if(worldNameList != null) {
					if(worldNameList.split("\\|").length >= 1) {
						boolean firstRunIsOver = false;
						for(String curWorldName : worldNameList.split("\\|")) {
							if(worldNameWasRetrieved) {
								break;
							}
							if(curWorldName.equals("") == false) {
								if(firstRunIsOver == false) {
									if(Bukkit.getWorld(curWorldName) != null) {
										primaryWorldName = curWorldName;
										firstRunIsOver = true;
									}
								}
								if(oldWorld.getName().equalsIgnoreCase(curWorldName) || oldWorld.getName().equalsIgnoreCase(primaryWorldName)) {
									rtrn = server.getWorld(primaryWorldName);
									sendDebugMsg((primaryWorldName.equalsIgnoreCase(oldWorld.getName()) ? "&aThe world to use is still: \"&6" + primaryWorldName + "&a\"." : "&aThe world to use is now \"&6" + primaryWorldName + "&a\" instead of world \"&6" + oldWorld.getName() + "&a\"."));
									worldNameWasRetrieved = true;
								}
							}
						}
						if(worldNameWasRetrieved == true) {
							break;
						}
					} else {
						EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
					}
				} else {
					EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
				}
			}
		} else {
			EPLib.unSpecifiedVarWarning("numberOfLists", configFileName, pluginName);
		}
		return rtrn;
	}
	
	/**This shows the given target parameter's given inventory to the given user parameter.(Usage: boolean openedOrNot = openPlayerInventory(Player user(the player who sees the inventory screen), Player target(the owner of the inventory), GameMode gm(can be null), String invToOpen)
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param invToOpen String
	 * @return Whether or not the inventory was opened to the user for the given world(this depends on whether or not the user had permission to open the target's inventory for the given world).
	 */
	private String openPlayerInventory(Player user, Player target, GameMode gm, World world, String invToOpen) {// TODO openPlayerInventory()
		String worldName = (worldsHaveSeparateInventories ? (world != null ? world : target.getWorld()) : getWhatWorldToUseFromWorld(world != null ? world : target.getWorld())).getName().toLowerCase().replaceAll(" ", "_");
		sendDebugMsg("&a'user': " + user.getName());
		sendDebugMsg("&a'target': " + target.getName());
		String success = "true";
		Inventory invToView = null;
		if(gm == null) {
			gm = user.getGameMode();
			sendDebugMsg("&athe 'gm' var was null; setting it to \"" + user.getName() + "\"'s current gamemode, which is &f" + gm.name().toLowerCase() + "&a.");
		}
		sendDebugMsg("&a'gm': " + gm.name());
		sendDebugMsg("&a'world': " + worldName);
		sendDebugMsg("&a'invToOpen': " + invToOpen);
		String gamemode = gm.name().toLowerCase();
		String playerName = target.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invFileName = worldName + (loadByGameMode ? "." + gamemode : "") + ".inv";
		String enderFileName = worldName + (loadByGameMode ? "." + gamemode : "") + ".enderInv";
		String extraFileName = worldName + (loadByGameMode ? "." + gamemode : "") + ".extraChestInv";
		//String armorFileName = (worldName + "." + gamemode + ".armorInv");
		//String expFileName = (worldName + "." + gamemode + ".exp");
		if(invToOpen.equalsIgnoreCase("inv")) {
			try{invToView = InventoryAPI.setTitle(target.getName() + "'s " + (loadByGameMode ? EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Inventory" : "Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), target));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested inventory because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "inventory. Have you been to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
		} else if(invToOpen.equalsIgnoreCase("ender") || invToOpen.equalsIgnoreCase("enderchest")) {
			if(invToOpen.equalsIgnoreCase("enderchest")) {invToOpen = "ender";}
			try{invToView = InventoryAPI.setTitle(target.getName() + "'s " + (loadByGameMode ? EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Ender Chest" : "Ender Chest"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), target));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested ender chest because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "ender chest. Have you been to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
			
		} else if(invToOpen.equalsIgnoreCase("extra")) {
			try{invToView = InventoryAPI.setTitle(target.getName() + "'s " + (loadByGameMode ? EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Extra Inventory" : "Extra Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraFileName, FolderName, dataFolderName, false), target));
			} catch (Exception e) {
				invToView = (loadByGameMode ? getPlayerExtraChest(target, server.getWorld(worldName), gm) : getPlayerExtraChest(target, server.getWorld(worldName)));
			}
		}
		if(invToView != null) {
			invToOpen = (invToOpen.equalsIgnoreCase("inv") ? "inv" : (invToOpen.equalsIgnoreCase("ender") ? "enderInv" : "extraChestInv"));
			if(user.equals(target) && invToOpen.equalsIgnoreCase("extra") ? true : (user.hasPermission("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + "." + target.getName()) || user.hasPermission("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + ".*")) ) {
				playersUsingInvsInfo.add(user.getName() + "|" + worldName);
				user.openInventory(invToView);
			} else {
				success = "noperm";
				sendDebugMsg("Could not open the requested inventory because &f" + user.getDisplayName() + "&r&e does not have any of the following permissions: &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + "." + target.getName()) + "&e, &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + ".*"));
			}
		} else {
			success = "false";
		}
		return success;
	}
	
	@SuppressWarnings("unused")
	private static void sendInvPermUsage(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {give|take} {view|edit} {inv|ender|extra} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} your {inv|ender|extra} Inventory(View this with \"&f/view&e\".");
	}
	
	private static void sendInvPermUsageForConsole(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {ownerName} {give|take} {view|edit} {inv|ender|extra} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} the {ownerName}'s {inv|ender|extra} Inventory.");
	}
	
	@SuppressWarnings("unused")
	private static void sendNotOnlineMessage(CommandSender sender, String arg) {
		sendMessage(sender, pluginName + "&e\"&f" + arg + "&e\" is not a player, or is not online.");
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryClickEvent(InventoryClickEvent evt) {//For checking perms and updating inventory screens.
		final Player editor = (Player) evt.getWhoClicked();
		Inventory sourceInv = evt.getInventory();//editor.getOpenInventory().getTopInventory();
		final String invName = sourceInv.getTitle();
		Player owner = null;
		if(sourceInv.getHolder() instanceof Player) {
			owner = (Player) sourceInv.getHolder();
		}
		sendDebugMsg("&1&n=====&r&6onInventoryClickEvent&f(&aInventoryClickEvent &2evt&f)&1&n=====&f: " + editor.getName());
		if(owner != null) {
			if(owner.getName().equals(editor.getName())) {
				sendDebugMsg("&1&n=====&r *eThe owner is the one who is editing!");
				if(invName.equals("container.crafting") || invName.equals("container.inventory")) {
					sendDebugMsg("The owner is viewing their vanilla mc inventory. Let's save it and update the inv screens for it!");
					sourceInv = owner.getOpenInventory().getBottomInventory();
					updateOwnersInvAndSave(owner, sourceInv, (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), false);
					updateOwnersInvAndSave(editor, editor.getOpenInventory().getBottomInventory(), (loadByGameMode ? editor.getGameMode() : null), "inv", editor.getWorld(), false);
					return;
				}
				GameMode invGameMode = (loadByGameMode ? (invName.contains("'s S") ? GameMode.SURVIVAL : (invName.contains("'s C") ? GameMode.CREATIVE : (invName.contains("'s A") ? GameMode.ADVENTURE : null))) : null);
				String invType = (invName.contains("Extra Inventory") ? "extra" : (invName.contains("Inventory") ? "inv" : (invName.contains("Ender Chest") ? "ender" : "UNKNOWN")));
				sendDebugMsg("&1&n=====&r&eThe owner is viewing their \"&f" + invType + "&r&e\" inventory. Let's save it and update the inv screens for it!");
				updateOwnersInvAndSave(owner, sourceInv, (loadByGameMode ? invGameMode : null), invType, owner.getWorld(), false);
				updateOwnersInvAndSave(editor, editor.getOpenInventory().getBottomInventory(), (loadByGameMode ? editor.getGameMode() : null), "inv", editor.getWorld(), false);
				sendDebugMsg("&1&n=====&r&eThe owner is viewing their \"&f" + invType + "&r&e\" inventory. Let's save it and update the inv screens for it!");
			} else {
				sendDebugMsg("&1&n=====&r *eSomeone else other than the owner is editing! That person is: \"&f" + editor.getName() + "&r&e\"; the owner is: \"&f" + owner.getName() + "&r&e!\"");
				
				String invType = "";
				if(invName.contains("Extra Inventory")) {
					invType = "extra";
				} else if(invName.contains("Ender Chest")) {
					sendDebugMsg("&1&n3 B");
					invType = "ender";
				} else if(invName.contains("Inventory")) {
					sendDebugMsg("&1&n3 C");
					invType = "inv";
				}
				String pGamemode = (loadByGameMode ? "gamemode." + (invName.contains("'s S") ? "survival." : (invName.contains("'s C") ? "creative." : (invName.contains("'s A") ? "adventure." : "UNKNOWN."))) : "");
				String perm = "eim.edit." + pGamemode + invType;
				if(editor.hasPermission(perm) || editor.hasPermission(perm + "." + owner.getName()) || editor.hasPermission(perm + ".*") || editor.hasPermission("eim.edit.*") || editor.hasPermission("eim.*")) {
					GameMode invGameMode = (loadByGameMode ? (invName.contains("'s S") ? GameMode.SURVIVAL : (invName.contains("'s C") ? GameMode.CREATIVE : (invName.contains("'s A") ? GameMode.ADVENTURE : null))) : null);
					sendDebugMsg("&1&n=====&r&eThe editor is viewing the owners' \"&f" + invType + "&r&e\" inventory. Let's save it and update the inv screens for it, since the editor has permission!");
					updateOwnersInvAndSave(owner, sourceInv, (loadByGameMode ? invGameMode : null), invType, owner.getWorld(), false);
					updateOwnersInvAndSave(editor, editor.getOpenInventory().getBottomInventory(), (loadByGameMode ? editor.getGameMode() : null), "inv", editor.getWorld(), false);
				} else {
					sendDebugMsg("&1&n=====&r&eThe editor did not have permission to edit the owners' inventory. Shutting it!");
					closePlayerInventory(editor);
				}
			}
			return;
		} else if(invName.equals("container.enderchest")) {
			updateOwnersInvAndSave(editor, sourceInv, (loadByGameMode ? editor.getGameMode() : null), "ender", editor.getWorld(), false);
			updateOwnersInvAndSave(editor, editor.getOpenInventory().getBottomInventory(), (loadByGameMode ? editor.getGameMode() : null), "inv", editor.getWorld(), false);
			sendDebugMsg("The owner is viewing their ender chest. Let's save it and update the inv screens for it!");
		} else {
			sendDebugMsg("&1&nATTENTION!!!&r &e???<Unknown happenstance 001>");
		}
	}
	
	void closePlayerInventory(final Player target) {
		server.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				target.getOpenInventory().setCursor(null);
				target.getOpenInventory().close();
			}
		});
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryOpenEvent(InventoryOpenEvent evt) {//For checking perms.
		Player player = server.getPlayer(evt.getPlayer().getName());
		sendDebugMsg("&1&n=====&r&6onInventoryOpenEvent&f(&aInventoryOpenEvent &2evt&f)&1&n=====&f: " + player.getName());
		sendDebugMsg("&aThe inventory title that &f" + player.getDisplayName() + "&r&a has opened is: &e" + evt.getInventory().getTitle());
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryCloseEvent(InventoryCloseEvent evt) {//For saving.
		sendDebugMsg("&1&n1");
		Player viewer = server.getPlayer(evt.getPlayer().getName());
		Inventory inv = evt.getInventory();
		final String invName = inv.getTitle();
		Player owner = null;
		if(inv.getHolder() instanceof Player) {
			sendDebugMsg("&1&n2");
			owner = (Player) inv.getHolder();
		}
		String invType = "";
		if(inv.getTitle().contains("Extra Inventory")) {
			sendDebugMsg("&1&n3 A");
			invType = "extra";
		} else if(inv.getTitle().contains("Ender Chest")) {
			sendDebugMsg("&1&n3 B");
			invType = "ender";
		} else if(inv.getTitle().contains("Inventory")) {
			sendDebugMsg("&1&n3 C");
			invType = "inv";
		} else if(inv.getTitle().equals("container.enderchest")) {
			sendDebugMsg("&1&n3 D");
			invType = "container.enderchest";
		} else if(inv.getTitle().equals("container.crafting") || inv.getTitle().equals("container.inventory")) {
			sendDebugMsg("&1&n3 E");
			invType = "container.inventory";
		} else {
			sendDebugMsg("&1&n3 F");
		}
		if(owner != null) {
			if(invType.equals("container.inventory")) {
				sendDebugMsg("&eDebug: 'invType' == \"" + invType + "\"! The inventory title is: &f" + inv.getTitle());
				updateOwnersInvAndSave(owner, owner.getInventory(), (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), true);
				
				return;
			}
			if(invType.equals("container.enderchest")) {
				sendDebugMsg("&eDebug: 'invType' == \"" + invType + "\"! The inventory title is: &f" + inv.getTitle());
				updateOwnersInvAndSave(owner, owner.getEnderChest(), (loadByGameMode ? owner.getGameMode() : null), "ender", owner.getWorld(), true);
				
				return;
			}
			if(invType.equals("")) {
				sendDebugMsg("'invType' == \"\"! The inventory title is: &f" + inv.getTitle() + "&e; &1&nThis means we can't very well do anything...");
				return;
			}
			if(viewer.getName().equals(owner.getName())) {
				sendDebugMsg("'owner'(&f" + owner.getName() + "&r&e) == 'player'(&f" + viewer.getName() + "&r&e)!");
				sendDebugMsg("&1&nWe don't have to check for perms -- unless the owner isn't in the same gamemode and world as the inventory!");
				if((invType.equalsIgnoreCase("inv") || invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") || invType.equalsIgnoreCase("extra")) == false) {
					sendDebugMsg("&cThe invType didn't pan out! It still equals: \"&f" + invType + "&c\"!");
					return;
				}
				updateOwnersInvAndSave(owner, inv, (loadByGameMode ? owner.getGameMode() : null), invType, owner.getWorld(), true);
			} else {
				sendDebugMsg("'owner'(&f" + owner.getName() + "&r&e) != 'player'(&f" + viewer.getName() + "&r&e)!");
				sendDebugMsg("&1&nNow we check for perms!");
				String pGamemode = (loadByGameMode ? "gamemode." + (invName.contains("'s S") ? "survival." : (invName.contains("'s C") ? "creative." : (invName.contains("'s A") ? "adventure." : "UNKNOWN."))) : "");
				String perm = "eim.edit." + pGamemode + invType;
				if(viewer.hasPermission(perm) || viewer.hasPermission(perm + "." + owner.getName()) || viewer.hasPermission(perm + ".*") || viewer.hasPermission("eim.edit.*") || viewer.hasPermission("eim.*")) {
					if((invType.equalsIgnoreCase("inv") || invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") || invType.equalsIgnoreCase("extra")) == false) {
						sendDebugMsg("&cThe invType didn't pan out! It still equals: \"&f" + invType + "&c\"!");
						return;
					}
					updateOwnersInvAndSave(owner, inv, (loadByGameMode ? owner.getGameMode() : null), invType, owner.getWorld(), true);
				} else {
					sendDebugMsg("Unable to save the inventory \"&f" + invName + "&r&e\" because the viewer/editor (\"&f" + viewer.getName() + "&r&e\") did not have any of the following permissions: ");
					sendDebugMsg("\"&f" + perm + "&r&e\";");
					sendDebugMsg("\"&f" + perm + "." + owner.getName() + "&r&e\";");
					sendDebugMsg("\"&f" + perm + ".*&r&e\";");
					sendDebugMsg("\"&feim.edit.*&r&e\";");
					sendDebugMsg("\"&feim.*&r&e\".");
					sendMessage(viewer, pluginName + "&eWhoops! It appears you do not have permission to edit that inventory. Has &f" + owner.getDisplayName() + "&r&e not given you permission to edit that inventory yet?");
				}
			}
		} else {
			sendDebugMsg("'owner' == null in &6onInventoryCloseEvent&f(&aInventoryCloseEvent &2evt&f)&e!");
		}
	}
	
	World getWorldFromPlayerInvInfo(Player owner, boolean removeFromList) {
		World rtrn = owner.getWorld();
		for(String player_worldName : playersUsingInvsInfo) {
			String[] Info = player_worldName.split("\\|");
			String playerName = Info[0];
			String worldName = Info[1];
			if(playerName.equals(owner.getName())) {
				World getWorld = server.getWorld(worldName);
				if(getWorld != null) {
					sendDebugMsg("&aThe world retrieved from the list is: &f" + getWorld.getName());
				} else {
					sendDebugMsg("&aThe world retrieved from the list is null...(?)");
				}
				if(removeFromList) {removePlayerFromList(player_worldName);}
				return getWorld;
			}
		}
		sendDebugMsg("&aThe list did not return a world. Returning the following world instead: &f" + rtrn.getName());
		return rtrn;
	}
	
	void removePlayerFromList(final String plyrToRemove) {
		server.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				playersUsingInvsInfo.remove(plyrToRemove);
			}
		});
	}
	
	public static Inventory getPlayerExtraChest(Player target, World world, GameMode gm) {
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = (worldName + "." + gm.name().toLowerCase() + ".extraChestInv");
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = server.createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}
	
	public static Inventory getPlayerExtraChest(Player target, World world) {
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = worldName + ".extraChestInv";
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = server.createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onInventoryEvent(InventoryEvent evt) {
		if(evt.getInventory().getHolder() instanceof Player) {
			Player player = (Player) evt.getInventory().getHolder();
			sendDebugMsg("&1&n=====&r&6onInventoryEvent&f(&aInventoryEvent &2evt&f)&1&n=====&f: " + player.getName());
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPickupItemEvent(PlayerPickupItemEvent evt) {
		Player player = evt.getPlayer();
		sendDebugMsg("&1&n=====&r&6onPlayerPickupItemEvent&f(&aPlayerPickupItemEvent &2evt&f)&1&n=====&f: " + player.getName());
		updateOwnersInvAndSave(player, player.getInventory(), (loadByGameMode ? player.getGameMode() : null), "inv", player.getWorld(), false);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerDropItemEvent(PlayerDropItemEvent evt) {
		Player player = evt.getPlayer();
		sendDebugMsg("&aPlayer name involved with onPlayerDropItemEvent(): &f" + player.getName());
		Player owner = evt.getPlayer();

		updateOwnersInvAndSave(owner, owner.getInventory(), (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), false);

	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerItemHeldEvent(org.bukkit.event.player.PlayerItemHeldEvent evt) {
		sendDebugMsg("&aPlayerItemHeldEvent(&21/1&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
		Player owner = evt.getPlayer();

		updateOwnersInvAndSave(owner, owner.getInventory(), (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), false);

	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPlaceBlockEvent(PlayerInteractEvent evt) {
		sendDebugMsg("&aPlayerInteractEvent(&21/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
		if(evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			sendDebugMsg("&aPlayerInteractEvent(&22/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
			if(evt.getItem() != null) {
				sendDebugMsg("&aPlayerInteractEvent(&23/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
				if(evt.getItem().getTypeId() != 0) {
					sendDebugMsg("&aPlayerInteractEvent(&24/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
					Player owner = evt.getPlayer();

					updateOwnersInvAndSave(owner, owner.getInventory(), (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), false);

				} else {
					sendDebugMsg("&aPlayerInteractEvent(&c-4/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
				}
			} else {
				sendDebugMsg("&aPlayerInteractEvent(&c-3/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
			}
		} else {
			sendDebugMsg("&aPlayerInteractEvent(&c-2/4&a): \"&f" + evt.getPlayer().getName() + "&r&a\"...");
		}
	}
	
	Inventory updateOwnersInvAndSave(final Player owner, final Inventory inv, final GameMode gm, final String invType, World givenWorld, final boolean removeFromList) {
		sendDebugMsg("0&1&n");
		final World world = getWhatWorldToUseFromWorld(givenWorld);
		String UFownerName = "";
		if(owner != null) {
			sendDebugMsg("&1&n1");
			UFownerName = limitStringToNumOfChars(owner.getName(), 12);
		}
		final String ownerName = UFownerName;
		server.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				sendDebugMsg("&1&n2");
				sendDebugMsg("&6updateOwnersInv&f(&cfinal &aPlayer &2owner(" + ownerName + ")&f. &cfinal &aInventory &2inv(" + inv.getTitle() + ")&f, &cfinal &2GameMode gm(" + (gm != null ? gm.name() : "null") + ")&f, &cfinal &aString &2invType(" + invType.toLowerCase() + ")&f, &cfinal &aWorld &2world(" + world.getName() + ")&f, &cfinal boolean &2removeFromList(" + removeFromList + ")&f)");
				if(gm != null) {// if(loadByGameMode) {
					sendDebugMsg("&1&n3 A");
					sendDebugMsg("'gm' != null! It equals: &3" + gm.name());
					if(owner.getGameMode().equals(gm)) {
						sendDebugMsg("&1&n4 A");
						sendDebugMsg("The owners' gamemode is the same as 'gm'!");
						World ownersWorld = getWhatWorldToUseFromWorld(getWorldFromPlayerInvInfo(owner, removeFromList));
						if(ownersWorld.getName().equals(world.getName())) {
							sendDebugMsg("&1&n5 A");
							sendDebugMsg("The owner is in the 'world'(&f" + world.getName() + "&r&e)!");
							String FolderName = "Inventories" + File.separatorChar + owner.getName();
							String fileNameToSaveTo = world.getName().toLowerCase().replaceAll(" ", "_") + (gm.equals(GameMode.SURVIVAL) ? ".survival" : (gm.equals(GameMode.CREATIVE) ? ".creative" : (gm.equals(GameMode.ADVENTURE) ? ".adventure" : ".UNKNOWN")));
							String invName = "";
							String gamemode = " " + (gm.equals(GameMode.SURVIVAL) ? "S" : (gm.equals(GameMode.CREATIVE) ? "C" : (gm.equals(GameMode.ADVENTURE) ? "A" : "U")));
							boolean Continue = true;
							if(invType.equalsIgnoreCase("inv")) {
								sendDebugMsg("&1&n6 A");
								fileNameToSaveTo += ".inv";
								invName = ownerName + "'s" + gamemode + " Inventory";
							} else if(invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest")) {
								sendDebugMsg("&1&n6 B");
								fileNameToSaveTo += ".enderInv";
								invName = ownerName + "'s" + gamemode + " Ender Chest";
							} else if(invType.equalsIgnoreCase("extra")) {
								sendDebugMsg("&1&n6 C");
								fileNameToSaveTo += ".extraChestInv";
								invName = ownerName + "'s" + gamemode + " Extra Inventory";
							} else {
								sendDebugMsg("&1&n6 D");
								Continue = false;
							}
							GameMode invGameMode = (inv.getTitle().contains("'s S") ? GameMode.SURVIVAL : (inv.getTitle().contains("'s C") ? GameMode.CREATIVE : (inv.getTitle().contains("'s A") ? GameMode.ADVENTURE : null)));
							sendDebugMsg("&1&n=====&r &e'invGameMode' == &f" + (invGameMode != null ? invGameMode.name().toLowerCase() : "null"));
							if(gm.equals(invGameMode) == false && invGameMode != null) {
								sendDebugMsg("&1&n=====&r &eThe inventory's gamemode does not match the owners' gamemode! We shall just have to act as if the owner weren't here....");
								
								
								fileNameToSaveTo = world.getName().toLowerCase().replaceAll(" ", "_") + (invGameMode.equals(GameMode.SURVIVAL) ? ".survival" : (invGameMode.equals(GameMode.CREATIVE) ? ".creative" : (invGameMode.equals(GameMode.ADVENTURE) ? ".adventure" : ".UNKNOWN")));
								
								
								if(invType.equalsIgnoreCase("inv")) {
									sendDebugMsg("&1&n6_1 A");
									fileNameToSaveTo += ".inv";
									invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Inventory";
								} else if(invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest")) {
									sendDebugMsg("&1&n6_1 B");
									fileNameToSaveTo += ".enderInv";
									invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Ender Chest";
								} else if(invType.equalsIgnoreCase("extra")) {
									sendDebugMsg("&1&n6_1 C");
									fileNameToSaveTo += ".extraChestInv";
									invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Extra Inventory";
								}
								
								
								
								sendDebugMsg("&1&n7_1 A");
								sendDebugMsg("'fileNameToSaveTo' == &f" + fileNameToSaveTo);
								boolean success = FileMgmt.WriteToFile(fileNameToSaveTo, InventoryAPI.serializeInventory(InventoryAPI.setTitle(invName, inv)), true, FolderName, dataFolderName);
								sendDebugMsg("The plugin has " + (success ? "successfully written to" : "failed to write to") + " the file &z&f" + dataFolderName + File.separatorChar + FolderName + File.separatorChar + fileNameToSaveTo + "&z&r&e for the inventory \"&f" + invName + "&r&e\"! The owners' inventory has NOT been updated[the owner's gamemode(\"&f" + gm.name().toLowerCase() + "&e\") != the inventory's gamemode(\"&f" + invGameMode.name().toLowerCase() + "&e\")!].");
								updateViewersInvScreens(owner, inv, invGameMode, invType);//Update all online players' top inventory screens(if it's the owners') to the 'inv' inventory!
								
								return;
							}
							if(Continue) {
								sendDebugMsg("&1&n7 A");
								sendDebugMsg("'fileNameToSaveTo' == &f" + fileNameToSaveTo);
								boolean success = FileMgmt.WriteToFile(fileNameToSaveTo, InventoryAPI.serializeInventory(InventoryAPI.setTitle(invName, inv)), true, FolderName, dataFolderName);
								sendDebugMsg("The plugin has " + (success ? "successfully written to" : "failed to write to") + " the file &z&f" + dataFolderName + File.separatorChar + FolderName + File.separatorChar + fileNameToSaveTo + "&z&r&e for the inventory \"&f" + invName + "&r&e\"!" + (invType.equalsIgnoreCase("inv") ? " The owners' inventory has been updated." : ""));
								if(invType.equalsIgnoreCase("inv")) {
									owner.getInventory().setContents(inv.getContents());
								}
								updateViewersInvScreens(owner, inv, gm, invType);//Update all online players' top inventory screens(if it's the owners') to the 'inv' inventory!
							} else {
								sendDebugMsg("&1&n7 B");
								sendDebugMsg("The invType wasn't one of the four applicable strings! Why is it set to \"&f" + invType + "&r&e\"???");
								return;
							}
							sendDebugMsg("&1&n8");
						} else {
							sendDebugMsg("&1&n5 B");
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n'world': &r&f" + world.getName());
							sendDebugMsg("&1&n'ownersWorld': &r&f" + ownersWorld.getName());
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n=====ATTENTION=====");
						}
					} else {
						sendDebugMsg("&1&n4 B");
						
						
						
						
						
						GameMode invGameMode = (inv.getTitle().contains("'s S") ? GameMode.SURVIVAL : (inv.getTitle().contains("'s C") ? GameMode.CREATIVE : (inv.getTitle().contains("'s A") ? GameMode.ADVENTURE : null)));
						sendDebugMsg("&1&n=====&r &e'invGameMode' == &f" + (invGameMode != null ? invGameMode.name().toLowerCase() : "null"));
						sendDebugMsg("The owners' gamemode is NOT the same as 'gm'! Owner's gamemode: &f" + owner.getGameMode().name().toLowerCase() + "&r&e; 'gm': &f" + gm.name().toLowerCase());
						World ownersWorld = getWhatWorldToUseFromWorld(getWorldFromPlayerInvInfo(owner, removeFromList));
						if(ownersWorld.getName().equals(world.getName())) {
							sendDebugMsg("&1&n5 A");
							sendDebugMsg("The owner is in the 'world'(&f" + world.getName() + "&r&e)!");
							String FolderName = "Inventories" + File.separatorChar + owner.getName();
							String fileNameToSaveTo = world.getName().toLowerCase().replaceAll(" ", "_") + (invGameMode.equals(GameMode.SURVIVAL) ? ".survival" : (invGameMode.equals(GameMode.CREATIVE) ? ".creative" : (invGameMode.equals(GameMode.ADVENTURE) ? ".adventure" : ".UNKNOWN")));
							String invName = "";
							sendDebugMsg("&1&n=====&r &eThe inventory's gamemode does not match the owners' gamemode! We shall just have to act as if the owner weren't here....");
							if(invType.equalsIgnoreCase("inv")) {
								sendDebugMsg("&1&n6_1 A");
								fileNameToSaveTo += ".inv";
								invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Inventory";
							} else if(invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest")) {
								sendDebugMsg("&1&n6_1 B");
								fileNameToSaveTo += ".enderInv";
								invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Ender Chest";
							} else if(invType.equalsIgnoreCase("extra")) {
								sendDebugMsg("&1&n6_1 C");
								fileNameToSaveTo += ".extraChestInv";
								invName = ownerName + "'s " + getFirstLetterOfGameMode(invGameMode) + " Extra Inventory";
							}
							sendDebugMsg("&1&n7_1 A");
							sendDebugMsg("'fileNameToSaveTo' == &f" + fileNameToSaveTo);
							boolean success = FileMgmt.WriteToFile(fileNameToSaveTo, InventoryAPI.serializeInventory(InventoryAPI.setTitle(invName, inv)), true, FolderName, dataFolderName);
							sendDebugMsg("The plugin has " + (success ? "successfully written to" : "failed to write to") + " the file &z&f" + dataFolderName + File.separatorChar + FolderName + File.separatorChar + fileNameToSaveTo + "&z&r&e for the inventory \"&f" + invName + "&r&e\"! The owners' inventory has NOT been updated[the owner's gamemode(\"&f" + gm.name().toLowerCase() + "&e\") != the inventory's gamemode(\"&f" + invGameMode.name().toLowerCase() + "&e\")!].");
							updateViewersInvScreens(owner, inv, invGameMode, invType);//Update all online players' top inventory screens(if it's the owners') to the 'inv' inventory!
							sendDebugMsg("&1&n8");
						} else {
							sendDebugMsg("&1&n5 B");
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n'world': &r&f" + world.getName());
							sendDebugMsg("&1&n'ownersWorld': &r&f" + ownersWorld.getName());
							sendDebugMsg("&1&n=====ATTENTION=====");
							sendDebugMsg("&1&n=====ATTENTION=====");
						/**/sendDebugMsg("&1&n=====&r &eWell, now what??");
						}
						return;
						
						
						
						
						
						
						
						
						
						
						
					}
				} else {// if(loadByGameMode == false) {
					sendDebugMsg("&1&n3 B");
					World ownersWorld = getWhatWorldToUseFromWorld(owner.getWorld());
					if(ownersWorld.getName().equals(world.getName())) {
						sendDebugMsg("&1&n4 A");
						sendDebugMsg("The owner is in the 'world'(&f" + world.getName() + "&r&e)!");
						String FolderName = "Inventories" + File.separatorChar + owner.getName();
						String fileNameToSaveTo = world.getName().toLowerCase().replaceAll(" ", "_");
						String invName = "";
						boolean Continue = true;
						if(invType.equalsIgnoreCase("inv")) {
							sendDebugMsg("&1&n5 A");
							fileNameToSaveTo += ".inv";
							invName = ownerName + "'s Inventory";
						} else if(invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest")) {
							sendDebugMsg("&1&n5 B");
							fileNameToSaveTo += ".inv";
							invName = ownerName + "'s Ender Chest";
						} else if(invType.equalsIgnoreCase("extra")) {
							sendDebugMsg("&1&n5 C");
							fileNameToSaveTo += ".inv";
							invName = ownerName + "'s Extra Inventory";
						} else {
							sendDebugMsg("&1&n5 D");
							Continue = false;
						}
						if(Continue) {
							sendDebugMsg("&1&n6 A");
							sendDebugMsg("'fileNameToSaveTo' == &f" + fileNameToSaveTo);
							boolean success = FileMgmt.WriteToFile(fileNameToSaveTo, InventoryAPI.serializeInventory(InventoryAPI.setTitle(invName, inv)), true, FolderName, dataFolderName);
							sendDebugMsg("&1&n=====&r &eThe plugin has " + (success ? "successfully written to" : "failed to write to") + " the file \"&z&f" + dataFolderName + File.separatorChar + FolderName + File.separatorChar + fileNameToSaveTo + "&z&r&e\" for the inventory \"&f" + invName + "&r&e\"!" + (invType.equalsIgnoreCase("inv") ? " The owners' inventory has been updated." : ""));
							if(invType.equalsIgnoreCase("inv")) {
								owner.getInventory().setContents(inv.getContents());
							}
							updateViewersInvScreens(owner, inv, null, invType);//Update all online players' top inventory screens(if it's the owners') to the 'inv' inventory!
						} else {
							sendDebugMsg("&1&n6 B");
							sendDebugMsg("The invType wasn't one of the four applicable strings! Why is it set to \"&f" + invType + "&r&e\"???");
							return;
						}
					} else {
						sendDebugMsg("&1&n4 B");
						sendDebugMsg("&1&n=====ATTENTION=====");
						sendDebugMsg("&1&n=====ATTENTION=====");
						sendDebugMsg("&1&n'world': &r&f" + world.getName());
						sendDebugMsg("&1&n'ownersWorld': &r&f" + ownersWorld.getName());
						sendDebugMsg("&1&n=====ATTENTION=====");
						sendDebugMsg("&1&n=====ATTENTION=====");
					}
				}
				sendDebugMsg("&1&nThe End!");
			}
		});
		sendDebugMsg("&1&nEnd of updateOwnersInvAndSave(); beginning of public void run()...");
		return inv;
	}
	
	String getFirstLetterOfGameMode(GameMode gm) {
		return gm.name().substring(0, 1).toUpperCase();
	}
	
	String sendDebugMsg(String str) {
		return (forceDebugMsgs ? sendConsoleMessage(pluginName + "&eDebug: " + str) : EPLib.formatColorCodes(pluginName + "&eDebug: " + str));
	}
	
	Inventory updateViewersInvScreens(final Player owner, final Inventory inv, final GameMode gm, final String invType) {
		server.getScheduler().runTask(plugin, new Runnable() {
			@Override
			public void run() {
				for(Player curPlayer : server.getOnlinePlayers()) {
					Inventory viewersInv = curPlayer.getOpenInventory().getTopInventory();
					if(gm == null) {// if(loadByGameMode == false) {
						String invTitleToUpdateBy = owner.getName() + "'s " + (invType.equalsIgnoreCase("inv") ? "Inventory" : (invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") ? "Ender Chest" : "Extra Inventory"));
						sendDebugMsg("\"&f" + viewersInv.getTitle() + "\"&r&e is supposed to equal \"&f" + invTitleToUpdateBy + "&r&e\" if we are to update it!");
						if(viewersInv.getTitle().equals(invTitleToUpdateBy)) {
							curPlayer.getOpenInventory().getTopInventory().setContents(inv.getContents());
						} else if(curPlayer.getName().equals(owner.getName())) {
							sendDebugMsg("");
						} else {
							sendDebugMsg("...but alas, it does not...");
						}
					} else {// if(loadByGameMode) {
						String gamemode = (gm.equals(GameMode.SURVIVAL) ? "S" : (gm.equals(GameMode.CREATIVE) ? "C" : (gm.equals(GameMode.ADVENTURE) ? "A" : "U")));
						String invTitleToUpdateBy = limitStringToNumOfChars(owner.getName(), 12) + "'s " + (invType.equalsIgnoreCase("inv") ? gamemode + " Inventory" : (invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") ? gamemode + " Ender Chest" : gamemode + " Extra Inventory"));
						sendDebugMsg("\"&f" + viewersInv.getTitle() + "\"&r&e is supposed to equal \"&f" + invTitleToUpdateBy + "&r&e\" if we are to update it!");
						if(viewersInv.getTitle().equals(invTitleToUpdateBy)) {
							curPlayer.getOpenInventory().getTopInventory().setContents(inv.getContents());
						} else {
							sendDebugMsg("...but alas, it does not...");
						}
					}
				}
			}
		});
		return inv;
	}
	
	String limitStringToNumOfChars(String str, int limit) {
		return (str != null ? (str.length() >= 1 ? (str.substring(0, (str.length() >= limit ? limit : str.length()))) : "") : "");
	}
	
}