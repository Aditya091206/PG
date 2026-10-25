"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.slugifyPgName = slugifyPgName;
exports.generateHmacSuffix = generateHmacSuffix;
exports.formatPgCode = formatPgCode;
const crypto = __importStar(require("crypto"));
const SAFE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"; // 32 characters, no 0, 1, I, O
/**
 * Creates a clean uppercase slug from a PG name.
 * Example: "Sunrise Executive PG" -> "SUNRISE"
 */
function slugifyPgName(name) {
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
function generateHmacSuffix(phone, secretKey, nonce = 0) {
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
function formatPgCode(slug, suffix) {
    return `${slug}-${suffix}`;
}
//# sourceMappingURL=codeGenerator.js.map