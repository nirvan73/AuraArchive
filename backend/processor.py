import os
import time
import json
import logging
import io
import google.generativeai as genai
from dotenv import load_dotenv
import cloudinary
import cloudinary.uploader

load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Configure Gemini API
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

# Configure Cloudinary (for permanent image storage)
cloudinary.config(
    cloud_name=os.getenv("CLOUDINARY_CLOUD_NAME"),
    api_key=os.getenv("CLOUDINARY_API_KEY"),
    api_secret=os.getenv("CLOUDINARY_API_SECRET")
)

def generate_cover_image(description):
    """Generates a blog cover image using Imagen 3 and uploads to Cloudinary."""
    logger.info(f"Generating cover image for: {description[:50]}...")
    try:
        # Generate image with Imagen 3
        imagen_model = genai.GenerativeModel('imagen-3.0-generate-001')
        result = imagen_model.generate_images(
            prompt=f"A high-quality, futuristic, minimalist tech blog cover image. No text. Theme: {description}",
            number_of_images=1,
            aspect_ratio="16:9"
        )
        
        # Convert to bytes for upload
        image = result[0]._pil_image
        buffer = io.BytesIO()
        image.save(buffer, format='PNG')
        buffer.seek(0)
        
        # Upload to Cloudinary for permanent storage
        logger.info("Uploading image to Cloudinary...")
        upload_result = cloudinary.uploader.upload(
            buffer,
            folder="auraarchive",
            resource_type="image"
        )
        
        permanent_url = upload_result['secure_url']
        logger.info(f"Image uploaded successfully: {permanent_url}")
        return permanent_url
        
    except Exception as e:
        logger.error(f"Image generation/upload failed: {e}", exc_info=True)
        return "https://placehold.co/600x400?text=Cover"

def generate_club_blog_pro(audio_file_path):
    """Main AI pipeline: Audio -> Blog + Links + Image Prompt."""
    
    # 1. Validate file exists
    if not os.path.exists(audio_file_path):
        logger.error(f"Audio file not found: {audio_file_path}")
        raise FileNotFoundError(f"Audio file not found: {audio_file_path}")
    
    # Detect mime type from file extension
    ext = audio_file_path.lower().split('.')[-1]
    mime_types = {
        'mp3': 'audio/mpeg',
        'm4a': 'audio/mp4',
        'wav': 'audio/wav',
        'aac': 'audio/aac',
        'ogg': 'audio/ogg',
        'flac': 'audio/flac'
    }
    mime_type = mime_types.get(ext, 'audio/mpeg')
    logger.info(f"Detected file type: {ext} (mime: {mime_type})")
    
    # 2. Setup Gemini 2.5 Flash (latest)
    model = genai.GenerativeModel(
        model_name="gemini-2.5-flash"
    )

    # 3. Upload Audio
    logger.info("Uploading audio file to Gemini API")
    audio_file = genai.upload_file(audio_file_path, mime_type=mime_type)
    
    # Wait for processing
    logger.info("Waiting for audio processing to complete")
    while audio_file.state.name == "PROCESSING":
        time.sleep(2)
        audio_file = genai.get_file(audio_file.name)

    if audio_file.state.name == "FAILED":
        logger.error("Audio processing failed on Gemini side")
        raise ValueError("Audio processing failed")

    # 3. The Prompt
    prompt = """
    You are the Scribe for a University Tech Club. Listen to this discussion.
    
    Goal: Create a structured blog post.
    
    1. **Content:** Summarize the technical discussion into a blog post (Markdown).
    2. **Resources:** If you know relevant documentation links or tutorials, include up to 3 (optional).
    3.  **Visuals:** Describe a cool cover image for this post in 1 sentence.

    Output strictly in this JSON format:
    {
        "title": "String",
        "summary": "String",
        "blog_markdown": "String (Markdown)",
        "image_prompt": "String",
        "external_links": [
            {"title": "String", "url": "String", "description": "String"}
        ]
    }
    """

    # 4. Generate Content with Retry Logic
    logger.info("Generating blog content with Gemini 2.5 Flash")
    MAX_RETRIES = 3
    response = None
    
    for attempt in range(MAX_RETRIES):
        try:
            response = model.generate_content([audio_file, prompt])
            logger.info(f"API call successful on attempt {attempt + 1}")
            break
        except Exception as api_error:
            if attempt < MAX_RETRIES - 1:
                wait_time = 2 ** attempt  # Exponential backoff: 1s, 2s, 4s
                logger.warning(f"API call failed (attempt {attempt + 1}/{MAX_RETRIES}), retrying in {wait_time}s: {api_error}")
                time.sleep(wait_time)
            else:
                logger.error(f"API call failed after {MAX_RETRIES} attempts", exc_info=True)
                raise
    
    # 5. Parse JSON
    try:
        # Clean up code blocks if Gemini adds them
        text = response.text.replace("```json", "").replace("```", "")
        data = json.loads(text)
        logger.info("Successfully parsed AI response")
    except Exception as e:
        logger.error(f"JSON parse error: {e}", exc_info=True)
        # Fallback data
        data = {
            "title": "AuraArchive Discussion (Processing Error)",
            "summary": "Could not parse AI output",
            "blog_markdown": response.text,
            "image_prompt": "Tech abstract",
            "external_links": []
        }

    # 6. Generate Image
    image_url = generate_cover_image(data.get("image_prompt", "Tech"))
    data["image_url"] = image_url
    
    logger.info("AI pipeline completed successfully")
    return data