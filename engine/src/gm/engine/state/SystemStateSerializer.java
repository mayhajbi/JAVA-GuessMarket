package gm.engine.state;

import gm.engine.core.GuessMarket;
import gm.engine.exception.SystemStateFileException;
import gm.engine.util.InputText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Saves the state of the system to a file and reads it back.
 * <p>
 * The state is written as a plain java serialized object, so every core object of the engine is
 * serializable. The user supplies a path without an extension, and the fixed
 * {@value #STATE_EXTENSION} extension is added by this class.
 */
public class SystemStateSerializer {

    private static final String STATE_EXTENSION = ".gm";

    public String save(GuessMarket market, String rawPath) {
        Path file = toStateFilePath(rawPath);
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(market);
        } catch (IOException exception) {
            throw new SystemStateFileException("The state of the system could not be saved to ["
                    + file + "]. Reason: " + exception.getMessage(), exception);
        }
        return file.toString();
    }

    public GuessMarket load(String rawPath) {
        Path file = toStateFilePath(rawPath);
        if (!Files.isRegularFile(file)) {
            throw new SystemStateFileException("The state file [" + file + "] does not exist. Please "
                    + "enter the path of a file that was saved by this system, without its "
                    + STATE_EXTENSION + " extension.");
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
            Object content = in.readObject();
            if (!(content instanceof GuessMarket)) {
                throw new SystemStateFileException("The file [" + file + "] does not contain a Guess "
                        + "Market saved state.");
            }
            return (GuessMarket) content;
        } catch (ClassNotFoundException | IOException exception) {
            throw new SystemStateFileException("The state of the system could not be loaded from ["
                    + file + "]. Reason: " + exception.getMessage(), exception);
        }
    }

    /**
     * The actual file that is used for the given path, after adding the state file extension.
     */
    public String resolveStateFilePath(String rawPath) {
        return toStateFilePath(rawPath).toString();
    }

    private Path toStateFilePath(String rawPath) {
        String path = InputText.cleanPath(rawPath);
        if (path.isEmpty()) {
            throw new SystemStateFileException("No file path was given. Please enter the full path of "
                    + "the file, without an extension.");
        }
        if (InputText.hasExtension(path, STATE_EXTENSION)) {
            path = path.substring(0, path.length() - STATE_EXTENSION.length());
        }
        try {
            return Paths.get(path + STATE_EXTENSION);
        } catch (InvalidPathException exception) {
            throw new SystemStateFileException(InputText.illegalPathMessage(rawPath));
        }
    }
}
