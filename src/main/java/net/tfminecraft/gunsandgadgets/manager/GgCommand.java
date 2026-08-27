package net.tfminecraft.gunsandgadgets.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.gunsandgadgets.GunsAndGadgets;
import net.tfminecraft.gunsandgadgets.utils.GunStatRefresher;

/**
 * {@code /gg reload} and {@code /gg refresh} — GunsAndGadgets admin commands.
 */
public final class GgCommand implements CommandExecutor, TabCompleter {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("§eUsage: /gg reload | /gg refresh");
			return true;
		}
		String sub = args[0].toLowerCase();
		if (sub.equals("reload")) {
			if (!sender.hasPermission("gunsandgadgets.reload")) {
				sender.sendMessage("§cYou do not have permission to reload GunsAndGadgets.");
				return true;
			}
			GunsAndGadgets.getInstance().reload();
			sender.sendMessage("§a[GunsAndGadgets] Reloaded configs (skins, parts, ammunition, config).");
			return true;
		}
		if (sub.equals("refresh")) {
			if (!sender.hasPermission("gunsandgadgets.reload")) {
				sender.sendMessage("§cYou do not have permission to refresh GunsAndGadgets items.");
				return true;
			}
			if (!(sender instanceof Player player)) {
				sender.sendMessage("§cThis command can only be used by a player.");
				return true;
			}
			ItemStack hand = player.getInventory().getItemInMainHand();
			if (!GunStatRefresher.isManaged(hand)) {
				player.sendMessage("§cThat item is not a managed crafted gun.");
				return true;
			}
			GunStatRefresher.RefreshResult result = GunStatRefresher.refresh(hand, true);
			if (result.getError() != null) {
				player.sendMessage("§cRefresh failed: " + result.getError());
				return true;
			}
			if (!result.isChanged()) {
				player.sendMessage("§eNothing to update on that gun.");
				return true;
			}
			player.getInventory().setItemInMainHand(result.getItem());
			player.sendMessage("§aGun stats refreshed.");
			return true;
		}
		sender.sendMessage("§eUsage: /gg reload | /gg refresh");
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
			if ("refresh".startsWith(prefix)) {
				out.add("refresh");
			}
			return out;
		}
		return Collections.emptyList();
	}
}
