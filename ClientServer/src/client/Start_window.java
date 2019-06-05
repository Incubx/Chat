package client;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Start_window extends JFrame {
    private JTextField adress;
    private JButton connect_button;
    private JPanel mainPanel;
    private JTextField Nick;
    private ClientWindow clientWindow;

    public Start_window(String ip,String NickName) {

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.setContentPane(mainPanel);

        this.setBounds(300,200,500,200);
        Nick.setText(NickName);
        adress.setText(ip);
        setVisible(true);
        connect_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip =adress.getText();
                String Nickname = Nick.getText();
                if(!Nickname.isEmpty()&&!ip.isEmpty()) {
                    dispose();
                    clientWindow = new ClientWindow(ip, Nickname);

                }

            }
        });
    }
}


