package org.campus.classroom.campusclassroomreservationsystem;

import io.jsonwebtoken.impl.Base64UrlCodec;
import org.campus.classroom.utils.JWTUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class NormalTest {

    @Test
    public void test() throws NoSuchAlgorithmException, InvalidKeyException {
    }
}
