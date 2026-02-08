# AuraArchive API - Output Structure & Constraints

## API Base URL
```
Production: https://your-app.onrender.com
Local: http://localhost:8000
```

---

## Endpoint: `POST /api/upload`

### Request
```
Content-Type: multipart/form-data
Body: file (audio file)
```

### Response
```json
{
  "id": "string (UUID)",
  "message": "Upload accepted"
}
```

**Constraints:**
- `id`: UUID v4 format (36 characters)
- Audio file size: No hard limit (tested up to 45MB)
- Supported formats: MP3, M4A, WAV, AAC, OGG, FLAC

---

## Endpoint: `GET /api/drafts`

### Response
```json
[
  {
    "status": "REVIEW_PENDING",
    "created_at": "string (UUID v1 timestamp)",
    "title": "string",
    "summary": "string",
    "content": "string (Markdown)",
    "image_url": "string (URL)",
    "external_links": [
      {
        "title": "string",
        "url": "string (URL)",
        "description": "string"
      }
    ]
  }
]
```

**Field Constraints:**

| Field | Type | Max Length | Required | Notes |
|-------|------|------------|----------|-------|
| `status` | enum | - | ✅ | `"PROCESSING"` \| `"REVIEW_PENDING"` \| `"FAILED"` |
| `created_at` | UUID v1 | 36 chars | ✅ | Timestamp embedded in UUID |
| `title` | string | ~200 chars | ✅ | AI-generated, concise title |
| `summary` | string | ~500 chars | ✅ | Plain text summary |
| `content` | string | ~5000+ chars | ✅ | **Full Markdown blog post** |
| `image_url` | string (URL) | - | ✅ | HTTPS Cloudinary URL or placeholder |
| `external_links` | array | 0-3 items | ✅ | Can be empty `[]` |
| `external_links[].title` | string | ~100 chars | ✅ | Link title |
| `external_links[].url` | string (URL) | - | ✅ | Valid HTTP/HTTPS URL |
| `external_links[].description` | string | ~200 chars | ✅ | Brief description |
| `error` | string | - | ❌ | Only present if `status: "FAILED"` |

**Status Flow:**
```
PROCESSING → REVIEW_PENDING → PUBLISHED
           ↘ FAILED
```

---

## Endpoint: `PUT /api/save/{id}`

### Request
```json
{
  "title": "string",
  "summary": "string",
  "blog_markdown": "string"
}
```

### Response
```json
{
  "status": "updated"
}
```

**Constraints:**
- Admin only endpoint
- Updates draft in Qdrant
- Does NOT change status

---

## Endpoint: `POST /api/publish/{id}`

### Response
```json
{
  "status": "published"
}
```

**Constraints:**
- Changes status from `REVIEW_PENDING` → `PUBLISHED`
- Irreversible action

---

## Endpoint: `GET /api/feed`

### Response
```json
[
  {
    "status": "PUBLISHED",
    "created_at": "string",
    "title": "string",
    "summary": "string",
    "content": "string (Markdown)",
    "image_url": "string",
    "external_links": [...]
  }
]
```

**Constraints:**
- Returns only `status: "PUBLISHED"` items
- Same structure as drafts
- Sorted by `created_at` (newest first)

---

## Content Format Specifications

### Markdown Content Structure
```markdown
# Main Title (H1)

Introductory paragraph...

## Section 1 (H2)
Content with **bold**, *italic*, and normal text.

## Section 2 (H2)
More content...

### Subsection (H3)
- Bullet points
- Lists

Final thoughts...
```

**Markdown Features:**
- ✅ Headers (H1, H2, H3)
- ✅ Bold (`**text**`)
- ✅ Italic (`*text*`)
- ✅ Paragraphs
- ✅ Lists (ordered and unordered)
- ✅ Links (if included by AI)
- ❌ Images (not in markdown, use `image_url` field)
- ❌ Code blocks (depends on audio content)

---

## Image URL Constraints

**Current:**
```
https://placehold.co/600x400?text=Cover
```

**Production (when Cloudinary is configured):**
```
https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/auraarchive/{image_id}.png
```

**Format:**
- Protocol: HTTPS only
- Aspect Ratio: 16:9 recommended
- Format: PNG/JPG
- Permanent storage: Yes

---

## Error Responses

### Processing Failed
```json
{
  "status": "FAILED",
  "created_at": "ffb56ffa-04c8-11f1-a154-988d46ac873e",
  "error": "Audio processing failed: [error details]"
}
```

### Upload Error
```json
{
  "detail": "File type not supported"
}
```

Status Code: `400 Bad Request`

---

## Data Validation Rules

### Frontend Should Validate:
1. **Title**: Not empty, max 200 chars
2. **Summary**: Not empty, max 1000 chars
3. **Content**: Not empty, valid Markdown
4. **Image URL**: Valid HTTPS URL
5. **External Links**: Each has title, url, description

### Backend Guarantees:
- ✅ All required fields present (except on FAILED status)
- ✅ URLs are valid format
- ✅ Content is valid Markdown
- ✅ External links array never null (can be `[]`)
- ✅ Status is one of valid enums

---

## Example Full Response

```json
{
  "status": "REVIEW_PENDING",
  "created_at": "ffb56ffa-04c8-11f1-a154-988d46ac873e",
  "title": "Understanding React Hooks in Modern Development",
  "summary": "A comprehensive discussion on useState and useEffect hooks, exploring their practical applications in building scalable React applications. Learn best practices and common pitfalls.",
  "content": "# Understanding React Hooks in Modern Development\n\nReact Hooks revolutionized how we write components...\n\n## Introduction to useState\n\nThe useState hook allows...\n\n## Working with useEffect\n\nSide effects are managed...",
  "image_url": "https://res.cloudinary.com/auraarchive/image/upload/v1234567890/auraarchive/react-hooks.png",
  "external_links": [
    {
      "title": "React Hooks Documentation",
      "url": "https://react.dev/reference/react",
      "description": "Official React documentation covering all built-in hooks with examples and best practices."
    },
    {
      "title": "Rules of Hooks",
      "url": "https://react.dev/warnings/invalid-hook-call-warning",
      "description": "Essential guidelines for using hooks correctly in React applications."
    },
    {
      "title": "Custom Hooks Guide",
      "url": "https://react.dev/learn/reusing-logic-with-custom-hooks",
      "description": "Learn how to extract component logic into reusable custom hooks."
    }
  ]
}
```

---

## Performance Characteristics

| Metric | Value |
|--------|-------|
| Upload Response Time | < 1 second |
| AI Processing Time | 20-60 seconds |
| Draft Fetch Time | < 500ms |
| Feed Fetch Time | < 1 second |
| Max Audio Length | ~1 hour |
| Max Audio Size | ~100MB (recommended < 50MB) |

---

## Frontend Integration Tips

### Rendering Markdown
```javascript
import ReactMarkdown from 'react-markdown'

<ReactMarkdown>{draft.content}</ReactMarkdown>
```

### Polling for Results
```javascript
// After upload
const sessionId = uploadResponse.id;

// Poll every 5 seconds
const interval = setInterval(async () => {
  const drafts = await fetch('/api/drafts').then(r => r.json());
  const myDraft = drafts.find(d => d.created_at === sessionId);
  
  if (myDraft && myDraft.status === 'REVIEW_PENDING') {
    clearInterval(interval);
    // Display draft
  }
}, 5000);
```

### Handling Empty States
```javascript
// External links might be empty
draft.external_links?.length > 0 
  ? renderLinks(draft.external_links)
  : renderNoLinks()
```

---

**This is the complete API contract for frontend integration!** 🚀
