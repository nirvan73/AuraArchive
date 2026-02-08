import os
import shutil
import uuid
import logging
from fastapi import FastAPI, UploadFile, File, BackgroundTasks, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from database import init_db, insert_draft, update_discussion_payload, get_discussions_by_status
from processor import generate_club_blog_pro 

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # PRODUCTION: Replace with specific domains like ["https://yourapp.com"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
def startup_event():
    init_db()

# --- Data Models ---
class SaveRequest(BaseModel):
    title: str
    summary: str
    blog_markdown: str

# --- Endpoints ---

@app.get("/")
def home():
    return {"status": "online", "message": "AuraArchive API is running"}

@app.post("/api/upload")
async def upload_audio(background_tasks: BackgroundTasks, file: UploadFile = File(...)):
    """Receives audio, returns ID, starts AI in background."""
    session_id = str(uuid.uuid4())
    
    # Save temp file
    file_path = f"temp_{session_id}_{file.filename}"
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    # Create DB Entry
    insert_draft(session_id, status="PROCESSING")

    # Background Task Wrapper
    def task(id, path):
        try:
            logger.info(f"Processing task {id} started")
            result = generate_club_blog_pro(path)
            
            # Update DB with AI results
            update_discussion_payload(id, {
                "status": "REVIEW_PENDING",
                "title": result["title"],
                "summary": result["summary"],
                "content": result["blog_markdown"],
                "image_url": result["image_url"],
                "external_links": result["external_links"]
            })
            logger.info(f"Task {id} completed successfully")
        except Exception as e:
            logger.error(f"Task {id} failed: {e}", exc_info=True)
            update_discussion_payload(id, {"status": "FAILED", "error": str(e)})
        finally:
            if os.path.exists(path):
                os.remove(path)
                logger.debug(f"Cleaned up temporary file: {path}")

    background_tasks.add_task(task, session_id, file_path)
    return {"id": session_id, "message": "Upload accepted"}

@app.get("/api/drafts")
def get_drafts():
    """For Admin Dashboard"""
    return get_discussions_by_status("REVIEW_PENDING")

@app.put("/api/save/{id}")
def save_draft(id: str, data: SaveRequest):
    """For 'Save' button"""
    update_discussion_payload(id, {
        "title": data.title,
        "summary": data.summary,
        "content": data.blog_markdown
    })
    return {"message": "Saved"}

@app.post("/api/publish/{id}")
def publish_draft(id: str):
    """For 'Publish' button"""
    update_discussion_payload(id, {"status": "PUBLISHED"})
    return {"message": "Published"}

@app.get("/api/feed")
def get_feed():
    """For Public App Feed"""
    return get_discussions_by_status("PUBLISHED")