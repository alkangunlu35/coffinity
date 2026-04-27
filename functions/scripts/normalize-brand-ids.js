#!/usr/bin/env node
"use strict";

/**
 * One-time Firestore migration for brands collection:
 * Ensure stored field `id` equals Firestore document id for every doc in `brands`.
 *
 * Usage:
 *   node scripts/normalize-brand-ids.js --dry-run
 *   node scripts/normalize-brand-ids.js --apply
 *
 * Optional:
 *   --project=<firebase-project-id>
 *   --page-size=300
 */

const path = require("path");
const fs = require("fs");
const admin = require("firebase-admin");

const BRANDS_COLLECTION = "brands";
const DEFAULT_PAGE_SIZE = 300;
const MAX_BATCH_SIZE = 450;

function parseArgs(argv) {
  const args = new Map();
  for (const raw of argv) {
    if (!raw.startsWith("--")) continue;
    const withoutPrefix = raw.slice(2);
    const [key, value] = withoutPrefix.split("=", 2);
    args.set(key, value === undefined ? "true" : value);
  }
  return args;
}

function parseBooleanFlag(args, key, fallback = false) {
  if (!args.has(key)) return fallback;
  const value = String(args.get(key)).trim().toLowerCase();
  if (value === "true" || value === "1" || value === "yes") return true;
  if (value === "false" || value === "0" || value === "no") return false;
  return fallback;
}

function parseNumberFlag(args, key, fallback) {
  if (!args.has(key)) return fallback;
  const value = Number(args.get(key));
  return Number.isFinite(value) && value > 0 ? Math.floor(value) : fallback;
}

function readProjectIdFromFirebaserc() {
  try {
    const firebasercPath = path.resolve(__dirname, "..", "..", ".firebaserc");
    const content = fs.readFileSync(firebasercPath, "utf8");
    const parsed = JSON.parse(content);
    return parsed?.projects?.default || null;
  } catch {
    return null;
  }
}

function initFirestore(projectId) {
  if (!admin.apps.length) {
    const options = projectId ? { projectId } : undefined;
    admin.initializeApp(options);
  }
  return admin.firestore();
}

function normalizeStoredId(value) {
  if (typeof value !== "string") return "";
  return value.trim();
}

async function commitBatchSafely(batchOps) {
  if (batchOps.length === 0) return 0;
  const db = admin.firestore();
  let committed = 0;
  for (let i = 0; i < batchOps.length; i += MAX_BATCH_SIZE) {
    const chunk = batchOps.slice(i, i + MAX_BATCH_SIZE);
    const batch = db.batch();
    for (const op of chunk) {
      batch.update(op.ref, { id: op.id });
    }
    await batch.commit();
    committed += chunk.length;
  }
  return committed;
}

async function run() {
  const args = parseArgs(process.argv.slice(2));
  const dryRun = parseBooleanFlag(args, "dry-run", true) && !parseBooleanFlag(args, "apply", false);
  const projectId = args.get("project") || readProjectIdFromFirebaserc();
  const pageSize = parseNumberFlag(args, "page-size", DEFAULT_PAGE_SIZE);

  const db = initFirestore(projectId);
  const documentIdField = admin.firestore.FieldPath.documentId();

  console.log("=== Coffinity Brand ID Normalization ===");
  console.log(`Project       : ${projectId || "(auto)"}`);
  console.log(`Collection    : ${BRANDS_COLLECTION}`);
  console.log(`Mode          : ${dryRun ? "DRY RUN" : "APPLY"}`);
  console.log(`Page size     : ${pageSize}`);
  console.log("----------------------------------------");

  let scanned = 0;
  let alreadyValid = 0;
  let updated = 0;
  let failed = 0;
  let lastDoc = null;

  while (true) {
    let query = db
      .collection(BRANDS_COLLECTION)
      .orderBy(documentIdField)
      .limit(pageSize);
    if (lastDoc) {
      query = query.startAfter(lastDoc);
    }

    const snapshot = await query.get();
    if (snapshot.empty) break;

    const updates = [];

    for (const doc of snapshot.docs) {
      scanned += 1;
      const docId = doc.id;
      const storedId = normalizeStoredId(doc.get("id"));

      if (storedId === docId) {
        alreadyValid += 1;
        continue;
      }

      console.log(
        `[PLAN] ${docId} | stored.id=${storedId || "(missing)"} -> ${docId}`
      );

      if (!dryRun) {
        updates.push({ ref: doc.ref, id: docId });
      }
    }

    if (!dryRun && updates.length > 0) {
      try {
        const committed = await commitBatchSafely(updates);
        updated += committed;
      } catch (error) {
        failed += updates.length;
        console.error(
          `[ERROR] Failed to update ${updates.length} docs in current page:`,
          error?.message || error
        );
      }
    }

    lastDoc = snapshot.docs[snapshot.docs.length - 1];
  }

  if (dryRun) {
    updated = scanned - alreadyValid;
  }

  console.log("----------------------------------------");
  console.log("Summary:");
  console.log(`- Total scanned : ${scanned}`);
  console.log(`- Already valid : ${alreadyValid}`);
  console.log(`- Updated       : ${updated}`);
  console.log(`- Failed        : ${failed}`);
  console.log("Done.");
}

run().catch((error) => {
  console.error("Migration failed:", error?.message || error);
  process.exitCode = 1;
});

