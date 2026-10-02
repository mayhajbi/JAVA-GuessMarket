package gm.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

/**
 * Adds the events of an uploaded XML file ({@code /upload}) on behalf of the user of the session.
 * The file never touches the disk: the threshold equals the maximal size, so the container keeps the
 * whole part in memory, and the content goes from the part straight to the engine, never through
 * {@code Part.write}.
 */
@MultipartConfig(fileSizeThreshold = UploadServlet.MAX_FILE_SIZE, maxFileSize = UploadServlet.MAX_FILE_SIZE,
        maxRequestSize = UploadServlet.MAX_FILE_SIZE)
public class UploadServlet extends GmServlet {

    static final int MAX_FILE_SIZE = 1024 * 1024;

    @Override
    protected void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        requirePost(request);
        String username = requireUsername(request);
        Part file = readFilePart(request);
        try (InputStream content = file.getInputStream()) {
            ServletUtils.writeJson(response,
                    ServletUtils.getEngine(getServletContext()).uploadEvents(username, file.getSubmittedFileName(), content));
        }
    }

    private static Part readFilePart(HttpServletRequest request) throws IOException {
        try {
            for (Part part : request.getParts()) {
                if (part.getSubmittedFileName() != null) {
                    return part;
                }
            }
        } catch (ServletException e) {
            throw new BadRequestException("The request must be a multipart upload with one file.");
        } catch (IllegalStateException e) {
            throw new BadRequestException("The file is too large. The maximum size is 1MB.");
        }
        throw new BadRequestException("The request has no file. Send the file in a form field.");
    }
}
