package wuliu_pics.common;

import com.fasterxml.jackson.jr.ob.JSON;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Metadata {
    public String checksum; // SHA-1
    public String checked;  // RFC3339
    public long size;
    public boolean damaged;

    private static final String CHECKSUM = "checksum";
    private static final String CHECKED = "checked";
    private static final String SIZE = "size";
    private static final String DAMAGED = "damaged";

    public Metadata() {
        // create an empty metadata
    }

    public Metadata(Path file) {
        checksum = getFileSHA1(file);
        checked = MyUtil.timeNowRFC3339();
        size = file.toFile().length();
        damaged = false;
    }

    public void writeToJson(File jsonFile) throws IOException {
        JSON.std.with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                .write(this.toMap(), jsonFile);
    }

    public Metadata readFromJson(Path jsonFile) throws IOException {
        var data = MyUtil.readJsonFileToMap(jsonFile);
        return readFromMap(data);
    }

    public Metadata readFromMap(Map<String,Object> data) {
        checksum = (String) data.get(CHECKSUM);
        checked = (String) data.get(CHECKED);
        size = getLongFromMap(data, SIZE);
        damaged = (boolean) data.get(DAMAGED);
        return this;
    }

    public LinkedHashMap<String,Object> toMap() {
        LinkedHashMap<String,Object> map = new LinkedHashMap<>();
        map.putLast(CHECKSUM, checksum);
        map.putLast(CHECKED, checked);
        map.putLast(SIZE, size);
        map.putLast(DAMAGED, damaged);
        return map;
    }

    /**
     * 讀取 file 的全部內容, 計算其 SHA-1, 轉換為 hex string 返回。
     */
    public static String getFileSHA1(Path file) {
        try {
            var md = MessageDigest.getInstance("SHA-1");
            var data = Files.readAllBytes(file);
            var digest = md.digest(data);
            var hex = HexFormat.of();
            return hex.formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}
