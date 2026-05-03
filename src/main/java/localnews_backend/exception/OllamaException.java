package localnews_backend.exception;

/**
 * Thrown when the Ollama process cannot be started, times out, or exits with
 * a non-zero status code.
 * Resolves to HTTP 503 Service Unavailable in {@link GlobalExceptionHandler}.
 */
public class OllamaException extends RuntimeException {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
