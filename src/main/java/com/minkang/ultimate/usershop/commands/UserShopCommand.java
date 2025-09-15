package com.minkang.ultimate.usershop.commands;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class UserShopCommand implements CommandExecutor {

    private final Main plugin;
    private final ShopManager shop;
    private final GUIManager gui;

    public UserShopCommand(Main plugin, ShopManager shop, GUIManager gui) {
        this.plugin = plugin;
        this.shop = shop;
        this.gui = gui;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            sendHelp(p);
            gui.openMain(p, 1);
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("열기")) {
            gui.openOwnerShop(p, p.getUniqueId(), 1);
            return true;
        }
        if (sub.equalsIgnoreCase("검색")) {
            gui.requestSearch(p);
            return true;
        }
        if (sub.equalsIgnoreCase("등록") && args.length >= 4) {
            try {
                double price = Double.parseDouble(args[1]);
                int qty = Integer.parseInt(args[2]);
                int slot = Integer.parseInt(args[3]);
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) { p.sendMessage(color("&c손에 아이템을 들어주세요.")); return true; }
                if (qty <= 0) qty = hand.getAmount();
                if (hand.getAmount() < qty) { p.sendMessage(color("&c수량이 부족합니다.")); return true; }
                ItemStack sell = hand.clone(); sell.setAmount(qty);
                hand.setAmount(hand.getAmount() - qty); p.getInventory().setItemInMainHand(hand.getAmount() > 0 ? hand : null);
                if (!shop.setListing(p.getUniqueId(), slot, sell, price)) {
                    p.sendMessage(color("&c등록 실패."));
                    return true;
                }
                String msg = plugin.getConfig().getString("messages.broadcasts.item_registered", "&e[상점] &f{seller}&7 님이 &a{item}&7 {qty}개를 &b{price}&7에 등록했습니다. 남은재고: &f{stock}")
                        .replace("{seller}", p.getName())
                        .replace("{item}", sell.getType().name())
                        .replace("{qty}", String.valueOf(sell.getAmount()))
                        .replace("{price}", String.format("%.2f", price))
                        .replace("{stock}", String.valueOf(sell.getAmount()));
                com.minkang.ultimate.usershop.realtime.ShopRealtime.get().broadcast(ChatColor.translateAlternateColorCodes('&', msg));
                p.sendMessage(color("&a등록 완료."));
                return true;
            } catch (Exception ex) {
                p.sendMessage(color("&c사용법: /유저상점 등록 <가격> <수량> <슬롯>"));
                return true;
            }
        }
        if (sub.equalsIgnoreCase("등록취소") && args.length >= 2) {
            try {
                int slot = Integer.parseInt(args[1]);
                boolean ok = shop.cancelListing(p.getUniqueId(), slot);
                p.sendMessage(ok ? color("&a등록 취소 완료.") : color("&c해당 슬롯에 등록된 물품이 없습니다."));
                return true;
            } catch (Exception ex) {
                p.sendMessage(color("&c사용법: /유저상점 등록취소 <슬롯>"));
                return true;
            }
        }
        if (sub.equalsIgnoreCase("확장")) {
            shop.addSlots(p.getUniqueId(), 9);
            p.sendMessage(color("&a상점 슬롯이 9칸 확장되었습니다."));
            return true;
        }
        sendHelp(p);
        return true;
    }

    private void sendHelp(Player p) {
        boolean isAdmin = p.hasPermission("usershop.admin");
        p.sendMessage(color("&a[유저상점 도움말]"));
        p.sendMessage(color("&e/유저상점 열기 &7- 내 상점 열기"));
        p.sendMessage(color("&e/유저상점 검색 &7- 검색"));
        p.sendMessage(color("&e/유저상점 등록 &f<가격> <갯수> <슬롯> &7- 손 아이템 등록"));
        p.sendMessage(color("&e/유저상점 등록취소 &f<슬롯> &7- 등록 취소"));
        p.sendMessage(color("&e/유저상점 확장 &7- 슬롯 +9"));
        if (isAdmin) {
            p.sendMessage(color("&8-------------------------"));
            p.sendMessage(color("&c[관리자] (추가 관리자 명령은 추후 확장)"));
        }
    }
}
