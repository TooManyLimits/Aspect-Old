package io.github.moonlightmaya.util;

import com.google.common.collect.ImmutableMap;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class IOUtils {

    /**
     * Doesn't recurse because im too dumb
     * to allow subfolders in aspects
     */
    public static ArrayList<File> getByExtension(Path root, String extension) {
        File file = root.toFile();
        ArrayList<File> list = new ArrayList<>();
        if (file.exists() && file.isDirectory()) {
            File[] arr = root.toFile().listFiles((f,s) -> s.endsWith("."+extension));
            if (arr != null)
                Collections.addAll(list, arr);
        }
        return list;
    }

    //Compacting strategy:
    //first write an initial byte,
    //this byte contains information about the upcoming values .
    //every two bits of the initial byte carry information about an element
    //00 -> this element is zero
    //01 -> this element is a single integer that fits in a byte (signed )
    //10 -> this element is a float 0-1 that fits in a byte
    //11 -> this element needs full float precision
    //case 0 : the element is conveyed with no additional info
    //case 1 or 2 : only a single byte is needed for the element
    //case 3 , all 4 float bytes are needed.
    //most cases will likely fall into the first 3 categories , making this
    //an efficient encoding method. rarely, when all elements do not fit in one of the
    //first 3 categories , it costs an extra byte

    private static int getCase(float v) {
        if (v == 0) return 0;
        if ((byte) v == v) return 1;
        if (v >= 0 && v < 1 && ((int) (v * 256) == v*256)) return 2;
        return 3;
    }

    private static void writeVectorElem(DataOutputStream dos, float v, int vCase) throws IOException {
        switch (vCase) {
            case 0 -> {}
            case 1 -> dos.write((byte) v);
            case 2 -> dos.write((int) (v*256));
            case 3 -> dos.writeFloat(v);
            default -> throw new IllegalArgumentException("Invalid case arg to Vector write: " + vCase);
        }
    }

    private static float readVectorElem(DataInputStream dis, int vCase) throws IOException {
        return switch (vCase) {
            case 0 -> 0;
            case 1 -> (byte) (dis.read());
            case 2 -> dis.read() / 256f;
            case 3 -> dis.readFloat();
            default -> throw new IllegalArgumentException("Invalid case arg to Vector read: " + vCase);
        };
    }

    public static void writeVector3f(DataOutputStream dos, Vector3f vec) throws IOException {
        int xCase = getCase(vec.x);
        int yCase = getCase(vec.y);
        int zCase = getCase(vec.z);
        dos.write(xCase | (yCase << 2) | (zCase << 4));
        writeVectorElem(dos, vec.x, xCase);
        writeVectorElem(dos, vec.y, yCase);
        writeVectorElem(dos, vec.z, zCase);
    }

    public static Vector3f readVector3f(DataInputStream dis) throws IOException {
        int initial = dis.read();
        int xCase = initial & 0b11;
        int yCase = (initial & 0b1100) >> 2;
        int zCase = (initial & 0b110000) >> 4;
        float x = readVectorElem(dis, xCase);
        float y = readVectorElem(dis, yCase);
        float z = readVectorElem(dis, zCase);
        return new Vector3f(x, y, z);
    }

    public static void writeVector4f(DataOutputStream dos, Vector4f vec) throws IOException {
        int xCase = getCase(vec.x);
        int yCase = getCase(vec.y);
        int zCase = getCase(vec.z);
        int wCase = getCase(vec.w);
        dos.write(xCase | (yCase << 2) | (zCase << 4) | (wCase << 6));
        writeVectorElem(dos, vec.x, xCase);
        writeVectorElem(dos, vec.y, yCase);
        writeVectorElem(dos, vec.z, zCase);
        writeVectorElem(dos, vec.w, wCase);
    }

    public static Vector4f readVector4f(DataInputStream dis) throws IOException {
        int initial = dis.read();
        int xCase = initial & 0b11;
        int yCase = (initial & 0b1100) >> 2;
        int zCase = (initial & 0b110000) >> 4;
        int wCase = (initial & 0b11000000) >> 6;
        float x = readVectorElem(dis, xCase);
        float y = readVectorElem(dis, yCase);
        float z = readVectorElem(dis, zCase);
        float w = readVectorElem(dis, wCase);
        return new Vector4f(x, y, z, w);
    }

    public static class AspectIOException extends RuntimeException {
        public AspectIOException(IOException wrapped) {
            super(wrapped);
        }
    }

}
