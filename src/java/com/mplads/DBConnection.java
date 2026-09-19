package com.mplads;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://mysql-d1e32da-elhanyasir81-f053.g.aivencloud.com:13489/MPLADs?ssl-mode=REQUIRED";

    private static final String USER = "avnadmin";

    private static final String PASSWORD = "AVNS_DszyK_UqyhVhh_T2zX2";

    public static Connection getConnection() throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }
}