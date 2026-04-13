const { onRequest } = require("firebase-functions/v2/https");
const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");
const cheerio = require("cheerio");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const TIMEOUT_MS = 15000;
const USER_AGENT =
  "Mozilla/5.0 (compatible; CoffinityBot/1.0; +https://coffinity.app)";

/* =========================
   HTTP FETCH
========================= */
async function fetchHtmlPage(url) {
  const response = await axios.get(url, {
    timeout: TIMEOUT_MS,
    maxRedirects: 5,
    validateStatus: () => true,
    headers: {
      "User-Agent": USER_AGENT,
      Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
      "Accept-Language": "en-US,en;q=0.9,tr;q=0.8",
    },
  });

  const contentType = String(response.headers["content-type"] || "").toLowerCase();

  if (response.status >= 400) {
    throw new Error(`fetch_failed status=${response.status}`);
  }

  if (!contentType.includes("text/html")) {
    throw new Error(`non_html_response contentType=${contentType}`);
  }

  const html = response.data;

  if (!html || typeof html !== "string" || !html.trim()) {
    throw new Error("empty_html_response");
  }

  return html;
}

/* =========================
   OPENAI HELPERS
========================= */
function cleanText(value, maxLength = 400) {
  return String(value || "").replace(/\s+/g, " ").trim().slice(0, maxLength);
}

function uniqueStrings(values, limit = 20) {
  const seen = new Set();
  const result = [];

  for (const value of values || []) {
    const normalized = cleanText(value, 500);
    if (!normalized) continue;
    const key = normalized.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    result.push(normalized);
    if (result.length >= limit) break;
  }

  return result;
}

function normalizeMaybeUrl(baseUrl, value) {
  const raw = cleanText(value, 500);
  if (!raw) return null;

  try {
    return new URL(raw, baseUrl).toString();
  } catch {
    return raw;
  }
}

function normalizeHost(value) {
  const raw = cleanText(value, 300);
  if (!raw) return null;

  try {
    const parsed = /^https?:\/\//i.test(raw) ? new URL(raw) : new URL(`https://${raw}`);
    return parsed.hostname.replace(/^www\./i, "").toLowerCase();
  } catch {
    return null;
  }
}

function buildAiSignals(url, html) {
  const $ = cheerio.load(html);

  const title = cleanText($("title").first().text(), 200) || null;
  const metaDescription =
    cleanText($('meta[name="description"]').attr("content"), 350) || null;
  const ogTitle =
    cleanText($('meta[property="og:title"]').attr("content"), 200) || null;
  const ogDescription =
    cleanText($('meta[property="og:description"]').attr("content"), 350) || null;
  const canonical =
    cleanText($("link[rel='canonical']").first().attr("href"), 400) || url;

  const headings = uniqueStrings(
    $("h1, h2")
      .map((_, el) => $(el).text())
      .get(),
    15
  );

  const paragraphs = uniqueStrings(
    $("main p, article p, section p, p")
      .map((_, el) => $(el).text())
      .get()
      .filter((text) => cleanText(text).length >= 40),
    12
  );

  const links = uniqueStrings(
    $("a[href]")
      .map((_, el) => {
        const href = $(el).attr("href");
        if (!href) return null;
        try {
          return new URL(href, url).toString();
        } catch {
          return null;
        }
      })
      .get(),
    40
  );

  const jsonLdBlocks = uniqueStrings(
    $("script[type='application/ld+json']")
      .map((_, el) => $(el).contents().text())
      .get(),
    5
  );

  return {
    title,
    metaDescription,
    ogTitle,
    ogDescription,
    canonical,
    headings,
    paragraphs,
    links,
    jsonLdBlocks,
  };
}

function buildAiPrompt(url, signals, basicPreview) {
  return `
Extract brand information from the website-derived content below.

Use only the provided content.
Do not guess.
Return JSON only.

Expected JSON shape:
{
  "brandName": string | null,
  "description": string | null,
  "websiteUrl": string | null,
  "websiteHost": string | null,
  "instagramUrl": string | null,
  "country": string | null,
  "city": string | null,
  "productHints": string[],
  "socialLinks": string[],
  "confidence": number,
  "warnings": string[]
}

Source URL:
${url}

Basic preview already detected:
${JSON.stringify(basicPreview, null, 2)}

Title:
${signals.title || "null"}

Meta description:
${signals.metaDescription || "null"}

OG title:
${signals.ogTitle || "null"}

OG description:
${signals.ogDescription || "null"}

Canonical:
${signals.canonical || "null"}

Headings:
${signals.headings.length ? signals.headings.join("\n") : "none"}

Paragraphs:
${signals.paragraphs.length ? signals.paragraphs.join("\n") : "none"}

Links:
${signals.links.length ? signals.links.join("\n") : "none"}

JSON-LD:
${signals.jsonLdBlocks.length ? signals.jsonLdBlocks.join("\n---\n") : "none"}
`.trim();
}

function readOpenAiOutputText(data) {
  if (typeof data?.output_text === "string" && data.output_text.trim()) {
    return data.output_text.trim();
  }

  const output = Array.isArray(data?.output) ? data.output : [];
  for (const item of output) {
    const content = Array.isArray(item?.content) ? item.content : [];
    for (const part of content) {
      if (typeof part?.text === "string" && part.text.trim()) {
        return part.text.trim();
      }
    }
  }

  return null;
}

async function runBrandAiExtraction(url, html, basicPreview) {
  const apiKey = OPENAI_API_KEY.value();
  if (!apiKey) {
    throw new Error("missing_openai_key");
  }

  const signals = buildAiSignals(url, html);
  const prompt = buildAiPrompt(url, signals, basicPreview);

  const payload = {
    model: "gpt-5.4-mini",
    store: false,
    input: [
      {
        role: "system",
        content: [
          {
            type: "input_text",
            text:
              "You extract brand onboarding data for a super-admin tool. " +
              "Use only provided content. Do not guess. Return valid JSON only.",
          },
        ],
      },
      {
        role: "user",
        content: [
          {
            type: "input_text",
            text: prompt,
          },
        ],
      },
    ],
    text: {
      format: {
        type: "json_schema",
        name: "brand_import_extraction",
        strict: true,
        schema: {
          type: "object",
          additionalProperties: false,
          properties: {
            brandName: { type: ["string", "null"] },
            description: { type: ["string", "null"] },
            websiteUrl: { type: ["string", "null"] },
            websiteHost: { type: ["string", "null"] },
            instagramUrl: { type: ["string", "null"] },
            country: { type: ["string", "null"] },
            city: { type: ["string", "null"] },
            productHints: {
              type: "array",
              items: { type: "string" },
            },
            socialLinks: {
              type: "array",
              items: { type: "string" },
            },
            confidence: {
              type: "number",
              minimum: 0,
              maximum: 1,
            },
            warnings: {
              type: "array",
              items: { type: "string" },
            },
          },
          required: [
            "brandName",
            "description",
            "websiteUrl",
            "websiteHost",
            "instagramUrl",
            "country",
            "city",
            "productHints",
            "socialLinks",
            "confidence",
            "warnings",
          ],
        },
      },
    },
  };

  const response = await axios.post("https://api.openai.com/v1/responses", payload, {
    timeout: 45000,
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
  });

  const outputText = readOpenAiOutputText(response.data);
  if (!outputText) {
    throw new Error("openai_empty_output");
  }

  let parsed;
  try {
    parsed = JSON.parse(outputText);
  } catch {
    throw new Error("openai_invalid_json");
  }

  return {
    brandName: cleanText(parsed.brandName, 200) || null,
    description: cleanText(parsed.description, 500) || null,
    websiteUrl: normalizeMaybeUrl(url, parsed.websiteUrl),
    websiteHost: normalizeHost(parsed.websiteHost || parsed.websiteUrl || url),
    instagramUrl: normalizeMaybeUrl(url, parsed.instagramUrl),
    country: cleanText(parsed.country, 120) || null,
    city: cleanText(parsed.city, 120) || null,
    productHints: uniqueStrings(parsed.productHints || [], 12),
    socialLinks: uniqueStrings(parsed.socialLinks || [], 20),
    confidence: Number.isFinite(Number(parsed.confidence))
      ? Math.max(0, Math.min(1, Number(parsed.confidence)))
      : 0,
    warnings: uniqueStrings(parsed.warnings || [], 10),
  };
}

/* =========================
   BRAND IMPORT
========================= */
exports.importBrandFromUrl = onRequest(
  {
    cors: true,
    timeoutSeconds: 60,
    memory: "512MiB",
    region: "us-central1",
    secrets: [OPENAI_API_KEY],
  },
  async (req, res) => {
    try {
      const url = req.body?.url;
      if (!url) {
        return res.status(400).json({ error: "invalid_url" });
      }

      const html = await fetchHtmlPage(url);
      const $ = cheerio.load(html);

      const title = cleanText($("title").text(), 200);
      const description =
        cleanText($('meta[name="description"]').attr("content"), 500) ||
        cleanText($('meta[property="og:description"]').attr("content"), 500) ||
        null;

      const logo =
        normalizeMaybeUrl(url, $('meta[property="og:image"]').attr("content")) ||
        normalizeMaybeUrl(url, $("link[rel='icon']").attr("href"));

      const basicPreview = {
        detectedBrandName: title ? title.split("|")[0].trim() : null,
        detectedDescription: description,
        detectedLogoUrl: logo || null,
        detectedWebsite: url,
      };

      let aiResult = null;

      try {
        aiResult = await runBrandAiExtraction(url, html, basicPreview);
      } catch (aiError) {
        logger.warn("AI brand extraction failed, falling back to basic preview", {
          message: aiError?.message,
          url,
        });
      }

      const result = {
        detectedBrandName: aiResult?.brandName || basicPreview.detectedBrandName || null,
        detectedDescription: aiResult?.description || basicPreview.detectedDescription || null,
        detectedLogoUrl: basicPreview.detectedLogoUrl || null,
        detectedCoverImageUrl: basicPreview.detectedLogoUrl || null,
        detectedWebsite: aiResult?.websiteUrl || url,
        detectedInstagram: aiResult?.instagramUrl || null,
        detectedCountry: aiResult?.country || null,
        detectedCity: aiResult?.city || null,
        detectedProductHints: aiResult?.productHints || [],
        detectedSocialLinks: aiResult?.socialLinks || [],
        aiWebsiteHost: aiResult?.websiteHost || normalizeHost(url),
        aiConfidence: aiResult?.confidence ?? null,
        extractionWarnings: aiResult?.warnings || [],
        extractionConfidenceNotes: aiResult
          ? ["basic_parse", "ai_enhanced"]
          : ["basic_parse", "ai_fallback_basic_only"],
      };

      const hasUsefulData = Boolean(
        result.detectedBrandName ||
          result.detectedDescription ||
          result.detectedLogoUrl ||
          result.detectedWebsite
      );

      if (!hasUsefulData) {
        return res.status(422).json({ error: "no_brand_data" });
      }

      return res.json(result);
    } catch (err) {
      logger.error("Brand import failed", err);

      const msg = String(err.message || "");

      if (msg.includes("fetch_failed")) {
        return res.status(502).json({ error: "unreachable_page" });
      }

      if (msg.includes("empty_html_response") || msg.includes("non_html_response")) {
        return res.status(422).json({ error: "no_brand_data" });
      }

      return res.status(500).json({ error: "brand_import_failed" });
    }
  }
);

/* =========================
   PRODUCT IMPORT
========================= */
exports.importProductFromUrl = onRequest(
  {
    cors: true,
    timeoutSeconds: 20,
    memory: "256MiB",
    region: "us-central1",
  },
  async (req, res) => {
    try {
      const url = req.body?.url;
      if (!url) {
        return res.status(400).json({ error: "invalid_url" });
      }

      const html = await fetchHtmlPage(url);
      const $ = cheerio.load(html);

      const name = cleanText($("title").text(), 200) || null;
      const description =
        cleanText($('meta[name="description"]').attr("content"), 500) ||
        cleanText($('meta[property="og:description"]').attr("content"), 500) ||
        null;

      return res.json({
        detectedProductName: name,
        detectedDescription: description,
      });
    } catch (err) {
      logger.error("Product import failed", err);
      return res.status(500).json({ error: "product_import_failed" });
    }
  }
);

/* =========================
   REVIEW AGGREGATE
========================= */
exports.syncReviewAggregates = onDocumentWritten(
  {
    document: "reviews/{reviewId}",
    region: "us-central1",
  },
  async () => {}
);