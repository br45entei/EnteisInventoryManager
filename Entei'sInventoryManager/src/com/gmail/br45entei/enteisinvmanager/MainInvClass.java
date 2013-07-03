package com.gmail.br45entei.enteisinvmanager;

import java.io.File;
import java.util.ArrayList;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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
	public static String pluginName = EPLib.rwhite + "["+ EPLib.green + "Entei's Inventory Manager" + EPLib.rwhite + "] ";
	public static String dataFolderName = "";
	public static boolean YamlsAreLoaded = false;
	public static FileConfiguration config;
	public static File configFile = null;
	public static String configFileName = "config.yml";
	public static ArrayList<Player> playersUsingInventories = new ArrayList<Player>();
	public static ArrayList<String> playersViewingInventories = new ArrayList<String>();
	
	// TODO To be loaded from config.yml
	public static boolean showDebugMsgs;
	public static String noPerm = "";
	public static boolean worldsHaveSeparateInventories = false;
	public static boolean manageExp = false;
	public static boolean loadByGameMode = false;
	
	// TODO Functions
 	public void LoginListener(MainInvClass JavaPlugin) {
		getServer().getPluginManager().registerEvents(this, plugin);
	}
	@Override
	public void onDisable() {
		sendConsoleMessage(pluginName + "&eSaving all online players' inventories...");
		for(Player curPlayer : Bukkit.getServer().getOnlinePlayers()) {
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
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		console = getServer().getConsoleSender();
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
		Inventory blankInv = Bukkit.getServer().createInventory(player, InventoryType.PLAYER);
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
				player.getEnderChest().setContents(Bukkit.getServer().createInventory(player, InventoryType.ENDER_CHEST).getContents());
				if(manageExp) {
					player.setLevel(0);
					player.setExp(0);
				}
			}



			try{player.getInventory().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player).getContents());
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + invFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the gamemode-specific version of this file; if unsuccessful, will save over it from the player's current inventory instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					invFileName = (worldName + "" + player.getGameMode().name().toLowerCase() + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + invFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						invFileName = (worldName + ".inv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
				} else {
					invFileName = (worldName + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player).getContents());
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
					try{player.getEnderChest().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = (worldName + ".enderInv"); //Intentional swappage
					}
					FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
				} else {
					enderFileName = (worldName + ".enderInv"); //Intentional swappage
					try{player.getEnderChest().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = (worldName + "." + player.getGameMode().name().toLowerCase() + ".enderInv"); //Intentional swappage
					}
					FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
				}
				//End smart loading.
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
		Player player = evt.getPlayer();
		GameMode oldGameMode = player.getGameMode();
		GameMode newGameMode = evt.getNewGameMode();
		if(evt.isCancelled() == false) {
			savePlayerInventory(player, player.getWorld(), oldGameMode);
			loadPlayerInventory(player, player.getWorld(), newGameMode, true);
		}
	}
	/**@param evt PlayerChangedWorldEvent
	 */
	@EventHandler(priority=EventPriority.LOWEST) 
	private void onPlayerChangedWorldEvent(PlayerChangedWorldEvent evt) {
		Player player = evt.getPlayer();
		World newWorld = player.getWorld();
		World oldWorld = evt.getFrom();
		if(worldsHaveSeparateInventories) {
			//Save the old inventory to disk
			savePlayerInventory(player, oldWorld);
			//Load the new inventory from disk
			loadPlayerInventory(player, newWorld, false);
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
		String primaryWorldName = "";
		boolean savedToWorld = false;
		boolean loadedFromWorld = false;
		String oldWorldName = "";
		if(loadSaveOrBoth.equalsIgnoreCase("save") || loadSaveOrBoth.equalsIgnoreCase("both")) {oldWorldName = oldWorld.getName();}
		boolean Continue = false;
		int numOfLists = 0;
		try{numOfLists = config.getInt("numberOfLists");
			Continue = true;
		} catch (Exception e) {
			Continue = false;
			e.printStackTrace();
		}
		if(Continue) {
			EPLib.showDebugMsg(pluginName + "&a'WorldTo': \"&6" + player.getWorld().getName() + "&a\"", showDebugMsgs);
			EPLib.showDebugMsg(pluginName + "&a'WorldFrom': \"&6" + oldWorldName + "&a\"", showDebugMsgs);
			if(loadSaveOrBoth.equalsIgnoreCase("save") || loadSaveOrBoth.equalsIgnoreCase("both")) {
				for(int num = 1; num <= numOfLists; num++) {
					EPLib.showDebugMsg(pluginName + "&2SAVING: &aDebug: num = " + num, showDebugMsgs);
					String worldNameList = config.getString("list_" + num);
					if(worldNameList != null) {
						if(worldNameList.split("\\|").length >= 1) {
							boolean firstRunIsOver = false;
							for(String curWorldName : worldNameList.split("\\|")) {
								if(savedToWorld) {
									EPLib.showDebugMsg(pluginName + "&2SAVING:&a savedToWorld = true", showDebugMsgs);
									break;
								}
								EPLib.showDebugMsg(pluginName + "&2SAVING:&r &a'curWorldName' = \"&6" + curWorldName + "&a\"", showDebugMsgs);
								if(curWorldName.equals("") == false) {
									if(firstRunIsOver == false) {
										if(Bukkit.getWorld(curWorldName) != null) {
											primaryWorldName = curWorldName;
											firstRunIsOver = true;
										}
									}
									if(oldWorld.getName().equalsIgnoreCase(curWorldName) || oldWorld.getName().equalsIgnoreCase(primaryWorldName)) {
										savePlayerInventory(player, Bukkit.getServer().getWorld(primaryWorldName));
										sendConsoleMessage(pluginName + (primaryWorldName.equalsIgnoreCase(oldWorld.getName()) ? "&2SAVING: &aSaved player \"&f" + player.getName() + "&a\"'s inventory for world: \"&6" + primaryWorldName + "&a\"." : "&2SAVING: &aSaved player \"&f" + player.getName() + "&a\"'s inventory to world: \"&6" + primaryWorldName + "&a\" instead of saving to world \"&6" + oldWorld.getName() + "&a\"."));
										savedToWorld = true;
									}
								}
							}
							if(savedToWorld == true) {
								break;
							}
						} else {
							EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
						}
					} else {
						EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
					}
				}
			}// else {print("This is a player join event, isn't it?");}
			if(loadSaveOrBoth.equalsIgnoreCase("load") || loadSaveOrBoth.equalsIgnoreCase("both")) {
				for(int num = 1; num <= numOfLists; num++) {
					EPLib.showDebugMsg(pluginName + "&5&nLOADING:&r &aDebug: num = " + num, showDebugMsgs);
					String worldNameList = config.getString("list_" + num);
					if(worldNameList != null) {
						if(worldNameList.split("\\|").length >= 1) {
							boolean firstRunIsOver = false;
							for(String curWorldName : worldNameList.split("\\|")) {
								if(loadedFromWorld) {
									EPLib.showDebugMsg(pluginName + "&5&nLOADING:&r&a loadedFromWorld = true", showDebugMsgs);
									break;
								}
								EPLib.showDebugMsg(pluginName + "&5&nLOADING:&r &a'curWorldName' = \"&6" + curWorldName + "&a\"", showDebugMsgs);
								if(curWorldName.equals("") == false) {
									if(firstRunIsOver == false) {
										if(Bukkit.getWorld(curWorldName) != null) {
											primaryWorldName = curWorldName;
											firstRunIsOver = true;
										}
									}
									if(newWorld.getName().equalsIgnoreCase(curWorldName) || newWorld.getName().equalsIgnoreCase(primaryWorldName)) {
										loadPlayerInventory(player, Bukkit.getServer().getWorld(primaryWorldName), true);
										if(primaryWorldName.equalsIgnoreCase(newWorld.getName()) == false) {
											sendConsoleMessage(pluginName + "&5&nLOADING:&r &aLoaded player \"&f" + player.getName() + "&a\"'s inventory from world: \"&6" + primaryWorldName + "&a\" instead of loading from world \"&6" + newWorld.getName() + "&a\".");
										} else {
											sendConsoleMessage(pluginName + "&5&nLOADING:&r &aLoaded player \"&f" + player.getName() + "&a\"'s inventory for world: \"&6" + primaryWorldName + "&a\".");
										}
										loadedFromWorld = true;
									} else {
										//sendConsoleMessage(pluginName + "&5&nLOADING:&r &enewWorld: \"&6" + newWorld.getName() + "&e\"; curWorldName: \"&6" + curWorldName + "&e\"");
									}
								}
							}
							if(loadedFromWorld == true) {
								break;
							}
						} else {
							EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
						}
					} else {
						EPLib.unSpecifiedVarWarning("list_" + num, configFileName, pluginName);
					}
				}
			}// else {print("This is a player quit event, isn't it?");}
		} else {
			EPLib.unSpecifiedVarWarning("numberOfLists", configFileName, pluginName);
		}
	}
	static String sendConsoleMessage(String msg) {
		return EPLib.sendConsoleMessage(msg);
	}
	private static String sendMessage(CommandSender target, String msg) {
		return EPLib.sendMessage(target,  msg);
	}
	private static String sendMessage(Player target, String msg) {
		return EPLib.sendMessage(target, msg);
	}
	public boolean LoadConfig() {
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
	public boolean reloadFiles(boolean ShowStatus) {
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
	public boolean saveYamls() {
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
	public static boolean loadYamlVariables() {
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
		Player user = Bukkit.getServer().getPlayer(sender.getName());
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
		} else if(command.equalsIgnoreCase("view")) {// TODO Re-do this whole section, be sure to leave the non-gamemode section(s) alone, as they are good as-is.(make sure of that, though...)
			if(user != null) {
				if(loadByGameMode == false) {
					if(args.length == 1) {
						if(args[0].equals("inv")) {
							user.openInventory(user.getInventory());
						} else if(args[0].equalsIgnoreCase("ender") || args[0].equalsIgnoreCase("enderchest")) {
							user.openInventory(user.getEnderChest());
						} else if(args[0].equalsIgnoreCase("extra")) {
							user.openInventory(getPlayerExtraChest(user));
						} else {
							sendMessage(sender, pluginName + "&eUsage: /" + command + " {inv|ender|extra} or /" + command + " {playerName} {inv|ender|extra}");
						}
					} else if(args.length == 2) {
						Player target = Bukkit.getServer().getPlayer(args[0]);
						if(target != null) {
							if(args[1].equalsIgnoreCase("inv")) {
								if(user.hasPermission("eim.*") || user.hasPermission("eim.view.inv.*") || user.hasPermission("eim.view.inv." + target.getName())) {
									user.openInventory(target.getInventory());
								} else {
									sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their inventory.");
									sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your inventory.");
								}
							} else if(args[1].equalsIgnoreCase("ender") || args[1].equalsIgnoreCase("enderchest")) {
								if(user.hasPermission("eim.*") || user.hasPermission("eim.view.ender.*") || user.hasPermission("eim.view.ender." + target.getName())) {
									user.openInventory(target.getEnderChest());
								} else {
									sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their ender chest.");
									sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your ender chest.");
								}
							} else if(args[1].equalsIgnoreCase("extra")) {
								if(user.hasPermission("eim.*") || user.hasPermission("eim.view.extra.*") || user.hasPermission("eim.view.extra." + target.getName())) {
									user.openInventory(getPlayerExtraChest(target));
								} else {
									sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their extra inventory.");
									sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your extra chest.");
								}
							} else {
								sendMessage(sender, pluginName + "&eUsage: /" + command + " {inv|ender|extra} or /" + command + " {playerName} {inv|ender|extra}");
							}
						} else {
							sendMessage(sender, pluginName + "&eThe player \"&f" + args[0] + "&e\" is not online or does not exist!");
						}
					} else {
						sendMessage(sender, pluginName + "&eUsage: /" + command + " {inv|ender|extra} or /" + command + " {playerName} {inv|ender|extra}");
					}
					return true;
				}
				if(loadByGameMode) {
					if(args.length == 3) {
						Player target = Bukkit.getServer().getPlayer(args[0]);
						if(target != null) {
							String gamemode = (args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? ".survival" : ((args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? ".creative" : ((args[1].equalsIgnoreCase("adventure") || args[1].equals("2") ? ".adventure" : "INVALID")))));
							if(gamemode.equals("INVALID")) {
								sendMessage(sender, pluginName + "&e\"&f" + args[1] + "&r&e\" is not a valid gamemode. You can enter 0-2 or the word, without spaces. Example: \"creative\"");
								sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
								return true;
							}
							if(args[2].equalsIgnoreCase("inv")) {
								if(target.equals(user) == false) {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.inv.*") || user.hasPermission("eim.view.inv" + gamemode + "." + target.getName())) {
										user.openInventory(target.getInventory());
									} else {
										sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their " + gamemode.replace(".", "") + " inventory.");
										sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your inventory.");
									}
								} else {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.inv.*") || user.hasPermission("eim.view.inv" + gamemode)) {
										user.openInventory(user.getInventory());
									} else {
										sendMessage(sender, pluginName + "&eSorry, but you don't have permission to view your " + gamemode.replace(".", "") + " inventory when you aren't in &c" + gamemode.replace(".", "") + "&f&e mode.");
									}
								}
							} else if(args[2].equalsIgnoreCase("ender") || args[2].equalsIgnoreCase("enderchest")) {
								if(target.equals(user) == false) {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.ender.*") || user.hasPermission("eim.view.ender" + gamemode + "." + target.getName())) {
										user.openInventory(target.getEnderChest());
									} else {
										sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their " + gamemode.replace(".", "") + " ender chest.");
										sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your ender chest.");
									}
								} else {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.ender.*") || user.hasPermission("eim.view.ender" + gamemode)) {
										user.openInventory(user.getInventory());
									} else {
										sendMessage(sender, pluginName + "&eSorry, but you don't have permission to view your " + gamemode.replace(".", "") + " ender chest when you aren't in &c" + gamemode.replace(".", "") + "&f&e mode.");
									}
								}
							} else if(args[2].equalsIgnoreCase("extra")) {
								if(target.equals(user) == false) {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.extra.*") || user.hasPermission("eim.view.extra" + gamemode + "." + target.getName())) {
										user.openInventory(getPlayerExtraChest(target, GameMode.getByValue(gamemode.equalsIgnoreCase(".survival") ? 0 : (gamemode.equalsIgnoreCase(".creative") ? 1 : 2))));
									} else {
										sendMessage(sender, pluginName + "&eSorry, but &f" + target.getDisplayName() + "&r&e hasn't given you permission to view their extra " + gamemode.replace(".", "") + " inventory.");
										sendMessage(target, pluginName + "&f" + userName + "&r&e just tried to view your extra chest.");
									}
								} else {
									if(user.hasPermission("eim.*") || user.hasPermission("eim.view.extra.*") || user.hasPermission("eim.view.extra" + gamemode)) {
										user.openInventory(user.getInventory());
									} else {
										sendMessage(sender, pluginName + "&eSorry, but you don't have permission to view your extra " + gamemode.replace(".", "") + " chest inventory when you aren't in &c" + gamemode.replace(".", "") + "&f&e mode.");
									}
								}
							} else {
								sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
							}
							return true;
						}
						sendMessage(sender, pluginName + "&eThe player \"&f" + args[0] + "&e\" is not online or does not exist!");
						return true;
					} else if(args.length == 2) {
						String gamemode = (args[0].equalsIgnoreCase("survival") || args[0].equals("0") ? "survival" : ((args[0].equalsIgnoreCase("creative") || args[0].equals("1") ? "creative" : ((args[0].equalsIgnoreCase("adventure") || args[0].equals("2") ? "adventure" : "INVALID")))));
						if(gamemode.equals("INVALID")) {
							sendMessage(sender, pluginName + "&e\"&f" + args[1] + "&r&e\" is not a valid gamemode. You can enter 0-2 or the word, without spaces. Example: \"creative\"");
							sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
							return true;
						}
						if(user.getGameMode().name().equalsIgnoreCase(gamemode)) {
							if(args[1].equalsIgnoreCase("inv")) {
								user.openInventory(user.getInventory());
							} else if(args[1].equalsIgnoreCase("ender") || args[1].equalsIgnoreCase("enderchest")) {
								user.openInventory(user.getEnderChest());
							} else if(args[1].equalsIgnoreCase("extra")) {
								user.openInventory(getPlayerExtraChest(user, GameMode.getByValue(gamemode.equalsIgnoreCase("survival") ? 0 : (gamemode.equalsIgnoreCase("creative") ? 1 : 2))));
							} else {
								sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
							}
						} else {
							Inventory invToView = null;
							String worldName = user.getWorld().getName().toLowerCase().replaceAll(" ", "_");
							String playerName = user.getName();
							String FolderName = "Inventories" + File.separatorChar + playerName;
							String invFileName = (worldName + "." + gamemode + ".inv");
							//String armorFileName = (worldName + "." + gamemode + ".armorInv");
							String enderFileName = (worldName + "." + gamemode + ".enderInv");
							//String expFileName = (worldName + "." + gamemode + ".exp");
							if(args[1].equalsIgnoreCase("inv")) {
								try{
									invToView = InventoryAPI.setTitle(user.getName() + "'s " + EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Inventory", InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), user));
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else if(args[1].equalsIgnoreCase("ender") || args[1].equalsIgnoreCase("enderchest")) {
								try{
									invToView = InventoryAPI.setTitle(user.getName() + "'s " + EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Ender Chest", InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), user));
								} catch (Exception e) {
									e.printStackTrace();
								}
								
							} else if(args[1].equalsIgnoreCase("extra")) {
								try{
									invToView = InventoryAPI.setTitle(user.getName() + "'s " + EPLib.capitalizeFirstLetter(gamemode).substring(0, 1) + " Extra Inventory", getPlayerExtraChest(user, GameMode.getByValue(gamemode.equalsIgnoreCase("survival") ? 0 : (gamemode.equalsIgnoreCase("creative") ? 1 : 2))));
								} catch (Exception e) {
									e.printStackTrace();
								}
							} else {
								sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
								return true;
							}
							if(invToView != null) {
								if(user.hasPermission("eim.view.gamemode." + gamemode + "." + args[1].toLowerCase())) {
									//final String invTitle = invToView.getTitle();
									playersViewingInventories.add(user.getName());
									user.openInventory(invToView);
								}
							} else {
								sendMessage(user, pluginName + "&eUnable to open your &f" + gamemode + "&e inventory. Have you been in " + gamemode + " mode for this world yet?");
								return true;
							}
						}
					} else if(args.length == 1) {
						if(args[0].equalsIgnoreCase("inv")) {
							user.openInventory(user.getInventory());
						} else if(args[0].equalsIgnoreCase("ender") || args[0].equalsIgnoreCase("enderchest")) {
							user.openInventory(user.getEnderChest());
						} else if(args[0].equalsIgnoreCase("extra")) {
							user.openInventory(getPlayerExtraChest(user, user.getGameMode()));
						} else {
							sendMessage(sender, pluginName + "&eUsage: /" + command + " {survival|creative|adventure} {inv|ender|extra} or /" + command + " {playerName} {survival|creative|adventure} {inv|ender|extra}");
						}
					} else {
						sendMessage(sender, pluginName + "&eUsage: /" + command + " {inv|ender|extra} or /" + command + " {playerName} {inv|ender|extra}");
					}
				}
				return true;
			}
			// TODO /view for console
			
			
			
			
			
			sendMessage(sender, pluginName + "&e/" + command + " is used to display a player's inventory. When used by the console, it is used to display the targeted players' inventory on their screen.");
			
			
			
			
			
			
			return true;
		} else if(command.equalsIgnoreCase("invperm")) {
			RegisteredServiceProvider<Permission> permProvider = null;
			Permission permission = null;
			if(EPLib.vaultIsAvailable) {
				permProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
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
				if(args.length == 4) {
					//if(args[0].equalsIgnoreCase("give")) {
					if(args[1].equalsIgnoreCase("view") || args[1].equalsIgnoreCase("edit")) {
						if(args[2].equalsIgnoreCase("inv") || args[2].equalsIgnoreCase("ender") || args[2].equalsIgnoreCase("enderchest") || args[2].equalsIgnoreCase("extra")) {
							Player target = Bukkit.getServer().getPlayer(args[3]);
							if(target != null) {
								if(args[0].equalsIgnoreCase("give")) {
									permission.playerAdd(Bukkit.getServer().getPlayer(sender.getName()).getWorld(), target.getName(), "eim." + (args[1].equalsIgnoreCase("view") ? "view" : "edit") + "." + (args[2].equalsIgnoreCase("inv") ? "inv" : ((args[2].equalsIgnoreCase("ender") || args[2].equalsIgnoreCase("enderchest")) ? "ender" : "extra")) + "." + sender.getName());
								} else if(args[0].equalsIgnoreCase("take")) {
									permission.playerRemove(Bukkit.getServer().getPlayer(sender.getName()).getWorld(), target.getName(), "eim." + (args[1].equalsIgnoreCase("view") ? "view" : "edit") + "." + (args[2].equalsIgnoreCase("inv") ? "inv" : ((args[2].equalsIgnoreCase("ender") || args[2].equalsIgnoreCase("enderchest")) ? "ender" : "extra")) + "." + sender.getName());
								} else {
									sendInvPermUsage(sender, command);
								}
								return true;
							}
							sendNotOnlineMessage(sender, args[3]);
							return true;
						}
						sendInvPermUsage(sender, command);
						return true;
					}
					sendInvPermUsage(sender, command);
					return true;
				}
				sendInvPermUsage(sender, command);
				return true;
			}
			//Used by Console
			if(args.length == 5) {
				Player owner = Bukkit.getServer().getPlayer(args[0]);
				if(owner != null) {
					if(args[2].equalsIgnoreCase("view") || args[2].equalsIgnoreCase("edit")) {
						if(args[3].equalsIgnoreCase("inv") || args[3].equalsIgnoreCase("ender") || args[3].equalsIgnoreCase("enderchest") || args[3].equalsIgnoreCase("extra")) {
							Player target = Bukkit.getServer().getPlayer(args[4]);
							if(target != null) {
								String Perm = "eim." + (args[2].equalsIgnoreCase("view") ? "view" : "edit") + "." + (args[3].equalsIgnoreCase("inv") ? "inv" : ((args[3].equalsIgnoreCase("ender") || args[3].equalsIgnoreCase("enderchest")) ? "ender" : "extra")) + "." + sender.getName();
								String targetMsg = "&f" + sender.getName() + "&b has just " + (args[1].equalsIgnoreCase("give") ? "given you permission to " : "taken away your permission to ") + (args[2].equalsIgnoreCase("view") ? "view" : "edit") + " &f" + owner.getName() + "&b's " + (args[3].equalsIgnoreCase("inv") ? "inventory" : ((args[3].equalsIgnoreCase("ender") || args[3].equalsIgnoreCase("enderchest")) ? "ender chest" : "extra inventory")) + "!";
								String senderMessage = "&bYou have just " + (args[1].equalsIgnoreCase("give") ? "given &f" + target.getName() + "&b permission to " : "taken away &f" + target.getName() + "&b's permission to ") + (args[2].equalsIgnoreCase("view") ? "view" : "edit") + " &f" + owner.getName() + "&b's " + (args[3].equalsIgnoreCase("inv") ? "inventory" : ((args[3].equalsIgnoreCase("ender") || args[3].equalsIgnoreCase("enderchest")) ? "ender chest" : "extra inventory")) + "!";
								String ownerMessage = "&f" + (sender.getName().equals(EPLib.console.getName()) ? EPLib.consoleSayFormat : "&e" + sender.getName().trim() + "&r ") + "&bhas just " + (args[1].equalsIgnoreCase("give") ? "given &f" + target.getName() + "&b permission to " : "taken away &f" + target.getName() + "&b's permission to ") + (args[2].equalsIgnoreCase("view") ? "view" : "edit") + " your " + (args[3].equalsIgnoreCase("inv") ? "inventory" : ((args[3].equalsIgnoreCase("ender") || args[3].equalsIgnoreCase("enderchest")) ? "ender chest" : "extra inventory")) + "!";
								if(args[1].equalsIgnoreCase("give")) {
									permission.playerAdd(Bukkit.getServer().getPlayer(sender.getName()).getWorld(), target.getName(), Perm);
									sendMessage(target, pluginName + targetMsg);
									sendMessage(owner, pluginName + ownerMessage);
									sendMessage(sender, pluginName + senderMessage);
								} else if(args[1].equalsIgnoreCase("take")) {
									permission.playerRemove(Bukkit.getServer().getPlayer(sender.getName()).getWorld(), target.getName(), Perm);
									sendMessage(target, pluginName + targetMsg);
									sendMessage(owner, pluginName + ownerMessage);
									sendMessage(sender, pluginName + senderMessage);
								} else {
									sendInvPermUsage(sender, command);
								}
								return true;
							}
							sendNotOnlineMessage(sender, args[4]);
							return true;
						}
						sendInvPermUsage(sender, command);
						return true;
					}
					sendInvPermUsage(sender, command);
					return true;
				}
				sendNotOnlineMessage(sender, args[0]);
				return true;
			}
			sendInvPermUsageForConsole(sender, command);
			return true;
		} else {
			return false;
		}
	}
	private static void sendInvPermUsage(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {give|take} {view|edit} {inv|ender|extra} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} your {inv|ender|extra} Inventory(View this with \"&f/view&e\".");
	}
	private static void sendInvPermUsageForConsole(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {ownerName} {give|take} {view|edit} {inv|ender|extra} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} the {ownerName}'s {inv|ender|extra} Inventory.");
	}
	private static void sendNotOnlineMessage(CommandSender sender, String arg) {
		sendMessage(sender, pluginName + "&e\"&f" + arg + "&e\" is not a player, or is not online.");
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryClickEvent(InventoryClickEvent evt) {//For checking perms and updating inventory screens.
		EPLib.showDebugMsg(pluginName + "&bDebug: The onInventoryClickEvent Event has fired!", showDebugMsgs);
		Inventory sourceInv = evt.getInventory();
		if(sourceInv.getHolder() instanceof Player) {
			Player owner =  (Player) sourceInv.getHolder();
			Player editor = (Player) evt.getWhoClicked();
			boolean playerIsntViewingOtherType = true;
			for(String curPlayerName : playersViewingInventories) {
				if(curPlayerName.equals(editor.getName())) {
					playerIsntViewingOtherType = false;
				}
			}
			if(playerIsntViewingOtherType) {
				EPLib.showDebugMsg("&eDebug: Running InventoryClickEvent normally...", true);
				if(sourceInv.getTitle().contains("'s Extra Inventory")) {
					EPLib.showDebugMsg(pluginName + "&bDebug: Test 1", showDebugMsgs);
					final String ownerName = owner.getName();
					final String editorName = editor.getName();
					if(owner.getName().equals(editor.getName()) == false) {
						EPLib.showDebugMsg(pluginName + "&bDebug: Test 1: \"&a" + ownerName + "&b\" &c!=&b \"&a" + editorName + "&b\".", showDebugMsgs);
						if(editor.hasPermission("eim.edit.extra." + owner.getName()) == false && editor.hasPermission("eim.edit.extra.others") == false && editor.hasPermission("eim.*") == false && editor.hasPermission("eim.edit.*") == false) {
							EPLib.showDebugMsg(pluginName + "&bDebug: Test 2: Permissions: &cFalse", showDebugMsgs);
							final InventoryClickEvent evt1 = evt;
							Bukkit.getServer().getScheduler().runTask(this, new Runnable() {
								@Override
								public void run() {
									EPLib.showDebugMsg(pluginName + "&bDebug: Closing inventory of \"&a" + editorName + "&b\".", showDebugMsgs);
									evt1.getView().close();
								}
							});
							evt.setCancelled(true);
							sendMessage(editor, pluginName + "&eYou do not have permission to edit \"&f" + owner.getDisplayName() + "&r&e\"'s extra inventory!");
							return;
						}
						EPLib.showDebugMsg(pluginName + "&bDebug: Test 2: Permissions: &2True", showDebugMsgs);
					} else {
						//The owner of the extra inventory should have permission to edit it!
						//If a check is needed here, then add it. Otherwise, it's fine.
						EPLib.showDebugMsg(pluginName + "&bDebug: Test 1: \"&a" + ownerName + "&b\" &2==&b \"&a" + editorName + "&b\"", showDebugMsgs);
					}
					final Inventory inv = sourceInv;
					EPLib.showDebugMsg(pluginName + "&bDebug: Test 3", showDebugMsgs);
					final ArrayList<Player> finalplayersUsingInventories = playersUsingInventories;
					Bukkit.getServer().getScheduler().runTask(this, new Runnable() {
						@Override
						public void run() {
							for(Player curPlayer : finalplayersUsingInventories) {
								EPLib.showDebugMsg(pluginName + "&bDebug: Test 4: curPlayer: \"&a" + curPlayer.getName() + "&b\"", showDebugMsgs);
								curPlayer.getOpenInventory().getTopInventory().setContents(inv.getContents());
							}
						}
					});
				} else {
					EPLib.showDebugMsg(pluginName + "&bDebug: \"&a" + sourceInv.getTitle() + "&b\" does not contain \"&a's Extra Inventory&b\".", showDebugMsgs);
				}
			} else {
				// TODO This is for when a player used the "/view {gamemode} {inv|ender|extra}" command!
				//Player arg for here: 'editor'
				sendConsoleMessage("&aDebug: The inventory title that &f" + editor.getDisplayName() + "&r&a is clicking in is: &e" + sourceInv.getTitle());
			}
		} else {
			sendConsoleMessage(pluginName + "The source inventory(named \"" + sourceInv.getTitle() + "\")'s holder was not an instance of a player...");
		}
		EPLib.showDebugMsg(pluginName + "&bDebug: End of event.", showDebugMsgs);
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryOpenEvent(InventoryOpenEvent evt) {//For checking perms.
		Player player = Bukkit.getServer().getPlayer(evt.getPlayer().getName());
		Inventory inv = evt.getInventory();
		String invName = inv.getTitle();
		boolean playerIsntViewingOtherType = true;
		for(String curPlayerName : playersViewingInventories) {
			if(curPlayerName.equals(player.getName())) {
				playerIsntViewingOtherType = false;
			}
		}
		if(playerIsntViewingOtherType) {
			EPLib.showDebugMsg("&eDebug: Running InventoryOpenEvent normally...", true);
			if(invName.equals(player.getName() + "'s Extra Inventory")) {
				playersUsingInventories.add(player);
				EPLib.showDebugMsg(pluginName + "&bDebug: Added &a" + player.getName() + "&b to the list of players who are using a custom inventory screen!", showDebugMsgs);
			} else if(invName.contains("'s Extra Inventory")) {
				EPLib.showDebugMsg(pluginName + "&bDebug: Attempting to scan through all online players and see if we get a match for \"&a" + invName + "&b\".)", showDebugMsgs);
				for(Player curPlayer : Bukkit.getServer().getOnlinePlayers()) {
					if(invName.equals(curPlayer.getName() + "'s Extra Inventory")) {
						if(player.hasPermission("eim.view.extra.others") || player.hasPermission("eim.view.extra." + curPlayer.getName()) || player.hasPermission("eim.*") || player.hasPermission("eim.view.*")) {
							playersUsingInventories.add(player);
							EPLib.showDebugMsg(pluginName + "&aDebug: Added &f" + player.getName() + "&a to the list of players who are using a custom inventory screen!", showDebugMsgs);
						} else {
							sendMessage(player, pluginName + "&eYou do not have permission to view &f" + curPlayer.getName() + "&e's extra inventory.");
							evt.setCancelled(true);
						}
						break;
					}
				}
			}
		} else {
			// TODO This is for when a player used the "/view {gamemode} {inv|ender|extra}" command!
			//Player arg for here: 'player'
			sendConsoleMessage("&aDebug: The inventory title that &f" + player.getDisplayName() + "&r&a has opened is: &e" + inv.getTitle());
		}
	}
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryCloseEvent(InventoryCloseEvent evt) {//For saving.
		Player player = Bukkit.getServer().getPlayer(evt.getPlayer().getName());
		Inventory inv = evt.getInventory();
		String invName = inv.getTitle();
		String worldName = player.getWorld().getName().toLowerCase().replaceAll(" ", "_");
		String playerName = player.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		boolean savedInventory = false;
		String invFileName = "";
		if(loadByGameMode == false) {
			invFileName = (worldName + ".extraChestInv");
		} else {
			invFileName = (worldName + (player.getGameMode() == GameMode.SURVIVAL ? ".survival" : (player.getGameMode() == GameMode.CREATIVE ? ".creative" : ".adventure")) + ".extraChestInv");
		}
		boolean playerIsntViewingOtherType = true;
		for(String curPlayerName : playersViewingInventories) {
			if(curPlayerName.equals(player.getName())) {
				playerIsntViewingOtherType = false;
			}
		}
		if(playerIsntViewingOtherType) {
			EPLib.showDebugMsg("&eDebug: Running InventoryCloseEvent normally...", true);
			if(invName.equals(playerName + "'s Extra Inventory")) {
				EPLib.showDebugMsg("&aSaved player \"&f" + player.getName() + "&a\"'s Inventory (\"&f" + invName + "&a\").", showDebugMsgs);
				EPLib.showDebugMsg("&aDebug: State One(players' own extra chest)", showDebugMsgs);
				savedInventory = FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(inv), true, FolderName, dataFolderName);
				sendMessage(player, (savedInventory ? "&eInventory \"&f" + invName + "&e\" was saved." : "&eUnable to save the inventory \"&f" + invName + "&e\"."));
				playersUsingInventories.remove(player);
				EPLib.showDebugMsg(pluginName + "&eDebug: Removed \"&f" + playerName + "&e\" from the list of players who are using a custom inventory screen!", showDebugMsgs);
			} else if(invName.contains("'s Extra Inventory")) {
				EPLib.showDebugMsg("&aPlayer name: \"&f" + player.getName() + "&a\"; Inventory Title: \"&f" + invName + "&a\"", showDebugMsgs);
				EPLib.showDebugMsg("&eDebug: Expected Inventory Title: \"&f" + (player.getName() + "'s Extra Inventory") + "&e\"", showDebugMsgs);
				EPLib.showDebugMsg("&aDebug: Attempting to scan through all online players and see if we get a match for \"&f" + invName + "&a\".)", showDebugMsgs);
				String ownerName = "";
				for(Player curPlayer : Bukkit.getServer().getOnlinePlayers()) {
					if(invName.equals(curPlayer.getName() + "'s Extra Inventory")) {
						ownerName = curPlayer.getDisplayName();
						if(player.hasPermission("eim.edit.others") || player.hasPermission("eim.edit." + curPlayer.getName()) || player.hasPermission("eim.*") || player.hasPermission("eim.edit.*")) {
							worldName = curPlayer.getWorld().getName().toLowerCase().replaceAll(" ", "_");
							FolderName = "Inventories" + File.separatorChar + curPlayer.getName();
							savedInventory = FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(inv), true, FolderName, dataFolderName);
							sendMessage(player, (savedInventory ? "&eInventory \"&f" + invName + "&e\" was saved." : "&eUnable to save the inventory \"&f" + invName + "&e\"."));
						}
						break;
					}
				}
				sendMessage(player, (savedInventory ? "&eInventory \"&f" + invName + "&e\" was saved." : "&eUnable to save the inventory \"&f" + invName + "&e\"." + (!(ownerName.equals("")) ? "&c(You do not have &f" + ownerName + "&r&c's permission to edit their inventory!)" : "(Did the owner leave while you had their extra inventory open?)")));
				playersUsingInventories.remove(player);
				EPLib.showDebugMsg(pluginName + "&eDebug: Removed \"&f" + playerName + "&e\" from the list of players who are using a custom inventory screen!", showDebugMsgs);
			}
		} else {
			// TODO This is for when a player used the "/view {gamemode} {inv|ender|extra}" command!
			//Player arg for here: 'player'
			sendConsoleMessage("&aDebug: The inventory that &f" + player.getDisplayName() + "&r&a just closed had the title: &e" + inv.getTitle());
			if(inv.getHolder() instanceof Player) {
				Player invOwner = (Player) inv.getHolder();
				boolean ownerIsEditing = true;
				if(invOwner.getName().equals(player.getName()) == false) {
					ownerIsEditing = false;
				}
				if(inv.getTitle().endsWith("'s S Inventory")) {//A player's survival inventory
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.survival.inv") || player.hasPermission("eim.edit.gamemode.survival.inv.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Survival Inventory was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.survival.inv." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.survival.inv.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Survival Inventory was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fSurvival Inventory&c!");
					}
				} else if(inv.getTitle().endsWith("'s C Inventory")) {//A player's creative inventory
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.creative.inv") || player.hasPermission("eim.edit.gamemode.creative.inv.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Creative Inventory was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.creative.inv." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.creative.inv.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Creative Inventory was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fCreative Inventory&c!");
					}
				} else if(inv.getTitle().endsWith("'s A Inventory")) {//A player's adventure inventory
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.adventure.inv") || player.hasPermission("eim.edit.gamemode.adventure.inv.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Adventure Inventory was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.adventure.inv." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.adventure.inv.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.inv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Adventure Inventory was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fAdventure Inventory&c!");
					}
				} else if(inv.getTitle().endsWith("'s S Ender Chest")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.survival.ender") || player.hasPermission("eim.edit.gamemode.survival.ender.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Survival Ender Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.survival.ender." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.survival.ender.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Survival Ender Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fSurvival Ender Chest&c!");
					}
				} else if(inv.getTitle().endsWith("'s C Ender Chest")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.creative.ender") || player.hasPermission("eim.edit.gamemode.creative.ender.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Creative Ender Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.creative.ender." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.creative.ender.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Creative Ender Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fCreative Ender Chest&c!");
					}
				} else if(inv.getTitle().endsWith("'s A Ender Chest")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.adventure.ender") || player.hasPermission("eim.edit.gamemode.adventure.ender.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Adventure Ender Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.adventure.ender." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.adventure.ender.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.enderInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Adventure Ender Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fAdventure Ender Chest&c!");
					}
				} else if(inv.getTitle().endsWith("'s S Extra Inventory")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.survival.extra") || player.hasPermission("eim.edit.gamemode.survival.extra.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Survival Extra Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.survival.extra." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.survival.extra.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".survival.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Survival Extra Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fSurvival Extra Chest&c!");
					}
				} else if(inv.getTitle().endsWith("'s C Extra Inventory")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.creative.extra") || player.hasPermission("eim.edit.gamemode.creative.extra.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Creative Extra Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.creative.extra." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.creative.extra.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".creative.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Creative Extra Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fCreative Extra Chest&c!");
					}
				} else if(inv.getTitle().endsWith("'s A Extra Inventory")) {
					if(ownerIsEditing && (player.hasPermission("eim.edit.gamemode.adventure.extra") || player.hasPermission("eim.edit.gamemode.adventure.extra.*") || player.hasPermission("eim.*"))) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						sendMessage(player, pluginName + "&aYour Adventure Extra Chest was successfully saved!");
					} else if(player.hasPermission("eim.edit.gamemode.adventure.extra." + invOwner.getName()) || player.hasPermission("eim.edit.gamemode.adventure.extra.*") || player.hasPermission("eim.*")) {
						FileMgmt.WriteToFile(invOwner.getWorld().getName().toLowerCase().replaceAll(" ", "_") + ".adventure.extraChestInv", InventoryAPI.serializeInventory(inv), true, "Inventories" + File.separatorChar + invOwner.getName(), dataFolderName);
						invOwner.getInventory().setContents(inv.getContents());
						sendMessage(player, pluginName + "&f" + invOwner.getDisplayName() + "&r&e's Adventure Extra Chest was successfully saved.");
					} else {
						sendMessage(player, pluginName + "&cYou do not have &f" + invOwner.getDisplayName() + "&r&c's permission to edit their &fAdventure Extra Chest&c!");
					}
				} else {
					sendConsoleMessage("&eDebug: The inventory's title wasn't a title made for this situation(how'd it wind up here?): &f" + inv.getTitle());
				}
			} else {
				sendConsoleMessage("&eDebug: The inventory(name: \"&f" + inv.getTitle() + "&r&e\")'s holder was not an instance of a player...");
			}
			playersViewingInventories.remove(player.getName());
		}
	}
	public Inventory getPlayerExtraChest(Player target, GameMode gm) {
		Inventory invToOpen = null;
		String worldName = target.getWorld().getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = (worldName + "." + gm.name().toLowerCase() + ".extraChestInv");
		try{
			invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = Bukkit.getServer().createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}
	public Inventory getPlayerExtraChest(Player target) {
		Inventory invToOpen = null;
		String worldName = target.getWorld().getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = worldName + ".extraChestInv";
		try{
			invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = Bukkit.getServer().createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}
	
}