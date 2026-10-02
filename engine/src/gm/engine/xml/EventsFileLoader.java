package gm.engine.xml;

import gm.engine.core.Event;
import gm.engine.exception.InvalidFilePathException;
import gm.engine.exception.XmlParsingException;
import gm.engine.util.InputText;
import gm.engine.xml.generated.XmlGuessMarket;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.InputStream;
import java.util.List;

/**
 * Loads a Guess Market data file into the objects of the engine.
 * <p>
 * This class is responsible for the technical part of the loading: the name of the file, reading its
 * content and converting it with JAXB. The logical validations of the content are done by
 * {@link EventsMapper}.
 */
public class EventsFileLoader {

    private static final String XML_EXTENSION = ".xml";

    /**
     * Reads and validates the content of a file of events, and builds its events. The file is read
     * from the stream and is never written anywhere.
     *
     * @param content  the content of the file; the caller closes it
     * @param fileName the name of the file, as the user knows it, for the messages
     * @return the events of the file, in order, without a market maker and without an id
     */
    public List<Event> loadEvents(InputStream content, String fileName) {
        String name = InputText.cleanPath(fileName);
        requireXmlName(name);
        return new EventsMapper().toEvents(readXml(content, name));
    }

    private void requireXmlName(String name) {
        if (name.isEmpty()) {
            throw new InvalidFilePathException("No file was selected. Choose an XML file to upload.");
        }
        if (!InputText.hasExtension(name, XML_EXTENSION)) {
            throw new InvalidFilePathException("The file '" + name + "' is not an XML file. The file name "
                    + "must end with the .xml extension.");
        }
    }

    private XmlGuessMarket readXml(InputStream stream, String fileName) {
        try {
            JAXBContext context = JAXBContext.newInstance(XmlGuessMarket.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Object content = unmarshaller.unmarshal(stream);
            if (!(content instanceof XmlGuessMarket)) {
                throw new XmlParsingException(fileName,
                        "the root element of the file is not 'Guess-Market'.", null);
            }
            return (XmlGuessMarket) content;
        } catch (JAXBException exception) {
            throw new XmlParsingException(fileName, describe(exception), exception);
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
