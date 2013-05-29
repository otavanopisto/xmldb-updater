package fi.internetix.updater.ui;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class VisualView extends JFrame {
  
  private static final long serialVersionUID = 1L;

  public VisualView(String title, int width, int height) {
    super();
    
    setTitle(title);
    setSize(width, height);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
  }

  protected JPanel createLabeledComponent(String label, JComponent component) {
    JPanel panel = new JPanel(new BorderLayout());
    JLabel labelComponent = new JLabel(label);
    
    panel.add(labelComponent, BorderLayout.NORTH);
    panel.add(component, BorderLayout.CENTER);
    
    return panel;
  }
  
  protected void hideUi() {
    dispose();
  }
  
  protected void showUi() {
    setVisible(true);
  }
  
}
