package wuliu_pics.tools;

import wuliu_pics.common.Metadata;
import wuliu_pics.common.MyUtil;
import wuliu_pics.common.MyUtilGUI;
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

public class WuliuPicsChecksum implements Runnable{
    private static final long MB = 1 << 20;
    static List<String> projects;

    private Path projRoot;
    private ProjectInfo projInfo;
    private long checkSizeLimit;

    private List<Path> albums;

    private JFrame frame;
    private JList<String> projList;
    private JTextField currentProjTF;
    private JTextArea msgArea;
    private JButton checkBtn;
    private JButton repairBtn;

    public static void main(String[] args) throws IOException {
        projects = ProjectInfo.fromJsonFile(MyUtil.PROJ_INFO_PATH).projects;
        projects = projects.stream().map(proj ->
                        Path.of(proj).toAbsolutePath().normalize().toString()).toList();
        SwingUtilities.invokeLater(new WuliuPicsChecksum());
    }

    @Override
    public void run() {
        createGUI();
        projList.setListData(projects.toArray(new String[0]));
        projList.addMouseListener(new DoubleClickAdapter());
        checkBtn.addActionListener(new CheckBtnListener());
    }

    private void createGUI() {
        List.of("OptionPane.messageFont", "TextField.font", "List.font",
                "Label.font", "TextArea.font", "Button.font"
        ).forEach(k -> UIManager.put(k, MyUtilGUI.FONT_20));

        frame = new JFrame("Wuliu Pics Checksum");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        var pane0 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        pane0.add(new JLabel("WuliuPicsChecksum: 檢查檔案是否受損（是否完整）"));
        pane0.add(MyUtilGUI.spacer(800, 5));
        pane0.add(new JLabel("請選擇專案(按兩下):"));
        projList = new JList<>();
        projList.setFixedCellWidth(850);
        pane0.add(projList);

        currentProjTF = new JTextField(46);
        currentProjTF.setEditable(false);
        pane0.add(new JLabel("已選擇專案:"));
        pane0.add(currentProjTF);

        msgArea = new JTextArea();
        msgArea.setLineWrap(true);
        var scrollArea = MyUtilGUI.verticalScrollPane(msgArea, 850, 400);
        pane0.add(MyUtilGUI.spacer(800, 5));
        pane0.add(scrollArea);

        checkBtn = new JButton("Check");
        checkBtn.setEnabled(false);
        pane0.add(MyUtilGUI.spacer(800, 2));
        pane0.add(checkBtn);

        repairBtn = new JButton("Repair");
        repairBtn.setEnabled(false);
        pane0.add(repairBtn);

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
                repairBtn.setEnabled(false);
                msgArea.setText("");
                int i = projList.locationToIndex(event.getPoint());
                currentProjTF.setText(projects.get(i));
                projRoot = Path.of(projects.get(i));
                try {
                    projInfo = ProjectInfo.fromJsonFile(projRoot.resolve(MyUtil.PROJECT_JSON));
                    checkSizeLimit = projInfo.checkSizeLimit * MB;
                    var albumsPath = projRoot.resolve(MyUtil.ALBUMS);
                    albums = MyUtil.getAlbums(albumsPath);
                    msgArea.append("Albums in %s%n".formatted(albumsPath));
                    msgArea.append("發現 %d 個相冊。\n".formatted(albums.size()));
                    if (albums.isEmpty()) return;
                    checkBtn.setEnabled(true);
                } catch (Exception e) {
                    msgArea.append(e.toString());
                }
            }
        }
    }

    class CheckBtnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkBtn.setEnabled(false);
            var result = new CheckAlbumResult();
            for (var album : albums) {
                try {
                    msgArea.append("%n檢查相冊 %s%n".formatted(album));
                    checkAlbum(album, result);
                } catch (Exception ex) {
                    msgArea.append(ex + "\n");
                    JOptionPane.showMessageDialog(frame, "出錯！");
                    return;
                }
            }
        }
    }

    /**
     * result 內的值會被修改
     */
    private void checkAlbum(Path album, CheckAlbumResult result) {
        try (var files = Files.list(album)) {
            var pics = files.map(Path::toString).filter(f -> !f.endsWith(".json")).toList();
            for (var picPathStr : pics) {
                msgArea.append(".");
                Path picPath = Path.of(picPathStr);
                Path metaPath = Path.of(picPathStr+".json");
                Metadata meta;
                if (Files.exists(metaPath)) {
                    meta = new Metadata().readFromJson(metaPath);
                    if (meta.damaged) {
                        result.damagedFiles.add(picPathStr); // 第一种可能损坏的情形
                        continue;
                    }
                    meta.damaged = checkIfFileDamaged(picPath, meta);
                    if (meta.damaged) {
                        result.damagedFiles.add(picPathStr); // 第二种可能损坏的情形
                    }
                    meta.checked = MyUtil.timeNowRFC3339();
                } else {
                    meta = new Metadata(picPath);            // 从这里开始不可能损坏
                }
                meta.writeToJson(metaPath.toFile());
                result.totalN += 1;
                result.totalSize += picPath.toFile().length();
                if (result.totalSize > checkSizeLimit) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkIfFileDamaged(Path file, Metadata meta) {
        var checksumNow = Metadata.getFileSHA1(file);
        return !meta.checksum.equals(checksumNow);
    }

}

class CheckAlbumResult {
    int totalN = 0;
    long totalSize = 0L;
    List<String> damagedFiles = new ArrayList<>();
}
