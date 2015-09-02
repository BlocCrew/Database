package com.bloccrew.plugin.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.milkbowl.vault.economy.Economy;

public class Database extends JavaPlugin implements Listener{

	private SQLStuff sql;
	public static Economy econ = null;
	File configFile;
    FileConfiguration config;
    int interval;
    double lowRate, midRate, highRate, lowBal, midBal;
    String user, pass, url, database, updated;

	public void onEnable(){
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy() ) {
			getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Database");
		configFile = new File(plugin.getDataFolder(), "config.yml");
		
		try {
	        firstRun();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    config = new YamlConfiguration();
	    loadYamls();
	    
	    BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
            	Date date = new Date();
            	DateFormat df = new SimpleDateFormat("EEEE");
            	String day = df.format(date);
            	if(day.equals("Saturday")){
                	df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                	String[] time = df.format(date).split("-");
                	if(time[3].equals("23") && time[4].equals("59")){
                		updateDatabase();
                	}
            	}
            }
        }, 0L, 1200L); //Every minute in ticks
        
		getLogger().info("[DATABASE] Database has been enabled");
	}
	
	public void onDisable(){
		getLogger().info("[DATABASE] Database has been disabled");
	}
	
	private void firstRun(){
		if(!configFile.exists()){
	        configFile.getParentFile().mkdirs();
	        copy(getResource("config.yml"), configFile);
	    }
	}
	
	private void copy(InputStream in, File file) {
	    try {
	        OutputStream out = new FileOutputStream(file);
	        byte[] buf = new byte[1024];
	        int len;
	        while((len=in.read(buf))>0){
	            out.write(buf,0,len);
	        }
	        out.close();
	        in.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	public void loadYamls() {
	    try {
	        config.load(configFile);
			url = config.getString("url");
			database = config.getString("database");
			user = config.getString("user");
			pass = config.getString("pass");
			sql = new SQLStuff(url, database, user, pass);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	@EventHandler
	public void onPlayerJoin(PlayerLoginEvent event){
		Date date = new Date();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String d = dateFormat.format(date);
		Player p = event.getPlayer();
		String name = p.getName();
		String uuid = p.getUniqueId().toString();
		Bukkit.getLogger().severe("[DATABASE] " + name + ", " + uuid + ", " + d);
		sql.execute("insert into database_players(uuid, player, firstJoined, recentlyJoined) values('"+uuid+"', '"+name+"', '"+d+"', '"+d+"') ON DUPLICATE KEY UPDATE player = '"+name+"', recentlyJoined = '"+d+"'");
	}
	
	private void updateDatabase(){
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date();
		String columnName = "D" + dateFormat.format(date);
		sql.execute("ALTER TABLE database_econ ADD "+columnName+" double");
		for(OfflinePlayer p : Bukkit.getServer().getOfflinePlayers()){
			String name = p.getName();
			String uuid = p.getUniqueId().toString();
			double bal = econ.getBalance(p);
			sql.execute("insert into database_econ(uuid, player, balance) values('"+uuid+"', '"+name+"', '"+bal+"') ON DUPLICATE KEY UPDATE player = '"+name+"', balance = "+bal);
			sql.execute("UPDATE database_econ SET "+columnName+" = "+bal+" WHERE uuid = '"+uuid+"'");
		}
		
		String serverVersion = Bukkit.getServer().getBukkitVersion();
		sql.execute("insert into database_plugins(plugin, version) values('SERVER', '"+serverVersion+"') ON DUPLICATE KEY UPDATE version = "+serverVersion);
		for(Plugin p : Bukkit.getServer().getPluginManager().getPlugins()){
			String name = p.getName();
			PluginDescriptionFile pdf = p.getDescription();
			String version = pdf.getVersion();
			String website = pdf.getWebsite();
			sql.execute("insert into database_plugins(plugin, version, website) values('"+name+"', '"+version+"', '"+website+"') ON DUPLICATE KEY UPDATE version = '"+version+"', website = '"+website+"'");
		}
		getLogger().info("[DATABASE] Database updated.");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("database") && sender.hasPermission("database")){
			updateDatabase();
			sender.sendMessage("Database updated.");
			return true;
		}
		return false;
	}

}
