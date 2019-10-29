/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

public class BlkInfo {
    public BlockSource source;
    public long offset;

    public BlkInfo(BlockSource source, long offset) {
        this.source = source;
        this.offset = offset;
    }

    public boolean equals(BlkInfo other) {
        return other.source == this.source && other.offset == this.offset;
    }

    public static enum BlockSource {
        NONE,
        INPUT,
        OUTPUT,
        ZERO;
        
    }

}
