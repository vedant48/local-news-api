from fastapi import FastAPI
from app.routes.report_route import router

app = FastAPI()

@app.get("/")
def home():
    return {"message": "ML Service Running 🚀"}

app.include_router(router, prefix="/ai")
