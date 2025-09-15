package com.minkang.ultimate.usershop.commands;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.util.ItemUtil;
import com.minkang.ultimate.usershop.util.NameMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class UserShopCommand implements CommandExecutor {

    private final Main plugin;
    private final ShopManager shopManager;
    private final GUIManager guiManager;

    public UserShopCommand(Main plugin, ShopManager shopManager, GUIManager guiManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        if (args.length == 0) { sendHelp(p); return true; }
        if (args[0].equalsIgnoreCase("열기")) { guiManager.openMain(p, 1); return true; }

        String sub = args[0];

        if (sub.equalsIgnoreCase("등록") || sub.equalsIgnoreCase("추가")) {
            if (args.length < 4) { p.sendMessage(color("&c사용법: /유저상점 등록 <가격> <갯수> <슬롯>")); return true; }
            double unitPrice; try { unitPrice = Double.parseDouble(args[1]); } catch (Exception e) { p.sendMessage(color("&c가격은 숫자여야 합니다.")); return true; }
            int amount; try { amount = Integer.parseInt(args[2]); } catch (Exception e) { p.sendMessage(color("&c갯수는 숫자여야 합니다.")); return true; }
            int slot; try { slot = Integer.parseInt(args[3]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
            if (amount < 1) { p.sendMessage(color("&c갯수는 1 이상이어야 합니다.")); return true; }

            int capacity = shopManager.getCapacity(uuid);
            if (slot < 0 || slot >= capacity) { p.sendMessage(color("&c해당 슬롯은 사용할 수 없습니다. (0~" + (capacity - 1) + ")")); return true; }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(color("&c손에 등록할 아이템을 들고 있어야 합니다.")); return true; }
            if (hand.getAmount() < amount) { p.sendMessage(color("&c손에 든 아이템 수량이 부족합니다. 현재: " + hand.getAmount())); return true; }

            boolean hasTicket = ItemUtil.consumeMatchingOne(p.getInventory(), plugin.getConfig().getConfigurationSection("tickets.register"));
            if (!hasTicket) { p.sendMessage(color("&c등록권이 필요합니다. (이름/로어 완전 일치)")); return true; }

            ItemStack copy = hand.clone();
            copy.setAmount(amount);
            int remain = hand.getAmount() - amount;
            if (remain <= 0) p.getInventory().setItemInMainHand(null);
            else { hand.setAmount(remain); p.getInventory().setItemInMainHand(hand); }

            shopManager.addListing(uuid, slot, unitPrice, copy);

            if (plugin.getConfig().getBoolean("announce.on-register", true)) {
                String itemName = (copy.hasItemMeta() && copy.getItemMeta().hasDisplayName())
                        ? ChatColor.stripColor(copy.getItemMeta().getDisplayName())
                        : copy.getType().name();
                String kor = plugin.getConfig().getString("search.korean-materials." + copy.getType().name(), "");
                if (kor != null && !kor.isEmpty()) itemName = kor;
                String msg = plugin.getConfig().getString(
                        "announce.register-format",
                        "&e{player}&7님이 &f{amount}x {item}&7을 &e{price} {currency}&7(개당)에 등록했습니다.");
                msg = msg.replace("{player}", p.getName())
                         .replace("{item}", itemName)
                         .replace("{amount}", String.valueOf(copy.getAmount()))
                         .replace("{price}", String.format("%.2f", unitPrice))
                         .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"));
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }

            p.sendMessage(color("&a슬롯 " + slot + "에 " + amount + "개 등록 완료. 단가: " + unitPrice + " " + plugin.getConfig().getString("defaults.currency-name", "코인")));
            return true;
        }

        if (sub.equalsIgnoreCase("등록취소") || sub.equalsIgnoreCase("삭제")) {
            if (args.length < 2) { p.sendMessage(color("&c사용법: /유저상점 등록취소 <슬롯>")); return true; }
            int slot; try { slot = Integer.parseInt(args[1]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
            boolean ok = shopManager.removeListingToPlayer(uuid, slot, p);
            if (!ok) p.sendMessage(color("&c해당 슬롯에 등록된 아이템이 없습니다."));
            else p.sendMessage(color("&a슬롯 " + slot + " 등록이 제거되었습니다."));
            return true;
        }

        if (sub.equalsIgnoreCase("확장")) {
            boolean inHand = ItemUtil.consumeOneFromHandIfMatches(p, plugin.getConfig().getConfigurationSection("tickets.expand"));
            if (!inHand) { p.sendMessage(color("&c손에 확장권을 들고 있어야 합니다.")); return true; }
            int newCap = shopManager.addCapacity(uuid, 9);
            p.sendMessage(color("&a상점 슬롯이 확장되었습니다. 현재: " + newCap));
            return true;
        }

        if (sub.equalsIgnoreCase("설정")) {
    if (!p.hasPermission("usershop.admin")) { p.sendMessage(color("&c권한이 없습니다.")); return true; }
    if (args.length == 1) { sendAdminHelp(p); return true; }
    String asub = args[1];

    if (asub.equalsIgnoreCase("오픈")) {
        boolean ok = com.minkang.ultimate.usershop.util.ItemUtil.applyTemplateFromItem(plugin, "opener-item", p.getInventory().getItemInMainHand());
        p.sendMessage(ok ? color("&a오픈 아이템 템플릿이 갱신되었습니다.") : color("&c손에 든 아이템이 없습니다."));
        return true;
    }
    if (asub.equalsIgnoreCase("등록권")) {
        boolean ok = com.minkang.ultimate.usershop.util.ItemUtil.applyTemplateFromItem(plugin, "tickets.register", p.getInventory().getItemInMainHand());
        p.sendMessage(ok ? color("&a등록권 템플릿이 갱신되었습니다.") : color("&c손에 든 아이템이 없습니다."));
        return true;
    }
    if (asub.equalsIgnoreCase("확장권")) {
        boolean ok = com.minkang.ultimate.usershop.util.ItemUtil.applyTemplateFromItem(plugin, "tickets.expand", p.getInventory().getItemInMainHand());
        p.sendMessage(ok ? color("&a확장권 템플릿이 갱신되었습니다.") : color("&c손에 든 아이템이 없습니다."));
        return true;
    }
    if (asub.equalsIgnoreCase("제거")) {
        if (args.length == 3) {
            int sslot;
            try { sslot = Integer.parseInt(args[2]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
            java.util.UUID target = p.getUniqueId();
            com.minkang.ultimate.usershop.gui.GUIManager.ViewContext vc = guiManager.getViewContext(p);
            if (vc != null && vc.mode == com.minkang.ultimate.usershop.gui.GUIManager.Mode.SHOP && vc.owner != null) target = vc.owner;
            boolean ok = shopManager.adminRemoveListing(target, sslot);
            p.sendMessage(ok ? color("&a강제 제거 완료 (대상: " + target + ", 슬롯: " + sslot + ")") : color("&c지정 슬롯에 등록이 없습니다."));
            return true;
        }
        if (args.length >= 4) {
            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
            if (op == null || op.getUniqueId() == null) { p.sendMessage(color("&c플레이어를 찾을 수 없습니다.")); return true; }
            int sslot; try { sslot = Integer.parseInt(args[3]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
            boolean ok = shopManager.adminRemoveListing(op.getUniqueId(), sslot);
            p.sendMessage(ok ? color("&a강제 제거 완료 (대상: " + (op.getName()!=null?op.getName():op.getUniqueId()) + ", 슬롯: " + sslot + ")") : color("&c지정 슬롯에 등록이 없습니다."));
            return true;
        }
        p.sendMessage(color("&c사용법: /유저상점 설정 제거 <슬롯> 또는 /유저상점 설정 제거 <플레이어> <슬롯>"));
        return true;
    }
    if (asub.equalsIgnoreCase("확장삭제")) {
        if (args.length < 4) { p.sendMessage(color("&c사용법: /유저상점 설정 확장삭제 <플레이어> <횟수(1~6)>")); return true; }
        org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
        if (op == null || op.getUniqueId() == null) { p.sendMessage(color("&c플레이어를 찾을 수 없습니다.")); return true; }
        int times; try { times = Integer.parseInt(args[3]); } catch (Exception e) { p.sendMessage(color("&c횟수는 숫자여야 합니다.")); return true; }
        if (times < 1) times = 1; if (times > 6) times = 6;
        int newCap = shopManager.reduceCapacity(op.getUniqueId(), times * 9);
        p.sendMessage(color("&a" + (op.getName()!=null?op.getName():op.getUniqueId()) + "님의 슬롯이 " + (times*9) + "만큼 감소. 현재 슬롯: " + newCap));
        return true;
    }
    sendAdminHelp(p);
    return true;
}
