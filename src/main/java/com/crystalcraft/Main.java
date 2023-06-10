package com.crystalcraft;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Main {
    public static Database database;
    public static JDA bot;

    public static void main(String[] args) {
        long cTime = System.currentTimeMillis();
        JDABuilder builder = JDABuilder.createDefault("");

        builder.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGES);

        builder.addEventListeners(new Listeners());

        bot = builder.build();

        setupCommands(bot);

        database = new Database();

        database.init();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                long cTime = System.currentTimeMillis();
                cooldowns.endAll();
                try {
                    bot.awaitShutdown();
                    keepAlive.stop();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
                database.terminate();
                System.out.println("shutdown completed! (completed in " + (System.currentTimeMillis() - cTime) + " ms)");
            }
        });

        Thread keepAliveThread = new Thread() {
            @Override
            public void run() {
                try {
                    keepAlive.keepAlive(8080);
                } catch (IOException e) {
                    if (e instanceof SocketException) return;
                    throw new RuntimeException(e);
                }
            }
        };

        try {
            bot.awaitReady();
            keepAliveThread.start();
            System.out.println("bot started (started in " + (System.currentTimeMillis() - cTime) + " ms)");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void setupCommands(JDA jda) {
        // creating commands
        SlashCommandData gamble = createCommand("gamble", "gamble your life away", false);
        gamble.addOption(OptionType.INTEGER, "amount", "amount of your life to gamble away", true);

        SlashCommandData work = createCommand("work", "get your life together", false);

        SlashCommandData beg = createCommand("beg", "if you cant work your life together why not beg it together", false);

        SlashCommandData daily = createCommand("daily", "hey look free money", false);
        SlashCommandData weekly = createCommand("weekly", "hey look even more money", false);
        SlashCommandData monthly = createCommand("monthly", "why do i keep finding money", false);

        SlashCommandData balance = createCommand("balance", "lets see how much i have", false);

        SlashCommandData transfer = createCommand("transfer", "lets give some away", false);
        transfer.addOption(OptionType.USER, "user", "the lucky guy",true);
        transfer.addOption(OptionType.INTEGER, "amount", "amount of cash", true);

        // adding commands
        jda.updateCommands().addCommands(
                gamble,
                work,
                beg,
                daily,
                weekly,
                monthly,
                balance,
                transfer
        ).queue();
    }

    static SlashCommandData createCommand(String name, String desc, boolean isStaff) {
        SlashCommandData commandData = Commands.slash(name, desc);

        if (isStaff)
            commandData.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));

        return commandData;
    }
}

class Listeners extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        System.out.println("bot ready");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        System.out.println("[cmd] " + event.getMember().getEffectiveName() + ": " + event.getName() + " " + event.getOptions());
        switch (event.getName()) {
            case "gamble" -> commands.gamble(event);
            case "work" -> commands.work(event);
            case "beg" -> commands.beg(event);
            case "daily" -> commands.daily(event);
            case "weekly" -> commands.weekly(event);
            case "monthly" -> commands.monthly(event);
            case "balance" -> commands.balance(event);
            case "transfer" -> commands.transfer(event);
        }
    }
}

class commands {
    public static void gamble(SlashCommandInteractionEvent event) {
        int amount = event.getOption("amount").getAsInt();
        Member member = event.getMember();

        if (!Main.database.canBet(member.getIdLong(), amount)) {
            event.reply("you cannot bet this much").setEphemeral(true).queue();
            return;
        }

        int random = new Random().nextInt(1,100);

        System.out.println(member.getEffectiveName() + " rolled " + random + " on gamble");

        if (random >= settings.GAMBLE_win) {
            Main.database.giveCash(member.getIdLong(),amount);
            event.reply("you win! you have received " + amount).queue();
        } else {
            Main.database.giveCash(member.getIdLong(),-amount);
            event.reply("you lose!").queue();
        }
    }

    public static void work(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        int amount = new Random().nextInt(settings.WORK_min, settings.WORK_max);

        System.out.println(member.getEffectiveName() + " worked for " + amount);
        Main.database.giveCash(member.getIdLong(), amount);
    }

    public static void beg(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        int randAmount = new Random().nextInt(1,100);

        System.out.println(member.getEffectiveName() + " rolled " + randAmount + " on beg");

        if (randAmount >= settings.BEG_WIN) {
            int gotAmount = new Random().nextInt(settings.BEG_min, settings.BEG_max);
            Main.database.giveCash(member.getIdLong(), gotAmount);
            System.out.println(member.getEffectiveName() + " got " + gotAmount + " from beg");
            event.reply("you got lucky and got " + gotAmount).setEphemeral(true).queue();
        } else {
            event.reply("go work yourself broke ass").setEphemeral(true).queue();
        }
    }

    public static void daily(SlashCommandInteractionEvent event) {
        if (!cooldowns.onDailyCooldown(event.getMember().getIdLong())) {
            event.reply("you have received " + settings.DAILY_recieve + ". you may redeem it again in 24 hours").setEphemeral(true).queue();
            Main.database.giveCash(event.getMember().getIdLong(), settings.DAILY_recieve);
        } else {
            event.reply("you are currently on cooldown. (but because of bad design i don't know how long").setEphemeral(true).queue();
        }
    }

    public static void weekly(SlashCommandInteractionEvent event) {
        if (!cooldowns.onWeeklyCooldown(event.getMember().getIdLong())) {
            event.reply("you have received " + settings.WEEKLY_recieve + ". you may redeem it again in 7 days").setEphemeral(true).queue();
            Main.database.giveCash(event.getMember().getIdLong(), settings.WEEKLY_recieve);
        } else {
            event.reply("you are currently on cooldown. (but because of bad design i don't know how long").setEphemeral(true).queue();
        }
    }

    public static void monthly(SlashCommandInteractionEvent event) {
        if (!cooldowns.onMonthlyCooldown(event.getMember().getIdLong())) {
            event.reply("you have received " + settings.MONTHLY_recieve + ". you may redeem it again in 30 days").setEphemeral(true).queue();
            Main.database.giveCash(event.getMember().getIdLong(), settings.MONTHLY_recieve);
        } else {
            event.reply("you are currently on cooldown. (but because of bad design i don't know how long").setEphemeral(true).queue();
        }
    }

    public static void balance(SlashCommandInteractionEvent event) {
        event.reply("you have " + Main.database.getUser(event.getMember().getIdLong()).money).setEphemeral(true).queue();
    }

    public static void transfer(SlashCommandInteractionEvent event) {
        Member memberA = event.getMember();
        Member memberB = event.getGuild().getMemberById(event.getOption("user").getAsUser().getIdLong());
        int amount = event.getOption("amount").getAsInt();

        if (Main.database.canBet(memberA.getIdLong(), amount)) {
            Main.database.giveCash(memberA, -amount);
            Main.database.giveCash(memberB, amount);
            memberB.getUser().openPrivateChannel().queue((channel) -> {
                channel.sendMessage(memberA.getAsMention() + " transferred " + amount + " to you!").queue();
            });
        } else {
            event.reply("you can't transfer this much").setEphemeral(true).queue();
        }
    }
}

class settings {
    // general settings
    public static final long SEVER_ID = 1116295943860523051L;

    // gamble settings
    public static final int GAMBLE_win = 51;
    public static final long GAMBLE_CHANNEL_logs = 1117118282579836928L;

    // money settings
    public static final int WORK_min = 100;
    public static final int WORK_max = 1000;
    public static final int BEG_WIN = 61;
    public static final int BEG_min = 50;
    public static final int BEG_max = 100;

    // daily/weekly/monthly settings
    public static final int DAILY_recieve = 250;
    public static final int WEEKLY_recieve = 1000;
    public static final int MONTHLY_recieve = 2500;
}

class cooldowns {
    public static HashMap<Long, Thread> dailyCooldowns = new HashMap<>();
    public static HashMap<Long, Thread> weeklyCooldowns = new HashMap<>();
    public static HashMap<Long, Thread> monthlyCooldowns = new HashMap<>();

    public static boolean onDailyCooldown(long id) {
        return dailyCooldowns.containsKey(id);
    }

    public static boolean onWeeklyCooldown(long id) {
        return weeklyCooldowns.containsKey(id);
    }

    public static boolean onMonthlyCooldown(long id) {
        return monthlyCooldowns.containsKey(id);
    }

    public static void setDailyCooldowns(long id) {
        Thread newThread = new Thread() {
            @Override
            public void run() {
                dailyCooldowns.put(id, this);
                try {
                    TimeUnit.DAYS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                dailyCooldowns.remove(id);
            }
        };

        newThread.start();
    }

    public static void setWeeklyCooldowns(long id) {
        Thread newThread = new Thread() {
            @Override
            public void run() {
                weeklyCooldowns.put(id, this);
                try {
                    TimeUnit.DAYS.sleep(7);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                weeklyCooldowns.remove(id);
            }
        };

        newThread.start();
    }

    public static void setMonthlyCooldowns(long id) {
        Thread newThread = new Thread() {
            @Override
            public void run() {
                monthlyCooldowns.put(id, this);
                try {
                    TimeUnit.DAYS.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                monthlyCooldowns.remove(id);
            }
        };

        newThread.start();
    }

    public static void endAll() {
        for (long id : dailyCooldowns.keySet()) {
            dailyCooldowns.get(id).stop();
        }

        for (long id : weeklyCooldowns.keySet()) {
            weeklyCooldowns.get(id).stop();
        }

        for (long id : monthlyCooldowns.keySet()) {
            monthlyCooldowns.get(id).stop();
        }
    }
}