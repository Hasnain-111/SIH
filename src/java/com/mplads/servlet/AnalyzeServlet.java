package com.mplads.servlet;

import com.mplads.dao.ProjectDAO;
import com.mplads.dao.RiskDAO;
import com.mplads.model.Project;
import com.mplads.service.MLService;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/analyze")
public class AnalyzeServlet extends HttpServlet {

    private ProjectDAO projectDAO = new ProjectDAO();
    private MLService mlService = new MLService();
    private RiskDAO riskDAO = new RiskDAO(); // Added RiskDAO

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String id = request.getParameter("id");

        if (id == null || id.isEmpty()) {
            response.setStatus(400);
            response.getWriter().print("{\"error\":\"Project ID required\"}");
            return;
        }

        Project project = projectDAO.getProjectById(id);

        if (project == null) {
            response.setStatus(404);
            response.getWriter().print("{\"error\":\"Project not found\"}");
            return;
        }

        try {
            // 1. Call the Python ML Engine
            String jsonResult = mlService.analyzeProject(project);

            // 2. Parse the JSON result from Python (Basic manual parsing)
            // Assuming Python returns: {"risk_score": 85.5, "risk_level": "High", "anomaly_score": 0.9, "model_version": "v1.0"}
            double riskScore = Double.parseDouble(extractJsonValue(jsonResult, "risk_score"));
            String riskLevel = extractJsonString(jsonResult, "risk_level");
            double anomalyScore = Double.parseDouble(extractJsonValue(jsonResult, "anomaly_score"));
            String modelVersion = extractJsonString(jsonResult, "model_version");

            // 3. Save into MySQL using your existing RiskDAO!
            riskDAO.saveRisk(id, riskScore, riskLevel, anomalyScore, modelVersion);

            // 4. Send the result back to the Dashboard UI
            response.getWriter().print(jsonResult);

        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().print("{\"error\":\"ML service error or DB save failed\"}");
            e.printStackTrace();
        }
    }

    // --- Helper methods to extract values from JSON string without extra libraries ---
    
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "0";
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return json.substring(start, end).trim().replaceAll("\"", "");
    }
    
    private String extractJsonString(String json, String key) {
        return extractJsonValue(json, key);
    }
}