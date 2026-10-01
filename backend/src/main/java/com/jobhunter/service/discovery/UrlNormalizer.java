package com.jobhunter.service.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class UrlNormalizer {

    private static final Logger log = LoggerFactory.getLogger(UrlNormalizer.class);

    private static final Set<String> TRACKING_PARAMS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "gh_src", "gh_jid", "lever-source", "lever-origin", "ref", "source",
            "fbclid", "gclid", "trk", "_hsenc", "_hsmi", "mc_cid", "mc_eid",
            "igshid", "si", "s_cid", "action", "mode"
    );

    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(127\\.|10\\.|192\\.168\\.|169\\.254\\.|0\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.).*"
    );

    public boolean isValidPublicUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return false;
            }

            String lowerHost = host.toLowerCase();

            // SSRF Blocklist: Localhost and internal domain names
            if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost") ||
                lowerHost.endsWith(".local") || lowerHost.endsWith(".internal") ||
                lowerHost.equals("::1")) {
                return false;
            }

            // SSRF Blocklist: Private and loopback IPv4 addresses
            if (PRIVATE_IP_PATTERN.matcher(lowerHost).matches()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "https";
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";

            if (host.isEmpty()) {
                return rawUrl.trim();
            }

            // Remove www. for consistency if appropriate, or keep standard host
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Parse and filter query parameters
            String query = uri.getQuery();
            StringBuilder cleanQuery = new StringBuilder();

            if (query != null && !query.isEmpty()) {
                String[] pairs = query.split("&");
                Map<String, String> keptParams = new TreeMap<>(); // Sorted for deterministic hash

                for (String pair : pairs) {
                    if (pair.isEmpty()) continue;
                    int idx = pair.indexOf("=");
                    String key = idx > 0 ? pair.substring(0, idx) : pair;
                    String value = idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : "";

                    String lowerKey = key.toLowerCase();
                    if (!TRACKING_PARAMS.contains(lowerKey) && !lowerKey.startsWith("utm_")) {
                        keptParams.put(key, value);
                    }
                }

                if (!keptParams.isEmpty()) {
                    for (Map.Entry<String, String> entry : keptParams.entrySet()) {
                        if (cleanQuery.length() > 0) {
                            cleanQuery.append("&");
                        }
                        cleanQuery.append(entry.getKey());
                        if (!entry.getValue().isEmpty()) {
                            cleanQuery.append("=").append(entry.getValue());
                        }
                    }
                }
            }

            StringBuilder normalized = new StringBuilder();
            normalized.append(scheme).append("://").append(host);

            int port = uri.getPort();
            if (port != -1 && port != 80 && port != 443) {
                normalized.append(":").append(port);
            }

            normalized.append(path);
            if (cleanQuery.length() > 0) {
                normalized.append("?").append(cleanQuery);
            }

            return normalized.toString();
        } catch (Exception e) {
            return rawUrl.trim();
        }
    }

    public static String computeSha256(String input) {
        if (input == null) {
            input = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
