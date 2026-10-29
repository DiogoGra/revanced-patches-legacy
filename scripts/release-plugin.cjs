const assert = require("node:assert/strict");
const { spawnSync } = require("node:child_process");
const { createHash } = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const AdmZip = require("adm-zip");

const repository = "https://github.com/DiogoGra/revanced-patches-legacy";

function checkVersion(version) {
  assert.match(version, /^\d+\.\d+\.\d+(?:-dev\.\d+)?$/, "Invalid release version");
  return version;
}

function readVersion(cwd) {
  const properties = fs.readFileSync(path.join(cwd, "gradle.properties"), "utf8");
  const matches = [...properties.matchAll(/^version\s*=\s*(\S+)\s*$/gm)];
  assert.equal(matches.length, 1, "Expected exactly one Gradle version property");
  return checkVersion(matches[0][1]);
}

function updateVersion(cwd, version) {
  checkVersion(version);
  readVersion(cwd);
  const file = path.join(cwd, "gradle.properties");
  const properties = fs.readFileSync(file, "utf8");
  fs.writeFileSync(file, properties.replace(/^version\s*=[^\r\n]*$/m, `version = ${version}`));
}

function validateBundle(file, version) {
  checkVersion(version);
  const data = fs.readFileSync(file);
  const zip = new AdmZip(data);
  const entries = zip.getEntries();
  const names = entries.map((entry) => entry.entryName);
  assert.equal(new Set(names).size, names.length, "Duplicate entries in patch bundle");
  // Read every entry so truncated archives and CRC errors fail before publication.
  for (const entry of entries) entry.getData();
  const manifest = zip.readAsText("META-INF/MANIFEST.MF").replace(/\r?\n /g, "");
  const mainSection = manifest.split(/\r?\n\r?\n/)[0];
  const attributes = Object.fromEntries(mainSection.split(/\r?\n/).filter(Boolean).map((line) => {
    const colon = line.indexOf(": ");
    assert.ok(colon > 0, "Invalid manifest attribute");
    return [line.slice(0, colon), line.slice(colon + 2)];
  }));
  assert.equal(attributes.Version, version, "Bundle manifest version does not match release");
  assert.equal(attributes.Source, repository, "Bundle source does not match this fork");
  const dex = zip.getEntry("classes.dex");
  assert.ok(dex, "Android bundle is missing classes.dex; use :patches:buildAndroid");
  const dexData = dex.getData();
  assert.ok(dexData.length >= 112 && /^dex\n0\d{2}\0/.test(dexData.subarray(0, 8).toString("ascii")),
    "Invalid DEX header");
  assert.ok(names.some((name) => name.startsWith("app/morphe/patches/") && name.endsWith(".class")),
    "Bundle is missing JVM patch classes");
  return { version, sha256: createHash("sha256").update(data).digest("hex"), bytes: data.length };
}

function validatePatchList(cwd, version) {
  const list = JSON.parse(fs.readFileSync(path.join(cwd, "patches-list.json"), "utf8"));
  assert.equal(list.version, `v${version}`, "Generated patch list version does not match bundle");
  assert.ok(Array.isArray(list.patches) && list.patches.length > 0, "Generated patch list is empty");
  assert.ok(list.patches.some((patch) =>
    patch.compatiblePackages?.["com.google.android.youtube"]?.includes("19.16.39")),
  "Generated patch list does not support YouTube 19.16.39");
  return list.patches.length;
}

function makeMetadata(version, notes, now = new Date()) {
  checkVersion(version);
  assert.ok(notes?.trim(), "Release notes are empty");
  return {
    created_at: now.toISOString().slice(0, 19),
    description: notes.trim(),
    signature_download_url: null,
    download_url: `${repository}/releases/download/${version}/patches-${version}.mpp`,
    version,
  };
}

function verifyRelease(_config, { env, options, branch, nextRelease }) {
  checkVersion(nextRelease.version);
  assert.equal(nextRelease.gitTag, nextRelease.version, "Release tags must not have a v prefix");
  assert.ok(["main", "dev"].includes(branch.name), "Releases are only allowed from main or dev");
  assert.equal(nextRelease.version.includes("-dev."), branch.name === "dev",
    "Release channel does not match branch");
  if (options.dryRun) return;
  assert.equal(env.GITHUB_ACTIONS, "true", "Publish from the manual GitHub Actions workflow");
  assert.equal(env.GITHUB_EVENT_NAME, "workflow_dispatch", "Automatic publication is disabled");
  assert.equal(env.RELEASE_MODE, "publish", "Publishing requires explicit publish mode");
  assert.equal(env.EXPECTED_VERSION, nextRelease.version,
    "Expected version does not match semantic-release; run preview first");
}

function prepare(config, context) {
  verifyRelease(config, context);
  const { cwd, env, nextRelease, logger } = context;
  const version = nextRelease.version;
  updateVersion(cwd, version);
  const result = spawnSync(process.platform === "win32" ? "gradlew.bat" : "./gradlew", [
    ":patches:buildAndroid", ":patches:test", ":extensions:shared:test",
    ":patches:generatePatchesList", "--no-daemon",
  ], { cwd, env, stdio: "inherit", shell: process.platform === "win32" });
  if (result.error) throw result.error;
  assert.equal(result.status, 0, "Release build or compatibility tests failed");

  const bundle = path.join(cwd, "patches/build/libs", `patches-${version}.mpp`);
  const validation = validateBundle(bundle, version);
  validation.patches = validatePatchList(cwd, version);
  const directory = path.join(cwd, "build/release");
  fs.mkdirSync(directory, { recursive: true });
  fs.copyFileSync(bundle, path.join(directory, "patches.mpp"));
  fs.writeFileSync(path.join(directory, "validation.json"), `${JSON.stringify(validation, null, 2)}\n`);
  fs.writeFileSync(path.join(cwd, "patches-bundle.json"),
    `${JSON.stringify(makeMetadata(version, nextRelease.notes), null, 4)}\n`);
  logger.log("Validated %s: %s (%d patches)", version, validation.sha256, validation.patches);
}

module.exports = {
  verifyRelease, prepare, readVersion, updateVersion, validateBundle, validatePatchList, makeMetadata,
};
