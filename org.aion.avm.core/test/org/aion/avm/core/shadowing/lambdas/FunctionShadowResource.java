package org.aion.avm.core.shadowing.lambdas;

import java.util.function.Function;

import org.aion.avm.userlib.abi.ABIEncoder;

import avm.Blockchain;


public class FunctionShadowResource {
    private static Function<String, String> MAPPER;


    public static byte[] main() {
        byte[] result = null;
        switch (Blockchain.getData()[0]) {
        case 0:
            result = getSimpleRunnable();
        case 1:
            result = getSimpleFunction();
        case 2:
            result = saveStringFunction();
        case 3:
            result = loadStringFunction();
            default:
                // Unknown.
                Blockchain.revert();
        }
        return result;
    }

    private static byte[] getSimpleRunnable() {
        byte[] holder = new byte[1];
        int x = 5;
        Runnable mapper = () -> {
            holder[0] = (byte) ((4 == x) ? 0 : 1);
        };
        int hashcode = mapper.hashCode();
        Blockchain.require(1 == holder[0]);
        return ABIEncoder.encodeOneInteger(hashcode);
    }

    private static byte[] getSimpleFunction() {
        Function<String, String> mapper = string -> string;
        int hashcode = mapper.hashCode();
        return ABIEncoder.encodeOneInteger(hashcode);
    }

    private static byte[] saveStringFunction() {
        // We will process this so we aren't literally pointing at the static constant.
        String prefix = "test".replace('s', 'y');
        MAPPER = string -> prefix + string;
        return new byte[0];
    }

    private static byte[] loadStringFunction() {
        // We will process this so we aren't literally pointing at the static constant.
        String string = "next".replace('x', 'y');
        String result = MAPPER.apply(string);
        int hashcode = result.hashCode();
        return ABIEncoder.encodeOneInteger(hashcode);
    }
}

