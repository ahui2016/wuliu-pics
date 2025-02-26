package wuliu_pics.common;

import javax.swing.*;
import java.awt.*;

public class MyUtilGUI {
    public static final Font FONT_20 = new Font(Font.SANS_SERIF, Font.PLAIN, 20);

    public static Component spacer(int width, int height) {
        return Box.createRigidArea(new Dimension(width, height));
    }

    public static JScrollPane verticalScrollPane(Component comp, int width, int height) {
        var scrollArea = new JScrollPane(comp,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollArea.setPreferredSize(new Dimension(width, height));
        return scrollArea;
    }

}
