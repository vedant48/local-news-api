package localnews_backend.exception;

/**
 * Thrown when the supplied URL is not a recognisable YouTube video URL.
 * Resolves to HTTP 400 Bad Request in {@link GlobalExceptionHandler}.
 */
public class InvalidYoutubeUrlException extends RuntimeException {

    public InvalidYoutubeUrlException(String message) {
        super(message);
    }
}
