package com.descartes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class MapsLinkResolverController {

    private static final Set<String> SHORT_LINK_HOSTS = Set.of("maps.app.goo.gl", "goo.gl", "g.co");
    private static final Set<String> GOOGLE_HOSTS = Set.of("maps.app.goo.gl", "goo.gl", "g.co", "google.com", "www.google.com", "maps.google.com");
    private static final Pattern COORDINATES = Pattern.compile(
            "!3d(-?\\d{1,3}(?:\\.\\d+)?)!4d(-?\\d{1,3}(?:\\.\\d+)?)"
                    + "|@(-?\\d{1,3}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)"
                    + "|(?:[?&]|^)(?:q|ll|query|center|destination|daddr)=(-?\\d{1,3}(?:\\.\\d+)?),\\s*(-?\\d{1,3}(?:\\.\\d+)?)");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @GetMapping("/api/resolve-link")
    public ResolvedMapsLink resolve(@RequestParam("url") String value) {
        URI current = parseShortLink(value);
        String page = "";

        for (int redirects = 0; redirects <= 5; redirects++) {
            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept", "text/html,application/xhtml+xml")
                    .GET()
                    .build();

            HttpResponse<InputStream> response;
            try {
                response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Interrupted while resolving Google Maps link", e);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not resolve Google Maps link", e);
            }

            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                try (InputStream body = response.body()) {
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps redirect did not include a destination"));
                    current = validateGoogleUri(current.resolve(location));
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read Google Maps redirect", e);
                }
                continue;
            }

            if (status < 200 || status >= 300) {
                try (InputStream body = response.body()) {
                    body.readNBytes(1_048_576);
                } catch (IOException e) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read Google Maps response", e);
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps returned HTTP " + status);
            }

            try (InputStream body = response.body()) {
                page = new String(body.readNBytes(1_048_576), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not read Google Maps response", e);
            }

            ResolvedMapsLink result = extract(current, page);
            if (result != null) return result;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Google Maps link did not contain coordinates");
        }

        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps link redirected too many times");
    }

    private static URI parseShortLink(String value) {
        try {
            String input = value.trim();
            if (!input.matches("(?i)^https?://.*")) input = "https://" + input;
            URI uri = URI.create(input);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !SHORT_LINK_HOSTS.contains(host(uri))
                    || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Expected a Google Maps short link");
        }
    }

    private static URI validateGoogleUri(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !GOOGLE_HOSTS.contains(host(uri))
                || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google Maps redirected to an unsupported destination");
        }
        return uri;
    }

    private static String host(URI uri) {
        return uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
    }

    private static ResolvedMapsLink extract(URI uri, String page) {
        String searchable = URLDecoder.decode(uri.toString(), StandardCharsets.UTF_8)
                + "\n" + URLDecoder.decode(page, StandardCharsets.UTF_8);
        Matcher coordinates = COORDINATES.matcher(searchable);
        while (coordinates.find()) {
            int latGroup = coordinates.group(1) != null ? 1 : coordinates.group(3) != null ? 3 : 5;
            int lonGroup = latGroup + 1;
            double lat = Double.parseDouble(coordinates.group(latGroup));
            double lon = Double.parseDouble(coordinates.group(lonGroup));
            if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                return new ResolvedMapsLink(lat, lon, placeName(uri), null, null, null);
            }
        }
        return null;
    }

    private static String placeName(URI uri) {
        String[] parts = uri.getPath().split("/");
        for (int i = 0; i + 1 < parts.length; i++) {
            if ("place".equals(parts[i]) || "search".equals(parts[i])) {
                String name = URLDecoder.decode(parts[i + 1], StandardCharsets.UTF_8)
                        .replace('+', ' ')
                        .trim();
                return name.isEmpty() ? "" : name;
            }
        }
        return "";
    }

    public record ResolvedMapsLink(double lat, double lon, String name, String country, String region, String website) {}
}
