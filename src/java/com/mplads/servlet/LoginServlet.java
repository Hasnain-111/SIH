package com.mplads.servlet;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.sql.*;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://mysql-d1e32da-elhanyasir81-f053.g.aivencloud.com:13489/MPLADs?ssl-mode=REQUIRED";

    private static final String USER =
            "avnadmin";

    private static final String PASSWORD =
            "AVNS_DszyK_UqyhVhh_T2zX2";


    @Override
    protected void doPost(HttpServletRequest req,
                           HttpServletResponse res)
            throws IOException, ServletException {

       
        String email =
                req.getParameter("email");

        String password =
                req.getParameter("password");


       
        if (email == null || email.isEmpty() ||
            password == null || password.isEmpty()) {

            res.sendRedirect(
                    req.getContextPath() + "/login.html"
            );

            return;
        }


        try {

           
            Class.forName(
                    "com.mysql.cj.jdbc.Driver"
            );


            
            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USER,
                            PASSWORD
                    );


            
            Statement stmt =
                    con.createStatement();


            
            String sql =
                    "SELECT * FROM official_users " +
                    "WHERE official_email = '" +
                    email + "' " +
                    "AND password_hash = '" +
                    password + "'";


            ResultSet rs =
                    stmt.executeQuery(sql);


            if (rs.next()) {

               
                HttpSession session =
                        req.getSession();

                session.setAttribute(
                        "officialEmail",
                        email
                );

                session.setAttribute(
                        "officialName",
                        rs.getString("full_name")
                );

                session.setAttribute(
                        "officialId",
                        rs.getString("official_id")
                );


                // Redirect to dashboard
                res.sendRedirect(
                        req.getContextPath() +
                        "/dashboard.html"
                );


            } else {

                
                res.setContentType("text/html");

                PrintWriter out =
                        res.getWriter();

                out.println(
                    "<script>" +
                    "alert('Invalid email or password');" +
                    "window.location='login.html';" +
                    "</script>"
                );
            }


            rs.close();
            stmt.close();
            con.close();


        } catch (Exception e) {

            e.printStackTrace();

            res.setContentType("text/html");

            PrintWriter out =
                    res.getWriter();

            out.println("<h1>Login Error</h1>");

            out.println(
                    "<p>" +
                    e.getClass().getName() +
                    "</p>"
            );

            out.println(
                    "<p>" +
                    e.getMessage() +
                    "</p>"
            );
        }
    }
}