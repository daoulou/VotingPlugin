package com.bencodez.votingplugin.commands;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.bencodez.advancedcore.DebugLevel;
import com.bencodez.advancedcore.api.command.CommandHandler;
import com.bencodez.advancedcore.api.command.TabCompleteHandle;
import com.bencodez.advancedcore.api.command.TabCompleteHandler;
import com.bencodez.advancedcore.api.gui.GUIMethod;
import com.bencodez.advancedcore.api.inventory.BInventory;
import com.bencodez.advancedcore.api.inventory.BInventory.ClickEvent;
import com.bencodez.advancedcore.api.inventory.BInventoryButton;
import com.bencodez.advancedcore.api.item.ItemBuilder;
import com.bencodez.advancedcore.api.messages.StringParser;
import com.bencodez.advancedcore.api.misc.ArrayUtils;
import com.bencodez.advancedcore.api.misc.PlayerUtils;
import com.bencodez.advancedcore.api.rewards.RewardHandler;
import com.bencodez.advancedcore.api.rewards.RewardOptions;
import com.bencodez.advancedcore.api.time.TimeChecker;
import com.bencodez.advancedcore.api.updater.Updater;
import com.bencodez.advancedcore.api.user.UUID;
import com.bencodez.advancedcore.api.user.UserStorage;
import com.bencodez.advancedcore.api.valuerequest.ValueRequest;
import com.bencodez.advancedcore.api.valuerequest.listeners.BooleanListener;
import com.bencodez.advancedcore.api.valuerequest.listeners.StringListener;
import com.bencodez.advancedcore.command.gui.UserGUI;
import com.bencodez.votingplugin.VotingPluginMain;
import com.bencodez.votingplugin.commands.executers.CommandAliases;
import com.bencodez.votingplugin.commands.gui.AdminGUI;
import com.bencodez.votingplugin.commands.gui.admin.AdminVoteHelp;
import com.bencodez.votingplugin.commands.gui.admin.AdminVotePerms;
import com.bencodez.votingplugin.commands.gui.admin.AdminVotePlaceholders;
import com.bencodez.votingplugin.commands.gui.admin.AdminVotePlaceholdersPlayer;
import com.bencodez.votingplugin.commands.gui.player.VoteBest;
import com.bencodez.votingplugin.commands.gui.player.VoteGUI;
import com.bencodez.votingplugin.commands.gui.player.VoteHelp;
import com.bencodez.votingplugin.commands.gui.player.VoteLast;
import com.bencodez.votingplugin.commands.gui.player.VoteNext;
import com.bencodez.votingplugin.commands.gui.player.VoteShop;
import com.bencodez.votingplugin.commands.gui.player.VoteStreak;
import com.bencodez.votingplugin.commands.gui.player.VoteToday;
import com.bencodez.votingplugin.commands.gui.player.VoteTopVoter;
import com.bencodez.votingplugin.commands.gui.player.VoteTopVoterLastMonth;
import com.bencodez.votingplugin.commands.gui.player.VoteTotal;
import com.bencodez.votingplugin.commands.gui.player.VoteURL;
import com.bencodez.votingplugin.commands.gui.player.VoteURLVoteSite;
import com.bencodez.votingplugin.commands.tabcompleter.AliasesTabCompleter;
import com.bencodez.votingplugin.config.Config;
import com.bencodez.votingplugin.config.ConfigVoteSites;
import com.bencodez.votingplugin.config.GUI;
import com.bencodez.votingplugin.config.SpecialRewardsConfig;
import com.bencodez.votingplugin.data.ServerData;
import com.bencodez.votingplugin.events.PlayerVoteEvent;
import com.bencodez.votingplugin.objects.VoteSite;
import com.bencodez.votingplugin.specialrewards.SpecialRewards;
import com.bencodez.votingplugin.test.VoteTester;
import com.bencodez.votingplugin.topvoter.TopVoter;
import com.bencodez.votingplugin.user.VotingPluginUser;
import com.bencodez.votingplugin.user.UserManager;
import com.bencodez.votingplugin.voteparty.VoteParty;
import com.tchristofferson.configupdater.ConfigUpdater;

// TODO: Auto-generated Javadoc
/**
 * The Class CommandLoader.
 */
public class CommandLoader {

	/** The config. */
	static Config config = Config.getInstance();

	/** The config vote sites. */
	static ConfigVoteSites configVoteSites = ConfigVoteSites.getInstance();

	/** The instance. */
	static CommandLoader instance = new CommandLoader();

	/** The plugin. */
	static VotingPluginMain plugin = VotingPluginMain.plugin;

	/**
	 * Gets the single instance of CommandLoader.
	 *
	 * @return single instance of CommandLoader
	 */
	public static CommandLoader getInstance() {
		return instance;
	}

	private Object pointLock = new Object();

	/** The commands. */
	private HashMap<String, CommandHandler> commands;

	private String adminPerm = "VotingPlugin.Admin";

	private String modPerm = "VotingPlugin.Mod";

	private String playerPerm = "VotingPlugin.Player";

	/**
	 * Instantiates a new command loader.
	 */
	private CommandLoader() {
	}

	/**
	 * Instantiates a new command loader.
	 *
	 * @param plugin the plugin
	 */
	public CommandLoader(VotingPluginMain plugin) {
		CommandLoader.plugin = plugin;
	}

	/**
	 * @return the adminPerm
	 */
	public String getAdminPerm() {
		return adminPerm;
	}

	/**
	 * Gets the commands.
	 *
	 * @return the commands
	 */
	public HashMap<String, CommandHandler> getCommands() {
		return commands;
	}

	/**
	 * @return the modPerm
	 */
	public String getModPerm() {
		return modPerm;
	}

	/**
	 * @return the playerPerm
	 */
	public String getPlayerPerm() {
		return playerPerm;
	}

	public boolean hasPermission(Player player, String cmd) {
		if (cmd.startsWith("votingplugin:")) {
			cmd = cmd.substring("votingplugin:".length());
		}

		if (commands.containsKey(cmd)) {
			return commands.get(cmd).hasPerm(player);
		}

		boolean adminCommand = false;

		if (cmd.startsWith("vote")) {
			cmd = cmd.substring("vote".length());
		} else if (cmd.startsWith("v")) {
			cmd = cmd.substring(1);
		} else if (cmd.startsWith("av")) {
			adminCommand = true;
			cmd = cmd.substring("av".length());
		} else if (cmd.startsWith("adminvote")) {
			adminCommand = true;
			cmd = cmd.substring("adminvote".length());
		}
		// plugin.debug(cmd);
		if (!adminCommand) {
			for (CommandHandler handle : plugin.getVoteCommand()) {
				if (handle.isCommand(cmd)) {
					// plugin.debug("is handle " + ArrayUtils.getInstance()
					// .makeStringList(ArrayUtils.getInstance().convert(handle.getArgs())));
					if (handle.hasPerm(player)) {
						// plugin.debug("has perm");
						return true;
					} else {
						// plugin.debug("no perm " + cmd);
					}
				}
			}
		} else {
			for (CommandHandler handle : plugin.getAdminVoteCommand()) {
				if (handle.isCommand(cmd)) {
					// plugin.debug("is handle " + ArrayUtils.getInstance()
					// .makeStringList(ArrayUtils.getInstance().convert(handle.getArgs())));
					if (handle.hasPerm(player)) {
						// plugin.debug("has perm");
						return true;
					} else {
						// plugin.debug("no perm " + cmd);
					}
				}
			}
		}
		return false;
	}

	public boolean isVotingPluginCommand(Player player, String cmd) {
		if (plugin.getCommand(cmd) != null || cmd.startsWith("votingplugin")) {
			return true;
		}
		for (String str : commands.keySet()) {
			if (str.equalsIgnoreCase(cmd)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Load admin vote command.
	 */
	private void loadAdminVoteCommand() {
		plugin.setAdminVoteCommand(new ArrayList<CommandHandler>());

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "CurrentPluginTime" },
				"VotingPlugin.Commands.AdminVote.CurrentPluginTime|" + adminPerm, "Current plugin time") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Config.getInstance().getFormatTimeFormat());
				sendMessage(sender, TimeChecker.getInstance().getTime().format(formatter));
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "SetPoints", "(number)" },
						"VotingPlugin.Commands.AdminVote.SetPoints|" + adminPerm, "Set players voting points") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						user.setPoints(Integer.parseInt(args[3]));
						sender.sendMessage(
								StringParser.getInstance().colorize("&cSet " + args[1] + " points to " + args[3]));
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ResyncMilestones" },
				"VotingPlugin.Commands.AdminVote.ResyncMilestones|" + adminPerm, "Resync Milestones") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				sendMessage(sender, "&cStarting...");
				for (String uuid : UserManager.getInstance().getAllUUIDs()) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					user.setMilestoneCount(user.getTotal(TopVoter.AllTime));
				}
				sendMessage(sender, "&cFinished");

			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "ResyncMilestonesAlreadyGiven" },
						"VotingPlugin.Commands.AdminVote.ResyncMilestonesGiven|" + adminPerm,
						"Resync Milestones already given") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						sendMessage(sender, "&cStarting...");
						ArrayList<Integer> nums = new ArrayList<Integer>();

						for (String str : SpecialRewardsConfig.getInstance().getMilestoneVotes()) {
							try {
								nums.add(Integer.parseInt(str));
							} catch (Exception e) {
								plugin.getLogger().warning("Failed to get number from " + str);
							}
						}
						for (String uuid : UserManager.getInstance().getAllUUIDs()) {
							VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
							int milestoneCount = user.getMilestoneCount();
							for (int num : nums) {
								if (milestoneCount >= num) {
									if (!user.hasGottenMilestone(num)) {
										sendMessage(sender,
												"&cMilestone " + num + " for " + user.getPlayerName()
														+ " not already given when it should be, Current AllTimeTotal: "
														+ user.getTotal(TopVoter.AllTime) + ", Current MileStoneCount: "
														+ user.getMilestoneCount());
										user.setHasGotteMilestone(num, true);
									}
								}
							}
						}
						sendMessage(sender, "&cFinished");

					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "ResetPoints" },
						"VotingPlugin.Commands.AdminVote.ResetPoints|" + adminPerm, "Clears all points of all players",
						true, false) {

					@Override
					public void execute(CommandSender sender, String[] args) {
						sendMessage(sender, "&cStarting...");
						for (String uuid : UserManager.getInstance().getAllUUIDs()) {
							VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
							user.setPoints(0);
						}
						sendMessage(sender, "&cFinished");

					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ResyncMilestones", "(player)" },
				"VotingPlugin.Commands.AdminVote.SetResyncMilestones|" + adminPerm, "Resync Milestones") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
				user.setMilestoneCount(user.getTotal(TopVoter.AllTime));

				sendMessage(sender, "&cResynced milestones for " + args[1]);

			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "AddPoints", "(number)" },
						"VotingPlugin.Commands.AdminVote.AddPoints|" + adminPerm, "Add to players voting points") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						synchronized (pointLock) {
							user.addPoints(Integer.parseInt(args[3]));
						}
						sender.sendMessage(StringParser.getInstance().colorize("&cGave " + args[1] + " " + args[3]
								+ " points" + ", " + args[1] + " now has " + user.getPoints() + " points"));
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "RemovePoints", "(number)" },
						"VotingPlugin.Commands.AdminVote.RemovePoints|" + adminPerm, "Remove voting points") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						user.removePoints(Integer.parseInt(args[3]));
						sender.sendMessage(StringParser.getInstance().colorize("&cRemoved " + args[3] + " points from "
								+ args[1] + ", " + args[1] + " now has " + user.getPoints() + " points"));
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Help&?" },
				"VotingPlugin.Commands.AdminVote.Help|" + adminPerm, "See this page") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new AdminVoteHelp(plugin, sender, 1).open();
				;
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "ServiceSites&Status" },
						"VotingPlugin.Commands.AdminVote.ServiceSites|" + adminPerm,
						"See a list of all service sites the server got") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						ArrayList<String> serviceSites = ServerData.getInstance().getServiceSites();
						if (!serviceSites.isEmpty()) {
							sendMessage(sender, "&cEvery service site the server has gotten from votifier:");
							for (String serviceSite : serviceSites) {
								boolean hasSite = plugin.hasVoteSite(serviceSite);
								if (hasSite) {
									String siteName = plugin.getVoteSiteName(serviceSite);
									sendMessage(sender, serviceSite + " : Current site = " + siteName);
								} else {
									sendMessage(sender, serviceSite
											+ " : No site with this service site, did you do something wrong?");
								}
							}
						} else {
							sendMessage(sender, "&cNo votes have been received. Please check your votifier settings.");
						}
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Help&?", "(number)" },
				"VotingPlugin.Commands.AdminVote.Help|" + adminPerm, "See this page") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new AdminVoteHelp(plugin, sender, page).open();
				;

			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Perms" },
				"VotingPlugin.Commands.AdminVote.Perms|" + adminPerm, "List permissions") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new AdminVotePerms(plugin, sender).open();
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Perms", "(Player)" },
						"VotingPlugin.Commands.AdminVote.Perms.Other|" + adminPerm,
						"List permissions from the plugin the player has") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						new AdminVotePerms(plugin, sender, 1, args[1]).open();
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Perms", "(Player)", "(Number)" },
						"VotingPlugin.Commands.AdminVote.Perms.Other|" + adminPerm,
						"List permissions from the plugin the player has") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						new AdminVotePerms(plugin, sender, Integer.parseInt(args[2]), args[1]).open();
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Reload" },
				"VotingPlugin.Commands.AdminVote.Reload|" + adminPerm, "Reload plugin, will not reload user storage") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				sendMessage(sender, "&4" + "Reloading " + plugin.getName() + "...");
				plugin.reload();
				if (plugin.isYmlError()) {
					sendMessage(sender, "&3Detected yml error, please check server log for details");
				}
				if (plugin.getProfile().equals("dev")) {
					sendMessage(sender, "&cDetected using dev build, there could be bugs, use at your own risk");
				}
				sendMessage(sender, "&4" + plugin.getName() + " v" + plugin.getDescription().getVersion()
						+ " reloaded! Note: User storage has not been reloaded");
				if (ServerData.getInstance().getServiceSites().size() == 0) {
					sendMessage(sender, "&c"
							+ "Detected that server hasn't received any votes from votifier, please check votifier connection");
				}
				if (!Config.getInstance().isDisableUpdateChecking()
						&& plugin.getUpdater().getResult().equals(Updater.UpdateResult.UPDATE_AVAILABLE)) {
					sendMessage(sender,
							"&3Plugin has update available! https://www.spigotmc.org/resources/votingplugin.15358/");
				}
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ReloadAll" },
				"VotingPlugin.Commands.AdminVote.Reload|" + adminPerm, "Reload plugin, including user storage") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				sendMessage(sender, "&4" + "Reloading " + plugin.getName() + "...");
				plugin.reloadAll();
				if (plugin.isYmlError()) {
					sendMessage(sender, "&3Detected yml error, please check server log for details");
				}
				if (plugin.getProfile().equals("dev")) {
					sendMessage(sender, "&cDetected using dev build, there could be bugs, use at your own risk");
				}
				sendMessage(sender,
						"&4" + plugin.getName() + " v" + plugin.getDescription().getVersion() + " reloaded!");
				if (ServerData.getInstance().getServiceSites().size() == 0) {
					sendMessage(sender, "&c"
							+ "Detected that server hasn't received any votes from votifier, please check votifier connection");
				}
				if (!Config.getInstance().isDisableUpdateChecking()
						&& plugin.getUpdater().getResult().equals(Updater.UpdateResult.UPDATE_AVAILABLE)) {
					sendMessage(sender,
							"&3Plugin has update available! https://www.spigotmc.org/resources/votingplugin.15358/");
				}
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Version" },
				"VotingPlugin.Commands.AdminVote.Version|" + adminPerm, "List version info") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (sender instanceof Player) {
					Player player = (Player) sender;
					Bukkit.getScheduler().runTask(plugin, new Runnable() {

						@Override
						public void run() {
							player.performCommand("bukkit:version " + plugin.getName());
						}
					});

				} else {
					Bukkit.getScheduler().runTask(plugin, new Runnable() {

						@Override
						public void run() {
							Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
									"bukkit:version " + plugin.getName());
						}
					});

				}
				sendMessage(sender,
						"Using AdvancedCore " + plugin.getVersion() + "' built on '" + plugin.getBuildTime());
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Sites" },
				"VotingPlugin.Commands.AdminVote.Sites|" + adminPerm, "List VoteSites", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				AdminGUI.getInstance().openAdminGUIVoteSites((Player) sender);

			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "GUI" },
				"VotingPlugin.Commands.AdminVote.GUI|" + adminPerm, "Admin GUI", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {

				com.bencodez.advancedcore.command.gui.AdminGUI.getInstance().openGUI((Player) sender);

			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Sites", "(sitename)" },
				"VotingPlugin.Commands.AdminVote.Sites.Site|" + adminPerm, "View Site Info") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (sender instanceof Player) {
					AdminGUI.getInstance().openAdminGUIVoteSiteSite((Player) sender, plugin.getVoteSite(args[1]));
				} else {
					sender.sendMessage("Must be a player to do this");
				}
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "UUID", "(player)" },
				"VotingPlugin.Commands.AdminVote.UUID|" + adminPerm, "View UUID of player") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				sender.sendMessage(ChatColor.GREEN + "UUID of player " + ChatColor.DARK_GREEN + args[1]
						+ ChatColor.GREEN + " is: " + PlayerUtils.getInstance().getUUID(args[1]));

			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "PlayerName", "(uuid)" },
				"VotingPlugin.Commands.AdminVote.PlayerName|" + adminPerm, "View PlayerName of player") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				try {
					sender.sendMessage(ChatColor.GREEN + "PlayerName of player " + ChatColor.DARK_GREEN + args[1]
							+ ChatColor.GREEN + " is: " + PlayerUtils.getInstance().getPlayerName(
									UserManager.getInstance().getVotingPluginUser(new UUID(args[1])), args[1]));
				} catch (IllegalArgumentException e) {
					sendMessage(sender, "&cInvalid uuid");
				}

			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ClearTotal" },
				"VotingPlugin.Commands.AdminVote.ClearTotal.All|" + adminPerm, "Reset totals for all players") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (sender instanceof Player) {
					sender.sendMessage(
							StringParser.getInstance().colorize("&cThis command can not be done from ingame"));
					return;
				}

				for (String uuid : UserManager.getInstance().getAllUUIDs()) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					user.clearTotals();
				}
				sender.sendMessage(StringParser.getInstance().colorize("&cCleared totals for everyone"));
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ClearOfflineRewards" },
				"VotingPlugin.Commands.AdminVote.ClearOfflineRewards|" + adminPerm, "Reset offline votes/rewards") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (sender instanceof Player) {
					sender.sendMessage(
							StringParser.getInstance().colorize("&cThis command can not be done from ingame"));
					return;
				}

				for (String uuid : UserManager.getInstance().getAllUUIDs()) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					user.clearOfflineRewards();
				}
				sender.sendMessage(StringParser.getInstance().colorize("&cCleared totals for everyone"));
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "ConvertFromData", "(userstorage)" },
						"VotingPlugin.Commands.AdminVote.ConvertFromData",
						"Convert from selected user storage to current user storage") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						if (sender instanceof Player) {
							sender.sendMessage(StringParser.getInstance().colorize("&cThis can not be done ingame"));
							return;
						}
						try {
							sender.sendMessage("Starting to convert");
							UserStorage prevStorage = UserStorage.valueOf(args[1].toUpperCase());
							plugin.convertDataStorage(prevStorage, VotingPluginMain.plugin.getStorageType());
							sender.sendMessage("Finished converting!");
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				});

		for (final TopVoter top : TopVoter.values()) {
			plugin.getAdminVoteCommand()
					.add(new CommandHandler(new String[] { "User", "(player)", "SetTotal", top.toString(), "(number)" },
							"VotingPlugin.Commands.AdminVote.SetTotal." + top.toString() + "|" + adminPerm,
							"Set " + top.toString() + " totals for player") {

						@Override
						public void execute(CommandSender sender, String[] args) {
							UserManager.getInstance().getVotingPluginUser(args[1]).setTotal(top,
									Integer.parseInt(args[4]));
							sender.sendMessage(StringParser.getInstance().colorize(
									"&cSet " + top.toString() + " total for '" + args[1] + "' to " + args[4]));
						}
					});

			plugin.getAdminVoteCommand()
					.add(new CommandHandler(new String[] { "User", "(player)", "AddTotal", top.toString(), "(number)" },
							"VotingPlugin.Commands.AdminVote.AddTotal." + top.toString() + "|" + adminPerm,
							"Add " + top.toString() + " totals for player") {

						@Override
						public void execute(CommandSender sender, String[] args) {
							VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
							user.setTotal(top, user.getTotal(top) + Integer.parseInt(args[4]));
							sender.sendMessage(StringParser.getInstance()
									.colorize("&cAdded " + top.toString() + " total for " + args[1]));
						}
					});
		}

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "User", "(player)", "ClearTotal" },
				"VotingPlugin.Commands.AdminVote.ClearTotal|" + adminPerm, "Clear Totals for player") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
				user.clearTotals();
				sender.sendMessage(StringParser.getInstance().colorize("&cCleared totals for '" + args[1] + "'"));
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "AddMilestoneCount", "(number)" },
						"VotingPlugin.Commands.AdminVote.AddMilestoneCount|" + adminPerm, "Add milestonecount") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						user.setMilestoneCount(user.getMilestoneCount() + Integer.parseInt(args[3]));
						sender.sendMessage(
								StringParser.getInstance().colorize("&cAdded milestonecount for " + args[1]));
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "SetMilestoneCount", "(number)" },
						"VotingPlugin.Commands.AdminVote.SetMilestoneCount|" + adminPerm, "Set milestonecount") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						user.setMilestoneCount(Integer.parseInt(args[3]));
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet milestonecount for " + args[1] + " to " + args[3]));
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Vote", "(player)", "(Sitename)" },
				"VotingPlugin.Commands.AdminVote.Vote|" + adminPerm, "Trigger manual vote") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				PlayerVoteEvent voteEvent = new PlayerVoteEvent(plugin.getVoteSite(args[2]), args[1], args[2], false);
				sendMessage(sender, "&cTriggering vote...");
				if (voteEvent.getVoteSite() != null) {
					if (!voteEvent.getVoteSite().isVaidServiceSite()) {
						sendMessage(sender, "&cPossible issue with service site, has the server gotten the vote from "
								+ voteEvent.getServiceSite() + "?");
					}
				}
				plugin.getServer().getPluginManager().callEvent(voteEvent);

				if (plugin.isYmlError()) {
					sendMessage(sender,
							"&4" + plugin.getName() + " v" + plugin.getDescription().getVersion() + " reloaded!");
					sendMessage(sender, "&3Detected yml error, please check server log for details");
				}

			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(Player)", "ForceVote", "(Sitename)" },
						"VotingPlugin.Commands.AdminVote.Vote|" + adminPerm, "Trigger manual vote") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						PlayerVoteEvent voteEvent = new PlayerVoteEvent(plugin.getVoteSite(args[3]), args[1], args[3],
								false);
						sendMessage(sender, "&cTriggering vote...");
						if (voteEvent.getVoteSite() != null) {
							if (!voteEvent.getVoteSite().isVaidServiceSite()) {
								sendMessage(sender,
										"&cPossible issue with service site, has the server gotten the vote from "
												+ voteEvent.getServiceSite() + "?");
							}
						}
						plugin.getServer().getPluginManager().callEvent(voteEvent);

					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "Create" },
				"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Create VoteSite") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				sender.sendMessage(StringParser.getInstance().colorize("&cCreating VoteSite..." + args[1]));

				ConfigVoteSites.getInstance().generateVoteSite(args[1]);
				sender.sendMessage(StringParser.getInstance().colorize("&cCreated VoteSite: &c&l" + args[1]));

			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Config", "TempDebug" },
						"VotingPlugin.Commands.AdminVote.Config.Edit|" + adminPerm,
						"Enable debug, effective until reload/restart") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginMain.plugin.getOptions().setDebug(DebugLevel.INFO);
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "Config", "Update" },
				"VotingPlugin.Commands.AdminVote.Config.Edit|" + adminPerm, "Updates config with missing values") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				ArrayList<String> ignoreSections = new ArrayList<String>();
				ignoreSections.add("VoteReminding");

				File file = new File(plugin.getDataFolder(), "Config.yml");
				try {
					ConfigUpdater.update(plugin, "Config.yml", file, ignoreSections);
				} catch (IOException e) {
					e.printStackTrace();
				}
				config.reloadData();
				config.loadValues();
				sendMessage(sender, "&cUpdated config with new values");
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Config", "TempExtraDebug" },
						"VotingPlugin.Commands.AdminVote.Config.Edit|" + adminPerm,
						"Enable extra debug, effective until reload/restart") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginMain.plugin.getOptions().setDebug(DebugLevel.EXTRA);
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "SetServiceSite", "(string)" },
						"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Set VoteSite SerivceSite") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						String voteSite = plugin.getVoteSiteName(args[1]);
						String serviceSite = args[3];
						ConfigVoteSites.getInstance().setServiceSite(voteSite, serviceSite);
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet ServiceSite to &c&l" + serviceSite + "&c on &c&l" + voteSite));
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "SetVoteURL", "(string)" },
						"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Set VoteSite VoteURL") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						String voteSite = plugin.getVoteSiteName(args[1]);
						String url = args[3];
						ConfigVoteSites.getInstance().setVoteURL(voteSite, url);
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet VoteURL to &c&l" + url + "&c on &c&l" + voteSite));
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "SetPriority", "(number)" },
						"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Set VoteSite Priority") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						String voteSite = plugin.getVoteSiteName(args[1]);
						int value = Integer.parseInt(args[3]);
						ConfigVoteSites.getInstance().setPriority(voteSite, value);
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet priortiy to &c&l" + value + "&c on &c&l" + voteSite));

					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "SetVoteDelay", "(number)" },
						"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Set VoteSite VoteDelay") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						String voteSite = plugin.getVoteSiteName(args[1]);
						int delay = Integer.parseInt(args[3]);
						ConfigVoteSites.getInstance().setVoteDelay(voteSite, delay);
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet VoteDelay to &c&l" + delay + "&c on &c&l" + voteSite));

					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "UpdateCheck" },
				"VotingPlugin.Commands.AdminVote.UpdateCheck|" + adminPerm, "Check for update") {

			@Override
			public void execute(CommandSender sender, String[] args) {

				Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

					@Override
					public void run() {
						sender.sendMessage(StringParser.getInstance().colorize("&cChecking for update..."));
						plugin.setUpdater(new Updater(plugin, 15358, false));
						final Updater.UpdateResult result = plugin.getUpdater().getResult();
						switch (result) {
						case FAIL_SPIGOT: {
							sender.sendMessage(StringParser.getInstance()
									.colorize("&cFailed to check for update for &c&l" + plugin.getName() + "&c!"));
							break;
						}
						case NO_UPDATE: {
							sender.sendMessage(StringParser.getInstance().colorize("&c&l" + plugin.getName()
									+ " &cis up to date! Version: &c&l" + plugin.getUpdater().getVersion()));
							break;
						}
						case UPDATE_AVAILABLE: {
							sender.sendMessage(StringParser.getInstance().colorize(
									"&c&l" + plugin.getName() + " &chas an update available! Your Version: &c&l"
											+ plugin.getDescription().getVersion() + " &cNew Version: &c&l"
											+ plugin.getUpdater().getVersion()));
							break;
						}
						default: {
							break;
						}
						}
					}
				});

			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "SetEnabled", "(boolean)" },
						"VotingPlugin.Commands.AdminVote.VoteSite.Edit|" + adminPerm, "Set VoteSite Enabled") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						String voteSite = plugin.getVoteSiteName(args[1]);
						boolean value = Boolean.parseBoolean(args[3]);

						ConfigVoteSites.getInstance().setEnabled(voteSite, value);
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cSet votesite " + voteSite + " enabled to " + value));

					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "VoteSite", "(sitename)", "Check" },
				"VotingPlugin.Commands.AdminVote.VoteSite.Check|" + adminPerm, "Check to see if VoteSite is valid") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				String siteName = args[1];
				if (!ConfigVoteSites.getInstance().isServiceSiteGood(siteName)) {
					sender.sendMessage(StringParser.getInstance()
							.colorize("&cServiceSite is invalid, votes may not work properly"));
				} else {
					String service = ConfigVoteSites.getInstance().getServiceSite(siteName);
					if (ServerData.getInstance().getServiceSites().contains(service)) {
						sender.sendMessage(StringParser.getInstance().colorize("&aServiceSite is properly setup"));
					} else {
						sender.sendMessage(StringParser.getInstance()
								.colorize("&cService may not be valid, haven't recieved a vote from " + service
										+ ", see /av servicesites"));
					}

				}
				if (!ConfigVoteSites.getInstance().isVoteURLGood(siteName)) {
					sender.sendMessage(StringParser.getInstance().colorize("&cVoteURL is invalid"));
				} else {
					sender.sendMessage(StringParser.getInstance().colorize("&aVoteURL is properly setup"));
				}
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "BackgroundUpdate" },
				"VotingPlugin.Commands.AdminVote.BackgroundUpdate|" + adminPerm, "Force a background update") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				plugin.setUpdate(true);
				plugin.update();
				sender.sendMessage(StringParser.getInstance().colorize("&cUpdating..."));
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "SetVotePartyCount", "(number)" },
				"VotingPlugin.Commands.AdminVote.SetVotePartyCount|" + adminPerm, "Set voteparty count") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				plugin.getVoteParty().setTotalVotes(Integer.parseInt(args[1]));
				sendMessage(sender, "&cSet vote party count to " + args[1]);
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "ClearOfflineVotes" },
				"VotingPlugin.Commands.AdminVote.ClearOfflineVotes|" + adminPerm, "Clear all offline votes") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				for (String uuid : UserManager.getInstance().getAllUUIDs()) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					user.setOfflineVotes(new ArrayList<String>());
				}
				sender.sendMessage(StringParser.getInstance().colorize("&cCleared"));
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Test", "(Player)", "(sitename)", "(number)" },
						"VotingPlugin.Commands.AdminVote.Test|" + adminPerm, "Test voting times") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VoteTester.getInstance().testVotes(Integer.parseInt(args[3]), args[1], args[2]);
						if (isPlayer(sender)) {
							sendMessage(sender, "&cSee console for details");
						}
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "TestReward", "(Player)", "(reward)", "(number)" },
						"VotingPlugin.Commands.AdminVote.TestReward|" + adminPerm, "Test reward times") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VoteTester.getInstance().testRewards(Integer.parseInt(args[3]), args[1], args[2]);
						if (isPlayer(sender)) {
							sendMessage(sender, "&cSee console for details");
						}
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Placeholders" },
						"VotingPlugin.Commands.AdminVote.Placeholders|" + adminPerm,
						"See possible placeholderapi placeholders") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						new AdminVotePlaceholders(plugin, sender).open();
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "ForceVoteParty" },
						"VotingPlugin.Commands.AdminVote.ForceVoteParty|" + adminPerm,
						"Force a voteparty reward, resets vote count") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VoteParty.getInstance().giveRewards();
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "VotePartyExtraRequired", "(Number)" },
						"VotingPlugin.Commands.AdminVote.VoteParty.SetExtraRequired|" + adminPerm,
						"Force a voteparty reward, resets vote count") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						ServerData.getInstance().setVotePartyExtraRequired(Integer.parseInt(args[1]));
						sendMessage(sender, "&cSet VotePartyExtraRequired to " + args[1]);
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "ForceVoteShop", "(VoteShop)" },
						"VotingPlugin.Commands.AdminVote.ForceVoteShop|" + adminPerm, "Force a voteshop reward") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						RewardHandler.getInstance().giveReward(user, Config.getInstance().getData(),
								GUI.getInstance().getChestShopIdentifierRewardsPath(args[3]), new RewardOptions());
						sendMessage(sender, "&cVoteShop " + args[3] + " forced");
					}
				});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "ForceMilestone", "(Number)" },
						"VotingPlugin.Commands.AdminVote.ForceMilestone|" + adminPerm, "Force a milestone") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						SpecialRewards.getInstance().giveMilestoneVoteReward(user, user.isOnline(), parseInt(args[3]));
						sendMessage(sender, "&cMilestone " + args[3] + " forced");
					}
				});

		for (final TopVoter top : TopVoter.valuesMinusAllTime()) {
			plugin.getAdminVoteCommand()
					.add(new CommandHandler(
							new String[] { "User", "(player)", "ForceTopVoter", top.toString(), "(Number)" },
							"VotingPlugin.Commands.AdminVote.ForceTopVoter." + top.toString() + "|" + adminPerm,
							"Force a top voter reward") {

						@Override
						public void execute(CommandSender sender, String[] args) {
							VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
							int place = parseInt(args[4]);
							switch (top) {
							case Daily:
								user.giveDailyTopVoterAward(place, args[4]);
								break;
							case Monthly:
								user.giveMonthlyTopVoterAward(place, args[4]);
								break;
							case Weekly:
								user.giveWeeklyTopVoterAward(place, args[4]);
								break;
							default:
								break;

							}
							sendMessage(sender, "&cTopVoter " + top.toString() + " " + args[4] + " forced");
						}
					});

			String text = "";
			switch (top) {
			case Daily:
				text = "Day";
				break;
			case Monthly:
				text = "Month";
				break;
			case Weekly:
				text = "Week";
				break;
			default:
				break;

			}

			final String str = text;

			plugin.getAdminVoteCommand()
					.add(new CommandHandler(new String[] { "User", "(player)", "ForceVoteStreak", str, "(Number)" },
							"VotingPlugin.Commands.AdminVote.ForceVoteStreak|" + adminPerm,
							"Force a votestreak reward for " + str) {

						@Override
						public void execute(CommandSender sender, String[] args) {
							VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
							SpecialRewards.getInstance().giveVoteStreakReward(user, user.isOnline(), str, args[4],
									parseInt(args[4]));
							sendMessage(sender, "&cVoteStreak " + str + " " + args[4] + " forced");
						}
					});
		}

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "User", "(player)", "ForceCumulative", "(Number)" },
						"VotingPlugin.Commands.AdminVote.ForceCumulative|" + adminPerm, "Force a cumulative reward") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						SpecialRewards.getInstance().giveCumulativeVoteReward(user, user.isOnline(), parseInt(args[3]));
						sendMessage(sender, "&cCumulative " + args[3] + " forced");
					}
				});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "User", "(player)", "ForceAllSites" },
				"VotingPlugin.Commands.AdminVote.ForceAllSites|" + adminPerm, "Force a allsites reward") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
				SpecialRewards.getInstance().giveAllSitesRewards(user, user.isOnline());
				sendMessage(sender, "&cAllSites forced");
			}
		});

		plugin.getAdminVoteCommand().add(new CommandHandler(new String[] { "User", "(player)", "ForceFirstVote" },
				"VotingPlugin.Commands.AdminVote.ForceFirstVote|" + adminPerm, "Force a firstvote reward") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
				SpecialRewards.getInstance().giveFirstVoteRewards(user, user.isOnline());
				sendMessage(sender, "&cFirstVote forced");
			}
		});

		plugin.getAdminVoteCommand()
				.add(new CommandHandler(new String[] { "Placeholders", "(player)" },
						"VotingPlugin.Commands.AdminVote.Placeholders.Players|" + adminPerm,
						"See possible placeholderapi placeholders with player values") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						new AdminVotePlaceholdersPlayer(plugin, sender,
								UserManager.getInstance().getVotingPluginUser(args[1])).open();
						;
					}
				});

		plugin.getAdminVoteCommand().add(
				new CommandHandler(new String[] {}, "VotingPlugin.Commands.AdminVote|" + adminPerm, "Base command") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						sendMessage(sender, "&cInvalid command, see /adminvote help");
					}
				});

		ArrayList<CommandHandler> avCommands = com.bencodez.advancedcore.command.CommandLoader.getInstance()
				.getBasicAdminCommands("VotingPlugin");
		for (CommandHandler cmd : avCommands) {
			cmd.setPerm(cmd.getPerm() + "|" + adminPerm);
		}
		plugin.getAdminVoteCommand().addAll(avCommands);

	}

	/**
	 * Load aliases.
	 */
	public void loadAliases() {
		commands = new HashMap<String, CommandHandler>();
		if (!Config.getInstance().isLoadCommandAliases()) {
			return;
		}
		for (CommandHandler cmdHandle : plugin.getVoteCommand()) {
			if (cmdHandle.getArgs().length > 0) {
				String[] args = cmdHandle.getArgs()[0].split("&");
				for (String arg : args) {
					commands.put("vote" + arg, cmdHandle);
					try {
						plugin.getCommand("vote" + arg).setExecutor(new CommandAliases(cmdHandle, false));

						plugin.getCommand("vote" + arg)
								.setTabCompleter(new AliasesTabCompleter().setCMDHandle(cmdHandle, false));
						for (String str : plugin.getCommand("vote" + arg).getAliases()) {
							commands.put(str, cmdHandle);
						}
					} catch (Exception ex) {
						plugin.devDebug("Failed to load command and tab completer for /vote" + arg);
					}
				}

			}
		}

		for (CommandHandler cmdHandle : plugin.getAdminVoteCommand()) {
			if (cmdHandle.getArgs().length > 0) {
				String[] args = cmdHandle.getArgs()[0].split("&");
				for (String arg : args) {
					commands.put("adminvote" + arg, cmdHandle);
					try {
						plugin.getCommand("adminvote" + arg).setExecutor(new CommandAliases(cmdHandle, true));

						plugin.getCommand("adminvote" + arg)
								.setTabCompleter(new AliasesTabCompleter().setCMDHandle(cmdHandle, true));
						for (String str : plugin.getCommand("adminvote" + arg).getAliases()) {
							commands.put(str, cmdHandle);
						}
					} catch (Exception ex) {
						plugin.devDebug("Failed to load command and tab completer for /adminvote" + arg + ": "
								+ ex.getMessage());

					}
				}

			}
		}
	}

	/**
	 * Load commands.
	 */
	public void loadCommands() {
		loadAdminVoteCommand();
		loadVoteCommand();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

			@Override
			public void run() {
				com.bencodez.advancedcore.thread.Thread.getInstance().run(new Runnable() {

					@Override
					public void run() {
						loadTabComplete();

						UserGUI.getInstance().addPluginButton(plugin,
								new BInventoryButton("Force Vote", new String[] {}, new ItemStack(Material.STONE)) {

									@Override
									public void onClick(ClickEvent clickEvent) {
										Player player = clickEvent.getPlayer();
										ArrayList<String> voteSites = new ArrayList<String>();
										for (VoteSite voteSite : plugin.getVoteSites()) {
											voteSites.add(voteSite.getKey());
										}
										new ValueRequest().requestString(player, "",
												ArrayUtils.getInstance().convert(voteSites), true,
												new StringListener() {

													@Override
													public void onInput(Player player, String value) {
														PlayerVoteEvent voteEvent = new PlayerVoteEvent(
																plugin.getVoteSite(value),
																UserGUI.getInstance().getCurrentPlayer(player),
																plugin.getVoteSiteServiceSite(value), false);
														plugin.getServer().getPluginManager().callEvent(voteEvent);

														player.sendMessage("Forced vote for "
																+ UserGUI.getInstance().getCurrentPlayer(player)
																+ " on " + value);
													}
												});

									}
								});

						UserGUI.getInstance().addPluginButton(plugin,
								new BInventoryButton("MileStones", new String[0], new ItemStack(Material.STONE)) {

									@Override
									public void onClick(ClickEvent event) {

										Player player = event.getWhoClicked();
										String playerName = (String) event.getMeta(player, "Player");
										BInventory inv = new BInventory("MileStones: " + playerName);
										for (String mileStoneName : SpecialRewardsConfig.getInstance()
												.getMilestoneVotes()) {
											if (StringParser.getInstance().isInt(mileStoneName)) {
												int mileStone = Integer.parseInt(mileStoneName);

												inv.addButton(inv.getNextSlot(),
														new BInventoryButton("" + mileStone, new String[] {
																"Enabled: " + SpecialRewardsConfig.getInstance()
																		.getMilestoneRewardEnabled(mileStone),
																"&cClick to set wether this has been completed or not" },
																new ItemStack(Material.STONE)) {

															@Override
															public void onClick(ClickEvent clickEvent) {
																if (StringParser.getInstance()
																		.isInt(clickEvent.getClickedItem().getItemMeta()
																				.getDisplayName())) {
																	Player player = clickEvent.getPlayer();
																	int mileStone = Integer
																			.parseInt(clickEvent.getClickedItem()
																					.getItemMeta().getDisplayName());
																	String playerName = (String) event.getMeta(player,
																			"Player");
																	VotingPluginUser user = UserManager.getInstance()
																			.getVotingPluginUser(playerName);
																	new ValueRequest().requestBoolean(player,
																			"" + user.hasGottenMilestone(mileStone),
																			new BooleanListener() {

																				@Override
																				public void onInput(Player player,
																						boolean value) {
																					String playerName = UserGUI
																							.getInstance()
																							.getCurrentPlayer(player);
																					VotingPluginUser user = UserManager
																							.getInstance()
																							.getVotingPluginUser(
																									playerName);
																					user.setHasGotteMilestone(mileStone,
																							value);
																					player.sendMessage(
																							"Set milestone completetion to "
																									+ value + " on "
																									+ mileStone);

																				}
																			});
																}
															}
														});
											}
										}
									}
								});

					}
				});
			}

		});

	}

	/**
	 * Load tab complete.
	 */
	public void loadTabComplete() {
		ArrayList<String> sites = new ArrayList<String>();
		for (VoteSite site : plugin.getVoteSites()) {
			sites.add(site.getKey());
		}

		TabCompleteHandler.getInstance().addTabCompleteOption(new TabCompleteHandle("(Sitename)", sites) {

			@Override
			public void reload() {
				ArrayList<String> sites = new ArrayList<String>();
				for (VoteSite site : plugin.getVoteSites()) {
					sites.add(site.getKey());
				}
				setReplace(sites);
			}

			@Override
			public void updateReplacements() {
			}
		});

		TabCompleteHandler.getInstance().addTabCompleteOption(new TabCompleteHandle("(VoteShop)", sites) {

			@Override
			public void reload() {
				ArrayList<String> sites = new ArrayList<String>();
				for (String str : GUI.getInstance().getChestShopIdentifiers()) {
					sites.add(str);
				}
				setReplace(sites);
			}

			@Override
			public void updateReplacements() {
			}
		});
	}

	/**
	 * Load vote command.
	 */
	private void loadVoteCommand() {
		plugin.setVoteCommand(new ArrayList<CommandHandler>());
		if (Config.getInstance().isUsePrimaryAccountForPlaceholders()) {
			plugin.getVoteCommand().add(new CommandHandler(new String[] { "SetPrimaryAccount", "(player)" },
					"VotingPlugin.Commands.Vote.SetPrimaryAccount|" + modPerm, "Set primary account", false) {

				@Override
				public void execute(CommandSender sender, String[] args) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(sender.getName());
					if (args[1].equals("none")) {
						user.setPrimaryAccount(null);
						sendMessage(sender, "&cRemoved primary account");
					} else {
						try {
							user.setPrimaryAccount(
									java.util.UUID.fromString(PlayerUtils.getInstance().getUUID(args[1])));
							sendMessage(sender, "&cPrimary account set");
						} catch (Exception e) {
							e.printStackTrace();
							sendMessage(sender, "Failed to set primary account: " + e.getMessage());
						}
					}
				}
			});
		}
		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Help&?" },
				"VotingPlugin.Commands.Vote.Help|" + playerPerm, "View help page") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteHelp(plugin, sender, 1).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Help&?", "(number)" },
				"VotingPlugin.Commands.Vote.Help|" + playerPerm, "View help page") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteHelp(plugin, sender, Integer.parseInt(args[1])).open();
			}
		});

		if (GUI.getInstance().getChestVoteShopEnabled()) {
			plugin.getVoteCommand().add(new CommandHandler(new String[] { "Shop" },
					"VotingPlugin.Commands.Vote.Shop|" + playerPerm, "Open VoteShop GUI", false) {

				@Override
				public void execute(CommandSender sender, String[] args) {
					new VoteShop(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
				}
			});
			plugin.getVoteCommand().add(new CommandHandler(new String[] { "Shop", "(Text)" },
					"VotingPlugin.Commands.Vote.Shop|" + playerPerm, "Open VoteShop GUI", false) {

				@Override
				public void execute(CommandSender sender, String[] args) {
					if (!GUI.getInstance().getChestVoteShopEnabled()) {
						sender.sendMessage(StringParser.getInstance().colorize("&cVote shop disabled"));
						return;
					}

					String identifier = args[1];
					Set<String> identifiers = GUI.getInstance().getChestShopIdentifiers();
					if (ArrayUtils.getInstance().containsIgnoreCase(identifiers, identifier)) {
						for (String ident : identifiers) {
							if (ident.equalsIgnoreCase(args[1])) {
								identifier = ident;
							}
						}

						String perm = GUI.getInstance().getChestVoteShopPermission(identifier);
						boolean hasPerm = false;
						if (perm.isEmpty()) {
							hasPerm = true;
						} else {
							hasPerm = sender.hasPermission(perm);
						}

						int limit = GUI.getInstance().getChestShopIdentifierLimit(identifier);

						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(sender.getName());
						boolean limitPass = true;
						if (limit > 0) {

							if (user.getVoteShopIdentifierLimit(identifier) >= limit) {
								limitPass = false;
							}
						}

						if (!GUI.getInstance().getChestVoteShopNotBuyable(identifier)) {
							if (hasPerm) {
								user.clearCache();
								int points = GUI.getInstance().getChestShopIdentifierCost(identifier);
								if (identifier != null) {

									if (limitPass) {
										HashMap<String, String> placeholders = new HashMap<String, String>();
										placeholders.put("identifier", identifier);
										placeholders.put("points", "" + points);
										placeholders.put("limit", "" + limit);
										if (user.removePoints(points)) {

											RewardHandler.getInstance().giveReward(user, Config.getInstance().getData(),
													GUI.getInstance().getChestShopIdentifierRewardsPath(identifier),
													new RewardOptions().setPlaceholders(placeholders));

											user.sendMessage(StringParser.getInstance().replacePlaceHolder(
													Config.getInstance().getFormatShopPurchaseMsg(), placeholders));
											if (limit > 0) {
												user.setVoteShopIdentifierLimit(identifier,
														user.getVoteShopIdentifierLimit(identifier) + 1);
											}
										} else {
											user.sendMessage(StringParser.getInstance().replacePlaceHolder(
													Config.getInstance().getFormatShopFailedMsg(), placeholders));
										}
									} else {
										user.sendMessage(GUI.getInstance().getChestVoteShopLimitReached());
									}
								}

							}
						}
					} else {
						sendMessage(sender, "&cWrong voteshop item");
					}
				}
			});
		}

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "URL" },
				"VotingPlugin.Commands.Vote.URL|" + playerPerm, "Open VoteURL GUI", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteURL(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender), true)
						.open();

			}
		});
		plugin.getVoteCommand().add(new CommandHandler(new String[] { "URL", "(SiteName)" },
				"VotingPlugin.Commands.Vote.URL.VoteSite", "Open VoteURL GUI for VoteSite", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteURLVoteSite(plugin, (Player) sender,
						UserManager.getInstance().getVotingPluginUser((Player) sender), args[1]).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Last", "(player)" },
				"VotingPlugin.Commands.Vote.Last.Other|" + modPerm, "See other players last votes") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteLast(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}

			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Last" },
				"VotingPlugin.Commands.Vote.Last|" + playerPerm, "See your last votes", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteLast(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand()
				.add(new CommandHandler(new String[] { "ToggleBroadcast" },
						"VotingPlugin.Commands.Vote.ToggleBroadcast|" + playerPerm,
						"Toggle whether or not you will recieve vote broadcasts", false) {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser((Player) sender);
						boolean value = !user.getDisableBroadcast();
						user.setDisableBroadcast(value);
						if (!value) {
							sendMessage(sender, Config.getInstance().getFormatCommandsVoteToggleBroadcastEnabled());
						} else {
							sendMessage(sender, Config.getInstance().getFormatCommandsVoteToggleBroadcastDisabled());
						}
					}
				});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Next", "(player)" },
				"VotingPlugin.Commands.Vote.Next.Other|" + modPerm, "See other players next votes") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteNext(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Points", "(player)" },
				"VotingPlugin.Commands.Vote.Points.Other|" + modPerm, "View pints of other player") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
					String msg = Config.getInstance().getFormatCommandVotePoints()
							.replace("%Player%", user.getPlayerName()).replace("%Points%", "" + user.getPoints());
					if (sender instanceof Player) {
						UserManager.getInstance().getVotingPluginUser((Player) sender).sendMessage(msg);
					} else {
						sender.sendMessage(StringParser.getInstance().colorize(msg));
					}
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}

			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Points" },
				"VotingPlugin.Commands.Vote.Points|" + playerPerm, "View your points", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				VotingPluginUser user = UserManager.getInstance().getVotingPluginUser((Player) sender);
				String msg = Config.getInstance().getFormatCommandVotePoints().replace("%Player%", user.getPlayerName())
						.replace("%Points%", "" + user.getPoints());
				user.sendMessage(msg);
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Next" },
				"VotingPlugin.Commands.Vote.Next|" + playerPerm, "See your next votes", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteNext(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "GUI" },
				"VotingPlugin.Commands.Vote.GUI|" + playerPerm, "Open VoteGUI", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteGUI(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "GUI", "(player)" },
				"VotingPlugin.Commands.Vote.GUI.Other|" + modPerm, "Open VoteGUI", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteGUI(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}

			}
		});

		if (GUI.getInstance().isLastMonthGUI()) {
			plugin.getVoteCommand()
					.add(new CommandHandler(new String[] { "LastMonthTop" },
							"VotingPlugin.Commands.Vote.LastMonthTop|" + playerPerm,
							"Open list of Top Voters from last month") {

						@Override
						public void execute(CommandSender sender, String[] args) {
							new VoteTopVoterLastMonth(plugin, sender,
									UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
						}
					});
		}

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top" },
				"VotingPlugin.Commands.Vote.Top|" + playerPerm, "Open list of Top Voters") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteTopVoter(plugin, sender, null, TopVoter.getDefault(), 1)
						.open(GUIMethod.valueOf(GUI.getInstance().getGuiMethodTopVoter().toUpperCase()));
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top", "(number)" },
				"VotingPlugin.Commands.Vote.Top|" + playerPerm, "Open page of Top Voters") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);

				new VoteTopVoter(plugin, sender, null, TopVoter.getDefault(), page).open(GUIMethod.CHAT);
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top", "(number)", "Monthly" },
				"VotingPlugin.Commands.Vote.Top.Monthly|" + playerPerm, "Open page of Top Voters") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new VoteTopVoter(plugin, sender, null, TopVoter.Monthly, page).open(GUIMethod.CHAT);
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top", "(number)", "All" },
				"VotingPlugin.Commands.Vote.Top.All|" + playerPerm, "Open page of Top Voters All Time") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new VoteTopVoter(plugin, sender, null, TopVoter.AllTime, page).open(GUIMethod.CHAT);
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top", "(number)", "Weekly" },
				"VotingPlugin.Commands.Vote.Top.Weekly|" + playerPerm, "Open page of Top Voters") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new VoteTopVoter(plugin, sender, null, TopVoter.Weekly, page).open(GUIMethod.CHAT);

			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Top", "(number)", "Daily" },
				"VotingPlugin.Commands.Vote.Top.Daily|" + playerPerm, "Open page of Top Voters") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new VoteTopVoter(plugin, sender, null, TopVoter.Daily, page).open(GUIMethod.CHAT);
			}
		});

		plugin.getVoteCommand()
				.add(new CommandHandler(new String[] { "Party" }, "VotingPlugin.Commands.Vote.Party|" + playerPerm,
						"View current amount of votes and how many more needed") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VoteParty.getInstance().commandVoteParty(sender);
					}
				});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Today", "(number)" },
				"VotingPlugin.Commands.Vote.Today|" + playerPerm, "Open page of who Voted Today") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				int page = Integer.parseInt(args[1]);
				new VoteToday(plugin, sender, null, page).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Today" },
				"VotingPlugin.Commands.Vote.Today|" + playerPerm, "View who list of who voted today") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteToday(plugin, sender, null, 1).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Total", "All" },
				"VotingPlugin.Commands.Vote.Total.All|" + playerPerm, "View server total votes") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				ArrayList<String> msg = new ArrayList<String>();

				ArrayList<String> uuids = UserManager.getInstance().getAllUUIDs();

				int daily = 0;
				int weekly = 0;
				int month = 0;
				int all = 0;

				for (String uuid : uuids) {
					VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(new UUID(uuid));
					daily += user.getTotal(TopVoter.Daily);
					weekly += user.getTotal(TopVoter.Weekly);
					month += user.getTotal(TopVoter.Monthly);
					all += user.getTotal(TopVoter.AllTime);
				}

				for (String s : config.getFormatCommandsVoteTotalAll()) {
					String str = StringParser.getInstance().replaceIgnoreCase(s, "%DailyTotal%", "" + daily);
					str = StringParser.getInstance().replaceIgnoreCase(str, "%WeeklyTotal%", "" + weekly);
					str = StringParser.getInstance().replaceIgnoreCase(str, "%MonthlyTotal%", "" + month);
					str = StringParser.getInstance().replaceIgnoreCase(str, "%AllTimeTotal%", "" + all);
					msg.add(str);
				}

				msg = ArrayUtils.getInstance().colorize(msg);
				sendMessage(sender, msg);

			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Total", "(player)" },
				"VotingPlugin.Commands.Vote.Total.Other|" + modPerm, "View other players total votes") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteTotal(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}

			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Total" },
				"VotingPlugin.Commands.Vote.Total|" + playerPerm, "View your total votes", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteTotal(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Best" },
				"VotingPlugin.Commands.Vote.Best|" + playerPerm, "View your best voting", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteBest(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Best", "(player)" },
				"VotingPlugin.Commands.Vote.Best.Other|" + modPerm, "View someone's best voting") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteBest(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Streak" },
				"VotingPlugin.Commands.Vote.Streak|" + playerPerm, "View your voting streak", false) {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteStreak(plugin, sender, UserManager.getInstance().getVotingPluginUser((Player) sender)).open();
			}
		});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "Streak", "(player)" },
				"VotingPlugin.Commands.Vote.Streak.Other|" + modPerm, "View someone's voting streak") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				if (com.bencodez.advancedcore.api.user.UserManager.getInstance().userExist(args[1])) {
					new VoteStreak(plugin, sender, UserManager.getInstance().getVotingPluginUser(args[1])).open();
				} else {
					sendMessage(sender, StringParser.getInstance()
							.replacePlaceHolder(Config.getInstance().getFormatUserNotExist(), "player", args[1]));
				}
			}
		});

		if (Config.getInstance().isAllowVotePointTransfers()) {
			plugin.getVoteCommand().add(new CommandHandler(new String[] { "GivePoints", "(player)", "(number)" },
					"VotingPlugin.Commands.Vote.GivePoints", "Give someone points from your points", false) {

				@Override
				public void execute(CommandSender sender, String[] args) {
					if (Config.getInstance().isAllowVotePointTransfers()) {
						VotingPluginUser cPlayer = UserManager.getInstance().getVotingPluginUser((Player) sender);

						VotingPluginUser user = UserManager.getInstance().getVotingPluginUser(args[1]);
						int pointsToGive = Integer.parseInt(args[2]);
						if (pointsToGive > 0) {
							if (cPlayer.getPoints() >= pointsToGive) {
								user.addPoints(pointsToGive);
								cPlayer.removePoints(pointsToGive);
								sendMessage(sender, "&c" + pointsToGive + " points given to " + user.getPlayerName());
								user.sendMessage(
										"&cYou received " + pointsToGive + " points from " + cPlayer.getPlayerName());
							} else {
								sendMessage(sender, "&cNot enough points");
							}
						} else {
							sendMessage(sender, "&cNumber of points needs to be greater than 0");
						}
					}
				}
			});
		}

		plugin.getVoteCommand().add(
				new CommandHandler(new String[] {}, "VotingPlugin.Commands.Vote|" + playerPerm, "See voting URLs") {

					@Override
					public void execute(CommandSender sender, String[] args) {
						VotingPluginUser user = null;
						if (sender instanceof Player) {
							user = UserManager.getInstance().getVotingPluginUser((Player) sender);
						}
						new VoteURL(plugin, sender, user, true).open();
					}
				});

		plugin.getVoteCommand().add(new CommandHandler(new String[] { "List&All" },
				"VotingPlugin.Commands.Vote.List|" + playerPerm, "See voting URLs") {

			@Override
			public void execute(CommandSender sender, String[] args) {
				new VoteURL(plugin, sender, null, true).open();
			}
		});

		ArrayList<CommandHandler> avCommands = com.bencodez.advancedcore.command.CommandLoader.getInstance()
				.getBasicCommands("VotingPlugin");
		for (CommandHandler cmd : avCommands) {
			cmd.setPerm(cmd.getPerm() + "|" + playerPerm);
		}
		plugin.getVoteCommand().addAll(avCommands);

		ArrayList<CommandHandler> list = plugin.getVoteCommand();
		ArrayList<String> disabledCommands = Config.getInstance().getDisabledCommands();
		for (int i = list.size() - 1; i >= 0; i--) {
			boolean remove = false;
			for (String disabled : disabledCommands) {
				CommandHandler handle = list.get(i);
				if (handle.getPerm().contains(disabled)) {
					remove = true;
				}
			}

			if (remove) {
				plugin.debug("Disabling: " + ArrayUtils.getInstance()
						.makeStringList(ArrayUtils.getInstance().convert(list.get(i).getArgs())));
				list.remove(i);
			}
		}
		ArrayList<String> disabledDefaultPerms = config.getDisabledDefaultPermissions();
		for (CommandHandler cmd : list) {
			boolean contains = false;
			for (String dis : disabledDefaultPerms) {
				if (cmd.getPerm().contains(dis + "|")) {
					contains = true;
				}
			}
			if (contains) {
				cmd.setPerm(cmd.getPerm().replace("|" + playerPerm, ""));
				plugin.debug("Disabling VotingPlugin.Player permission on " + cmd.getPerm());

			}
		}
	}

	/**
	 * Sets the commands.
	 *
	 * @param commands the commands
	 */
	public void setCommands(HashMap<String, CommandHandler> commands) {
		this.commands = commands;
	}

	public BInventoryButton getBackButton(VotingPluginUser user) {
		ConfigurationSection sec = GUI.getInstance().getCHESTBackButton();
		ItemBuilder item;
		if (sec != null) {
			item = new ItemBuilder(sec);
		} else {
			item = new ItemBuilder(Material.BARRIER, 1).setName("&8Back to VoteGUI");
		}

		BInventoryButton b = new BInventoryButton(item) {

			@Override
			public void onClick(ClickEvent event) {
				new VoteGUI(plugin, event.getWhoClicked(), user)
						.open(GUIMethod.valueOf(GUI.getInstance().getGuiMethodGUI().toUpperCase()));
			}
		};

		if (!Config.getInstance().isAlwaysCloseInventory()) {
			b.dontClose();
		}

		return b;
	}
}
