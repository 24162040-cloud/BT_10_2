package vn.iotstar.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private static final String ISSUER = "jwt-nimbus";
    private final byte[] secret;
    private final long expirationTime;

    public JwtService(@Value("${security.jwt.secret-key}") String encodedSecret,
                      @Value("${security.jwt.expiration-time}") long expirationTime) {
        secret = encodedSecret.isBlank() ? new byte[32] : Base64.getDecoder().decode(encodedSecret);
        if (encodedSecret.isBlank()) new SecureRandom().nextBytes(secret);
        if (secret.length < 32 || expirationTime <= 0) {
            throw new IllegalArgumentException("JWT key must have at least 32 bytes and expiry must be positive");
        }
        this.expirationTime = expirationTime;
    }

    public String generateToken(UserDetails user) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getUsername()).issuer(ISSUER).audience(ISSUER)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusMillis(expirationTime))).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256)
                .type(JOSEObjectType.JWT).build(), claims);
        try {
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Could not sign JWT", ex);
        }
    }

    // Verify once, before trusting the subject or any other claim.
    public String extractUsername(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secret))) {
                throw new BadCredentialsException("JWT signature is invalid");
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date now = new Date();
            if (!ISSUER.equals(claims.getIssuer()) || !List.of(ISSUER).equals(claims.getAudience())
                    || claims.getSubject() == null || claims.getSubject().isBlank()
                    || claims.getExpirationTime() == null || !claims.getExpirationTime().after(now)
                    || claims.getIssueTime() == null || claims.getIssueTime().after(now)
                    || (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().after(now))) {
                throw new BadCredentialsException("JWT claims are invalid or expired");
            }
            return claims.getSubject();
        } catch (ParseException | JOSEException | IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT is invalid", ex);
        }
    }

    public long getExpirationTime() { return expirationTime; }
}
