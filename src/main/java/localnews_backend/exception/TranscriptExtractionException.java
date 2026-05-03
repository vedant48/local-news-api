package localnews_backend.exception;

/**
 * Thrown when yt-dlp fails, times out, or no subtitle track is available.
 * Resolves to HTTP 422 in {@link GlobalExceptionHandler}.
 */
public class TranscriptExtractionException extends RuntimeException {

    public TranscriptExtractionException(String message) {
        super(message);
    }

    public TranscriptExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
