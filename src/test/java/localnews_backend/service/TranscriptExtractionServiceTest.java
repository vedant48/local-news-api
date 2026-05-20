package localnews_backend.service;

import localnews_backend.exception.TranscriptExtractionException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptExtractionServiceTest {

    @Test
    void mapsYoutubeBotCheckErrorToHelpfulMessage() throws Exception {
        TranscriptExtractionService service = new TranscriptExtractionService();
        Method method = TranscriptExtractionService.class
                .getDeclaredMethod("createYtDlpFailure", int.class, String.class);
        method.setAccessible(true);

        TranscriptExtractionException exception = (TranscriptExtractionException) method.invoke(
                service,
                1,
                "ERROR: [youtube] abc123: Sign in to confirm you’re not a bot"
        );

        assertEquals(
                "YouTube blocked automated access for this video (bot-check). " +
                        "Try another video, or configure yt-dlp authentication cookies.",
                exception.getMessage()
        );
    }

    @Test
    void mapsMissingSubtitleOutputToNoSubtitlesMessage() throws Exception {
        TranscriptExtractionService service = new TranscriptExtractionService();
        Method method = TranscriptExtractionService.class
                .getDeclaredMethod("createYtDlpFailure", int.class, String.class);
        method.setAccessible(true);

        TranscriptExtractionException exception = (TranscriptExtractionException) method.invoke(
                service,
                1,
                "WARNING: no subtitles"
        );

        assertEquals("No English subtitles are available for this video", exception.getMessage());
    }

    @Test
    void extractsYoutubeIdFromShareUrl() throws Exception {
        TranscriptExtractionService service = new TranscriptExtractionService();
        Method method = TranscriptExtractionService.class
                .getDeclaredMethod("extractYoutubeId", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Optional<String> result = (Optional<String>) method.invoke(
                service,
                "https://youtu.be/vtO_BoysKfE?si=dUYL771sHE6ruEcB"
        );

        assertTrue(result.isPresent());
        assertEquals("vtO_BoysKfE", result.get());
    }

    @Test
    void parsesTimedTextXmlIntoTranscript() throws Exception {
        TranscriptExtractionService service = new TranscriptExtractionService();
        Method method = TranscriptExtractionService.class
                .getDeclaredMethod("parseTimedTextXml", String.class);
        method.setAccessible(true);

        String transcript = (String) method.invoke(
                service,
                "<transcript><text start=\"0\">Hello%20world</text><text start=\"1\">Tom &amp; Jerry</text></transcript>"
        );

        assertEquals("Hello world Tom & Jerry", transcript);
    }
}
