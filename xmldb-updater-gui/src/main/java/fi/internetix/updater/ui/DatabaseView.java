package fi.internetix.updater.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.hibernate.dialect.Dialect;

import fi.internetix.updater.core.Database;

public class DatabaseView extends VisualView {
  
  private static final long serialVersionUID = 1L;
  
  public DatabaseView(DatabaseViewController controller) {
    super("Database connection", 350, 230);
    
    this.controller = controller;
    
    buildUi();
    attachListeners();
  }
    
  private void attachListeners() {
    this.databaseCombobox.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getItem() instanceof Database) {
          Database database = ((Database) e.getItem());
          if (database.getDialect() != null) {
            databaseUrl.setText(database.getDefaultUrl());
            connectButton.setEnabled(true);
            return;
          }
        }
        connectButton.setEnabled(false);
      }
    });
    
    this.connectButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          controller.openUpdater();
        } catch (Exception e1) {
          JOptionPane.showMessageDialog(null, e1.getMessage());
        }
      }
    });
  }
  
  private void buildUi() {
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
    
    JPanel dbPanel = new JPanel(new GridLayout(4, 1)); 
    
    databaseCombobox = new JComboBox();
    
    databaseCombobox.addItem("Select...");
    for (Database database : Database.values()) {
      databaseCombobox.addItem(database);
    }
    dbPanel.add(createLabeledComponent("Database vendor", databaseCombobox));
    
    databaseUrl = new JTextField();
    dbPanel.add(createLabeledComponent("Database URL", databaseUrl));
    
    databaseUsername = new JTextField(); 
    dbPanel.add(createLabeledComponent("Database username", databaseUsername));
    
    databasePassword = new JPasswordField();
    dbPanel.add(createLabeledComponent("Database password", databasePassword));
    
    mainPanel.add(dbPanel, BorderLayout.CENTER);
    
    connectButton = new JButton("Connect");
    connectButton.setEnabled(false);
    mainPanel.add(connectButton, BorderLayout.SOUTH);
    
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(mainPanel, BorderLayout.CENTER);
    
    center();
  }

  public String getDatabasePassword() {
    return new String(databasePassword.getPassword());
  }

  public String getDatabaseUrl() {
    return databaseUrl.getText();
  }

  public String getDatabaseUsername() {
    return databaseUsername.getText();
  }

  public Database getSelectedDatabase() {
    return (Database) this.databaseCombobox.getSelectedItem();
  }
  
  public void setDatabaseVendor(String vendor) {
    Database database = Enum.valueOf(Database.class, vendor);
    if (database != null)
      databaseCombobox.setSelectedItem(database); 
  }
  
  public void setDatabaseUrl(String url) {
    if (!StringUtils.isBlank(url))
      databaseUrl.setText(url);
  }
  
  public void setDatabaseUsername(String username) {
    if (!StringUtils.isBlank(username))
      databaseUsername.setText(username);
  }
  
  public void setDatabasePassword(String password) {
    if (!StringUtils.isBlank(password))
      databasePassword.setText(password);
  }
  
  public Class<Dialect> getDialect() {
  	Database database = ((Database) databaseCombobox.getSelectedItem());
  	return database.getDialect();
  }
  
  private void center() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension size = getSize();
    screenSize.height = screenSize.height / 2;
    screenSize.width = screenSize.width / 2;
    size.height = size.height / 2;
    size.width = size.width / 2;
    int y = screenSize.height - size.height;
    int x = screenSize.width - size.width;
    setLocation(x, y);
  }
  
  private DatabaseViewController controller;
  private JComboBox databaseCombobox;
  private JPasswordField databasePassword;
  private JTextField databaseUrl;
  private JTextField databaseUsername;
  private JButton connectButton;
}
