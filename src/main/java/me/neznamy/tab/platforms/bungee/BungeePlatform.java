package me.neznamy.tab.platforms.bungee;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.BungeeTABLoadEvent;
import me.neznamy.tab.shared.Platform;
import me.neznamy.tab.shared.TAB;
import me.neznamy.tab.shared.config.ConfigurationFile;
import me.neznamy.tab.shared.features.GlobalPlayerlist;
import me.neznamy.tab.shared.features.NameTag16;
import me.neznamy.tab.shared.features.PlaceholderManager;
import me.neznamy.tab.shared.features.bossbar.BossBar;
import me.neznamy.tab.shared.permission.BungeePerms;
import me.neznamy.tab.shared.permission.LuckPerms;
import me.neznamy.tab.shared.permission.PermissionPlugin;
import me.neznamy.tab.shared.permission.UltraPermissions;
import me.neznamy.tab.shared.permission.VaultBridge;
import me.neznamy.tab.shared.placeholders.PlayerPlaceholder;
import me.neznamy.tab.shared.placeholders.UniversalPlaceholderRegistry;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

/**
 * Bungeecord implementation of Platform
 */
public class BungeePlatform implements Platform {

	//instance of plugin
	private Plugin plugin;
	
	/**
	 * Constructs new instance with given parameter
	 * @param plugin - main class
	 */
	public BungeePlatform(Plugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public PermissionPlugin detectPermissionPlugin() {
		if (ProxyServer.getInstance().getPluginManager().getPlugin("LuckPerms") != null) {
			return new LuckPerms(ProxyServer.getInstance().getPluginManager().getPlugin("LuckPerms").getDescription().getVersion());
		} else if (ProxyServer.getInstance().getPluginManager().getPlugin("UltraPermissions") != null) {
			return new UltraPermissions(ProxyServer.getInstance().getPluginManager().getPlugin("UltraPermissions").getDescription().getVersion());
		} else if (ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms") != null) {
			return new BungeePerms(ProxyServer.getInstance().getPluginManager().getPlugin("BungeePerms").getDescription().getVersion());
		} else {
			return new VaultBridge(Main.plm);
		}
	}

	@Override
	public void loadFeatures() throws Exception{
		TAB tab = TAB.getInstance();
		tab.getPlaceholderManager().addRegistry(new BungeePlaceholderRegistry());
		tab.getPlaceholderManager().addRegistry(new UniversalPlaceholderRegistry());
		tab.getPlaceholderManager().registerPlaceholders();
		tab.getFeatureManager().registerFeature("injection", new BungeePipelineInjector(tab));
		if (tab.getConfiguration().config.getBoolean("change-nametag-prefix-suffix", true)) tab.getFeatureManager().registerFeature("nametag16", new NameTag16(tab));
		loadUniversalFeatures();
		if (tab.getConfiguration().BossBarEnabled) 										tab.getFeatureManager().registerFeature("bossbar", new BossBar(tab));
		if (tab.getConfiguration().config.getBoolean("global-playerlist.enabled", false)) 	tab.getFeatureManager().registerFeature("globalplayerlist", new GlobalPlayerlist(tab));
		for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
			TabPlayer t = new BungeeTabPlayer(p);
			tab.data.put(p.getUniqueId(), t);
		}
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void sendConsoleMessage(String message, boolean translateColors) {
		ProxyServer.getInstance().getConsole().sendMessage(translateColors ? message.replace('&', '\u00a7') : message);
	}
	
	@Override
	public void registerUnknownPlaceholder(String identifier) {
		if (identifier.startsWith("%rel_")) return;
		if (identifier.contains("_")) {
			String plugin = identifier.split("_")[0].replace("%", "").toLowerCase();
			if (plugin.equals("some")) return;
			TAB.getInstance().debug("Detected used PlaceholderAPI placeholder " + identifier);
			PlaceholderManager pl = TAB.getInstance().getPlaceholderManager();
			int cooldown = pl.defaultRefresh;
			if (pl.playerPlaceholderRefreshIntervals.containsKey(identifier)) cooldown = pl.playerPlaceholderRefreshIntervals.get(identifier);
			if (pl.serverPlaceholderRefreshIntervals.containsKey(identifier)) cooldown = pl.serverPlaceholderRefreshIntervals.get(identifier);
			TAB.getInstance().getPlaceholderManager().registerPlaceholder(new PlayerPlaceholder(identifier, cooldown){
				public String get(TabPlayer p) {
					Main.plm.requestPlaceholder(p, identifier);
					return lastValue.get(p.getName());
				}
			});
			return;
		}
	}
	
	@Override
	public void convertConfig(ConfigurationFile config) {
		convertUniversalOptions(config);
		if (config.getName().equals("config.yml")) {
			if (config.getObject("global-playerlist") instanceof Boolean) {
				rename(config, "global-playerlist", "global-playerlist.enabled");
				config.set("global-playerlist.spy-servers", Arrays.asList("spyserver1", "spyserver2"));
				Map<String, List<String>> serverGroups = new HashMap<String, List<String>>();
				serverGroups.put("lobbies", Arrays.asList("lobby1", "lobby2"));
				serverGroups.put("group2", Arrays.asList("server1", "server2"));
				config.set("global-playerlist.server-groups", serverGroups);
				config.set("global-playerlist.display-others-as-spectators", false);
				TAB.getInstance().print('2', "Converted old global-playerlist section to new one in config.yml.");
			}
			rename(config, "tablist-objective-value", "yellow-number-in-tablist");
		}
	}
	
	@Override
	public String getServerVersion() {
		return ProxyServer.getInstance().getVersion();
	}

	@Override
	public String getSeparatorType() {
		return "server";
	}

	@Override
	public File getDataFolder() {
		return plugin.getDataFolder();
	}

	@Override
	public void callLoadEvent() {
		ProxyServer.getInstance().getPluginManager().callEvent(new BungeeTABLoadEvent());
	}

	@Override
	public int getMaxPlayers() {
		return ProxyServer.getInstance().getConfigurationAdapter().getListeners().iterator().next().getMaxPlayers();
	}
}