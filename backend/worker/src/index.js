const TOKEN_SKEW_MS = 30_000;
const ALLOWED_PREFIXES = [
  "/chapters",
  "/verses",
  "/recitations",
  "/resources",
  "/chapter_recitations",
  "/audio",
  "/quran",
  "/tafsirs",
  "/translations",
  "/search"
];

let cachedToken = null;
let tokenExpiresAt = 0;
let cachedEnvironment = "";
let cachedScope = "";
let tokenPromise = null;

function json(body, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
      "Cache-Control": "no-store",
      ...extraHeaders
    }
  });
}

function environmentUrls(environment) {
  if (environment === "production") {
    return {
      auth: "https://oauth2.quran.foundation",
      api: "https://apis.quran.foundation"
    };
  }
  return {
    auth: "https://prelive-oauth2.quran.foundation",
    api: "https://apis-prelive.quran.foundation"
  };
}

async function getAccessToken(env) {
  const clientId = env.QF_CLIENT_ID;
  const clientSecret = env.QF_CLIENT_SECRET;
  const environment = env.QF_ENV || "prelive";
  const scope = env.QF_SCOPE || "content.audio.read";

  if (!clientId || !clientSecret) {
    throw new Error("Quran Foundation credentials are not configured");
  }

  if (environment !== "prelive" && environment !== "production") {
    throw new Error("QF_ENV must be prelive or production");
  }

  if (
    cachedToken &&
    Date.now() < tokenExpiresAt &&
    cachedEnvironment === environment &&
    cachedScope === scope
  ) {
    return cachedToken;
  }

  if (tokenPromise) return tokenPromise;

  tokenPromise = (async () => {
    const urls = environmentUrls(environment);
    const basic = btoa(clientId + ":" + clientSecret);
    const response = await fetch(urls.auth + "/oauth2/token", {
      method: "POST",
      headers: {
        Authorization: "Basic " + basic,
        "Content-Type": "application/x-www-form-urlencoded"
      },
      body: new URLSearchParams({
        grant_type: "client_credentials",
        scope
      })
    });

    if (!response.ok) {
      throw new Error("Quran Foundation token request failed: " + response.status);
    }

    const payload = await response.json();
    if (!payload.access_token || !payload.expires_in) {
      throw new Error("Quran Foundation returned an invalid token response");
    }

    cachedToken = payload.access_token;
    tokenExpiresAt = Date.now() + Number(payload.expires_in) * 1000 - TOKEN_SKEW_MS;
    cachedEnvironment = environment;
    cachedScope = scope;
    return cachedToken;
  })();

  try {
    return await tokenPromise;
  } finally {
    tokenPromise = null;
  }
}

function clearToken() {
  cachedToken = null;
  tokenExpiresAt = 0;
  cachedEnvironment = "";
  cachedScope = "";
}

function withCors(response) {
  const headers = new Headers(response.headers);
  headers.set("Access-Control-Allow-Origin", "*");
  headers.set("Access-Control-Allow-Methods", "GET, OPTIONS");
  headers.set("Access-Control-Allow-Headers", "Content-Type");
  headers.append("Vary", "Origin");
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers
  });
}

async function callSunnah(env, upstreamPath, queryString) {
  if (!env.SUNNAH_API_KEY) {
    throw new Error("Sunnah.com API key is not configured");
  }
  const response = await fetch(
    "https://api.sunnah.com/v1" + upstreamPath + queryString,
    {
      method: "GET",
      headers: {
        Accept: "application/json",
        "X-API-Key": env.SUNNAH_API_KEY
      }
    }
  );
  return withCors(response);
}

async function callQuranFoundation(env, upstreamPath, queryString, retry = true) {
  const urls = environmentUrls(env.QF_ENV || "prelive");
  const token = await getAccessToken(env);
  const response = await fetch(urls.api + "/content/api/v4" + upstreamPath + queryString, {
    method: "GET",
    headers: {
      Accept: "application/json",
      "x-auth-token": token,
      "x-client-id": env.QF_CLIENT_ID
    }
  });

  if (response.status === 401 && retry) {
    clearToken();
    return callQuranFoundation(env, upstreamPath, queryString, false);
  }

  return withCors(response);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET, OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
          "Access-Control-Max-Age": "86400"
        }
      });
    }

    if (request.method !== "GET") {
      return json({ error: "method_not_allowed" }, 405, { Allow: "GET, OPTIONS" });
    }

    if (url.pathname === "/health") {
      return json({
        ok: true,
        service: "yaqeen-content-proxy",
        environment: env.QF_ENV || "prelive"
      });
    }

    const sunnahPrefix = "/api/sunnah";
    if (url.pathname.startsWith(sunnahPrefix + "/")) {
      const upstreamPath = url.pathname.slice(sunnahPrefix.length);
      const allowed = ["/collections", "/hadiths"].some(
        item => upstreamPath === item || upstreamPath.startsWith(item + "/")
      );
      if (!allowed || upstreamPath.includes("..")) {
        return json({ error: "route_not_allowed" }, 404);
      }
      try {
        return await callSunnah(env, upstreamPath, url.search);
      } catch (error) {
        return json({
          error: "sunnah_upstream_unavailable",
          message: error instanceof Error ? error.message : "Unknown upstream error"
        }, 502);
      }
    }

    const prefix = "/api/quran";
    if (!url.pathname.startsWith(prefix + "/")) {
      return json({ error: "not_found" }, 404);
    }

    const upstreamPath = url.pathname.slice(prefix.length);
    const allowed = ALLOWED_PREFIXES.some(
      item => upstreamPath === item || upstreamPath.startsWith(item + "/")
    );

    if (!allowed || upstreamPath.includes("..")) {
      return json({ error: "route_not_allowed" }, 404);
    }

    try {
      const response = await callQuranFoundation(env, upstreamPath, url.search);
      return response;
    } catch (error) {
      return json({
        error: "upstream_unavailable",
        message: error instanceof Error ? error.message : "Unknown upstream error"
      }, 502);
    }
  }
};
