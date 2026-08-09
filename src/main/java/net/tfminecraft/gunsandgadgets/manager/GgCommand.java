package net.tfminecraft.gunsandgadgets.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;

/**
 * {@code /gg reload} — reload all GunsAndGadgets YAML configs.
 */
public final class GgCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("§eUsage: /gg reload");
			return true;
		}
		if (!args[0].equalsIgnoreCase("reload")) {
			sender.sendMessage("§eUsage: /gg reload");
			return true;
		}
		if (!sender.hasPermission("gunsandgadgets.reload")) {
			sender.sendMessage("§cYou do not have permission to reload GunsAndGadgets.");
			return true;
		}
		GunsAndGadgets.getInstance().reload();
		sender.sendMessage("§a[GunsAndGadgets] Reloaded configs (skins, parts, ammunition, config).");
		return true;
	}

	@Override
	public List<String> onTabComplete(
		CommandSender sender,
		Command command,
		String alias,
		String[] args
	) {
		if (args.length == 1 && sender.hasPermission("gunsandgadgets.reload")) {
			String prefix = args[0].toLowerCase();
			List<String> out = new ArrayList<>();
			if ("reload".startsWith(prefix)) {
				out.add("reload");
			}
			return out;
		}
		return Collections.emptyList();
	}
}
