package com.crystalcraft;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class Database {
    public File dir = new File("users");

    public ArrayList<User> users = new ArrayList<>();

    boolean initialized;

    public void init() {
        System.out.println("initializing database");
        long cTime = System.currentTimeMillis();

        if (!dir.exists())
            dir.mkdirs();

        if (dir.listFiles() != null) {
            if (dir.listFiles().length != 0) {
                for (File userFile : dir.listFiles()) {
                    users.add(new User(userFile));
                }
            }
        }

        initialized = true;
        System.out.println("database finished initializing (initialized in " + (System.currentTimeMillis() - cTime) + " ms)");
    }

    public void terminate() {
        System.out.println("terminating database");
        long cTime = System.currentTimeMillis();

        if (!users.isEmpty()) {
            for (User u : users) {
                FileWriter writer = null;
                try {
                    writer = new FileWriter(dir.getAbsolutePath() + "/" + u.id + ".json");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (writer == null)
                    continue;
                JSONObject jsonObject = new JSONObject();

                jsonObject.put("id", u.id);
                jsonObject.put("money", u.money);
                jsonObject.put("tud", u.timeUntilDaily);
                jsonObject.put("tuw", u.timeUntilWeekly);
                jsonObject.put("tum", u.timeUntilMonthly);

                try {
                    writer.write(jsonObject.toJSONString());
                    writer.flush();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("database finished terminating (terminated in " + (System.currentTimeMillis() - cTime) + " ms)");
    }

    public User getUser(net.dv8tion.jda.api.entities.User user) {
        return getUser(user.getIdLong());
    }

    public User getUser(Member member) {
        return getUser(member.getIdLong());
    }

    public User getUser(long id) {
        if (users == null) {
            users = new ArrayList<>();
        }

        if (users.size() != 0) {
            for (User u : users) {
                if (u.id == id)
                    return u;
            }
        }

        User u = new User(id, 100,0,0,0);
        addUser(u);
        return u;
    }

    public void addUser(User u) {
        if (users == null) users = new ArrayList<>();
        users.add(u);
    }

    public boolean canBet(net.dv8tion.jda.api.entities.User user, int amount) {
        return canBet(user.getIdLong(), amount);
    }

    public boolean canBet(Member member, int amount) {
        return canBet(member.getIdLong(), amount);
    }

    public boolean canBet(long id, int amount) {
        return (getUser(id).money >= amount && amount >= 0);
    }

    public void giveCash(net.dv8tion.jda.api.entities.User user, int amount) {
        giveCash(user.getIdLong(), amount);
    }

    public void giveCash(Member member, int amount) {
        giveCash(member.getIdLong(), amount);
    }

    public void giveCash(long id, int amount) {
        getUser(id).money += amount;
        System.out.println("gave " + id + " " + amount + " they now have " + getUser(id).money);


        Guild guild = Main.bot.getGuildById(settings.SEVER_ID);
        if (guild == null) return;
        TextChannel channel = guild.getTextChannelById(settings.GAMBLE_CHANNEL_logs);
        if (channel == null) return;

        Member member = guild.getMemberById(id);

        EmbedBuilder builder = new EmbedBuilder();

        builder.setAuthor(member.getEffectiveName(), member.getAvatarUrl());
        builder.setTitle("Balance updated");
        builder.setColor(Color.YELLOW);
        builder.setDescription(member.getEffectiveName() + "'s balance has been updated\nthey now have " + getUser(id).money);

        channel.sendMessageEmbeds(builder.build()).queue();

    }
}

class User {

    public long id;
    public int money;
    public long timeUntilDaily;
    public long timeUntilWeekly;
    public long timeUntilMonthly;


    public User(long id, int money, long timeUntilDaily, long timeUntilWeekly, long timeUntilMonthly) {
        this.id = id;
        this.money = money;
        this.timeUntilDaily = timeUntilDaily;
        this.timeUntilWeekly = timeUntilWeekly;
        this.timeUntilMonthly = timeUntilMonthly;
    }

    public User(File userFile) {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(userFile.getAbsolutePath())){
            Object obj = parser.parse(reader);
            JSONObject jsonObj = (JSONObject) obj;

            long id = (long) jsonObj.get("id");
            int money = Math.toIntExact((long) jsonObj.get("money"));
            long timeUntilDaily = 0L;
            long timeUntilWeekly = 0L;
            long timeUntilMonthly = 0L;

            if (jsonObj.get("tud") != null)
                timeUntilDaily = (long) jsonObj.get("tud");
            if (jsonObj.get("tuw") != null)
                timeUntilWeekly = (long) jsonObj.get("tuw");
            if (jsonObj.get("tum") != null)
                timeUntilMonthly = (long) jsonObj.get("tum");

            this.id = id;
            this.money = money;
            this.timeUntilDaily = timeUntilDaily;
            this.timeUntilWeekly = timeUntilWeekly;
            this.timeUntilMonthly = timeUntilMonthly;
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("id:").append(id);
        sb.append(", money:").append(money);
        sb.append(", tud:").append(timeUntilDaily);
        sb.append(", tuw:").append(timeUntilWeekly);
        sb.append(", tum:").append(timeUntilMonthly);
        sb.append('}');
        return sb.toString();
    }
}
