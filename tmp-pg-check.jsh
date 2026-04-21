import java.sql.*;
Class.forName("org.postgresql.Driver");
try (Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/postgres", "postgres", "postgres"); Statement stmt = conn.createStatement()) {
    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM public.resume_vector_store")) {
        while (rs.next()) System.out.println("resume_vector_store_count=" + rs.getInt("cnt"));
    }
    try (ResultSet rs = stmt.executeQuery("SELECT content, metadata::text AS metadata FROM public.resume_vector_store ORDER BY id DESC LIMIT 1")) {
        while (rs.next()) {
            System.out.println("last_content=" + rs.getString("content"));
            System.out.println("last_metadata=" + rs.getString("metadata"));
        }
    }
}
/exit
