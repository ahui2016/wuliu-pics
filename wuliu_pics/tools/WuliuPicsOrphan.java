package wuliu_pics.tools;

import wuliu_pics.common.MyUtil;
import wuliu_pics.common.ProjectInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WuliuPicsOrphan implements Runnable{
    static ProjectInfo projInfo;

    private Path projRoot;

    private JFrame frame;
    private JList<String> projList;
    private JTextField currentProjTF;
    private JTextArea msgArea;
    private JButton checkBtn;

    public static void main(String[] args) throws IOException {
        projInfo = ProjectInfo.fromJsonFile(MyUtil.PROJ_INFO_PATH);
        SwingUtilities.invokeLater(new WuliuPicsOrphan());
    }

    @Override
    public void run() {
        createGUI();
        projList.setListData(projInfo.projects.toArray(new String[0]));
        projList.addMouseListener(new DoubleClickAdapter());
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
                msgArea.setText("");
                int i = projList.locationToIndex(event.getPoint());
                projRoot = Path.of(projInfo.projects.get(i)).toAbsolutePath().normalize();
                currentProjTF.setText(projRoot.toString());
                try {
                    var albums = MyUtil.getAlbums(projRoot);
                    checkAlbums(albums, projRoot);
                } catch (Exception e) {
                    msgArea.append(e.getMessage());
                    return;
                }
            }
        }
    }

    private void checkAlbums(List<Path> albums, Path projRoot) {
        var albumsPath = projRoot.resolve(MyUtil.ALBUMS);
        msgArea.append("Albums in %s%n".formatted(albumsPath));
        try (var files = Files.list(albumsPath)) {
            var orphans = files.filter(Files::isRegularFile).toList();
            if (!orphans.isEmpty()) {
                msgArea.append(
                        "在 %s 的第一層子目錄中不允許出現普通檔案（非資料夾）：%n".formatted(albumsPath));
                var orphansStrList = orphans.stream().map(Path::toString).toList();
                var msg = String.join("\n", orphansStrList);
                msgArea.append(msg);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (var album : albums) {
            try (var files = Files.list(album)) {
                var folders = files.filter(Files::isDirectory).toList();
                if (!folders.isEmpty()) {
                    msgArea.append("在各個相冊中不允許包含子資料夾：\n");
                    var foldersStrList = folders.stream().map(Path::toString).toList();
                    var msg = String.join("\n", foldersStrList);
                    msgArea.append(msg);
                    return;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        checkBtn.setEnabled(true);
    }
}
