package com.Ben12345rocks.VotingPlugin.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.Ben12345rocks.VotingPlugin.Main;
import com.Ben12345rocks.VotingPlugin.Files.Files;

public class ConfigOtherRewards {

	static ConfigOtherRewards instance = new ConfigOtherRewards();

	static Main plugin = Main.plugin;

	public static ConfigOtherRewards getInstance() {
		return instance;
	}

	FileConfiguration data;

	File dFile;

	private ConfigOtherRewards() {
	}

	public ConfigOtherRewards(Main plugin) {
		ConfigOtherRewards.plugin = plugin;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getAllSitesReward() {
		try {
			return (ArrayList<String>) getData().getList("AllSites");
		} catch (Exception ex) {
			return new ArrayList<String>();
		}
	}

	public FileConfiguration getData() {
		return data;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getFirstVoteRewards() {
		try {
			return (ArrayList<String>) getData().getList("FirstVote");
		} catch (Exception ex) {
			return new ArrayList<String>();
		}
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getNumberOfVotes() {
		try {
			ArrayList<String> list = (ArrayList<String>) getData().getList(
					"CumulativeRewards");
			if (list != null) {
				return list;
			}

			return new ArrayList<String>();
		} catch (Exception ex) {
			return new ArrayList<String>();
		}
	}

	public boolean getNumberOfVotesVotesInSameDay() {
		return getData().getBoolean("VotesInSameDay");
	}

	public boolean getVotePartyEnabled() {
		return getData().getBoolean("VoteParty.Enabled");
	}

	public boolean getVotePartyGiveAllPlayers() {
		return getData().getBoolean("VoteParty.GiveAllPlayers");
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getVotePartyRewards() {
		return (ArrayList<String>) getData().getList("VoteParty.Rewards");
	}

	public int getVotePartyVotesRequired() {
		return getData().getInt("VoteParty.VotesRequired");
	}

	public int getVotesRequired() {
		return getData().getInt("VotesRequired");
	}

	public void reloadData() {
		data = YamlConfiguration.loadConfiguration(dFile);
	}

	public void saveData() {
		Files.getInstance().editFile(dFile, data);
	}

	public void setup(Plugin p) {
		if (!p.getDataFolder().exists()) {
			p.getDataFolder().mkdir();
		}

		dFile = new File(p.getDataFolder(), "Rewards.yml");

		if (!dFile.exists()) {
			try {
				dFile.createNewFile();
				plugin.saveResource("Rewards.yml", true);
			} catch (IOException e) {
				Bukkit.getServer()
				.getLogger()
				.severe(ChatColor.RED + "Could not create Rewards.yml!");
			}
		}

		data = YamlConfiguration.loadConfiguration(dFile);
	}

}
