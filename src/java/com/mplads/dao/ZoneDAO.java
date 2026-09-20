package com.mplads.dao;

import com.mplads.DBConnection;
import com.mplads.model.Project;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads zone projects from the ML results table (mplads_ml_results).
 * Red = High risk, Yellow = Medium risk, Green = Low risk.
 */
public class ZoneDAO {

    // Preferred query: ML rows + project_id / approval looked up from `projects`
    // (matched on MP name + work + allocation amount; lowest project_id is used).
    private static final String SQL_WITH_LOOKUP =
            "SELECT m.mp_name, m.work, m.house, m.state, m.constituency, "
          + "       m.recommended_date, m.allocation_amount, m.status, "
          + "       k.project_id AS pid, p.ida_approval AS approval "
          + "FROM mplads_ml_results m "
          + "LEFT JOIN (SELECT mp_name, work_, allocation_amount, MIN(project_id) AS project_id "
          + "           FROM projects GROUP BY mp_name, work_, allocation_amount) k "
          + "  ON k.mp_name = m.mp_name AND k.work_ = m.work "
          + " AND k.allocation_amount = m.allocation_amount "
          + "LEFT JOIN projects p ON p.project_id = k.project_id "
          + "WHERE LOWER(TRIM(m.risk_level)) = ? "
          + "ORDER BY m.id";

    // Fallback if the lookup query cannot run: ML columns only
    private static final String SQL_PLAIN =
            "SELECT m.mp_name, m.work, m.house, m.state, m.constituency, "
          + "       m.recommended_date, m.allocation_amount, m.status, "
          + "       0 AS pid, '' AS approval "
          + "FROM mplads_ml_results m "
          + "WHERE LOWER(TRIM(m.risk_level)) = ? "
          + "ORDER BY m.id";

    /** @param level "high", "medium" or "low" (any letter case) */
    public List<Project> getByRiskLevel(String level) throws Exception {
        String wanted = level.trim().toLowerCase();
        try {
            return run(SQL_WITH_LOOKUP, wanted);
        } catch (Exception lookupFailed) {
            lookupFailed.printStackTrace();
            return run(SQL_PLAIN, wanted);
        }
    }

    /** Number of records per risk level: keys "high", "medium", "low". */
    public Map<String, Integer> getZoneCounts() throws Exception {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("high", 0);
        counts.put("medium", 0);
        counts.put("low", 0);

        String sql = "SELECT LOWER(TRIM(risk_level)) AS lv, COUNT(*) AS c "
                   + "FROM mplads_ml_results GROUP BY LOWER(TRIM(risk_level))";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String lv = rs.getString("lv");
                if (lv != null && counts.containsKey(lv)) {
                    counts.put(lv, rs.getInt("c"));
                }
            }
        }
        return counts;
    }

    private List<Project> run(String sql, String wanted) throws Exception {
        List<Project> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, wanted);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project p = new Project();
                    p.setProjectId(rs.getInt("pid"));              // 0 when not found
                    p.setMpName(rs.getString("mp_name"));
                    p.setWork(rs.getString("work"));
                    p.setState(rs.getString("state"));
                    p.setConstituency(rs.getString("constituency"));
                    p.setDate(rs.getString("recommended_date"));
                    BigDecimal amt = rs.getBigDecimal("allocation_amount");
                    p.setAllocationAmount(amt == null ? 0 : amt.intValue());
                    p.setIdaApproval(rs.getString("approval"));
                    p.setProjectStatus(rs.getString("status"));
                    p.setHouse(rs.getString("house"));
                    list.add(p);
                }
            }
        }
        return list;
    }
}