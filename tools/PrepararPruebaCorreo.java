import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

class PrepararPruebaCorreo {
    public static void main(String[] args) throws Exception {
        var config = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(args[0]))) { config.load(reader); }
        String url = config.getProperty("spring.datasource.url");
        if (!url.matches("jdbc:mysql://localhost:3306/rinconcito_demo_mozo[?].*")) {
            throw new IllegalStateException("Esta prueba solo prepara rinconcito_demo_mozo local.");
        }
        try (var connection = DriverManager.getConnection(url, config.getProperty("spring.datasource.username"),
                config.getProperty("spring.datasource.password")); var statement = connection.createStatement()) {
            for (int index = 1; index < args.length; index++) {
                String sql = Files.readString(Path.of(args[index])).replaceAll("(?m)^--.*$", "")
                        .replaceAll("(?im)^USE\\s+\\w+;", "");
                for (String query : sql.split(";")) if (!query.isBlank()) statement.execute(query);
                System.out.println("Preparado en base demo: " + Path.of(args[index]).getFileName());
            }
        }
    }
}
