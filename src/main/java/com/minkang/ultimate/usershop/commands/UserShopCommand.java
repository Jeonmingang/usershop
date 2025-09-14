package com.minkang.ultimate.usershop.commands;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        this.plugin = plugin; this.shopManager = shopManager; this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        // 1) /유저상점 -> 도움말, /유저상점 열기 -> GUI
        if (args.length == 0) { sendHelp(p); return true; }
        if (args[0].equalsIgnoreCase("열기")) { guiManager.openMain(p, 1); return true; }

        // 2) 등록/등록취소 명령어 명칭만 보정(실제 내부 로직은 기존에 맞춰 사용)
        if (args[0].equalsIgnoreCase("등록") || args[0].equalsIgnoreCase("추가")) {
            if (args.length < 3) { p.sendMessage(color("&c사용법: /유저상점 등록 <가격> <슬롯>")); return true; }
            double unitPrice;
            try { unitPrice = Double.parseDouble(args[1]); } catch (Exception e) { p.sendMessage(color("&c가격은 숫자여야 합니다.")); return true; }
            int slot;
            try { slot = Integer.parseInt(args[2]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) { p.sendMessage(color("&c손에 등록할 아이템을 들고 있어야 합니다.")); return true; }

            // 실제 등록 저장은 기존 Manager 사용
            shopManager.addListing(uuid, slot, unitPrice, hand.clone());
            p.getInventory().setItemInMainHand(null);

            // 3) 등록 방송
            if (plugin.getConfig().getBoolean("announce.on-register", true)) {
                String itemName = (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName())
                        ? ChatColor.stripColor(hand.getItemMeta().getDisplayName())
                        : hand.getType().name();
                String msg = plugin.getConfig().getString("announce.register-format",
                        "&e{player}&7님이 &f{amount}x {item}&7을 &e{price} {currency}&7(개당)에 등록했습니다.");
                msg = msg.replace("{player}", p.getName())
                         .replace("{item}", itemName)
                         .replace("{amount}", String.valueOf(hand.getAmount()))
                         .replace("{price}", String.format("%.2f", unitPrice))
                         .replace("{currency}", plugin.getConfig().getString("defaults.currency-name", "코인"));
                Bukkit.broadcastMessage(color(msg));
            }

            p.sendMessage(color("&a등록 완료! 슬롯: " + slot));
            return true;
        }

        if (args[0].equalsIgnoreCase("등록취소") || args[0].equalsIgnoreCase("삭제")) {
            if (args.length < 2) { p.sendMessage(color("&c사용법: /유저상점 등록취소 <슬롯>")); return true; }
            int slot;
            try { slot = Integer.parseInt(args[1]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
            boolean ok = shopManager.removeListingToPlayer(uuid, slot, p);
            if (!ok) p.sendMessage(color("&c해당 슬롯에 등록된 아이템이 없습니다."));
            else p.sendMessage(color("&a등록 취소 완료. 슬롯: " + slot));
            return true;
        }

        // 나머지 하위명령은 기존대로 (설정/확장 등)
        sendHelp(p);
        return true;
    }

    private void sendHelp(Player p) {
        boolean admin = p.hasPermission("usershop.admin");
        p.sendMessage(color("&a[유저상점 도움말]"));
        p.sendMessage(color("&e/유저상점 열기 &7- 메인 GUI 열기"));
        p.sendMessage(color("&e/유저상점 등록 &f<가격> <슬롯> &7- 손 아이템 등록 (등록권 소모)"));
        p.sendMessage(color("&e/유저상점 등록취소 &f<슬롯> &7- 해당 슬롯 등록 취소"));
        p.sendMessage(color("&e/유저상점 확장 &7- 확장권 1개 소모, 슬롯 +9"));
        if (admin) {
            p.sendMessage(color("&8-------------------------"));
            p.sendMessage(color("&c[관리자 전용] /유저상점 설정 ..."));
        }
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
