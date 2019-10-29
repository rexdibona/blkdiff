/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import blkdiff.BlkInfo;
import blkdiff.SrcHash;

public class DataChunk {
    private BlkInfo.BlockSource file;
    private long offset;
    private long length;
    private int flagB;
    private byte[] offsetB;
    private byte[] lengthB;
    private static /* synthetic */ int[] $SWITCH_TABLE$blkdiff$BlkInfo$BlockSource;

    public DataChunk(BlkInfo.BlockSource file, long offset, long length) {
        this.file = file;
        this.offset = offset;
        this.length = length;
        this.offsetB = DataChunk.longToByte(offset);
        this.lengthB = DataChunk.longToByte(length);
        switch (DataChunk.$SWITCH_TABLE$blkdiff$BlkInfo$BlockSource()[file.ordinal()]) {
            case 4: {
                this.flagB = 0;
                break;
            }
            case 2: {
                this.flagB = 64;
                break;
            }
            case 3: {
                this.flagB = 128;
                break;
            }
            default: {
                this.flagB = 192;
            }
        }
        this.flagB = this.flagB | this.offsetB.length - 1 & 7 | this.lengthB.length - 1 << 3 & 0x38;
    }

    public DataChunk(byte fileB, byte[] data) {
        int i;
        this.flagB = fileB;
        this.offsetB = new byte[(fileB & 7) + 1];
        this.lengthB = new byte[(fileB >> 3 & 7) + 1];
        long offset = 0L;
        long length = 0L;
        int dsrc = 0;
        for (i = 0; i < (fileB & 7) + 1; ++i) {
            this.offsetB[i] = data[dsrc];
            offset |= (long)((data[dsrc] & 0xFF) << i * 8);
            ++dsrc;
        }
        for (i = 0; i < (fileB >> 3 & 7) + 1; ++i) {
            this.lengthB[i] = data[dsrc];
            length |= (long)((data[dsrc] & 0xFF) << i * 8);
            ++dsrc;
        }
        this.offset = offset;
        this.length = length;
        switch (fileB & 0xC0) {
            case 0: {
                this.file = BlkInfo.BlockSource.ZERO;
                break;
            }
            case 64: {
                this.file = BlkInfo.BlockSource.INPUT;
                break;
            }
            case 128: {
                this.file = BlkInfo.BlockSource.OUTPUT;
                break;
            }
            default: {
                this.file = BlkInfo.BlockSource.NONE;
            }
        }
    }

    public static byte[] longToByte(long field) {
        byte[] temp = new byte[8];
        if (field < 0L) {
            field = 0L;
        }
        int len = 0;
        do {
            temp[len] = (byte)(field & 0xFFL);
            ++len;
        } while ((field >>= 8) > 0L);
        byte[] res = new byte[len];
        for (int i = 0; i < len; ++i) {
            res[i] = temp[i];
        }
        return res;
    }

    public static int flagToLength(int flag) {
        int len = (flag & 7) + 1;
        len += (flag >> 3 & 7) + 1;
        return ++len;
    }

    public int length() {
        return DataChunk.flagToLength(this.flagB);
    }

    public long getOffset() {
        return this.offset;
    }

    public long getLength() {
        return this.length;
    }

    public BlkInfo.BlockSource getFile() {
        return this.file;
    }

    public byte[] byteStr() {
        int i;
        int d = 0;
        byte[] str = new byte[this.length()];
        str[d++] = (byte)this.flagB;
        for (i = 0; i < (this.flagB & 7) + 1; ++i) {
            str[d++] = this.offsetB[i];
        }
        for (i = 0; i < (this.flagB >> 3 & 7) + 1; ++i) {
            str[d++] = this.lengthB[i];
        }
        return str;
    }

    public String toString() {
        return SrcHash.hexString(this.byteStr());
    }

    static /* synthetic */ int[] $SWITCH_TABLE$blkdiff$BlkInfo$BlockSource() {
        if ($SWITCH_TABLE$blkdiff$BlkInfo$BlockSource != null) {
            int[] arrn;
            return arrn;
        }
        int[] arrn = new int[BlkInfo.BlockSource.values().length];
        try {
            arrn[BlkInfo.BlockSource.INPUT.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {}
        try {
            arrn[BlkInfo.BlockSource.NONE.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {}
        try {
            arrn[BlkInfo.BlockSource.OUTPUT.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {}
        try {
            arrn[BlkInfo.BlockSource.ZERO.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {}
        $SWITCH_TABLE$blkdiff$BlkInfo$BlockSource = arrn;
        return $SWITCH_TABLE$blkdiff$BlkInfo$BlockSource;
    }
}
