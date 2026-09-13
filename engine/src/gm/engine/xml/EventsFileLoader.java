package gm.engine.xml;

import gm.engine.core.GuessMarket;
import gm.engine.exception.InvalidFilePathException;
import gm.engine.exception.XmlParsingException;
import gm.engine.util.InputText;
import gm.engine.xml.generated.XmlGuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Loads a Guess Market data file into the objects of the engine.
 * <p>
 * This class is responsible for the technical part of the loading: the path itself, reading the file
 * and converting it with JAXB. The logical validations of the content are done by
 * {@link EventsMapper}.
 */
public class EventsFileLoader {

    private static final String XML_EXTENSION = ".xml";

    /**
     * Reads and validates the given file and builds a new system out of it.
     *
     * @param rawPath full path of the XML file, as it was given by the user
     * @return a new system that contains all the events of the file
     */
    public GuessMarket loadFile(String rawPath) {
        String path = InputText.cleanPath(rawPath);
        Path file = validatePath(path);
        XmlGuessMarket xmlSystem = readXmlFile(file, path);
        return new EventsMapper().toGuessMarket(xmlSystem);
    }

    private Path validatePath(String path) {
        if (path.isEmpty()) {
            throw new InvalidFilePathException("No file path was given. Please enter the full path "
                    + "of the XML file you would like to load.");
        }
        if (!path.toLowerCase(Locale.ROOT).endsWith(XML_EXTENSION)) {
            throw new InvalidFilePathException("The path [" + path + "] does not point to an XML "
                    + "file. The file name must end with the .xml extension.");
        }

        Path file;
        try {
            file = Paths.get(path);
        } catch (InvalidPathException exception) {
            throw new InvalidFilePathException("The path [" + path + "] is not a legal file path on "
                    + "this computer. Please check it and try again.");
        }
        if (!Files.exists(file)) {
            throw new InvalidFilePathException("The file [" + path + "] does not exist. Please check "
                    + "the path and try again.");
        }
        if (!Files.isRegularFile(file)) {
            throw new InvalidFilePathException("The path [" + path + "] does not point to a file "
                    + "(it may be a folder). Please enter the full path of the XML file itself.");
        }
        if (!Files.isReadable(file)) {
            throw new InvalidFilePathException("The file [" + path + "] cannot be read. Please make "
                    + "sure it is not open in another program and that you have permission to read "
                    + "it.");
        }
        return file;
    }

    private XmlGuessMarket readXmlFile(Path file, String path) {
        try (InputStream fileStream = Files.newInputStream(file)) {
            JAXBContext context = JAXBContext.newInstance(XmlGuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object content = unmarshaller.unmarshal(fileStream);
            if (!(content instanceof XmlGuessMarket)) {
                throw new XmlParsingException(path,
                        "the root element of the file is not <Guess-Market>", null);
            }
            return (XmlGuessMarket) content;
        } catch (JAXBException exception) {
            throw new XmlParsingException(path, describe(exception), exception);
        } catch (IOException exception) {
            throw new XmlParsingException(path, "the file could not be read ("
                    + exception.getMessage() + ")", exception);
        }
    }

    /**
     * Extracts the most detailed message available out of a JAXB failure.
     */
    private String describe(JAXBException exception) {
        Throwable cause = exception.getLinkedException() != null
                ? exception.getLinkedException()
                : exception;
        String message = cause.getMessage();
        return message == null || message.isBlank()
                ? "the file is not a valid XML document"
                : message;
    }
}
