import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class Window extends JFrame {

    private JPanel mainPanel;

    public Window() {

        setContentPane(mainPanel);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Сервер");
        setVisible(true);
        setBounds(0,0,400,100);
        Server server = new Server();


    }
}
