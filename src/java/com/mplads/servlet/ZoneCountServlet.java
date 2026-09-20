package com.mplads.servlet;

import com.mplads.dao.ZoneDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * GET /api/zone-counts
 * Returns {"high":N,"medium":N,"low":N} from mplads_ml_results.risk_level
 * (Red Zone = High, Yellow Zone = Medium, Green Zone = Low).
 */
@WebServlet("/api/zone-counts")
public class ZoneCountServlet extends HttpServlet {

    private ZoneDAO dao = new ZoneDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Map<String, Integer> c = dao.getZoneCounts();
            response.getWriter().print("{\"high\":" + c.get("high")
                    + ",\"medium\":" + c.get("medium")
                    + ",\"low\":" + c.get("low") + "}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"Could not load zone counts\"}");
        }
    }
}