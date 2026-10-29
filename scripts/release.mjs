import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { createHash } from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import semanticRelease from "semantic-release";
import tooling from "./release-plugin.cjs";

const mode = process.env.RELEASE_MODE || "preview";
assert.ok(["preview", "publish"].includes(mode), "Unknown release mode");
if (mode === "publish") {
  assert.equal(process.env.GITHUB_ACTIONS, "true", "Publish from GitHub Actions");
  assert.equal(process.env.GITHUB_EVENT_NAME, "workflow_dispatch", "Publish must be started manually");
  assert.ok(process.env.EXPECTED_VERSION, "Run preview and enter the expected version before publishing");
}

const cwd = process.cwd();
const git = (...args) => execFileSync("git", args, { cwd, encoding: "utf8" }).trim();
const branch = git("branch", "--show-current");
assert.ok(["main", "dev"].includes(branch), "Run release from main or dev");
function verifyRemoteHead() {
  const remoteHead = git("ls-remote", "--heads", "origin", `refs/heads/${branch}`).split(/\s/)[0];
  assert.equal(git("rev-parse", "HEAD"), remoteHead,
    "Local branch differs from GitHub; synchronize it before preview or publish");
}
verifyRemoteHead();
const directory = path.join(cwd, "build/release");
fs.mkdirSync(directory, { recursive: true });
const currentVersion = tooling.readVersion(cwd);
const validation = tooling.validateBundle(
  path.join(cwd, "patches/build/libs", `patches-${currentVersion}.mpp`), currentVersion);
fs.writeFileSync(path.join(directory, "preview-validation.json"), `${JSON.stringify(validation, null, 2)}\n`);

const result = await semanticRelease({ dryRun: mode !== "publish" });
if (!result) verifyRemoteHead();
let summary = "No release is required by the commits on this branch.\n";
if (result) {
  const { version, notes } = result.nextRelease;
  fs.writeFileSync(path.join(directory, "release-notes.md"), notes);
  summary = `## ${mode === "publish" ? "Published" : "Preview"}: ${version}\n\n${notes}\n`;
  if (mode === "preview") {
    summary += `\nNo tags, commits, releases or assets were published. To publish, run this workflow again with mode \`publish\` and expected version \`${version}\`.\n`;
    summary += `\nThe Android build validated in preview has the current Gradle version (${currentVersion}); publication rebuilds and validates it with ${version}.\n`;
  } else {
    const expected = JSON.parse(fs.readFileSync(path.join(directory, "validation.json"), "utf8"));
    const metadata = tooling.makeMetadata(version, notes);
    // GitHub's download redirect may take a moment to become available.
    for (let attempt = 0; ; attempt++) {
      try {
        const response = await fetch(metadata.download_url, { signal: AbortSignal.timeout(120000) });
        assert.ok(response.ok, `Published asset download returned HTTP ${response.status}`);
        const data = Buffer.from(await response.arrayBuffer());
        assert.equal(createHash("sha256").update(data).digest("hex"), expected.sha256,
          "Published asset hash does not match validated bundle");
        summary += `\nPublished asset SHA-256: \`${expected.sha256}\`\n`;
        break;
      } catch (error) {
        if (attempt === 3) throw error;
        await new Promise((resolve) => setTimeout(resolve, 5000 * (attempt + 1)));
      }
    }
  }
}
fs.writeFileSync(path.join(directory, "summary.md"), summary);
if (process.env.GITHUB_STEP_SUMMARY) fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary);
console.log(summary);
