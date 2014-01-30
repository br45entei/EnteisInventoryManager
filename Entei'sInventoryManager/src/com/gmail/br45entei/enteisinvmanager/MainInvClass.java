package com.gmail.br45entei.enteisinvmanager;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.gmail.br45entei.enteispluginlib.EPLib;
import com.gmail.br45entei.enteispluginlib.FileMgmt;
import com.gmail.br45entei.enteispluginlib.InvalidYamlException;
import com.gmail.br45entei.enteispluginlib.InventoryAPI;

public class MainInvClass extends org.bukkit.plugin.java.JavaPlugin implements org.bukkit.event.Listener {
	public String sendDevMsg(String str) {
		str = pluginName + EPLib.formatColorCodes(str);
		/*if(config.getBoolean("showDebugs") == true) {
			sendConsoleMessage(str);
		}*/
		return str;
	}
	public void DEBUG(String str) {
		if(config.getBoolean("showDebugMsgs") == true) {
			sendConsoleMessage(pluginName + EPLib.formatColorCodes(str));
		}
	}

	private final MainInvClass plugin = this;
	public static org.bukkit.plugin.PluginDescriptionFile pdffile;
	public static org.bukkit.command.ConsoleCommandSender console;
	public static org.bukkit.Server server = null;
	public static org.bukkit.scheduler.BukkitScheduler scheduler = null;
	public static String pluginName = EPLib.rwhite + "["+ EPLib.green + "Entei's Inventory Manager" + EPLib.rwhite + "] ";
	public static String dataFolderName = "";
	public static boolean YamlsAreLoaded = false;
	public static org.bukkit.configuration.file.FileConfiguration config;
	public static java.io.File configFile = null;
	public static String configFileName = "config.yml";
	public static boolean updateInvScreensDebounce = false;
	public static final String invPermEditOtherPlayers = "eim.cmd.invperm.editothers";
	//private static java.util.ArrayList<String> playersUsingInvsInfo = new java.util.ArrayList<String>();

	private boolean enabled = true;
	// TODO To be loaded from config.yml
	public static boolean showDebugMsgs = false;
	public static String noPerm = "";
	public static String configVersion = "";
	public static boolean worldsHaveSeparateInventories = false;
	public static boolean manageExp = false;

	public static boolean manageHealth = false;
	public static boolean manageHunger = false;
	public static boolean manageEffects = false;

	public static boolean loadByGameMode = false;
	
	protected static boolean stopLooping = false;//Used to stop the saveAllPlayersPotionEffects() function from running every 30 seconds.
	protected static Thread loopThread = null;
	protected static int loopNum = -1;//This gets ++'d right off the bat, starting it out on zero, so it needs to be -1 to count up to zero.
	
	static final boolean forceDebugMsgs = false;

	public int NumberOfUpdates = 0; //Used for updateViewScreens().

	// TODO Functions
	public void LoginListener(MainInvClass JavaPlugin) {
		getServer().getPluginManager().registerEvents(this, plugin);
	}

	@Override
	public void onDisable() {
		sendConsoleMessage(pluginName + "&eSaving all online players' inventories...");
		for(Player curPlayer : server.getOnlinePlayers()) {
			savePlayerInventory(curPlayer, curPlayer.getWorld(), curPlayer.getGameMode());
			curPlayer.getOpenInventory().close();
		}
		//saveYamls();
		sendConsoleMessage(pluginName + "&eVersion " + pdffile.getVersion() + " is now disabled.");
		enabled = false;
	}

	@Override
	public void onEnable() {pdffile = this.getDescription();
		server = org.bukkit.Bukkit.getServer();
		server.getPluginManager().registerEvents(this, this);
		console = server.getConsoleSender();
		scheduler = server.getScheduler();
		java.io.File dataFolder = getDataFolder();
		if(!(dataFolder.exists())) {
			dataFolder.mkdir();
		}
		try{dataFolderName = getDataFolder().getAbsolutePath();} catch (SecurityException e) {FileMgmt.LogCrash(e, "onEnable()", "Failed to get the full directory of this plugin's folder(\"" + dataFolderName + "\")!", true, dataFolderName);}
		EPLib.showDebugMsg(pluginName + "The dataFolderName variable is: \"" + dataFolderName + "\"!", showDebugMsgs);
		// TODO Loading Files
		LoadConfig();
		// TODO End of Loading Files
		if(EPLib.enabled == false) {
			enabled = false;
			server.getPluginManager().disablePlugin(plugin);
		}
		if(enabled) {sendConsoleMessage(pluginName + "&aVersion " + pdffile.getVersion() + " is now enabled!");}
		
		if(config.getBoolean("updatePlayerPotionEffectsEvery30Seconds") == true) {
			loopPotionEffectSaving();
		}
	}

	public void loadPlayerInventory(Player player, World world, GameMode gm, boolean wipeInvs) {
		World oldWorld = world;
		world = getWhatWorldToUseFromWorld(world);
		sendConsoleMessage("&1&n=====&r &aDebug: &6loadPlayerInventory&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldWorld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f, &cboolean &2wipeinvs(" + wipeInvs + ")&f);");
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = player.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		Inventory blankInv = server.createInventory(player, InventoryType.PLAYER);
		String invFileName = "";
		String armorFileName = "";
		String enderFileName = "";

		if(loadByGameMode == false) {
			invFileName = ((worldsHaveSeparateInventories ? worldName : "") +".inv");
			armorFileName = ((worldsHaveSeparateInventories ? worldName : "") +".armorInv");
			enderFileName = ((worldsHaveSeparateInventories ? worldName : "") +".enderInv");
		} else {
			String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
			invFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".inv");
			armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".armorInv");
			enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".enderInv");
		}
		try{
			if(wipeInvs) {
				player.getOpenInventory().setCursor(null);
				player.getInventory().setContents(blankInv.getContents());
				player.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1), new ItemStack(Material.AIR, 1)});
				player.getEnderChest().setContents(server.createInventory(player, InventoryType.ENDER_CHEST).getContents());
				if(manageExp) {
					player.setLevel(0);
					player.setExp(0);
				}
			}
			String invTitle = getPlayerInvName(player) + " " + (loadByGameMode ? ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Inventory") : "Inventory");
			String enderTitle = getPlayerInvName(player) + " " + (loadByGameMode ? ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Ender Chest") : "Ender Chest");
			//String extraTitle = getPlayerInvName(player) + " " + (loadByGameMode ? ((player.getGameMode().equals(GameMode.SURVIVAL) ? "S " : (player.getGameMode().equals(GameMode.CREATIVE) ? "C " : (player.getGameMode().equals(GameMode.ADVENTURE) ? "A " : "? "))) + "Extra Inventory") : "Extra Inventory");

			try{player.getInventory().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player).getContents());
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + invFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current inventory instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					invFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.setTitle(invTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + invFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						invFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".inv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
				} else {
					invFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".inv"); //Intentional swappage.
					try{player.getInventory().setContents(InventoryAPI.setTitle(invTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + invFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						invFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".inv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}

			try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
				player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + armorFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current armor instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".armorInv"); //Intentional swappage.
					try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
						player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + armorFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".armorInv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
				} else {
					armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".armorInv"); //Intentional swappage.
					try{Inventory newArmorInv = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), player);
						player.getInventory().setArmorContents(new ItemStack[] {newArmorInv.getItem(0), newArmorInv.getItem(1), newArmorInv.getItem(2), newArmorInv.getItem(3)});
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + armorFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".armorInv"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}

			try{player.getEnderChest().setContents(InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player).getContents());
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + enderFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current enderchest instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".enderInv"); //Intentional swappage
					try{player.getEnderChest().setContents(InventoryAPI.setTitle(enderTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".enderInv"); //Intentional swappage
					}
					FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
				} else {
					enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".enderInv"); //Intentional swappage
					try{player.getEnderChest().setContents(InventoryAPI.setTitle(enderTitle, InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), player)).getContents());
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + enderFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".enderInv"); //Intentional swappage
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
			loadPlayerExp(player, world, gm);
		} catch (Exception e) {e.printStackTrace();/*savePlayerInventory(player, world);*/}
	}

	public void savePlayerInventory(Player player, World world, GameMode gm) {
		World oldWorld = world;
		world = getWhatWorldToUseFromWorld(world);
		sendConsoleMessage("&1&n=====&r &aDebug: &6savePlayerInventory&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldworld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = player.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		String invFileName = "";
		String armorFileName = "";
		String enderFileName = "";

		if(loadByGameMode == false) {
			invFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".inv");
			armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".armorInv");
			enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".enderInv");
		} else {
			String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
			invFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".inv");
			armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".armorInv");
			enderFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".enderInv");
		}
		FileMgmt.WriteToFile(invFileName, InventoryAPI.serializeInventory(player, "inventory"), true, FolderName, dataFolderName);
		FileMgmt.WriteToFile(armorFileName, InventoryAPI.serializeInventory(player, "armor"), true, FolderName, dataFolderName);
		FileMgmt.WriteToFile(enderFileName, InventoryAPI.serializeInventory(player, "enderchest"), true, FolderName, dataFolderName);
		savePlayerExp(player, world, gm);
	}

	@SuppressWarnings("boxing")
	public void loadPlayerHealthAndHunger(Player player, World world, GameMode gm) {
		if(manageHealth) {
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6loadPlayerHealth&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldWorld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;

			String healthFileName = "";
			double playerHealth = player.getHealth(); //Add in support for saving & loading player health!!!!

			if(loadByGameMode == false) {
				healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".health");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".health");
			}

			Double newHealth = (double) 0;
			try{newHealth = InventoryAPI.deserializeHealth(FileMgmt.ReadFromFile(healthFileName, FolderName, dataFolderName, false), player);
				sendConsoleMessage("&cDEBUG: The deserialized health is: \"" + newHealth + "\"...");
				Double Zero = (double) 0;
				if(newHealth.equals(Zero)) {
					player.setHealth(playerHealth);
					EPLib.sendConsoleMessage(pluginName + "&e[1]Set player \"" + player.getName() + "\"'s  health to: " + playerHealth);
				} else {
					player.setHealth(newHealth);
					EPLib.sendConsoleMessage(pluginName + "&e[1]Set player \"" + player.getName() + "\"'s  health to: " + newHealth);
				}
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + healthFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current health instead.", true);
				newHealth = (double) 0;

				//Start 'smart' loading
				if(loadByGameMode == false) {
					healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".health"); //Intentional swappage.
					try{newHealth = InventoryAPI.deserializeHealth(FileMgmt.ReadFromFile(healthFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + healthFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".health"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(healthFileName, InventoryAPI.serializeHealth(player), true, FolderName, dataFolderName);
				} else {
					healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".health"); //Intentional swappage.
					try{newHealth = InventoryAPI.deserializeHealth(FileMgmt.ReadFromFile(healthFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + healthFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".health"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(healthFileName, InventoryAPI.serializeHealth(player), true, FolderName, dataFolderName);
				}
				//End smart loading.

				Double Zero = (double) 0;
				if(newHealth.equals(Zero)) {
					player.setHealth(playerHealth);
					EPLib.sendConsoleMessage(pluginName + "&e[2]Set player \"" + player.getName() + "\"'s  health to: " + playerHealth);
				} else {
					player.setHealth(newHealth);
					EPLib.sendConsoleMessage(pluginName + "&e[2]Set player \"" + player.getName() + "\"'s  health to: " + newHealth);
				}
			}
			sendConsoleMessage("&aThe end result of the \"newHealth\" is: \"" + newHealth + "\"... The player's health ended up being: \"" + player.getHealth() + "\"...");
		}

		if(manageHunger) {
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6loadPlayerHunger&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldWorld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;

			String hungerFileName = "";

			String playerHunger = InventoryAPI.serializeHunger(player);

			//int playerFood = Integer.valueOf(InventoryAPI.deserializeHunger(playerHunger, player)[0]);
			//float playerExhaustion = Float.valueOf(InventoryAPI.deserializeHunger(playerHunger, player)[1]);

			if(loadByGameMode == false) {
				hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".hunger");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".hunger");
			}

			String[] newHunger = {"", ""};
			try{newHunger = InventoryAPI.deserializeHunger(FileMgmt.ReadFromFile(hungerFileName, FolderName, dataFolderName, false), player);
				sendConsoleMessage("&cDEBUG: The deserialized hunger is: \"FOOD:" + newHunger[0] + "\"; \"EXHAUSTION:" + newHunger[1] + "\"...");

				player.setFoodLevel(Integer.valueOf(newHunger[0])) ;
				player.setExhaustion(Float.valueOf(newHunger[1]));
				EPLib.sendConsoleMessage(pluginName + "&e[1]Set player \"" + player.getName() + "\"'s  hunger to: " + playerHunger);
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + hungerFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current hunger instead.", true);
				newHunger = InventoryAPI.deserializeHunger(playerHunger, player);

				//Start 'smart' loading
				if(loadByGameMode == false) {
					hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".hunger"); //Intentional swappage.
					try{newHunger = InventoryAPI.deserializeHunger(FileMgmt.ReadFromFile(hungerFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + hungerFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".hunger"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(hungerFileName, InventoryAPI.serializeHunger(player), true, FolderName, dataFolderName);
				} else {
					hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".hunger"); //Intentional swappage.
					try{newHunger = InventoryAPI.deserializeHunger(FileMgmt.ReadFromFile(hungerFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + hungerFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".hunger"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(hungerFileName, InventoryAPI.serializeHunger(player), true, FolderName, dataFolderName);
				}
				//End smart loading.

				player.setFoodLevel(Integer.valueOf(newHunger[0]));
				player.setExhaustion(Float.valueOf(newHunger[1]));
				EPLib.sendConsoleMessage(pluginName + "&e[2]Set player \"" + player.getName() + "\"'s  hunger to: \"FOOD: " + newHunger[0] + "\"; \"EXHAUSTION: " + newHunger[1] + "\"...");
			}
			sendConsoleMessage("&aThe end result of the \"newHunger\" is: \"FOOD: " + newHunger[0] + "\"; \"EXHAUSTION: " + newHunger[1] + " \"... The player's hunger ended up being: \"" + player.getHealth() + "\"...");
		}
	}

	public void savePlayerHealthAndHunger(Player player, World world, GameMode gm) {
		if(manageHealth) {
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6savePlayerHealth&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldworld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String healthFileName = "";

			if(loadByGameMode == false) {
				healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".health");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				healthFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".health");
			}
			FileMgmt.WriteToFile(healthFileName, InventoryAPI.serializeHealth(player), true, FolderName, dataFolderName);
		}
		if(manageHunger) {
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6savePlayerHunger&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldworld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String hungerFileName = "";

			if(loadByGameMode == false) {
				hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".hunger");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				hungerFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".hunger");
			}
			FileMgmt.WriteToFile(hungerFileName, InventoryAPI.serializeHunger(player), true, FolderName, dataFolderName);
		}
	}

	public void loadPlayerPotionEffects(Player player, World world, GameMode gm) {
		if(manageEffects) {
			removeAllPotionEffectsFromPlayer(player);
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6loadPlayerPotionEffects&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldWorld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String effectsFileName = "";
			if(loadByGameMode == false) {
				effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".effects");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".effects");
			}
			java.util.Collection<PotionEffect> newPotionEffects = player.getActivePotionEffects();
			try{newPotionEffects = InventoryAPI.deserializePotionEffects(FileMgmt.ReadFromFile(effectsFileName, FolderName, dataFolderName, false), player);
				//sendConsoleMessage("&cDEBUG: The deserialized potion effects are: \"" + newPotionEffects + "\"...");
				
				player.addPotionEffects(newPotionEffects);
				EPLib.sendConsoleMessage(pluginName + "&e[1]Set player \"" + player.getName() + "\"'s  effects to: " + newPotionEffects);
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + effectsFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current potion effects instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".effects"); //Intentional swappage.
					try{newPotionEffects = InventoryAPI.deserializePotionEffects(FileMgmt.ReadFromFile(effectsFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + effectsFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".effects"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(effectsFileName, InventoryAPI.serializePotionEffects(player), true, FolderName, dataFolderName);
				} else {
					effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".effects"); //Intentional swappage.
					try{newPotionEffects = InventoryAPI.deserializePotionEffects(FileMgmt.ReadFromFile(effectsFileName, FolderName, dataFolderName, false), player);
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + effectsFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".effects"); //Intentional swappage.
					}
					FileMgmt.WriteToFile(effectsFileName, InventoryAPI.serializePotionEffects(player), true, FolderName, dataFolderName);
				}
				//End smart loading.
				player.addPotionEffects(newPotionEffects);
				EPLib.sendConsoleMessage(pluginName + "&e[2]Set player \"" + player.getName() + "\"'s  effects to: " + newPotionEffects);
			}
			sendConsoleMessage("&aThe end result of the \"newPotionEffects\" is: \"" + newPotionEffects + "\"... The player's potion effects ended up being: \"" + player.getActivePotionEffects() + "\"...");
		}
	}

	public void savePlayerPotionEffects(Player player, World world, GameMode gm) {
		if(manageEffects) {
			World oldWorld = world;
			world = getWhatWorldToUseFromWorld(world);
			sendConsoleMessage("&1&n=====&r &aDebug: &6savePlayerPotionEffects&f(&aPlayer &2player(" + player.getName() + ")&f, &aWorld &2world(" + world.getName() + "; &3oldworld&2 = \"" + oldWorld.getName() + "\")&f, &2GameMode gm(" + gm.name() + ")&f);");
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String effectsFileName = "";
			if(loadByGameMode == false) {
				effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".effects");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				effectsFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".effects");
			}
			FileMgmt.WriteToFile(effectsFileName, InventoryAPI.serializePotionEffects(player), true, FolderName, dataFolderName);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onFoodLevelChangeEvent(org.bukkit.event.entity.FoodLevelChangeEvent evt) {
		if(manageHunger) {
			if(evt.isCancelled() == false) {
				if(evt.getEntity() instanceof Player) {
					Player healed = (Player) evt.getEntity();
					savePlayerHealthAndHunger(healed, healed.getWorld(), healed.getGameMode());
				} else {
					DEBUG("&f\"&d" + evt.getEntity().getType().name() + "&f\" is not a Player, so we don't have to save it's food level.");
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	private void onEntityRegainHealthEvent(org.bukkit.event.entity.EntityRegainHealthEvent evt) {
		if(manageHealth) {
			if(evt.isCancelled() == false) {
				if(evt.getEntity() instanceof Player) {
					Player healed = (Player) evt.getEntity();
					savePlayerHealthAndHunger(healed, healed.getWorld(), healed.getGameMode());
				} else {
					DEBUG("&f\"&d" + evt.getEntity().getType().name() + "&f\" is not a Player, so we don't have to save it's health.");
				}
			}
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	private void onEntityDamageEvent(org.bukkit.event.entity.EntityDamageEvent evt) {
		Entity entity = evt.getEntity();
		if(entity instanceof Player) {
			Player hurted = ((Player) entity);
			EPLib.sendConsoleMessage("&cTEST: \"" + hurted.getName() + "\" was damaged by \"" + evt.getDamage() + "\" points, which was caused by: \"" + evt.getCause().name().toLowerCase() + "\". &eTheir health is now \"" + hurted.getHealth() + "\" out of a maximum of \"" + hurted.getMaxHealth() + "\"...");
			//hurted.setMaxHealth(200);
			savePlayerHealthAndHunger(hurted, hurted.getWorld(), hurted.getGameMode());
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerJoinEvent(org.bukkit.event.player.PlayerJoinEvent evt) {
		Player newPlayer = evt.getPlayer();
		loadPlayerInventory(newPlayer, newPlayer.getWorld(), newPlayer.getGameMode(), false);
		loadPlayerHealthAndHunger(newPlayer, newPlayer.getWorld(), newPlayer.getGameMode());
		loadPlayerPotionEffects(newPlayer, newPlayer.getWorld(), newPlayer.getGameMode());
	}

	@EventHandler(priority=EventPriority.LOWEST)
	private void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent evt) {
		Player oldPlayer = evt.getPlayer();
		savePlayerInventory(oldPlayer, oldPlayer.getWorld(), oldPlayer.getGameMode());
		savePlayerHealthAndHunger(oldPlayer, oldPlayer.getWorld(), oldPlayer.getGameMode());
		savePlayerPotionEffects(oldPlayer, oldPlayer.getWorld(), oldPlayer.getGameMode());
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerGameModeChangeEvent(org.bukkit.event.player.PlayerGameModeChangeEvent evt) {
		final Player player = evt.getPlayer();
		GameMode oldGameMode = player.getGameMode();
		final GameMode newGameMode = evt.getNewGameMode();
		if(evt.isCancelled() == false) {
			savePlayerInventory(player, player.getWorld(), oldGameMode);
			savePlayerHealthAndHunger(player, player.getWorld(), oldGameMode);
			savePlayerPotionEffects(player, player.getWorld(), oldGameMode);

			scheduler.runTaskLater(plugin, new Runnable() {@Override public void run() {
				loadPlayerInventory(player, player.getWorld(), newGameMode, true);
				sendMessage(player, pluginName + "&aYour inventory has been updated to your current gamemode!");
				if(manageHealth) {loadPlayerHealthAndHunger(player, player.getWorld(), newGameMode);
					sendMessage(player, pluginName + "&aYour health " + (manageHunger ? "and hunger have" : "has") + " been updated to your current gamemode!");
				}
				if(manageEffects) {
					loadPlayerPotionEffects(player, player.getWorld(), newGameMode);
					sendMessage(player, pluginName + "&aAny potion effects you previously had have been restored for your current gamemode!");
				}
			}}, 2);//Two ticks later(To let the player's inventory get saved before wiping it)!
		}
	}

	@EventHandler(priority=EventPriority.LOWEST) 
	private void onPlayerChangedWorldEvent(org.bukkit.event.player.PlayerChangedWorldEvent evt) {
		Player player = evt.getPlayer();
		World newWorld = getWhatWorldToUseFromWorld(player.getWorld());
		World oldWorld = evt.getFrom();
		savePlayerInventory(player, oldWorld, player.getGameMode());//Save the old inventory to disk
		savePlayerHealthAndHunger(player, oldWorld, player.getGameMode());
		savePlayerPotionEffects(player, oldWorld, player.getGameMode());

		loadPlayerInventory(player, newWorld, player.getGameMode(), false);//Load the new inventory from disk
		loadPlayerHealthAndHunger(player, newWorld, player.getGameMode());
		loadPlayerPotionEffects(player, newWorld, player.getGameMode());
	}

	static String sendConsoleMessage(String msg) {return EPLib.sendConsoleMessage(msg);}
	static String sendMessage(CommandSender target, String msg) {return EPLib.sendMessage(target,  msg);}
	static String sendMessage(Player target, String msg) {return EPLib.sendMessage(target, msg);}

	private boolean LoadConfig() {
		this.saveDefaultConfig();
		configFile = new java.io.File(dataFolderName, configFileName);
		config = new org.bukkit.configuration.file.YamlConfiguration();
		//NEWCONFIGFile = new java.io.File(dataFolderName, NEWCONFIGFileName);
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
			FileMgmt.LogCrash(e, "reloadFiles()", "Failed to load one or more of the following YAML files: " + unloadedFiles, false, dataFolderName);
			EPLib.showDebugMsg(pluginName + "&cThe following YAML files failed to load properly! Check the server log or \"" + dataFolderName + "\\crash-reports.txt\" to solve the problem: (" + unloadedFiles + ")", true);
			//logger.severe(e.toString());//A test
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
			//logger.severe(e.toString());//A test
			return false;
		}
	}

	@SuppressWarnings("boxing")
	private boolean loadYamlVariables() {
		boolean loadedAllVars = true;
		try{
			configVersion = EPLib.formatColorCodes(config.getString("version"));
			if(configVersion.equals(pdffile.getVersion())) {
				EPLib.showDebugMsg(pluginName + "&aThe " + configFileName + "'s version matches this plugin's version(&f" + pdffile.getVersion() + "&a)!", showDebugMsgs);
			} else {
				EPLib.showDebugMsg(pluginName + "&eThe " + configFileName + "'s version does NOT match this plugin's version(&f" + pdffile.getVersion() + "&e)! Make sure that you update the " + configFileName + " from this plugin's latest version! You can find this at &ahttp://dev.bukkit.org/plugins/enteis-inventory-manager/&e.", true/*showDebugMsgs*/);
			}
		} catch (Exception e) {
			EPLib.unSpecifiedVarWarning("version", "config.yml", pluginName);
			sendConsoleMessage(pluginName + "&cInvalid configuration settings detected! Disabling this plugin to prevent bad settings from corrupting saved player data...");
			server.getPluginManager().disablePlugin(plugin);
			enabled = false;
			return false;
		}
		try{showDebugMsgs = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("showDebugMsgs")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("showDebugMsgs", "config.yml", pluginName);}
		try{noPerm = EPLib.formatColorCodes(config.getString("noPermission"));
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("noPermission", "config.yml", pluginName);}
		try{worldsHaveSeparateInventories = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("worldsHaveSeparateInventories")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("worldsHaveSeparateInventories", "config.yml", pluginName);}

		try{manageExp = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("manageExp")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("manageExp", "config.yml", pluginName);}

		try{manageHealth = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("manageHealth")))) == true;
		} catch(Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("manageHealth", "config.yml", pluginName);}

		try{manageHunger = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("manageHunger")))) == true;
		} catch(Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("manageHunger", "config.yml", pluginName);}

		try{manageEffects = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("manageEffects")))) == true;
		} catch(Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("manageEffects", "config.yml", pluginName);}

		try{loadByGameMode = (Boolean.valueOf(EPLib.formatColorCodes(config.getString("loadByGameMode")))) == true;
		} catch (Exception e) {loadedAllVars = false;EPLib.unSpecifiedVarWarning("loadByGameMode", "config.yml", pluginName);}

		return loadedAllVars;
	}

	/**@author Bukkit and Brian_Entei @ 01-24-2014
	 * @param sender
	 * @param cmd
	 * @param command
	 * @param args
	 * @return
	 */
	@Override
	public boolean onCommand(final CommandSender sender, final org.bukkit.command.Command cmd, final String command, final String[] args) {
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
		/*if(command.equalsIgnoreCase("listinvscreendata")) {
			if(sender instanceof ConsoleCommandSender) {
				listPlayerInvScreenData();
			} else {
				sendMessage(sender, pluginName + "This command can only be used by the console.");
			}
			return true;
		}*/
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
							for(String curAuthor : pdffile.getAuthors()) {
								authors += curAuthor + "\", \"";
							}
							if(authors.equals("\"") == false) {
								authors += ".";
								authors = authors.replace("\", \".", "\"");
							} else {
								authors = "&oNone specified in plugin.yml!&r";
							}
							sendMessage(sender, EPLib.green + pdffile.getPrefix() + " " + pdffile.getVersion() + "; Main class: " + pdffile.getMain() + "; Author(s): (" + authors + "&a).");
						} else {
							sendMessage(sender, pluginName + "&eUsage: /" + command + " info");
						}
						//return true;
					} else {
						sendMessage(sender, pluginName + noPerm);
					}
					return true;
				} else if(args[0].equalsIgnoreCase("deactivate30sectimer")) {
					if(sender.isOp()) {
						if(stopLooping == false) {
							stopLooping = true;
							loopThread.interrupt();
							EPLib.sendMessage(sender, pluginName + "&aStopped looping function \"&6updateAllPlayersPotionEffects&f()&a\"... please wait up to 30 seconds for changes to apply.");
						} else {
							EPLib.sendMessage(sender, pluginName + "&eThe 30 second timer is already inactive!");
						}
					}
				} else if(args[0].equalsIgnoreCase("activate30sectimer")) {
					if(sender.isOp()) {
						if(stopLooping == true) {
							stopLooping = false;
							EPLib.sendMessage(sender, pluginName + "&aStarted looping function \"&6updateAllPlayersPotionEffects&f()&a\"... please wait up to 30 seconds for changes to apply.");
							scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
								@Override
								public void run() {
									loopPotionEffectSaving();
								}
							}, 600);
						} else {
							EPLib.sendMessage(sender, pluginName + "&eThe 30 second timer is already active!");
						}
					}
				} else {
					EPLib.sendMessage(sender, pluginName + "&eUsage: \"/" + command + " info\" or use an admin command.");
				}
				return true;
			}
			EPLib.sendMessage(sender, pluginName + "&eUsage: \"/" + command + " info\" or use an admin command.");
			return true;
		} else if(command.equalsIgnoreCase("viewoffline")) {
			if(user != null) {
				if(user.hasPermission("eim.cmd.use.view") == false && user.hasPermission("eim.*") == false) {
					sendMessage(user, pluginName + noPerm);
					return true;
				}
				String targetName = "";
				String invToLoad = null;
				GameMode gm = null;
				World targetWorld = null;
				if(args.length == 2) {// /view {playerName} {inv|ender|extra|armorinv|armourinv}
					invToLoad = args[1];
					targetName = args[0];
					gm = (args[0].equalsIgnoreCase("survival") || args[0].equals("0") ? GameMode.SURVIVAL : (args[0].equalsIgnoreCase("creative") || args[0].equals("1") ? GameMode.CREATIVE : (args[0].equalsIgnoreCase("adventure") || args[0].equals("2") ? GameMode.ADVENTURE : null)));
					targetWorld = server.getWorld(args[0]);
					if(targetName == null && gm == null && targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName, worldName, or gameMode: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName|worldName|gamemode} {inv|ender|extra|armorinv|armourinv}");
						sendMessage(user, pluginName + "&aIf you would like to open the inventory of an offline player, use \"viewOffline\" instead.");
						return true;
					}
				} else if(args.length == 4) {// /view {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}
					invToLoad = args[3];
					targetName = args[0];
					gm = (args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? GameMode.SURVIVAL : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? GameMode.CREATIVE : GameMode.ADVENTURE));
					targetWorld = server.getWorld(args[2]);
					if(targetName == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					} else if(gm == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid gameMode: \"&f" + args[1] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					} else if(targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid worldName: \"&f" + args[2] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					}
				} else {
					sendMessage(user, pluginName + "&eThe arguments that you entered were not compatable for this command. Here is a list of all possible usages:");
					showUsageForCmd("view", "all", user);
					return true;
				}
				if(gm == null) {gm = user.getGameMode();}
				if(targetWorld == null) {targetWorld = user.getWorld();}
				if(invToLoad.equalsIgnoreCase("inv")) {
					String success = openPlayerInventory(user, targetName, gm, targetWorld, "inv");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(targetName) ? " your" : " &f" + targetName + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " inventory.");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("ender") || invToLoad.equalsIgnoreCase("enderchest")) {
					String success = openPlayerInventory(user, targetName, gm, targetWorld, "ender");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + targetName + "&r&e's" + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " ender chest.");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("extra")) {
					String success = openPlayerInventory(user, targetName, gm, targetWorld, "extra");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + targetName + "&r&e's extra" + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " chest.");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("armor") || invToLoad.equalsIgnoreCase("armour") || invToLoad.equalsIgnoreCase("armorinv") || invToLoad.equalsIgnoreCase("armourinv")) {
					if(user.getName().equals(targetName) && user.getGameMode().equals(gm)) {
						sendMessage(user, pluginName + "&eYou can view this inventory by pressing 'e', or whatever your inventory button is set to!");
						return true;
					}
					String success = openPlayerInventory(user, targetName, gm, targetWorld, "armorinv");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open \"&f" + targetName + "&r&e\"'s \"&f" + (loadByGameMode ? " " + gm.name().toLowerCase() : "") + "&e\" armor inventory.");
					}
					return true;
				} else {
					sendMessage(sender, pluginName + "&eThe argument you entered, \"&f" + invToLoad + "&r&e\", must be one of the following: &finv ender enderchest extra armor armour armorinv armourinv&e.");
					return true;
				}
			}
			return false;
		} else if(command.equalsIgnoreCase("view")) {
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
				} else if(args.length == 2) {// /view {playerName|worldName|gamemode} {inv|ender|extra|armorinv|armourinv}
					invToLoad = args[1];
					target = server.getPlayer(args[0]);
					gm = (args[0].equalsIgnoreCase("survival") || args[0].equals("0") ? GameMode.SURVIVAL : (args[0].equalsIgnoreCase("creative") || args[0].equals("1") ? GameMode.CREATIVE : GameMode.ADVENTURE));
					targetWorld = server.getWorld(args[0]);
					if(target == null && gm == null && targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName, worldName, or gameMode: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName|worldName|gamemode} {inv|ender|extra|armorinv|armourinv}");
						sendMessage(user, pluginName + "&aIf you would like to open the inventory of an offline player, use \"viewOffline\" instead.");
						return true;
					}
				} else if(args.length == 3) {
					invToLoad = args[2];
					// /view {worldName} {gameMode} {inv|ender|extra|armorinv|armourinv}
					gm = (args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? GameMode.SURVIVAL : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? GameMode.CREATIVE : GameMode.ADVENTURE));
					targetWorld = server.getWorld(args[0]);
					if(targetWorld != null && gm != null) {
						target = user;
					}
					if(server.getPlayer(args[0]) != null) {// /view {playerName} {worldName|gamemode} {inv|ender|extra|armorinv|armourinv}
						target = server.getPlayer(args[0]);
						targetWorld = server.getWorld(args[1]);
						if(gm == null && targetWorld == null) {
							/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[1] + "&r&e\", is not a valid gameMode or a valid worldName."/*)*/);
							/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName|gamemode} {inv|ender|extra|armorinv|armourinv} or /" + command + " {worldName} {gameMode} {inv|ender|extra|armorinv|armourinv}"/*)*/);
							return true;
						}
						if(gm == null) {gm = (target != null ? target.getGameMode() : null);}
						if(targetWorld == null) {targetWorld = (target != null ? target.getWorld() : null);}
					} else if(targetWorld == null) {// /view {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[0] + "&r&e\", is not a valid worldName.")/*)*/;
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}")/*)*/;
						return true;
					} else if(gm == null) {// /view {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eThe following argument that you entered, \"&f" + args[1] + "&r&e\", is not a valid gameMode.")/*)*/;
						/*sendConsoleMessage(*/sendMessage(user, pluginName + "&eUsage: /" + command + " {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}")/*)*/;
						return true;
					}
					if(target == null && gm == null && targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName, worldName, or gameMode: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName|worldName|gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					}
				} else if(args.length == 4) {// /view {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}
					invToLoad = args[3];
					target = server.getPlayer(args[0]);
					gm = (args[1].equalsIgnoreCase("survival") || args[1].equals("0") ? GameMode.SURVIVAL : (args[1].equalsIgnoreCase("creative") || args[1].equals("1") ? GameMode.CREATIVE : GameMode.ADVENTURE));
					targetWorld = server.getWorld(args[2]);
					if(target == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid playerName: \"&f" + args[0] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					} else if(gm == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid gameMode: \"&f" + args[1] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
						return true;
					} else if(targetWorld == null) {
						sendMessage(user, pluginName + "&eThe following argument that you typed is not a valid worldName: \"&f" + args[2] + "&r&e\".");
						sendMessage(user, pluginName + "&eUsage: /" + command + " {playerName} {worldName} {gamemode} {inv|ender|extra|armorinv|armourinv}");
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
				invToLoad = invToLoad.toLowerCase();
				if(invToLoad.equalsIgnoreCase("inv")) {
					if(user.getName().equals(target.getName()) && user.getGameMode().equals(gm) && user.getWorld().equals(targetWorld)) {
						sendMessage(user, pluginName + "&eYou can view this inventory by pressing 'e', or whatever your inventory button is set to!");
						return true;
					}
					String success = openPlayerInventory(user, target, gm, targetWorld, "inv");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your" : " &f" + target.getDisplayName() + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " inventory.&z&cYou need the following permission to do this: \"&f" + getPermissionStringForOpeningInventory(target, gm, targetWorld, invToLoad.toLowerCase().replace("armour", "armor").replace("armorinv", "armor").replace("armourinv", "armor").replace("enderchest", "ender")) + "&c\".");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("ender") || invToLoad.equalsIgnoreCase("enderchest")) {
					String success = openPlayerInventory(user, target, gm, targetWorld, "ender");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your" : " &f" + target.getDisplayName() + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " ender chest.&z&cYou need the following permission to do this: \"&f" + getPermissionStringForOpeningInventory(target, gm, targetWorld, invToLoad.toLowerCase().replace("armour", "armor").replace("armorinv", "armor").replace("armourinv", "armor").replace("enderchest", "ender")) + "&c\".");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("extra")) {
					String success = openPlayerInventory(user, target, gm, targetWorld, "extra");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your extra" : " &f" + target.getDisplayName() + "&r&e's extra") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + " chest.&z&cYou need the following permission to do this: \"&f" + getPermissionStringForOpeningInventory(target, gm, targetWorld, invToLoad.toLowerCase().replace("armour", "armor").replace("armorinv", "armor").replace("armourinv", "armor").replace("enderchest", "ender")) + "&c\".");
					}
					return true;
				} else if(invToLoad.equalsIgnoreCase("armorinv") || invToLoad.equalsIgnoreCase("armourinv")) {
					if(user.getName().equals(target.getName()) && user.getGameMode().equals(gm)) {
						sendMessage(user, pluginName + "&eYou can view this inventory by pressing 'e', or whatever your inventory button is set to!");
						return true;
					}
					String success = openPlayerInventory(user, target, gm, targetWorld, "armorinv");
					if(success.equalsIgnoreCase("noperm")) {
						sendMessage(user, pluginName + "&eIt appears you do not have permission to open " + (user.getName().equals(target.getName()) ? " your" : " &f" + target.getDisplayName() + "&r&e's") + (loadByGameMode ? " &f" + gm.name().toLowerCase() + "&e" : "") + "armo(u)r inventory.&z&cYou need the following permission to do this: \"&f" + getPermissionStringForOpeningInventory(target, gm, targetWorld, invToLoad.toLowerCase().replace("armour", "armor").replace("armorinv", "armor").replace("armourinv", "armor").replace("enderchest", "ender")) + "&c\".");
					}
					return true;
				} else {
					sendMessage(sender, pluginName + "&eThe argument you entered, \"&f" + invToLoad + "&r&e\", must be one of the following: &finv ender enderchest extra armorinv armourinv&e.");
					return true;
				}
			}
			//TODO /view for console
			sendMessage(sender, pluginName + "&e/" + command + " is used to display a player's inventory. When used by the console, it is used to display the targeted players' inventory on their screen. This command is, however, currently NYI for console use.(Not Yet Implemented)");
			return true;
		} else if(command.equalsIgnoreCase("invperm")) {
			if(EPLib.vaultAvailable != false) {
				if(EPLib.perm != null) {
					EPLib.showDebugMsg(pluginName + "&aInternal variable \"permission\" is not null!", showDebugMsgs);
				} else {
					EPLib.showDebugMsg(pluginName + "&4Could not load permission service...(No Vault Plugin, or coding issue?)", showDebugMsgs);
					sendMessage(sender, pluginName + "&eWhoops! There was an error when trying to load &bVault&e's permission service... Please let the server owner know so he/she can fix it!");
					return true;
				}
			} else {
				sendMessage(sender, pluginName + "&eSorry, this command is only available with &bVault&e installed! Please ask the server owner about installing &bVault&e.");
				return true;
			}
			if(user != null) {
				if(args.length == 5) {
					EPLib.sendMessage(user, pluginName + "&4java.lang.someRandomErrorThatMakesNoSenseError: Command syntax Not Yet Implemented!");
					EPLib.sendMessage(user, pluginName + "&eUsage: \"&f/invperm {give|take} {view|edit} {gamemode|worldName} {inv|ender|extra|armorinv|armourinv} {playerName}&e\".");
					return true;
				}
				if(args.length == 6 || args.length == 7) {// /invperm {give|take} {view|edit} [gamemode] [worldName] {inv|ender|extra|armorinv|armourinv} {playerName} [ownerName]
					String giveOrTake = args[0];
					if(giveOrTake.equalsIgnoreCase("give") == false && giveOrTake.equalsIgnoreCase("take") == false) {
						EPLib.sendMessage(user, pluginName + "&eThe argument that you entered (\"&f" + args[0] + "&r&e\") must be either \"&fgive&e\" or \"&ftake&e\".");
						return true;
					}
					String viewOrEdit = args[1];
					if(viewOrEdit.equalsIgnoreCase("view") == false && viewOrEdit.equalsIgnoreCase("edit") == false) {
						EPLib.sendMessage(user, pluginName + "&eThe argument that you entered (\"&f" + args[1] + "&r&e\") must be either \"&fview&e\" or \"&fedit&e\".");
						return true;
					}
					String getGameMode = getGameModeFromString(args[2]);
					GameMode gamemode = null;
					if(getGameMode.equals("s")) {
						gamemode = GameMode.SURVIVAL;
					} else if(getGameMode.equals("c")) {
						gamemode = GameMode.CREATIVE;
					} else if(getGameMode.equals("a")) {
						gamemode = GameMode.ADVENTURE;
					} else {
						EPLib.sendMessage(user, pluginName + "&e\"&f" + args[3] + "&r&e\" is not a valid gamemode. Valid gamemodes include: '&fsurvival&e' '&fs&e' or '&f0&e', '&fcreative&e' '&fc&e' or '&f1&e', '&fadventure&e' '&fa&e' or '&f2&e'.");
						return true;
					}
					World world = server.getWorld(args[3]);
					if(world == null) {
						EPLib.sendMessage(user, pluginName + "&e\"&f" + args[3] + "&r&e\" is not a valid world name. Listing all available world names:");
						int z = 0;
						/*TODO Neat world list for loop*/for(World curWorld : server.getWorlds()) {z++;EPLib.sendMessage(user, "&f[&3" + z + "&f]: \"&e" + curWorld.getName() + "&r&f\"" + (z == server.getWorlds().size() ? "." : ";"));}
						return true;
					}
					world = getWhatWorldToUseFromWorld(world);
					String invType = (args[4].equalsIgnoreCase("inv") || args[4].equalsIgnoreCase("inventory") ? "inv" : (args[4].equalsIgnoreCase("ender") || args[4].equalsIgnoreCase("enderchest") ? "ender" : (args[4].equalsIgnoreCase("extra") || args[4].equalsIgnoreCase("extrachest") ? "extra" : (args[4].equalsIgnoreCase("armor") || args[4].equalsIgnoreCase("armour") || args[4].equalsIgnoreCase("armorinv") || args[4].equalsIgnoreCase("armourinv") ? "armor" : "null" ))));
					if(invType.equalsIgnoreCase("null")) {EPLib.sendMessage(user, pluginName + "&e\"&f" + args[4] + "&r&e\" is not a valid inventory type.");
						EPLib.sendMessage(user, pluginName + "&eValid inventory types include: \"inv\", \"ender\", \"extra\", \"armor\", \"armour\", \"armorinv\", \"armourinv\".");
						return true;
					}
					Player target = server.getPlayer(args[5]);
					if(target == null) {EPLib.sendMessage(user, pluginName + "&e\"&f" + args[5] + "&r&e\" is not a valid online player name. Please check your spelling and try again.");return true;}
					//BugFixing
					Player owner = null;
					if(args.length == 7) {
						if(user.isOp() || user.hasPermission(invPermEditOtherPlayers) || user.hasPermission("eim.*")) {
							//getOwner = args[6];
						} else {
							EPLib.sendMessage(user, pluginName + "&cYou do not have permission to edit other players' inventory permissions. The permission you need to be able to do that is: \"&f" + invPermEditOtherPlayers + "&r&c\"");
							return true;
						}
					} else {
						owner = user;
					}
					//End BugFixing
					if(owner == null) {
						//EPLib.sendMessage(user, pluginName + "&e\"&f" + args[6] + "&r&e\" is not a valid online player name. Please check your spelling and try again.");
						//return true;
						owner = user;
					}
					String args6 = "";
					if(args.length == 7) {
						args6 = args[6];
					}
					DEBUG("&e[&f1/7&e]/invperm give|take = \"&f" + giveOrTake + "&r&e\" (args[0]: \"" + args[0] + "\");");
					DEBUG("&e[&f2/7&e]/invperm view|edit = \"&f" + viewOrEdit + "&r&e\" (args[1]: \"" + args[1] + "\");");
					DEBUG("&e[&f3/7&e]/invperm Gamemode = \"&f" + gamemode.name() + "&e\" (args[2]: \"" + args[2] + "\");");
					DEBUG("&e[&f4/7&e]/invperm World = \"&f" + world.getName() + "&e\" (args[3]: \"" + args[3] + "\");");
					DEBUG("&e[&f5/7&e]/invperm inventoryType = \"&f" + invType + "&r&e\" (args[4]: \"" + args[4] + "\");");
					DEBUG("&e[&f6/7&e]/invperm Target player = \"&f" + target.getName() + "&r&e\" (args[5]: \"" + args[5] + "\");");
					DEBUG("&e[&f7/7&e]/invperm OwnerName = \"&f" + owner.getName() + "&r&e\" (args[6]: \"" + args6 + "\").");
					boolean successful = changeInvPerms(owner, target, giveOrTake, viewOrEdit, gamemode, world, invType);
					if(successful == false) {
						EPLib.sendConsoleMessage(pluginName + "&eAn error occurred while attempting to perform the following command: \"&f/" + command + "&r&f " + strArgs + "&r&e\" by user \"&f" + sender.getName() + "&r&e\"...");
						return true;
					}
					return true;
				} else if(args.length == 4) {// /invperm {give|take} {view|edit} {inv|ender|extra|armorinv|armourinv} {playerName}
					String giveOrTake = args[0];
					String viewOrEdit = args[1];
					
					String invType = (args[2].equalsIgnoreCase("inv") || args[2].equalsIgnoreCase("inventory") ? "inv" : (args[2].equalsIgnoreCase("ender") || args[2].equalsIgnoreCase("enderchest") ? "ender" : (args[2].equalsIgnoreCase("extra") || args[2].equalsIgnoreCase("extrachest") ? "extra" : (args[2].equalsIgnoreCase("armor") || args[2].equalsIgnoreCase("armour") || args[2].equalsIgnoreCase("armorinv") || args[2].equalsIgnoreCase("armourinv") ? "armor" : "null" ))));
					if(invType.equalsIgnoreCase("null")) {EPLib.sendMessage(user, pluginName + "&e\"&f" + args[2] + "&r&e\" is not a valid inventory type.");
						EPLib.sendMessage(user, pluginName + "&eValid inventory types include: \"inv\", \"ender\", \"extra\", \"armor\", \"armour\", \"armorinv\", \"armourinv\".");
						return true;
					}
					
					Player target = server.getPlayer(args[3]);
					if(target == null) {EPLib.sendMessage(user, pluginName + "&e\"&f" + args[3] + "&r&e\" is not a valid online player name. Please check your spelling and try again.");return true;}
					Player owner = user;
					World world = getWhatWorldToUseFromWorld(owner.getWorld());
					GameMode gamemode = owner.getGameMode();
					
					DEBUG("&e[&f1/4&e]/invperm give|take = \"&f" + giveOrTake + "&r&e\";");
					DEBUG("&e[&f2/4&e]/invperm view|edit = \"&f" + viewOrEdit + "&r&e\";");
					DEBUG("&e[&f3/4&e]/invperm inventoryType = \"&f" + invType + "&r&e\";");
					DEBUG("&e[&f4/4&e]/invperm Target player = \"&f" + target.getName() + "&r&e\";");
					DEBUG("&e[&f5/4&e]/invperm Gamemode = \"&f" + gamemode.name() + "&e\";");
					DEBUG("&e[&f6/4&e]/invperm World = \"&f" + world.getName() + "&e\";");
					DEBUG("&e[&f7/4&e]/invperm OwnerName = \"&f" + owner.getName() + "\".");
					
					boolean successful = changeInvPerms(owner, target, giveOrTake, viewOrEdit, gamemode, world, invType);
					if(successful == false) {
						EPLib.sendConsoleMessage(pluginName + "&eAn error occurred while attempting to perform the following command: \"&f/" + command + "&r&f " + strArgs + "&r&e\" by user \"&f" + sender.getName() + "&r&e\"...");
					}
					return true;
				}
				EPLib.sendMessage(user, pluginName + "&eUsage: \"&f/invperm {give|take} {view|edit} {inv|ender|extra|armorinv|armourinv} {playerName}&e\" OR \"&f/invperm {give|take} {view|edit} {gamemode} {worldName} {inv|ender|extra|armorinv|armourinv} {playerName} [ownerName]&e\".");
				return true;
			}
			if(sender instanceof org.bukkit.command.ConsoleCommandSender || sender.isOp()) {
				if(args.length == 7) {// /invperm {give|take} {view|edit} [gamemode] [worldName] {inv|ender|extra|armorinv|armourinv} {playerName} {ownerName}
					String giveOrTake = args[0];
					String viewOrEdit = args[1];
					String getGameMode = (args[2].equalsIgnoreCase("survival") || args[2].equals("0") || args[2].equalsIgnoreCase("s") ? "s" : (args[2].equalsIgnoreCase("creative") || args[2].equals("1") || args[2].equalsIgnoreCase("c") ? "c" : (args[2].equalsIgnoreCase("adventure") || args[2].equals("2") || args[2].equalsIgnoreCase("a") ? "a" : "null")));
					GameMode gamemode = null;
					if(getGameMode.equals("s")) {
						gamemode = GameMode.SURVIVAL;
					} else if(getGameMode.equals("c")) {
						gamemode = GameMode.CREATIVE;
					} else if(getGameMode.equals("a")) {
						gamemode = GameMode.ADVENTURE;
					}
					World world = server.getWorld(args[3]);
					if(world == null) {
						EPLib.sendMessage(sender, pluginName + "&e\"&f" + args[3] + "&r&e\" is not a valid world name. Listing all available world names:");
						int z = 0;
						/*TODO Neat world list for loop*/for(World curWorld : server.getWorlds()) {z++;EPLib.sendMessage(sender, "&f[&3" + z + "&f]: \"&e" + curWorld.getName() + "&r&f\"" + (z == server.getWorlds().size() ? "." : ";"));}
						return true;
					}
					world = getWhatWorldToUseFromWorld(world);
					String invType = (args[4].equalsIgnoreCase("inv") || args[4].equalsIgnoreCase("inventory") ? "inv" : (args[4].equalsIgnoreCase("ender") || args[4].equalsIgnoreCase("enderchest") ? "ender" : (args[4].equalsIgnoreCase("extra") || args[4].equalsIgnoreCase("extrachest") ? "extra" : (args[4].equalsIgnoreCase("armor") || args[4].equalsIgnoreCase("armour") || args[4].equalsIgnoreCase("armorinv") || args[4].equalsIgnoreCase("armourinv") ? "armor" : "null" ))));
					if(invType.equalsIgnoreCase("null")) {EPLib.sendMessage(sender, pluginName + "&c\"&f" + args[4] + "&r&c\" is not a valid inventory type.");
						EPLib.sendMessage(sender, pluginName + "Valid inventory types include: \"inv\", \"ender\", \"extra\", \"armor\", \"armour\", \"armorinv\", \"armourinv\".");
						return true;
					}
					Player target = server.getPlayer(args[5]);
					if(target == null) {EPLib.sendMessage(sender, pluginName + "&c\"&f" + args[5] + "&r&c\" is not a valid online player name. Please check your spelling and try again.");return true;}
					Player owner = server.getPlayer(args[6]);
					if(owner == null) {
						EPLib.sendMessage(sender, pluginName + "&c\"&f" + args[6] + "&r&c\" is not a valid online player name. Please check your spelling and try again.");
						return true;
					}
					DEBUG("&e[&f1/7&e]/invperm give|take = \"&f" + giveOrTake + "&r&e\";");
					DEBUG("&e[&f2/7&e]/invperm view|edit = \"&f" + viewOrEdit + "&r&e\";");
					DEBUG("&e[&f3/7&e]/invperm Gamemode = \"&f" + gamemode.name() + "&e\";");
					DEBUG("&e[&f4/7&e]/invperm World = \"&f" + world.getName() + "&e\";");
					DEBUG("&e[&f5/7&e]/invperm inventoryType = \"&f" + invType + "&r&e\";");
					DEBUG("&e[&f6/7&e]/invperm Target player = \"&f" + target.getName() + "&r&e\";");
					DEBUG("&e[&f7/7&e]/invperm OwnerName = \"&f" + owner.getName() + "\".");
					boolean successful = changeInvPerms(owner, target, giveOrTake, viewOrEdit, gamemode, world, invType);
					if(successful == false) {
						EPLib.sendConsoleMessage(pluginName + "&eAn error occurred while attempting to perform the following command: \"&f/" + command + "&r&f " + strArgs + "&r&e\" by user \"&f" + sender.getName() + "&r&e\"...");
						return true;
					}
				} else {
					EPLib.sendMessage(sender, pluginName + "&eUsage: \"&f/invperm {give|take} {view|edit} {gamemode} {worldName} {inv|ender|extra|armorinv|armourinv} {playerName} [ownerName]&e\".");
				}
			} else {
				EPLib.sendMessage(sender, pluginName + "&4You do not have access to that command.");
			}
			return true;
		} else {
			DEBUG("&eA registered command was never implemented: \"&f" + command + "&r&e\"...");
			return false;
		}
	}

	/**@author Brian_Entei @ 01-23-2014
	 * Allows for users to change other users' permissions to view or edit their various inventory types, which also vary in 3 different gamemodes and in multiple worlds as well.
	 * @param owner Player
	 * @param target Player
	 * @param giveOrTake String
	 * @param viewOrEdit String
	 * @param gamemode GameMode
	 * @param world World
	 * @param invType String
	 * @return Whether or not the function was successful in completing its' task.
	 */
	public boolean changeInvPerms(Player owner, Player target, String giveOrTake, String viewOrEdit, GameMode gamemode, World world, String invType) {
		boolean rtrn = false;
		DEBUG("&f----------> &cpublic void &6changeInvPerms&f(&aPlayer &2owner&f, &aPlayer &2target&f, &aString &2viewOrEdit&f, &2GameMode gamemode&f, &aWorld &2world&f, &aString &2invType&f)");
		if(owner == null) {
			DEBUG("&4The \"owner\" variable equals null! We can't do anything if there is no owner to get permissions from...");
			DEBUG("&f----------> &4End of function: &cpublic void &6changeInvPerms&f()");
			return rtrn;
		}
		if(target == null) {
			DEBUG("&4The \"target\" variable equals null! We can't do anything if there is no target to set permissions to...");
			DEBUG("&f----------> &4End of function: &cpublic void &6changeInvPerms&f()");
			return rtrn;
		}
		if(world == null) {
			world = getWhatWorldToUseFromWorld(owner.getWorld());
		} else {
			world = getWhatWorldToUseFromWorld(world);
		}		
		if(gamemode == null) {
			gamemode = owner.getGameMode();
		}
		String perm = "eim." + (viewOrEdit.equalsIgnoreCase("view") ? "view." : (viewOrEdit.equalsIgnoreCase("edit") ? "edit." : "NULL."))
				+ (loadByGameMode ? "gamemode." + gamemode.name().toLowerCase() + "." : "")
				+ (invType.equalsIgnoreCase("inv") ? "inv." : (invType.equalsIgnoreCase("ender") ? "ender." : (invType.equalsIgnoreCase("extra") ? "extra." : (invType.equalsIgnoreCase("armor") || invType.equalsIgnoreCase("armour") ? "armor." : "NULL."))))
				+ (worldsHaveSeparateInventories ? "world." + world.getName().toLowerCase() + "." : "") + "owner." + owner.getName();
		if(perm.contains("NULL.")) {//TODO: Useful debugging information sent to console about "perm" here.
			EPLib.sendConsoleMessage(pluginName + "&cError: critical internal variable \"&fperm&c\" contains \"&4NULL.&c\", which means that the permission to give or take away is missing something! The \"&fperm&c\" variable is:&z&f" + perm);
			EPLib.sendConsoleMessage(pluginName + "&cThis function was parsed as follows: \"&cpublic void &6changeInvPerms&f(&aPlayer &2owner(\"" + owner.getName() + "\")&f, &aPlayer &2target(\"" + target.getName() + "\")&f, &aString &2viewOrEdit(\"" + viewOrEdit + "\")&f, &2GameMode gamemode(\"" + gamemode.name() + "\")&f, &aWorld &2world(\"" + world.getName() + "\")&f, &aString &2invType(\"" + invType + "\")&f)&c\"...");
			return false;
		}
		net.milkbowl.vault.permission.Permission permission = EPLib.perm;
		boolean success = false;
		if(giveOrTake.equalsIgnoreCase("give")) {
			success = permission.playerAdd(world, target.getName(), perm);
			if(target.hasPermission(perm) == false) {
				DEBUG("&fBut yet, \"" + target.getName() + "\" does not have the above mentioned permission when doing \"if(target.hasPermission(perm) == false);\".... Hmm...");
				return false;
			}
			String giveMessage = "&2You have been given permission to " + viewOrEdit.toLowerCase() + " \"&f" + owner.getName() + "&r&2\"'s " + (loadByGameMode ? gamemode.name().toLowerCase() : "") + getInvTypeFullName(invType) + (worldsHaveSeparateInventories ? " in the world \"&f" + world.getName() + "&r&e\"" : "") + "!";
			String takeMessage = "&cYour permission to " + viewOrEdit.toLowerCase() + " \"&f" + owner.getName() + "&r&c\"'s " + (loadByGameMode ? gamemode.name().toLowerCase() : "") + getInvTypeFullName(invType) + (worldsHaveSeparateInventories ? " for the world \"&f" + world.getName() + "&r&c\"" : "") + "&c has been taken away.";
			EPLib.sendMessage(target, pluginName + (giveOrTake.equalsIgnoreCase("give") ? giveMessage : takeMessage));
			EPLib.sendMessage(owner, pluginName + "&eThe player \"&f" + target.getName() + "&r&e\"" + (giveOrTake.equalsIgnoreCase("give") ? " has successfully been given permission to " : "'s permission to ") + viewOrEdit.toLowerCase() + " your " + (loadByGameMode ? gamemode.name().toLowerCase() + " " : "") + getInvTypeFullName(invType) + (giveOrTake.equalsIgnoreCase("give") ? (worldsHaveSeparateInventories ? " in the world \"&f" + world.getName() + "&r&e\"" : "") + "!" : " has successfully been taken away."));
		} else if(giveOrTake.equalsIgnoreCase("take")) {
			success = permission.playerRemove(world, target.getName(), perm);
			if(target.hasPermission(perm)) {
				DEBUG("&fBut yet, \"" + target.getName() + "\" still has the above mentioned permission when doing \"if(target.hasPermission(perm));\".... Hmm...");
				return false;
			}
			
		}
		DEBUG("&fSuccessful in '" + (giveOrTake.equalsIgnoreCase("give") ? "giving" : "taking") + "' \"" + target.getName() + "\"'s permission to '&f" + viewOrEdit + "&r&f' \"" + owner.getName() + "\"'s \"&f" + invType + "&r&f\" inventory in world \"&f" + world.getName() + "&r&f\" for gamemode \"" + gamemode.name() + "\": " + (success + ".").toUpperCase());
		DEBUG("&aPermission given: \"&f\"" + perm + "\"&r&a\"...");
		DEBUG("&f----------> &4End of function: &cpublic void &6changeInvPerms&f()");
		rtrn = success;
		return rtrn;
	}

	/**
	 * @param cmd
	 * @param mode
	 * @param target
	 */
	void showUsageForCmd(String cmd, String mode, CommandSender target) {
		if(cmd.equalsIgnoreCase("view")) {
			if(mode.equalsIgnoreCase("all")) {
				sendMessage(target, pluginName + "&e/" + cmd + " &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &6{gamemode} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &d{worldName} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &d{worldName} &6{gamemode} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &6{gamemode} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &d{worldName} &3{inv|ender|extra|armorinv|armourinv}");
				sendMessage(target, pluginName + "&e/" + cmd + " &2{playerName} &d{worldName} &6{gamemode} &3{inv|ender|extra|armorinv|armourinv}");
				return;
			}
		}
	}

	/**
	 * @param oldWorld
	 * @return
	 */
	World getWhatWorldToUseFromWorld(World oldWorld) {
		World rtrn = oldWorld;
		if(worldsHaveSeparateInventories) {return rtrn;}
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
									if(server.getWorld(curWorldName) != null) {
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

	/**@author Brian_Entei @ 01-24-2014
	 * This shows the given target parameter's given inventory to the given user parameter.(Usage: boolean openedOrNot = openPlayerInventory(Player user(the player who sees the inventory screen), Player target(the owner of the inventory), GameMode gm(can be null), String invToOpen)
	 * @param user Player
	 * @param owner Player
	 * @param gm GameMode
	 * @param world World
	 * @param invToOpen String
	 * @return Whether or not the inventory was opened to the user for the given world(this depends on whether or not the user had permission to open the target's inventory for the given world).
	 */
	String openPlayerInventory(Player user, Player owner, GameMode gm, World world, String invToOpen) {
		invToOpen = invToOpen.toLowerCase();
		world = getWhatWorldToUseFromWorld(world);
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		sendDebugMsg("&a'user': " + user.getName());
		sendDebugMsg("&a'target': " + owner.getName());
		String success = "true";
		Inventory invToView = null;
		if(gm == null) {
			gm = user.getGameMode();
			sendDebugMsg("&athe 'gm' var was null; setting it to \"" + user.getName() + "\"'s current gamemode, which is &f" + gm.name().toLowerCase() + "&a.");
		}
		DEBUG("&f==========>> &6openPlayerInventory&f(&aPlayer &2user(\"" + user.getName() + "\")&f, &aPlayer &2owner(\"" + owner.getName() + "\")&f, &2GameMode gm(\"" + gm.name().toLowerCase() + "\")&f, &aWorld &2world(\"" + world.getName() + "\")&f, &aString &2invToOpen(\"" + invToOpen + "\")&f) {");
		sendDebugMsg("&a'gm': " + gm.name());
		sendDebugMsg("&a'world': " + worldName);
		sendDebugMsg("&a'invToOpen': " + invToOpen);
		String gamemode = gm.name().toLowerCase();
		String ownerName = owner.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + ownerName;
		String invFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".inv";
		String enderFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".enderInv";
		String extraFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".extraChestInv";
		String armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".armorInv");
		//String expFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + gamemode + ".exp");
		if(invToOpen.equals("inv")) {
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(owner) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Inventory" : "Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), owner));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested inventory because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "inventory. " + (owner.getName().equals(user.getName()) ? "Have you been" : "Has &f" + owner.getDisplayName() + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
		} else if(invToOpen.equals("ender") || invToOpen.equals("enderchest")) {
			if(invToOpen.equals("enderchest")) {invToOpen = "ender";}
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(owner) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Ender Chest" : "Ender Chest"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), owner));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested ender chest because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "ender chest. " + (owner.getName().equals(user.getName()) ? "Have you been" : "Has &f" + owner.getDisplayName() + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
		} else if(invToOpen.equals("extra")) {
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(owner) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Extra Inventory" : "Extra Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraFileName, FolderName, dataFolderName, false), owner));
			} catch (Exception e) {
				invToView = (loadByGameMode ? getPlayerExtraChest(owner, getWhatWorldToUseFromWorld(world != null ? world : owner.getWorld())/*server.getWorld(worldName)*/, gm) : getPlayerExtraChest(owner, server.getWorld(worldName)));
			}
		} else if(invToOpen.equals("armorinv") || invToOpen.equals("armourinv")) {
			if(invToOpen.equals("armourinv")) {invToOpen = "armorinv";}
			EPLib.sendConsoleMessage(pluginName + "&aAttempting to open target player \"" + owner.getName() + "\"'s armour inventory in \"" + gm.name().toLowerCase() + "\" mode, and in the world \"" + getWhatWorldToUseFromWorld(world != null ? world : owner.getWorld()).getName() + "\"...");

			try{invToView = InventoryAPI.setTitle(getPlayerInvName(owner) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Armour Inventory" : "Armour Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), owner));
			} catch (Exception e) {
				success = "false";
				//sendDebugMsg
				EPLib.sendConsoleMessage(pluginName + "&cCould not open the requested inventory because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "armor inventory. " + (owner.getName().equals(user.getName()) ? "Have you been" : "Has &f" + owner.getDisplayName() + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular armor inventory yet?");
			}
		}
		if(invToView != null) {
			//invToOpen = (invToOpen.equals("inv") ? "inv" : (invToOpen.equals("ender") ? "enderInv" : "extraChestInv"));
			if(canPlayerViewInventory(user, owner, gm, world, invToView) == true) {
				//playersUsingInvsInfo.add(user.getName() + "|" + worldName);
				user.openInventory(invToView);
			} else {
				success = "noperm";
				DEBUG("&aCould not open the requested inventory because &f" + user.getDisplayName() + "&r&e does not have any of the following permissions: &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + "." + owner.getName()) + "&e, &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + ".*"));
			}
		} else {
			success = "false";
		}
		DEBUG("&f==========>> &4End of &6openPlayerInventory&f()&4....");
		return success;
	}

	/**@author Brian_Entei @ 01-24-2014(Fixed on 01-29-2014)
	 * @param invTitle String
	 * @return The inventory type in string.
	 */
	public String getInvTypeFromTitle(String invTitle) {
		String rtrn = "";
		if(invTitle.contains("Ender")) {
			rtrn = "ender";
		} else if(invTitle.contains("Armour")) {
			rtrn = "armor";
		} else if(invTitle.contains("Extra")) {
			rtrn = "extra";
		} else if(invTitle.contains("Inventory")) {
			rtrn = "inv";
		} else {
			rtrn = "NULL";
		}
		DEBUG("===>> &4public &aString &6getInvTypeFromTitle&f(&aString &2invTitle(\"" + invTitle + "\")&f) resulted in: \"&a" + rtrn + "&f\"...");
		return rtrn;
	}

	/**@author Brian_Entei @ 01-24-2014
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param title String
	 * @return True if the player has permission to open the specified inventory; false otherwise. Note that this function will automatically return false if the user does not have the permission "eim.cmd.use.view".
	 */
	private boolean canPlayerViewInventory(Player editor, Player owner, GameMode gm, World world, Inventory inventory) {
		return canPlayerViewInventory(editor, owner.getName(), gm, world, inventory.getTitle());
	}

	/**@author Brian_Entei @ 01-24-2014
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param title String
	 * @return True if the player has permission to open the specified inventory; false otherwise. Note that this function will automatically return false if the user does not have the permission "eim.cmd.use.view".
	 */
	private boolean canPlayerViewInventory(Player editor, String ownerName, GameMode gm, World world, Inventory inventory) {
		return canPlayerViewInventory(editor, ownerName, gm, world, inventory.getTitle());
	}

	/**@author Brian_Entei @ 01-24-2014
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param title String
	 * @return True if the player has permission to open the specified inventory; false otherwise. Note that this function will automatically return false if the user does not have the permission "eim.cmd.use.view".
	 */
	private boolean canPlayerViewInventory(Player editor, String ownerName, GameMode gm, World world, String invTitle) {
		boolean canPlayerViewInv;
		if(editor.hasPermission("eim.cmd.use.view") == false) {//If the user can't even use the /view command, then
			return false;
		}
		world = getWhatWorldToUseFromWorld(world);
		String invType = getInvTypeFromTitle(invTitle);
		//String permissionTemplate = "eim.{view|edit}.gamemode.{survival|creative|adventure}.{inv|ender|extra|armor}.world.WORLDNAME.owner.PLAYERNAME";
		String perm = getPermissionStringForOpeningInventory(ownerName, gm, world, invType).replace("view", "view");
		String godPerm = "eim.*";
		String viewAllInvTypes = "eim.view.*";
		String abilityToViewOtherPeoplesInvs = "eim.view.others";
		if(editor.hasPermission(godPerm) || editor.hasPermission(viewAllInvTypes) || editor.hasPermission(abilityToViewOtherPeoplesInvs) || editor.getName().equals(ownerName)) {//If the user has perm to view other peoples' inventories OR if the user is the owner, then
			if(editor.hasPermission(godPerm) || editor.hasPermission(viewAllInvTypes) || editor.hasPermission(perm)) {//if the user has permission to view anything or to view the owner's inventory in specific, then
				canPlayerViewInv = true;
				DEBUG("&a\"&f" + editor.getName() + "&r&a\" did have the following permission to view \"&f" + ownerName + "&r&a\"'s inventory: &z&a\"&f" + perm + "&r&a\"!");
			} else {
				canPlayerViewInv = false;
				DEBUG("&c\"&f" + editor.getName() + "&r&c\" didn't have the following permission to view \"&f" + ownerName + "&r&c\"'s inventory: &z&c\"&f" + perm + "&r&c\"...");
			}
		} else {
			canPlayerViewInv = false;
			DEBUG("&c\"&f" + editor.getName() + "&r&c\" didn't have any of the following permissions to view \"&f" + ownerName + "&r&c\"'s inventory:&z&c\"&f" + godPerm + "&r&c\";&z&c\"&f" + viewAllInvTypes + "&r&c\";&z&c\"&f" + abilityToViewOtherPeoplesInvs + "&r&c\"... At least one of those permissions is required to view another players' inventory of any kind(usually&z&c\"&f" + abilityToViewOtherPeoplesInvs + "&r&c\" is used for this).");
		}
		return canPlayerViewInv;
	}

	public GameMode getGameModeFromInvTitle(Inventory inv) {
		return getGameModeFromInvTitle(inv.getTitle());
	}

	public GameMode getGameModeFromInvTitle(String invTitle) {
		return (loadByGameMode ? (invTitle.contains("'s S") ? GameMode.SURVIVAL : (invTitle.contains("'s C") ? GameMode.CREATIVE : (invTitle.contains("'s A") ? GameMode.ADVENTURE : null))) : null);
	}

	public String getInvTypeFullName(String invType) {
		return (invType.equalsIgnoreCase("inv") ? "inventory" : (invType.equalsIgnoreCase("ender") ? "ender chest" : (invType.equalsIgnoreCase("extra") ? "extra chest" : (invType.equalsIgnoreCase("armor") ? "armour inventory" : invType))));
	}

	/**@author Brian_Entei @ 01-24-2014
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param title String
	 * @return True if the player has permission to edit the specified inventory; false otherwise. Note that this function will automatically return false if the user does not have the permission "eim.cmd.use.view".
	 */
	public boolean canPlayerEditInventory(Player user, Player owner, GameMode gm, World world, Inventory inventory) {
		return canPlayerEditInventory(user, owner.getName(), gm, world, inventory.getTitle());
	}

	/**@author Brian_Entei @ 01-24-2014
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param title String
	 * @return True if the player has permission to edit the specified inventory; false otherwise. Note that this function will automatically return false if the user does not have the permission "eim.cmd.use.view".
	 */
	public boolean canPlayerEditInventory(Player user, String ownerName, GameMode gm, World world, Inventory inventory) {
		return canPlayerEditInventory(user, ownerName, gm, world, inventory.getTitle());
	}

	public boolean canPlayerEditInventory(Player editor, String ownerName, GameMode gm, World world, String invTitle) {
		boolean canPlayerEditInv;
		if(editor.hasPermission("eim.cmd.use.view") == false) {//If the user can't even use the /view command, then
			return false;
		}
		world = getWhatWorldToUseFromWorld(world);
		String invType = getInvTypeFromTitle(invTitle);
		//String permissionTemplate = "eim.{view|edit}.gamemode.{survival|creative|adventure}.{inv|ender|extra|armor}.world.WORLDNAME.owner.PLAYERNAME";
		String perm = getPermissionStringForOpeningInventory(ownerName, gm, world, invType).replace("view", "edit");
		String godPerm = "eim.*";
		String editAllInvTypes = "eim.edit.*";
		String abilityToEditOtherPeoplesInvs = "eim.edit.others";
		if(editor.hasPermission(godPerm) || editor.hasPermission(editAllInvTypes) || editor.hasPermission(abilityToEditOtherPeoplesInvs) || editor.getName().equals(ownerName)) {//If the user has perm to edit other peoples' inventories OR if the user is the owner, then
			if(editor.hasPermission(godPerm) || editor.hasPermission(editAllInvTypes) || editor.hasPermission(perm)) {//if the user has permission to edit anything or to edit the owner's inventory in specific, then
				canPlayerEditInv = true;
				DEBUG("&a\"&f" + editor.getName() + "&r&a\" did have the following permission to edit \"&f" + ownerName + "&r&a\"'s inventory: &z&a\"&f" + perm + "&r&a\"!");
			} else {
				canPlayerEditInv = false;
				DEBUG("&c\"&f" + editor.getName() + "&r&c\" didn't have the following permission to edit \"&f" + ownerName + "&r&c\"'s inventory: &z&c\"&f" + perm + "&r&c\"...");
			}
		} else {
			canPlayerEditInv = false;
			DEBUG("&c\"&f" + editor.getName() + "&r&c\" didn't have any of the following permissions to edit \"&f" + ownerName + "&r&c\"'s inventory:&z&c\"&f" + godPerm + "&r&c\";&z&c\"&f" + editAllInvTypes + "&r&c\";&z&c\"&f" + abilityToEditOtherPeoplesInvs + "&r&c\"... At least one of those permissions is required to edit another players' inventory of any kind(usually&z&c\"&f" + abilityToEditOtherPeoplesInvs + "&r&c\" is used for this).");
		}
		return canPlayerEditInv;
	}

	public String getPermissionStringForOpeningInventory(Player owner, GameMode gm, World world, Inventory inventory) {return getPermissionStringForOpeningInventory(owner.getName(), gm, world, getInvTypeFromTitle(inventory.getTitle()));}

	public String getPermissionStringForOpeningInventory(Player owner, GameMode gm, World world, String invType) {return getPermissionStringForOpeningInventory(owner.getName(), gm, world, invType);}

	public String getPermissionStringForOpeningInventory(String ownerName, GameMode gm, World world, String invType) {
		world = getWhatWorldToUseFromWorld(world);
		String perm;
		String gamemode = getGMStrForPerms(gm);
		String worldName = world.getName();
		String viewBothGameModeAndWorldInvs = "eim.view." + (loadByGameMode ? "gamemode." + gamemode : "") + invType + ".world." + worldName + ".owner." + ownerName;
		String viewOnlyGameModeInvs = "eim.view." + (loadByGameMode ? "gamemode." + gamemode : "") + invType + ".owner." + ownerName;
		String viewOnlyWorldInvs = "eim.view." + invType + ".world." + worldName + ".owner." + ownerName;
		String viewOnlyVanillaInvs = "eim.view." + invType + ".owner." + ownerName;
		if(loadByGameMode && worldsHaveSeparateInventories) {
			perm = viewBothGameModeAndWorldInvs;
		} else if(loadByGameMode) {
			perm = viewOnlyGameModeInvs;
		} else if(worldsHaveSeparateInventories) {
			perm = viewOnlyWorldInvs;
		} else {
			perm = viewOnlyVanillaInvs;
		}
		DEBUG("&cpublic &aString &6getPermissionStringForOpeningInventory&f(&aString &2ownerName(" + ownerName + ")&f, &2GameMode gm&f, &aWorld &2world(" + world.getName() + ")&f, &aString &2invType(" + invType + ")&f)&r&a\" resulted in the following permission:&z&a\"&f" + perm + "&r&a\".");
		return perm;
	}

	/**@param str String
	 * @return The first lowercase letter of the gamemode retrieved from the string, or "NULL" if the gamemode could not be determined. "survival", "2", etc. are some examples of what to enter.
	 */
	public static String getGameModeFromString(String str) {
		return (str.equalsIgnoreCase("survival") || str.equals("0") || str.equalsIgnoreCase("s") ? "s" : (str.equalsIgnoreCase("creative") || str.equals("1") || str.equalsIgnoreCase("c") ? "c" : (str.equalsIgnoreCase("adventure") || str.equals("2") || str.equalsIgnoreCase("a") ? "a" : "null")));
	}

	/**@param gm GameMode
	 * @return Either "survival.", "creative.", "adventure.", or "NULL.", depending on what gamemode the variable "gm" is.
	 */
	public static String getGMStrForPerms(GameMode gm) {
		return (gm.equals(GameMode.SURVIVAL) ? "survival." : (gm.equals(GameMode.CREATIVE) ? "survival." : (gm.equals(GameMode.ADVENTURE) ? "adventure." : "NULL.")));
	}

	/**This shows the given target parameter's given inventory to the given user parameter.(Usage: boolean openedOrNot = openPlayerInventory(Player user(the player who sees the inventory screen), Player target(the owner of the inventory), GameMode gm(can be null), String invToOpen)
	 * @param user Player
	 * @param target Player
	 * @param gm GameMode
	 * @param world World
	 * @param invToOpen String
	 * @return Whether or not the inventory was opened to the user for the given world(this depends on whether or not the user had permission to open the target's inventory for the given world).
	 */
	String openPlayerInventory(Player user, String ownerName, GameMode gm, World world, String invToOpen) {
		invToOpen = invToOpen.toLowerCase();
		world = getWhatWorldToUseFromWorld(world);
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		sendDebugMsg("&a'user': " + user.getName());
		sendDebugMsg("&a'target': " + ownerName);
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
		//String playerName = targetName;
		String FolderName = "Inventories" + java.io.File.separatorChar + ownerName;
		String invFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".inv";
		String enderFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".enderInv";
		String extraFileName = (worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".extraChestInv";
		String armorFileName = ((worldsHaveSeparateInventories ? worldName : "") + (loadByGameMode ? "." + gamemode : "") + ".armorInv");
		//String expFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + gamemode + ".exp");
		if(invToOpen.equals("inv")) {
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(ownerName) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Inventory" : "Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(invFileName, FolderName, dataFolderName, false), ownerName));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested inventory because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "inventory. " + (ownerName.equals(user.getName()) ? "Have you been" : "Has &f" + ownerName + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
		} else if(invToOpen.equals("ender") || invToOpen.equals("enderchest")) {
			if(invToOpen.equals("enderchest")) {invToOpen = "ender";}
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(ownerName) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Ender Chest" : "Ender Chest"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(enderFileName, FolderName, dataFolderName, false), ownerName));
			} catch (Exception e) {
				success = "false";
				sendDebugMsg("Could not open the requested ender chest because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "ender chest. " + (ownerName.equals(user.getName()) ? "Have you been" : "Has &f" + ownerName + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular inventory yet?");
			}
		} else if(invToOpen.equals("extra")) {
			try{invToView = InventoryAPI.setTitle(getPlayerInvName(ownerName) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Extra Inventory" : "Extra Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraFileName, FolderName, dataFolderName, false), ownerName));
			} catch (Exception e) {
				invToView = (loadByGameMode ? getPlayerExtraChest(ownerName, getWhatWorldToUseFromWorld(world != null ? world : user.getWorld())/*server.getWorld(worldName)*/, gm) : getPlayerExtraChest(ownerName, server.getWorld(worldName)));
			}
		} else if(invToOpen.equals("armorinv")) {

			EPLib.sendConsoleMessage(pluginName + "&aAttempting to open target player \"" + ownerName + "\"'s armour inventory in \"" + gm.name().toLowerCase() + "\" mode, and in the world \"" + getWhatWorldToUseFromWorld(world != null ? world : user.getWorld()).getName() + "\"...");

			try{invToView = InventoryAPI.setTitle(getPlayerInvName(ownerName) + " " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(gm) + " Armour Inventory" : "Armour Inventory"), InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(armorFileName, FolderName, dataFolderName, false), ownerName));
			} catch (Exception e) {
				success = "false";
				//sendDebugMsg
				EPLib.sendConsoleMessage(pluginName + "&cCould not open the requested inventory because \"&f" + e.getMessage() + "&r&e\"...");
				//e.printStackTrace();
				sendMessage(user, pluginName + "&eUnable to open the requested " + (loadByGameMode ? gamemode + " " : "") + "armor inventory. " + (ownerName.equals(user.getName()) ? "Have you been" : "Has &f" + ownerName + "&r&e been") + " to the world \"&f" + worldName + "&r&e\" and opened that particular armor inventory yet?");
			}
		}
		if(invToView != null) {
			//invToOpen = (invToOpen.equals("inv") ? "inv" : (invToOpen.equals("ender") ? "enderInv" : "extraChestInv"));
			if(canPlayerViewInventory(user, ownerName, gm, world, invToView) == true) {//if((user.hasPermission("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + "." + targetName) || user.hasPermission("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + ".*")) ) {
				//playersUsingInvsInfo.add(user.getName() + "|" + worldName);
				user.openInventory(invToView);
			} else {
				success = "noperm";
				sendDebugMsg("Could not open the requested inventory because &f" + user.getDisplayName() + "&r&e does not have any of the following permissions: &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + "." + ownerName) + "&e, &f" + ("eim.view." + (loadByGameMode ? "gamemode." + gamemode + "." : "") + invToOpen + ".*"));
			}
		} else {
			success = "false";
		}
		return success;
	}

	@SuppressWarnings("unused")
	private static void sendInvPermUsage(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {give|take} {view|edit} {inv|ender|extra|armorinv|armourinv} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} your {inv|ender|extra|armorinv|armourinv} Inventory(View this with \"&f/view&e\".");
	}

	@SuppressWarnings("unused")
	private static void sendInvPermUsageForConsole(CommandSender sender, String command) {
		sendMessage(sender, pluginName + "&eUsage: /" + command + " {ownerName} {give|take} {view|edit} {inv|ender|extra|armorinv|armourinv} {playerName}");
		sendMessage(sender, pluginName + "&eThis will {give|take} {playerName}'s permission to {view|edit} the {ownerName}'s {inv|ender|extra|armorinv|armourinv} Inventory.");
	}

	@SuppressWarnings("unused")
	private static void sendNotOnlineMessage(CommandSender sender, String arg) {
		sendMessage(sender, pluginName + "&e\"&f" + arg + "&e\" is not a player, or is not online.");
	}

	/**This function is for use in the "onInventoryClickEvent()" event. It updates the viewers' perspective of the owner's inv, then saves the owner's inv.
	 * @param owner - Player
	 * @param invTitle - String(The final viewInvScreen's .getTitle(); used for matching which inventory to update in the "updateViewersScreens(Player, String, boolean)" function.
	 * @param delCursorItem - boolean
	 */
	void updateViewScreensAndSave(final Player owner, final String invTitle, final boolean delCursorItem) {
		scheduler.scheduleSyncDelayedTask(plugin, new Thread() {
			@Override
			public void run() {
				updateViewersScreens(owner, invTitle, delCursorItem);
				DEBUG("&f----> }");
			}
		}, (long) 1.25); //5 would wait 0.25 seconds; 2.5 would wait 0.125 seconds; 1.25 waits 0.0625 seconds(lol)...\


		scheduler.scheduleSyncDelayedTask(plugin, new Thread() {
			@Override
			public void run() {
				savePlayerInventory(owner, owner.getWorld(), owner.getGameMode());
			}
		}, (long) 1.25); //5 would wait 0.25 seconds; 2.5 would wait 0.125 seconds; 1.25 waits 0.0625 seconds(lol)...
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerExpChangeEvent(org.bukkit.event.player.PlayerExpChangeEvent evt) {
		savePlayerExp(evt.getPlayer());
	}
	

	@SuppressWarnings("boxing")
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent evt) {
		//Player user = evt.getPlayer();
		String command = EPLib.getCommandFromMsg(evt.getMessage()).trim();
		if(evt.isCancelled() == false) {
			String[] args = EPLib.getArgumentsFromCommand(evt.getMessage());
			if(command.equalsIgnoreCase("clear")) {
				if(args.length >= 1) {
					final Player target = server.getPlayer(args[0]);
					if(target != null) {
						final MainInvClass main = this;
						scheduler.runTask(plugin, new Runnable() {
							@Override
							public void run() {
								main.updateOwnersInv(target);
							}
						});
					}
				}
			} else if(command.equalsIgnoreCase("effect")) {
				if(args.length == 4) {
					final Player target = server.getPlayer(args[0]);
					if(target == null) {
						return;
					}
					PotionEffectType effectType = PotionEffectType.getByName(args[1]);
					if(effectType == null) {
						return;
					}
					int dur = -1;
					int amp = -1;
					try{
						dur = Integer.valueOf(args[2]);
					} catch(NumberFormatException e) {
						DEBUG("" + e.getCause().getMessage());
					}
					try{
						amp = Integer.valueOf(args[3]);
					} catch(NumberFormatException e) {
						DEBUG("" + e.getCause().getMessage());
					}
					if(dur == -1) {
						return;
					}
					if(amp == -1) {
						return;
					}
					//PotionEffect effect = new PotionEffect(effectType, dur, amp);
					scheduler.runTask(plugin, new Runnable() {
						@Override
						public void run() {
							DEBUG("&f===== &aSaving potion effects");
							savePlayerPotionEffects(target, target.getWorld(), target.getGameMode());
						}
					});
				}
			}
		}
	}

	public boolean doesPlayerHaveAPotionEffect(Player player) {
		if(player.hasPotionEffect(PotionEffectType.ABSORPTION)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.BLINDNESS)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.CONFUSION)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.HARM)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.HEAL)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.HEALTH_BOOST)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.HUNGER)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.JUMP)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.POISON)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.REGENERATION)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.SATURATION)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.SLOW)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.SPEED)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.WEAKNESS)) {return true;}
		if(player.hasPotionEffect(PotionEffectType.WITHER)) {return true;}
		return false;
	}

	/**Removes all potion effects from the specified player.
	 * @param player Player
	 */
	public void removeAllPotionEffectsFromPlayer(Player player) {
		if(player.hasPotionEffect(PotionEffectType.ABSORPTION)) {player.removePotionEffect(PotionEffectType.ABSORPTION);}
		if(player.hasPotionEffect(PotionEffectType.BLINDNESS)) {player.removePotionEffect(PotionEffectType.BLINDNESS);}
		if(player.hasPotionEffect(PotionEffectType.CONFUSION)) {player.removePotionEffect(PotionEffectType.CONFUSION);}
		if(player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);}
		if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) {player.removePotionEffect(PotionEffectType.FAST_DIGGING);}
		if(player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);}
		if(player.hasPotionEffect(PotionEffectType.HARM)) {player.removePotionEffect(PotionEffectType.HARM);}
		if(player.hasPotionEffect(PotionEffectType.HEAL)) {player.removePotionEffect(PotionEffectType.HEAL);}
		if(player.hasPotionEffect(PotionEffectType.HEALTH_BOOST)) {player.removePotionEffect(PotionEffectType.HEALTH_BOOST);}
		if(player.hasPotionEffect(PotionEffectType.HUNGER)) {player.removePotionEffect(PotionEffectType.HUNGER);}
		if(player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);}
		if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {player.removePotionEffect(PotionEffectType.INVISIBILITY);}
		if(player.hasPotionEffect(PotionEffectType.JUMP)) {player.removePotionEffect(PotionEffectType.JUMP);}
		if(player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {player.removePotionEffect(PotionEffectType.NIGHT_VISION);}
		if(player.hasPotionEffect(PotionEffectType.POISON)) {player.removePotionEffect(PotionEffectType.POISON);}
		if(player.hasPotionEffect(PotionEffectType.REGENERATION)) {player.removePotionEffect(PotionEffectType.REGENERATION);}
		if(player.hasPotionEffect(PotionEffectType.SATURATION)) {player.removePotionEffect(PotionEffectType.SATURATION);}
		if(player.hasPotionEffect(PotionEffectType.SLOW)) {player.removePotionEffect(PotionEffectType.SLOW);}
		if(player.hasPotionEffect(PotionEffectType.SLOW_DIGGING)) {player.removePotionEffect(PotionEffectType.SLOW_DIGGING);}
		if(player.hasPotionEffect(PotionEffectType.SPEED)) {player.removePotionEffect(PotionEffectType.SPEED);}
		if(player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {player.removePotionEffect(PotionEffectType.WATER_BREATHING);}
		if(player.hasPotionEffect(PotionEffectType.WEAKNESS)) {player.removePotionEffect(PotionEffectType.WEAKNESS);}
		if(player.hasPotionEffect(PotionEffectType.WITHER)) {player.removePotionEffect(PotionEffectType.WITHER);}
		return;
	}

	/**Once started, this function loops itself every 30 seconds until it is stopped by the MainInvClass.stopLooping variable equaling true.
	 */
	public void updateAllPlayersPotionEffects() {
		DEBUG("Start of updateAllPlayersPotionEffects()");
		loopNum++;
		DEBUG("Loop number: " + loopNum);
		for(Player curPlayer : server.getOnlinePlayers()) {
			if(doesPlayerHaveAPotionEffect(curPlayer)) {
				savePlayerPotionEffects(curPlayer, curPlayer.getWorld(), curPlayer.getGameMode());
			}
		}
		DEBUG("End of updateAllPlayersPotionEffects()");
	}

	private void loopPotionEffectSaving() {
		if(stopLooping != true) {
			if(loopThread != null) {
				loopThread.interrupt();
			}
			loopThread = new Thread(null, null, "") {
				@SuppressWarnings("static-access")
				@Override
				public void run() {
					for(int i = loopNum; i < Integer.MAX_VALUE; i++) {
						
						DEBUG("for loop start:");
						try {
							loopThread.sleep(30000);//Every 30 seconds(hopefully).
						} catch (Exception e) {
							//FileMgmt.LogCrash(e, "run()", "Unable to wait 30000 milliseconds(30 seconds).", false, dataFolderName);
							DEBUG("Unable to wait 30 seconds... Cause: " + e.getCause()/*.getCause().getCause().getCause().getCause()*/.toString());
						}
						if(MainInvClass.stopLooping == true) {
							loopThread.interrupt();
							break;
						}
						updateAllPlayersPotionEffects();
						DEBUG("for loop end.");
					}
				}
			};
		}
	}

	///**
	// * 
	// * @param evt
	// */
	//public void onEntityPotionEffectChangeEvent(org.bukkit.event.entity.EntityPotionEffectChangeEvent evt) {
	//	Player player = evt.getPlayer();
	//	savePlayerPotionEffects(player, player.getWorld(), player.getGameMode());
	//}

	public void onPotionSplashEvent(org.bukkit.event.entity.PotionSplashEvent evt) {
		java.util.Collection<LivingEntity> getAffectedPlayers = evt.getAffectedEntities();
		for(LivingEntity curEntity : getAffectedPlayers) {
			if(curEntity instanceof Player) {
				Player curPlayer = (Player) curEntity;
				savePlayerPotionEffects(curPlayer, curPlayer.getWorld(), curPlayer.getGameMode());
			}
		}
	}

	@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryClickEvent(org.bukkit.event.inventory.InventoryClickEvent evt) {//For checking perms and updating inventory screens.
		if(evt.getCursor().getType().toString().equals("AIR") && evt.getSlot() == -999) {
			DEBUG("&fThe player is only clicking on the area around the inventory screen with nothing in their hand(on the cursor)...");
			return;
		}
		if(showDebugMsgs) {//Why waste the cpu power & time if the debug messages are disabled, right??
			DEBUG("&l&n================&r&f==============");
			DEBUG("&aCursor item: \"&f" + evt.getCursor().getType().toString() + "&r&a\"; clicked slot: \"&f" + evt.getSlot() + "&r&a\"...");
			DEBUG("&fitem.getDurability(): " + evt.getCursor().getDurability());
			DEBUG("&fitem.getAmount(): " + evt.getCursor().getAmount());
			if(evt.getCursor().getType() == Material.ENCHANTED_BOOK) {
				org.bukkit.inventory.meta.EnchantmentStorageMeta EnchantMeta = (org.bukkit.inventory.meta.EnchantmentStorageMeta) evt.getCursor().getItemMeta();
				if(EnchantMeta != null) {
					Map<Enchantment,Integer> isEnch = EnchantMeta.getStoredEnchants();
					if(isEnch.size() > 0) {
						int num = 0;
						for(Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
							num++;
							DEBUG("&f[" + num + "] MetaData Book Enchantments: \"" + ench.getKey().getName() + "\"");
						}
					} else {
						DEBUG("&fItem has enchanted book MetaData, but it is empty(??).");
					}
				} else {
					DEBUG("&fItem has no enchanted book MetaData.");
				}
			} else {
				DEBUG("&fItem is not an enchantment book: \"" + evt.getCursor().getType().name() + "\"");
			}
			Map<Enchantment,Integer> isEnch = evt.getCursor().getEnchantments();
			if(isEnch.size() > 0) {
				int num = 0;
				for(Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
					num++;
					DEBUG("&f[" + num + "] Item Enchantments: \"" + ench.getKey().getName() + "\"");
				}
			} else {
				DEBUG("&fItem has no Enchantments.");
			}
			org.bukkit.inventory.meta.ItemMeta meta = evt.getCursor().getItemMeta();
			if(meta != null) {
				if(meta.hasEnchants()) {
					isEnch = meta.getEnchants();
					if(isEnch.size() > 0) {
						int num = 0;
						for(Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
							num++;
							DEBUG("&f[" + num + "] MetaData Enchantments: \"" + ench.getKey().getName() + "\"");
						}
					}
				} else {
					DEBUG("&fItem has no MetaData Enchantments.");
				}
				
				if(meta.hasDisplayName()) {
					DEBUG("&fItem Display Name: \"" + meta.getDisplayName() + "\"");
				} else {
					DEBUG("&fItem has no Display Name.");
				}
				if(evt.getCursor().getItemMeta().hasLore()) {
					Iterator<String> it = meta.getLore().iterator();
					int num = 0;
					while(it.hasNext()) {
						num++;
						DEBUG("&f[" + num + "] Item Lore: \"" + it.next() + "\"");
						
					}
				} else {
					DEBUG("&fItem has no lore.");
				}
				
			} else {
				DEBUG("&fItem has no MetaData.");
			}
			
			if(evt.getCursor().getType() == Material.BOOK_AND_QUILL || evt.getCursor().getType() == Material.WRITTEN_BOOK) {
				org.bukkit.inventory.meta.BookMeta Bookmeta = (org.bukkit.inventory.meta.BookMeta) evt.getCursor().getItemMeta();
				if(Bookmeta != null) {
					String author = (Bookmeta.hasAuthor() ? Bookmeta.getAuthor() : null);
					String title = (Bookmeta.hasTitle() ? Bookmeta.getTitle() : null);
					if(author != null) {
						DEBUG("&fItem Author: \"" + author + "\"");
					} else {
						DEBUG("&fItem has no author.");
					}
					if(title != null) {
						DEBUG("&fItem book title: \"" + Bookmeta.getTitle() + "\"");
					} else {
						DEBUG("&fItem has no book title.");
					}
					if(Bookmeta.hasPages()) {
						List<String> pages = Bookmeta.getPages();
						int num = 0;
						for(String curPage : pages) {
							num++;
							DEBUG("&f Item book page[" + num + "]: \"" + curPage + "\"");
						}
					} else {
						DEBUG("&fItem has no book pages.");
					}
				} else {
					DEBUG("&fItem has no book MetaData.");
				}
			} else {
				DEBUG("&fItem is not a book of any kind: \"" + evt.getCursor().getType().name() + "\"");
			}
			DEBUG("&l&n================&r&f==============");
		}

		DEBUG("&f----> &cpublic void &6onInventoryClickEvent&f(&aInventoryClickEvent &2evt&f) {");
		final Player editor = (Player) evt.getWhoClicked();
		Inventory sourceInv = evt.getInventory();//editor.getOpenInventory().getTopInventory();
		//Inventory editorInv = editor.getOpenInventory().getBottomInventory();
		//final String editorInvName = editorInv.getTitle();
		final String invName = sourceInv.getTitle();
		final int invSize = sourceInv.getSize();
		Player owner;
		if(sourceInv.getHolder() instanceof Player) {
			owner = (Player) sourceInv.getHolder();
		} else {
			DEBUG("Error: wrong kind of entity owner: " + sourceInv.getTitle());
			//return; <-- What if the entity owner is a villager? Hmm?
			owner = editor; //'owner' can't equal null after this point
		}

		GameMode gm = owner.getGameMode();

		DEBUG((loadByGameMode ? "'gm': \"" + gm.name().toLowerCase() + "\"..." : ""));
		DEBUG("The invName is: \"" + invName + "\"...");


		// TODO This needs to be better! What if the owner was in the nether, but the viewer typed in "/view PLAYERNAME creative world inv"?
		World givenWorld = getWhatWorldToUseFromWorld(owner.getWorld());


		DEBUG("&c[NOTICE]&bInventory Name: &f" + invName + "&b; Clicked slot: &f" + evt.getRawSlot() + "&b; invSize: " + invSize + ";");

		if(true) {
			String invTitle = getPlayerInvName(owner) + (loadByGameMode ? " " + EPLib.getFirstLetterOfGameMode(owner.getGameMode()) : "") + " ";
			if(invName.equals("container.crafting")) {
				if(invSize == 5) {//You'd have pressed 'e' to get to this.
					if(evt.getRawSlot() >= 5 && evt.getRawSlot() <= 8) {
						DEBUG("&bThe owner clicked on one of their armor slots.");
						updateViewersScreens(owner, invTitle + "Inventory", false);
						invTitle += "Armour Inventory";
					} else if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 4) {
						DEBUG("&bThe owner clicked on one of their crafting slots.");
						invTitle += "Inventory";
					} else if(evt.getRawSlot() >= 9 && evt.getRawSlot() <= 44) {
						DEBUG("&bThe owner clicked on one of their inventory slots.");
						invTitle += "Inventory";
					}
				} else if(invSize == 10) {//You'd have opened a crafting table to get to this.
					if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 9) {
						DEBUG("&bThe owner clicked on one of the crafting table slots.");
						invTitle += "Inventory";
					} else if(evt.getRawSlot() >= 10 && evt.getRawSlot() <= 45) {
						DEBUG("&bThe owner clicked on one of their inventory slots.");
						invTitle += "Inventory";
					}
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.chest")) {//Default invSize == 27
				if(invSize == 27) {
					DEBUG("&bA player clicked in an inventory with a chest open.");
					if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 26) {
						DEBUG("&bA player clicked in a chest.");
						invTitle += "Inventory";
					} else {
						DEBUG("&bA player clicked in their inventory while having a chest inventory open.");
						invTitle += "Inventory";
					}
				} else {
					DEBUG("&bA non-standard container.chest invSize was used: \"" + invSize + "\"!");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.chestDouble")) {// Default invSize == 54
				if(invSize == 54) {
					DEBUG("&bA player clicked in an inventory with a double chest open.");
					if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 53) {
						DEBUG("&bA player clicked in a double chest.");
						invTitle += "Inventory";
					} else {
						DEBUG("&bA player clicked in their inventory while having a double chest open.");
						invTitle += "Inventory";
					}
				} else {
					DEBUG("&bA non-standard container.chestDouble invSize was used: \"" + invSize + "\"!");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.enderchest")) {//invSize == 27
				DEBUG("&bA player clicked in an inventory with an ender chest open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 26) {
					DEBUG("&bA player clicked in an ender chest.");
					invTitle += "Ender Chest";
				} else {
					DEBUG("&bA player clicked in their inventory while having an ender chest inventory open.");
					updateViewersScreens(owner, invTitle + "Inventory", false);//This is needed because this is the only time a vanilla Minecraft inventory screen has two of the main inventory types open in the same screen('inv' and 'ender')!
					invTitle += "Ender Chest";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.furnace")) {//invSize == 3
				DEBUG("&bA player clicked in an inventory with a furnace open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 2) {
					DEBUG("&bA player clicked on a furnace slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a furnace inventory open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("Enchant")) {//invSize == 1(lol)
				DEBUG("&bA player clicked in an inventory with an enchanting table open.");
				if(evt.getRawSlot() == 0) {
					DEBUG("&bA player clicked on the enchanting table's slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having an enchanting table open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("Repair")) {//invSize == 3
				DEBUG("&bA player clicked in an inventory with an anvil open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 2) {
					DEBUG("&bA player clicked in an anvil slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having an anvil open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.hopper")) {//invSize == 5
				DEBUG("&bA player clicked in an inventory with a hopper open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 4) {
					DEBUG("&bA player clicked in a hopper slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a hopper open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.dispenser")) {//invSize == 9
				DEBUG("&bA player clicked in an inventory with a dispenser open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 8) {
					DEBUG("&bA player clicked in a dispenser slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a dispenser open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.dropper")) {//invSize == 9
				DEBUG("&bA player clicked in an inventory with a dropper open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 8) {
					DEBUG("&bA player clicked in a dropper slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a dropper open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.brewing")) {//invSize == 4
				DEBUG("&bA player clicked in an inventory with a brewing stand open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 3) {
					DEBUG("&bA player clicked in a brewing stand slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a brewing stand open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("mob.villager")) {//invSize == 3
				DEBUG("&bA player clicked in an inventory with a villager's shop open.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 2) {
					DEBUG("&bA player clicked in a villager's shop slot.");
					invTitle += "Inventory";
				} else {
					DEBUG("&bA player clicked in their inventory while having a villager's shop open.");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else if(invName.equals("container.inventory")) {//usually a creative inventory, but not always...
				if(invSize == 36) {
					DEBUG("&bA player clicked in an inventory with a \"container.inventory\" inventory open.");
					//if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 35) {
					//	DEBUG("&bA player clicked in their \"container.inventory\" inventory(a creative inventory???).");
					//	invTitle += "Inventory";
					//} else {
					//	DEBUG("&bA player clicked in their inventory while having a \"container.inventory\" inventory open.&a(??? What inventory screen is this?!)");
						invTitle += "Inventory";
					//}
				} else {
					DEBUG("&bA non-standard container.chest invSize was used: \"" + invSize + "\"!");
					invTitle += "Inventory";
				}
				updateViewScreensAndSave(owner, invTitle, false);
				return;
			} else {
				DEBUG("&c[NOTICE]&fThe following inventory type is not a default minecraft inventory type:");
				DEBUG("&c[NOTICE]&&2" + invName);
			}
		}

		if(invName.contains("'s")) {//Here's where it gets... "interesting"...
			DEBUG("&aAn editing viewer(\"&f" + editor.getName() + "&r&a\") clicked on the owner(\"&f" + owner.getName() + "&r&a\")'s \"&f" + invName + "&r&a\" screen.");
			boolean canEditorEdit = canPlayerEditInventory(editor, owner, gm, owner.getWorld(), sourceInv);
			String invType = getInvTypeFromTitle(invName);
			String invTypeFullName = getInvTypeFullName(invType);
			GameMode invGameMode = getGameModeFromInvTitle(invName);
			if(invGameMode == null) {
				invGameMode = owner.getGameMode();
			}
			String perm = getPermissionStringForOpeningInventory(owner, gm, givenWorld, invType);
			DEBUG("&a'perm': \"&f" + perm + "&r&a\"...");
			if(invType.equals("inv")) {//if(invName.contains("Inventory") && invName.contains("Armour") == false && invName.contains("Extra") == false) {
				DEBUG("A viewer clicked in the owner's inventory screen.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 35) { //44
					DEBUG("A viewer clicked on one of the owner's inventory slots.");
					if(canEditorEdit) {
						updateOwnersInv(owner, editor, sourceInv, invName, givenWorld, owner.getGameMode());
					} else {
						tellPlayerTheyCantEdit(owner, editor, evt, invGameMode, invTypeFullName, givenWorld, perm);
					}
					//(editor.hasPermission(perm) || editor.hasPermission(perm + "." + owner.getName()) || editor.hasPermission(perm + ".*") || editor.hasPermission("eim.edit.*") || editor.hasPermission("eim.*"))
				} else {
					DEBUG("A viewer clicked in their inventory while having the owner's inventory open.");
				}
			} else if(invType.equals("ender")) {//if(invName.contains("Ender")) {//invSize == 27
				DEBUG("A viewer clicked in an inventory with the owner's ender chest open.");
				DEBUG("&fEvent.getRawSlot(): \"&4" + evt.getRawSlot() + "&f\"...");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 26) {
					DEBUG("A viewer clicked on one of the owner's ender chest slots.");
					if(canEditorEdit) {
						updateOwnersInv(owner, editor, sourceInv, invName, givenWorld, owner.getGameMode());
					} else {
						tellPlayerTheyCantEdit(owner, editor, evt, invGameMode, invTypeFullName, givenWorld, perm);
					}
				} else {DEBUG("A viewer clicked in their inventory while having the owner's ender chest open.");}
			} else if(invType.equals("extra")) {//if(invName.contains("Extra")) {//invSize == 54
				DEBUG("A viewer clicked in the owner's extra inventory screen.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 53) {// 0 - 53 / 54 - 89
					DEBUG("A viewer clicked on one of the owner's extra inventory slots.");
					if(canEditorEdit) {
						updateOwnersInv(owner, editor, sourceInv, invName, givenWorld, owner.getGameMode());
					} else {
						tellPlayerTheyCantEdit(owner, editor, evt, invGameMode, invTypeFullName, givenWorld, perm);
					}
				} else if(evt.getRawSlot() >= 54 && evt.getRawSlot() <= 89) {
					DEBUG("A viewer clicked on one of their inventory slots while viewing the owner's extra inventory.");
				}
			} else if(invType.equals("armor")) {//if(invName.contains("Armour")) {//invSize == 4
				//String invType = "armor";
				DEBUG("A viewer clicked in the owner's armor inventory screen.");
				if(evt.getRawSlot() >= 0 && evt.getRawSlot() <= 3) {// 0 - 8 / 9 - 44
					DEBUG("A viewer clicked on one of the owner's armor slots.");
					if(canEditorEdit) {
						updateOwnersInv(owner, editor, sourceInv, invName, givenWorld, owner.getGameMode());
					} else {
						tellPlayerTheyCantEdit(owner, editor, evt, invGameMode, invTypeFullName, givenWorld, perm);
					}
				} else if(evt.getRawSlot() >= 4 && evt.getRawSlot() <= 8) {
					DEBUG("A viewer clicked on one of the owner's dead armor slots.(Insta-close and &4msg&r&f.)");
					EPLib.sendMessage(editor, pluginName + "&4You cannot click there; it is a 'dead' slot, and will not be saved!");
					evt.setCancelled(true);
					editor.closeInventory();//Using a bukkit method here instead of "closePlayerInventory(editor);" for a reason.
					return;
				} else if(evt.getRawSlot() >= 9 && evt.getRawSlot() <= 44) {
					DEBUG("A viewer clicked on one of their inventory slots while viewing the owner's armour inventory.");
				}
			} else {
				DEBUG("&4Error: variable 'invType' equals: \"&f" + invType + "&r&4\"...");
			}
			//Since the editor will always have their personal inventory on the bottom, let's go ahead and update their inventory:
			DEBUG("&l&n========&r&5UPDATING the editor\"\"'s personal inventory because it's on the bottom!");
			updateOwnersInv(editor, editor, editor.getOpenInventory().getBottomInventory(), getPlayerInvName(editor, "Inventory"), editor.getWorld(), editor.getGameMode());
			return;
		}

		EPLib.sendConsoleMessage(pluginName + "&d\"&f" + invName + "&d\" is an un-implemented inventory name! Brian_Entei may need to implement it.");
		return;
	}

	private void tellPlayerTheyCantEdit(Player owner, Player editor, org.bukkit.event.inventory.InventoryClickEvent evt, GameMode invGameMode, String invTypeFullName, World givenWorld, String perm) {
		EPLib.sendMessage(editor, pluginName + "&cSorry, but it seems that \"&f" + owner.getName() + "&r&c\" hasn't given you permission to edit their " + (loadByGameMode ? invGameMode.name().toLowerCase() + " " : "") + invTypeFullName + (worldsHaveSeparateInventories ? " for the world \"&f" + givenWorld.getName() + "&r&c\"" : "")+ "...&z&ePerhaps you could ask them for the following permission: \"&f" + perm + "." + owner.getName() + "&c\"?");
		evt.setCancelled(true);
		closePlayerInventory(editor);
		return;
	}

	/**
	 * @param owner
	 * @param invType
	 * @return
	 */
	public String getPlayerInvName(Player owner, String invType) {
		return EPLib.limitStringToNumOfChars(owner.getName(), 12) + "'s " + (loadByGameMode ? EPLib.getFirstLetterOfGameMode(owner.getGameMode()) + " " : "") + invType;
	}

	/**Takes the player's .getName() and limits it to only 12 characters, then adds "'s" onto it. This function is used for coding convenience in this plugin.
	 * @param owner Player
	 * @return
	 * @see Main#getPlayerInvName(String ownerName)
	 */
	public String getPlayerInvName(Player owner) {
		return EPLib.limitStringToNumOfChars(owner.getName(), 12) + "'s";
	}

	/**Takes the supplied string and limits it to only 12 characters, then adds "'s" onto it. This function is used for coding convenience in this plugin.
	 * @param ownerName String
	 * @return
	 * @see Main#getPlayerInvName(Player owner)
	 */
	public String getPlayerInvName(String ownerName) {
		return EPLib.limitStringToNumOfChars(ownerName, 12) + "'s";
	}

	/**Loads the players' experience to file with the players' current gamemode and world(gamemode and world preferences are set in this plugin's config.yml file).
	 * @param player
	 * @see Main#loadPlayerExp(Player, World, GameMode)
	 */
	public void loadPlayerExp(Player player) {
		loadPlayerExp(player, player.getWorld(), player.getGameMode());
	}

	/**Loads the players' experience to file with the specified gamemode and world(gamemode and world preferences are set in this plugin's config.yml file).
	 * @param player
	 * @param world
	 * @param gm
	 * @see Main#loadPlayerExp(Player)
	 */
	public void loadPlayerExp(Player player, World world, GameMode gm) {
		if(manageExp) {
			world = getWhatWorldToUseFromWorld(world);
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String expFileName = "";
			if(loadByGameMode == false) {
				expFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".exp");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				expFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".exp");
			}
			try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
				player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
			} catch (Exception e) {
				EPLib.showDebugMsg(pluginName + "&eError loading file \"&f" + expFileName + "&e\"(Cause: \"&c" + e.toString() + "&e\"). Attempting to load from the " + (worldsHaveSeparateInventories ? "gamemode-specific" : "world-specific") + " version of this file; if unsuccessful, will save over it from the player's current exp instead.", true);
				//Start 'smart' loading
				if(loadByGameMode == false) {
					expFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".exp"); //Intentional swappage
					try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
						player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + expFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						expFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".exp"); //Intentional swappage
					}
					FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);
				} else {
					expFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".exp"); //Intentional swappage
					try{player.setLevel(InventoryAPI.deserializeLevel(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
						player.setExp(InventoryAPI.deserializeExp(FileMgmt.ReadFromFile(expFileName, FolderName, dataFolderName, false)));
						EPLib.showDebugMsg(pluginName + "&eSuccessfuly loaded from the file \"&f" + expFileName + "&r&e\" instead. Saving the contents of this file to the original one to prevent future data loss.", true);
					} catch (Exception e1) {
						expFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + player.getGameMode().name().toLowerCase() + ".exp"); //Intentional swappage
					}
					FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);
				}
				//End smart loading.
			}
		} else {
			EPLib.sendOneTimeMessage(pluginName + "&eThe var \"&fmanageExp&e\" was set to false in the config.yml; not managing player experience levels.", "console");
		}
	}

	/**Saves the players' experience to file with the specified gamemode and world(gamemode and world preferences are set in this plugin's config.yml file).
	 * @param player Player
	 * @param world World
	 * @param gm GameMode
	 */
	public void savePlayerExp(Player player, World world, GameMode gm) {
		if(manageExp) {
			world = getWhatWorldToUseFromWorld(world);
			String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
			String playerName = player.getName();
			String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
			String expFileName = "";

			if(loadByGameMode == false) {
				expFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".exp");
			} else {
				String gamemode = (gm == GameMode.SURVIVAL ? ".survival" : (gm == GameMode.CREATIVE ? ".creative" : ".adventure"));
				expFileName = ((worldsHaveSeparateInventories ? worldName : "") + gamemode + ".exp");
			}
			
			FileMgmt.WriteToFile(expFileName, InventoryAPI.serializeExperience(player), true, FolderName, dataFolderName);
		} else {
			EPLib.sendOneTimeMessage(pluginName + "&eThe var \"&fmanageExp&e\" was set to false in the config.yml; not managing player experience levels.", "console");
		}
	}

	/**Saves the players' experience to file with the players' current gamemode and world(gamemode and world preferences are set in this plugin's config.yml file).
	 * @param player Player
	 */
	public void savePlayerExp(Player player) {
		savePlayerExp(player, player.getWorld(), player.getGameMode());
	}

	/**
	 * @param owner Player
	 */
	void updateOwnersInv(final Player owner) {
		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				updateOwnersInv(owner, owner, owner.getInventory(), getPlayerInvName(owner, "Inventory"), owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));
			}
		}, (long) 5.0);
	}

	/**
	 * @param owner Player
	 * @param editor Player
	 * @param sourceInv Inventory
	 * @param invTitle String
	 * @param givenWorld World
	 * @param gm GameMode
	 */
	void updateOwnersInv(final Player owner, final Player editor, final Inventory sourceInv, final String invTitle, final World givenWorld, final GameMode gm) {
		DEBUG("------------- Applying changes made to \"" + owner.getName() + "\"'s inventory by \"" + editor.getName() + "\"....");
		owner.getInventory().setContents(sourceInv.getContents());
		DEBUG("Changes applied to \"" + owner.getName() + "\"'s inventory.");


		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				DEBUG("------------- Updating \"" + owner.getName() + "\"'s opened view of the inventory \"" + invTitle + "\"...");
				if((loadByGameMode ? owner.getGameMode().equals(gm) : true) && owner.getWorld().equals(givenWorld)) {
					owner.getInventory().setContents(sourceInv.getContents());
				}
				if(owner.getOpenInventory().getBottomInventory().getTitle().equals("container.inventory")) {
					if(owner.getOpenInventory().getBottomInventory().getSize() == 36) {
						owner.getOpenInventory().getBottomInventory().setContents(owner.getInventory().getContents());

						DEBUG("---------------------------- \"" + owner.getName() + "\"'s inventory should have updated.");

						scheduler.runTask(plugin, new Runnable() {
							@Override
							public void run() {
								GameMode gamemode = null;
								if(loadByGameMode) {
									gamemode = gm;
								}

								DEBUG("------------- Saving changes made to \"" + owner.getName() + "\"'s inventory.");
								savePlayerInventory(owner, givenWorld, gamemode);

								scheduler.runTask(plugin, new Runnable() {
									@Override
									public void run() {
										DEBUG("------------- Updating opened view screens of the inventory \"" + invTitle + "\"...");
										updateViewersScreens(owner, invTitle, false);
									}
								});

							}
						});
					} else {
						DEBUG("&f---------------------------- &c'owner.getOpenInventory().getBottomInventory().getSize()'&f == " + owner.getOpenInventory().getBottomInventory().getSize());
					}
				} else {
					DEBUG("&f---------------------------- &c'owner.getOpenInventory().getBottomInventory().getTitle()'&f == " + owner.getOpenInventory().getBottomInventory().getTitle());
				}
			}
		});
	}

	/**
	 * @param owner Player
	 * @param editor Player
	 * @param sourceInv Inventory
	 * @param invTitle String
	 * @param givenWorld World
	 * @param gm GameMode
	 */
	void updateOwnersEnderInv(final Player owner, final Player editor, final Inventory sourceInv, final String invTitle, final World givenWorld, final GameMode gm) {
		owner.getEnderChest().setContents(sourceInv.getContents());
		DEBUG("&2[NOTICE]&f(1/3): Set player \"" + owner.getName() + "\"'s ender chest to match what editor \"" + editor.getName() + "\" did.");

		savePlayerInventory(owner, givenWorld, gm);
		DEBUG("&2[NOTICE]&f(2/3): Saved \"" + owner.getName() + "\"'s ender chest.");

		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				updateViewersScreens(owner, invTitle, false);
				DEBUG("&2[NOTICE]&f(3/3): Updated everyone's view of \"" + owner.getName() + "\"'s ender chest.");
			}
		}, (long) 2.0);
	}

	/**
	 * @param owner Player
	 * @param editor Player
	 * @param sourceInv Inventory
	 * @param invTitle String
	 * @param givenWorld World
	 * @param gm GameMode
	 */
	void updateOwnersExtraInv(final Player owner, final Player editor, final Inventory sourceInv, final String invTitle, final World givenWorld, final GameMode gm) {
		DEBUG("------------- Applying changes made to \"" + owner.getName() + "\"'s extra chest inventory by \"" + editor.getName() + "\"....");
		savePlayerExtraChest(owner, sourceInv, givenWorld, gm);

		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				DEBUG("------------- Updating \"" + owner.getName() + "\"'s opened view of the extra chest inventory \"" + invTitle + "\"...");
				if(owner.getOpenInventory().getTopInventory().getTitle().equals(invTitle)) {
					if(owner.getOpenInventory().getTopInventory().getSize() == 54) {
						owner.getOpenInventory().getTopInventory().setContents(sourceInv.getContents());

						DEBUG("---------------------------- \"" + owner.getName() + "\"'s extra chest inventory should have updated.");

						scheduler.runTask(plugin, new Runnable() {
							@Override
							public void run() {
								DEBUG("------------- Saving changes made to \"" + owner.getName() + "\"'s extra chest inventory.");
								savePlayerInventory(owner, givenWorld, gm);//Save their main inventory first, then:

								savePlayerExtraChest(owner, sourceInv, owner.getWorld(), owner.getGameMode());//Save their extra chest!

								scheduler.runTask(plugin, new Runnable() {
									@Override
									public void run() {
										DEBUG("------------- Updating opened view screens of the inventory \"" + invTitle + "\"...");
										updateViewersScreens(owner, invTitle, false);
									}
								});

							}
						});
					} else {
						DEBUG("&f---------------------------- &c'owner.getOpenInventory().getTopInventory().getSize()'&f == " + owner.getOpenInventory().getTopInventory().getSize());
					}
				} else {
					DEBUG("&f---------------------------- &c'owner.getOpenInventory().getTitle()'&f == " + owner.getOpenInventory().getTitle());
				}
			}
		});
	}

	/**@param owner Player
	 * @param editor Player
	 * @param sourceInv Inventory
	 * @param invTitle String
	 * @param givenWorld World
	 * @param gm GameMode
	 */
	void updateOwnersArmorInv(final Player owner, final Player editor, final Inventory sourceInv, final String invTitle, final World givenWorld, final GameMode gm) {

		DEBUG("------------- Applying changes made to \"" + owner.getName() + "\"'s armour inventory by \"" + editor.getName() + "\"....");
		owner.getInventory().setArmorContents(new ItemStack[] {sourceInv.getItem(0), sourceInv.getItem(1), sourceInv.getItem(2), sourceInv.getItem(3)});

		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				DEBUG("------------- Updating \"" + owner.getName() + "\"'s opened view of the inventory \"" + invTitle + "\"...");
				//5 = helmet, 8 = shoes
				if(owner.getOpenInventory().getTitle().equals("container.crafting")) {
					if(owner.getOpenInventory().getTopInventory().getSize() == 5) {
						owner.getOpenInventory().setItem(5, sourceInv.getItem(3));//Helmet
						owner.getOpenInventory().setItem(6, sourceInv.getItem(2));//Chestplate
						owner.getOpenInventory().setItem(7, sourceInv.getItem(1));//Leggings
						owner.getOpenInventory().setItem(8, sourceInv.getItem(0));//Boots

						DEBUG("---------------------------- \"\"'s armour inventory should have updated.");

						scheduler.runTask(plugin, new Runnable() {
							@Override
							public void run() {
								DEBUG("------------- Saving changes made to \"" + owner.getName() + "\"'s armour inventory.");
								savePlayerInventory(owner, givenWorld, gm);

								scheduler.runTask(plugin, new Runnable() {
									@Override
									public void run() {
										DEBUG("------------- Updating opened view screens of the inventory \"" + invTitle + "\"...");
										updateViewersScreens(owner, invTitle, false);
									}
								});

							}
						});
					} else {
						DEBUG("&f---------------------------- &c'owner.getOpenInventory().getTopInventory().getSize()'&f == " + owner.getOpenInventory().getTopInventory().getSize());
					}
				} else {
					DEBUG("&f---------------------------- &c'owner.getOpenInventory().getTitle()'&f == " + owner.getOpenInventory().getTitle());
				}
			}
		});
	}

	/**Closes the target players' inventory, and sets their cursor to null. Use closePlayerInventory(Player target, boolean delCursorItem) if you want to specify whether or not you want the cursor item deleted.
	 * @param target
	 */
	public void closePlayerInventory(final Player target) {
		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				target.getOpenInventory().setCursor(null);
				target.getOpenInventory().close();
			}
		});
	}

	/**Closes the target players' inventory, and sets their cursor to null if "delCursorItem" is set to true.
	 * @param target
	 */
	public void closePlayerInventory(final Player target, final boolean delCursorItem) {
		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				if(delCursorItem) {target.getOpenInventory().setCursor(null);}
				target.getOpenInventory().close();
			}
		});
	}

	/*@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryOpenEvent(InventoryOpenEvent evt) {//For checking perms.
		Player player = server.getPlayer(evt.getPlayer().getName());
		sendDebugMsg("&1&n=====&r&6onInventoryOpenEvent&f(&aInventoryOpenEvent &2evt&f)&1&n=====&f: " + player.getName());
		sendDebugMsg("&aThe inventory title that &f" + player.getDisplayName() + "&r&a has opened is: &e" + evt.getInventory().getTitle());
	}*/

	/*@EventHandler(priority=EventPriority.LOWEST)
	public void onInventoryCloseEvent(InventoryCloseEvent evt) {//For saving.
		sendDevMsg("&1&n1");
		Player viewer = server.getPlayer(evt.getPlayer().getName());
		Inventory inv = evt.getInventory();
		final String invName = inv.getTitle();
		Player owner = null;
		if(inv.getHolder() instanceof Player) {
			sendDevMsg("&1&n2");
			owner = (Player) inv.getHolder();
		}
		String invType = "";
		if(inv.getTitle().contains("Extra Inventory")) {
			sendDevMsg("&1&n3 A");
			invType = "extra";
		} else if(inv.getTitle().contains("Ender Chest")) {
			sendDevMsg("&1&n3 B");
			invType = "ender";
		} else if(inv.getTitle().contains("Inventory") && (inv.getTitle().contains("Armour") == false)) {
			sendDevMsg("&1&n3 C");
			invType = "inv";
		} else if(inv.getTitle().equals("container.enderchest")) {
			sendDevMsg("&1&n3 D");
			invType = "container.enderchest";
		} else if(inv.getTitle().equals("container.crafting") || inv.getTitle().equals("container.inventory")) {
			sendDevMsg("&1&n3 E");
			invType = "container.inventory";
		} else if(inv.getTitle().contains("Armour")) {
			sendDevMsg("&1&n3 F");
			invType = "armor";
		} else {
			sendDevMsg("&1&n3 G");
		}
		if(owner != null) {
			if(invType.equals("container.inventory")) {
				sendDevMsg("&eDebug: 'invType' == \"" + invType + "\"! The inventory title is: &f" + inv.getTitle());
				updateOwnersInvAndSave(owner, owner.getInventory(), (loadByGameMode ? owner.getGameMode() : null), "inv", owner.getWorld(), true, false);

				return;
			}
			if(invType.equals("container.enderchest")) {
				sendDevMsg("&eDebug: 'invType' == \"" + invType + "\"! The inventory title is: &f" + inv.getTitle());
				updateOwnersInvAndSave(owner, owner.getEnderChest(), (loadByGameMode ? owner.getGameMode() : null), "ender", owner.getWorld(), true, false);

				return;
			}
			if(invType.equals("")) {
				sendDevMsg("'invType' == \"\"! The inventory title is: &f" + inv.getTitle() + "&e; &1&nThis means we can't very well do anything...");
				return;
			}
			if(invType.equals("armor")) {
				sendDevMsg("&eDebug: 'invType' == \"" + invType + "\"! The inventory title is: &f" + inv.getTitle());

				// disabledTODO HERE!!!!!

				updateOwnersInvAndSave(owner, getArmorInv(owner), (loadByGameMode ? owner.getGameMode() : null), invType, owner.getWorld(), true, true);


				return;
			}
			if(viewer.getName().equals(owner.getName())) {
				sendDevMsg("'owner'(&f" + owner.getName() + "&r&e) == 'player'(&f" + viewer.getName() + "&r&e)!");
				sendDevMsg("&1&nWe don't have to check for perms -- unless the owner isn't in the same gamemode and world as the inventory!");
				if((invType.equalsIgnoreCase("inv") || invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") || invType.equalsIgnoreCase("extra")) == false) {
					sendDevMsg("&cThe invType didn't pan out! It still equals: \"&f" + invType + "&c\"!");
					return;
				}
				updateOwnersInvAndSave(owner, inv, (loadByGameMode ? owner.getGameMode() : null), invType, owner.getWorld(), true, false);
			} else {
				sendDevMsg("'owner'(&f" + owner.getName() + "&r&e) != 'player'(&f" + viewer.getName() + "&r&e)!");
				sendDevMsg("&1&nNow we check for perms!");
				String pGamemode = (loadByGameMode ? "gamemode." + (invName.contains("'s S") ? "survival." : (invName.contains("'s C") ? "creative." : (invName.contains("'s A") ? "adventure." : "UNKNOWN."))) : "");
				String perm = "eim.edit." + pGamemode + invType;
				if(viewer.hasPermission(perm) || viewer.hasPermission(perm + "." + owner.getName()) || viewer.hasPermission(perm + ".*") || viewer.hasPermission("eim.edit.*") || viewer.hasPermission("eim.*")) {
					if((invType.equalsIgnoreCase("inv") || invType.equalsIgnoreCase("ender") || invType.equalsIgnoreCase("enderchest") || invType.equalsIgnoreCase("extra")) == false) {
						sendDevMsg("&cThe invType didn't pan out! It still equals: \"&f" + invType + "&c\"!");
						return;
					}
					updateOwnersInvAndSave(owner, inv, (loadByGameMode ? owner.getGameMode() : null), invType, owner.getWorld(), true, false);
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
	}*/

	/**@param owner
	 * @return The 9 slot display inventory(0 - 3 usable slots; the slots 4 - 8 are duds), which can be used to display the owners' armor on someone else's screen.
	 */
	public Inventory getArmorInv(Player owner) {
		Inventory armorInv = InventoryAPI.deserializeInventory(InventoryAPI.serializeInventory(owner, "armor"), owner);
		return armorInv;
	}

	/*World getWorldFromPlayerInvInfo(Player owner, boolean removeFromList) {// This is used to help with maintaining inventory screens.
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
	}*/

	/*void removePlayerFromList(final String plyrToRemove) {
		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
				playersUsingInvsInfo.remove(plyrToRemove);
			}
		});
	}*/

	/*void listPlayerInvScreenData() {
		sendConsoleMessage(pluginName + "&aDebug: Listing all players who are currently using inventory screens:");
		int x = 0;
		for(String player_worldName : playersUsingInvsInfo) {
			x++;
			sendConsoleMessage(pluginName + "&aDebug:(&f" + x + "&r&a) &f" + player_worldName);
		}
	}*/

	public Inventory savePlayerExtraChest(Player target, Inventory newInv, World world, GameMode gm) {
		world = getWhatWorldToUseFromWorld(world);
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		String extraInvFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + gm.name().toLowerCase() + ".extraChestInv");
		FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(newInv), true, FolderName, dataFolderName);
		return newInv;
	}

	public Inventory savePlayerExtraChest(Player target, Inventory newInv, World world) {
		world = getWhatWorldToUseFromWorld(world);
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		String extraInvFileName = (worldsHaveSeparateInventories ? worldName : "") + ".extraChestInv";
		FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(newInv), true, FolderName, dataFolderName);
		return newInv;
	}

	public Inventory getPlayerExtraChest(String targetName, World world, GameMode gm) {
		world = getWhatWorldToUseFromWorld(world);
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String FolderName = "Inventories" + java.io.File.separatorChar + targetName;
		//String invName = (targetName + "'s Extra Inventory");
		String extraInvFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + gm.name().toLowerCase() + ".extraChestInv");
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), targetName);
		} catch(Exception e) {
			invToOpen = null;//server.createInventory(Bukkit.getPlayer(targetName), 54, invName);
			//FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}

	public Inventory getPlayerExtraChest(Player target, World world, GameMode gm) {
		world = getWhatWorldToUseFromWorld(world);
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = ((worldsHaveSeparateInventories ? worldName : "") + "." + gm.name().toLowerCase() + ".extraChestInv");
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = server.createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}

	public Inventory getPlayerExtraChest(String targetName, World world) {
		world = getWhatWorldToUseFromWorld(world);
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String FolderName = "Inventories" + java.io.File.separatorChar + targetName;
		//String invName = (targetName + "'s Extra Inventory");
		String extraInvFileName = ((worldsHaveSeparateInventories ? worldName : "") + ".extraChestInv");
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), targetName);
		} catch(Exception e) {
			invToOpen = null;//server.createInventory(Bukkit.getPlayer(targetName), 54, invName);
			//FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}

	public Inventory getPlayerExtraChest(Player target, World world) {
		world = getWhatWorldToUseFromWorld(world);
		Inventory invToOpen = null;
		String worldName = world.getName().toLowerCase().replaceAll(" ", "_");
		String playerName = target.getName();
		String FolderName = "Inventories" + java.io.File.separatorChar + playerName;
		String invName = (playerName + "'s Extra Inventory");
		String extraInvFileName = (worldsHaveSeparateInventories ? worldName : "") + ".extraChestInv";
		try{invToOpen = InventoryAPI.deserializeInventory(FileMgmt.ReadFromFile(extraInvFileName, FolderName, dataFolderName, false), target);
		} catch(Exception e) {
			invToOpen = server.createInventory(target, 54, invName);
			FileMgmt.WriteToFile(extraInvFileName, InventoryAPI.serializeInventory(invToOpen), true, FolderName, dataFolderName);
		}
		return invToOpen;
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onInventoryEvent(org.bukkit.event.inventory.InventoryEvent evt) {
		if(evt.getInventory().getHolder() instanceof Player) {
			final Player owner = (Player) evt.getInventory().getHolder();
			savePlayerInventory(owner, owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));

			scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					updateOwnersInv(owner);
				}
			}, (long) 2.0);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPickupItemEvent(org.bukkit.event.player.PlayerPickupItemEvent evt) {
		if(evt.isCancelled() == false) {
			final Player owner = evt.getPlayer();
			savePlayerInventory(owner, owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));

			scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					updateOwnersInv(owner);
				}
			}, (long) 2.0);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerDropItemEvent(org.bukkit.event.player.PlayerDropItemEvent evt) {
		if(evt.isCancelled() == false) {
			final Player owner = evt.getPlayer();
			savePlayerInventory(owner, owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));

			scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					updateOwnersInv(owner);
				}
			}, (long) 2.0);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerItemHeldEvent(org.bukkit.event.player.PlayerItemHeldEvent evt) {
		if(evt.isCancelled() == false) {
			final Player owner = evt.getPlayer();
			savePlayerInventory(owner, owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));

			scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
				@Override
				public void run() {
					updateOwnersInv(owner);
				}
			}, (long) 2.0);
		}
	}

	@SuppressWarnings("unused")
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPlaceBlockEvent(org.bukkit.event.player.PlayerInteractEvent evt) throws org.bukkit.event.EventException {
		if(evt.isCancelled() == false) {
			final Player owner = evt.getPlayer();
			if(owner != null) {
				savePlayerInventory(owner, owner.getWorld(), (loadByGameMode ? owner.getGameMode() : null));

				scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
					@Override
					public void run() {
						updateOwnersInv(owner);
					}
				}, (long) 2.0);
			}
		}
	}

	//updateOwnersInvAndSave() became a moot method, and was therefor deleted.

	/**If editing an inventory before this method, be sure to use this method a few milliseconds(1.25 ticks would do) AFTER you edit it. Also use this method 1.25 ticks AFTER saving, if you are going to save after it.<br>Otherwise, you'll end up with messed-up inventory screens and improperly saved files...
	 * @param owner -Player 
	 * @param invTitle - String
	 * @param delCursorItem - boolean
	 * @return Number of times successfully updated an inventory screen, in integers.
	 */
	public int updateViewersScreens(final Player owner, final String invTitle, final boolean delCursorItem) {
		NumberOfUpdates = 0;
		scheduler.runTask(plugin, new Runnable() {
			@Override
			public void run() {
			DEBUG("&f------> &cpublic int &6updateViewersScreens&f(&aPlayer &2owner(" + owner.getName() + ")&f, &aString &2invTitle(\"" + invTitle + "\")&f, &cboolean &2delCursorItem(" + delCursorItem + ")&f) {");


			String invType = "";
			String mode = "vanilla"; //Or "custom"; this means the inv is either a vanilla mc screen, or a custom screen(made by this plugin).

			GameMode customInvGameMode = null;

			if(invTitle.contains("'s")) {
				mode = "custom";

				if(loadByGameMode) {
					if(invTitle.contains("'s S")) {
						customInvGameMode = GameMode.SURVIVAL;
					} else if(invTitle.contains("'s C")) {
						customInvGameMode = GameMode.CREATIVE;
					} else if(invTitle.contains("'s A")) {
						customInvGameMode = GameMode.ADVENTURE;
					} else {
						DEBUG("&c[WARNING]&f: The gamemode could not be determined! :o");
						return;
					}
				}
			} else {
				mode = "vanilla";
			}

			if(mode.equals("vanilla")) {
				DEBUG("What should be done here??? I forgot :p");
				return;
			} else if(mode.equals("custom")) {
				if(invTitle.contains("Inventory") && (invTitle.contains("Armour") == false && invTitle.contains("Extra") == false)) {
					invType = "inv";
				} else if(invTitle.contains("Ender")) {
					invType = "ender";
				} else if(invTitle.contains("Extra")) {
					invType = "extra";
				} else if(invTitle.contains("Armour")) {
					invType = "armor";
				}
			} else {
				DEBUG("&c[WARNING]&f: The internal variable \"mode\" doesn't equal \"vanilla\" or \"custom\"; instead, it equals: \"" + mode + "\"... Terminating function due to critical error.");
				return;
			}


			for(Player curPlayer : server.getOnlinePlayers()) {
				org.bukkit.inventory.InventoryView curInvView = curPlayer.getOpenInventory();

				Inventory curTopInv = curInvView.getTopInventory();
				Inventory curBottomInv = curInvView.getBottomInventory();

				DEBUG("&2[FOR-LOOP]&f: 'curPlayer': \"&a" + curPlayer.getName() + "&f\"; 'invTitle' to match: \"&a" + invTitle + "&f\";&z'customInvGameMode': \"&a" + (loadByGameMode ? (customInvGameMode != null ? customInvGameMode.name().toLowerCase() : "<null>") : "<noGM>") + "&f\";&z\"curplayer\"'s top inventory name: \"&a" + curTopInv.getTitle() + "&f\"; bottom: \"&a" + curBottomInv.getTitle() + "&f\"; ...");

				if(curTopInv.getTitle().contains(invTitle)) {
					DEBUG("curPlayer \"" + curPlayer.getName() + "\"'s open top inventory contains the 'invTitle' string!");

					if(mode.equals("custom")) {
						DEBUG("'mode' equals \"custom\"! 'invType' equals \"" + invType + "\".");
						if(invType.equals("inv")) {
							curPlayer.getOpenInventory().getTopInventory().setContents(owner.getInventory().getContents());
							DEBUG("Set (viewer)\"" + curPlayer.getName() + "\"'s top open inv. to (owner)\"" + owner.getName() + "\"'s " + (loadByGameMode ? owner.getGameMode().name().toLowerCase() + " " : "") + "inventory!");

							NumberOfUpdates += 1;

						} else if(invType.equals("ender")) {
							curPlayer.getOpenInventory().getTopInventory().setContents(owner.getEnderChest().getContents());
							DEBUG("Set (viewer)\"" + curPlayer.getName() + "\"'s top open inv. to (owner)\"" + owner.getName() + "\"'s " + (loadByGameMode ? owner.getGameMode().name().toLowerCase() + " " : "") + "inventory!");

							NumberOfUpdates += 1;

						} else if(invType.equals("extra")) {
							curPlayer.getOpenInventory().getTopInventory().setContents((loadByGameMode ? getPlayerExtraChest(owner, owner.getWorld(), customInvGameMode) : getPlayerExtraChest(owner, owner.getWorld())).getContents());
							DEBUG("Set (viewer)\"" + curPlayer.getName() + "\"'s top open inv. to (owner)\"" + owner.getName() + "\"'s " + (loadByGameMode ? owner.getGameMode().name().toLowerCase() + " " : "") + "inventory!");

							NumberOfUpdates += 1;

						} else if(invType.equals("armor")) {
							curPlayer.getOpenInventory().getTopInventory().setContents(getArmorInv(owner).getContents());
							DEBUG("Set (viewer)\"" + curPlayer.getName() + "\"'s top open inv. to (owner)\"" + owner.getName() + "\"'s " + (loadByGameMode ? owner.getGameMode().name().toLowerCase() + " " : "") + "inventory!");

							NumberOfUpdates += 1;

						}
					} else if(mode.equals("vanilla")) { 
						DEBUG("[1]What should be done here? I forgot :p");
					}
				}
			}

			DEBUG("&f------> }");
			}
		});
		return NumberOfUpdates;
	}

	//updateViewersInvScreens() became a moot method, and was therefor deleted.

	String sendDebugMsg(String str) {return ((forceDebugMsgs || showDebugMsgs) ? sendConsoleMessage(pluginName + "&eDebug: " + str) : EPLib.formatColorCodes(pluginName + "&eDebug: " + str));}
}