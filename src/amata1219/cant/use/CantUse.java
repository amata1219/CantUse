package amata1219.cant.use;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class CantUse extends JavaPlugin implements Listener {

    private List<String> list;
    private List<UUID> uuid;

    public void onEnable() {
        getCommand("cantuse").setExecutor(this);
        saveDefaultConfig();
        list = getConfig().getStringList("List");
        uuid = new ArrayList<>();
        getConfig().getStringList("UUID").forEach(s -> uuid.add(UUID.fromString(s)));
        getServer().getPluginManager().registerEvents(this, this);
    }

    public void onDisable() {
        getConfig().set("List", list);
        List<String> s = new ArrayList<>();
        uuid.forEach((u) -> {
            s.add(u.toString());
        });
        getConfig().set("UUID", s);
        saveConfig();
        reloadConfig();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("" + ChatColor.AQUA + ChatColor.BOLD + "CantUse");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "/cantuse reload");
            sender.sendMessage(ChatColor.AQUA + "コンフィグを再読み込みします。");
            sender.sendMessage(ChatColor.AQUA + "/cantuse add");
            sender.sendMessage(ChatColor.AQUA + "メインハンドに持っているアイテムを使用制限リストに追加します。");
            sender.sendMessage(ChatColor.AQUA + "/cantuse remove");
            sender.sendMessage(ChatColor.AQUA + "メインハンドに持っているアイテムを使用制限リストから削除します。");
            sender.sendMessage(ChatColor.AQUA + "/cantuse list");
            sender.sendMessage(ChatColor.AQUA + "アイテム使用制限リストの内容を表示します。");
            sender.sendMessage(ChatColor.AQUA + "/cantuse register");
            sender.sendMessage(ChatColor.AQUA + "コマンド実行者をアイテム制限回避リストに追加します。");
        } else {
            if (args[0].equalsIgnoreCase("add")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
                    return true;
                }

                Player player = (Player)sender;
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
                    String text = convertItemStack(item);
                    if (list.contains(text)) {
                        sender.sendMessage(ChatColor.RED + "このアイテム既に使用制限リストに追加されています。");
                        return true;
                    }

                    list.add(text);
                    getConfig().set("List", list);
                    saveConfig();
                    reloadConfig();
                    player.sendMessage(ChatColor.AQUA + "アイテム(" + text + ")を追加しました。");
                    return true;
                }

                player.sendMessage(ChatColor.RED + "任意のアイテムをメインハンドに持って下さい。");
            } else if (args[0].equalsIgnoreCase("remove")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
                    return true;
                }

                Player player = (Player)sender;
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
                    String text = convertItemStack(item);
                    if (!list.contains(text)) {
                        sender.sendMessage(ChatColor.RED + "このアイテムは使用制限リストに追加されていません。");
                        return true;
                    }

                    list.remove(text);
                    getConfig().set("List", list);
                    saveConfig();
                    reloadConfig();
                    player.sendMessage(ChatColor.AQUA + "アイテム(" + text + ")を削除しました。");
                    return true;
                }

                player.sendMessage(ChatColor.RED + "任意のアイテムをメインハンドに持って下さい。");
            } else if (args[0].equalsIgnoreCase("list")) {
                StringBuilder builder = new StringBuilder();
                list.forEach((s) -> {
                    builder.append("§b, §r" + s);
                });
                sender.sendMessage(ChatColor.AQUA + "使用制限中のアイテム一覧");
                sender.sendMessage(builder.substring(3));
            } else if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage(ChatColor.AQUA + "コンフィグを再読み込みしました。");
            } else if (args[0].equalsIgnoreCase("register")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "ゲーム内から実行して下さい。");
                    return true;
                }

                Player player = (Player) sender;
                if (contains(player)) {
                    player.sendMessage(ChatColor.RED + "既に登録されています。");
                    return true;
                }

                uuid.add(player.getUniqueId());
                player.sendMessage(ChatColor.RED + "登録しました。");
                return true;
            }

        }
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (contains(e.getPlayer()) || !e.hasItem()) return;

        if (e.getItem() == null || e.getItem().getType() == Material.AIR) return;

        if (list.contains(convertItemStack(e.getItem()))) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;

        Player player = (Player) e.getPlayer();
        if (contains(player)) return;

        player.getInventory().forEach((item) -> {
            if (item != null && item.getType() != Material.AIR && list.contains(convertItemStack(item))) {
                e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), item);
                item.setAmount(0);
            }

        });
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player) || contains((Player) e.getEntity())) return;

        ItemStack item = e.getItem().getItemStack();
        if (item.getType() != Material.AIR && list.contains(convertItemStack(item))) e.setCancelled(true);
    }

    public String convertItemStack(ItemStack item) {
        String text = item.getType().toString();
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) text = text + "#" + item.getItemMeta().getDisplayName();
            if (item.getItemMeta().hasLore()) text = text + "#" + item.getItemMeta().getLore().toString();
        }

        return text;
    }

    public boolean contains(Player player) {
        return uuid.contains(player.getUniqueId());
    }
}