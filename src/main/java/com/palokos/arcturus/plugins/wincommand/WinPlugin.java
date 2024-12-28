package com.palokos.arcturus.plugins.wincommand;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import gnu.trove.map.hash.THashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WinPlugin extends HabboPlugin implements EventListener {
    
    private static final String PERMISSION = "cmd_win";
    private static final String TABLE_NAME = "hotel_win_options";
    private static final String LOG_TABLE = "hotel_win_logs";
    
    @Override
    public void onEnable() {
        Emulator.getPluginManager().registerEvents(this);
        Emulator.getGameEnvironment().getCommandHandler().addCommand(new WinCommand());
        createTables();
        Emulator.getLogging().logStart("Win Plugin -> Erfolgreich geladen!");
    }
    
    @Override
    public void onDisable() {
        Emulator.getPluginManager().unregisterEvents(this);
        Emulator.getLogging().logShutdown("Win Plugin -> Deaktiviert!");
    }
    
    private void createTables() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            String createWinOptionsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "win_type ENUM('credits', 'pixels', 'points', 'diamonds', 'badge', 'furniture') NOT NULL,"
                + "win_amount INT NOT NULL,"
                + "win_value VARCHAR(255)," // Für Badges oder Furniture IDs
                + "win_description VARCHAR(255) NOT NULL"
                + ")";
            
            String createWinLogsTable = "CREATE TABLE IF NOT EXISTS " + LOG_TABLE + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "staff_username VARCHAR(255) NOT NULL,"
                + "winner_username VARCHAR(255) NOT NULL,"
                + "win_type VARCHAR(255) NOT NULL,"
                + "win_amount INT NOT NULL,"
                + "win_value VARCHAR(255)"
                + ")";
            
            connection.prepareStatement(createWinOptionsTable).execute();
            connection.prepareStatement(createWinLogsTable).execute();
        } catch (Exception e) {
            Emulator.getLogging().logErrorLine(e);
        }
    }

    public class WinCommand extends Command {
        public WinCommand() {
            super(PERMISSION, ":win <username>");
        }
        
        @Override
        public boolean handle(GameClient gameClient, String[] params) {
            if (params.length < 2) {
                gameClient.getHabbo().whisper("Syntax: :win <username>");
                return true;
            }
            
            String targetUsername = params[1];
            Habbo target = Emulator.getGameServer().getGameClientManager().getHabbo(targetUsername);
            
            if (target == null) {
                gameClient.getHabbo().whisper("User " + targetUsername + " nicht gefunden!");
                return true;
            }
            
            List<WinOption> winOptions = getWinOptions();
            if (winOptions.size() < 3) {
                gameClient.getHabbo().whisper("Es müssen mindestens 3 Win-Optionen in der Datenbank definiert sein!");
                return true;
            }
            
            sendWinSelectionDialog(target, winOptions, gameClient.getHabbo());
            
            return true;
        }
    }
    
    private class WinOption {
        String type;
        int amount;
        String value;
        String description;
        
        public WinOption(String type, int amount, String value, String description) {
            this.type = type;
            this.amount = amount;
            this.value = value;
            this.description = description;
        }
    }
    
    private List<WinOption> getWinOptions() {
        List<WinOption> options = new ArrayList<>();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT win_type, win_amount, win_value, win_description FROM " + TABLE_NAME
            );
            ResultSet set = statement.executeQuery();
            
            while (set.next()) {
                options.add(new WinOption(
                    set.getString("win_type"),
                    set.getInt("win_amount"),
                    set.getString("win_value"),
                    set.getString("win_description")
                ));
            }
        } catch (Exception e) {
            Emulator.getLogging().logErrorLine(e);
        }
        return options;
    }
    
    private void sendWinSelectionDialog(Habbo winner, List<WinOption> options, Habbo staff) {
        THashMap<String, String> keys = new THashMap<>();
        keys.put("display", "BUBBLE");
        keys.put("title", "${win.selection.title}");
        keys.put("message", "${win.selection.message}");
        
        for (int i = 0; i < 3; i++) {
            WinOption option = options.get(i);
            keys.put("button" + (i + 1), option.description);
            keys.put("event" + (i + 1), "win:select:" + i + ":" + winner.getHabboInfo().getId() + ":" 
                + staff.getHabboInfo().getId() + ":" + option.type + ":" + option.amount + ":" + option.value);
        }
        
        winner.getClient().sendResponse(new BubbleAlertComposer("win_selection", keys));
    }
    
    public void handleWinSelection(Habbo winner, String selectedType, int amount, String value, Habbo staff) {
        switch (selectedType.toLowerCase()) {
            case "credits":
                winner.getHabboInfo().addCredits(amount);
                break;
            case "pixels":
                winner.getHabboInfo().addPixels(amount);
                break;
            case "points":
                winner.getHabboInfo().addPoints(amount);
                break;
            case "diamonds":
                winner.getHabboInfo().addDiamonds(amount);
                break;
            case "badge":
                if (value != null && !value.isEmpty()) {
                    winner.getInventory().getBadgesComponent().addBadge(value);
                }
                break;
            case "furniture":
                if (value != null && !value.isEmpty()) {
                    try {
                        int furniId = Integer.parseInt(value);
                        Emulator.getGameEnvironment().getItemManager().createItem(
                            winner.getHabboInfo().getId(), 
                            furniId, 
                            0, 
                            0, 
                            ""
                        );
                    } catch (NumberFormatException e) {
                        Emulator.getLogging().logErrorLine(e);
                    }
                }
                break;
        }
        
        // Log erstellen
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + LOG_TABLE + " (staff_username, winner_username, win_type, win_amount, win_value) " +
                "VALUES (?, ?, ?, ?, ?)"
            );
            statement.setString(1, staff.getHabboInfo().getUsername());
            statement.setString(2, winner.getHabboInfo().getUsername());
            statement.setString(3, selectedType);
            statement.setInt(4, amount);
            statement.setString(5, value);
            statement.execute();
        } catch (Exception e) {
            Emulator.getLogging().logErrorLine(e);
        }
        
        winner.getHabboStats().eventPoints++;
        
        winner.whisper("Du hast deine Belohnung erhalten und einen Eventpunkt bekommen!");
        staff.whisper(winner.getHabboInfo().getUsername() + " hat seine Belohnung erhalten.");
    }

    @Override
    public boolean hasPermission(Habbo habbo, String s) {
        return false;
    }
}
