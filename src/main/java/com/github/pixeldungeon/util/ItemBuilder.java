package com.github.pixeldungeon.util;
import org.bukkit.*; import org.bukkit.inventory.*; import org.bukkit.inventory.meta.ItemMeta; import org.bukkit.persistence.PersistentDataType; import java.util.*;
public class ItemBuilder{
  private final ItemStack item; private final ItemMeta meta; private final List<String> lore=new ArrayList<>();
  public ItemBuilder(Material m){ this.item=new ItemStack(m); this.meta=item.getItemMeta(); }
  public ItemBuilder name(String s){ meta.setDisplayName(Text.color(s)); return this; }
  public ItemBuilder lore(String... lines){ for(String l:lines) lore.add(Text.color(l)); return this; }
  public ItemBuilder pdc(NamespacedKey key, String val){ meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, val); return this; }
  public ItemStack build(){ meta.setLore(lore); item.setItemMeta(meta); return item; }
}