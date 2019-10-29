/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import blkdiff.DataChunk;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DiffReader {
    private int hashlen;
    private int hashStrLen;
    private int blkShift;
    private int blkSize;
    private FileChannel diffFile;
    private byte[] inputHashValue;
    private byte[] outputHashValue;
    private String hash;
    private long blockDataLength = 0L;
    private ArrayList<DataChunk> chunks = new ArrayList();

    public DiffReader(FileChannel diffFile) {
        this.diffFile = diffFile;
    }

    public boolean verifyHeader() throws IOException {
        int len;
        byte[] header = new byte[17];
        ByteBuffer input = ByteBuffer.wrap(header);
        this.diffFile.position(0L);
        while ((len = this.diffFile.read(input)) != -1 && input.hasRemaining()) {
        }
        if (len == -1) {
            System.err.printf("Invalid header: Short Read of 17 bytes\n", new Object[0]);
            return false;
        }
        if (header[0] != 4) {
            System.err.printf("Invalid header value [0]: %d\n", header[0]);
            return false;
        }
        if (header[9] != 0) {
            System.err.printf("Invalid header value [9]: %d\n", header[9]);
            return false;
        }
        if (header[14] != 0) {
            System.err.printf("Invalid header value [14]: %d\n", header[14]);
            return false;
        }
        if (!DiffReader.byteToString(header, 1, 8).equals("COMSDIFF")) {
            System.err.printf("Invalid header value [COMSDIFF]: %s\n", DiffReader.byteToString(header, 1, 8));
            return false;
        }
        if (!DiffReader.byteToString(header, 10, 4).equals("V1.1")) {
            System.err.printf("Invalid header value [V1.1]: %s\n", DiffReader.byteToString(header, 10, 4));
            return false;
        }
        this.blkShift = header[15] & 0xFF;
        if (this.blkShift < 6 || this.blkShift > 15) {
            System.err.printf("Invalid Block Shift Value: %d\n", this.blkShift);
            return false;
        }
        this.blkSize = 1 << this.blkShift;
        this.hashlen = header[16] & 0xFF;
        this.inputHashValue = new byte[this.hashlen];
        this.outputHashValue = new byte[this.hashlen];
        ByteBuffer ihv = ByteBuffer.wrap(this.inputHashValue);
        while ((len = this.diffFile.read(ihv)) != -1 && ihv.hasRemaining()) {
        }
        if (len == -1) {
            System.err.printf("Invalid header: Short Read of input hash value: %d bytes\n", this.hashlen);
            return false;
        }
        ByteBuffer ohv = ByteBuffer.wrap(this.outputHashValue);
        while ((len = this.diffFile.read(ohv)) != -1 && ohv.hasRemaining()) {
        }
        if (len == -1) {
            System.err.printf("Invalid header: Short Read of output hash value: %d bytes\n", this.hashlen);
            return false;
        }
        input = ByteBuffer.wrap(header, 0, 1);
        len = this.diffFile.read(input);
        if (len != 1) {
            System.err.printf("Invalid header: Short Read of hash name length: %d bytes\n", len);
            return false;
        }
        this.hashStrLen = header[0];
        byte[] hashbyte = new byte[this.hashStrLen];
        input = ByteBuffer.wrap(hashbyte);
        while ((len = this.diffFile.read(input)) != -1 && input.hasRemaining()) {
        }
        if (len == -1) {
            System.err.printf("Invalid header: Short Read of output hash value: %d bytes\n", this.hashStrLen);
            return false;
        }
        this.hash = DiffReader.byteToString(hashbyte, 0, this.hashStrLen);
        input = ByteBuffer.wrap(header, 0, 1);
        len = this.diffFile.read(input);
        if (len != 1) {
            System.err.printf("Invalid header: Short Read of hash name length: %d bytes\n", len);
            return false;
        }
        if (header[0] != 0) {
            System.err.printf("Invalid header value HASH TERMINATOR: %d\n", header[0]);
            return false;
        }
        input = ByteBuffer.wrap(header, 0, 8);
        while ((len = this.diffFile.read(input)) != -1 && input.hasRemaining()) {
        }
        if (len == -1) {
            System.err.printf("Invalid header: Short Read of 8 bytes: %d remaining\n", input.hasRemaining());
            return false;
        }
        for (int i = 0; i < 8; ++i) {
            this.blockDataLength |= (long)((header[i] & 0xFF) << 8 * i);
        }
        int count = 0;
        while ((long)count < this.blockDataLength) {
            this.chunks.add(this.readBlockData());
            ++count;
        }
        return true;
    }

    public DataChunk getBlockData(int index) {
        return this.chunks.get(index);
    }

    public DataChunk readBlockData() {
        int len;
        byte[] data;
        byte[] header;
        block7 : {
            block6 : {
                header = new byte[1];
                data = null;
                ByteBuffer input = ByteBuffer.wrap(header);
                len = 0;
                len = this.diffFile.read(input);
                if (len == 1) break block6;
                System.err.printf("Invalid header: Short Read of hash name length: %d bytes\n", len);
                return null;
            }
            len = DataChunk.flagToLength(header[0]) - 1;
            if (len >= 2) break block7;
            System.err.printf("Invalid DataChunk: Short length: %d [%x]\n", len, header[0]);
            return null;
        }
        try {
            data = new byte[len];
            ByteBuffer dataBB = ByteBuffer.wrap(data);
            while ((len = this.diffFile.read(dataBB)) != -1 && dataBB.hasRemaining()) {
            }
            if (len == -1) {
                System.err.printf("Invalid DataChunk: Short Read: %d remaining\n", dataBB.hasRemaining());
                return null;
            }
        }
        catch (IOException e) {
            System.err.printf("Invalid header: Short Read of hash name length: %d bytes\n", len);
            e.printStackTrace();
            return null;
        }
        return new DataChunk(header[0], data);
    }

    public static String byteToString(byte[] arr) {
        String res = "";
        int len = arr.length;
        for (int i = 0; i < len; ++i) {
            res = String.valueOf(res) + Character.toString((char)arr[i]);
        }
        return res;
    }

    public static String byteToString(byte[] arr, int start, int len) {
        String res = "";
        for (int i = 0; i < len; ++i) {
            res = String.valueOf(res) + Character.toString((char)arr[i + start]);
        }
        return res;
    }

    public byte[] getInputHash() {
        return this.inputHashValue;
    }

    public byte[] getOutputHash() {
        return this.outputHashValue;
    }

    public String getHash() {
        return this.hash;
    }

    public int getBlkSize() {
        return this.blkSize;
    }

    public long getBlockDataLength() {
        return this.blockDataLength;
    }
}
