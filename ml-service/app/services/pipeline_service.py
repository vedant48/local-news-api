from app.services.transcript_service import extract_transcript
from app.services.llm_service import generate_news_report


def run_pipeline(url: str) -> dict:
    """
    Orchestrates full pipeline:
    URL → Transcript → LLM → Report
    """

    try:
        # Extract transcript
        transcript = extract_transcript(url)

        #Handle failure case
        if not transcript or "Transcript not available" in transcript:
            return {
                "status": "error",
                "message": "Failed to fetch transcript from YouTube"
            }

        #  ---------------------Generate report using LLM----------
        report = generate_news_report(transcript)

        # --------------------to Validate LLM output--------------
        if not report or len(report.strip()) == 0:
            return {
                "status": "error",
                "message": "LLM failed to generate report"
            }

         
        return {
            "status": "success",
            "report": report
        }

    except Exception as e:
        print("PIPELINE ERROR:", str(e))

        return {
            "status": "error",
            "message": str(e)
        }