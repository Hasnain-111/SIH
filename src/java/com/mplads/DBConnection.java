package com.mplads;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String HOST = System.getenv("DB_HOST");
    private static final String PORT = System.getenv("DB_PORT");
    private static final String DATABASE = System.getenv("DB_NAME");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
            + "?sslMode=REQUIRED"
            + "&connectTimeout=10000"
            + "&socketTimeout=15000";

    public static Connection getConnection() throws Exception {

        System.out.println("========== DB DEBUG ==========");
        System.out.println("DB_HOST = " + HOST);
        System.out.println("DB_PORT = " + PORT);
        System.out.println("DB_NAME = " + DATABASE);
        System.out.println("DB_USER = " + USER);
        System.out.println("DB_PASSWORD configured = "
                + (PASSWORD != null && !PASSWORD.isEmpty()));

        System.out.println("Loading MySQL driver...");

        Class.forName("com.mysql.cj.jdbc.Driver");

        System.out.println("MySQL driver loaded.");
        System.out.println("Attempting database connection...");

        Connection con = DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );

        System.out.println("========== DATABASE CONNECTED ==========");

        return con;
    }
}