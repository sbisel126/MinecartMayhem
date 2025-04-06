package io.github.sbisel126.minecartMayhem;

// for logging to terminal
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.entity.Player;

// awesome documentation of java SQL https://docs.oracle.com/javase/tutorial/jdbc/basics/processingsqlstatements.html
import java.io.File;
import java.sql.*;

// begin class
public class DatabaseHandler {
    ComponentLogger logger;
    private Connection dbConnection;
    private Boolean connected = false;
    private final MinecartMayhem instance;
    public DatabaseHandler(MinecartMayhem mm, ComponentLogger logger){
        this.logger = logger;
        this.instance = mm;

        logger.info(Component.text("We connecting to the db"));
        connect();
        logger.info(Component.text("We running db checks"));
        databaseIntegrityCheck();
        logger.info(Component.text("DB ready!"));
    }

    // initializes a connection to the database
    private void connect(){
        if (!instance.getDataFolder().exists()) {
            instance.getDataFolder().mkdirs();
        }

        File dbFile = new File(instance.getDataFolder(), "MM.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

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
    private void databaseIntegrityCheck() {
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
        }
        return false;
    }

    //table creation functions
    private void createPlayersTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE Players (player_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, UUID TEXT);");
        } catch (SQLException e) {
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }

    private void createMapsTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE Maps (map_id INTEGER PRIMARY KEY AUTOINCREMENT, map_name TEXT);");
            statement.execute("INSERT INTO Maps (map_name) VALUES ('Grass');");
            statement.execute("INSERT INTO Maps (map_name) VALUES ('Sand');");
        } catch (SQLException e) {
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }

    private void createTopScoresTable() {
        try(Statement statement = dbConnection.createStatement()) {
            statement.execute("CREATE TABLE TopScores (score_id INTEGER PRIMARY KEY AUTOINCREMENT, map_id INTEGER, player_id INTEGER, top_score INTEGER, FOREIGN KEY(map_id) REFERENCES Maps(map_id), FOREIGN KEY(player_id) REFERENCES Players(player_id));");
        } catch (SQLException e) {
            logger.error(Component.text("Error creating table: " + e.getMessage()));
        }
    }

    // Given a map and a Username, returns a Users top score on a map.
    // returns -1 if user or map not found
    public int GetUserTopScore(String map, String username) {
        // get the ID's to search for
        int MapID = GetMapID(map);
        int PlayerID = GetPlayerID(username);

        // if the map or player isn't found, early return.
        if (MapID == -1 || PlayerID == -1) {
            return -1;
        }
        // add ID's to base query
        String query = "SELECT top_score FROM TopScores WHERE map_id=? AND player_id=?";
        // run the query
        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setInt(1, MapID);
            statement.setInt(2, PlayerID);
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                // the top score should be the only column returned if it exists.
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error(Component.text("Error getting user top score from map: " + e.getMessage()));
        }
        // return -1 in the event that the DB is unable to find a user's top score on a map.
        return -1;
    }

    // Converts map_name String to mapID Integer
    // returns -1 if map not found
    public int GetMapID(String map_name) {
        String query = "SELECT map_id FROM Maps WHERE map_name=%s";
        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, map_name);
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e){
            logger.error(Component.text("Error getting map id: " + e.getMessage()));
        }
        return -1;
    }
    // Converts Username String to PlayerID Integer
    // returns -1 if map not found
    public int GetPlayerID(String username) {
        String query = "SELECT player_id FROM Players WHERE username = ?";
        try (PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e){
            logger.error(Component.text("Error getting player id: " + e.getMessage()));
        }
        return -1;
    }

    // triggered when user joins the server
    // checks if user is in database already, if not
    // breaks down a user object and inserts it into the DB
    public void InsertUser(Player user) {
        // First, check if the user exists within the database
        String userCheckQuery = "SELECT count(player_id) FROM Players WHERE username=?";
        try (PreparedStatement checkStatement = dbConnection.prepareStatement(userCheckQuery)) {
            checkStatement.setString(1, user.getName());
            ResultSet rs = checkStatement.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) == 1) {
                    // if row exists, we exit the Insert process. Otherwise, we go to the next block and insert the user
                    return;
                }
            }
        } catch (SQLException e) {
            logger.error(Component.text("Error when checking Players: " + e.getMessage()));
        }

        String UserUUID = String.valueOf(user.getUniqueId());
        String query = "INSERT INTO Players (username, uuid) VALUES (?, ?);";
        // we use this format of createStatement, execute, as we do not expect a return value from the DB.
        try(PreparedStatement statement = dbConnection.prepareStatement(query)) {
            statement.setString(1, user.getName());
            statement.setString(2, UserUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            // Is this enough for error handling? I don't think so.
            logger.error(Component.text("Error inserting user: " + e.getMessage()));
        }
    }
}
