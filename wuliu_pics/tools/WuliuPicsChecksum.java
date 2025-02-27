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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public class WuliuPicsChecksum implements Runnable{
    private static final long MB = 1 << 20;
    private static final int DAY = 24 * 60 * 60;
    private static final ZoneOffset HongKongTimeZone = ZoneOffset.of("+08:00");
    private static final int listCellWidth = 850;
    private static final int textColumns = 46;

    private static long needCheckUnix;
    static List<String> projects;
    static List<String> projects2;

    private Path projRoot;
    private Path projRoot2;
    private ProjectInfo projInfo;
    private long checkSizeLimit;
    private CheckAlbumResult result;

    private List<Path> albums;

    private JFrame frame;
    private JPanel pane0;
    private JList<String> projList;
    private JTextField currentProjTF;
    private JTextArea msgArea;
    private JScrollPane scrollArea;
    private JButton checkBtn;
    private JButton gotoPane2Btn;

    private JList<String> projList2;
    private JTextField project2TF;
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
        gotoPane2Btn.addActionListener(_ -> gotoPane2());
    }

    private void createGUI() {
        List.of("OptionPane.messageFont", "TextField.font", "List.font",
                "Label.font", "TextArea.font", "Button.font"
        ).forEach(k -> UIManager.put(k, MyUtilGUI.FONT_20));

        frame = new JFrame("Wuliu Pics Checksum");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pane0 = new JPanel(new FlowLayout(FlowLayout.LEFT));

        pane0.add(new JLabel("WuliuPicsChecksum: 檢查檔案是否受損（是否完整）"));
        pane0.add(MyUtilGUI.spacer(800, 5));
        pane0.add(new JLabel("請選擇專案(按兩下):"));
        projList = new JList<>();
        projList.setFixedCellWidth(listCellWidth);
        pane0.add(projList);

        currentProjTF = new JTextField(textColumns);
        currentProjTF.setEditable(false);
        pane0.add(new JLabel("已選擇專案:"));
        pane0.add(currentProjTF);

        msgArea = new JTextArea();
        msgArea.setLineWrap(true);
        scrollArea = MyUtilGUI.verticalScrollPane(msgArea, 850, 400);
        pane0.add(MyUtilGUI.spacer(800, 5));
        pane0.add(scrollArea);

        checkBtn = new JButton("Check");
        checkBtn.setEnabled(false);
        pane0.add(MyUtilGUI.spacer(800, 2));
        pane0.add(checkBtn);

        gotoPane2Btn = new JButton("Repair");
        gotoPane2Btn.setEnabled(false);
        pane0.add(gotoPane2Btn);

        frame.add(BorderLayout.CENTER, pane0);
        frame.setSize(900, 750);
        frame.setLocationRelativeTo(null); // 窗口居中
        frame.setVisible(true);
    }

    private void gotoPane2() {
        pane0.setVisible(false);
        var pane_2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pane_2.add(new JLabel("Repair: 嘗試使用專案2修復專案1中的受損檔案"));
        pane_2.add(MyUtilGUI.spacer(850, 5));
        pane_2.add(new JLabel("專案 1"));
        pane_2.add(currentProjTF);
        frame.add(pane_2);

        projects2 = projects.stream()
                .filter(p ->!projRoot.equals(Path.of(p))).toList();

        pane_2.add(new JLabel("請選擇另一個專案(按兩下):"));
        projList2 = new JList<>(projects2.toArray(new String[0]));
        projList2.setFixedCellWidth(listCellWidth);
        projList2.addMouseListener(new DoubleClickAdapter2());
        pane_2.add(projList2);

        pane_2.add(new JLabel("專案 2"));
        project2TF = new JTextField(textColumns);
        project2TF.setEditable(false);
        pane_2.add(project2TF);

        msgArea.setText("");
        pane_2.add(MyUtilGUI.spacer(850, 5));
        pane_2.add(scrollArea);

        repairBtn = new JButton("Repair");
        // repairBtn.addActionListener(new RepairBtnListener());
        repairBtn.setEnabled(false);
        pane_2.add(MyUtilGUI.spacer(850, 2));
        pane_2.add(repairBtn);
    }

    class DoubleClickAdapter extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
                checkBtn.setEnabled(false);
                gotoPane2Btn.setEnabled(false);
                msgArea.setText("");
                int i = projList.locationToIndex(event.getPoint());
                currentProjTF.setText(projects.get(i));
                projRoot = Path.of(projects.get(i));
                try {
                    projInfo = ProjectInfo.fromJsonFile(projRoot.resolve(MyUtil.PROJECT_JSON));
                    checkSizeLimit = projInfo.checkSizeLimit * MB;
                    var checkInterval = projInfo.checkInterval * DAY;
                    needCheckUnix = Instant.now().getEpochSecond() - checkInterval;
                    var albumsPath = projRoot.resolve(MyUtil.ALBUMS);
                    albums = MyUtil.getAlbums(albumsPath);
                    msgArea.append("Albums in %s%n".formatted(albumsPath));
                    msgArea.append("發現 %d 個相冊。\n\n".formatted(albums.size()));
                    if (albums.isEmpty()) return;
                    checkBtn.setEnabled(true);
                } catch (Exception e) {
                    msgArea.append(e.toString());
                }
            }
        }
    }

    class DoubleClickAdapter2 extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
                repairBtn.setEnabled(false);
                msgArea.setText("");
                int i = projList2.locationToIndex(event.getPoint());
                project2TF.setText(projects2.get(i));
                projRoot2 = Path.of(projects2.get(i));
                if (Files.notExists(projRoot2)) {
                    msgArea.append("[Error] Not Found %s%n".formatted(projRoot2));
                    return;
                }
                msgArea.setText("在專案 1 中發現 %d 個受損檔案%n".formatted(result.damagedFiles.size()));
                msgArea.append("請點擊 Repair 按鈕開始修復\n");
                repairBtn.setEnabled(true);
            }
        }
    }

    class CheckBtnListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            checkBtn.setEnabled(false);
            result = new CheckAlbumResult();
            for (var album : albums) {
                try {
                    msgArea.append("檢查相冊 %s%n".formatted(album));
                    checkAlbum(album, result);
                } catch (Exception ex) {
                    msgArea.append(ex + "\n");
                    JOptionPane.showMessageDialog(frame, "出錯！");
                    return;
                }
            }
            var msg = "\n[檢查結果]\n";
            msg += "這次共檢查 %d 個檔案 (合計 %s)%n".formatted(
                    result.totalN, MyUtil.fileSizeToString(result.totalSize));
            msg += "其中，發現受損檔案 %d 個".formatted(result.damagedFiles.size());
            if (!result.damagedFiles.isEmpty()) {
                gotoPane2Btn.setEnabled(true);
                msg += ":%n%s%n".formatted(String.join("\n", result.damagedFiles));
            }
            msgArea.append(msg);
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
                result.totalN += 1;
                Path picPath = Path.of(picPathStr);
                Path metaPath = Path.of(picPathStr+".json");
                Metadata meta;
                if (Files.exists(metaPath)) {
                    meta = new Metadata().readFromJson(metaPath);
                    if (meta.damaged) {
                        result.damagedFiles.add(picPathStr); // 第一种可能损坏的情形
                        continue;
                    }
                    if (!checkIfNeedToCheck(meta.checked)) {
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
                result.totalSize += picPath.toFile().length();
                if (result.totalSize > checkSizeLimit) {
                    break;
                }
            }
            msgArea.append("\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean checkIfFileDamaged(Path file, Metadata meta) {
        var checksumNow = Metadata.getFileSHA1(file);
        return !meta.checksum.equals(checksumNow);
    }

    private boolean checkIfNeedToCheck(String lastChecked) {
        var needCheckDT = LocalDateTime.ofEpochSecond(needCheckUnix, 0, HongKongTimeZone);
        var needCheckStr = needCheckDT.format(MyUtil.RFC3339);
        return lastChecked.compareTo(needCheckStr) < 0;
    }
}

class CheckAlbumResult {
    int totalN = 0;
    long totalSize = 0L;
    List<String> damagedFiles = new ArrayList<>();
}
