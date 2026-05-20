from fastapi import APIRouter
from app.models.request import YoutubeRequest
from app.services.pipeline_service import run_pipeline

router = APIRouter()

@router.post("/generate-report")
def generate_report(request: YoutubeRequest):
    return run_pipeline(request.url)