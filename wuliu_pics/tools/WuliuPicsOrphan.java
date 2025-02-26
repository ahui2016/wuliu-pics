package wuliu_pics.tools;

import wuliu_pics.common.Metadata;
import wuliu_pics.common.MyUtil;
import wuliu_pics.common.MyUtilGUI;
import wuliu_pics.common.ProjectInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class WuliuPicsOrphan implements Runnable{
    static ProjectInfo projInfo;
    static Path projRoot;

    private List<Path> albums;
    private Orphans allOrphans;

    private JFrame frame;
    private JTextField currentProjTF;
    private JTextArea msgArea;
    private JButton checkBtn;
    private JButton fixBtn;

    public static void main(String[] args) throws IOException {
        projRoot = Path.of("").toAbsolutePath();
        projInfo = ProjectInfo.fromJsonFile(MyUtil.PROJ_INFO_PATH);
        SwingUtilities.invokeLater(new WuliuPicsOrphan());
    }

    @Override
    public void run() {
        createGUI();
        initCheck();
        checkBtn.addActionListener(new CheckBtnListener());
        fixBtn.addActionListener(new FixBtnListener());
    }

    private void createGUI() {
        List.of("OptionPane.messageFont", "TextField.font",
                "Label.font", "TextArea.font", "Button.font"
        ).forEach(k -> UIManager.put(k, MyUtilGUI.FONT_20));

        frame = new JFrame("Wuliu Pics Orphan");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var pane0 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        pane0.add(new JLabel("WuliuPicsOrphan: 尋找孤立的圖片或JSON檔案"));
        pane0.add(MyUtilGUI.spacer(800, 5));

        currentProjTF = new JTextField(47);
        currentProjTF.setEditable(false);
        pane0.add(new JLabel("當前專案:"));
        pane0.add(currentProjTF);

        msgArea = new JTextArea();
        msgArea.setLineWrap(true);
        var scrollArea = MyUtilGUI.verticalScrollPane(msgArea, 850, 500);
        pane0.add(MyUtilGUI.spacer(800, 5));
        pane0.add(scrollArea);

        checkBtn = new JButton("Check");
        checkBtn.setEnabled(false);
        pane0.add(MyUtilGUI.spacer(800, 2));
        pane0.add(checkBtn);

        fixBtn = new JButton("Fix");
        fixBtn.setEnabled(false);
        // pane0.add(spacer(500, 2));
        pane0.add(fixBtn);

        frame.add(BorderLayout.CENTER, pane0);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null); // 窗口居中
        frame.setVisible(true);
    }

    private void initCheck() {
        currentProjTF.setText(projRoot.toString());
        try {
            MyUtil.checkNotBackup(projInfo);
            albums = MyUtil.getAlbums(MyUtil.ALBUMS_PATH);
            msgArea.append("發現 %d 個相冊。\n".formatted(albums.size()));
            if (albums.isEmpty()) return;
            checkAlbums();
        } catch (Exception e) {
            msgArea.append(e.toString());
        }
    }

    class FixBtnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            fixBtn.setEnabled(false);
            deleteJsonOrphans(allOrphans.metas());
            try {
                createMetas(allOrphans.files());
            } catch (Exception ex) {
                msgArea.append(ex + "\n");
                JOptionPane.showMessageDialog(frame, "出錯！");
            }
            msgArea.append("\n完成。\n");
        }

        private void deleteJsonOrphans(List<String> jsonOrphans) {
            if (jsonOrphans.isEmpty()) {
                return;
            }
            msgArea.setText("正在刪除孤立的 json 檔案\n");
            for (var orphan : jsonOrphans) {
                try {
                    Files.deleteIfExists(Path.of(orphan));
                } catch (Exception ex) {
                    msgArea.append(ex + "\n");
                }
                msgArea.append(".");
            }
            msgArea.append("\n");
        }

        private void createMetas(List<String> picOrphans) throws IOException {
            if (picOrphans.isEmpty()) {
                return;
            }
            msgArea.setText("正在生成所需的 json 檔案\n");
            for (var orphan : picOrphans) {
                var meta = new Metadata(Path.of(orphan));
                meta.writeToJson(Path.of(orphan+".json").toFile());
                msgArea.append(".");
            }
            msgArea.append("\n");
        }
    }

    class CheckBtnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkBtn.setEnabled(false);
            allOrphans = getAllOrphans();
            if (allOrphans == null) {
                JOptionPane.showMessageDialog(frame, "出錯！");
                return;
            }
            msgArea.append("\n");
            msgArea.append(allOrphans.toString());
            if (allOrphans.sum() > 0) {
                msgArea.append("\n點擊 Fix 按鈕可刪除孤立的 json 檔案，並為孤立的圖片檔案生成對應的 json 檔案。\n");
                fixBtn.setEnabled(true);
            }
        }
    }

    private void checkAlbums() {
        try (var files = Files.list(MyUtil.ALBUMS_PATH)) {
            var orphans = files.filter(Files::isRegularFile).toList();
            if (!orphans.isEmpty()) {
                msgArea.append("在 albums 的第一層子目錄中不允許出現普通檔案（非資料夾）：\n");
                var orphansStrList = orphans.stream().map(
                        f -> f.getFileName().toString()).toList();
                var msg = String.join("\n", orphansStrList);
                msgArea.append(msg);
                return;
            }
        } catch (Exception e) {
            msgArea.append(e.toString());
            return;
        }
        for (var album : albums) {
            try (var files = Files.list(album)) {
                var folders = files.filter(Files::isDirectory).toList();
                if (!folders.isEmpty()) {
                    var folder = folders.getFirst();
                    msgArea.append("在相冊 [%s] 中不允許包含子資料夾: %s\n"
                            .formatted(album.getFileName(), folder.getFileName()));
                    return;
                }
            } catch (Exception e) {
                msgArea.append(e.toString());
                return;
            }
        }
        checkBtn.setEnabled(true);
    }

    private Orphans getAllOrphans() {
        List<String> fileOrphans = new ArrayList<>();
        List<String> metaOrphans = new ArrayList<>();
        for (var album : albums) {
            var orphans = getOrphans(album);
            if (orphans == null) {
                return null;
            }
            fileOrphans.addAll(orphans.files());
            metaOrphans.addAll(orphans.metas());
        }
        return new Orphans(fileOrphans, metaOrphans);
    }

    private Orphans getOrphans(Path album) {
        Set<String> pics_json = new HashSet<>();
        Set<String> metas = new HashSet<>();
        try (var files = Files.list(album)) {
            files.forEach(path -> {
                var pathStr = path.toString();
                if (pathStr.endsWith(".json")) {
                    metas.add(pathStr);
                } else {
                    pics_json.add(pathStr+".json");
                }
            });
        } catch (Exception e) {
            msgArea.append(e.toString());
            return null;
        }

        var metaOrphans = inFirstListOnly(metas, pics_json);
        var pics_json_orphans = inFirstListOnly(pics_json, metas);
        var picsOrphans = pics_json_orphans.stream().map(filename ->
                filename.substring(0, filename.length() - ".json".length())).toList();
        return new Orphans(picsOrphans, new ArrayList<>(metaOrphans));
    }

    private Set<String> inFirstListOnly(Set<String> firstSet, Set<String> otherSet) {
        firstSet = new HashSet<>(firstSet);
        firstSet.removeAll(otherSet); // firstSet 自身改變，因此在此之前需要複製。
        return firstSet;
    }

}

record Orphans(List<String> files, List<String> metas) {
    @Override
    public String toString() {
        if (files.isEmpty() && metas.isEmpty()) {
            return "無孤立檔案。\n";
        }
        var str = "";
        if (!files.isEmpty()) {
            str += "以下圖片檔案缺少對應的 json 檔案:\n";
            str += String.join("\n", files);
            str += "\n";
        }
        if (!metas.isEmpty()) {
            str += "以下 json 檔案沒有對應的圖片檔案:\n";
            str += String.join("\n", metas);
            str += "\n";
        }
        return str;
    }

    public int sum() {
        return files().size() + metas().size();
    }
}
