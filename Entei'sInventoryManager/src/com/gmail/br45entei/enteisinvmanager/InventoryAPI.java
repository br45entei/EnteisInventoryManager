package com.gmail.br45entei.enteisinvmanager;
//package YOUR.PACKAGE.HERE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**This class is a useful tool if you want a simple way to convert any inventory to a String, and vice versa.
 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
 */
public class InventoryAPI {
	/**This is the tag that is used to start the item listings when serializing.
	 */
	private static String item_start = "#";
	/**This is the tag that is used at the end of every serialized ItemStack.
	 */
	private static String item_separator = ";";
	/**This is the tag that is used at the start of every item attribute(like an enchantment, item damage, item lore, the book title, the item's name, the book author, a book page, etc.)
	 */
	private static String attribute_start = ":";
	/**This is the tag that is used to separate every item attribute(like an enchantment, item damage, item lore, the book title, the item's name, the book author, a book page, etc.)
	 */
	private static String attribute_separator = "@";
	/**Converts a set of symbols that are used within the serializing functions to prevent data corruption, as well as any ChatColor characters(it changes them to their respective '&' codes). 
	 * @param str String
	 * @return The given string, with certain symbols changed to keep inventory formatting from getting broken because of a simple character.
	 * @see InventoryAPI#convertSymbolsForLoading(String str)
	 */
	public static String convertSymbolsForSaving(String str) {
		str = str.replaceAll(item_separator, "<item_separator>")
			.replaceAll(item_start, "<item_start>")
			.replaceAll(attribute_start, "<attribute_start>")
			.replaceAll(attribute_separator, "<attribute_separator>")
			.replaceAll("\n", "<Nline>")
			.replaceAll("\r", "<Rline>")
			.replaceAll("(?i)\u00A7b","&b")
			.replaceAll("\u00A70","&0")
			.replaceAll("\u00A79","&9")
			.replaceAll("(?i)\u00A7l","&l")
			.replaceAll("\u00A73","&3")
			.replaceAll("\u00A71","&1")
			.replaceAll("\u00A78","&8")
			.replaceAll("\u00A72","&2")
			.replaceAll("\u00A75","&5")
			.replaceAll("\u00A74","&4")
			.replaceAll("\u00A76","&6")
			.replaceAll("\u00A77","&7")
			.replaceAll("(?i)\u00A7a","&a")
			.replaceAll("(?i)\u00A7o","&o")
			.replaceAll("(?i)\u00A7d","&d")
			.replaceAll("(?i)\u00A7k","&k")
			.replaceAll("(?i)\u00A7c","&c")
			.replaceAll("(?i)\u00A7m","&m")
			.replaceAll("(?i)\u00A7n","&n")
			.replaceAll("(?i)\u00A7f","&f")
			.replaceAll("(?i)\u00A7e","&e")
			.replaceAll("(?i)\u00A7r","&r");
		return str;
	}
	/**The opposite function of {@link InventoryAPI#convertSymbolsForSaving(String)}.
	 * @param str String
	 * @return The given string, restored to its original state.
	 * @see {@link InventoryAPI#convertSymbolsForSaving(String str)}
	 */
	public static String convertSymbolsForLoading(String str) {
		str = str.replaceAll("<item_separator>", item_separator)
			.replaceAll("<item_start>", item_start)
			.replaceAll("<attribute_start>", attribute_start)
			.replaceAll("<attribute_separator>", attribute_separator)
			.replaceAll("<Nline>", "\n")
			.replaceAll("<Rline>", "\r")
			.replaceAll("(?i)&0",ChatColor.BLACK+"")
			.replaceAll("(?i)&9",ChatColor.BLUE+"")
			.replaceAll("(?i)&l",ChatColor.BOLD+"")
			.replaceAll("(?i)&3",ChatColor.DARK_AQUA+"")
			.replaceAll("(?i)&1",ChatColor.DARK_BLUE+"")
			.replaceAll("(?i)&8",ChatColor.DARK_GRAY+"")
			.replaceAll("(?i)&2",ChatColor.DARK_GREEN+"")
			.replaceAll("(?i)&5",ChatColor.DARK_PURPLE+"")
			.replaceAll("(?i)&4",ChatColor.DARK_RED+"")
			.replaceAll("(?i)&6",ChatColor.GOLD+"")
			.replaceAll("(?i)&7",ChatColor.GRAY+"")
			.replaceAll("(?i)&a",ChatColor.GREEN+"")
			.replaceAll("(?i)&o",ChatColor.ITALIC+"")
			.replaceAll("(?i)&d",ChatColor.LIGHT_PURPLE+"")
			.replaceAll("(?i)&k",ChatColor.MAGIC+"")
			.replaceAll("(?i)&c",ChatColor.RED+"")
			.replaceAll("(?i)&m",ChatColor.STRIKETHROUGH+"")
			.replaceAll("(?i)&n",ChatColor.UNDERLINE+"")
			.replaceAll("(?i)&f",ChatColor.WHITE+"")
			.replaceAll("(?i)&e",ChatColor.YELLOW+"")
			.replaceAll("(?i)&r",ChatColor.RESET+"");
		return str;
	}
	/**Serializes the given inventory and converts and saves all data that may be associated with the items within the inventory.<p>
	 * <strong>Serialization tags(for easy readability):</strong><br>
	 * 0# = The slot number(where 0 is the first slot in the hotbar of a player's inventory)<br>
	 * t@ = Item Type(Notice this one is NOT prefixed with a colon)<br>
	 * :d@ = Item Durability<br>
	 * :a@ = Item Amount<br>
	 * :e@ = Item Enchantment Data<br>
	 * :l@ = Item Lore Data<br>
	 * :n@ = Item Name<br>
	 * :b@ = Book Data<br>
	 * You can add an additional '@' after a tag to specify additional data if it has the word 'Data' in it's name above.<p>
	 * <strong>An example of a player inventory, containing a Stone Pickaxe, with the custom Name "Hello, world!" and some enchantments and lore:</strong><br>
	 * <font face="consolas" size="2">36;container.inventory;0#t@274:d@60:e@34@3:e@35@1:e@32@2:n@&3Hello, world!:l@&4The item's Lore, Line One@&2The Item's Lore, Line Two@Etc, &3etc.;</font><br>
	 * The "36;" is the size of the inventory(in slots), and the "container.inventory;" is the title of the inventory(That is the default Minecraft title, anything else that you type here will be displayed instead of 'Inventory').<br>
	 * Deleting these two stored values will render the whole inventory un-readable, and upon being read from by {@link InventoryAPI#deserializeInventory(String)}, will be returned null.
	 * @param inv Inventory
	 * @return The given Inventory, serialized into a single-line String.<p>
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}<br>
	 * {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#deserializeBook(ItemStack, String[])}
	 */
	public static String serializeInventory(Inventory inv) {
		String serialization = inv.getSize() + item_separator + inv.getTitle() + item_separator;
		for(int i = 0; i < inv.getSize(); i++) {
			ItemStack is = inv.getItem(i);
			if(is != null) {
				String serializedItemStack = new String();
				String isType = String.valueOf(is.getType().getId());
				serializedItemStack += "t" + attribute_separator + isType;
				if(is.getDurability() != 0) {
					String isDurability = String.valueOf(is.getDurability());
					serializedItemStack += attribute_start + "d" + attribute_separator + isDurability;
				}
				if(is.getAmount() != 1) {
					String isAmount = String.valueOf(is.getAmount());
					serializedItemStack += attribute_start + "a" + attribute_separator + isAmount;
				}
				Map<Enchantment,Integer> isEnch = is.getEnchantments();
				if(isEnch.size() > 0) {
					for(Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
						serializedItemStack += attribute_start + "e" + attribute_separator + ench.getKey().getId() + attribute_separator + ench.getValue();
					}
				}
				if(is.getItemMeta().hasDisplayName()) {
					serializedItemStack += attribute_start + "n" + attribute_separator + convertSymbolsForSaving(is.getItemMeta().getDisplayName());
				}
				if(is.getItemMeta().hasLore()) {
					Iterator<String> it = is.getItemMeta().getLore().iterator();
					String lores = attribute_start + "l";
					if(it.hasNext() == false) {
						lores += attribute_separator;
					}
					while(it.hasNext()) {
						lores += attribute_separator + convertSymbolsForSaving(it.next());
					}
					serializedItemStack += lores;
				}
				if(is.getType() == Material.BOOK_AND_QUILL || is.getType() == Material.WRITTEN_BOOK) {
					serializedItemStack += serializeBook(is);
				}
				serialization += i + item_start + serializedItemStack + item_separator;
			}
		}
		return serialization;
	}
	/**This function is the same as {@link InventoryAPI#deserializeInventory(String, Player)}, except for the Player parameter, which is null.
	 * @param invString String
	 * @return The inventory with no holder, or null, if no string is given.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}<br>
	 * {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#deserializeBook(ItemStack, String[])}
	 */
	public static Inventory deserializeInventory(String invString) {
		return deserializeInventory(invString, null);
	}
	/**Attempts to return the Inventory of the Player, deserialized from the String parameter. The Inventory that is returned will be owned by the Player.
	 * @param invString String
	 * @param player Player
	 * @return The deserialized inventory with the given player parameter as the holder, or the players' inventory, if no string is given. If neither a string nor a player is given, then this function returns null.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#deserializeBook(ItemStack, String[])}
	 */
	@SuppressWarnings("boxing")
	public static Inventory deserializeInventory(String invString, Player player) {
		if(invString != null) {
			if(invString.equals("") == false) {
				String[] serializedBlocks = invString.split(item_separator);
				//String invInfo = serializedBlocks[0];
				//Inventory deserializedInventory = Bukkit.getServer().createInventory(player, invType);
				//Inventory deserializedInventory = Bukkit.getServer().createInventory(player, Integer.valueOf(invInfo));
				Inventory deserializedInventory = Bukkit.getServer().createInventory(player, Integer.valueOf(serializedBlocks[0]), String.valueOf(serializedBlocks[1]));
				//for(int i = 1; i < serializedBlocks.length; i++) {
				for(int i = 2; i < serializedBlocks.length; i++) {
					String[] serializedBlock = serializedBlocks[i].split(item_start);
					int stackPosition = Integer.valueOf(serializedBlock[0]);
					if(stackPosition >= deserializedInventory.getSize()) {
						continue;
					}
					ItemStack is = null;
					Boolean createdItemStack = false;
					String[] serializedItemStack = serializedBlock[1].split(attribute_start);
					for(String itemInfo : serializedItemStack) {
						String[] itemAttribute = itemInfo.split(attribute_separator);
						if(itemAttribute[0].equals("t")) {
							is = new ItemStack(Material.getMaterial(Integer.valueOf(itemAttribute[1])));
							createdItemStack = true;
						}
						if(createdItemStack) {
							if(itemAttribute[0].equals("d")) {
								is.setDurability(Short.valueOf(itemAttribute[1]));
							} else if(itemAttribute[0].equals("a")) {
								is.setAmount(Integer.valueOf(itemAttribute[1]));
							} else if(itemAttribute[0].equals("n")) {
								ItemMeta meta = null;
								if(is.getItemMeta() == null) {
									meta = Bukkit.getServer().getItemFactory().getItemMeta(is.getType());
								} else {
									meta = is.getItemMeta();
								}
								meta.setDisplayName(convertSymbolsForLoading(itemAttribute[1]));
								is.setItemMeta(meta);
							} else if(itemAttribute[0].equals("e")) {
								is.addUnsafeEnchantment(Enchantment.getById(Integer.valueOf(itemAttribute[1])), Integer.valueOf(itemAttribute[2]));
							} else if(itemAttribute[0].equals("l")) {
								ArrayList<String> lores = new ArrayList<String>();
								for(int j = 1; j < itemAttribute.length; j++) {
									lores.add(convertSymbolsForLoading(itemAttribute[j]));
								}
								ItemMeta meta = null;
								if(is.getItemMeta() == null) {
									meta = Bukkit.getServer().getItemFactory().getItemMeta(is.getType());
								} else {
									meta = is.getItemMeta();
								}
								meta.setLore(lores);
								is.setItemMeta(meta);
							} else if(itemAttribute[0].equals("b")) {
								is = deserializeBook(is, itemAttribute);
							}
						}
					}
					deserializedInventory.setItem(stackPosition, is);
				}
				return deserializedInventory;
			} else if(player != null) {
				return player.getInventory();
/*			}
		} else {
			return null;
		}
		return Bukkit.getServer().createInventory(null, InventoryType.PLAYER);
	}*/
			} else {
				return null;
			}
		}
		return null;
	}
	/**With this function allows you to easily choose what Inventory is to be used from the given Player in the function {@link InventoryAPI#serializeInventory(Inventory)}, and have it returned all in the same function.
	 * <br>With the parameter invToConvert, you can specify either "inventory", "armor", or "enderchest", and the function will return the serialized inventory of the type that you specified.
	 * @param player Player
	 * @param invToConvert String
	 * @return The serialized inventory as a single-line String. If the Player parameter is null, then a blank String is returned. If the String parameter is null, or isn't one of the options listed, the player's default inventory is returned, serialized as a single-line String. If both the Player and the String parameters are null, a blank String is returned.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}<br>
	 * {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#deserializeBook(ItemStack, String[])}
	 */
	public static String serializeInventory(Player player, String invToConvert) {
		if(player != null) {
			Inventory invInventory = Bukkit.getServer().createInventory(player, InventoryType.PLAYER);
			if(invToConvert != null) {
				if(invToConvert.equalsIgnoreCase("inventory")) {
					invInventory = player.getInventory();
				} else if(invToConvert.equalsIgnoreCase("armor")) {
					Inventory newInv = Bukkit.getServer().createInventory(player, 9);
					//newInv.setContents(player.getInventory().getArmorContents());
					int num = 0;
					for(ItemStack curItem : player.getInventory().getArmorContents()) {
						newInv.setItem(num, curItem);num++;
					}
					invInventory = newInv;
				} else if(invToConvert.equalsIgnoreCase("enderchest")) {
					invInventory = player.getEnderChest();
				} else {
					invInventory = player.getInventory();
				}
			} else {
				invInventory = player.getInventory();
			}
			return serializeInventory(invInventory);
		}
		return "";
	}
	/**Attempts to return the given Player's default Inventory, serialized as a single-line String. Uses {@link InventoryAPI#serializeInventory(Inventory)} for serialization.
	 * @param player Player
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @return The player's default Inventory, serialized as a single-line String. If the player is null, a blank string is returned.
	 * @see {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}<br>
	 * {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#deserializeBook(ItemStack, String[])}
	 */
	public static String serializeInventory(Player player) {
		return serializeInventory(player, null);
	}
	/**Attempts to serialize the given ItemStack and return it as a single-line String. If the ItemStack provided is not a Book and Quill or a Written Book, this function will return a blank string.
	 * @param item ItemStack
	 * @return The given ItemStack in a serialized single-line String if it is a Book and Quill or a Written Book. Otherwise, a blank string is returned.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#deserializeBook(ItemStack, String[])}<br>
	 * {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}
	 */
	public static String serializeBook(ItemStack item) {
		String rtrn = ":b@";
		BookMeta meta;
		if(item.getType() == Material.BOOK_AND_QUILL || item.getType() == Material.WRITTEN_BOOK) {
			meta = (BookMeta) item.getItemMeta();
			String author = (meta.hasAuthor() ? meta.getAuthor() : null);
			String title = (meta.hasTitle() ? meta.getTitle() : null);
			if(author != null) {
				rtrn += convertSymbolsForSaving(author);
			} else {
				rtrn += "<NO-AUTHOR>";
			}
			if(title != null) {
				rtrn += "@" + convertSymbolsForSaving(meta.getTitle());
			} else {
				rtrn += "@<NO-TITLE>";
			}
			if(meta.hasPages()) {
				List<String> pages = meta.getPages();
				for(String curPage : pages) {
					rtrn += "@" + convertSymbolsForSaving(curPage);//The start of the pages will be itemAttribute[3] and up.
				}
			}
		}
		return (rtrn.equals(":b@") ? "" : rtrn);
	}
	/**Attempts to deserialize the BookMeta data from the String[] parameter and add it to the given ItemStack, which is then returned. If no ItemStack is given, a Written Book is generated in its place, and the BookMeta assigned. If no String[] parameter is given, then this function will return the given ItemStack, or a blank Book and Quill if no ItemStack was given either. 
	 * @param item ItemStack
	 * @param itemAttribute String[]
	 * @return The deserialized Book, with all of its previous BookMeta intact. If the String[] parameter is null, then this will return the given ItemStack.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeBook(ItemStack)}<br>
	 * {@link InventoryAPI#serializeInventory(Inventory)}<br>
	 * {@link InventoryAPI#serializeInventory(Player, String)}<br>
	 * {@link InventoryAPI#serializeInventory(Player)}<br>
	 * {@link InventoryAPI#deserializeInventory(String)}<br>
	 * {@link InventoryAPI#deserializeInventory(String, Player)}
	 */
	public static ItemStack deserializeBook(ItemStack item, String[] itemAttribute) {
		if(item == null && itemAttribute == null) {return new ItemStack(Material.BOOK_AND_QUILL);}
		if(item == null) {item = new ItemStack(Material.WRITTEN_BOOK);}
		if(itemAttribute == null) {return item;}
		if(item.getType() == Material.BOOK_AND_QUILL || item.getType() == Material.WRITTEN_BOOK) {
			BookMeta meta = (BookMeta) (item.hasItemMeta() ? item.getItemMeta() : Bukkit.getServer().getItemFactory().getItemMeta(item.getType()));
			String author = itemAttribute[1];
			String title = itemAttribute[2];
			if(author != null) {
				meta.setAuthor((author.equals("<NO-AUTHOR>") == false ? convertSymbolsForLoading(author) : null));
			} else {
				item.setItemMeta(meta);
				return item;
			}
			if(title != null) {
				meta.setTitle((title.equals("<NO-TITLE>") == false ? convertSymbolsForLoading(title) : null));
			} else {
				item.setItemMeta(meta);
				return item;
			}
			for(int i = 3; i < itemAttribute.length;) {
				if(itemAttribute[i] != null) {
					meta.addPage(convertSymbolsForLoading(itemAttribute[i]));
				}
				i++;
			}
			item.setItemMeta(meta);
		}
		return item;
	}
	/**Example usage: inv = setTitle("Hello, world!", inv);
	 * @param str String
	 * @param inv Inventory
	 * @return The inv parameter with the new title.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 */
	public static Inventory setTitle(String str, Inventory inv) {
		//MainInvClass.sendConsoleMessage("&eDebug: The inventory's size is: \"&f" + inv.getSize() + "&e\"!");
		Inventory newInv = Bukkit.getServer().createInventory(inv.getHolder(), inv.getSize(), str);
		newInv.setContents(inv.getContents());
		newInv.setMaxStackSize(inv.getMaxStackSize());//In case some of you out there need this to do this.
		return newInv;
	}
	/**Attempts to return the given experience and level serialized, as a single-line String.
	 * @param level Integer
	 * @param exp float
	 * @return The serialized result, or a blank string if the given parameters somehow had no value.
	 * @see {@link InventoryAPI#serializeExperience(Player)}<br>
	 * {@link InventoryAPI#deserializeExperience(String)}<br>
	 * {@link InventoryAPI#deserializeExp(String)}<br>
	 * {@link InventoryAPI#deserializeLevel(String)}
	 */
	public static String serializeExperience(int level, float exp) {
		String rtrn = "x@";
		rtrn += level + "@" + exp;
		return (rtrn.equals("x@") ? "" : (rtrn.equals("x@@") ? "" : rtrn));
	}
	/**Attempts to serialize the level and experience of the given Player as a single-line String.<br>
	 * This function uses {@link InventoryAPI#serializeExperience(int, float)} for deserialization.
	 * @param player Player
	 * @return The serialized form of the given Player's experience and level.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#serializeExperience(int, float)}<br>
	 * {@link InventoryAPI#deserializeExperience(String)}<br>
	 * {@link InventoryAPI#deserializeExp(String)}<br>
	 * {@link InventoryAPI#deserializeLevel(String)}
	 */
	public static String serializeExperience(Player player) {
		return serializeExperience(player.getLevel(), player.getExp());
	}
	/**Attempts to return a String[] array containing the Integer level and the float experience, deserialized from the String parameter.
	 * @param serializedExp String
	 * @return A String[] array containing the Integer level and the float experience, deserialized from the String parameter. May return values that are not an Integer or a float in the String[] array, so it is recommended to either check the result of this function or use one(or both) of the following functions instead:<br>{@link InventoryAPI#deserializeExp(String)}<br>{@link InventoryAPI#deserializeLevel(String)}
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#deserializeExp(String)}<br>
	 * {@link InventoryAPI#deserializeLevel(String)}<br>
	 * {@link InventoryAPI#serializeExperience(int, float)}<br>
	 * {@link InventoryAPI#serializeExperience(Player)}
	 */
	public static String[] deserializeExperience(String serializedExp) {
		return serializedExp.replace("x@", "").split("@");
	}
	/**Attempts to return the int value of the deserialized level.<br>
	 * This function uses {@link InventoryAPI#deserializeExperience(String)} for deserialization.
	 * @param serializedExp String
	 * @return The int value of the deserialized level, or 0 if the deserialization did not return a valid Integer.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#deserializeExperience(String)}
	 * {@link InventoryAPI#deserializeExp(String)}
	 * {@link InventoryAPI#serializeExperience(int, float)}
	 * {@link InventoryAPI#serializeExperience(Player)}
	 */
	public static int deserializeLevel(String serializedExp) {
		int rtrn = 0;
		int num = 0;
		for(String curNum : deserializeExperience(serializedExp)) {
			num++;
			boolean isInt = true;
			if(num == 1) {
				try{Integer.parseInt(curNum);
				} catch (NumberFormatException e) {
					isInt = false;
				}
				if(isInt) {
					rtrn = Integer.parseInt(curNum);
				}
			}
		}
		return rtrn;
	}
	/**Attempts to return the float value of the deserialized experience.<br>
	 * This function uses {@link InventoryAPI#deserializeExperience(String)} for deserialization.
	 * @param serializedExp String
	 * @return The float value of the deserialized experience, or 0 if the deserialization did not return a valid float.
	 * @author <a href="http://enteisislandsurvival.no-ip.org/about/author.html">Brian_Entei</a>
	 * @see {@link InventoryAPI#deserializeExperience(String)}<br>
	 * {@link InventoryAPI#deserializeLevel(String)}<br>
	 * {@link InventoryAPI#serializeExperience(int, float)}<br>
	 * {@link InventoryAPI#serializeExperience(Player)}
	 */
	public static float deserializeExp(String serializedExp) {
		float rtrn = 0;
		int num = 0;
		for(String curNum : deserializeExperience(serializedExp)) {
			num++;
			boolean isFloat = true;
			if(num == 2) {
				try{Float.parseFloat(curNum);
				} catch (NumberFormatException e) {
					isFloat = false;
				}
				if(isFloat) {
					rtrn = Float.parseFloat(curNum);
				}
			}
		}
		return rtrn;
	}
}