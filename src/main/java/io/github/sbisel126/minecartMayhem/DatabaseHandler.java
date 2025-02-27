package io.github.sbisel126.minecartMayhem;

// for logging to terminal
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

// awesome documentation of java SQL https://docs.oracle.com/javase/tutorial/jdbc/basics/processingsqlstatements.html
import java.sql.*;

// begin class
public class DatabaseHandler {
    ComponentLogger logger;
    public DatabaseHandler(ComponentLogger logger){
        logger.info(Component.text("We connecting to the db"));
        connect();
        this.logger = logger;
        logger.info(Component.text("We running db checks"));
        DatabaseIntegrityCheck();
        logger.info(Component.text("DB ready!"));
    }
    private Connection dbConnection;
    private Boolean connected = false;
    // initializes a connection to the database
    private void connect(){
        var url = "jdbc:sqlite:./MM.db";

        if (connected) {
            return;
        }

        try {
            dbConnection = DriverManager.getConnection(url);
            if (dbConnection != null){
                connected = true;
            }
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    // DatabaseIntegrityCheck verifies that the required tables for the game mode exist and are ready to be read/written to
    // should be run during plugin instantiation.
    private void DatabaseIntegrityCheck() {
        if (!connected) {
            connect();
        }

        if (!doesTableExist("Players")) {
            logger.info(Component.text("Players table does not exist, creating..."));
            createPlayersTable();
        }

        if (!doesTableExist("Maps")) {
            logger.info(Component.text("Maps table does not exist, creating..."));
            createMapsTable();
        }

        if (!doesTableExist("TopScores")) {
            logger.info(Component.text("TopScores table does not exist, creating..."));
            createTopScoresTable();
        }
    }

    // helper function for DatabaseIntegrityCheck that checks for the presence of a table
    private boolean doesTableExist(String tableName) {
        String query = "SELECT count(name) FROM sqlite_master WHERE type='table' AND name=?";

        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // if getInt returns the number of tables matching the name we provide
                // we want 1 table, anything else means something unexpected has occurred.
                return rs.getInt(1) == 1;
            }
        } catch (SQLException e) {
            logger.error(Component.text("Error checking table existence: " + e.getMessage()));
            e.printStackTrace();
        }
        return false;
    }

    //table creation functions
    private void createPlayersTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE Players (player_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, UUID TEXT);");
        } catch (SQLException e) {
            //TODO Handle error
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }

    private void createMapsTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE Maps (map_id INTEGER PRIMARY KEY AUTOINCREMENT, map_name TEXT);");
        } catch (SQLException e) {
            //TODO Handle error
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }

    private void createTopScoresTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE TopScores (score_id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, player_id INTEGER, top_score INTEGER, FOREIGN KEY(map_id) REFERENCES Maps(map_id), FOREIGN KEY(player_id) REFERENCES Players(player_id));");
        } catch (SQLException e) {
            //TODO Handle error
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }
}
