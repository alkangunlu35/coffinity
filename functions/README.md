# Coffinity URL Product Import Function

This folder contains the backend function used by the Android URL import flow.

## Function

- `importProductFromUrl`
  - HTTP POST
  - Region: `us-central1`
  - Input: `{ "url": "https://example.com/product" }`
  - Output: normalized import preview fields (no auto-save)

## Local setup

1. Install Firebase CLI
2. Install dependencies

```bash
cd functions
npm install
```

3. From project root, make sure Firebase is configured for this project (project id from `google-services.json`)
4. Deploy:

```bash
firebase deploy --only functions
```

## Notes

- The function performs a single-page fetch only (no crawling)
- It validates URL scheme/host and blocks local/private hosts
- It uses Open Graph, JSON-LD, then fallback text parsing
- It returns warnings for missing fields instead of inventing data
