package org.aion.avm.arraywrapper;

import org.aion.avm.internal.*;
import java.util.Arrays;

import org.aion.avm.RuntimeMethodFeeSchedule;

public class DoubleArray extends Array {

    private double[] underlying;

    /**
     * Static DoubleArray factory
     *
     * After instrumentation, NEWARRAY bytecode (with double as type) will be replaced by a INVOKESTATIC to
     * this method.
     *
     * @param size Size of the double array
     *
     * @return New empty double array wrapper
     */
    public static DoubleArray initArray(int size){
        chargeEnergy(size * ArrayElement.DOUBLE.getEnergy());
        return new DoubleArray(size);
    }

    @Override
    public int length() {
        lazyLoad();
        return this.underlying.length;
    }

    public double get(int idx) {
        lazyLoad();
        return this.underlying[idx];
    }

    public void set(int idx, double val) {
        lazyLoad();
        this.underlying[idx] = val;
    }

    @Override
    public IObject avm_clone() {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.DoubleArray_avm_clone + RuntimeMethodFeeSchedule.RT_METHOD_FEE_FACTOR * length());
        lazyLoad();
        return new DoubleArray(Arrays.copyOf(underlying, underlying.length));
    }

    @Override
    public IObject clone() {
        lazyLoad();
        return new DoubleArray(Arrays.copyOf(underlying, underlying.length));
    }

    //========================================================
    // Internal Helper
    //========================================================

    public DoubleArray(int c) {
        IInstrumentation.attachedThreadInstrumentation.get().chargeEnergy(RuntimeMethodFeeSchedule.DoubleArray_avm_constructor);
        this.underlying = new double[c];
    }

    public DoubleArray(double[] underlying) {
        RuntimeAssertionError.assertTrue(null != underlying);
        this.underlying = underlying;
    }

    public double[] getUnderlying() {
        lazyLoad();
        return underlying;
    }

    @Override
    public void setUnderlyingAsObject(java.lang.Object u){
        RuntimeAssertionError.assertTrue(null != u);
        lazyLoad();
        this.underlying = (double[]) u;
    }

    @Override
    public java.lang.Object getUnderlyingAsObject(){
        lazyLoad();
        return underlying;
    }

    @Override
    public java.lang.Object getAsObject(int idx){
        lazyLoad();
        return this.underlying[idx];
    }

    //========================================================
    // Persistent Memory Support
    //========================================================

    public DoubleArray(Void ignore, int readIndex) {
        super(ignore, readIndex);
    }

    public void deserializeSelf(java.lang.Class<?> firstRealImplementation, IObjectDeserializer deserializer) {
        super.deserializeSelf(DoubleArray.class, deserializer);

        // TODO (AKI-118):  We probably want faster array copies.
        int length = deserializer.readInt();
        this.underlying = new double[length];
        for (int i = 0; i < length; ++i) {
            this.underlying[i] = deserializer.readDouble();
        }
    }

    public void serializeSelf(java.lang.Class<?> firstRealImplementation, IObjectSerializer serializer) {
        super.serializeSelf(DoubleArray.class, serializer);

        // TODO (AKI-118):  We probably want faster array copies.
        serializer.writeInt(this.underlying.length);
        for (int i = 0; i < this.underlying.length; ++i) {
            serializer.writeDouble(this.underlying[i]);
        }
    }
}
