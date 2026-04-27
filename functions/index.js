// FILE: functions/index.js
// FULL REPLACEMENT

const { onRequest } = require("firebase-functions/v2/https");
const {
  onDocumentCreated,
  onDocumentUpdated,
  onDocumentWritten,
} = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const axios = require("axios");
const cheerio = require("cheerio");
const crypto = require("crypto");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const TIMEOUT_MS = 15000;
const USER_AGENT =
  "Mozilla/5.0 (compatible; CoffinityBot/1.0; +https://coffinity.net)";
const IMPORT_ALLOWED_ROLES = new Set(["super_admin", "brand_admin"]);

/* =========================
   AUTH / ACCESS HELPERS
========================= */
function createHttpError(status, code, message) {
  const error = new Error(message || code);
  error.status = status;
  error.code = code;
  return error;
}

function getBearerToken(req) {
  const header = String(req.headers?.authorization || req.headers?.Authorization || "").trim();
  if (!header) return "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  return match?.[1]?.trim() || "";
}

function normalizeRoleList(value) {
  if (Array.isArray(value)) {
    return value
      .map((item) => String(item || "").trim().toLowerCase())
      .filter(Boolean);
  }

  if (typeof value === "string") {
    return String(value)
      .split(",")
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean);
  }

  return [];
}

function hasAllowedImportRole({ claims = {}, userData = {} }) {
  const claimRoles = [
    ...normalizeRoleList(claims.role),
    ...normalizeRoleList(claims.roles),
  ];
  const userRoles = [
    ...normalizeRoleList(userData.role),
    ...normalizeRoleList(userData.roles),
  ];

  const mergedRoles = new Set([...claimRoles, ...userRoles]);

  for (const role of IMPORT_ALLOWED_ROLES) {
    if (mergedRoles.has(role)) return true;
  }

  if (
    claims.admin === true ||
    claims.isAdmin === true ||
    claims.super_admin === true ||
    claims.brand_admin === true ||
    userData.isAdmin === true ||
    userData.isSuperAdmin === true ||
    userData.isBrandAdmin === true
  ) {
    return true;
  }

  return false;
}

async function verifyImportAccess(req) {
  const token = getBearerToken(req);
  if (!token) {
    throw createHttpError(401, "missing_auth", "Authorization token is required");
  }

  let decodedToken;
  try {
    decodedToken = await admin.auth().verifyIdToken(token, true);
  } catch (error) {
    logger.warn("Import auth token verification failed", {
      message: String(error?.message || ""),
    });
    throw createHttpError(401, "invalid_auth", "Invalid or expired auth token");
  }

  const uid = String(decodedToken?.uid || "").trim();
  if (!uid) {
    throw createHttpError(401, "invalid_auth", "Invalid auth token payload");
  }

  let userData = {};
  try {
    const userSnap = await db.collection("users").doc(uid).get();
    userData = userSnap.exists ? (userSnap.data() || {}) : {};
  } catch (error) {
    logger.error("Import auth user document lookup failed", {
      uid,
      message: String(error?.message || ""),
    });
    throw createHttpError(500, "auth_lookup_failed", "Failed to verify user access");
  }

  const allowed = hasAllowedImportRole({
    claims: decodedToken,
    userData,
  });

  if (!allowed) {
    throw createHttpError(403, "forbidden", "Insufficient permissions for import");
  }

  return {
    uid,
    email: String(decodedToken.email || userData.email || "").trim(),
    role: String(userData.role || decodedToken.role || "").trim(),
  };
}

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
      const access = await verifyImportAccess(req);
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
          uid: access.uid,
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
      logger.error("Brand import failed", {
        code: String(err?.code || ""),
        status: Number(err?.status || 500),
        message: String(err?.message || ""),
      });

      const httpStatus = Number(err?.status || 500);
      const code = String(err?.code || "");
      const msg = String(err?.message || "");

      if (httpStatus === 401 || code === "missing_auth" || code === "invalid_auth") {
        return res.status(401).json({ error: code || "unauthorized" });
      }

      if (httpStatus === 403 || code === "forbidden") {
        return res.status(403).json({ error: "forbidden" });
      }

      if (httpStatus === 500 && code === "auth_lookup_failed") {
        return res.status(500).json({ error: "auth_lookup_failed" });
      }

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
      await verifyImportAccess(req);

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
      logger.error("Product import failed", {
        code: String(err?.code || ""),
        status: Number(err?.status || 500),
        message: String(err?.message || ""),
      });

      const httpStatus = Number(err?.status || 500);
      const code = String(err?.code || "");
      const msg = String(err?.message || "");

      if (httpStatus === 401 || code === "missing_auth" || code === "invalid_auth") {
        return res.status(401).json({ error: code || "unauthorized" });
      }

      if (httpStatus === 403 || code === "forbidden") {
        return res.status(403).json({ error: "forbidden" });
      }

      if (httpStatus === 500 && code === "auth_lookup_failed") {
        return res.status(500).json({ error: "auth_lookup_failed" });
      }

      if (msg.includes("fetch_failed")) {
        return res.status(502).json({ error: "unreachable_page" });
      }

      if (msg.includes("empty_html_response") || msg.includes("non_html_response")) {
        return res.status(422).json({ error: "no_product_data" });
      }

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

/* =========================
   PHASE-1 NOTIFICATIONS
========================= */
function boolOrDefault(value, fallback) {
  return typeof value === "boolean" ? value : fallback;
}

function tokenDocId(token) {
  return crypto.createHash("sha256").update(String(token || "")).digest("hex");
}

function safeString(value, maxLength = 300) {
  return String(value || "").trim().slice(0, maxLength);
}

function shortMessage(value, maxLength = 100) {
  const text = safeString(value, 1000).replace(/\s+/g, " ");
  if (!text) return "";
  if (text.length <= maxLength) return text;
  return `${text.slice(0, Math.max(1, maxLength - 1)).trim()}…`;
}

function stringifyDataValue(value) {
  if (value === undefined || value === null) return "";
  return String(value);
}

function buildNotificationDeliveryId({
  type = "",
  route = "",
  eventId = "",
  inviteId = "",
  chatId = "",
  messageId = "",
  senderId = "",
  userId = "",
}) {
  return crypto
    .createHash("sha1")
    .update(
      [
        safeString(type, 80),
        safeString(route, 80),
        safeString(eventId, 200),
        safeString(inviteId, 200),
        safeString(chatId, 200),
        safeString(messageId, 200),
        safeString(senderId, 200),
        safeString(userId, 200),
      ].join("|")
    )
    .digest("hex");
}

function normalizeNotificationData(data = {}) {
  const raw = {};
  Object.entries(data || {}).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    raw[key] = stringifyDataValue(value);
  });

  const type = safeString(raw.type || raw.notif_type, 80);
  const route = safeString(raw.route || raw.notif_route, 80);
  const eventId = safeString(raw.eventId || raw.meetId || raw.notif_eventId, 200);
  const inviteId = safeString(raw.inviteId || raw.notif_inviteId, 200);
  const chatId = safeString(raw.chatId || raw.notif_chatId, 200);
  const messageId = safeString(raw.messageId || raw.notif_messageId, 200);
  const senderId = safeString(raw.senderId || raw.senderUserId || raw.notif_senderId, 200);
  const userId = safeString(raw.userId || raw.recipientUserId || raw.notif_userId, 200);
  const deliveryId =
    safeString(raw.deliveryId || raw.notif_deliveryId || raw["google.message_id"] || raw.message_id, 200) ||
    buildNotificationDeliveryId({
      type,
      route,
      eventId,
      inviteId,
      chatId,
      messageId,
      senderId,
      userId,
    });

  return {
    ...raw,

    type,
    route,
    eventId,
    meetId: eventId,
    inviteId,
    chatId,
    messageId,
    senderId,
    userId,
    deliveryId,

    notif_type: type,
    notif_route: route,
    notif_eventId: eventId,
    notif_inviteId: inviteId,
    notif_chatId: chatId,
    notif_messageId: messageId,
    notif_senderId: senderId,
    notif_userId: userId,
    notif_deliveryId: deliveryId,
  };
}

async function acquireDispatchLock(lockId, payload) {
  const normalizedLockId = safeString(lockId, 200);
  if (!normalizedLockId) return false;

  const lockRef = db.collection("_notificationDispatchLocks").doc(normalizedLockId);
  try {
    await lockRef.create({
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      ...payload,
    });
    return true;
  } catch (error) {
    const code = String(error?.code || "");
    const message = safeString(error?.message, 300);
    if (code === "already-exists" || code === "6" || message.includes("Already exists")) {
      return false;
    }
    throw error;
  }
}

async function loadNotificationPrefs(userId) {
  const settingsRef = db
    .collection("users")
    .doc(userId)
    .collection("private")
    .doc("settings");
  const settingsSnap = await settingsRef.get();
  const raw = settingsSnap.exists ? settingsSnap.data() : {};
  return {
    notificationsEnabled: boolOrDefault(raw.notificationsEnabled, true),
    inviteNotifications: boolOrDefault(raw.inviteNotifications, true),
    chatNotifications: boolOrDefault(raw.chatNotifications, true),
    meetParticipants: boolOrDefault(raw.meetParticipants, true),
    meetUpdates: boolOrDefault(raw.meetUpdates, true),
    meetReminders: boolOrDefault(raw.meetReminders, true),
    nearbyMeet: boolOrDefault(raw.nearbyMeet, true),
  };
}

async function loadDeviceTokens(userId) {
  const tokenSnap = await db
    .collection("users")
    .doc(userId)
    .collection("private")
    .doc("settings")
    .collection("deviceTokens")
    .get();

  return tokenSnap.docs
    .map((doc) => String(doc.get("token") || "").trim())
    .filter((token) => token.length > 0);
}

async function resolveDisplayName(userId) {
  if (!userId) return null;
  const userSnap = await db.collection("users").doc(userId).get();
  if (!userSnap.exists) return null;
  const data = userSnap.data() || {};
  const displayName = String(data.displayName || data.fullName || "").trim();
  if (displayName) return displayName;
  const email = String(data.email || "").trim();
  if (email.includes("@")) return email.split("@")[0];
  return null;
}

function normalizeParticipants(value) {
  if (!Array.isArray(value)) return [];
  return value
    .filter((item) => typeof item === "string")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function extractUserIdFromSettingsPath(path) {
  const parts = String(path || "").split("/");
  if (parts.length !== 4) return "";
  if (
    parts[0] !== "users" ||
    parts[2] !== "private" ||
    parts[3] !== "settings"
  ) {
    return "";
  }
  return String(parts[1] || "").trim();
}

async function loadNearbyMeetAudienceUserIds(excludeUserId) {
  const snapshot = await db
    .collectionGroup("settings")
    .where("notificationsEnabled", "==", true)
    .get();

  const userIds = new Set();
  snapshot.docs.forEach((doc) => {
    const raw = doc.data() || {};
    const nearbyMeet = boolOrDefault(raw.nearbyMeet, true);
    if (!nearbyMeet) return;

    const userId = extractUserIdFromSettingsPath(doc.ref.path);
    if (!userId || userId === excludeUserId) return;
    userIds.add(userId);
  });
  return Array.from(userIds);
}

async function cleanupInvalidTokens(userId, tokens, responses, logContext = {}) {
  const eventId = safeString(logContext.eventId, 200);
  const entityId = safeString(logContext.entityId, 200);
  const cleanup = [];
  responses.forEach((response, index) => {
    if (response.success) return;
    const code = String(response.error?.code || "");
    const isTokenInvalid =
      code.includes("registration-token-not-registered") ||
      code.includes("invalid-registration-token");
    if (!isTokenInvalid) return;
    const token = tokens[index];
    if (!token) return;
    logger.info("NOTIF_TOKEN_CLEANUP", {
      userId,
      eventId,
      entityId,
      tokenDocId: tokenDocId(token),
      code,
    });
    cleanup.push(
      db
        .collection("users")
        .doc(userId)
        .collection("private")
        .doc("settings")
        .collection("deviceTokens")
        .doc(tokenDocId(token))
        .delete()
    );
  });
  if (cleanup.length) {
    await Promise.allSettled(cleanup);
  }
}

async function sendNotificationToUser({
  userId,
  preferenceKey,
  title,
  body,
  data,
  logContext = {},
}) {
  const normalizedUserId = String(userId || "").trim();
  if (!normalizedUserId) return { sent: false, reason: "invalid_user" };

  const prefs = await loadNotificationPrefs(normalizedUserId);
  const categoryAllowed = boolOrDefault(prefs[preferenceKey], true);
  if (!prefs.notificationsEnabled || !categoryAllowed) {
    return {
      sent: false,
      reason: "prefs_disabled",
      preferenceKey: safeString(preferenceKey, 80),
      notificationsEnabled: boolOrDefault(prefs.notificationsEnabled, true),
      categoryPreferenceEnabled: categoryAllowed,
    };
  }

  const tokens = await loadDeviceTokens(normalizedUserId);
  if (!tokens.length) return { sent: false, reason: "no_tokens" };

  const normalizedData = normalizeNotificationData(data || {});
  const payload = {
    tokens,
    notification: { title, body },
    data: {
      ...normalizedData,
      title: safeString(title, 120),
      body: safeString(body, 220),
    },
    android: {
      priority: "high",
      notification: {
        channelId: "coffinity_general",
      },
    },
  };

  const result = await admin.messaging().sendEachForMulticast(payload);
  await cleanupInvalidTokens(normalizedUserId, tokens, result.responses, logContext);
  return { sent: result.successCount > 0, successCount: result.successCount };
}

async function sendNotificationWithLog({
  logTag,
  userId,
  eventId,
  entityId,
  preferenceKey,
  title,
  body,
  data,
}) {
  const normalizedUserId = safeString(userId, 200);
  const normalizedEventId = safeString(eventId, 200);
  const normalizedEntityId = safeString(entityId, 200);
  if (!normalizedUserId) return { sent: false, reason: "invalid_user" };

  try {
    const result = await sendNotificationToUser({
      userId: normalizedUserId,
      preferenceKey,
      title: safeString(title, 120) || "Coffinity",
      body: safeString(body, 220) || "Bir kullanıcı",
      data: data || {},
      logContext: {
        eventId: normalizedEventId,
        entityId: normalizedEntityId,
      },
    });

    if (result.reason === "no_tokens") {
      logger.info("NOTIF_NO_TOKENS", {
        userId: normalizedUserId,
        eventId: normalizedEventId,
        entityId: normalizedEntityId,
      });
      return result;
    }

    if (!result.sent) {
      logger.error("NOTIF_SEND_FAILED", {
        userId: normalizedUserId,
        eventId: normalizedEventId,
        entityId: normalizedEntityId,
        reason: safeString(result.reason, 120),
        ...(result.reason === "prefs_disabled"
          ? {
              preferenceKey: safeString(result.preferenceKey || preferenceKey, 80),
              notificationsEnabled: boolOrDefault(result.notificationsEnabled, true),
              categoryPreferenceEnabled: boolOrDefault(
                result.categoryPreferenceEnabled,
                true
              ),
            }
          : {}),
      });
      return result;
    }

    logger.info(logTag, {
      userId: normalizedUserId,
      eventId: normalizedEventId,
      entityId: normalizedEntityId,
      successCount: Number(result.successCount || 0),
    });
    return result;
  } catch (error) {
    logger.error("NOTIF_SEND_FAILED", {
      userId: normalizedUserId,
      eventId: normalizedEventId,
      entityId: normalizedEntityId,
      message: safeString(error?.message, 300),
    });
    return { sent: false, reason: "send_exception" };
  }
}

function hasMeaningfulMeetUpdate(beforeData, afterData) {
  const meaningfulFields = [
    "title",
    "description",
    "scheduledAt",
    "time",
    "locationName",
    "latitude",
    "longitude",
    "maxParticipants",
    "purpose",
    "brewingType",
  ];
  return meaningfulFields.some((field) => {
    const beforeValue = beforeData?.[field] ?? null;
    const afterValue = afterData?.[field] ?? null;
    return beforeValue !== afterValue;
  });
}

function resolveInviteExpiryTimestamp(inviteData) {
  const endTime = Number(inviteData?.endTime || 0);
  if (Number.isFinite(endTime) && endTime > 0) return endTime;

  const startTime = Number(inviteData?.startTime || 0);
  if (Number.isFinite(startTime) && startTime > 0) return startTime + 2 * 60 * 60 * 1000;

  const inviteDate = Number(inviteData?.inviteDate || 0);
  if (Number.isFinite(inviteDate) && inviteDate > 0) return inviteDate + 24 * 60 * 60 * 1000;

  return 0;
}

exports.notifyMeetOwnerOnParticipantJoined = onDocumentWritten(
  {
    document: "events/{eventId}",
    region: "us-central1",
  },
  async (event) => {
    const beforeData = event.data?.before?.exists ? event.data.before.data() : null;
    const afterData = event.data?.after?.exists ? event.data.after.data() : null;
    if (!beforeData || !afterData) return;

    const hostId = String(afterData.hostId || "").trim();
    if (!hostId) return;

    const beforeParticipants = normalizeParticipants(beforeData.participants);
    const afterParticipants = normalizeParticipants(afterData.participants);

    if (afterParticipants.length <= beforeParticipants.length) return;

    const beforeSet = new Set(beforeParticipants);
    const addedParticipants = afterParticipants.filter(
      (participantId) => participantId && !beforeSet.has(participantId) && participantId !== hostId
    );
    if (!addedParticipants.length) return;

    const joinedUserId = addedParticipants[0];
    const joinedDisplayName = await resolveDisplayName(joinedUserId);
    const eventId = safeString(event.params.eventId, 200);
    const meetTitle = String(afterData.title || "").trim() || "Coffee Meet";
    const body = joinedDisplayName
      ? `${joinedDisplayName} joined your meet "${meetTitle}".`
      : `Someone joined your meet "${meetTitle}".`;

    await sendNotificationToUser({
      userId: hostId,
      preferenceKey: "meetParticipants",
      title: "Coffinity Meet",
      body,
      data: {
        type: "meet_participant_joined",
        route: "event_detail",
        eventId,
        userId: joinedUserId,
        participantUserId: joinedUserId,
      },
    });
  }
);

exports.notifyParticipantsOnMeetUpdate = onDocumentWritten(
  {
    document: "events/{eventId}",
    region: "us-central1",
  },
  async (event) => {
    const beforeData = event.data?.before?.exists ? event.data.before.data() : null;
    const afterData = event.data?.after?.exists ? event.data.after.data() : null;
    if (!beforeData || !afterData) return;
    if (!hasMeaningfulMeetUpdate(beforeData, afterData)) return;

    const eventId = String(event.params.eventId || "").trim();
    if (!eventId) return;

    const participants = normalizeParticipants(afterData.participants);
    if (!participants.length) return;

    const meetTitle = String(afterData.title || "").trim() || "Coffee Meet";
    const body = `A meet you joined has been updated: "${meetTitle}".`;

    for (const participantId of participants) {
      await sendNotificationToUser({
        userId: participantId,
        preferenceKey: "meetUpdates",
        title: "Meet updated",
        body,
        data: {
          type: "meet_update",
          route: "event_detail",
          eventId,
        },
      });
    }
  }
);

exports.notifyUsersOnMeetCreated = onDocumentWritten(
  {
    document: "events/{eventId}",
    region: "us-central1",
  },
  async (event) => {
    const beforeExists = event.data?.before?.exists === true;
    const afterExists = event.data?.after?.exists === true;
    if (beforeExists || !afterExists) return;

    const eventId = String(event.params.eventId || "").trim();
    if (!eventId) return;

    const afterData = event.data?.after?.data() || {};
    const hostId = String(afterData.hostId || "").trim();
    if (!hostId) return;

    const status = String(afterData.status || "").trim().toLowerCase();
    const isDeleted = afterData.isDeleted === true;
    if (isDeleted || (status && status !== "active")) {
      logger.info("notifyUsersOnMeetCreated skipped non-active event", {
        eventId,
        status,
        isDeleted,
      });
      return;
    }

    const audienceUserIds = await loadNearbyMeetAudienceUserIds(hostId);
    if (!audienceUserIds.length) {
      logger.info("notifyUsersOnMeetCreated no eligible audience", { eventId, hostId });
      return;
    }

    const meetTitle = String(afterData.title || "").trim();
    const payloadData = {
      type: "meet_created",
      route: "social",
      eventId,
      hostId,
      meetTitle,
    };

    const title = "Coffinity Meet";
    const body = "Yakında bir kahve meet oluşturuldu. Katılmak ister misin?";

    const sendResults = await Promise.allSettled(
      audienceUserIds.map((userId) =>
        sendNotificationToUser({
          userId,
          preferenceKey: "nearbyMeet",
          title,
          body,
          data: payloadData,
        })
      )
    );

    const successCount = sendResults.filter(
      (result) => result.status === "fulfilled" && result.value?.sent === true
    ).length;
    const failureCount = sendResults.length - successCount;
    const rejectedCount = sendResults.filter(
      (result) => result.status === "rejected"
    ).length;

    logger.info("notifyUsersOnMeetCreated completed", {
      eventId,
      hostId,
      audienceCount: audienceUserIds.length,
      successCount,
      failureCount,
      rejectedCount,
    });
  }
);

exports.notifyOnCoffeeInviteCreated = onDocumentCreated(
  {
    document: "coffeeInvites/{inviteId}",
    region: "us-central1",
  },
  async (event) => {
    const inviteId = safeString(event.params?.inviteId, 200);
    const inviteData = event.data?.data() || {};
    const senderId = safeString(inviteData.senderUserId, 200);
    const recipientId = safeString(inviteData.recipientUserId, 200);
    const eventId = safeString(inviteData.eventId || inviteData.meetId, 200);

    if (!inviteId || !senderId || !recipientId) return;
    if (senderId === recipientId) return;

    const lockId = `notif_invite_created_${safeString(event.id, 200) || inviteId}`;
    const lockAcquired = await acquireDispatchLock(lockId, {
      type: "invite_new",
      inviteId,
      eventId,
      userId: recipientId,
    });
    if (!lockAcquired) return;

    const senderName = (await resolveDisplayName(senderId)) || "Bir kullanıcı";

    await sendNotificationWithLog({
      logTag: "NOTIF_INVITE_CREATED",
      userId: recipientId,
      eventId,
      entityId: inviteId,
      preferenceKey: "inviteNotifications",
      title: "Yeni davet",
      body: `${senderName} seni bir coffee meet'e davet etti`,
      data: {
        type: "invite_new",
        route: "social",
        eventId: eventId || "",
        inviteId,
        senderId,
      },
    });
  }
);

exports.notifyOnCoffeeInviteAccepted = onDocumentUpdated(
  {
    document: "coffeeInvites/{inviteId}",
    region: "us-central1",
  },
  async (event) => {
    const inviteId = safeString(event.params?.inviteId, 200);
    const beforeData = event.data?.before?.data() || {};
    const afterData = event.data?.after?.data() || {};
    const beforeStatus = safeString(beforeData.status, 40).toLowerCase();
    const afterStatus = safeString(afterData.status, 40).toLowerCase();
    if (beforeStatus === "accepted" || afterStatus !== "accepted") return;

    const actorUserId = safeString(afterData.recipientUserId, 200);
    const recipientUserId = safeString(afterData.senderUserId, 200);
    const eventId = safeString(afterData.eventId || afterData.meetId, 200);
    if (!inviteId || !actorUserId || !recipientUserId) return;
    if (actorUserId === recipientUserId) return;

    const lockId =
      `notif_invite_accepted_${safeString(event.id, 200) || `${inviteId}_${afterData.updatedAt || ""}`}`;
    const lockAcquired = await acquireDispatchLock(lockId, {
      type: "invite_accepted",
      inviteId,
      eventId,
      userId: recipientUserId,
    });
    if (!lockAcquired) return;

    let chatId = "";
    try {
      const chatSnap = await db
        .collection("coffeeChats")
        .where("inviteId", "==", inviteId)
        .limit(1)
        .get();

      if (!chatSnap.empty) {
        chatId = chatSnap.docs[0].id;
      }
    } catch (error) {
      logger.error("INVITE_ACCEPTED_CHAT_LOOKUP_FAILED", {
        inviteId,
        eventId,
        message: safeString(error?.message, 300),
      });
    }

    const receiverName = (await resolveDisplayName(actorUserId)) || "Bir kullanıcı";

    await sendNotificationWithLog({
      logTag: "NOTIF_INVITE_ACCEPTED",
      userId: recipientUserId,
      eventId,
      entityId: inviteId,
      preferenceKey: "inviteNotifications",
      title: "Davet kabul edildi",
      body: `${receiverName} davetini kabul etti`,
      data: {
        type: "invite_accepted",
        route: chatId ? "chat" : "social",
        eventId: eventId || "",
        inviteId,
        userId: actorUserId,
        chatId: chatId || "",
      },
    });
  }
);

exports.notifyOnCoffeeChatMessageCreated = onDocumentCreated(
  {
    document: "coffeeChats/{chatId}/messages/{messageId}",
    region: "us-central1",
  },
  async (event) => {
    const chatId = safeString(event.params?.chatId, 200);
    const messageId = safeString(event.params?.messageId, 200);
    const messageData = event.data?.data() || {};
    const senderId = safeString(messageData.senderUserId, 200);
    if (!chatId || !messageId || !senderId) return;

    if (senderId === "system" || messageId === "__system_activation__") return;

    const lockId = `notif_chat_message_${chatId}_${messageId}`;
    const lockAcquired = await acquireDispatchLock(lockId, {
      type: "chat_message",
      chatId,
      messageId,
      userId: senderId,
    });
    if (!lockAcquired) return;

    const chatSnap = await db.collection("coffeeChats").doc(chatId).get();
    if (!chatSnap.exists) return;
    const chatData = chatSnap.data() || {};

    const participants = normalizeParticipants(chatData.participantIds);
    const recipients = Array.from(new Set(participants))
      .map((id) => safeString(id, 200))
      .filter((id) => id && id !== senderId);
    if (!recipients.length) return;

    const inviteId = safeString(chatData.inviteId, 200);
    const eventId = safeString(chatData.eventId || chatData.meetId || inviteId, 200);
    const senderName = (await resolveDisplayName(senderId)) || "Bir kullanıcı";
    const preview = shortMessage(messageData.text, 100);
    const body = preview ? `${senderName}: ${preview}` : `${senderName}: Yeni bir mesaj`;

    await Promise.allSettled(
      recipients.map((recipientId) =>
        sendNotificationWithLog({
          logTag: "NOTIF_CHAT_MESSAGE",
          userId: recipientId,
          eventId,
          entityId: messageId,
          preferenceKey: "chatNotifications",
          title: "Yeni mesaj",
          body,
          data: {
            type: "chat_message",
            route: "chat",
            eventId: eventId || "",
            messageId,
            senderId,
            inviteId,
            chatId,
          },
        })
      )
    );
  }
);

exports.sendMeetReminders = onSchedule(
  {
    schedule: "every 15 minutes",
    region: "us-central1",
    timeZone: "Etc/UTC",
  },
  async () => {
    const now = Date.now();
    const oneHourFromNow = now + 60 * 60 * 1000;
    const windowStart = oneHourFromNow - 10 * 60 * 1000;
    const windowEnd = oneHourFromNow + 10 * 60 * 60 * 1000 / 60; // preserved original timing intent footprint
    const snapshot = await db
      .collection("events")
      .whereEqualTo("isDeleted", false)
      .whereEqualTo("status", "active")
      .where("scheduledAt", ">=", windowStart)
      .where("scheduledAt", "<=", windowEnd)
      .get();

    for (const doc of snapshot.docs) {
      const data = doc.data() || {};
      const scheduledAt = Number(data.scheduledAt || 0);
      if (!Number.isFinite(scheduledAt) || scheduledAt <= 0) continue;

      const alreadySentFor = Number(data.reminder1hScheduledAt || 0);
      if (alreadySentFor === scheduledAt) continue;

      const participants = normalizeParticipants(data.participants);
      if (!participants.length) {
        await doc.ref.set(
          {
            reminder1hScheduledAt: scheduledAt,
            reminder1hProcessedAt: now,
          },
          { merge: true }
        );
        continue;
      }

      const meetTitle = String(data.title || "").trim() || "Coffee Meet";
      const body = `Reminder: "${meetTitle}" starts in about 1 hour.`;

      for (const participantId of participants) {
        await sendNotificationToUser({
          userId: participantId,
          preferenceKey: "meetReminders",
          title: "Meet reminder",
          body,
          data: {
            type: "meet_reminder",
            route: "event_detail",
            eventId: doc.id,
            startTime: String(scheduledAt),
          },
        });
      }

      await doc.ref.set(
        {
          reminder1hScheduledAt: scheduledAt,
          reminder1hSentAt: now,
          reminder1hProcessedAt: now,
        },
        { merge: true }
      );
    }
  }
);

exports.runInviteEventLifecycle = onSchedule(
  {
    schedule: "every 30 minutes",
    region: "us-central1",
    timeZone: "Etc/UTC",
  },
  async () => {
    const now = Date.now();
    const pageSize = 200;

    let inviteLastDoc = null;
    let inviteExpiredCount = 0;
    while (true) {
      let query = db
        .collection("coffeeInvites")
        .where("status", "==", "pending")
        .limit(pageSize);

      if (inviteLastDoc) query = query.startAfter(inviteLastDoc);

      const snapshot = await query.get();
      if (snapshot.empty) break;

      const batch = db.batch();
      let batchCount = 0;

      snapshot.docs.forEach((doc) => {
        const data = doc.data() || {};
        const expiryAt = resolveInviteExpiryTimestamp(data);
        if (!Number.isFinite(expiryAt) || expiryAt <= 0 || expiryAt > now) return;

        batch.set(
          doc.ref,
          {
            status: "expired",
            expiredAt: now,
            updatedAt: now,
          },
          { merge: true }
        );
        batchCount += 1;
      });

      if (batchCount > 0) {
        await batch.commit();
        inviteExpiredCount += batchCount;
      }

      inviteLastDoc = snapshot.docs[snapshot.docs.length - 1];
      if (snapshot.size < pageSize) break;
    }

    let eventLastDoc = null;
    let eventCompletedCount = 0;
    while (true) {
      let query = db
        .collection("events")
        .where("status", "==", "active")
        .limit(pageSize);

      if (eventLastDoc) query = query.startAfter(eventLastDoc);

      const snapshot = await query.get();
      if (snapshot.empty) break;

      const batch = db.batch();
      let batchCount = 0;

      snapshot.docs.forEach((doc) => {
        const data = doc.data() || {};
        const isDeleted = data.isDeleted === true;
        const scheduledAt = Number(data.scheduledAt || 0);
        if (isDeleted) return;
        if (!Number.isFinite(scheduledAt) || scheduledAt <= 0 || scheduledAt > now) return;

        batch.set(
          doc.ref,
          {
            status: "completed",
            completedAt: now,
            updatedAt: now,
          },
          { merge: true }
        );
        batchCount += 1;
      });

      if (batchCount > 0) {
        await batch.commit();
        eventCompletedCount += batchCount;
      }

      eventLastDoc = snapshot.docs[snapshot.docs.length - 1];
      if (snapshot.size < pageSize) break;
    }

    logger.info("INVITE_EVENT_LIFECYCLE_COMPLETED", {
      inviteExpiredCount,
      eventCompletedCount,
      processedAt: now,
    });
  }
);