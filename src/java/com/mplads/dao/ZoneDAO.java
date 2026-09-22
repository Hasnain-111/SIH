package com.mplads.dao;

import com.mplads.DBConnection;
import com.mplads.model.Project;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads zone projects from the ML results table (mplads_ml_results).
 *
 * Red    = High risk
 * Yellow = Medium risk
 * Green  = Low risk
 *
 * The ML table does not contain project_id / ida_approval.
 * Therefore ML rows are linked to the projectt table using:
 *
 * 1. Strong match:
 *    MP + work + allocation + state + constituency + date
 *
 * 2. Weak match:
 *    MP + work + allocation
 *
 * The lowest project_id wins when multiple records match.
 */
public class ZoneDAO {

    // =========================================================
    // ZONE QUERY
    // =========================================================

    private static final String SQL_ZONE_ROWS =
            "SELECT m.mp_name, " +
            "       m.work, " +
            "       m.house, " +
            "       m.state, " +
            "       m.constituency, " +
            "       m.recommended_date, " +
            "       m.allocation_amount, " +
            "       m.status " +
            "FROM mplads_ml_results m " +
            "WHERE LOWER(TRIM(m.risk_level)) = ? " +
            "ORDER BY m.id";


    // =========================================================
    // PROJECT INDEX QUERY
    // =========================================================
    //
    // IMPORTANT:
    // Do NOT call ProjectDAO.getAllProjects() here.
    //
    // ProjectDAO is now responsible for paginated project
    // requests. ZoneDAO needs the complete matching index,
    // so it uses a separate query containing only the columns
    // required for matching.
    //

    private static final String SQL_PROJECT_INDEX =
            "SELECT project_id, " +
            "       mp_name, " +
            "       work_, " +
            "       state, " +
            "       constituency, " +
            "       Date_, " +
            "       allocation_amount, " +
            "       ida_approval " +
            "FROM projectt " +
            "ORDER BY project_id";


    // =========================================================
    // CACHE
    // =========================================================

    private static final long CACHE_MS = 5 * 60 * 1000L;

    private static volatile Index cachedIndex = null;

    private static volatile long cachedAt = 0;


    // =========================================================
    // GET PROJECTS BY RISK LEVEL
    // =========================================================

    /**
     * @param level "high", "medium" or "low"
     * @return projects belonging to the requested risk level
     */
    public List<Project> getByRiskLevel(String level) throws Exception {

        if (level == null || level.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Risk level is required"
            );
        }

        String wanted =
                level.trim().toLowerCase(Locale.ROOT);


        // -----------------------------------------------------
        // Step 1: Read ML rows
        // -----------------------------------------------------

        List<Project> rows =
                readZoneRows(wanted);


        // -----------------------------------------------------
        // Step 2: Build / get project index
        // -----------------------------------------------------

        int linked = 0;

        try {

            Index idx = getIndex();


            // -------------------------------------------------
            // Step 3: Link ML rows to projectt
            // -------------------------------------------------

            for (Project row : rows) {

                Project hit =
                        find(idx, row);


                if (hit != null) {

                    row.setProjectId(
                            hit.getProjectId()
                    );

                    row.setIdaApproval(
                            hit.getIdaApproval()
                    );

                    linked++;
                }
            }

        } catch (Exception e) {

            /*
             * The ML rows should still be returned even if
             * project linking fails.
             */

            e.printStackTrace();
        }


        System.out.println(
                "[ZoneDAO] " +
                wanted +
                " risk: " +
                rows.size() +
                " rows, " +
                linked +
                " linked to projects table"
        );


        return rows;
    }


    // =========================================================
    // GET ZONE COUNTS
    // =========================================================

    /**
     * Returns number of records for each risk level.
     *
     * Keys:
     * high
     * medium
     * low
     */
    public Map<String, Integer> getZoneCounts()
            throws Exception {

        Map<String, Integer> counts =
                new HashMap<>();


        counts.put("high", 0);
        counts.put("medium", 0);
        counts.put("low", 0);


        String sql =
                "SELECT LOWER(TRIM(risk_level)) AS lv, " +
                "       COUNT(*) AS c " +
                "FROM mplads_ml_results " +
                "GROUP BY LOWER(TRIM(risk_level))";


        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                String lv =
                        rs.getString("lv");


                if (
                        lv != null &&
                        counts.containsKey(lv)
                ) {

                    counts.put(
                            lv,
                            rs.getInt("c")
                    );
                }
            }
        }


        return counts;
    }


    // =========================================================
    // GET ML RESULT FOR ONE PROJECT
    // =========================================================

    /**
     * Finds the ML result corresponding to a project.
     *
     * Matching:
     *
     * MP name
     * allocation amount
     * work
     * state
     * constituency
     * recommended date
     *
     * The requested risk level receives additional priority.
     */
    public Map<String, Object> getMlResult(
            Project p,
            String level
    ) throws Exception {


        String sql =
                "SELECT * " +
                "FROM mplads_ml_results m " +
                "WHERE LOWER(TRIM(m.mp_name)) = ? " +
                "AND ROUND(m.allocation_amount) = ?";


        Map<String, Object> best = null;

        int bestScore = -1;


        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(
                    1,
                    norm(p.getMpName())
            );


            ps.setInt(
                    2,
                    p.getAllocationAmount()
            );


            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                // -------------------------------------------------
                // Find anomaly explanation column dynamically
                // -------------------------------------------------

                ResultSetMetaData md =
                        rs.getMetaData();


                String explCol = null;


                for (
                        int i = 1;
                        i <= md.getColumnCount();
                        i++
                ) {

                    String n =
                            md.getColumnLabel(i);


                    if (
                            n != null &&
                            n.toLowerCase(
                                    Locale.ROOT
                            ).startsWith(
                                    "anomaly_explanat"
                            )
                    ) {

                        explCol = n;

                        break;
                    }
                }


                // -------------------------------------------------
                // Find best ML match
                // -------------------------------------------------

                while (rs.next()) {

                    if (
                            !normWork(
                                    rs.getString("work")
                            )
                            .equals(
                                    normWork(
                                            p.getWork()
                                    )
                            )
                    ) {

                        continue;
                    }


                    int score = 0;


                    if (
                            norm(
                                    rs.getString("state")
                            )
                            .equals(
                                    norm(
                                            p.getState()
                                    )
                            )
                    ) {

                        score++;
                    }


                    if (
                            norm(
                                    rs.getString(
                                            "constituency"
                                    )
                            )
                            .equals(
                                    norm(
                                            p.getConstituency()
                                    )
                            )
                    ) {

                        score++;
                    }


                    if (
                            norm(
                                    rs.getString(
                                            "recommended_date"
                                    )
                            )
                            .equals(
                                    norm(
                                            p.getDate()
                                    )
                            )
                    ) {

                        score++;
                    }


                    String lv =
                            rs.getString("risk_level");


                    if (
                            level != null &&
                            lv != null &&
                            lv.trim().equalsIgnoreCase(
                                    level.trim()
                            )
                    ) {

                        score += 10;
                    }


                    if (score > bestScore) {

                        bestScore = score;


                        best =
                                new HashMap<>();


                        best.put(
                                "risk_score",
                                rs.getInt(
                                        "risk_score"
                                )
                        );


                        best.put(
                                "risk_level",
                                lv == null
                                        ? ""
                                        : lv.trim()
                        );


                        best.put(
                                "anomaly_score",
                                rs.getDouble(
                                        "anomaly_score"
                                )
                        );


                        best.put(
                                "recommendation",
                                explCol == null
                                        ? ""
                                        : rs.getString(
                                                explCol
                                        )
                        );
                    }
                }
            }
        }


        return best;
    }


    // =========================================================
    // READ ML ZONE ROWS
    // =========================================================

    private List<Project> readZoneRows(
            String wanted
    ) throws Exception {

        List<Project> list =
                new ArrayList<>();


        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                SQL_ZONE_ROWS
                        )
        ) {

            ps.setString(
                    1,
                    wanted
            );


            try (
                    ResultSet rs =
                            ps.executeQuery()
            ) {

                while (rs.next()) {

                    Project p =
                            new Project();


                    p.setMpName(
                            rs.getString("mp_name")
                    );


                    p.setWork(
                            rs.getString("work")
                    );


                    p.setState(
                            rs.getString("state")
                    );


                    p.setConstituency(
                            rs.getString(
                                    "constituency"
                            )
                    );


                    p.setDate(
                            rs.getString(
                                    "recommended_date"
                            )
                    );


                    BigDecimal amt =
                            rs.getBigDecimal(
                                    "allocation_amount"
                            );


                    p.setAllocationAmount(
                            amt == null
                                    ? 0
                                    : amt.setScale(
                                            0,
                                            RoundingMode.HALF_UP
                                    ).intValue()
                    );


                    p.setProjectStatus(
                            rs.getString("status")
                    );


                    p.setHouse(
                            rs.getString("house")
                    );


                    list.add(p);
                }
            }
        }


        return list;
    }


    // =========================================================
    // PROJECT INDEX
    // =========================================================

    /**
     * Index used to connect ML rows with projectt rows.
     */
    static final class Index {

        final Map<String, Project> strong =
                new HashMap<>();


        final Map<String, Project> weak =
                new HashMap<>();
    }


    // =========================================================
    // GET / BUILD INDEX
    // =========================================================

    private static synchronized Index getIndex()
            throws Exception {

        long now =
                System.currentTimeMillis();


        // -----------------------------------------------------
        // Return cached index if still valid
        // -----------------------------------------------------

        if (
                cachedIndex != null &&
                now - cachedAt < CACHE_MS
        ) {

            return cachedIndex;
        }


        // -----------------------------------------------------
        // Load only required project columns
        // -----------------------------------------------------

        List<Project> all =
                loadProjectIndexRows();


        Index idx =
                buildIndex(all);


        /*
         * Do not cache an empty result.
         *
         * If the DB temporarily fails, the next request
         * should try again.
         */

        if (!all.isEmpty()) {

            cachedIndex = idx;

            cachedAt = now;
        }


        return idx;
    }


    // =========================================================
    // LOAD PROJECT INDEX
    // =========================================================

    private static List<Project> loadProjectIndexRows()
            throws Exception {

        List<Project> all =
                new ArrayList<>();


        try (
                Connection con =
                        DBConnection.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                SQL_PROJECT_INDEX
                        );

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                Project p =
                        new Project();


                p.setProjectId(
                        rs.getInt("project_id")
                );


                p.setMpName(
                        rs.getString("mp_name")
                );


                p.setWork(
                        rs.getString("work_")
                );


                p.setState(
                        rs.getString("state")
                );


                p.setConstituency(
                        rs.getString(
                                "constituency"
                        )
                );


                p.setDate(
                        rs.getString("Date_")
                );


                p.setAllocationAmount(
                        rs.getInt(
                                "allocation_amount"
                        )
                );


                p.setIdaApproval(
                        rs.getString(
                                "ida_approval"
                        )
                );


                all.add(p);
            }
        }


        System.out.println(
                "[ZoneDAO] Project index loaded: " +
                all.size() +
                " projects"
        );


        return all;
    }


    // =========================================================
    // BUILD INDEX
    // =========================================================

    static Index buildIndex(
            List<Project> all
    ) {

        Index idx =
                new Index();


        for (Project p : all) {

            putLowest(
                    idx.weak,
                    weakKey(p),
                    p
            );


            putLowest(
                    idx.strong,
                    strongKey(p),
                    p
            );
        }


        return idx;
    }


    // =========================================================
    // KEEP LOWEST PROJECT ID
    // =========================================================

    private static void putLowest(
            Map<String, Project> map,
            String key,
            Project p
    ) {

        Project current =
                map.get(key);


        if (
                current == null ||
                p.getProjectId() <
                        current.getProjectId()
        ) {

            map.put(
                    key,
                    p
            );
        }
    }


    // =========================================================
    // FIND MATCH
    // =========================================================

    /**
     * First tries strong matching.
     * Then falls back to weak matching.
     */
    static Project find(
            Index idx,
            Project row
    ) {

        Project hit =
                idx.strong.get(
                        strongKey(row)
                );


        if (hit == null) {

            hit =
                    idx.weak.get(
                            weakKey(row)
                    );
        }


        return hit;
    }


    // =========================================================
    // WEAK KEY
    // =========================================================

    private static String weakKey(
            Project p
    ) {

        return
                norm(p.getMpName()) +
                "\u001f" +
                normWork(p.getWork()) +
                "\u001f" +
                p.getAllocationAmount();
    }


    // =========================================================
    // STRONG KEY
    // =========================================================

    private static String strongKey(
            Project p
    ) {

        return
                weakKey(p) +
                "\u001f" +
                norm(p.getState()) +
                "\u001f" +
                norm(p.getConstituency()) +
                "\u001f" +
                norm(p.getDate());
    }


    // =========================================================
    // NORMALIZE STRING
    // =========================================================

    /**
     * Trim spaces, collapse repeated whitespace,
     * and ignore letter case.
     */
    private static String norm(
            String s
    ) {

        if (s == null) {
            return "";
        }


        return s
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        Locale.ROOT
                );
    }


    // =========================================================
    // NORMALIZE WORK
    // =========================================================

    /**
     * The ML table stores work as VARCHAR(500),
     * therefore compare only the first 500 characters.
     */
    private static String normWork(
            String s
    ) {

        String n =
                norm(s);


        return n.length() > 500
                ? n.substring(0, 500)
                : n;
    }
}