package com.mplads.servlet;

import com.mplads.DBConnection;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/registration")
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException, ServletException {

        // =====================================================
        // GET FORM DATA
        // =====================================================

        String name = req.getParameter("fullName");
        String id = req.getParameter("officialId");
        String email = req.getParameter("officialEmail");
        String designation = req.getParameter("designation");
        String department = req.getParameter("department");
        String password = req.getParameter("password");

        // =====================================================
        // CHECK EMPTY VALUES
        // =====================================================

        if (name == null || name.trim().isEmpty()
                || id == null || id.trim().isEmpty()
                || email == null || email.trim().isEmpty()
                || designation == null || designation.trim().isEmpty()
                || department == null || department.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {

            res.sendRedirect(
                    req.getContextPath() + "/signup.html"
            );

            return;
        }

        // Remove unnecessary spaces
        name = name.trim();
        id = id.trim();
        email = email.trim();
        designation = designation.trim();
        department = department.trim();

        // =====================================================
        // SQL QUERY
        // =====================================================

        String sql =
                "INSERT INTO official_users " +
                "(full_name, official_id, official_email, " +
                "designation, department, password_hash) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        // =====================================================
        // DATABASE CONNECTION
        // =====================================================

        try (
                Connection con = DBConnection.getConnection();
                PreparedStatement stmt = con.prepareStatement(sql)
        ) {

            // -------------------------------------------------
            // SET VALUES
            // -------------------------------------------------

            stmt.setString(1, name);
            stmt.setString(2, id);
            stmt.setString(3, email);
            stmt.setString(4, designation);
            stmt.setString(5, department);
            stmt.setString(6, password);

            // -------------------------------------------------
            // EXECUTE INSERT
            // -------------------------------------------------

            int result = stmt.executeUpdate();

            // =================================================
            // REGISTRATION SUCCESS
            // =================================================

            if (result > 0) {

                res.sendRedirect(
                        req.getContextPath()
                        + "/login.html"
                );

            } else {

                // Registration failed
                res.sendRedirect(
                        req.getContextPath()
                        + "/signup.html"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();

            res.setContentType("text/html");

            PrintWriter out = res.getWriter();

            out.println("<h1>Registration Error</h1>");

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