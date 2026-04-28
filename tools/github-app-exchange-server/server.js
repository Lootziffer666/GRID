const express = require("express");
const jwt = require("jsonwebtoken");
const fs = require("fs");
const path = require("path");
require("dotenv").config();

const app = express();
app.use(express.json({ limit: "256kb" }));

const PORT = Number(process.env.PORT || 3000);
const GITHUB_API_BASE = "https://api.github.com";

function loadPrivateKeyFromEnv() {
  const inline = process.env.APP_PRIVATE_KEY;
  if (inline && inline.trim().length > 0) {
    return inline.replace(/\\n/g, "\n");
  }

  const pemPath = process.env.APP_PRIVATE_KEY_PATH;
  if (pemPath && pemPath.trim().length > 0) {
    const absolute = path.resolve(pemPath);
    return fs.readFileSync(absolute, "utf8");
  }

  throw new Error("Missing APP_PRIVATE_KEY or APP_PRIVATE_KEY_PATH");
}

function createGitHubAppJwt(appId, privateKeyPem) {
  const now = Math.floor(Date.now() / 1000);
  return jwt.sign(
    {
      iat: now - 60,
      exp: now + 10 * 60,
      iss: appId,
    },
    privateKeyPem,
    { algorithm: "RS256" },
  );
}

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.post("/github-app/exchange", async (req, res) => {
  try {
    const installationId = String(req.body?.installation_id || "").trim();
    if (!installationId) {
      return res.status(400).json({ error: "installation_id is required" });
    }

    const appId = String(process.env.APP_ID || "").trim();
    if (!appId) {
      return res.status(500).json({ error: "APP_ID is not configured" });
    }

    const privateKeyPem = loadPrivateKeyFromEnv();
    const appJwt = createGitHubAppJwt(appId, privateKeyPem);

    const response = await fetch(
      `${GITHUB_API_BASE}/app/installations/${encodeURIComponent(installationId)}/access_tokens`,
      {
        method: "POST",
        headers: {
          Accept: "application/vnd.github+json",
          Authorization: `Bearer ${appJwt}`,
          "X-GitHub-Api-Version": "2022-11-28",
          "User-Agent": "painkiller-github-app-exchange-server",
        },
      },
    );

    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      return res.status(response.status).json({
        error: payload?.message || "Could not mint installation token",
      });
    }

    return res.json({
      token: payload.token,
      expires_at: payload.expires_at,
    });
  } catch (error) {
    return res.status(500).json({
      error: "Could not mint installation token",
      detail: error instanceof Error ? error.message : "unknown_error",
    });
  }
});

app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`GitHub App exchange server listening on http://localhost:${PORT}`);
});
