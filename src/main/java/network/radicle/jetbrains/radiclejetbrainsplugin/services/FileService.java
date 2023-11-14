package network.radicle.jetbrains.radiclejetbrainsplugin.services;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

public class FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    public String getBase64(byte[] fileBytes) {
        var base64 = Base64.getEncoder().encodeToString(fileBytes);
        return getBase64Prefix(fileBytes) + base64;
    }

    public String calculateGitObjectId(String base64) {
        try {
            //the base64 here has a header in the following format "<HEADER>,<PAYLOAD>"
            var base64Parts = base64.split(",");
            var base64Payload = base64Parts.length > 1 ? base64Parts[1] : null;
            var base64PayloadBytes = base64Payload != null ? Base64.getDecoder().decode(base64Payload) : null;
            if (base64PayloadBytes == null) {
                logger.error("Empty base64 payload for {}", base64);
                return null;
            }

            // Create the header
            var header = "blob " + base64PayloadBytes.length + "\0";
            var headerBytes = header.getBytes(StandardCharsets.UTF_8);

            // Concatenate the header and the original file content
            var combined = Arrays.copyOf(headerBytes, headerBytes.length + base64PayloadBytes.length);
            System.arraycopy(base64PayloadBytes, 0, combined, headerBytes.length, base64PayloadBytes.length);

            // Create a MessageDigest with the SHA-1 algorithm
            var digest = MessageDigest.getInstance("SHA-1");

            // Update the digest with the data
            digest.update(combined);

            // Get the hash bytes
            var hashBytes = digest.digest();

            // Convert the hash bytes to a hexadecimal string
            var hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                var hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Exception caught for {}", base64, e);
            return null;
        }
    }

    private String getMimeType(byte[] fileBytes) {
        try (var is = new ByteArrayInputStream(fileBytes)) {
            return new Tika().detect(is);
        } catch (Exception e) {
            logger.error("Exception upon detecting mime-type", e);
            return null;
        }
    }

    private String getBase64Prefix(byte[] fileBytes) {
        var fileType = getMimeType(fileBytes);
        if (fileType != null) {
            return "data:" + fileType + ";base64,";
        } else {
            return "data:application/octet-stream;base64,";
        }
    }
}
