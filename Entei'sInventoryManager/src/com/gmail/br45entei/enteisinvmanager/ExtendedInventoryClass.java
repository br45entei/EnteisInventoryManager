package com.gmail.br45entei.enteisinvmanager;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
/**@author Brian_Entei */
public class ExtendedInventoryClass {
	/**Example usage: inv = setTitle("Hello, world!", inv);
	 * @param str String
	 * @param inv Inventory
	 * @return The inv parameter with the new title.
	 */
	public static Inventory setTitle(String str, Inventory inv) {
		Inventory newInv = Bukkit.getServer().createInventory(inv.getHolder(), inv.getSize(), str);
		newInv.setContents(inv.getContents());
		return newInv;
	}
}