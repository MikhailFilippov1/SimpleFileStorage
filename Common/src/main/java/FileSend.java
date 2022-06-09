
public class FileSend extends AbstractMessage {
    private String filename;

    public String getFilename() {
        return filename;
    }

    public FileSend(String filename) {
        this.filename = filename;
    }
}
