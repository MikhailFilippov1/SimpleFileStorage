import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends AbstractMessage {
    String filename;
    int partNumber;
    int partsCount;
    byte[] data;

    public String getFilename() {
        return filename;
    }

    public byte[] getData() {
        return data;
    }

    public FileMessage(Path path, int partNumber, int partsCount, byte[] data) {
        filename = path.getFileName().toString();
        this.partNumber = partNumber;
        this.partsCount = partsCount;
        //data = Files.readAllBytes(path);
        this.data = data;
    }
}



