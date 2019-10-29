/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHash {
    MessageDigest md;

    public FileHash(String digestFunction) throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance(digestFunction);
    }

    public void update(byte[] data, int offset, int len) {
        this.md.update(data, offset, len);
    }

    public void update(byte[] data) {
        this.md.update(data);
    }

    public byte[] digest() {
        return this.md.digest();
    }
}
