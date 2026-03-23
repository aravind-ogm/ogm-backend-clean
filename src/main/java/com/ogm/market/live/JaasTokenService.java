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
    private String appId;          // vpaas-magic-cookie-abc123

    @Value("${jaas.key-id}")
    private String keyId;          // vpaas-magic-cookie-abc123/0d885c

    @Value("${jaas.private-key}")
    private String privateKeyPem;  // contents of the .pk file, base64 encoded

    public String generateToken(String userName, String roomName, boolean isModerator) throws Exception {
        PrivateKey privateKey = loadPrivateKey(privateKeyPem);

        Map<String, Object> userContext = new HashMap<>();
        userContext.put("name",      userName);
        userContext.put("moderator", isModerator);
        userContext.put("id",        UUID.randomUUID().toString());
        userContext.put("avatar",    "");
        userContext.put("email",     "");

        Map<String, Object> features = new HashMap<>();
        features.put("recording",      false);
        features.put("livestreaming",  false);
        features.put("transcription",  false);
        features.put("outbound-call",  false);

        Map<String, Object> context = new HashMap<>();
        context.put("user",     userContext);
        context.put("features", features);

        Map<String, Object> header = new HashMap<>();
        header.put("kid", keyId);
        header.put("typ", "JWT");

        return Jwts.builder()
                .setHeader(header)
                .setIssuer("chat")
                .setSubject(appId)
                .setAudience("jitsi")
                .claim("room",    roomName)
                .claim("context", context)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000)) // 1 hour
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}