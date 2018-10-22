package org.aion.cli;

import org.aion.avm.api.ABIEncoder;
import org.aion.avm.api.Address;
import org.aion.avm.core.Avm;
import org.aion.avm.core.NodeEnvironment;
import org.aion.avm.core.util.CodeAndArguments;
import org.aion.avm.core.util.Helpers;
import org.aion.avm.core.util.StorageWalker;
import org.aion.avm.internal.RuntimeAssertionError;
import org.aion.kernel.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class AvmCLI {
    static Block block = new Block(new byte[32], 1, Helpers.randomBytes(Address.LENGTH), System.currentTimeMillis(), new byte[0]);

    public static void deploy(IEnvironment env, String storagePath, String jarPath, byte[] sender, long energyLimit) {

        reportDeployRequest(env, storagePath, jarPath, sender);

        if (sender.length != Address.LENGTH){
            throw env.fail("deploy : Invalid sender address");
        }

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);
        Avm avm = NodeEnvironment.singleton.buildAvmInstance(kernel);

        Path path = Paths.get(jarPath);
        byte[] jar;
        try {
            jar = Files.readAllBytes(path);
        }catch (IOException e){
            throw env.fail("deploy : Invalid location of Dapp jar");
        }

        Transaction createTransaction = Transaction.create(sender, kernel.getNonce(sender), 0, new CodeAndArguments(jar, null).encodeToBytes(), energyLimit, 1L);

        TransactionContext createContext = new TransactionContextImpl(createTransaction, block);
        TransactionResult createResult = avm.run(new TransactionContext[] {createContext})[0].get();
        avm.shutdown();

        reportDeployResult(env, createResult);
    }

    public static void reportDeployRequest(IEnvironment env, String storagePath, String jarPath, byte[] sender) {
        lineSeparator(env);
        env.logLine("DApp deployment request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("Dapp Jar     : " + jarPath);
        env.logLine("Sender       : " + Helpers.bytesToHexString(sender));
    }

    public static void reportDeployResult(IEnvironment env, TransactionResult createResult){
        String dappAddress = Helpers.bytesToHexString(createResult.getReturnData());
        env.noteRelevantAddress(dappAddress);
        
        lineSeparator(env);
        env.logLine("DApp deployment status");
        env.logLine("Result status: " + createResult.getStatusCode().name());
        env.logLine("Dapp Address : " + dappAddress);
        env.logLine("Energy cost  : " + createResult.getEnergyUsed());
    }

    public static void call(IEnvironment env, String storagePath, byte[] contract, byte[] sender, String method, Object[] args, long energyLimit) {
        reportCallRequest(env, storagePath, contract, sender, method, args);

        if (contract.length != Address.LENGTH){
            throw env.fail("call : Invalid Dapp address ");
        }

        if (sender.length != Address.LENGTH){
            throw env.fail("call : Invalid sender address");
        }

        byte[] arguments = ABIEncoder.encodeMethodArguments(method, args);

        File storageFile = new File(storagePath);

        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);
        Avm avm = NodeEnvironment.singleton.buildAvmInstance(kernel);

        Transaction callTransaction = Transaction.call(sender, contract, kernel.getNonce(sender), 0L, arguments, energyLimit, 1L);
        TransactionContext callContext = new TransactionContextImpl(callTransaction, block);
        TransactionResult callResult = avm.run(new TransactionContext[] {callContext})[0].get();
        avm.shutdown();

        reportCallResult(env, callResult);
    }

    private static void reportCallRequest(IEnvironment env, String storagePath, byte[] contract, byte[] sender, String method, Object[] args){
        lineSeparator(env);
        env.logLine("DApp call request");
        env.logLine("Storage      : " + storagePath);
        env.logLine("Dapp Address : " + Helpers.bytesToHexString(contract));
        env.logLine("Sender       : " + Helpers.bytesToHexString(sender));
        env.logLine("Method       : " + method);
        env.logLine("Arguments    : ");
        for (int i = 0; i < args.length; i += 2){
            env.logLine("             : " + args[i]);
        }
    }

    private static void reportCallResult(IEnvironment env, TransactionResult callResult){
        lineSeparator(env);
        env.logLine("DApp call result");
        env.logLine("Result status: " + callResult.getStatusCode().name());
        env.logLine("Return value : " + Helpers.bytesToHexString(callResult.getReturnData()));
        env.logLine("Energy cost  : " + callResult.getEnergyUsed());

        if (callResult.getStatusCode() == TransactionResult.Code.FAILED_EXCEPTION) {
            env.dumpThrowable(callResult.getUncaughtException());
        }
    }

    private static void lineSeparator(IEnvironment env){
        env.logLine("*******************************************************************************************");
    }

    public static void openAccount(IEnvironment env, String storagePath, byte[] toOpen){
        lineSeparator(env);

        if (toOpen.length != Address.LENGTH){
            throw env.fail("open : Invalid address to open");
        }

        env.logLine("Creating Account " + Helpers.bytesToHexString(toOpen));

        File storageFile = new File(storagePath);
        KernelInterfaceImpl kernel = new KernelInterfaceImpl(storageFile);

        kernel.createAccount(toOpen);
        kernel.adjustBalance(toOpen, 100000000000L);

        env.logLine("Account Balance : " + kernel.getBalance(toOpen));
    }

    public static void exploreStorage(IEnvironment env, String storagePath, byte[] dappAddress) {
        // Create the PrintStream abstraction that walkAllStaticsForDapp expects.
        // (ideally, we would incrementally filter this but that could be a later improvement - current Dapps are very small).
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream printer = new PrintStream(stream);
        
        // Create the directory-backed kernel.
        KernelInterfaceImpl kernel = new KernelInterfaceImpl(new File(storagePath));
        
        // Walk everything, treating unexpected exceptions as fatal.
        try {
            StorageWalker.walkAllStaticsForDapp(printer, kernel, dappAddress);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | IOException e) {
            // This tool can fail out if something goes wrong.
            throw env.fail(e.getMessage());
        }
        
        // Flush all the captured data and feed it to the env.
        printer.flush();
        String raw = new String(stream.toByteArray());
        env.logLine(raw);
    }

    public static void testingMain(IEnvironment env, String[] args) {
        internalMain(env, args);
    }

    static public void main(String[] args){
        IEnvironment env = new IEnvironment() {
            @Override
            public RuntimeException fail(String message) {
                if (null != message) {
                    System.err.println(message);
                }
                System.exit(1);
                throw new RuntimeException();
            }
            @Override
            public void noteRelevantAddress(String address) {
                // This implementation doesn't care.
            }
            @Override
            public void logLine(String line) {
                System.out.println(line);
            }

            @Override
            public void dumpThrowable(Throwable throwable) {
                throwable.printStackTrace();
            }
        };
        internalMain(env, args);
    }

    private static void internalMain(IEnvironment env, String[] args) {
        ArgumentParser.Invocation invocation = ArgumentParser.parseArgs(args);
        if (null == invocation.errorString) {
            // There must be at least one command or there should have been a parse error (usually just defaulting to usage).
            RuntimeAssertionError.assertTrue(invocation.commands.size() > 0);
            // This logging line is largely just for test verification so it might be removed in the future.
            env.logLine("Running block with " + invocation.commands.size() + " transactions");
            
            // Do the thing.
            // Before we run any command, make sure that the specified storage directory exists.
            // (we want the underlying storage engine to remain very passive so it should always expect that the directory was created for it).
            verifyStorageExists(env, invocation.storagePath);
            
            for (ArgumentParser.Command command : invocation.commands) {
                switch (command.action) {
                case CALL:
                    Object[] callArgs = new Object[command.args.size()];
                    command.args.toArray(callArgs);
                    call(env, invocation.storagePath, Helpers.hexStringToBytes(command.contractAddress), Helpers.hexStringToBytes(command.senderAddress), command.method, callArgs, command.energyLimit);
                    break;
                case DEPLOY:
                    deploy(env, invocation.storagePath, command.jarPath, Helpers.hexStringToBytes(command.senderAddress), command.energyLimit);
                    break;
                case EXPLORE:
                    exploreStorage(env, invocation.storagePath, Helpers.hexStringToBytes(command.contractAddress));
                    break;
                case OPEN:
                    openAccount(env, invocation.storagePath, Helpers.hexStringToBytes(command.contractAddress));
                    break;
                default:
                    throw new AssertionError("Unknown option");
                }
            }
        } else {
            env.fail(invocation.errorString);
        }
    }

    private static void verifyStorageExists(IEnvironment env, String storageRoot) {
        File directory = new File(storageRoot);
        if (!directory.isDirectory()) {
            boolean didCreate = directory.mkdirs();
            // Is this the best way to handle this failure?
            if (!didCreate) {
                // System.exit isn't ideal but we are very near the top of the entry-point so this shouldn't cause confusion.
                throw env.fail("Failed to create storage root: \"" + storageRoot + "\"");
            }
        }
    }
}
