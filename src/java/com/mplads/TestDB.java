package com.mplads;

//import com.mplads.DBConnection;
import java.sql.Connection;
public class TestDB {

    public static void main(String[] args) {

        try {

            Connection con = DBConnection.getConnection();

            System.out.println("Database Connected ELHAN Successfully!");

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
