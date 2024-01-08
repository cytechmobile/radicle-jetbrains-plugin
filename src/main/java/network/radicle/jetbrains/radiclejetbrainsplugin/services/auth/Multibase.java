package network.radicle.jetbrains.radiclejetbrainsplugin.services.auth;

import java.util.Map;
import java.util.TreeMap;

public class Multibase {
    public enum Base {
        Base58BTC('z');

        public final char prefix;

        Base(char prefix) {
            this.prefix = prefix;
        }

        private static Map<Character, Base> lookup = new TreeMap<>();

        static {
            for (Base b : Base.values()) {
                lookup.put(b.prefix, b);
            }
        }

        public static Base lookup(char p) {
            if (!lookup.containsKey(p)) {
                throw new IllegalArgumentException("Unknown Multibase type: " + p);
            }
            return lookup.get(p);
        }
    }

    public static String encode(Base b, byte[] data) {
        switch (b) {
            case Base58BTC:
                return b.prefix + Base58.encode(data);
            default:
                throw new UnsupportedOperationException("Unsupported base encoding: " + b.name());
        }
    }

    public static Base encoding(String data) {
        return Base.lookup(data.charAt(0));
    }

    public static byte[] decode(String data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Cannot decode an empty string");
        }
        Base b = encoding(data);
        String rest = data.substring(1);
        switch (b) {
            case Base58BTC:
                return Base58.decode(rest);
            default:
                throw new UnsupportedOperationException("Unsupported base encoding: " + b.name());
        }
    }
}
