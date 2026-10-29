const assert = require("node:assert/strict");
const { execFileSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { test } = require("node:test");
const AdmZip = require("adm-zip");
const YAML = require("yaml");
const tooling = require("./release-plugin.cjs");

const root = path.resolve(__dirname, "..");
const config = JSON.parse(fs.readFileSync(path.join(root, ".releaserc"), "utf8"));
const repository = "https://github.com/DiogoGra/revanced-patches-legacy";

function temporary(t) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), "rvx-release-"));
  t.after(() => fs.rmSync(directory, { recursive: true, force: true }));
  return directory;
}

function fixture(t, { version = "1.3.3", dex = true, source = repository, jvm = true } = {}) {
  const file = path.join(temporary(t), "patches.mpp");
  const zip = new AdmZip();
  zip.addFile("META-INF/MANIFEST.MF", Buffer.from(
    `Manifest-Version: 1.0\r\nVersion: ${version}\r\nSource: ${source}\r\n\r\n`));
  if (dex) zip.addFile("classes.dex", Buffer.concat([Buffer.from("dex\n035\0"), Buffer.alloc(104)]));
  if (jvm) zip.addFile("app/morphe/patches/Test.class", Buffer.from("fixture"));
  zip.writeZip(file);
  return file;
}

function context(overrides = {}) {
  return {
    env: {
      GITHUB_ACTIONS: "true", GITHUB_EVENT_NAME: "workflow_dispatch",
      RELEASE_MODE: "publish", EXPECTED_VERSION: "1.3.3",
    },
    options: { dryRun: false },
    branch: { name: "main" },
    nextRelease: { version: "1.3.3", gitTag: "1.3.3" },
    ...overrides,
  };
}

test("workflow is manual-only, defaults to preview and serializes release runs", () => {
  const workflow = YAML.parse(fs.readFileSync(path.join(root, ".github/workflows/release.yml"), "utf8"));
  assert.deepEqual(Object.keys(workflow.on), ["workflow_dispatch"]);
  const inputs = workflow.on.workflow_dispatch.inputs;
  assert.equal(inputs.mode.default, "preview");
  assert.deepEqual(inputs.mode.options, ["preview", "publish"]);
  assert.ok(inputs.expected_version);
  assert.deepEqual(workflow.concurrency, { group: "release", "cancel-in-progress": false });
  assert.match(workflow.jobs.release.if, /DiogoGra\/revanced-patches-legacy/);
  assert.equal(workflow.jobs.release.permissions.contents, "write");
  assert.equal(workflow.jobs.release.permissions.packages, "read");
  const steps = workflow.jobs.release.steps;
  assert.equal(steps.find((step) => step.name === "Checkout").with["fetch-depth"], 0);
  assert.ok(steps.some((step) => step.run?.includes(":patches:buildAndroid")));
  assert.ok(steps.some((step) => step.run === "npm run release"));
  assert.ok(!steps.some((step) => step.uses?.includes("import-gpg")));
});

test("release configuration preserves existing unprefixed tags and both channels", () => {
  assert.equal(config.tagFormat, "${version}");
  assert.equal(config.repositoryUrl, `${repository}.git`);
  assert.deepEqual(config.branches, ["main", { name: "dev", prerelease: true }]);
  const plugins = config.plugins.map((entry) => Array.isArray(entry) ? entry[0] : entry);
  assert.ok(plugins.indexOf("./scripts/release-plugin.cjs") < plugins.indexOf("@semantic-release/git"));
  assert.ok(plugins.indexOf("@semantic-release/git") < plugins.indexOf("@semantic-release/github"));
  const github = config.plugins.find((entry) => entry[0] === "@semantic-release/github")[1];
  assert.deepEqual(github.assets, [{
    path: "build/release/patches.mpp", name: "patches-<%= nextRelease.version %>.mpp",
  }]);
  const changelog = config.plugins.find((entry) => entry[0] === "@MorpheApp/changelog")[1];
  assert.equal(changelog.releaseJson.enabled, false);
  const backmerge = config.plugins.find((entry) => entry[0] === "@cleyrop-org/semantic-release-backmerge")[1];
  assert.equal(backmerge.backmergeStrategy, "merge");
  assert.equal(backmerge.forcePush, false);
});

test("inherited upstream tags are ignored without deleting or changing any tags", async (t) => {
  const directory = temporary(t);
  const git = (...args) => execFileSync("git", args, { cwd: directory, encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] }).trim();
  git("init", "--initial-branch=main");
  git("-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "--allow-empty", "-m", "Upstream fixture");
  git("tag", "v4.0.0");
  git("-c", "user.name=Test", "-c", "user.email=test@example.invalid", "commit", "--allow-empty", "-m", "Legacy fixture");
  git("tag", "1.3.2");
  git("tag", "1.3.3-dev.1");
  const before = git("show-ref", "--tags");
  const { default: getTags } = await import("semantic-release/lib/branches/get-tags.js");
  const [branch] = await getTags({ cwd: directory, env: process.env, options: config }, [{ name: "main" }]);
  assert.deepEqual(branch.tags.map((tag) => tag.gitTag).sort(), ["1.3.2", "1.3.3-dev.1"]);
  assert.equal(git("show-ref", "--tags"), before);
});

test("publishing requires manual dispatch, publish mode and the exact preview version", () => {
  assert.doesNotThrow(() => tooling.verifyRelease({}, context()));
  for (const [key, value] of Object.entries({
    GITHUB_ACTIONS: "false", GITHUB_EVENT_NAME: "push", RELEASE_MODE: "preview", EXPECTED_VERSION: "1.3.4",
  })) {
    const current = context();
    current.env[key] = value;
    assert.throws(() => tooling.verifyRelease({}, current));
  }
  assert.doesNotThrow(() => tooling.verifyRelease({}, context({ options: { dryRun: true }, env: {} })));
});

test("stable and prerelease versions cannot be published on the wrong branch", () => {
  assert.throws(() => tooling.verifyRelease({}, context({ branch: { name: "dev" } })));
  const current = context({
    branch: { name: "dev" }, nextRelease: { version: "1.3.3-dev.1", gitTag: "1.3.3-dev.1" },
  });
  current.env.EXPECTED_VERSION = "1.3.3-dev.1";
  assert.doesNotThrow(() => tooling.verifyRelease({}, current));
  assert.throws(() => tooling.verifyRelease({}, context({ nextRelease: { version: "1.3.3", gitTag: "v1.3.3" } })));
});

test("version update changes only the existing Gradle version property", (t) => {
  const directory = temporary(t);
  const file = path.join(directory, "gradle.properties");
  fs.writeFileSync(file, "org.gradle.caching = true\r\nversion = 1.3.2\r\n");
  tooling.updateVersion(directory, "1.3.3-dev.1");
  assert.equal(tooling.readVersion(directory), "1.3.3-dev.1");
  assert.equal(fs.readFileSync(file, "utf8"), "org.gradle.caching = true\r\nversion = 1.3.3-dev.1\r\n");
  assert.throws(() => tooling.updateVersion(directory, "../../asset"));
  fs.appendFileSync(file, "version = 1.3.3\n");
  assert.throws(() => tooling.readVersion(directory));
});

test("Android bundle validation accepts the expected manifest, DEX and JVM classes", (t) => {
  const validation = tooling.validateBundle(fixture(t), "1.3.3");
  assert.equal(validation.version, "1.3.3");
  assert.match(validation.sha256, /^[a-f0-9]{64}$/);
  assert.ok(validation.bytes > 112);
});

test("bundle validation rejects JVM-only, stale, foreign and truncated bundles", (t) => {
  assert.throws(() => tooling.validateBundle(fixture(t, { dex: false }), "1.3.3"), /classes.dex/);
  assert.throws(() => tooling.validateBundle(fixture(t, { version: "1.3.2" }), "1.3.3"), /version/);
  assert.throws(() => tooling.validateBundle(fixture(t, { source: "https://github.com/anddea/revanced-patches" }), "1.3.3"), /source/);
  assert.throws(() => tooling.validateBundle(fixture(t, { jvm: false }), "1.3.3"), /JVM/);
  const file = fixture(t);
  fs.truncateSync(file, 30);
  assert.throws(() => tooling.validateBundle(file, "1.3.3"));
});

test("patch list must match the bundle and support YouTube 19.16.39", (t) => {
  const directory = temporary(t);
  const file = path.join(directory, "patches-list.json");
  const list = { version: "v1.3.3", patches: [{ compatiblePackages: { "com.google.android.youtube": ["19.16.39"] } }] };
  fs.writeFileSync(file, JSON.stringify(list));
  assert.equal(tooling.validatePatchList(directory, "1.3.3"), 1);
  assert.throws(() => tooling.validatePatchList(directory, "1.3.2"));
  list.patches = [];
  fs.writeFileSync(file, JSON.stringify(list));
  assert.throws(() => tooling.validatePatchList(directory, "1.3.3"));
});

test("Morphe metadata uses the exact asset URL and no nonexistent signature", () => {
  const notes = "### Bug Fixes\n\n* **YouTube:** Fix icon\n";
  for (const version of ["1.3.3", "1.3.3-dev.1"]) {
    const metadata = tooling.makeMetadata(version, notes, new Date("2026-10-25T22:00:00Z"));
    assert.equal(metadata.version, version);
    assert.equal(metadata.download_url, `${repository}/releases/download/${version}/patches-${version}.mpp`);
    assert.equal(metadata.signature_download_url, null);
    assert.equal(metadata.created_at, "2026-10-25T22:00:00");
    assert.equal(metadata.description, notes.trim());
  }
  assert.throws(() => tooling.makeMetadata("1.3.3", ""));
});

test("Anddea's default notes generator includes heading, scope, issue and commit links", async () => {
  const { generateNotes } = await import("@semantic-release/release-notes-generator");
  const { analyzeCommits } = await import("@semantic-release/commit-analyzer");
  const current = {
    cwd: root,
    options: config,
    commits: [{ hash: "1234567890abcdef1234567890abcdef12345678", message: "fix(YouTube - Add missing resources): Restore Create button icon (#5)" }],
    lastRelease: { version: "1.3.2", gitTag: "1.3.2" },
    nextRelease: { version: "1.3.3", gitTag: "1.3.3" },
    logger: { log() {} },
  };
  assert.equal(await analyzeCommits({}, current), "patch");
  const notes = await generateNotes({}, current);
  assert.match(notes, /^#+ \[1\.3\.3\]\(https:\/\/github.com\/DiogoGra\/revanced-patches-legacy\/compare\/1\.3\.2\.\.\.1\.3\.3\) \(\d{4}-\d{2}-\d{2}\)/);
  assert.ok(notes.includes("### Bug Fixes"));
  assert.ok(notes.includes("**YouTube - Add missing resources:** Restore Create button icon"));
  assert.ok(notes.includes(`${repository}/issues/5`));
  assert.ok(notes.includes(`${repository}/commit/1234567890abcdef1234567890abcdef12345678`));
});
