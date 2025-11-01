package com.github.pixeldungeon.manager;
import com.github.pixeldungeon.PixelDungeonPlugin; import org.bukkit.*; import org.bukkit.entity.Player; import org.bukkit.event.*; import org.bukkit.event.player.PlayerInteractEvent; import org.bukkit.inventory.*; import org.bukkit.inventory.meta.ItemMeta; import org.bukkit.persistence.PersistentDataType;
public class PartyTicketListener implements Listener{
  @EventHandler public void onUse(PlayerInteractEvent e){ if(e.getHand()!=EquipmentSlot.HAND) return; ItemStack it=e.getItem(); if(it==null||it.getType()==Material.AIR) return; ItemMeta meta=it.getItemMeta(); if(meta==null) return;
    if(meta.getPersistentDataContainer().has(PixelDungeonPlugin.get().ticketKey(), PersistentDataType.STRING)){ Player p=e.getPlayer(); int sec=PixelDungeonPlugin.get().getConfig().getInt("partyTicketDurationSeconds",600);
      PixelDungeonPlugin.get().partyManager().addPermit(p.getUniqueId(), sec*1000L); it.setAmount(it.getAmount()-1); String tpl = com.github.pixeldungeon.PixelDungeonPlugin.get().getConfig().getString("messages.party.ticket_activated", "&d파티 초대권&f이 {seconds}초 동안 활성화되었습니다.");
            p.sendMessage(com.github.pixeldungeon.util.Text.color(tpl.replace("{seconds}", String.valueOf(sec)))); e.setCancelled(true); } }
}