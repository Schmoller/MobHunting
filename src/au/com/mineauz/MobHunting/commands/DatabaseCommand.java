package au.com.mineauz.MobHunting.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import au.com.mineauz.MobHunting.Messages;
import au.com.mineauz.MobHunting.MobHunting;
import au.com.mineauz.MobHunting.storage.DataStore;
import au.com.mineauz.MobHunting.storage.DataStoreException;
import au.com.mineauz.MobHunting.storage.MySQLDataStore;
import au.com.mineauz.MobHunting.storage.SQLiteDataStore;

public class DatabaseCommand implements ICommand, Listener {

	private DataStore mStore;

	// private DataStoreManager mStoreManager;

	public DatabaseCommand() {
		if (MobHunting.config().databaseType.equalsIgnoreCase("mysql"))
			mStore = new MySQLDataStore();
		else
			mStore = new SQLiteDataStore();

		try {
			mStore.initialize();
		} catch (DataStoreException e) {
			e.printStackTrace();

			try {
				mStore.shutdown();
			} catch (DataStoreException e1) {
				e1.printStackTrace();
			}
		}
		// mStoreManager = new DataStoreManager(mStore);
	}

	@Override
	public String getName() {
		return "database";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "db" };
	}

	@Override
	public String getPermission() {
		return "mobhunting.database";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { label + ChatColor.GOLD + " fixLeaderboard" };
	}

	@Override
	public String getDescription() {
		return Messages.getString("mobhunting.commands.database.description");
	}

	@Override
	public boolean canBeConsole() {
		return true;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label,
			String[] args) {

		ArrayList<String> items = new ArrayList<String>();
		if (args.length == 1) {
			items.add("fixLeaderboard");
			items.add("backup");
			items.add("restore");
			items.add("deletebackup");
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("backup")) {
				// TODO: set items do defaultname
				// items.add(today);
			} else if (args[0].equalsIgnoreCase("backup")) {
				// TODO: list posible backups
			} else if (args[0].equalsIgnoreCase("deletebackup")) {
				// TODO: list posible backups
			}
		}
		return items;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {
		if (args.length == 0)
			return false;
		if (args.length == 1 && (args[0].equalsIgnoreCase("fixleaderboard"))) {
			try {
				mStore.databaseFixLeaderboard();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("backup")) {
			// TODO: create a backup
			sender.sendMessage("Backup feature is not implemented yet.");
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("restore")) {
			// TODO: restore a backup
			sender.sendMessage("Restore feature is not implemented yet.");
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("deletebackup")) {
			// TODO: restore a backup
			sender.sendMessage("Deletebackup feature is not implemented yet.");
			return true;
		}
		return false;
	}
}
