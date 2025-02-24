package wuliu_pics.tools;

import wuliu_pics.common.MyUtil;
import wuliu_pics.common.ProjectInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class WuliuPicsOrphan implements Runnable{
    static ProjectInfo projInfo;

    private Path projRoot;
    private List<Path> albums;
    private Orphans allOrphans;

    private JFrame frame;
    private JList<String> projList;
    private JTextField currentProjTF;
    private JTextArea msgArea;
    private JButton checkBtn;
    private JButton fixBtn;

    public static void main(String[] args) throws IOException {
        projInfo = ProjectInfo.fromJsonFile(MyUtil.PROJ_INFO_PATH);
        SwingUtilities.invokeLater(new WuliuPicsOrphan());
    }

    @Override
    public void run() {
        createGUI();
        projList.setListData(projInfo.projects.toArray(new String[0]));
        projList.addMouseListener(new DoubleClickAdapter());
        checkBtn.addActionListener(new CheckBtnListener());
    }

    private void createGUI() {
        List.of("OptionPane.messageFont", "TextField.font", "List.font",
                "Label.font", "TextArea.font", "Button.font"
        ).forEach(k -> UIManager.put(k, MyUtil.FONT_20));

        frame = new JFrame("Wuliu Pics Orphan");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var pane0 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        pane0.add(new JLabel("WuliuPicsOrphan: 尋找孤立的圖片或JSON檔案"));
        pane0.add(Box.createRigidArea(new Dimension(800, 5)));
        pane0.add(new JLabel("請選擇專案(按兩下):"));
        projList = new JList<>();
        projList.setFixedCellWidth(850);
        pane0.add(projList);

        currentProjTF = new JTextField(45);
        currentProjTF.setEditable(false);
        pane0.add(new JLabel("已選擇專案:"));
        pane0.add(currentProjTF);

        msgArea = new JTextArea();
        msgArea.setLineWrap(true);
        var scrollArea = new JScrollPane(msgArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane0.add(Box.createRigidArea(new Dimension(800, 5)));
        scrollArea.setPreferredSize(new Dimension(850, 400));
        pane0.add(scrollArea);

        checkBtn = new JButton("Check");
        checkBtn.setEnabled(false);
        pane0.add(Box.createRigidArea(new Dimension(800, 2)));
        pane0.add(checkBtn);

        fixBtn = new JButton("Fix");
        fixBtn.setEnabled(false);
        pane0.add(Box.createRigidArea(new Dimension(500, 2)));
        pane0.add(fixBtn);

        frame.add(BorderLayout.CENTER, pane0);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null); // 窗口居中
        frame.setVisible(true);
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
                checkBtn.setEnabled(false);
                fixBtn.setEnabled(false);
                msgArea.setText("");
                int i = projList.locationToIndex(event.getPoint());
                projRoot = Path.of(projInfo.projects.get(i)).toAbsolutePath().normalize();
                currentProjTF.setText(projRoot.toString());
                try {
                    var albumsPath = projRoot.resolve(MyUtil.ALBUMS);
                    albums = MyUtil.getAlbums(albumsPath);
                    msgArea.append("Albums in %s%n".formatted(albumsPath));
                    msgArea.append("發現 %d 個相冊。\n".formatted(albums.size()));
                    if (albums.isEmpty()) return;
                    checkAlbums(albumsPath);
                } catch (Exception e) {
                    msgArea.append(e.getMessage());
                }
            }
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

    private void checkAlbums(Path albumsPath) {
        try (var files = Files.list(albumsPath)) {
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
            msgArea.append(e.getMessage());
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
                msgArea.append(e.getMessage());
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
            msgArea.append(e.getMessage());
            return null;
        }
        var pics_json_orphans = inFirstListOnly(pics_json, metas);
        var picsOrphans = pics_json_orphans.stream().map(filename ->
                filename.substring(0, filename.length() - ".json".length())).toList();
        var metaOrphans = inFirstListOnly(metas, pics_json);
        return new Orphans(picsOrphans, new ArrayList<>(metaOrphans));
    }

    private Set<String> inFirstListOnly(Set<String> firstSet, Set<String> otherSet) {
        firstSet.removeAll(otherSet);
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
        }
        if (!metas.isEmpty()) {
            str += "\n以下 json 檔案沒有對應的圖片檔案:\n";
            str += String.join("\n", metas);
        }
        return str + "\n";
    }

    public int sum() {
        return files().size() + metas().size();
    }
}
