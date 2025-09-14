package com.minkang.ultimate.usershop.commands;

import com.minkang.ultimate.usershop.Main;
import com.minkang.ultimate.usershop.data.ShopManager;
import com.minkang.ultimate.usershop.gui.GUIManager;
import com.minkang.ultimate.usershop.util.ItemUtil;
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
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;
        UUID uuid = p.getUniqueId();

        if (args.length == 0) {
            guiManager.openMain(p, 1);
            return true;
        }

        String sub = args[0];

        if (sub.equalsIgnoreCase("추가")) {
            if (args.length < 3) {
                p.sendMessage(color("&c사용법: /유저상점 추가 <가격> <슬롯>"));
                return true;
            }
            double unitPrice;
            try { unitPrice = Double.parseDouble(args[1]); } catch (Exception e) {
                p.sendMessage(color("&c가격은 숫자여야 합니다.")); return true; }
            int slot;
            try { slot = Integer.parseInt(args[2]); } catch (Exception e) {
                p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }

            int capacity = shopManager.getCapacity(uuid);
            if (slot < 0 || slot >= capacity) {
                p.sendMessage(color("&c해당 슬롯은 사용할 수 없습니다. (0~" + (capacity - 1) + ")"));
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                p.sendMessage(color("&c손에 등록할 아이템을 들고 있어야 합니다."));
                return true;
            }

            boolean hasTicket = ItemUtil.consumeMatchingOne(p.getInventory(), plugin.getConfig().getConfigurationSection("tickets.register"));
            if (!hasTicket) {
                p.sendMessage(color("&c등록권이 필요합니다. (이름/로어 완전 일치)"));
                return true;
            }

            ItemStack copy = hand.clone();
            p.getInventory().setItemInMainHand(null);
            shopManager.addListing(uuid, slot, unitPrice, copy);
            p.sendMessage(color("&a슬롯 " + slot + "에 등록 완료. 단가: " + unitPrice + " " + plugin.getConfig().getString("defaults.currency-name", "코인")));
            return true;
        }

        if (sub.equalsIgnoreCase("삭제")) {
            if (args.length < 2) {
                p.sendMessage(color("&c사용법: /유저상점 삭제 <슬롯>"));
                return true;
            }
            int slot;
            try { slot = Integer.parseInt(args[1]); } catch (Exception e) {
                p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
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
            if (args.length < 2) { p.sendMessage(color("&c사용법: /유저상점 설정 <오픈|등록권|확장권|확장삭제|제거> ...")); return true; }
            String action = args[1];
            if (action.equalsIgnoreCase("오픈")) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!ItemUtil.applyTemplateFromItem(plugin, "opener-item", hand)) p.sendMessage(color("&c손에 든 아이템이 비어있습니다."));
                else { plugin.saveConfig(); p.sendMessage(color("&a오픈 아이템이 설정되었습니다. (이름/로어 완전 일치)")); }
                return true;
            } else if (action.equalsIgnoreCase("등록권")) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!ItemUtil.applyTemplateFromItem(plugin, "tickets.register", hand)) p.sendMessage(color("&c손에 든 아이템이 비어있습니다."));
                else { plugin.saveConfig(); p.sendMessage(color("&a등록권이 설정되었습니다.")); }
                return true;
            } else if (action.equalsIgnoreCase("확장권")) {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (!ItemUtil.applyTemplateFromItem(plugin, "tickets.expand", hand)) p.sendMessage(color("&c손에 든 아이템이 비어있습니다."));
                else { plugin.saveConfig(); p.sendMessage(color("&a확장권이 설정되었습니다.")); }
                return true;
            } else if (action.equalsIgnoreCase("확장삭제")) {
                if (args.length < 4) { p.sendMessage(color("&c사용법: /유저상점 설정 확장삭제 <플레이어> <횟수(1~6)>")); return true; }
                String targetName = args[2];
                int times; try { times = Integer.parseInt(args[3]); } catch (Exception e) { p.sendMessage(color("&c횟수는 숫자여야 합니다.")); return true; }
                int maxSteps = plugin.getConfig().getInt("defaults.max-expansions", 5) + 1;
                if (times < 1 || times > maxSteps) { p.sendMessage(color("&c횟수는 1~" + maxSteps + " 사이여야 합니다.")); return true; }
                OfflinePlayer target = p.getServer().getOfflinePlayer(targetName);
                if (target == null || target.getName() == null) { p.sendMessage(color("&c플레이어를 찾을 수 없습니다.")); return true; }
                int newCap = shopManager.reduceCapacity(target.getUniqueId(), times * 9);
                p.sendMessage(color("&a" + target.getName() + "의 상점 슬롯을 " + (times*9) + "만큼 감소. 현재: " + newCap));
                return true;
            } else if (action.equalsIgnoreCase("제거")) {
                if (args.length == 3) {
                    int slot; try { slot = Integer.parseInt(args[2]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
                    boolean ok = shopManager.adminRemoveListingToPlayer(p.getUniqueId(), slot, p);
                    if (!ok) p.sendMessage(color("&c해당 슬롯에 등록된 아이템이 없습니다."));
                    else p.sendMessage(color("&a본인 상점에서 슬롯 " + slot + " 제거 완료."));
                    return true;
                } else if (args.length == 4) {
                    String targetName = args[2];
                    Player target = p.getServer().getPlayerExact(targetName);
                    if (target == null) { p.sendMessage(color("&c온라인 플레이어만 지원합니다.")); return true; }
                    int slot; try { slot = Integer.parseInt(args[3]); } catch (Exception e) { p.sendMessage(color("&c슬롯은 숫자여야 합니다.")); return true; }
                    boolean ok = shopManager.adminRemoveListingToPlayer(target.getUniqueId(), slot, target);
                    if (!ok) p.sendMessage(color("&c해당 슬롯에 등록된 아이템이 없습니다."));
                    else p.sendMessage(color("&a" + target.getName() + "의 슬롯 " + slot + " 제거 완료."));
                    return true;
                } else { p.sendMessage(color("&c사용법: /유저상점 설정 제거 <슬롯> 또는 <플레이어> <슬롯>")); return true; }
            } else { p.sendMessage(color("&c알 수 없는 설정입니다.")); return true; }
        }

        guiManager.openMain(p, 1);
        return true;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
}
