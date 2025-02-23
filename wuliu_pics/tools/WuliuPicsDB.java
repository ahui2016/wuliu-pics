package wuliu_pics.tools;

import wuliu_pics.common.MyUtil;
import wuliu_pics.common.ProjectInfo;

import java.io.IOException;

public class WuliuPicsDB {
    static ProjectInfo projInfo;

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(0);
        }
/*
        switch (args[0]) {
            case "-update" -> updateDB();
            default      -> printHelp();
        }
*/
    }

    static void printHelp() {
        System.out.println("""
            $ java -cp ".;classes/*" wuliu_j.tools.WuliuDB [options]
            options:
            -init 第一次使用 wuliu-j 時, 初始化數據庫
            """);
    }

    static void loadsProjInfo() {
        try {
            projInfo = ProjectInfo.fromJsonFile(MyUtil.PROJ_INFO_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
