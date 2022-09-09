package com.max480.randomstuff.gae;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Based on https://github.com/max4805/RandomBackendStuff/blob/main/src/mod-structure-verifier/BinToXML.java,
 * except this outputs JSON instead of outputting XML, for ease of use by the frontend.
 */
@WebServlet(name = "BinToJSONService", urlPatterns = {"/celeste/bin-to-json"})
public class BinToJSONService extends HttpServlet {
    private static final java.util.logging.Logger logger = Logger.getLogger("BinToJSON");


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject json = toJsonDocument(new BufferedInputStream(req.getInputStream()));

        if (json != null) {
            resp.setContentType("application/json");
            IOUtils.write(json.toString(), resp.getWriter());
        } else {
            resp.setStatus(400);
            resp.setContentType("text/plain");
            IOUtils.write("Error parsing the map bin!", resp.getWriter());
        }
    }

    private enum AttributeValueType {
        Boolean(0),
        Byte(1),
        Short(2),
        Integer(3),
        Float(4),
        FromLookup(5),
        String(6),
        LengthEncodedString(7);

        private final int value;

        AttributeValueType(int value) {
            this.value = value;
        }

        public static AttributeValueType fromValue(int value) {
            return Arrays.stream(AttributeValueType.values())
                    .filter(v -> v.value == value)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Attribute value " + value + " does not exist!"));
        }
    }

    private static JSONObject toJsonDocument(InputStream is) {
        long startTime = System.currentTimeMillis();

        try (DataInputStream bin = new DataInputStream(is)) {
            JSONObject root = new JSONObject();
            root.put("name", "CelesteMap");
            root.put("attributes", new JSONObject());
            root.put("children", new JSONArray());

            readString(bin); // skip "CELESTE MAP"
            root.getJSONObject("attributes").put("Package", readString(bin));

            int lookupTableSize = readShort(bin);
            String[] stringLookupTable = new String[lookupTableSize];
            for (int i = 0; i < lookupTableSize; i++) {
                stringLookupTable[i] = readString(bin);
            }
            recursiveConvert(bin, root, stringLookupTable, true);

            logger.info("Converted input to JSON in " + (System.currentTimeMillis() - startTime) + " ms");
            return root;
        } catch (Exception e) {
            logger.warning("Could not convert BIN to JSON! " + e);
            return null;
        }
    }

    // strings are encoded by C# by writing the character count in LEB128 format, then the string itself.

    private static String readString(DataInputStream bin) throws Exception {
        // read LEB128-encoded number, see https://en.wikipedia.org/wiki/LEB128
        int length = 0;
        int shift = 0;
        while (true) {
            int next = bin.readUnsignedByte();
            length |= (next & 0b01111111) << shift;
            if ((next & 0b10000000) == 0) {
                break;
            }
            shift += 7;
        }

        // read the string itself now!
        byte[] stringBytes = new byte[length];
        if (bin.read(stringBytes) != length) throw new IOException("Missing characters in string!");
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    // we need our own readShort() and readInt() methods because Java's ones don't have the endianness we want.
    // in other words, we want to read the bytes backwards.

    private static short readShort(DataInputStream bin) throws Exception {
        int byte1 = bin.readUnsignedByte();
        int byte2 = bin.readUnsignedByte();

        // just swap the bytes and we'll be fine lol
        return (short) ((byte2 << 8) + byte1);
    }

    private static int readInt(DataInputStream bin) throws Exception {
        int byte1 = bin.readUnsignedByte();
        int byte2 = bin.readUnsignedByte();
        int byte3 = bin.readUnsignedByte();
        int byte4 = bin.readUnsignedByte();

        // reading numbers backwards is fun!
        return (byte4 << 24) + (byte3 << 16) + (byte2 << 8) + byte1;
    }

    private static void recursiveConvert(DataInputStream bin, JSONObject current, String[] stringLookupTable, boolean first) throws Exception {
        JSONObject element;
        if (!first) {
            element = new JSONObject();
            element.put("name", stringLookupTable[readShort(bin)]);
            element.put("attributes", new JSONObject());
            element.put("children", new JSONArray());

            current.getJSONArray("children").put(element);
        } else {
            element = current;
            readShort(bin);
        }

        recursiveConvertAttributes(bin, element, stringLookupTable, bin.readUnsignedByte());

        short childrenCount = readShort(bin);
        for (int i = 0; i < childrenCount; i++) {
            recursiveConvert(bin, element, stringLookupTable, false);
        }
    }

    private static void recursiveConvertAttributes(DataInputStream bin, JSONObject element, String[] stringLookupTable, int count) throws Exception {
        for (byte b = 0; b < count; b = (byte) (b + 1)) {
            String localName = stringLookupTable[readShort(bin)];
            AttributeValueType attributeValueType = AttributeValueType.fromValue(bin.readUnsignedByte());
            Object obj = null;
            switch (attributeValueType) {
                case Boolean:
                    obj = bin.readBoolean();
                    break;
                case Byte:
                    obj = bin.readUnsignedByte();
                    break;
                case Float:
                    obj = Float.intBitsToFloat(readInt(bin));
                    break;
                case Integer:
                    obj = readInt(bin);
                    break;
                case LengthEncodedString: {
                    short length = readShort(bin);
                    byte[] array = new byte[length];
                    if (bin.read(array) != length) throw new IOException("Missing characters in string!");

                    StringBuilder result = new StringBuilder();
                    for (int i = 0; i < array.length; i += 2) {
                        // byte 1 is how many times the byte is repeated (unsigned), byte 2 is the byte.
                        // the result isn't an UTF-8 string, but a sequence of UTF-8 code points,
                        // so we are converting them individually rather than calling new String(bytes, UTF_8).

                        int countTimes = unsignedByteToInt(array[i]);
                        int codePoint = unsignedByteToInt(array[i + 1]);

                        for (int j = 0; j < countTimes; j++) {
                            result.append(Character.toChars(codePoint));
                        }
                    }
                    obj = result;
                    break;
                }
                case Short:
                    obj = readShort(bin);
                    break;
                case String:
                    obj = readString(bin);
                    break;
                case FromLookup:
                    obj = stringLookupTable[readShort(bin)];
                    break;
            }

            element.getJSONObject("attributes").put(localName, obj);
        }
    }

    private static int unsignedByteToInt(byte b) {
        // for instance, "-92" is 0xA4, which for an unsigned byte is 164.
        // -92 + 256 = 164
        int i = b;
        if (i < 0) i += 256;
        return i;
    }
}
