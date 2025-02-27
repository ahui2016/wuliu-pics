package wuliu_pics.common;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.io.File;
import java.util.List;

public class MyUtil {
    public static final String RepoURL = "https://github.com/ahui2016/wuliu-pics";
    public static final DateTimeFormatter RFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String PROJECT_JSON = "project.json";
    public static final String ALBUMS = "albums";

    public static final Path PROJ_INFO_PATH = Path.of(PROJECT_JSON);
    public static final Path ALBUMS_PATH = Path.of("albums");

    public static String timeNowRFC3339() {
        return LocalDateTime.now().format(RFC3339);
    }

    public static void checkNotBackup(ProjectInfo info) {
        if (info.isBackup) {
            throw new RuntimeException("這是備份專案, 不可使用該功能");
        }
    }

    /**
     * 確保 folder 存在, 如果不存在或有同名檔案, 則拋出異常。
     * 如果 folder 存在則無事發生。
     */
    public static void folderMustExists(Path folder) {
        if (Files.notExists(folder) || !Files.isDirectory(folder)) {
            throw new RuntimeException("Not Found Folder: " + folder);
        }
    }

    public static void pathMustExists(Path path) {
        if (Files.notExists(path)) {
            throw new RuntimeException("Not Found: " + path);
        }
    }

    /**
     * 確保 path 不存在, 如果存在則拋出異常。 如果 path 不存在則無事發生。
     */
    public static void pathMustNotExists(Path path) {
        if (Files.exists(path)) {
            throw new RuntimeException("檔案已存在: " + path);
        }
    }

    public static void mkdirIfNotExists(Path folder) throws IOException {
        if (Files.exists(folder) && Files.isDirectory(folder)) {
            return;
        }
        // Files.createDirectory 會檢查是否存在同名檔案。
        // if (Files.isRegularFile(folder)) {}
        Files.createDirectory(folder);
    }

    public static Map<String,Object> readJsonFileToMap(Path jsonPath) throws IOException {
        String json = Files.readString(jsonPath);
        return JSON.std.mapFrom(json);
    }

    public static void writeJsonToFilePretty(Map<String,Object> map, File file) throws IOException {
        JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT).write(map, file);
    }

    public static Long getLongFromMap(Map<String,Object> map, String key) {
        Number n = (Number) map.get(key);
        return n.longValue();
    }

    public static Integer getIntFromMap(Map<String,Object> map, String key) {
        Number n = (Number) map.get(key);
        return n.intValue();
    }

    public static List<String> getStrListFromMap(Map<String,Object> data, String key) {
        Object obj = data.get(key);
        if (obj instanceof List<?>) {
            @SuppressWarnings("unchecked")
            var list = (List<String>) obj;
            return list;
        }
        throw new RuntimeException(String.format("%s is not a string list", key));
    }

    public static List<Path> getAlbums(Path albumsPath) {
        try (var files = Files.list(albumsPath)) {
            return files.filter(Files::isDirectory).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String fileSizeToString(double size) {
        long BYTE = 1L;
        long KB = BYTE << 10;
        long MB = KB << 10;
        long GB = MB << 10;
        if (size <= 0) return "0";
        if (size >= GB) return formatSize(size, GB, "GB");
        if (size >= MB) return formatSize(size, MB, "MB");
        return formatSize(size, KB, "KB");
    }

    private static String formatSize(double size, long divider, String unitName) {
        var formater = new DecimalFormat("#.##");
        return formater.format(size / divider) + " " + unitName;
    }
}
