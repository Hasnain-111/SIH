package com.mplads.dao;

import com.mplads.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class RiskDAO {

    public int saveRisk(
            String projectId,
            double riskScore,
            String riskLevel,
            double anomalyScore,
            String modelVersion) throws Exception {

        String sql =
            "INSERT INTO risk_results " +
            "(project_id, risk_score, risk_level, anomaly_score, model_version, analyzed_at) " +
            "VALUES (?, ?, ?, ?, ?, NOW())";

        try (
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(
                    sql,
                    PreparedStatement.RETURN_GENERATED_KEYS)
        ) {

            ps.setString(1, projectId);
            ps.setDouble(2, riskScore);
            ps.setString(3, riskLevel);
            ps.setDouble(4, anomalyScore);
            ps.setString(5, modelVersion);

            ps.executeUpdate();

            var keys = ps.getGeneratedKeys();

            if (keys.next()) {
                return keys.getInt(1);
            }
        }

        return -1;
    }
}