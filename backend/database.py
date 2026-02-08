import os
import uuid
import logging
from qdrant_client import QdrantClient
from qdrant_client.http import models
from dotenv import load_dotenv

load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# --- Configuration ---
QDRANT_URL = os.getenv("QDRANT_URL")
QDRANT_API_KEY = os.getenv("QDRANT_API_KEY")
COLLECTION_NAME = "aura_archive"

# Initialize Client
if QDRANT_URL and QDRANT_API_KEY:
    client = QdrantClient(url=QDRANT_URL, api_key=QDRANT_API_KEY)
    logger.info("Connected to Qdrant Cloud")
else:
    client = QdrantClient(":memory:")
    logger.warning("Using in-memory Qdrant - data will be lost on restart")

def init_db():
    """Creates the collection if it doesn't exist."""
    try:
        client.get_collection(COLLECTION_NAME)
        logger.info(f"Collection '{COLLECTION_NAME}' exists")
    except Exception as e:
        logger.info(f"Creating collection '{COLLECTION_NAME}'")
        client.create_collection(
            collection_name=COLLECTION_NAME,
            vectors_config=models.VectorParams(size=768, distance=models.Distance.COSINE)
        )
    
    # CRITICAL: Create keyword index on 'status' field for filtering
    try:
        client.create_payload_index(
            collection_name=COLLECTION_NAME,
            field_name="status",
            field_schema=models.PayloadSchemaType.KEYWORD
        )
        logger.info("Payload index on 'status' field created/verified")
    except Exception as e:
        # Index might already exist, log and continue
        logger.info(f"Status index creation skipped (may already exist): {e}")


def insert_draft(id: str, status: str = "PROCESSING"):
    """Creates a placeholder entry for a new upload."""
    client.upsert(
        collection_name=COLLECTION_NAME,
        points=[
            models.PointStruct(
                id=id,
                vector=[0.0] * 768, # Dummy vector until we generate a real one
                payload={
                    "status": status,
                    "created_at": str(uuid.uuid1()) # Timestamp from UUID
                }
            )
        ]
    )

def update_discussion_payload(id: str, data: dict):
    """Updates the metadata (Blog content, Title, Status, etc.)."""
    client.set_payload(
        collection_name=COLLECTION_NAME,
        payload=data,
        points=[id]  # Correct format: list of point IDs
    )
    logger.info(f"Updated payload for point {id}: {list(data.keys())}")


def get_discussions_by_status(status: str):
    """Fetches discussions based on their status (REVIEW_PENDING or PUBLISHED)."""
    try:
        res = client.scroll(
            collection_name=COLLECTION_NAME,
            scroll_filter=models.Filter(
                must=[
                    models.FieldCondition(
                        key="status",
                        match=models.MatchValue(value=status)
                    )
                ]
            ),
            limit=100 
        )
        # Return just the payload data for the frontend
        # Handle empty results gracefully
        if res and len(res) > 0 and res[0]:
            return [point.payload for point in res[0]]
        return []
    except Exception as e:
        logger.error(f"Error fetching discussions with status '{status}': {e}", exc_info=True)
        return []