package wuliu_pics.common;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectInfo {
    public String projectName;         // 备份时要求专案名称相同
    public boolean isBackup;           // 是否副本（副本禁止添加、删除等）
    public List<String> projects;      // 第一个是主专案，然后是备份专案
    public Integer checkInterval;      // 检查完整性, 单位: day
    public Integer checkSizeLimit;     // 检查完整性, 单位: MB

    public ProjectInfo() {}

    public ProjectInfo(String name) {
        projectName = name;
        isBackup = false;
        projects = List.of(".");
        checkInterval = 90;
        checkSizeLimit = 1024;
    }

    public static ProjectInfo fromJsonFile(Path jsonPath) throws IOException {
        Map<String,Object> data = MyUtil.readJsonFileToMap(jsonPath);
        return of(data);
    }

    public static ProjectInfo of(Map<String,Object> data) {
        ProjectInfo info = new ProjectInfo();
        info.projectName = (String) data.get("ProjectName");
        info.isBackup = (boolean) data.get("IsBackup");
        info.projects = MyUtil.getStrListFromMap(data, "Projects");
        info.checkInterval = MyUtil.getIntFromMap(data, "CheckInterval");
        info.checkSizeLimit = MyUtil.getIntFromMap(data, "CheckSizeLimit");
        return info;
    }

    public LinkedHashMap<String, Object> toMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.putLast("ProjectName", projectName);
        map.putLast("IsBackup", isBackup);
        map.putLast("Projects", projects);
        map.putLast("CheckInterval", checkInterval);
        map.putLast("CheckSizeLimit", checkSizeLimit);
        return map;
    }
}
