package co.ignitus.mysqlnicks.commands;

import co.ignitus.mysqlnicks.MySQLNicks;
import co.ignitus.mysqlnicks.util.DataUtil;
import co.ignitus.mysqlnicks.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class NickCMD implements CommandExecutor {

    final MySQLNicks mySQLNicks = MySQLNicks.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.getMessage("nick.insufficient-arguments"));
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.getMessage("nick.no-console"));
            return true;
        }
        final Player player = (Player) sender;
        if (!player.hasPermission("mysqlnicks.nick")) {
            player.sendMessage(MessageUtil.getMessage("nick.no-permission"));
            return true;
        }

        //The player who's nickname is been changed
        final Player target;
        String nickname;
        if (args.length == 2 && player.hasPermission("mysqlnicks.staff")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(MessageUtil.getMessage("nick.invalid-player"));
                return true;
            }
            nickname = args[1];
        } else {
            target = player;
            nickname = args[0];
        }

        final FileConfiguration config = mySQLNicks.getConfig();
        if (config.getBoolean("limit.enabled")) {
            boolean includeColors = config.getBoolean("limit.include-color");
            int length = config.getInt("limit.length");
            String input = includeColors ? nickname : ChatColor.stripColor(MessageUtil.format(nickname));
            if (input.length() > length && !player.hasPermission("mysqlnicks.bypass.limit")) {
                player.sendMessage(MessageUtil.getMessage("nick.exceeds-limit"));
                return true;
            }
        }

        nickname = nickname.replace("§", "&");
        if (!player.hasPermission("mysqlnicks.nick.color")) {
            if (!player.hasPermission("mysqlnicks.nick.color.simple") && !MessageUtil.stripLegacy(nickname).equals(nickname)) {
                player.sendMessage(MessageUtil.getMessage("nick.no-codes"));
                return true;
            }
            if (!player.hasPermission("mysqlnicks.nick.color.hex") && !MessageUtil.stripHex(nickname).equals(nickname)) {
                player.sendMessage(MessageUtil.getMessage("nick.no-hex"));
                return true;
            }
        }
        if (!player.hasPermission("mysqlnicks.nick.format") && (nickname.contains("&l") ||
                nickname.contains("&m") || nickname.contains("&n") || nickname.contains("&o") ||
                nickname.contains("&r"))) {
            player.sendMessage(MessageUtil.getMessage("nick.no-formatting"));
            return true;
        }
        if (!player.hasPermission("mysqlnicks.nick.magic") && nickname.contains("&k")) {
            player.sendMessage(MessageUtil.getMessage("nick.no-magic"));
            return true;
        }

        if (nickname.equalsIgnoreCase("off"))
            nickname = null;

        String path = "nick." + (target.equals(player) ? "" : "staff.");
        if (!DataUtil.setNickname(target.getUniqueId(), nickname)) {
            player.sendMessage(MessageUtil.getMessage(path + "error",
                    "%player%", target.getName()));
            return true;
        }
        if (nickname == null) {
            player.sendMessage(MessageUtil.getMessage(path + "unset",
                    "%player%", target.getName()));
            return true;
        }
        player.sendMessage(MessageUtil.getMessage(path + "set",
                "%player%", target.getName(),
                "%nickname%", nickname));
        return true;
    }
}
