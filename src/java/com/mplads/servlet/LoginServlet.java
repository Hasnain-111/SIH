package com.mplads.servlet;

import com.mplads.DBConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // =====================================================
    // FIXED ADMIN CREDENTIALS
    // =====================================================

    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin123";

    // =====================================================
    // POST LOGIN
    // =====================================================

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException, ServletException {

        // -------------------------------------------------
        // GET EMAIL AND PASSWORD
        // -------------------------------------------------

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        // -------------------------------------------------
        // CHECK EMPTY VALUES
        // -------------------------------------------------

        if (email == null || email.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {

            res.sendRedirect(
                    req.getContextPath() + "/login.html"
            );

            return;
        }

        email = email.trim();

        // =================================================
        // ADMIN LOGIN
        // =================================================

        if (email.equals(ADMIN_EMAIL)
                && password.equals(ADMIN_PASSWORD)) {

            HttpSession session = req.getSession();

            session.setAttribute(
                    "admin",
                    email
            );

            session.setAttribute(
                    "userType",
                    "ADMIN"
            );

            res.sendRedirect(
                    req.getContextPath()
                    + "/admin-dashboard.html"
            );

            return;
        }

        // =================================================
        // NORMAL OFFICIAL LOGIN
        // =================================================

        String sql =
                "SELECT * FROM official_users " +
                "WHERE official_email = ? " +
                "AND password_hash = ?";

        try {

            // -------------------------------------------------
            // CONNECT TO AIVEN MYSQL THROUGH DBConnection
            // -------------------------------------------------

            Connection con = DBConnection.getConnection();

            // -------------------------------------------------
            // PREPARED STATEMENT
            // -------------------------------------------------

            PreparedStatement stmt =
                    con.prepareStatement(sql);

            stmt.setString(
                    1,
                    email
            );

            stmt.setString(
                    2,
                    password
            );

            // -------------------------------------------------
            // EXECUTE QUERY
            // -------------------------------------------------

            ResultSet rs =
                    stmt.executeQuery();

            // =================================================
            // OFFICIAL LOGIN SUCCESS
            // =================================================

            if (rs.next()) {

                HttpSession session =
                        req.getSession();

                session.setAttribute(
                        "officialEmail",
                        rs.getString("official_email")
                );

                session.setAttribute(
                        "officialName",
                        rs.getString("full_name")
                );

                session.setAttribute(
                        "officialId",
                        rs.getString("official_id")
                );

                session.setAttribute(
                        "userType",
                        "OFFICIAL"
                );

                // -------------------------------------------------
                // REDIRECT TO OFFICIAL DASHBOARD
                // -------------------------------------------------

                res.sendRedirect(
                        req.getContextPath()
                        + "/dashboard.html"
                );

            } else {

                // -------------------------------------------------
                // INVALID OFFICIAL LOGIN
                // -------------------------------------------------

                res.setContentType("text/html");

                res.getWriter().println(
                        "<script>" +
                        "alert('Invalid email or password');" +
                        "window.location='" +
                        req.getContextPath() +
                        "/login.html';" +
                        "</script>"
                );
            }

            // -------------------------------------------------
            // CLOSE DATABASE RESOURCES
            // -------------------------------------------------

            rs.close();
            stmt.close();
            con.close();

        } catch (Exception e) {

            // -------------------------------------------------
            // DATABASE ERROR
            // -------------------------------------------------

            e.printStackTrace();

            res.setContentType("text/html");

            res.getWriter().println(
                    "<script>" +
                    "alert('Database connection error');" +
                    "window.location='" +
                    req.getContextPath() +
                    "/login.html';" +
                    "</script>"
            );
        }
    }
}