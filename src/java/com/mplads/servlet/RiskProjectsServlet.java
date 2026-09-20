package com.mplads.servlet;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/api/risk-projects")
public class RiskProjectsServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String level = request.getParameter("level");

        if (level == null) {
            response.getWriter().print(
                "{\"message\":\"RiskProjectsServlet is working\"}"
            );
            return;
        }

        response.getWriter().print(
            "{\"level\":\"" + level + "\"}"
        );
    }
}