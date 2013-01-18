package com.DustinSchreiber.SaveLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public class SaveLog extends JavaPlugin implements Listener {
	
	public final Logger logger = Logger.getLogger("Minecraft");
	public String LogPath = "server.log";
    File configPath = new File("plugins/SaveLog/config.yml");
    File DirPath = new File("plugins/SaveLog/backups");
    File LogFile = new File(this.LogPath);
    String timestamp = "";
    File BackupFile;
    int backupLogs = 0;
    
	@Override
	public void onEnable(){
	    if(!DirPath.exists()){ DirPath.mkdirs(); }
		getServer().getPluginManager().registerEvents(this, this);
	    FileConfiguration config = getConfig();
	    if (!configPath.exists()){
	      this.logger.info("[SaveLog] Creating Default Config");
	      config.options().header("SaveLog Configuration\nBackupsToSave - Number of auto backups\nBackupInterval - Number of seconds between auto backups\nAuto - TRUE enables auto backup\nNOTE: Backup here also clears afterward");
	      config.set("BackupsToSave", Integer.valueOf(5));
	      config.set("BackupInterval", Integer.valueOf(86400));
	      config.set("Auto", Boolean.valueOf(true));
	      saveConfig();
	    }
	    generateTimestamp();
	    if(config.getBoolean("Auto")){
	    	schedule();
	    }
	    backupLogs = DirPath.listFiles().length;
	}
	
	@Override
	public void onDisable(){
	    generateTimestamp();
		try{
	    	copyLog();
	    }catch (IOException e){
	    	e.printStackTrace();
	    }
		clearLog();
		getLogger().info("SaveLog disabled!");
	}
	
	public void generateTimestamp(){
	    Date dNow = new Date();
	    SimpleDateFormat ft = new SimpleDateFormat ("MM_dd_YY 'at' hh_mm_ss a zzz");
	    timestamp = ft.format(dNow);
	    BackupFile = new File(DirPath + "/" + timestamp + ".log");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
    	if(cmd.getName().equalsIgnoreCase("savelog")){
    		if(args.length<1){
    			sender.sendMessage("Too few arguments");
    			return false;
    		}
    		if(args[0].equalsIgnoreCase("backupnow")){
    		    generateTimestamp();
				getLogger().info("Log file backed up and cleared by " + sender.getName());
				for(Player player : getServer().getOnlinePlayers()){
					if(player.hasPermission("savelog.notify")){
						player.sendMessage(ChatColor.ITALIC  + "" + ChatColor.LIGHT_PURPLE  + "Log file backed up and cleared by " + sender.getName());
					}
				}
			    try{
			    	copyLog();
			    }catch (IOException e){
			    	e.printStackTrace();
			    }
				clearLog();
			    return true;
    		}
    		if(args[0].equalsIgnoreCase("clear")){
				getLogger().info("Log file cleared by " + sender.getName());
				for(Player player : getServer().getOnlinePlayers()){
					if(player.hasPermission("savelog.notify")){
						player.sendMessage(ChatColor.ITALIC  + "" + ChatColor.LIGHT_PURPLE  + "Log file cleared by " + sender.getName());
					}
				}
    		    clearLog();
				return true;
    		}
    	}
		return false;
	}
	
	public void clearLog(){
		try{
			BufferedWriter out = new BufferedWriter(new FileWriter("server.log"));
			out.write("");
			out.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void schedule(){
		boolean clear = getConfig().getBoolean("Auto");
		if(clear == false){
			return;
		}
		int timer = getConfig().getInt("BackupInterval");
		long time = timer * 20;
		getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new Runnable(){
					@Override
					public void run(){
		    		    generateTimestamp();
						for(Player player : getServer().getOnlinePlayers()){
							if(player.hasPermission("savelog.notify")){
								player.sendMessage(ChatColor.ITALIC  + "" + ChatColor.LIGHT_PURPLE  + "<Auto> Log backed up and cleared");
							}
						}
						getLogger().info("<Auto> Log backed up and cleared");
					    try{
					    	copyLog();
					    }catch (IOException e){
					    	e.printStackTrace();
					    }
						clearLog();
					}
				}, 0, time);
	}
	
	public void copyLog() throws IOException{
	    backupLogs = DirPath.listFiles().length;
		if(backupLogs >= getConfig().getInt("BackupsToSave")){
			long lastMod = Long.MAX_VALUE;
			File oldest = null;
			for(File file : DirPath.listFiles()){
				if (file.lastModified() < lastMod){
					oldest = file;
					lastMod = file.lastModified();
				}
			}
			oldest.delete();
		}
		Files.copy(LogFile, BackupFile);
	}
}
