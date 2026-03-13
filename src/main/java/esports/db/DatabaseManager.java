package esports.db;

import esports.model.MatchRecord;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    public static final String DEFAULT_DB = "esports_matches";
    private static final String ADMIN_ROLE_PASS = "admin123";
    private static final String GUEST_ROLE_PASS = "guest123";

    private Connection conn;
    private String currentUser;
    private String currentRole;
    private String currentDb;

    private static final String PG_SUPERUSER = "postgres";
    private static final String PG_SUPERPASS = "postgres";

    private Connection pgConnect(String dbName) throws SQLException {
        return pgConnectAs(dbName, PG_SUPERUSER, PG_SUPERPASS);
    }

    private Connection pgConnectAs(String dbName, String user, String pass) throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/" + dbName;
        java.util.Properties p = new java.util.Properties();
        p.setProperty("user", user);
        if (pass != null && !pass.isEmpty()) p.setProperty("password", pass);
        return DriverManager.getConnection(url, p);
    }

    /**
     * Автоматически создаёт базу данных и заливает setup.sql если база не существует.
     * Если база уже есть — всё равно перезаливает setup.sql чтобы роли/права были актуальны.
     */
    public void autoInitDb(String dbName) {
        try {
            // Проверяем существует ли база
            boolean exists;
            try (Connection c = pgConnect("postgres");
                 PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname=?")) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (!exists) {
                // Создаём базу
                try (Connection c = pgConnect("postgres");
                     Statement st = c.createStatement()) {
                    c.setAutoCommit(true);
                    st.execute("CREATE DATABASE \"" + dbName + "\"");
                }
            }

            // В любом случае заливаем setup.sql — идемпотентно
            runSetupViaJdbc(dbName);

        } catch (Exception e) {
            System.err.println("autoInitDb warning: " + e.getMessage());
        }
    }

    public List<String> listDatabases() {
        List<String> dbs = new ArrayList<>();
        try (Connection c = pgConnect("postgres");
             PreparedStatement ps = c.prepareStatement(
                     "SELECT datname FROM pg_database WHERE datistemplate=false ORDER BY datname");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) dbs.add(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("listDatabases error: " + e.getMessage());
        }
        return dbs;
    }

    public String checkCredentials(String dbName, String username, String password) {
        try (Connection c = pgConnectAs(dbName, "es_guest", GUEST_ROLE_PASS);
             PreparedStatement ps = c.prepareStatement("SELECT fn_app_login(?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException ignored) {}
        return null;
    }

    public void createDatabase(String dbName) throws Exception {
        if (currentRole != null && !"admin".equals(currentRole)) {
            throw new Exception("Недостаточно прав: требуется роль администратора.");
        }
        try (Connection c = pgConnect("postgres")) {
            c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM pg_database WHERE datname=?")) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new Exception("База данных '" + dbName + "' уже существует.");
                }
            }
            try (Statement st = c.createStatement()) {
                st.execute("CREATE DATABASE \"" + dbName + "\"");
            }
        }
        runSetupViaJdbc(dbName);
    }

    public void initializeDb(String dbName) throws Exception {
        runSetupViaJdbc(dbName);
    }

    private void runSetupViaJdbc(String dbName) throws Exception {
        URL resource = getClass().getClassLoader().getResource("setup.sql");
        if (resource == null) throw new Exception("setup.sql не найден в classpath");

        String sql;
        try (InputStream in = resource.openStream()) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (Connection c = pgConnectAs(dbName, PG_SUPERUSER, PG_SUPERPASS)) {
            c.setAutoCommit(true);
            List<String> statements = splitSql(sql);
            try (Statement st = c.createStatement()) {
                for (String stmt : statements) {
                    stmt = stmt.trim();
                    if (!stmt.isEmpty()) {
                        try {
                            st.execute(stmt);
                        } catch (SQLException e) {
                            String msg = e.getMessage();
                            if (msg != null && (msg.contains("already exists") || msg.contains("уже существует"))) {
                                System.out.println("Skipped (already exists): " + stmt.substring(0, Math.min(60, stmt.length())));
                            } else {
                                throw new Exception("Ошибка выполнения setup.sql:\n" + msg);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<String> splitSql(String sql) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDollarQuote = false;
        String dollarTag = null;
        int i = 0;

        while (i < sql.length()) {
            if (!inDollarQuote && sql.charAt(i) == '$') {
                int end = sql.indexOf('$', i + 1);
                if (end >= i + 1) {
                    String tag = sql.substring(i, end + 1);
                    inDollarQuote = true;
                    dollarTag = tag;
                    current.append(tag);
                    i = end + 1;
                    continue;
                }
            } else if (inDollarQuote && dollarTag != null && sql.startsWith(dollarTag, i)) {
                current.append(dollarTag);
                i += dollarTag.length();
                inDollarQuote = false;
                dollarTag = null;
                continue;
            }

            char ch = sql.charAt(i);

            if (!inDollarQuote && ch == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                int eol = sql.indexOf('\n', i);
                if (eol == -1) eol = sql.length();
                i = eol;
                continue;
            }

            if (!inDollarQuote && ch == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) result.add(stmt);
                current = new StringBuilder();
                i++;
                continue;
            }

            current.append(ch);
            i++;
        }

        String last = current.toString().trim();
        if (!last.isEmpty()) result.add(last);
        return result;
    }

    public void dropDatabase(String dbName) throws Exception {
        if (!"admin".equals(currentRole)) {
            throw new Exception("Недостаточно прав: требуется роль администратора.");
        }
        if (conn != null && !conn.isClosed()) { conn.close(); conn = null; }
        try (Connection c = pgConnect("postgres")) {
            c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname=? AND pid<>pg_backend_pid()")) {
                ps.setString(1, dbName);
                ps.executeQuery();
            }
            try (Statement st = c.createStatement()) {
                st.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
            }
        }
    }

    public void login(String dbName, String appUser, String appPass) throws Exception {
        // Автоматически создаём и инициализируем базу если нужно
        autoInitDb(dbName);

        String role;
        try (Connection c = pgConnectAs(dbName, "es_guest", GUEST_ROLE_PASS);
             PreparedStatement ps = c.prepareStatement("SELECT fn_app_login(?,?)")) {
            ps.setString(1, appUser);
            ps.setString(2, appPass);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new Exception("Неверный логин или пароль.");
                role = rs.getString(1);
            }
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("does not exist")) {
                throw new Exception("База данных не инициализирована. Нажмите 'Инициализировать'.");
            }
            throw new Exception(cleanMsg(msg));
        }

        String jdbcUser = "admin".equals(role) ? "es_admin" : "es_guest";
        String jdbcPass = "admin".equals(role) ? ADMIN_ROLE_PASS : GUEST_ROLE_PASS;

        if (conn != null && !conn.isClosed()) conn.close();
        conn = pgConnectAs(dbName, jdbcUser, jdbcPass);
        conn.setAutoCommit(true);
        this.currentUser = appUser;
        this.currentRole = role;
        this.currentDb = dbName;
    }

    public void register(String dbName, String username, String password, String role, String adminCode) throws Exception {
        try (Connection c = pgConnectAs(dbName, "es_guest", GUEST_ROLE_PASS);
             PreparedStatement ps = c.prepareStatement("CALL sp_app_register(?,?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, adminCode == null ? "" : adminCode);
            ps.execute();
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("does not exist")) {
                throw new Exception("База данных не инициализирована.");
            }
            throw new Exception(cleanMsg(msg));
        }
    }

    public void createUser(String username, String password, String role) throws Exception {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_create_user(?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.execute();
        } catch (SQLException e) {
            throw new Exception(cleanMsg(e.getMessage()));
        }
    }

    public void disconnect() {
        if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} conn = null; }
        currentUser = null; currentRole = null; currentDb = null;
    }

    public boolean isConnected() {
        try { return conn != null && !conn.isClosed(); } catch (SQLException e) { return false; }
    }

    public String getCurrentRole() { return currentRole; }
    public String getCurrentUser() { return currentUser; }
    public String getCurrentDb()   { return currentDb; }

    private void requireConnection() throws SQLException {
        if (!isConnected()) throw new SQLException("Нет подключения к базе данных.");
    }

    public void addMatchRecord(MatchRecord r) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_add_match(?,?,?,?,?,?,?,?)")) {
            ps.setString(1, r.getMatchDate());
            ps.setString(2, r.getMatchDuration());
            ps.setString(3, r.getTournament());
            ps.setString(4, r.getGame());
            ps.setString(5, r.getFormat());
            ps.setString(6, r.getTeam1());
            ps.setString(7, r.getTeam2());
            ps.setString(8, r.getWinner());
            ps.execute();
        }
    }

    public void updateMatch(MatchRecord r) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_update_match(?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, r.getId());
            ps.setString(2, r.getMatchDate());
            ps.setString(3, r.getMatchDuration());
            ps.setString(4, r.getTournament());
            ps.setString(5, r.getGame());
            ps.setString(6, r.getFormat());
            ps.setString(7, r.getTeam1());
            ps.setString(8, r.getTeam2());
            ps.setString(9, r.getWinner());
            ps.execute();
        }
    }

    public void deleteById(int id) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_delete_by_id(?)")) {
            ps.setInt(1, id); ps.execute();
        }
    }

    public void deleteByTournament(String tournament) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_delete_by_tournament(?)")) {
            ps.setString(1, tournament); ps.execute();
        }
    }

    public void clearTable() throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("CALL sp_clear_table()")) {
            ps.execute();
        }
    }

    public List<MatchRecord> getAll() throws SQLException {
        requireConnection();
        List<MatchRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM fn_get_all()");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public MatchRecord getById(int id) throws SQLException {
        requireConnection();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM fn_get_by_id(?)")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<MatchRecord> searchByTournament(String tournament) throws SQLException {
        requireConnection();
        List<MatchRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM fn_search_by_tournament(?)")) {
            ps.setString(1, tournament);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void exportExcel(List<MatchRecord> records, String filePath) throws Exception {
        String[] headers = {"ID","Date","Duration","Tournament","Game","Format","Team 1","Team 2","Winner"};
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(filePath))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
            zos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
            zos.write(("<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels"));
            zos.write(("<?xml version=\"1.0\"?><Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "</Relationships>").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new java.util.zip.ZipEntry("xl/workbook.xml"));
            zos.write(("<?xml version=\"1.0\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets><sheet name=\"Matches\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new java.util.zip.ZipEntry("xl/worksheets/sheet1.xml"));
            StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            sb.append("<row>");
            for (String h : headers) sb.append("<c t=\"inlineStr\"><is><t>").append(escXml(h)).append("</t></is></c>");
            sb.append("</row>");
            for (MatchRecord r : records) {
                String[] vals = {String.valueOf(r.getId()), r.getMatchDate(), r.getMatchDuration(),
                        r.getTournament(), r.getGame(), r.getFormat(), r.getTeam1(), r.getTeam2(), r.getWinner()};
                sb.append("<row>");
                for (String v : vals) sb.append("<c t=\"inlineStr\"><is><t>").append(escXml(v)).append("</t></is></c>");
                sb.append("</row>");
            }
            sb.append("</sheetData></worksheet>");
            zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }

    private String escXml(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    public static class PartialImportException extends Exception {
        public final int added, skipped;
        public final String details;
        public PartialImportException(int added, int skipped, String details) {
            super("Partial import"); this.added = added; this.skipped = skipped; this.details = details;
        }
    }

    public int importJson(String filePath) throws Exception {
        requireConnection();
        String json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)), StandardCharsets.UTF_8).trim();
        if (!json.startsWith("[")) throw new Exception("JSON должен быть массивом объектов матчей.");
        json = json.substring(1, json.lastIndexOf(']')).trim();

        List<String> objects = new ArrayList<>();
        int depth = 0; int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { objects.add(json.substring(start, i + 1)); start = -1; } }
        }

        int added = 0, skipped = 0; StringBuilder errors = new StringBuilder();
        for (String obj : objects) {
            try {
                MatchRecord r = parseJsonObject(obj);
                addMatchRecord(r);
                added++;
            } catch (Exception e) {
                skipped++;
                errors.append("\n- ").append(cleanMsg(e.getMessage()));
            }
        }
        if (skipped > 0) throw new PartialImportException(added, skipped, errors.toString());
        return added;
    }

    private MatchRecord parseJsonObject(String obj) throws Exception {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        obj = obj.trim().replaceAll("^\\{|\\}$", "");
        for (String pair : obj.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = pair.split(":", 2);
            if (kv.length < 2) continue;
            String k = kv[0].trim().replaceAll("\"", "");
            String v = kv[1].trim().replaceAll("^\"|\"$", "");
            m.put(k, v);
        }
        MatchRecord r = new MatchRecord();
        r.setMatchDate(m.getOrDefault("match_date", ""));
        r.setMatchDuration(m.getOrDefault("match_duration", ""));
        r.setTournament(m.getOrDefault("tournament", ""));
        r.setGame(m.getOrDefault("game", ""));
        r.setFormat(m.getOrDefault("format", ""));
        r.setTeam1(m.getOrDefault("team1", ""));
        r.setTeam2(m.getOrDefault("team2", ""));
        r.setWinner(m.getOrDefault("winner", ""));
        return r;
    }

    private MatchRecord mapRow(ResultSet rs) throws SQLException {
        MatchRecord r = new MatchRecord();
        r.setId(rs.getInt("id"));
        r.setMatchDate(rs.getString("match_date"));
        r.setMatchDuration(rs.getString("match_duration"));
        r.setTournament(rs.getString("tournament"));
        r.setGame(rs.getString("game"));
        r.setFormat(rs.getString("format"));
        r.setTeam1(rs.getString("team1"));
        r.setTeam2(rs.getString("team2"));
        r.setWinner(rs.getString("winner"));
        return r;
    }

    private String cleanMsg(String msg) {
        if (msg == null) return "Неизвестная ошибка.";
        if (msg.contains("ERROR:")) {
            int idx = msg.indexOf("ERROR:") + 6;
            String tail = msg.substring(idx).trim();
            if (tail.contains("\n")) tail = tail.substring(0, tail.indexOf('\n')).trim();
            return tail;
        }
        return msg;
    }
}
