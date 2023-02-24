package io.github.moonlightmaya.conversion;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Class used to automatically convert between records and byte arrays.
 * Be warned: EXTREME amounts of generic jank inside.
 * View at your own risk.
 */
public class BinaryConverter {

    private static final Map<Class<?>, Handler<?>> HANDLERS = new HashMap<>();
    static {
        //Primitives
        registerType(boolean.class, wrap(DataOutputStream::writeBoolean), wrap(DataInputStream::readBoolean));
        registerType(byte.class, wrap((d, i) -> d.writeByte(i)), wrap(DataInputStream::readByte));
        registerType(short.class, wrap((d, i) -> d.writeShort(i)), wrap(DataInputStream::readShort));
        registerType(int.class, wrap(DataOutputStream::writeInt), wrap(DataInputStream::readInt));
        registerType(long.class, wrap(DataOutputStream::writeLong), wrap(DataInputStream::readLong));
        registerType(float.class, wrap(DataOutputStream::writeFloat), wrap(DataInputStream::readFloat));
        registerType(double.class, wrap(DataOutputStream::writeDouble), wrap(DataInputStream::readDouble));
        //Vectors
        registerType(Vector2f.class,
                wrap((d, v) -> {d.writeFloat(v.x); d.writeFloat(v.y);}),
                wrap(d -> new Vector2f(d.readFloat(), d.readFloat())));
        registerType(Vector3f.class,
                wrap((d, v) -> {d.writeFloat(v.x); d.writeFloat(v.y); d.writeFloat(v.z);}),
                wrap(d -> new Vector3f(d.readFloat(), d.readFloat(), d.readFloat())));
        registerType(Vector4f.class,
                wrap((d, v) -> {d.writeFloat(v.x); d.writeFloat(v.y); d.writeFloat(v.z); d.writeFloat(v.w);}),
                wrap(d -> new Vector4f(d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat())));
        //Extras
        registerType(String.class,
                wrap((d, s) -> {
                    byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                    d.writeInt(bytes.length);
                    d.write(bytes);
                }),
                wrap(d -> new String(d.readNBytes(d.readInt()), StandardCharsets.UTF_8)));
        registerType(byte[].class,
                wrap((d, s) -> {
                    d.writeInt(s.length);
                    d.write(s);
                }),
                wrap(d -> {
                    int len = d.readInt();
                    return d.readNBytes(len);
                }));
    }

    private static <T> void registerType(Class<T> clazz, BiConsumer<DataOutputStream, T> writer, Function<DataInputStream, T> reader) {
        HANDLERS.put(clazz, new Handler<>(clazz, writer, reader));
    }

    private static  <T> Handler<T> getHandler(Class<T> clazz) {
        if (!HANDLERS.containsKey(clazz))
            throw new IllegalArgumentException("Record contains unregistered class: " + clazz.getName());
        return (Handler<T>) HANDLERS.get(clazz);
    }

    /**
     * Writes the data from the given instance to the output stream.
     */
    public static <T> void write(DataOutputStream outputStream, Class<T> writtenType, T instance, Class... generics) {
        try {
            if (instance instanceof CustomHandling custom) {
                custom.write(outputStream);
            } else if (writtenType.isRecord()) {
                for (RecordComponent component : writtenType.getRecordComponents()) {
                    if (component.isAnnotationPresent(Nullable.class)) {
                        outputStream.writeBoolean(instance != null);
                    }
                    if (instance != null) {
                        Class componentType = component.getType();
                        Object value = component.getAccessor().invoke(instance);
                        if (component.getGenericType() instanceof ParameterizedType parameterizedType) {
                            Class genericClass = (Class) parameterizedType.getActualTypeArguments()[0];
                            write(outputStream, componentType, value, genericClass);
                        } else {
                            write(outputStream, componentType, value);
                        }
                    }
                }
            } else if (writtenType.isEnum()) {
                int index = 0;
                for (Object x : writtenType.getEnumConstants()) {
                    if (x == instance) break;
                    index++;
                }
                outputStream.writeInt(index);
            } else if (writtenType == List.class) {
                List<?> list = (List<?>) instance;
                int size = list.size();
                outputStream.writeInt(size);
                for (Object o : list) {
                    write(outputStream, generics[0], o);
                }
            } else {
                Handler<T> handler = getHandler(writtenType);
                handler.writer.accept(outputStream, instance);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the data from the given input stream and
     * produces an instance of the class.
     */
    public static <T> T read(DataInputStream inputStream, Class<T> readType, Class... generics) {
        try {
            if (CustomHandling.class.isAssignableFrom(readType)) {
                Constructor constructor = readType.getConstructor(DataInputStream.class);
                return (T) constructor.newInstance(inputStream);
            } else if (readType.isRecord()) {
                Constructor[] constructors = readType.getDeclaredConstructors();
                if (constructors.length != 1)
                    throw new IllegalArgumentException("Read type must have only one constructor!");
                Object[] args = new Object[constructors[0].getParameterCount()];
                int i = 0;
                for (RecordComponent component : readType.getRecordComponents()) {
                    if (component.isAnnotationPresent(Nullable.class)) {
                        if (!inputStream.readBoolean())
                            return null;
                    }
                    Class<?> componentType = component.getType();
                    if (component.getGenericType() instanceof ParameterizedType parameterizedType) {
                        Class genericClass = (Class) parameterizedType.getActualTypeArguments()[0];
                        args[i] = read(inputStream, componentType, genericClass);
                    } else {
                        args[i] = read(inputStream, componentType);
                    }
                    i++;
                }
                return (T) constructors[0].newInstance(args);
            } else if (readType.isEnum()) {
                return readType.getEnumConstants()[inputStream.readInt()];
            } else if (readType == List.class) {
                int len = inputStream.readInt();
                List list = new ArrayList(len);
                for (int i = 0; i < len; i++) {
                    list.add(read(inputStream, generics[0]));
                }
                return (T) list;
            } else {
                Handler handler = getHandler(readType);
                return (T) handler.reader.apply(inputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record Handler<T>(Class<T> clazz, BiConsumer<DataOutputStream, T> writer, Function<DataInputStream, T> reader) {}

    @FunctionalInterface
    private interface Writer<T> {
        void write(DataOutputStream dos, T obj) throws IOException;
    }

    @FunctionalInterface
    private interface Reader<T> {
        T read(DataInputStream dis) throws IOException;
    }

    private static <T> BiConsumer<DataOutputStream, T> wrap(Writer<T> writer) {
        return (dos, obj) -> {
            try {
                writer.write(dos, obj);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private static <T> Function<DataInputStream, T> wrap(Reader<T> reader) {
        return dis -> {
            try {
                return reader.read(dis);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * Contract: by implementing this, you must create a
     * constructor that accepts a DataInputStream.
     */
    public interface CustomHandling {
        void write(DataOutputStream dos);
    }
}
