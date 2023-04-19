import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
package com.shinear;

/**
 * Hello world!
 *
 */
public class Dhtemp {
    public static void main(String[] args) {
        String dbuatUrl = "jdbc:oracle:thin:@//localhost:1521/dbuat";
        String dbuatUser = "username";
        String dbuatPassword = "password";
        String dbtrainingUrl = "jdbc:oracle:thin:@//localhost:1521/dbtraining";
        String dbtrainingUser = "username";
        String dbtrainingPassword = "password";
        String proxyHost = "proxyhost";
        int proxyPort = 8080;
        String jrServerUrl = "http://sss.sss.com/analysis";

        // Task 1: Query data from dbuat and dbtraining, and update dbuat with the
        // results
        try (Connection dbuatConn = DriverManager.getConnection(dbuatUrl, dbuatUser, dbuatPassword);
                Connection dbtrainingConn = DriverManager.getConnection(dbtrainingUrl, dbtrainingUser,
                        dbtrainingPassword)) {
            // Create uatMap
            Map<String, String> uatMap = new HashMap<>();
            try (Statement stmt = dbuatConn.createStatement()) {
                String sql = "select pid, allowbuy from tb_prod where allowbuy = 'Y'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String pid = rs.getString("pid");
                    String allowbuy = rs.getString("allowbuy");
                    uatMap.put(pid, allowbuy);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // Create pidsuat
            List<String> pidsuat = new ArrayList<>(uatMap.keySet());
            // Create trainMap
            Map<String, String> trainMap = new HashMap<>();
            String sql = "select pid, allowbuy from tb_prod where pid in (";
            for (int i = 0; i < pidsuat.size(); i++) {
                sql += "?";
                if (i < pidsuat.size() - 1) {
                    sql += ",";
                }
            }
            sql += ")";
            try (PreparedStatement pstmt = dbtrainingConn.prepareStatement(sql)) {
                for (int i = 0; i < pidsuat.size(); i++) {
                    pstmt.setString(i + 1, pidsuat.get(i));
                }
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String pid = rs.getString("pid");
                    String allowbuy = rs.getString("allowbuy");
                    trainMap.put(pid, allowbuy);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Compare uatMap and trainMap, and update uatMap2
            Map<String, String> uatMap2 = new HashMap<>(uatMap);
            for (String pid : uatMap.keySet()) {
                String uatAllowbuy = uatMap.get(pid);
                String trainAllowbuy = trainMap.get(pid);
                if (trainAllowbuy != null && !uatAllowbuy.equals(trainAllowbuy)) {
                    uatMap2.put(pid, trainAllowbuy);
                }
            }
            // Update dbuat with uatMap2
            try (PreparedStatement pstmt = dbuatConn
                    .prepareStatement("update tb_prod set allowbuy = ? where pid = ?")) {
                for (String pid : uatMap2.keySet()) {
                    String allowbuy = uatMap2.get(pid);
                    pstmt.setString(1, allowbuy);
                    pstmt.setString(2, pid);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Task 2: Send requests to JRServer's restful API and output the results
        List<String> requestPid = new ArrayList<>();
        try (Connection dbuatConn = DriverManager.getConnection(dbuatUrl, dbuatUser, dbuatPassword)) {
            // Create requestPid
            try (Statement stmt = dbuatConn.createStatement()) {
                String sql = "select pid, allowbuy from tb_prod where allowbuy = 'Y'";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    String pid = rs.getString("pid");
                    requestPid.add(pid);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // Send requests to JRServer's restful API
            List<String> tempPid = new ArrayList<>();
            List<String> analyzedPid = new ArrayList<>();
            int batchSize = 20;
            for (int i = 0; i < requestPid.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, requestPid.size());
                List<String> batch = requestPid.subList(i, endIndex);
                String batchStr = String.join(",", batch);
                URL url = new URL(jrServerUrl + "?pid=" + batchStr);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");
                BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
                String output;
                while ((output = br.readLine()) != null) {
                    JSONObject json = new JSONObject(output);
                    JSONArray analyzed = json.getJSONArray("analyzed");
                    for (int j = 0; j < analyzed.length(); j++) {
                        analyzedPid.add(analyzed.getString(j));
                    }
                    JSONArray notanalyzed = json.getJSONArray("notanalyzed");
                    for (int j = 0; j < notanalyzed.length(); j++) {
                        tempPid.add(notanalyzed.getString(j));
                    }
                }
                conn.disconnect();
            }
            // Output the results
            List<String> cannotAnalyzePid = new ArrayList<>(tempPid);
            cannotAnalyzePid.removeAll(analyzedPid);
            System.out.println("Pids that can be analyzed: " + analyzedPid);
            System.out.println("Pids that cannot be analyzed: " + cannotAnalyzePid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
