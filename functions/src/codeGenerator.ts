import * as crypto from "crypto";

const SAFE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // 32 characters, no 0, 1, I, O

/**
 * Creates a clean uppercase slug from a PG name.
 * Example: "Sunrise Executive PG" -> "SUNRISE"
 */
export function slugifyPgName(name: string): string {
  const clean = name
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
  return clean.substring(0, Math.min(8, clean.length)) || "PG";
}

/**
 * Computes a deterministic 5-character HMAC suffix for a phone number.
 * Uses a secret salt and optional regeneration nonce to guarantee phone privacy.
 */
export function generateHmacSuffix(
  phone: string,
  secretKey: string,
  nonce: number = 0
): string {
  const hmac = crypto.createHmac("sha256", secretKey);
  hmac.update(`${phone.trim()}:${nonce}`);
  const hash = hmac.digest();

  let suffix = "";
  for (let i = 0; i < 5; i++) {
    const byte = hash[i];
    suffix += SAFE_ALPHABET.charAt(byte % SAFE_ALPHABET.length);
  }
  return suffix;
}

/**
 * Combines slug and HMAC suffix into a human-readable shareable code.
 * Example: "SUNRISE-4F9K2"
 */
export function formatPgCode(slug: string, suffix: string): string {
  return `${slug}-${suffix}`;
}
