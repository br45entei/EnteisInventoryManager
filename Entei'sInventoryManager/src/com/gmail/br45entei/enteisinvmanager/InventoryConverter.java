package com.gmail.br45entei.enteisinvmanager;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryConverter {
	public static String serializeInventory(Inventory inv) {
		String serialization = inv.getSize() + ";" + inv.getTitle() + ";";
		for(int i = 0; i < inv.getSize(); i++) {
			ItemStack is = inv.getItem(i);
			if(is != null) {
				String serializedItemStack = new String();
				String isType = String.valueOf(is.getType().getId());
				serializedItemStack += "t@" + isType;
				if(is.getDurability() != 0) {
					String isDurability = String.valueOf(is.getDurability());
					serializedItemStack += ":d@" + isDurability;
				}
				if(is.getAmount() != 1) {
					String isAmount = String.valueOf(is.getAmount());
					serializedItemStack += ":a@" + isAmount;
				}
				Map<Enchantment,Integer> isEnch = is.getEnchantments();
				if(isEnch.size() > 0) {
					for(Entry<Enchantment,Integer> ench : isEnch.entrySet()) {
						serializedItemStack += ":e@" + ench.getKey().getId() + "@" + ench.getValue();
					}
				}
				serialization += i + "#" + serializedItemStack + ";";
			}
		}
		return serialization;
	}
	public static String InventoryToString(Player player, String invToConvert) {
		Inventory invInventory = Bukkit.getServer().createInventory(player, InventoryType.PLAYER);
		if(invToConvert.equalsIgnoreCase("inventory")) {
			invInventory = player.getInventory();
		} else if(invToConvert.equalsIgnoreCase("armor")) {
			Inventory newInv = Bukkit.getServer().createInventory(player, 9);
			int num = 0;
			for(ItemStack curItem : player.getInventory().getArmorContents()) {
				newInv.setItem(num, curItem);num++;
			}
			invInventory = newInv;
		} else if(invToConvert.equalsIgnoreCase("enderchest")) {
			invInventory = player.getEnderChest();
		}
		return serializeInventory(invInventory);
	}
	@SuppressWarnings("boxing")
	public static Inventory StringToInventory(String invString, Player player) {
		String[] serializedBlocks = invString.split(";");
		//String invInfo = serializedBlocks[0];
		//Inventory deserializedInventory = Bukkit.getServer().createInventory(player, invType);
		//Inventory deserializedInventory = Bukkit.getServer().createInventory(player, Integer.valueOf(invInfo));
		Inventory deserializedInventory = Bukkit.getServer().createInventory(player, Integer.valueOf(serializedBlocks[0]), String.valueOf(serializedBlocks[1]));
		//for(int i = 1; i < serializedBlocks.length; i++) {
		for(int i = 2; i < serializedBlocks.length; i++) {
			String[] serializedBlock = serializedBlocks[i].split("#");
			int stackPosition = Integer.valueOf(serializedBlock[0]);
			if(stackPosition >= deserializedInventory.getSize()) {
				continue;
			}
			ItemStack is = null;
			Boolean createdItemStack = false;
			String[] serializedItemStack = serializedBlock[1].split(":");
			for(String itemInfo : serializedItemStack) {
				String[] itemAttribute = itemInfo.split("@");
				if(itemAttribute[0].equals("t")) {
					is = new ItemStack(Material.getMaterial(Integer.valueOf(itemAttribute[1])));
					createdItemStack = true;
				} else if(itemAttribute[0].equals("d") && createdItemStack) {
					is.setDurability(Short.valueOf(itemAttribute[1]));
				} else if(itemAttribute[0].equals("a") && createdItemStack) {
					is.setAmount(Integer.valueOf(itemAttribute[1]));
				} else if(itemAttribute[0].equals("e") && createdItemStack) {
					is.addUnsafeEnchantment(Enchantment.getById(Integer.valueOf(itemAttribute[1])), Integer.valueOf(itemAttribute[2]));
				}
			}
			deserializedInventory.setItem(stackPosition, is);
		}
		return deserializedInventory;
	}
	public static Inventory StringToInventory(String invString) {return StringToInventory(invString, null);}
	
}