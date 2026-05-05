import requests

OLLAMA_URL = "http://localhost:11434/api/generate"


def generate_news_report(transcript: str) -> str:
    """
    Generate structured news report from transcript using Ollama (Llama3)
    """

    if not transcript or len(transcript.strip()) == 0:
        raise ValueError("Empty transcript received")

    
    prompt = f"""
You are a senior NEWS EDITOR working for a international media organization.

Your task is to transform a short and possibly incomplete transcript into a detailed, professional news article.

-

STRICT RULES (MANDATORY):
- Do NOT act like a chatbot
- Do NOT include explanations about the transcript
- Do NOT mention "transcript", "audio", or "video"
- Do NOT include notes, disclaimers, or analysis
- Do NOT add any section outside the required format
- Output ONLY the news article
- If information is unclear, write cautiously but DO NOT explain uncertainty

OBJECTIVE:
Even if the transcript is short or incomplete, you must:
- Extract key information
- Infer context logically
- Expand it into a FULL-LENGTH NEWS ARTICLE

OUTPUT FORMAT (STRICT):

Headline:
<Clear, factual, engaging headline>

Summary:
<5-6 concise sentences summarizing the topic>

Key Points:
- <Important point 1>
- <Important point 2>
- <Important point 3>
- <Important point 4>


Conclusion:
<Clear closing paragraph summarizing impact, significance, or future implications>

---

WRITING STYLE:
- Neutral and factual
WRITING STYLE:
- Formal news reporting tone
- Expand meaning logically (but do NOT hallucinate random facts)
- Use clear transitions between paragraphs
- Keep it informative and structured
- No filler words

---

TRANSCRIPT:
{transcript[:1800]}

---

IMPORTANT:
If the transcript is noisy or unclear, still extract the most meaningful information and present it clearly in news format.
- Focus on clarity, structure, and completeness
"""

    try:
        response = requests.post(
            OLLAMA_URL,
            json={
                "model": "llama3",  
                "prompt": prompt,
                "stream": False,
                "options": {
                    "temperature": 0.2,
                    "top_p": 0.9
                }
            },
            timeout=120
        )

        #-------http error check---------
        if response.status_code != 200:
            raise Exception(f"Ollama API Error: {response.text}")

        data = response.json()

        result = data.get("response", "").strip()

        
        if not result:
            raise Exception("Empty response from LLM")

        return result

    except requests.exceptions.Timeout:
        raise Exception("LLM request timed out")

    except requests.exceptions.ConnectionError:
        raise Exception("Cannot connect to Ollama. Is it running?")

    except Exception as e:
        print("LLM ERROR:", str(e))
        raise Exception("Failed to generate news report")