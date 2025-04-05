import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.*;

public class StickyNote {
    
    public static JFrame frame;
    public static JTextArea tArea;
    public static final String FILE_NAME = "data/notes.txt";
    private static final String PASSWORD_FILE = "data/password.dat";
    private static final String SECURITY_FILE = "data/security.dat";
    private static final String ENCRYPTED_NOTES_FILE = "data/notes.enc";
    
    public void frm(){
        frame = new JFrame("Sticky Note");
        frame.setSize(300,300);
        frame.setAlwaysOnTop(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        
        // Add reset button to the frame
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showResetDialog(false);
            }
        });
        
        frame.add(resetButton, BorderLayout.SOUTH);
    }

    private static void showResetDialog(boolean isForgotPassword) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        // Only ask for current password if not coming from "Forgot Password"
        JPasswordField currentPassField = null;
        if (!isForgotPassword) {
            panel.add(new JLabel("Current Password:"));
            currentPassField = new JPasswordField(20);
            panel.add(currentPassField);
        }
        
        panel.add(new JLabel("Parent's Name:"));
        JTextField securityField = new JTextField(20);
        panel.add(securityField);
        
        panel.add(new JLabel("New Password:"));
        JPasswordField newPassField = new JPasswordField(20);
        panel.add(newPassField);
        
        JButton resetBtn = new JButton("Reset Password");
        
        JDialog dialog = new JDialog(frame, "Security Verification", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(resetBtn, BorderLayout.SOUTH);
        dialog.setSize(350, isForgotPassword ? 180 : 220);
        dialog.setLocationRelativeTo(frame);
        
        final JPasswordField finalCurrentPassField = currentPassField;
        resetBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String parentName = securityField.getText().trim();
                char[] newPassword = newPassField.getPassword();
                
                // Verify current password if required
                if (!isForgotPassword) {
                    if (!verifyCurrentPassword(new String(finalCurrentPassField.getPassword()))) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Current password is incorrect!", 
                            "Verification Failed", 
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
                
                if (verifySecurityAnswer(parentName) && newPassword.length > 0) {
                    try {
                        // Save new password
                        BufferedWriter writer = new BufferedWriter(new FileWriter(PASSWORD_FILE));
                        writer.write(new String(newPassword));
                        writer.close();
                        
                        JOptionPane.showMessageDialog(dialog, 
                            "Password has been reset successfully!", 
                            "Reset Complete", 
                            JOptionPane.INFORMATION_MESSAGE);
                        
                        dialog.dispose();
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(dialog, 
                            "Error saving new password", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(dialog, 
                        "Invalid security answer or new password!", 
                        "Verification Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        dialog.setVisible(true);
    }
    
    private static boolean verifyCurrentPassword(String password) {
        try (BufferedReader reader = new BufferedReader(new FileReader(PASSWORD_FILE))) {
            String savedPassword = reader.readLine();
            return password.equals(savedPassword);
        } catch (IOException e) {
            return false;
        }
    }
    
    private static boolean verifySecurityAnswer(String answer) {
        File securityFile = new File(SECURITY_FILE);
        if (!securityFile.exists()) return false;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(SECURITY_FILE))) {
            String savedAnswer = reader.readLine();
            return answer.equalsIgnoreCase(savedAnswer);
        } catch (IOException e) {
            return false;
        }
    }

    public void txt(){
        tArea = new JTextArea() {
            private BufferedImage bg_Image;
            {
                try {
                    bg_Image = ImageIO.read(new File("data/note_bg.png"));
                } catch (IOException e) {
                    System.out.println("Couldn't load background image");
                    bg_Image = null;
                }
            }
            
            @Override
            protected void paintComponent(Graphics g) {
                g.drawImage(bg_Image, 0, 0, getWidth(), getHeight(), this);
                super.paintComponent(g);
            }
        };
        
        tArea.setOpaque(false);
        tArea.setForeground(Color.BLACK);
        tArea.setFont(new Font("Freestyle Script", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(tArea);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    public void load(){
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))){
            tArea.read(reader,null);
        } catch (IOException e) {
            System.out.println("Error loading notes");
        }
    }

    public void save(){
        tArea.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent e){
                try(BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
                    tArea.write(writer);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    private static boolean checkPassword() {
        File passwordFile = new File(PASSWORD_FILE);
        File securityFile = new File(SECURITY_FILE);
        
        if (passwordFile.exists()) {
            String savedPassword = "";
            try (BufferedReader reader = new BufferedReader(new FileReader(PASSWORD_FILE))) {
                savedPassword = reader.readLine();
            } catch (IOException e) {
                System.out.println("Error reading password file");
                return false;
            }
            
            // Create password dialog with reset option
            JPanel panel = new JPanel(new BorderLayout(5, 5));
            JLabel label = new JLabel("Enter password:");
            JPasswordField pass = new JPasswordField(10);
            panel.add(label, BorderLayout.WEST);
            panel.add(pass, BorderLayout.CENTER);
            
            JButton forgotBtn = new JButton("Forgot Password?");
            forgotBtn.addActionListener(e -> {
                showResetDialog(true);
                System.exit(0);
            });
            
            panel.add(forgotBtn, BorderLayout.SOUTH);
            
            int result = JOptionPane.showConfirmDialog(
                null, 
                panel, 
                "Sticky Note - Password Required", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (result == JOptionPane.OK_OPTION) {
                char[] input = pass.getPassword();
                return new String(input).equals(savedPassword);
            }
            
            return false;
        } else {
            // First-time setup - create password and security question
            JPanel passwordPanel = new JPanel(new BorderLayout(5, 5));
            JLabel passLabel = new JLabel("Create new password:");
            JPasswordField passField = new JPasswordField(10);
            passwordPanel.add(passLabel, BorderLayout.WEST);
            passwordPanel.add(passField, BorderLayout.CENTER);
            
            JPanel securityPanel = new JPanel(new BorderLayout(5, 5));
            JLabel securityLabel = new JLabel("Enter parent's name (mother/father):");
            JTextField securityField = new JTextField(10);
            securityPanel.add(securityLabel, BorderLayout.WEST);
            securityPanel.add(securityField, BorderLayout.CENTER);
            
            JPanel mainPanel = new JPanel(new GridLayout(2, 1));
            mainPanel.add(passwordPanel);
            mainPanel.add(securityPanel);
            
            int result = JOptionPane.showConfirmDialog(
                null, 
                mainPanel, 
                "Sticky Note - Initial Setup", 
                JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (result == JOptionPane.OK_OPTION) {
                char[] password = passField.getPassword();
                String securityAnswer = securityField.getText().trim();
                
                if (password.length > 0 && !securityAnswer.isEmpty()) {
                    try {
                        // Save password
                        BufferedWriter writer = new BufferedWriter(new FileWriter(PASSWORD_FILE));
                        writer.write(new String(password));
                        writer.close();
                        
                        // Save security answer
                        writer = new BufferedWriter(new FileWriter(SECURITY_FILE));
                        writer.write(securityAnswer);
                        writer.close();
                        
                        return true;
                    } catch (IOException e) {
                        System.out.println("Error creating password/security files");
                        return false;
                    }
                }
            }
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        // Create data directory if it doesn't exist
        new File("data").mkdirs();
        
        // Show password dialog first
        if (checkPassword()) {
            StickyNote obj = new StickyNote();
            obj.frm();
            obj.txt();
            obj.save();
            obj.load();
            frame.setVisible(true);
        } else {
            System.exit(0);
        }
    }
}