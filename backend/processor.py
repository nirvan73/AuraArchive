import os
import time
import json
import logging
import io
import re
import google.generativeai as genai
from dotenv import load_dotenv
import cloudinary
import cloudinary.uploader
import typing_extensions as typing

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

# Define schemas for Structured Output
class ExternalLink(typing.TypedDict):
    title: str
    url: str
    description: str

class BlogContent(typing.TypedDict):
    title: str
    summary: str
    blog_markdown: str
    image_prompt: str
    external_links: list[ExternalLink]

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

    # 3. Configure generation with Structured Output
    generation_config = {
        "response_mime_type": "application/json",
        "response_schema": BlogContent,
    }

    # 4. The Prompt (Simplified/Focused on Content)
    prompt = """
    You are an expert Content Architect and Editor. Your goal is to transform this audio into a high-quality, structured document.

    ### 1. Analyze the Audio Context:
    First, identify what kind of audio this is:
    - **Meeting/Discussion:** Multiple people discussing topics, making decisions.
    - **Interview/Podcast:** Host and guest(s) Q&A format.
    - **Lecture/Talk:** Single speaker teaching or explaining concepts.
    - **Story/Narrative:** Personal experience or storytelling.
    - **Casual Conversation:** Informal chat.

    ### 2. Output Requirements:
    Based on the context, structure the `blog_markdown` field accordingly (in Markdown):

    - **If Meeting:** Use **Meeting Minutes** format. Include Attendees (inferred), Agenda, Key Discussion Points, Decisions Made, and Action Items.
    - **If Interview:** Use **Q&A** or **Feature Article** format. Highlight the guest's insights.
    - **If Lecture:** Use **Study Guide** or **Article** format with clear headings and bullet points.
    - **If Story/Casual:** Use **Blog Post** narrative format.

    ### 3. General Rules:
    - **Title:** Create a catchy, relevant title.
    - **Summary:** A concise executive summary (2-3 sentences).
    - **Speakers:** If multiple speakers are detected, differentiate them (e.g., "Speaker A", "Host", "Guest", or by name if mentioned).
    - **Tone:** Professional yet engaging.
    - **Links:** Act as a research assistant. Identify key topics, concepts, or tools discussed. Then, generate 3-5 high-quality external resources (Official Documentation, Coursera/EdX Courses, seminal Books, or reputable Articles) that would help the user learn more. Even if specific links aren't mentioned, infer the best resources for these topics.
    - **Verification:** Ensure all generated links are valid, reputable, and directly relevant to the content.

    ### 4. Visuals:
    - **Image Prompt:** Write a 1-sentence prompt for an AI image generator to create a relevant cover image for this content.

    ### Output Format:
    You must output valid JSON matching the schema provided.
    
    CRITICAL: 
    1. `blog_markdown` MUST take up the majority of the response. It must be the FULL blog post in standard Markdown format (headers, bullets, paragraphs). Do NOT put JSON inside this string.
    2. `external_links` MUST contain 3-5 inferred high-quality resources.
    """

    # 4. Generate Content with Retry Logic
    logger.info("Generating blog content with Gemini 2.5 Flash")
    MAX_RETRIES = 3
    response = None
    
    for attempt in range(MAX_RETRIES):
        try:
            response = model.generate_content(
                [audio_file, prompt],
                generation_config=generation_config
            )
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
    
    # 5. Parse JSON (Simplified)
    # 5. Robust JSON Parsing
    def extract_json_from_response(response_text):
        """Extract JSON from LLM response, handling various formats"""
        # Try markdown code blocks first
        code_block_pattern = r'```(?:json)?\s*(\{[\s\S]*?\})\s*```'
        matches = re.findall(code_block_pattern, response_text, re.DOTALL)
        if matches:
            json_str = max(matches, key=len).strip()
            try:
                json.loads(json_str)
                return json_str
            except json.JSONDecodeError:
                pass
        
        # Find JSON with balanced braces
        brace_count = 0
        start_idx = -1
        
        for i, char in enumerate(response_text):
            if char == '{':
                if brace_count == 0:
                    start_idx = i
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0 and start_idx != -1:
                    json_str = response_text[start_idx:i + 1]
                    try:
                        json.loads(json_str)
                        return json_str
                    except json.JSONDecodeError:
                        # Reset and continue searching
                        start_idx = -1
                        brace_count = 0
                        continue
        
        # Fallback: find first { and last }
        start_idx = response_text.find('{')
        end_idx = response_text.rfind('}') + 1
        
        if start_idx == -1 or end_idx <= 0:
             # Just try parsing the whole text if no braces found (unlikely for JSON)
             # or return raw regex fail.
             # Actually, just parse the raw text as a last resort
             return response_text
        
        json_str = response_text[start_idx:end_idx]
        return json_str

    try:
        json_str = extract_json_from_response(response.text)
        data = json.loads(json_str)
        
        # Safety Check for nested JSON string in blog_markdown (Double encoding)
        content = data.get("blog_markdown", "").strip()
        if content.startswith("{") and "blog_markdown" in content:
            logger.warning("Model returned nested JSON in blog_markdown. Attempting to unwrap.")
            try:
                nested_data = json.loads(content)
                if isinstance(nested_data, dict) and "blog_markdown" in nested_data:
                    data["blog_markdown"] = nested_data["blog_markdown"]
                    logger.info("Successfully unwrapped nested JSON")
            except:
                pass

        # Sanitize Validations
        if not data.get("blog_markdown"):
            data["blog_markdown"] = f"# {data.get('title', 'Draft')}\n\n{data.get('summary', '')}"
            
        logger.info("Successfully parsed refined AI response")
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