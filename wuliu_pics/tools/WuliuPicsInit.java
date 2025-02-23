package wuliu_pics.tools;

import wuliu_pics.common.DB;
import wuliu_pics.common.MyUtil;
import wuliu_pics.common.ProjectInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WuliuPicsInit {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("ERROR! 請提供專案名稱");
        }
        var projName = args[0];

        System.out.println("創建專案: " + projName);
        MyUtil.pathMustNotExists(MyUtil.PROJ_INFO_PATH);
        MyUtil.pathMustNotExists(MyUtil.ALBUMS_PATH);
        MyUtil.pathMustNotExists(Path.of(MyUtil.WULIU_PICS_DB));

        var projInfo = new ProjectInfo(projName);
        System.out.println("Create => " + MyUtil.PROJ_INFO_PATH);
        MyUtil.writeJsonToFilePretty(projInfo.toMap(), MyUtil.PROJ_INFO_PATH.toFile());
        System.out.println("Create => " + MyUtil.ALBUMS_PATH);
        Files.createDirectory(MyUtil.ALBUMS_PATH);
        System.out.println("Create => " + MyUtil.WULIU_PICS_DB);
        var db = new DB(MyUtil.WULIU_PICS_DB);
        db.createTables();
        System.out.println("成功創建專案: " + projName);
    }
}
