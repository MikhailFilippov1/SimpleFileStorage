import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

public class MainController implements Initializable {

    @FXML
    TextField sendFileName;

    @FXML
    TextField tfFileName;

    @FXML
    ListView<String> filesList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CountDownLatch cdl = new CountDownLatch(1);
        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    AbstractMessage input = Network.readObject();
                    if(input instanceof FileMessage) {              // Прием и запись файла на стороне Client
                        FileMessage fm = (FileMessage) input;
                        boolean append = true;
                        if (fm.partsCount == 1) {
                            append = false;
                        }
                        System.out.println(fm.partNumber + " / " + fm.partsCount);
                        FileOutputStream fos = new FileOutputStream("client_storage/" + fm.filename, append);
                        fos.write(fm.data);
                        fos.close();
                        if (fm.partNumber == fm.partsCount) {
                        cdl.countDown();
//                            break;
                        }
                    }
            }
//                refreshLocalFilesList();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();

        refreshLocalFilesList();

        filesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if(newValue != null) {
                    sendFileName.setText(newValue);
                }
            }
        });
    }

    public void pressOnDownloadBtn(ActionEvent actionEvent) {
        if (tfFileName.getLength() > 0) {
            Network.sendMsg(new FileRequest(tfFileName.getText()));
            tfFileName.clear();
        }
    }

    public void pressOnSendBtn(ActionEvent actionEvent) throws IOException {
        System.out.println(sendFileName.getText());
        Network.sendMsg(new FileSend(sendFileName.getText()));
        sendFileMethod(sendFileName.getText());
        sendFileName.clear();
    }

    public void mockAction(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Sorry");
        alert.setHeaderText(null);
        alert.setContentText("Method under reconstruction");

        alert.showAndWait();
    }

    public void refreshLocalFilesList() {
        Platform.runLater(() -> {
            try {
                filesList.getItems().clear();
                Files.list(Paths.get("client_storage"))
                        .filter(p -> !Files.isDirectory(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(o -> filesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendFileMethod(String fileName) throws IOException {           //Отправка файла серверу

            System.out.println("File send block starting ...");
            File file = new File("client_storage/" + fileName);
            Path path = Paths.get("client_storage/" + fileName);
            int bufSize = 1024 * 1024 * 1;
            int partsCount = new Long(file.length() / bufSize).intValue();
            if (file.length() % bufSize != 0) {
                partsCount++;
            }
            FileMessage sf = new FileMessage(path, -1, partsCount, new byte[bufSize]);
            FileInputStream in = new FileInputStream("client_storage/" + fileName);
            for (int i = 0; i < partsCount; i++) {
                int readedBytes = in.read(sf.data);
                sf.partNumber = i + 1;
                if (readedBytes < bufSize) {
                    sf.data = Arrays.copyOfRange(sf.data, 0, readedBytes);
                }
                Network.sendMsg(sf);
                System.out.println("Отправлена часть #" + (i + 1));
            }
            in.close();
    }
}


