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
            // Win-Optionen Tabelle
            String createWinOptionsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "win_option VARCHAR(255) NOT NULL"
                + ")";
            
            // Win-Logs Tabelle
            String createWinLogsTable = "CREATE TABLE IF NOT EXISTS " + LOG_TABLE + " ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "staff_username VARCHAR(255) NOT NULL,"
                + "winner_username VARCHAR(255) NOT NULL,"
                + "selected_win VARCHAR(255) NOT NULL"
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
            
            List<String> winOptions = getWinOptions();
            if (winOptions.size() < 3) {
                gameClient.getHabbo().whisper("Es müssen mindestens 3 Win-Optionen in der Datenbank definiert sein!");
                return true;
            }
            
            sendWinSelectionDialog(target, winOptions, gameClient.getHabbo());
            
            return true;
        }
    }
    
    private List<String> getWinOptions() {
        List<String> options = new ArrayList<>();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT win_option FROM " + TABLE_NAME);
            ResultSet set = statement.executeQuery();
            
            while (set.next()) {
                options.add(set.getString("win_option"));
            }
        } catch (Exception e) {
            Emulator.getLogging().logErrorLine(e);
        }
        return options;
    }
    
    private void sendWinSelectionDialog(Habbo winner, List<String> options, Habbo staff) {
        THashMap<String, String> keys = new THashMap<>();
        keys.put("display", "BUBBLE");
        keys.put("title", "${win.selection.title}");
        keys.put("message", "${win.selection.message}");
        
        for (int i = 0; i < 3; i++) {
            keys.put("button" + (i + 1), options.get(i));
            keys.put("event" + (i + 1), "win:select:" + i + ":" + winner.getHabboInfo().getId() + ":" + staff.getHabboInfo().getId());
        }
        
        winner.getClient().sendResponse(new BubbleAlertComposer("win_selection", keys));
    }
    
    public void handleWinSelection(Habbo winner, String selectedWin, Habbo staff) {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + LOG_TABLE + " (staff_username, winner_username, selected_win) VALUES (?, ?, ?)"
            );
            statement.setString(1, staff.getHabboInfo().getUsername());
            statement.setString(2, winner.getHabboInfo().getUsername());
            statement.setString(3, selectedWin);
            statement.execute();
        } catch (Exception e) {
            Emulator.getLogging().logErrorLine(e);
        }
        
        winner.getHabboStats().eventPoints++;
        
        // Benachrichtige User
        winner.whisper("Du hast " + selectedWin + " als Gewinn gewählt und einen Eventpunkt erhalten!");
        staff.whisper(winner.getHabboInfo().getUsername() + " hat " + selectedWin + " als Gewinn gewählt.");
    }

    @Override
    public boolean hasPermission(Habbo habbo, String s) {
        return false;
    }
}
