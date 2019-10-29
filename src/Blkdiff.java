/*
 * Decompiled with CFR 0.146.
 */
package blkdiff;

import blkdiff.BlkInfo;
import blkdiff.DataChunk;
import blkdiff.DiffReader;
import blkdiff.DiffWriter;
import blkdiff.FileHash;
import blkdiff.SrcHash;
import gnu.getopt.Getopt;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Blkdiff {
    private static int blkSize = 4096;
    private static int blkShift = 12;
    private static String hash = "SHA-256";
    private static int debug = 0;
    private static boolean verbose = false;
    private static boolean paranoid = false;
    private static Mode mode = Mode.BUILD;
    private static boolean doInputHash = true;
    private static boolean doOutputHash = true;
    private static byte[] inputHashValue = null;
    private static byte[] outputHashValue = null;
    private static SrcHash srcHash = null;
    private static FileHash inputHash;
    private static FileHash outputHash;
    private static Path srcPath;
    private static Path dstPath;
    private static FileChannel srcFile;
    private static FileChannel dstFile;
    private static DiffReader diffReader;
    private static /* synthetic */ int[] $SWITCH_TABLE$blkdiff$Blkdiff$Mode;
    private static /* synthetic */ int[] $SWITCH_TABLE$blkdiff$BlkInfo$BlockSource;

    static {
        srcFile = null;
        dstFile = null;
        diffReader = null;
    }

    private static enum Mode {
        BUILD,
        REBUILD,
        VERIFY;
        
    }

    public static void main(String[] args) {
        int c;
        Getopt g = new Getopt("BlkDiff", args, "Vb:dvi:Io:Orph:");
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'b': {
                    int bs;
                    String arg = g.getOptarg();
                    if (arg == null) {
                        Blkdiff.usage();
                    }
                    if ((bs = Integer.parseInt(arg)) > 6 && bs < 16) {
                        blkSize = 1 << bs;
                        blkShift = bs;
                    }
                }
                case 'd': {
                    ++debug;
                    break;
                }
                case 'v': {
                    verbose = true;
                    break;
                }
                case 'h': {
                    String arg = g.getOptarg();
                    if (arg == null) {
                        Blkdiff.usage();
                    }
                    hash = arg;
                    break;
                }
                case 'p': {
                    paranoid = true;
                    break;
                }
                case 'r': {
                    mode = Mode.REBUILD;
                    break;
                }
                case 'V': {
                    mode = Mode.VERIFY;
                    break;
                }
                case 'i': {
                    String arg = g.getOptarg();
                    if (arg == null) {
                        Blkdiff.usage();
                    }
                    inputHashValue = Blkdiff.parseHash(arg);
                    break;
                }
                case 'o': {
                    String arg = g.getOptarg();
                    if (arg == null) {
                        Blkdiff.usage();
                    }
                    outputHashValue = Blkdiff.parseHash(arg);
                    break;
                }
                case 'I': {
                    doInputHash = false;
                    break;
                }
                case 'O': {
                    doOutputHash = false;
                    break;
                }
                case '?': {
                    break;
                }
                default: {
                    System.err.println("getopt() returned " + c);
                }
            }
        }
        if (args.length - g.getOptind() < 2) {
            System.err.println("Insufficient Arguments");
            Blkdiff.usage();
        }
        srcPath = Paths.get(args[g.getOptind()], new String[0]);
        dstPath = Paths.get(args[g.getOptind() + 1], new String[0]);
        if (!Files.isRegularFile(srcPath, new LinkOption[0]) || !Files.isReadable(srcPath)) {
            System.err.println("Source file " + args[g.getOptind()] + " Not a regular file: ");
            Blkdiff.usage();
        }
        try {
            srcFile = FileChannel.open(srcPath, StandardOpenOption.READ);
            dstFile = FileChannel.open(dstPath, StandardOpenOption.READ);
        }
        catch (IOException e) {
            System.err.println("Cannot open Source files for Reading: ");
            Blkdiff.usage();
        }
        try {
            srcHash = new SrcHash(hash, blkSize, srcFile, dstFile, debug, verbose, paranoid);
            inputHash = new FileHash(hash);
            outputHash = new FileHash(hash);
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Invalid Hash Function");
            Blkdiff.usage();
        }
        if (g.getOptind() != args.length - 2) {
            System.err.println("Invalid Number of Arguments. Expected two named arguments");
            Blkdiff.usage();
        }
        if (doInputHash && inputHashValue == null) {
            if (verbose) {
                System.err.println("Hashing Source File");
            }
            inputHashValue = Blkdiff.hashFile(srcFile, inputHash);
        } else if (inputHashValue == null) {
            inputHashValue = new byte[16];
            Arrays.fill(inputHashValue, (byte)0);
        }
        if (verbose) {
            System.err.printf("Input File Hash: %s\n", SrcHash.hexString(inputHashValue));
        }
        switch (mode) {
            case Mode.BUILD: {
                Blkdiff.doBuild();
                break;
            }
            case Mode.REBUILD: {
                diffReader = new DiffReader(dstFile);
                Blkdiff.doVerify();
                try {
                    Blkdiff.doCopyBlocks();
                }
                catch (IOException e) {
                    System.err.println("Error when rebuilding the destination file.\nTry using -v for more information.");
                    e.printStackTrace();
                }
                break;
            }
            case Mode.VERIFY: {
            default: {
                diffReader = new DiffReader(dstFile);
                Blkdiff.doVerify();
                if (!verbose && debug <= 0) break;
                Blkdiff.doWalkBlocks();
            }
        }
    }

    private static void doBuild() {
        long length;
        if (doOutputHash && outputHashValue == null) {
            if (verbose) {
                System.err.println("Hashing Destintion File");
            }
            outputHashValue = Blkdiff.hashFile(dstFile, outputHash);
        } else if (outputHashValue == null) {
            outputHashValue = new byte[16];
            Arrays.fill(outputHashValue, (byte)0);
        }
        if (verbose) {
            System.err.printf("Output File Hash: %s\n", SrcHash.hexString(outputHashValue));
        }
        DiffWriter diffWriter = new DiffWriter(blkShift, inputHashValue, outputHashValue, hash, srcFile, dstFile, debug, verbose);
        if (verbose) {
            System.err.println("Constructing source file Hash structures");
        }
        try {
            length = srcFile.size();
            int len = 0;
            for (long offset = 0L; offset < length; offset += (long)Blkdiff.blkSize) {
                byte[] b = new byte[blkSize];
                ByteBuffer input = ByteBuffer.wrap(b);
                srcFile.position(offset);
                while ((len = srcFile.read(input)) != -1 && input.hasRemaining()) {
                }
                input.flip();
                if (input.remaining() == blkSize) {
                    srcHash.buildBlock(BlkInfo.BlockSource.INPUT, offset / (long)blkSize, b, 0, input.remaining());
                }
                input.rewind();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        diffWriter.writeHeader();
        long dstOffset = 0L;
        BlkInfo expected = new BlkInfo(BlkInfo.BlockSource.NONE, -1L);
        try {
            length = dstFile.size();
            int len = 0;
            long blkNum = 0L;
            long offset = 0L;
            while (offset < length) {
                byte[] b = new byte[blkSize];
                ByteBuffer input = ByteBuffer.wrap(b);
                dstFile.position(offset);
                while ((len = dstFile.read(input)) != -1 && input.hasRemaining()) {
                }
                input.flip();
                if (input.remaining() > 0) {
                    if (input.remaining() == blkSize) {
                        BlkInfo blkinfo;
                        if (verbose || debug > 0) {
                            System.err.printf("Chunk %06d: ", blkNum);
                        }
                        if ((blkinfo = srcHash.getBlock(b, expected)) == null) {
                            diffWriter.writeBlockLocation(BlkInfo.BlockSource.OUTPUT, dstOffset, false);
                            srcHash.addBlock(b, dstOffset);
                            ++dstOffset;
                            expected.source = BlkInfo.BlockSource.NONE;
                            expected.offset = -1L;
                        } else if (blkinfo.source == BlkInfo.BlockSource.ZERO) {
                            diffWriter.writeBlockLocation(blkinfo.source, blkNum, true);
                            expected.source = BlkInfo.BlockSource.ZERO;
                            expected.offset = blkNum + 1L;
                            if (verbose || debug > 0) {
                                System.err.printf("Found ZERO at %d -> stored at %d\n", expected.offset, dstOffset);
                            }
                        } else {
                            diffWriter.writeBlockLocation(blkinfo.source, blkinfo.offset, true);
                            expected.source = blkinfo.source;
                            expected.offset = blkinfo.offset + 1L;
                        }
                    } else {
                        if (verbose || debug > 0) {
                            System.err.printf("Writing last %d bytes to differences file as a partial block\n", input.remaining());
                        }
                        System.out.write(b, 0, input.remaining());
                    }
                }
                input.rewind();
                offset += (long)blkSize;
                ++blkNum;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            diffWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doVerify() {
        try {
            if (!diffReader.verifyHeader()) {
                System.err.println("Failed to Verify Header - aborting");
                System.exit(1);
            }
            if (verbose) {
                System.err.println("Header Match.");
                System.err.printf("Input Hash:  %s\n", SrcHash.hexString(diffReader.getInputHash()));
                System.err.printf("Output Hash: %s\n", SrcHash.hexString(diffReader.getOutputHash()));
                System.err.printf("Hash Function: %s\n", diffReader.getHash());
                System.err.printf("Block Size: %d\n", diffReader.getBlkSize());
                System.err.printf("Data Chunk Descriptors: %d\n", diffReader.getBlockDataLength());
            }
            if (doInputHash) {
                if (Arrays.equals(diffReader.getInputHash(), inputHashValue)) {
                    if (verbose) {
                        System.err.println("Input File Hashes Match");
                    }
                } else {
                    System.err.printf("\n********\n\nInput File Hash Mismatch:\n", new Object[0]);
                    System.err.printf("Header Input Hash: %s\n", SrcHash.hexString(diffReader.getInputHash()));
                    System.err.printf("Calculated Hash:   %s\n", SrcHash.hexString(inputHashValue));
                    System.err.printf("Use option '-I' to ignore\n", new Object[0]);
                    System.exit(2);
                }
            }
            if (doInputHash || doOutputHash) {
                if (hash.equals(diffReader.getHash())) {
                    if (verbose) {
                        System.err.println("Hash Functions Match");
                    }
                } else {
                    System.err.printf("\n********\n\nHash Functions Mismatch:\n", new Object[0]);
                    System.err.printf("Header Hash function: %s\n", diffReader.getHash());
                    System.err.printf("Local Hash function:  %s\n", hash);
                    System.err.printf("Use option '-h hash_name' to change\n", new Object[0]);
                    System.err.printf("Use options '-I' and '-O' to ignore\n", new Object[0]);
                    System.exit(2);
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void doWalkBlocks() {
        long length = diffReader.getBlockDataLength();
        int count = 0;
        count = 0;
        while ((long)count < length) {
            DataChunk dc = diffReader.getBlockData(count);
            if (dc == null) {
                System.err.printf("Invalid Data Chunk Descriptor\n", new Object[0]);
                System.exit(1);
            }
            if (verbose) {
                System.err.printf("Descriptor %d: %6s, Offset: %d, Length: %d\n", new Object[]{count, dc.getFile(), dc.getOffset(), dc.getLength()});
            }
            ++count;
        }
    }

    private static void doCopyBlocks() throws IOException {
        long length = diffReader.getBlockDataLength();
        int count = 0;
        long currentOutputOffset = 0L;
        long baseOffset = dstFile.position();
        byte[] zero = srcHash.getZero();
        count = 0;
        while ((long)count < length) {
            DataChunk dc = diffReader.getBlockData(count);
            if (dc == null) {
                System.err.printf("Invalid Data Chunk Descriptor\n", new Object[0]);
                System.exit(1);
            }
            if (verbose) {
                System.err.printf("Descriptor %d: %6s, Offset: %d, Length: %d\n", new Object[]{count, dc.getFile(), dc.getOffset(), dc.getLength()});
            }
            long num = dc.getLength();
            switch (Blkdiff.$SWITCH_TABLE$blkdiff$BlkInfo$BlockSource()[dc.getFile().ordinal()]) {
                case 4: {
                    if (verbose) {
                        System.err.printf("Writing %x zero bytes to location %x\n", num * (long)blkSize, currentOutputOffset);
                    }
                    int i = 0;
                    while ((long)i < num) {
                        System.out.write(zero);
                        currentOutputOffset += (long)blkSize;
                        ++i;
                    }
                    break;
                }
                case 2: {
                    if (verbose) {
                        System.err.printf("Writing %x INPUT bytes from location %x to location %x\n", num * (long)blkSize, dc.getOffset() * (long)blkSize, currentOutputOffset);
                    }
                    DiffWriter.copyBlocks(srcFile, blkSize, dc.getOffset(), num, 0L);
                    currentOutputOffset += num * (long)blkSize;
                    break;
                }
                case 3: {
                    if (verbose) {
                        System.err.printf("Writing %x DIFF bytes from location %x to location %x\n", num * (long)blkSize, dc.getOffset() * (long)blkSize + baseOffset, currentOutputOffset);
                    }
                    DiffWriter.copyBlocks(dstFile, blkSize, dc.getOffset(), num, baseOffset);
                    currentOutputOffset += num * (long)blkSize;
                    break;
                }
                default: {
                    System.err.printf("Invalid Descriptor Source: %s\n", new Object[]{dc.getFile()});
                }
            }
            ++count;
        }
    }

    private static byte[] hashFile(FileChannel fd, FileHash fh) {
        try {
            long length = fd.size();
            int len = 0;
            for (long offset = 0L; offset < length; offset += (long)Blkdiff.blkSize) {
                byte[] b = new byte[blkSize];
                ByteBuffer input = ByteBuffer.wrap(b);
                fd.position(offset);
                while ((len = fd.read(input)) != -1 && input.hasRemaining()) {
                }
                input.flip();
                if (input.remaining() > 0) {
                    fh.update(b, 0, input.remaining());
                }
                input.rewind();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.exit(2);
        }
        return fh.digest();
    }

    private static int ctoh(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
        }
        System.err.printf("Invalid Character '%c' in hash string\n", Character.valueOf(c));
        System.exit(2);
        return 0;
    }

    private static byte[] parseHash(String hash) {
        int len = hash.length() / 2;
        byte[] res = new byte[len];
        for (int i = 0; i < len; ++i) {
            res[i] = (byte)(Blkdiff.ctoh(hash.charAt(i * 2)) << 4 | Blkdiff.ctoh(hash.charAt(i * 2 + 1)));
        }
        return res;
    }

    private static void usage() {
        System.err.println("Usage: BlkDiff [-b blockSize] [-d] [-v] [-h hash_function] [-i input_hash] [-I] [-o output_hash] [-O] [-p] source_file {dest_file| -r diff_file}");
        System.err.println("-b block_size: block size in 2^ bytes. Only 7 to 15 are valid values");
        System.err.println("-d:            debug (stutter for additional debugging)");
        System.err.println("-v:            verbose");
        System.err.println("-h hash_function: Select hash function. Default: SHA-256");
        System.err.println("-i input_hash: specify, don't calculate, hash of source file");
        System.err.println("-o output_hash: specify, don't calculate, hash of destination file");
        System.err.println("-I:            ignore input file hash computation/verification");
        System.err.println("-O:            ignore output file hash computation/verification");
        System.err.println("-p:            pedantic check of block values rather than hash comparison only");
        System.err.println("-r:            reconstruct. The second file is taken to be a block differences file and the original destination file is rebuilt.");
        System.exit(1);
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
