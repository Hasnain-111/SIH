package com.mplads.servlet;

import com.mplads.dao.ProjectDAO;
import com.mplads.model.Project;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/projects")
public class ProjectServlet extends HttpServlet {

    private ProjectDAO dao = new ProjectDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<Project> projects = dao.getAllProjects();
        PrintWriter out = response.getWriter();
        out.print("[");

        for (int i = 0; i < projects.size(); i++) {
            Project p = projects.get(i);
            out.print("{");
            out.print("\"project_id\":" + p.getProjectId() + ",");
            // Escape quotes inside strings to prevent JSON breaking
            out.print("\"mp_name\":\"" + escapeJson(p.getMpName()) + "\",");
            out.print("\"work_\":\"" + escapeJson(p.getWork()) + "\",");
            out.print("\"state\":\"" + escapeJson(p.getState()) + "\",");
            out.print("\"constituency\":\"" + escapeJson(p.getConstituency()) + "\",");
            out.print("\"ida\":\"" + escapeJson(p.getIda()) + "\",");
            out.print("\"Date_\":\"" + escapeJson(p.getDate()) + "\",");
            out.print("\"allocation_amount\":" + p.getAllocationAmount() + ",");
            out.print("\"ida_approval\":\"" + escapeJson(p.getIdaApproval()) + "\",");
            out.print("\"project_status\":\"" + escapeJson(p.getProjectStatus()) + "\",");
            out.print("\"house\":\"" + escapeJson(p.getHouse()) + "\"");
            out.print("}");

            if (i < projects.size() - 1) {
                out.print(",");
            }
        }
        out.print("]");
    }

    private String escapeJson(String data) {
        if (data == null) return "";
        return data.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
