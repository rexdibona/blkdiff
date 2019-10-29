/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import blkdiff.BlkInfo;
import blkdiff.DataChunk;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class DiffWriter {
    private int blkShift;
    private int blkSize;
    private byte[] inputHashValue;
    private byte[] outputHashValue;
    private String hash;
    private FileChannel srcFile;
    private FileChannel dstFile;
    private int debug;
    private boolean verbose;
    private ArrayList<DataChunk> chunks;
    private long currentOutputOffset = 0L;
    private BlkInfo.BlockSource currentSource = BlkInfo.BlockSource.NONE;
    private long currentStart = 0L;
    private long currentLength = 0L;

    public DiffWriter(int blkShift, byte[] inputHashValue, byte[] outputHashValue, String hash, FileChannel srcFile, FileChannel dstFile, int debug, boolean verbose) {
        this.blkShift = blkShift;
        this.inputHashValue = inputHashValue;
        this.outputHashValue = outputHashValue;
        this.hash = hash;
        this.srcFile = srcFile;
        this.dstFile = dstFile;
        this.debug = debug;
        this.verbose = verbose;
        this.blkSize = 1 << blkShift;
        this.chunks = new ArrayList();
    }

    public void writeHeader() {
        try {
            System.out.write(4);
            this.writeString("COMSDIFF");
            System.out.write(0);
            this.writeString("V1.1");
            System.out.write(0);
            System.out.write(this.blkShift);
            System.out.write(this.inputHashValue.length);
            System.out.write(this.inputHashValue);
            System.out.write(this.outputHashValue);
            System.out.write(this.hash.length());
            this.writeString(this.hash);
            System.out.write(0);
            this.currentOutputOffset = 19 + this.inputHashValue.length + this.outputHashValue.length + this.hash.length();
        }
        catch (Exception e) {
            System.err.println("Failed to write the header information");
            e.printStackTrace();
            System.exit(2);
        }
    }

    private void writeString(String s) {
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            System.out.write((byte)c);
        }
    }

    public void writeBlockLocation(BlkInfo.BlockSource file, long offset, boolean continuation) {
        if (file == this.currentSource && this.currentStart + this.currentLength == offset && continuation) {
            ++this.currentLength;
            if (this.debug > 2) {
                System.err.printf("Increasing Chunk: %s, %d, %d\n", new Object[]{this.currentSource, this.currentStart, this.currentLength});
            }
        } else {
            if (this.currentSource != BlkInfo.BlockSource.NONE) {
                DataChunk chunk = new DataChunk(this.currentSource, this.currentStart, this.currentLength);
                if (this.debug > 2) {
                    System.err.printf("Flushing Chunk: %s, %d, %d => %s\n", new Object[]{this.currentSource, this.currentStart, this.currentLength, chunk.toString()});
                }
                this.chunks.add(chunk);
            }
            this.currentSource = file;
            this.currentStart = offset;
            this.currentLength = 1L;
            if (this.debug > 2) {
                System.err.printf("Creating Chunk: %s, %d, %d\n", new Object[]{this.currentSource, this.currentStart, this.currentLength});
            }
        }
    }

    public void flush() throws IOException {
        if (this.currentSource != BlkInfo.BlockSource.NONE) {
            DataChunk chunk = new DataChunk(this.currentSource, this.currentStart, this.currentLength);
            if (this.debug > 2) {
                System.err.printf("Flushing Chunk: %s, %d, %d => %s\n", new Object[]{this.currentSource, this.currentStart, this.currentLength, chunk.toString()});
            }
            this.chunks.add(chunk);
        }
        long len = this.chunks.size();
        if (this.debug > 2) {
            System.err.printf("Number of chunk Descriptors: %d (%08x)\n", len, len);
        }
        for (int i = 0; i < 8; ++i) {
            int v = (int)(len & 0xFFL);
            if (this.debug > 1) {
                System.err.printf("Length[%d] = %x\n", i, v);
            }
            System.out.write(v);
            len >>= 8;
        }
        this.currentOutputOffset += 8L;
        if (this.verbose) {
            System.err.printf("Descriptors start at offset %d (%x)\n", this.currentOutputOffset, this.currentOutputOffset);
        }
        int count = 0;
        for (DataChunk d : this.chunks) {
            try {
                System.out.write(d.byteStr());
                this.currentOutputOffset += (long)d.length();
            }
            catch (IOException e) {
                System.err.println("Failed to write the chunk information");
                e.printStackTrace();
                System.exit(2);
            }
            if (!this.verbose) continue;
            System.err.printf("Descriptor %d: %6s, Offset: %d, Length: %d\n", new Object[]{count, d.getFile(), d.getOffset(), d.getLength()});
            ++count;
        }
        if (this.verbose) {
            System.err.printf("Local Data Blocks start at offset %d (%x)\n", this.currentOutputOffset, this.currentOutputOffset);
        }
        long diffOffset = 0L;
        long dstOffset = 0L;
        for (DataChunk d : this.chunks) {
            if (d.getFile() == BlkInfo.BlockSource.OUTPUT) {
                if (diffOffset < d.getOffset()) {
                    System.err.printf("Destination chunk %d: Differences file chunk %d is listed as %d\n", dstOffset, diffOffset, d.getOffset());
                } else if (diffOffset == d.getOffset()) {
                    if (this.debug > 0) {
                        System.err.printf("Copying From dst File: %x bytes from location: %x\n", d.getLength() * (long)this.blkSize, dstOffset * (long)this.blkSize);
                    }
                    DiffWriter.copyBlocks(this.dstFile, this.blkSize, dstOffset, d.getLength(), 0L);
                    if (this.verbose) {
                        System.err.printf("Copied blocks:    Differences offset: %4d, %d blocks from offset %d\n", d.getOffset(), d.getLength(), dstOffset);
                    }
                    diffOffset += d.getLength();
                    this.currentOutputOffset += d.getLength() * (long)this.blkSize;
                } else if (this.verbose) {
                    System.err.printf("Duplicate blocks: Differences offset: %4d, %d blocks from offset %d\n", d.getOffset(), d.getLength(), dstOffset);
                }
            }
            dstOffset += d.getLength();
        }
        if (this.verbose) {
            System.err.printf("Local Data Blocks end at offset %d (%x)\n", this.currentOutputOffset, this.currentOutputOffset);
        }
    }

    public static void copyBlocks(FileChannel fd, int blkSize, long offset, long count, long skip) throws IOException {
        byte[] buffer = new byte[blkSize];
        ByteBuffer input = ByteBuffer.wrap(buffer);
        fd.position(offset * (long)blkSize + skip);
        int i = 0;
        while ((long)i < count) {
            int len;
            while ((len = fd.read(input)) != -1 && input.hasRemaining()) {
            }
            input.flip();
            if (input.remaining() != blkSize) {
                System.err.printf("Bad Block copy: blk: %d(%d), read: %d\n", offset, offset * (long)blkSize, input.remaining());
                System.exit(2);
            }
            System.out.write(buffer);
            input.rewind();
            ++i;
        }
    }
}
