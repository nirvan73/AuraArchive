# AuraArchive API - Complete Output Specification

## All Possible Status Values

| Status | Description | When It Occurs |
|--------|-------------|----------------|
| `PROCESSING` | Audio is being analyzed by AI | Immediately after upload |
| `REVIEW_PENDING` | Draft ready for admin review | After successful AI processing |
| `PUBLISHED` | Blog is live on public feed | After admin publishes |
| `FAILED` | Processing failed | AI or upload error |

---

## Complete API Response Examples

### 1. POST /api/upload - Upload Success

**Request:**
```http
POST /api/upload
Content-Type: multipart/form-data

file: [audio_file.mp3]
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Upload accepted"
}
```

**Status Code:** `200 OK`

---

### 2. GET /api/drafts - Status: PROCESSING

```json
[
  {
    "status": "PROCESSING",
    "created_at": "1b2ec9b9-04c5-11f1-99be-988d46ac873e"
  }
]
```

**Notes:**
- Only `status` and `created_at` fields present
- No content yet
- Poll again in 5-10 seconds

---

### 3. GET /api/drafts - Status: REVIEW_PENDING (Success)

```json
[
  {
    "status": "REVIEW_PENDING",
    "created_at": "1b2ec9b9-04c5-11f1-99be-988d46ac873e",
    "title": "Understanding React Hooks in Modern Development",
    "summary": "A comprehensive discussion on useState and useEffect hooks, exploring their practical applications in building scalable React applications.",
    "content": "# Understanding React Hooks in Modern Development\n\nReact Hooks revolutionized...\n\n## Introduction to useState\n\nThe useState hook allows...",
    "image_url": "https://res.cloudinary.com/auraarchive/image/upload/v123/auraarchive/abc123.png",
    "external_links": [
      {
        "title": "React Official Documentation",
        "url": "https://react.dev/reference/react",
        "description": "Complete guide to React hooks and APIs"
      },
      {
        "title": "Rules of Hooks",
        "url": "https://react.dev/warnings/invalid-hook-call-warning",
        "description": "Essential guidelines for using hooks correctly"
      }
    ]
  }
]
```

**All Fields Present:**
- ✅ `status`: Always "REVIEW_PENDING"
- ✅ `created_at`: UUID v1
- ✅ `title`: String, 10-200 characters
- ✅ `summary`: String, 50-1000 characters
- ✅ `content`: Markdown string, 500-10000 characters
- ✅ `image_url`: HTTPS URL
- ✅ `external_links`: Array (0-3 items)

---

### 4. GET /api/drafts - Status: REVIEW_PENDING (No External Links)

```json
[
  {
    "status": "REVIEW_PENDING",
    "created_at": "1b2ec9b9-04c5-11f1-99be-988d46ac873e",
    "title": "Personal Growth Story",
    "summary": "A motivational story about focus and mindfulness.",
    "content": "# The Monk and the Spoon\n\nA timeless parable...",
    "image_url": "https://placehold.co/600x400?text=Cover",
    "external_links": []
  }
]
```

**Notes:**
- `external_links` can be empty array `[]`
- Still valid response
- Image might be placeholder if Cloudinary fails

---

### 5. GET /api/drafts - Status: FAILED

```json
[
  {
    "status": "FAILED",
    "created_at": "1b2ec9b9-04c5-11f1-99be-988d46ac873e",
    "error": "Audio processing failed: Invalid audio format"
  }
]
```

**Fields:**
- ✅ `status`: "FAILED"
- ✅ `created_at`: UUID v1
- ✅ `error`: String describing the error
- ❌ No `title`, `summary`, `content`, `image_url`, `external_links`

**Possible Error Messages:**
- `"Audio processing failed: Invalid audio format"`
- `"Audio processing failed: File too large"`
- `"Image generation failed: [error details]"`
- `"JSON parse error: Could not parse AI output"`

---

### 6. GET /api/drafts - Empty (No Drafts)

```json
[]
```

**Notes:**
- Empty array when no drafts in review
- Status Code: `200 OK`

---

### 7. GET /api/feed - Published Posts

```json
[
  {
    "status": "PUBLISHED",
    "created_at": "1b2ec9b9-04c5-11f1-99be-988d46ac873e",
    "title": "Understanding React Hooks",
    "summary": "A comprehensive discussion on useState and useEffect...",
    "content": "# Understanding React Hooks\n\n...",
    "image_url": "https://res.cloudinary.com/...",
    "external_links": [...]
  }
]
```

**Same structure as REVIEW_PENDING** but with `status: "PUBLISHED"`

---

### 8. PUT /api/save/{id} - Save Draft

**Request:**
```json
{
  "title": "Updated Title",
  "summary": "Updated summary text",
  "blog_markdown": "# Updated Content\n\nNew markdown content..."
}
```

**Response:**
```json
{
  "status": "updated"
}
```

**Status Code:** `200 OK`

---

### 9. POST /api/publish/{id} - Publish Draft

**Response:**
```json
{
  "status": "published"
}
```

**Status Code:** `200 OK`

**Notes:**
- Changes draft status from `REVIEW_PENDING` → `PUBLISHED`
- Draft no longer appears in `/api/drafts`
- Now appears in `/api/feed`

---

### 10. Error Responses

#### Upload Error - Invalid File Type
```json
{
  "detail": "File type not supported"
}
```
**Status Code:** `400 Bad Request`

#### Upload Error - No File
```json
{
  "detail": "No file provided"
}
```
**Status Code:** `422 Unprocessable Entity`

#### Server Error
```json
{
  "detail": "Internal server error"
}
```
**Status Code:** `500 Internal Server Error`

---

## Field Specifications

### Title
- **Type:** String
- **Min Length:** 10 characters
- **Max Length:** 200 characters
- **Format:** Plain text
- **Example:** "Understanding React Hooks in Modern Development"

### Summary
- **Type:** String
- **Min Length:** 50 characters
- **Max Length:** 1000 characters
- **Format:** Plain text paragraph
- **Example:** "A comprehensive discussion on useState and useEffect hooks..."

### Content
- **Type:** String
- **Min Length:** 500 characters
- **Max Length:** ~10,000 characters
- **Format:** Valid Markdown
- **Structure:**
  ```markdown
  # Main Title (H1)
  
  Introduction paragraph...
  
  ## Section 1 (H2)
  Content...
  
  ## Section 2 (H2)
  More content...
  ```

### Image URL
- **Type:** String (URL)
- **Format:** HTTPS only
- **Examples:**
  - Production: `https://res.cloudinary.com/auraarchive/image/upload/v123/auraarchive/abc.png`
  - Fallback: `https://placehold.co/600x400?text=Cover`

### External Links
- **Type:** Array of Objects
- **Min Items:** 0
- **Max Items:** 3
- **Item Structure:**
  ```json
  {
    "title": "string (max 100 chars)",
    "url": "string (valid HTTPS URL)",
    "description": "string (max 200 chars)"
  }
  ```

### Created At
- **Type:** String (UUID v1)
- **Format:** `xxxxxxxx-xxxx-11xx-xxxx-xxxxxxxxxxxx`
- **Contains:** Timestamp information
- **Example:** `"1b2ec9b9-04c5-11f1-99be-988d46ac873e"`

---

## Status Lifecycle

```
┌─────────────┐
│   Upload    │
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ PROCESSING  │ ← Audio being analyzed (20-60s)
└──────┬──────┘
       │
       ├─────Success─────┐
       │                 ▼
       │          ┌──────────────┐
       │          │REVIEW_PENDING│ ← Admin reviews
       │          └──────┬───────┘
       │                 │
       │            Publish
       │                 │
       │                 ▼
       │          ┌──────────────┐
       │          │  PUBLISHED   │ ← Live on feed
       │          └──────────────┘
       │
       └─────Error──────┐
                        ▼
                 ┌─────────────┐
                 │   FAILED    │
                 └─────────────┘
```

---

## Frontend Integration Checklist

### Handling Each Status:

**PROCESSING:**
```javascript
// Show loading spinner
// Poll /api/drafts every 5 seconds
```

**REVIEW_PENDING:**
```javascript
// Display draft with edit controls
// Show: title, summary, content (as markdown), image, links
// Allow: edit, publish actions
```

**PUBLISHED:**
```javascript
// Show in public feed
// Read-only view
```

**FAILED:**
```javascript
// Show error message from error field
// Allow retry upload
```

### Required Frontend Validations:

1. **Before Display:**
   - Check `status` field exists
   - Check `external_links` is array (might be empty)
   - Validate `image_url` is HTTPS

2. **Before Save:**
   - Title: 10-200 chars
   - Summary: 50-1000 chars
   - Content: Not empty

3. **Markdown Rendering:**
   - Sanitize HTML if rendering markdown
   - Support: H1-H3, bold, italic, lists, paragraphs

---

## Complete TypeScript Interface

```typescript
// Draft/Feed Item
interface BlogPost {
  status: 'PROCESSING' | 'REVIEW_PENDING' | 'PUBLISHED' | 'FAILED';
  created_at: string;  // UUID v1
  title?: string;      // Present when status is not PROCESSING/FAILED
  summary?: string;    // Present when status is not PROCESSING/FAILED
  content?: string;    // Markdown, present when status is not PROCESSING/FAILED
  image_url?: string;  // Present when status is not PROCESSING/FAILED
  external_links?: ExternalLink[];  // Present when status is not PROCESSING/FAILED
  error?: string;      // Only present when status is FAILED
}

interface ExternalLink {
  title: string;
  url: string;
  description: string;
}

// Upload Response
interface UploadResponse {
  id: string;  // UUID v4
  message: string;
}

// Save Request
interface SaveRequest {
  title: string;
  summary: string;
  blog_markdown: string;
}

// Save/Publish Response
interface ActionResponse {
  status: 'updated' | 'published';
}
```

---

## Testing All States

### Test Case 1: Successful Flow
1. Upload audio → Get session ID
2. Poll /api/drafts → See PROCESSING
3. Wait 30s
4. Poll /api/drafts → See REVIEW_PENDING with full data
5. PUT /api/save/{id} → Update draft
6. POST /api/publish/{id} → Publish
7. GET /api/feed → See PUBLISHED post

### Test Case 2: Failed Processing
1. Upload corrupt audio
2. Poll /api/drafts → See PROCESSING
3. Wait 30s
4. Poll /api/drafts → See FAILED with error message

### Test Case 3: Empty Links
1. Upload non-technical audio (story, music)
2. Get REVIEW_PENDING with `external_links: []`

---

**This is the complete, exhaustive API specification!** 🚀

Share `API_OUTPUT.md` with Nirvan for frontend integration.
