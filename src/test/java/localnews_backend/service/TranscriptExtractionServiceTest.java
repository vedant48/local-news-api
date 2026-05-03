package localnews_backend.service;

import localnews_backend.exception.TranscriptExtractionException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
