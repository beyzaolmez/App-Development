import { access, readFile } from "node:fs/promises";
import { resolve } from "node:path";
import { applicationDefault, initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const args = new Set(process.argv.slice(2));
const shouldWrite = args.has("--write");
const dryRun = args.has("--dry-run") || !shouldWrite;
const seedPath = resolve("app/build/reports/quest-seed/firebase-quests.seed.json");
const projectId = process.env.FIREBASE_PROJECT_ID || "momentum-p4g5";

const seed = JSON.parse(await readFile(seedPath, "utf8"));
validateSeed(seed);

if (dryRun) {
  console.log(`[dry-run] ${seed.documents.length} quest documents are ready for ${projectId}/${seed.collection}.`);
  seed.documents.forEach((document) => {
    console.log(`[dry-run] ${seed.collection}/${document.id}`);
  });
  console.log("Run `npm run seed:quests -- --write` to write these documents.");
  process.exit(0);
}

await validateApplicationCredentials();

initializeApp({
  credential: applicationDefault(),
  projectId
});

const firestore = getFirestore();
const batch = firestore.batch();

seed.documents.forEach((document) => {
  batch.set(firestore.collection(seed.collection).doc(document.id), document.data);
});

await batch.commit();
console.log(`Seeded ${seed.documents.length} quest documents to ${projectId}/${seed.collection}.`);

function validateSeed(seedContent) {
  if (seedContent?.collection !== "quests") {
    throw new Error("Seed file must target the quests collection.");
  }

  if (!Array.isArray(seedContent.documents) || seedContent.documents.length === 0) {
    throw new Error("Seed file must include at least one document.");
  }

  const ids = new Set();
  seedContent.documents.forEach((document, index) => {
    if (!document || typeof document !== "object") {
      throw new Error(`Document ${index} must be an object.`);
    }
    if (typeof document.id !== "string" || document.id.trim() === "") {
      throw new Error(`Document ${index} must have a non-blank id.`);
    }
    if (ids.has(document.id)) {
      throw new Error(`Duplicate quest id '${document.id}'.`);
    }
    ids.add(document.id);

    const data = document.data;
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      throw new Error(`Document '${document.id}' must include data.`);
    }
    if (data.isActive !== true) {
      throw new Error(`Document '${document.id}' must be active.`);
    }
    if (!Array.isArray(data.steps) || data.steps.length === 0) {
      throw new Error(`Document '${document.id}' must include steps.`);
    }
  });
}

async function validateApplicationCredentials() {
  const credentialsPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;
  if (!credentialsPath) {
    throw new Error(
      "GOOGLE_APPLICATION_CREDENTIALS is not set. Set it to the real Firebase service account JSON path before using --write."
    );
  }

  if (credentialsPath.includes("C:\\path\\to") || credentialsPath.includes("/path/to")) {
    throw new Error(
      `GOOGLE_APPLICATION_CREDENTIALS still points at the example placeholder: ${credentialsPath}`
    );
  }

  try {
    await access(credentialsPath);
  } catch {
    throw new Error(
      `GOOGLE_APPLICATION_CREDENTIALS points to a file that does not exist: ${credentialsPath}`
    );
  }
}
