package com.ogm.market.live;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Service
public class JaasTokenService {

    @Value("${jaas.app-id}")
    private String appId;

    @Value("${jaas.key-id}")
    private String keyId;

    @Value("${jaas.private-key-base64}")
    private String privateKeyBase64;

    public String generateToken(String userName, String roomName, boolean isModerator) throws Exception {
        PrivateKey privateKey = loadPrivateKey(privateKeyBase64);

        Map<String, Object> userContext = new LinkedHashMap<>();
        userContext.put("id",        UUID.randomUUID().toString());
        userContext.put("name",      userName != null ? userName : "Guest");
        userContext.put("email",     "");
        userContext.put("avatar",    "");
        userContext.put("moderator", isModerator);

        Map<String, Object> features = new LinkedHashMap<>();
        features.put("recording",      isModerator);  // agent can record
        features.put("livestreaming",  false);
        features.put("transcription",  false);
        features.put("outbound-call",  false);
        // ✅ File sharing — enabled for both moderator and participant
        features.put("file-sharing",   true);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("user",     userContext);
        context.put("features", features);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", keyId);

        long nowMs = System.currentTimeMillis();

        return Jwts.builder()
                .setHeader(header)
                .setIssuer("chat")
                .setSubject(appId)
                .setAudience("jitsi")
                .claim("room", "*")
                .claim("context", context)
                .setIssuedAt(new Date(nowMs))
                .setExpiration(new Date(nowMs + 7_200_000L))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey(String base64Key) throws Exception {
        String cleaned = base64Key.replaceAll("[\\s\\n\\r]", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}