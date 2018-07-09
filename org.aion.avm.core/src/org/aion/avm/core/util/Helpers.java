package org.aion.avm.core.util;

import org.aion.avm.api.BlockchainRuntime;
import org.aion.avm.api.IBlockchainRuntime;
import org.aion.avm.core.classloading.AvmClassLoader;
import org.aion.avm.internal.Helper;
import org.aion.avm.internal.IHelper;
import org.aion.avm.internal.RuntimeAssertionError;

import java.io.*;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;


/**
 * Common utilities we often want to use in various tests (either temporarily or permanently).
 * These are kept here just to avoid duplication.
 */
public class Helpers {

    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    /**
     * Converts byte array into it's hex string representation.
     *
     * @param bytes
     * @return
     */
    public static String toHexString(byte[] bytes) {
        int length = bytes.length;

        char[] hexChars = new char[length * 2];
        for (int i = 0; i < length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Writes the given bytes to the file at the given path.
     * This is effective for dumping re-written bytecode to file for offline analysis.
     *
     * @param bytes The bytes to write.
     * @param path  The path where the file should be written.
     */
    public static void writeBytesToFile(byte[] bytes, String path) {
        File f = new File(path);
        f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            fos.write(bytes);
        } catch (IOException e) {
            // This is for tests so we aren't expecting the failure.
            Assert.unexpected(e);
        }
    }

    /**
     * Reads file as a byte array.
     *
     * @param path
     * @return
     */
    public static byte[] readFileToBytes(String path) {
        File f = new File(path);
        byte[] b = new byte[(int) f.length()];

        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            in.readFully(b);
        } catch (IOException e) {
            Assert.unexpected(e);
        }

        return b;
    }

    /**
     * A helper which will attempt to load the given resource path as bytes.
     * Returns null if the resource could not be found.
     *
     * @param resourcePath The path to this resource, within the parent class loader.
     * @return The resource as bytes, or null if not found.
     */
    public static byte[] loadRequiredResourceAsBytes(String resourcePath) {
        InputStream stream = Helpers.class.getClassLoader().getResourceAsStream(resourcePath);
        byte[] raw = null;
        if (null != stream) {
            try {
                raw = stream.readAllBytes();
            } catch (IOException e) {
                Assert.unexpected(e);
            }
        }
        return raw;
    }

    private static SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate random byte array of the specified length.
     *
     * @param n
     * @return
     */
    public static byte[] randomBytes(int n) {
        byte[] bytes = new byte[n];
        secureRandom.nextBytes(bytes);

        return bytes;
    }

    /**
     * Converts a fully qualified class name into it's JVM internal form.
     *
     * @param fullyQualifiedName
     * @return
     */
    public static String fulllyQualifiedNameToInternalName(String fullyQualifiedName) {
        return fullyQualifiedName.replaceAll("\\.", "/");
    }

    /**
     * Converts a JVM internal class name into a fully qualified name.
     *
     * @param internalName
     * @return
     */
    public static String internalNameToFulllyQualifiedName(String internalName) {
        return internalName.replaceAll("/", ".");
    }

    private static String helperClassName = Helper.class.getName();
    private static byte[] helperBytes = Helpers.loadRequiredResourceAsBytes(helperClassName.replaceAll("\\.", "/") + ".class");

    private static String blockchainRuntimeClassName = BlockchainRuntime.class.getName();
    private static byte[] blockchainRuntimeBytes = Helpers.loadRequiredResourceAsBytes(blockchainRuntimeClassName.replaceAll("\\.", "/") + ".class");

    /**
     * A common helper used to construct a map of visible class bytecode for an AvmClassLoader instance.
     * Typically, this is used right before "instantiateHelper()", below (this creates/adds the class it loads).
     *
     * @param inputMap The initial map of class names to bytecodes.
     * @return The inputMap with the Helper bytecode added.
     */
    public static Map<String, byte[]> mapIncludingHelperBytecode(Map<String, byte[]> inputMap) {
        Map<String, byte[]> modifiedMap = new HashMap<>(inputMap);
        modifiedMap.put(helperClassName, helperBytes);
        modifiedMap.put(blockchainRuntimeClassName, blockchainRuntimeBytes);

        return modifiedMap;
    }

    /**
     * Loads and instantiates the IHelper instance to access the "Helper" statics within the given contractLoader.
     * This "Helper" bytecode is typically added to the classloader using the "mapIncludingHelperBytecode", above.
     *
     * @param contractLoader The loader which will load all the code running within the contract.
     * @param energyLimit The energy limit
     * @return The instance which will trampoline into the "Helper" statics called by the instrumented code within this contract.
     */
    public static IHelper instantiateHelper(AvmClassLoader contractLoader, long energyLimit) {
        IHelper helper = null;
        try {
            String helperClassName = Helper.class.getName();
            Class<?> helperClass = contractLoader.loadClass(helperClassName);
            helper = (IHelper) helperClass.getConstructor(ClassLoader.class, long.class).newInstance(contractLoader, energyLimit);
        } catch (Throwable t) {
            // Errors at this point imply something wrong with the installation so fail.
            RuntimeAssertionError.unexpected(t);
        }
        return helper;
    }

    /**
     * Attaches a BlockchainRuntime instance to the Helper class (per contract) so DApp can
     * access blockchain related methods.
     *
     * @param contractLoader
     * @param rt
     */
    public static void attachBlockchainRuntime(AvmClassLoader contractLoader, IBlockchainRuntime rt) {
        try {
            String helperClassName = Helper.class.getName();
            Class<?> helperClass = contractLoader.loadClass(helperClassName);
            helperClass.getField("blockchainRuntime").set(null, rt);
        } catch (Throwable t) {
            // Errors at this point imply something wrong with the installation so fail.
            RuntimeAssertionError.unexpected(t);
        }
    }

    // for test suites only
    public static byte[] address(int n) {
        byte[] arr = new byte[32];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) n;
        }
        return arr;
    }

    public static byte[] merge(byte[]...arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }

        byte[] ret = new byte[length];
        int start = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, ret, start, array.length);
            start += array.length;
        }

        return ret;
    }
}
