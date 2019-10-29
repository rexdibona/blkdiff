/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import blkdiff.BlkInfo;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SrcHash {
    int blkSize;
    int debug;
    boolean verbose;
    boolean paranoid;
    private MessageDigest md;
    private HashMap<Digest, ArrayList<BlkInfo>> map;
    private FileChannel srcFd;
    private FileChannel dstFd;
    private byte[] zero;

    public SrcHash(String digestFunction, int blkSize, FileChannel srcFd, FileChannel dstFd, int debug, boolean verbose, boolean paranoid) throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance(digestFunction);
        this.blkSize = blkSize;
        this.srcFd = srcFd;
        this.dstFd = dstFd;
        this.debug = debug;
        this.verbose = verbose;
        this.paranoid = paranoid;
        this.map = new HashMap();
        this.zero = new byte[blkSize];
        Arrays.fill(this.zero, (byte)0);
        byte[] b = this.md.digest(this.zero);
        Digest digest = new Digest(b);
        ArrayList<BlkInfo> blkList = new ArrayList<BlkInfo>();
        blkList.add(new BlkInfo(BlkInfo.BlockSource.ZERO, 0L));
        this.map.put(digest, blkList);
        if (verbose || debug > 1) {
            System.err.printf("Adding Hash for block %06d at %s file: %s\n", new Object[]{0, BlkInfo.BlockSource.ZERO, SrcHash.hexString(b)});
        }
    }

    public void buildBlock(BlkInfo.BlockSource file, long offset, byte[] data, int doff, int length) throws IOException {
        boolean duplicate = true;
        String equals = "";
        this.md.update(data, doff, length);
        byte[] b = this.md.digest();
        Digest digest = new Digest(b);
        BlkInfo blk = new BlkInfo(file, offset);
        ArrayList<BlkInfo> blkList = this.map.get(digest);
        if (blkList == null) {
            blkList = new ArrayList();
            this.map.put(digest, blkList);
            duplicate = false;
        } else if (this.paranoid || this.debug > 2) {
            if (blkList.get((int)0).source == BlkInfo.BlockSource.ZERO) {
                equals = " ZERO";
            } else {
                int len;
                long current = this.srcFd.position();
                byte[] b1 = new byte[length];
                ByteBuffer input = ByteBuffer.wrap(b1);
                input.rewind();
                this.srcFd.position(blkList.get((int)0).offset * (long)this.blkSize);
                while ((len = this.srcFd.read(input)) != -1 && input.hasRemaining()) {
                }
                equals = Arrays.equals(data, b1) ? " ==" : " !=";
                this.srcFd.position(current);
            }
        } else {
            equals = " NP";
        }
        if (this.verbose || this.debug > 1) {
            System.err.printf("Adding Hash for block %06d at %s file: %s%s%s\n", new Object[]{offset, file, SrcHash.hexString(b), duplicate ? " *" : "", equals});
        }
        blkList.add(blk);
    }

    public void addBlock(byte[] data, long offset) {
        this.md.update(data);
        byte[] b = this.md.digest();
        Digest digest = new Digest(b);
        BlkInfo blk = new BlkInfo(BlkInfo.BlockSource.OUTPUT, offset);
        ArrayList<BlkInfo> blkList = this.map.get(digest);
        if (blkList != null) {
            System.err.printf("addBlock for offset %d already exists - error!", offset);
            System.exit(3);
        }
        blkList = new ArrayList();
        this.map.put(digest, blkList);
        blkList.add(blk);
        if (this.debug > 1) {
            System.err.printf("Searching for Hash: %s Added at destination offset %d\n", SrcHash.hexString(b), offset);
        }
    }

    public BlkInfo getBlock(byte[] data, BlkInfo expected) {
        byte[] b = this.md.digest(data);
        Digest digest = new Digest(b);
        ArrayList<BlkInfo> blkList = this.map.get(digest);
        if (blkList == null) {
            if (this.debug > 1) {
                System.err.printf("Searching for Hash: %s Failed\n", SrcHash.hexString(b));
            }
            return null;
        }
        if (expected.offset != -1L) {
            for (BlkInfo bl : blkList) {
                if (bl.source == BlkInfo.BlockSource.ZERO) {
                    if (this.debug > 1) {
                        System.err.printf("Searching for Hash: %s Found at ZERO file expected offset %d\n", SrcHash.hexString(b), expected.offset);
                    }
                    return new BlkInfo(BlkInfo.BlockSource.ZERO, expected.offset);
                }
                if (bl.source != expected.source || bl.offset != expected.offset) continue;
                if (this.debug > 1) {
                    System.err.printf("Searching for Hash: %s Found at expected\n", SrcHash.hexString(b));
                }
                return bl;
            }
        }
        BlkInfo res = blkList.get(0);
        if (this.debug > 1) {
            System.err.printf("Searching for Hash: %s Found at %s offset %d\n", new Object[]{SrcHash.hexString(b), res.source, res.offset});
        }
        return res;
    }

    public static String hexString(byte[] digest) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; ++i) {
            hexString.append(Integer.toHexString((digest[i] & 0xFF) + 256).substring(1));
        }
        return hexString.toString();
    }

    public byte[] getZero() {
        return this.zero;
    }

    private class Digest {
        private byte[] data;

        public Digest(byte[] data) {
            if (data == null) {
                throw new NullPointerException();
            }
            this.data = data;
        }

        public boolean equals(Object other) {
            return other instanceof Digest && Arrays.equals(this.data, ((Digest)other).data);
        }

        public int hashCode() {
            return Arrays.hashCode(this.data);
        }
    }

}
