package main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import main.Metrics.Graph;
import net.minecraft.server.v1_4_6.EntityFireball;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import tools.Abilities;
import tools.BendingType;
import tools.ConfigManager;
import tools.Tools;

public class Bending extends JavaPlugin {

	public static long time_step = 1; // in ms
	public static Logger log = Logger.getLogger("Minecraft");

	public final BendingManager manager = new BendingManager(this);
	public final BendingListener listener = new BendingListener(this);
	private final RevertChecker revertChecker = new RevertChecker(this);
	public final TagAPIListener Taglistener = new TagAPIListener();

	static Map<String, String> commands = new HashMap<String, String>();
	public static ConcurrentHashMap<String, List<BendingType>> benders = new ConcurrentHashMap<String, List<BendingType>>();

	// public BendingPlayers config = new BendingPlayers(getDataFolder(),
	// getResource("bendingPlayers.yml"));
	public static ConfigManager configManager = new ConfigManager();
	public static Language language = new Language();
	public StorageManager config;
	public Tools tools;

	public String[] waterbendingabilities;
	public String[] airbendingabilities;
	public String[] earthbendingabilities;
	public String[] firebendingabilities;
	public String[] chiblockingabilities;

	public void onDisable() {

		Tools.stopAllBending();
	}

	public void onEnable() {

		configManager.load(new File(getDataFolder(), "config.yml"));
		language.load(new File(getDataFolder(), "language.yml"));

		config = new StorageManager(getDataFolder());

		tools = new Tools(config);

		tools = new Tools(config);

		for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
			benders.put(player.getName(),
					config.getBendingTypes(player.getName()));
		}

		waterbendingabilities = Abilities.getWaterbendingAbilities();
		airbendingabilities = Abilities.getAirbendingAbilities();
		earthbendingabilities = Abilities.getEarthbendingAbilities();
		firebendingabilities = Abilities.getFirebendingAbilities();
		chiblockingabilities = Abilities.getChiBlockingAbilities();

		getServer().getPluginManager().registerEvents(listener, this);

		if (Bukkit.getPluginManager().getPlugin("TagAPI") != null
				&& ConfigManager.useTagAPI) {
			getServer().getPluginManager().registerEvents(Taglistener, this);
		}

		getServer().getScheduler().scheduleSyncRepeatingTask(this, manager, 0,
				1);

		getServer().getScheduler().scheduleAsyncRepeatingTask(this,
				revertChecker, 0, 40);

		removeFireballs();

		Tools.printHooks();
		Tools.verbose("Bending v" + this.getDescription().getVersion()
				+ " has been loaded.");

		try {
			Metrics metrics = new Metrics(this);

			Graph bending = metrics.createGraph("Bending");

			bending.addPlotter(new Metrics.Plotter("Air") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {
						if (Tools.isBender(p.getName(), BendingType.Air))
							i++;
					}
					return i;
				}

			});

			bending.addPlotter(new Metrics.Plotter("Fire") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {
						if (Tools.isBender(p.getName(), BendingType.Fire))
							i++;
					}
					return i;
				}

			});

			bending.addPlotter(new Metrics.Plotter("Water") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {
						if (Tools.isBender(p.getName(), BendingType.Water))
							i++;
					}
					return i;
				}

			});

			bending.addPlotter(new Metrics.Plotter("Earth") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {
						if (Tools.isBender(p.getName(), BendingType.Earth))
							i++;
					}
					return i;
				}

			});

			bending.addPlotter(new Metrics.Plotter("Chi Blocker") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {
						if (Tools.isBender(p.getName(), BendingType.ChiBlocker))
							i++;
					}
					return i;
				}

			});

			bending.addPlotter(new Metrics.Plotter("Non-Bender") {

				@Override
				public int getValue() {
					int i = 0;
					for (OfflinePlayer p : Bukkit.getServer()
							.getOfflinePlayers()) {

						if (!Tools.isBender(p.getName(), BendingType.ChiBlocker)
								&& !Tools.isBender(p.getName(), BendingType.Air)
								&& !Tools.isBender(p.getName(),
										BendingType.Fire)
								&& !Tools.isBender(p.getName(),
										BendingType.Water)
								&& !Tools.isBender(p.getName(),
										BendingType.Earth))
							i++;
					}
					return i;
				}

			});

			metrics.start();
			log.info("Bending is sending data for Plugin Metrics.");
		} catch (IOException e) {
			// Failed to submit the stats :-(
		}

		registerCommands();

	}

	public void reloadConfiguration() {
		getConfig().options().copyDefaults(true);
		saveConfig();

	}

	private void registerCommands() {
		commands.put("command.admin", "remove <player>");
		commands.put("admin.reload", "reload");
		commands.put("admin.permaremove", "permaremove <player>");
		commands.put("command.choose", "choose <element>");
		commands.put("admin.choose", "choose <player> <element>");
		commands.put("admin.add", "add <element>");
		commands.put("command.displayelement", "display <element>");
		commands.put("command.clear", "clear");
		commands.put("command.display", "display");
		commands.put("command.bind", "bind <ability>");
	}

	private void removeFireballs() {
		for (World world : getServer().getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (entity instanceof EntityFireball) {
					entity.remove();
				}
			}
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		if (cmd.getName().equalsIgnoreCase("bending")) {

			new BendingCommand(player, args, getDataFolder(), config,
					getServer());

		}

		return true;
	}
}
