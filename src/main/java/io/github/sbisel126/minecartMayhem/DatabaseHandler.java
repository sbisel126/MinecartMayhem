package io.github.sbisel126.minecartMayhem;

import java.sql.*;

public class DatabaseHandler {
    public DatabaseHandler(){
        connect();
    }
    private Connection dbConnection;
    private Boolean connected = false;
    // initializes a connection to the database
    public void connect(){
        // connection string
        var url = "jdbc:sqlite:./MM.db";

        // is the connection already made? don't do anything.
        if (connected) {
            return;
        }
        // if we do not have a connection, let's make one instead.
        try (var conn = DriverManager.getConnection(url)){
            if (conn != null){
                // we got a connection! let's update the class variables and get out of here!
                connected = true;
                dbConnection = conn;
            }
        } catch (SQLException e){
            // something didn't go to plan, and we have an error.
            // TODO implement better error handling
            System.out.println(e.getMessage());
        }
    }

    // DatabaseIntegrityCheck verifies that the required tables for the game mode exist and are ready to be read/written to
    // should be run during plugin instantiation.
    public Boolean DatabaseIntegrityCheck(){
        // if we don't have a database connection yet, lets go get one.
        if (!connected) {
            connect();
        }
        // now that we have a connection to the DB, let's run some queries to see if the tables already exist.
        try (Statement checkPlayersTable = dbConnection.createStatement()) {
            ResultSet rs = checkPlayersTable.executeQuery("SELECT count(name) FROM sqlite_master WHERE type='table' AND name='Players'");
            System.out.println(rs.next());
        } catch (SQLException e) {
            // TODO implement better error handling
            throw new RuntimeException(e);
        }
        //here we run a series of SQL command to ensure the tables have been created. If they do not exist, we make them.
        return true;
    }
}
