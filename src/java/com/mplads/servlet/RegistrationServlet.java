package com.mplads.servlet;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.sql.*;

@WebServlet("/registration")
public class RegistrationServlet extends HttpServlet {

    private static final String URL =
            "jdbc:mysql://mysql-d1e32da-elhanyasir81-f053.g.aivencloud.com:13489/MPLADs?ssl-mode=REQUIRED";

    private static final String USER =
            "avnadmin";

    private static final String PASSWORD =
            "AVNS_DszyK_UqyhVhh_T2zX2";


   
    protected void doPost(HttpServletRequest req,
                           HttpServletResponse res)
            throws IOException, ServletException {

        
        String name = req.getParameter("fullName");
        String id = req.getParameter("officialId");
        String email = req.getParameter("officialEmail");
        String designation = req.getParameter("designation");
        String department = req.getParameter("department");
        String password = req.getParameter("password");


        
        if (name == null || name.isEmpty() ||
            id == null || id.isEmpty() ||
            email == null || email.isEmpty() ||
            designation == null || designation.isEmpty() ||
            department == null || department.isEmpty() ||
            password == null || password.isEmpty()) {

            res.sendRedirect(
                    req.getContextPath() + "/signup.html"
            );

            return;
        }


        try {

           
            Class.forName("com.mysql.cj.jdbc.Driver");


            
            Connection con =
                    DriverManager.getConnection(
                            URL,
                            USER,
                            PASSWORD
                    );


           
            Statement stmt =
                    con.createStatement();


            
            String sql =
                    "INSERT INTO official_users " +
                    "(full_name, official_id, official_email, " +
                    "designation, department, password_hash) " +
                    "VALUES ('" +
                    name + "', '" +
                    id + "', '" +
                    email + "', '" +
                    designation + "', '" +
                    department + "', '" +
                    password + "')";


            
            int result =
                    stmt.executeUpdate(sql);


           
            stmt.close();
            con.close();


            
            if (result > 0) {

                res.sendRedirect(
                        req.getContextPath() +
                        "/login.html"
                );

            } else {

               
                res.sendRedirect(
                        req.getContextPath() +
                        "/signup.html"
                );
            }


        } catch (Exception e) {

            e.printStackTrace();

            res.setContentType("text/html");

            PrintWriter out =
                    res.getWriter();

            out.println("<h1>Registration Error</h1>");
            out.println("<p>" +
                    e.getClass().getName() +
                    "</p>");
            out.println("<p>" +
                    e.getMessage() +
                    "</p>");
        }
    }
}