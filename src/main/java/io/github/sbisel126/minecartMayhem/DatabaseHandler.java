package io.github.sbisel126.minecartMayhem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHandler {
    public DatabaseHandler(){

    }

    public Connection connect() {
        // connection string
        var url = "jdbc:sqlite:./MM.db";

        try (var conn = DriverManager.getConnection(url)) {
            if (conn != null){
                System.out.println("Connection to SQLite has been established.");
                return conn;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
