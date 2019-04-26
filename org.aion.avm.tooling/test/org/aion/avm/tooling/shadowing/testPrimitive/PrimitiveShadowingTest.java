package org.aion.avm.tooling.shadowing.testPrimitive;

import static org.junit.Assert.fail;

import avm.Address;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import org.aion.avm.core.util.ABIUtil;
import org.aion.avm.tooling.AvmRule;
import org.aion.avm.tooling.AvmRule.ResultWrapper;
import org.aion.kernel.AvmTransactionResult;
import org.aion.vm.api.interfaces.TransactionResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class PrimitiveShadowingTest {
    @Rule
    public AvmRule avmRule = new AvmRule(false);

    private Address from = avmRule.getPreminedAccount();
    private Address dappAddr;

    private long energyLimit = 600_000_00000L;
    private long energyPrice = 1;

    @Before
    public void setup() {
        byte[] txData = avmRule.getDappBytes (TestResource.class, null);
        dappAddr = avmRule.deploy(from, BigInteger.ZERO, txData, energyLimit, energyPrice).getDappAddress();
    }

    @Test
    public void testBoolean() {
        byte[] txData = ABIUtil.encodeMethodArguments("testBoolean");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testByte() {
        byte[] txData = ABIUtil.encodeMethodArguments("testByte");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testDouble() {
        byte[] txData = ABIUtil.encodeMethodArguments("testDouble");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testFloat() {
        byte[] txData = ABIUtil.encodeMethodArguments("testFloat");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testInteger() {
        byte[] txData = ABIUtil.encodeMethodArguments("testInteger");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testLong() {
        byte[] txData = ABIUtil.encodeMethodArguments("testLong");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testShort() {
        byte[] txData = ABIUtil.encodeMethodArguments("testShort");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }

    @Test
    public void testCharacter() {
        byte[] txData = ABIUtil.encodeMethodArguments("testCharacter");
        ResultWrapper result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData,
            energyLimit, energyPrice);

        TransactionResult tr = result.getTransactionResult();
        if(! (tr instanceof AvmTransactionResult)) {
            throw new RuntimeException("Expected result to be of type AvmTransactionResult");
        }
        AvmTransactionResult atr = (AvmTransactionResult) tr;

        // The test program throws AssertionError for test failures.  Look for AssertionError
        // and if found, fail this test and display the stack trace for reporting/informational
        // purposes.  If any other kind of uncaught exception present, but not AssertionError,
        // throw an error.
        if(atr.getUncaughtException() != null) {
            Throwable outerEx = atr.getUncaughtException();
            Object unwrapped =
                ((org.aion.avm.exceptionwrapper.org.aion.avm.shadow.java.lang.Throwable) outerEx).unwrap();

            if(unwrapped instanceof org.aion.avm.shadow.java.lang.AssertionError) {
                fail("Test failed.  Reason: "
                    + ((org.aion.avm.shadow.java.lang.AssertionError) unwrapped).avm_getMessage()
                    + System.lineSeparator()
                    + throwableAsPrettyString(outerEx));
            } else if (unwrapped instanceof org.aion.avm.shadow.java.lang.Throwable){
                throw new RuntimeException("Test error.  Reason: "
                    + ((org.aion.avm.shadow.java.lang.Throwable) unwrapped).avm_getMessage()
                    + System.lineSeparator()
                    + throwableAsPrettyString(outerEx));
            } else {
                throw new RuntimeException("Test error.  Reason: "
                    + ((org.aion.avm.shadow.java.lang.Throwable) unwrapped).avm_getMessage()
                    + System.lineSeparator()
                    + throwableAsPrettyString(outerEx));
            }
        }
        Assert.assertEquals(true, result.getDecodedReturnData());

    }

    private static String throwableAsPrettyString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String stStr = sw.toString();
        return "----- Avm uncaught exception stack trace start ------ "
            + System.lineSeparator() + stStr + System.lineSeparator()
            + "----- End of Avm uncaught exception ------";
    }

    @Test
    public void testAutoboxing() {
        byte[] txData = ABIUtil.encodeMethodArguments("testAutoboxing");
        Object result = avmRule.call(from, dappAddr, BigInteger.ZERO, txData, energyLimit, energyPrice).getDecodedReturnData();

        Assert.assertEquals(true, result);
    }
}
