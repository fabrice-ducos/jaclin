/*
 * Copyright (c) 2006 Mo DeJong
 *
 * See the file "license.amd" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: TJCBench.java,v 1.9 2006/06/20 01:48:23 mdejong Exp $
 *
 */

// This class is an ungly workaround for a bug in the
// JDK 1.4 JVM that makes it impossible to load code
// in the tcl.lang.* package via a classloader. This
// class define Tcl tests that check implementation
// runtimes for the Tcl Bench suite. These tests
// should be defined in code compiled by Tcl Bench
// but compiling code inside a package does not
// work when classes access other package members.

package tcl.lang;

public class TJCBench extends TJC.CompiledCommand {

    public void cmdProc(
        Interp interp,
        TclObject[] objv)
            throws TclException
    {
	if (objv.length != 2) {
	    throw new TclNumArgsException(interp, 2, objv, "testname");
	}
        String testname = objv[1].toString();

        if (testname.equals("InternalTclObjectPreserve")) {
             InternalTclObjectPreserve(interp);
        } else if (testname.equals("InternalTclObjectPreserveRelease")) {
             InternalTclObjectPreserveRelease(interp);
        } else if (testname.equals("InternalExprParseIntValue")) {
             InternalExprParseIntValue(interp);
        } else if (testname.equals("InternalExprParseDoubleValue")) {
             InternalExprParseDoubleValue(interp);
        } else if (testname.equals("InternalExprGetBooleanInt")) {
             InternalExprGetBooleanInt(interp);
        } else if (testname.equals("InternalExprGetBooleanDouble")) {
             InternalExprGetBooleanDouble(interp);
        } else if (testname.equals("InternalExprGetBooleanString")) {
             InternalExprGetBooleanString(interp);
        } else if (testname.equals("InternalIncr")) {
             InternalIncr(interp);
        } else if (testname.equals("InternalTclListAppend")) {
             InternalTclListAppend(interp);
        } else if (testname.equals("InternalTclListLength")) {
             InternalTclListLength(interp);
        } else if (testname.equals("InternalTclListLindex")) {
             InternalTclListLindex(interp);
        } else if (testname.equals("InternalTclStringNewInstance")) {
             InternalTclStringNewInstance(interp);
        } else if (testname.equals("InternalTclIntegerNewInstance")) {
             InternalTclIntegerNewInstance(interp);
        } else if (testname.equals("InternalTclDoubleNewInstance")) {
             InternalTclDoubleNewInstance(interp);
        } else if (testname.equals("InternalTclListNewInstance")) {
             InternalTclListNewInstance(interp);
        } else if (testname.equals("InternalTclStringDuplicate")) {
             InternalTclStringDuplicate(interp);
        } else if (testname.equals("InternalTclIntegerDuplicate")) {
             InternalTclIntegerDuplicate(interp);
        } else if (testname.equals("InternalTclDoubleDuplicate")) {
             InternalTclDoubleDuplicate(interp);
        } else if (testname.equals("InternalTclListDuplicate")) {
             InternalTclListDuplicate(interp);
        } else if (testname.equals("InternalTclIntegerType")) {
             InternalTclIntegerType(interp);
        } else if (testname.equals("InternalTclDoubleType")) {
             InternalTclDoubleType(interp);
        } else if (testname.equals("InternalTclStringType")) {
             InternalTclStringType(interp);
        } else if (testname.equals("InternalTclListType")) {
             InternalTclListType(interp);
        } else if (testname.equals("InternalTclIntegerGet")) {
             InternalTclIntegerGet(interp);
        } else if (testname.equals("InternalExprGetKnownInt")) {
             InternalExprGetKnownInt(interp);
        } else if (testname.equals("InternalExprInlineGetInt")) {
             InternalExprInlineGetInt(interp);
        } else if (testname.equals("InternalTclDoubleGet")) {
             InternalTclDoubleGet(interp);
        } else if (testname.equals("InternalExprGetKnownDouble")) {
             InternalExprGetKnownDouble(interp);
        } else if (testname.equals("InternalExprInlinedIntNotOperator")) {
             InternalExprInlinedIntNotOperator(interp);
        } else if (testname.equals("InternalExprInlinedIntNotBitwiseOperator")) {
             InternalExprInlinedIntNotBitwiseOperator(interp);
        } else if (testname.equals("InternalExprValueIntNotOperator")) {
             InternalExprValueIntNotOperator(interp);
        } else if (testname.equals("InternalExprValueIntNotNstrOperator")) {
             InternalExprValueIntNotNstrOperator(interp);
        } else if (testname.equals("InternalSetTclObjectResult")) {
             InternalSetTclObjectResult(interp);
        } else if (testname.equals("InternalSetSameTclObjectResult")) {
             InternalSetSameTclObjectResult(interp);
        } else if (testname.equals("InternalResetResult")) {
             InternalResetResult(interp);
        } else if (testname.equals("InternalSetBooleanResult")) {
             InternalSetBooleanResult(interp);
        } else if (testname.equals("InternalSetIntResult")) {
             InternalSetIntResult(interp);
        } else if (testname.equals("InternalSetIntResultViaExprValue")) {
             InternalSetIntResultViaExprValue(interp);
        } else if (testname.equals("InternalExprSetIntResult")) {
             InternalExprSetIntResult(interp);
        } else if (testname.equals("InternalExprOpIntPlus")) {
             InternalExprOpIntPlus(interp);
        } else if (testname.equals("InternalExprOpDoublePlus")) {
             InternalExprOpDoublePlus(interp);
        } else if (testname.equals("InternalExprOpIntNot")) {
             InternalExprOpIntNot(interp);
        } else if (testname.equals("InternalExprOpIntNotGrabReleaseResult")) {
             InternalExprOpIntNotGrabReleaseResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueResult")) {
             InternalExprOpIntNotStackValueResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueIntResult")) {
             InternalExprOpIntNotStackValueIntResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueBooleanResult")) {
             InternalExprOpIntNotStackValueBooleanResult(interp);
        } else if (testname.equals("InternalExprOpIntNotStackValueBooleanLookupResult")) {
             InternalExprOpIntNotStackValueBooleanLookupResult(interp);
        } else if (testname.equals("InternalExprOpIntInlinedNotStackValueResult")) {
             InternalExprOpIntInlinedNotStackValueResult(interp);
        } else if (testname.equals("InternalExprOpIntInlinedNotNstrStackValueResult")) {
             InternalExprOpIntInlinedNotNstrStackValueResult(interp);
        } else if (testname.equals("InternalExprOpIntInlinedNotNstrStackValueIntResult")) {
             InternalExprOpIntInlinedNotNstrStackValueIntResult(interp);
        } else if (testname.equals("InternalExprOpIntInlinedNotNstrStackValueBooleanResult")) {
             InternalExprOpIntInlinedNotNstrStackValueBooleanResult(interp);
        } else {
             throw new TclException(interp, "unknown test name \"" + testname + "\"");
        }
    }

    // Each test must take special care to save
    // the result of operations to a variable
    // so that the optimizer does not incorrectly
    // eliminate what it thinks is dead code.

    static int RESULT_INT = 0;
    static Object RESULT_OBJ = null;

    // Invoke TclObject.preserve() over and over again
    // on a new TclObject.

    void InternalTclObjectPreserve(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);

        for (int i=0; i < 5000; i++) {
            tobj.preserve();
        }
        RESULT_INT = tobj.getRefCount();
    }

    // Invoke preserve() and release() on a TclObject.

    void InternalTclObjectPreserveRelease(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        tobj.preserve(); // refcount = 1
        tobj.preserve(); // refcount = 2

        for (int i=0; i < 5000; i++) {
            tobj.preserve();
            tobj.release();
        }
        RESULT_INT = tobj.getRefCount();
    }

    // Invoke ExprParseObject() over and over again on a
    // TclObject with a TclInteger internal rep.

    void InternalExprParseIntValue(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        ExprValue value = new ExprValue(0, null);

        for (int i=0; i < 5000; i++) {
            Expression.ExprParseObject(interp, tobj, value);
        }
        RESULT_INT = TclInteger.get(interp, tobj);
    }

    // Invoke ExprParseObject() over and over again on a
    // TclObject with a TclDouble internal rep.

    void InternalExprParseDoubleValue(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        ExprValue value = new ExprValue(0, null);

        for (int i=0; i < 5000; i++) {
            Expression.ExprParseObject(interp, tobj, value);
        }
        RESULT_INT = (int) TclDouble.get(interp, tobj);
    }

    // Invoke TJC.getBoolean() over and over with a TclInteger

    void InternalExprGetBooleanInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Invoke TJC.getBoolean() over and over with a TclDouble

    void InternalExprGetBooleanDouble(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Invoke TJC.getBoolean() over and over with a TclString

    void InternalExprGetBooleanString(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("true");
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = TJC.getBoolean(interp, tobj);
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Invoke "incr" operation on an unshared TclInteger.
    // This checks the runtime execution speed
    // of the TclInteger.incr() operation in the
    // most common case of an unshared int.

    void InternalIncr(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(0);

        for (int i=0; i < 5000; i++) {
            TclInteger.incr(interp, tobj, 1);
        }
        RESULT_INT = TclInteger.get(interp, tobj);
    }

    // Invoke TclList.getLength() on an unshared
    // TclObject with the TclList type. This
    // will get timing info for this commonly
    // used low level operation.

    void InternalTclListLength(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        // Create list of length 3
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        int size = 0;

        for (int i=0; i < 5000; i++) {
            size += TclList.getLength(interp, tlist);
        }
        RESULT_INT = size;
    }

    // Invoke TclList.index() in a loop to get
    // timing info for this low level operation.

    void InternalTclListLindex(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        // Create list of length 10
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);
        TclList.append(interp, tlist, tobj);

        for (int i=0; i < 5000; i++) {
            tobj = TclList.index(interp, tlist, 6);
        }
        RESULT_OBJ = tobj;
    }

    // Invoke TclList.append() on an unshared
    // TclObject with the TclList type. This
    // will get timing info for TclList.append(),
    // a low level and commonly used operation.

    void InternalTclListAppend(Interp interp)
        throws TclException
    {
        TclObject tlist = TclList.newInstance();
        TclObject tobj = interp.checkCommonString(null); // Empty string

        for (int i=0; i < 5000; i++) {
            TclList.append(interp, tlist, tobj);
        }
        RESULT_OBJ = tlist;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclString internal rep.

    void InternalTclStringNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclString.newInstance("foo");
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclInteger internal rep.

    void InternalTclIntegerNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclInteger.newInstance(1);
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclDouble internal rep.

    void InternalTclDoubleNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclDouble.newInstance(1.0);
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for allocation of a
    // TclObject with a TclList internal rep.

    void InternalTclListNewInstance(Interp interp)
        throws TclException
    {
        TclObject tobj = null;

        for (int i=0; i < 5000; i++) {
            tobj = TclList.newInstance();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclString internal rep.

    void InternalTclStringDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("foo");;

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclInteger internal rep.

    void InternalTclIntegerDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclDouble internal rep.

    void InternalTclDoubleDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.duplicate()
    // of TclObject with a TclList internal rep.

    void InternalTclListDuplicate(Interp interp)
        throws TclException
    {
        TclObject tobj = TclList.newInstance();

        for (int i=0; i < 5000; i++) {
            tobj = tobj.duplicate();
        }
        RESULT_OBJ = tobj;
    }

    // Establish timing results for TclObject.isIntType() API.

    void InternalTclIntegerType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isIntType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isDoubleType() API.

    void InternalTclDoubleType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isDoubleType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isStringType() API.

    void InternalTclStringType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclString.newInstance("foo");
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isStringType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclObject.isListType() API.

    void InternalTclListType(Interp interp)
        throws TclException
    {
        TclObject tobj = TclList.newInstance();
        boolean b = false;

        for (int i=0; i < 5000; i++) {
            b = tobj.isListType();
        }
        RESULT_INT = (b ? 1 : 0);
    }

    // Establish timing results for TclInteger.get().

    void InternalTclIntegerGet(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = TclInteger.get(interp, tobj);
        }
        RESULT_INT = ivalue;
    }

    // Establish timing results for TJC.exprGetKnownInt(),
    // this API is used to access the int value inside
    // a TclObject known to contain an int in expr code.

    void InternalExprGetKnownInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = TJC.exprGetKnownInt(tobj);
        }
        RESULT_INT = ivalue;
    }

    // Grab the int value out of a TclObject by
    // directly accessing the package protected
    // field. Testing seems to indicate that
    // the method inliner is working well and
    // that the TJC.exprGetKnownInt() method
    // works just as fast as directly accessing
    // the field in this case.

    void InternalExprInlineGetInt(Interp interp)
        throws TclException
    {
        TclObject tobj = TclInteger.newInstance(1);
        int ivalue = 0;

        for (int i=0; i < 5000; i++) {
            ivalue = tobj.ivalue;
        }
        RESULT_INT = ivalue;
    }

    // Establish timing results for TclDouble.get().

    void InternalTclDoubleGet(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        double d = 0.0;

        for (int i=0; i < 5000; i++) {
            d = TclDouble.get(interp, tobj);
        }
        RESULT_INT = (int) d;
    }

    // Establish timing results for TJC.exprGetKnownInt(),
    // this API is used to access the int value inside
    // a TclObject known to contain an int in expr code.

    void InternalExprGetKnownDouble(Interp interp)
        throws TclException
    {
        TclObject tobj = TclDouble.newInstance(1.0);
        double d = 0.0;

        for (int i=0; i < 5000; i++) {
            d = TJC.exprGetKnownDouble(tobj);
        }
        RESULT_INT = (int) d;
    }

    // Note that there is no test like
    // InternalExprInlineGetInt since there
    // is no double field to access.


    // Integer not operator as inlined logic

    void InternalExprInlinedIntNotOperator(Interp interp)
        throws TclException
    {
        int v = 1;

        for (int i=0; i < 5000; i++) {
            v = ((v == 0) ? 1 : 0);
        }
        RESULT_INT = v;
    }

    // Integer not operator as inlined bitwise operation

    void InternalExprInlinedIntNotBitwiseOperator(Interp interp)
        throws TclException
    {
        int v = 1;

        for (int i=0; i < 5000; i++) {
            v = ((v | -v) >>> 31) ^ 1;
        }
        RESULT_INT = v;
    }

    // Integer not operator via ExprValue.optIntUnaryNot() method

    void InternalExprValueIntNotOperator(Interp interp)
        throws TclException
    {
        ExprValue ev = new ExprValue(1, null);

        for (int i=0; i < 5000; i++) {
            ev.optIntUnaryNot();
        }
        RESULT_INT = ev.getIntValue();
    }

    // Integer not operator via ExprValue.optIntUnaryNotNstr() method.
    // Unlike optIntUnaryNot, this method does not null out the
    // string value.

    void InternalExprValueIntNotNstrOperator(Interp interp)
        throws TclException
    {
        ExprValue ev = new ExprValue(1, null);

        for (int i=0; i < 5000; i++) {
            ev.optIntUnaryNotNstr();
        }
        RESULT_INT = ev.getIntValue();
    }

    // Establish timing results for integer.setResult(TclObject).
    // Note that we need to use two different TclObject values
    // here to avoid an optimization in setResult() that
    // returns right away when the same result is set again.
    // There also need to be two set operations in the loop
    // so we can compare to the int/boolean operations below.

    void InternalSetTclObjectResult(Interp interp)
        throws TclException
    {
        TclObject tobj1 = TclInteger.newInstance(1);
        TclObject tobj2 = TclInteger.newInstance(2);

        // Make the objects shared so that they don't
        // get deallocated when the interp result
        // is changed.
        tobj1.preserve();
        tobj1.preserve();
        tobj2.preserve();
        tobj2.preserve();

        for (int i=0; i < 5000; i++) {
            interp.setResult(tobj1);
            interp.setResult(tobj2);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Invoke integer.setResult(TclObject) over and over
    // again on the same TclObject. This should execute
    // very quickly since the setResult() API contains
    // a short circut return in this case.

    void InternalSetSameTclObjectResult(Interp interp)
        throws TclException
    {
        TclObject tobj1 = TclInteger.newInstance(1);
        tobj1.preserve();
        tobj1.preserve();

        for (int i=0; i < 5000; i++) {
            interp.setResult(tobj1);
            interp.setResult(tobj1);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Establish timing results for integer.resetResult().

    void InternalResetResult(Interp interp)
        throws TclException
    {
        for (int i=0; i < 5000; i++) {
            interp.resetResult();
            interp.resetResult();
        }
        RESULT_OBJ = interp.getResult();
    }

    // Establish timing results for integer.setResult(boolean).
    // Both the true and false values have a shared object.

    void InternalSetBooleanResult(Interp interp)
        throws TclException
    {
        boolean b1 = true;
        boolean b2 = false;
        if (RESULT_INT == 0) {
            // Make sure booleans are not seen as compile
            // time constant values.
            b1 = false;
            b2 = true;
        }

        for (int i=0; i < 5000; i++) {
            interp.setResult(b1);
            interp.setResult(b2);
        }
        RESULT_INT = (TclBoolean.get(interp, interp.getResult()) ? 1 : 0);
    }

    // Establish timing results for integer.setResult(int).
    // Both the 0 and 1 values have a shared object.

    void InternalSetIntResult(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }

        for (int i=0; i < 5000; i++) {
            interp.setResult(i1);
            interp.setResult(i2);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Invoke integer.setResult(int) with a result
    // stored in an ExprValue. This is like the
    // test above except that it includes execution
    // time for getting the int value out of the
    // ExprValue object. In that way, it is like
    // TJC.exprSetResult() but without a type switch.

    void InternalSetIntResultViaExprValue(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }
        ExprValue value1 = TJC.exprGetValue(interp, i1, null);
        ExprValue value2 = TJC.exprGetValue(interp, i2, null);

        for (int i=0; i < 5000; i++) {
            interp.setResult(value1.getIntValue());
            interp.setResult(value2.getIntValue());
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Establish timing results for TJC.exprSetResult()
    // when invoked with an int type. These results
    // can be compared to InternalSetIntResultViaExprValue
    // to see how long the type query and branch
    // operation is taking.

    void InternalExprSetIntResult(Interp interp)
        throws TclException
    {
        int i1 = 1;
        int i2 = 0;
        if (RESULT_INT == 0) {
            // Make sure ints are not seen as compile
            // time constant values.
            i1 = 0;
            i2 = 1;
        }
        ExprValue value1 = TJC.exprGetValue(interp, i1, null);
        ExprValue value2 = TJC.exprGetValue(interp, i2, null);

        for (int i=0; i < 5000; i++) {
            TJC.exprSetResult(interp, value1);
            TJC.exprSetResult(interp, value2);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Invoke binary + operator on a TclInteger.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpIntPlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1, null);
        ExprValue value2 = new ExprValue(2, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
        RESULT_INT = value1.getIntValue();
    }

    // Invoke binary + operator on a TclDouble.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpDoublePlus(Interp interp)
        throws TclException
    {
        ExprValue value1 = new ExprValue(1.0, null);
        ExprValue value2 = new ExprValue(2.0, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalBinaryOperator(interp, TJC.EXPR_OP_PLUS, value1, value2);
        }
        RESULT_INT = (int) value1.getDoubleValue();
    }

    // Invoke unary ! operator on a TclInteger.
    // This tests the execution time for just the
    // Expression.evalBinaryOperator() API.

    void InternalExprOpIntNot(Interp interp)
        throws TclException
    {
        ExprValue value = new ExprValue(1, null);

        for (int i=0; i < 5000; i++) {
            Expression.evalUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, value);
        }
        RESULT_INT = value.getIntValue();
    }

    // This is the logic emitted for a unary
    // logical ! operator with +inline-containers.
    // This code will grab an ExprValue, init it,
    // invoke the operator method, and then
    // set the interp result. Note that because
    // the interp is set to the int 0 over
    // and over again, short circut logic
    // in the setResult(TclObject) method
    // will return right away since the new
    // value is the same as the existing result.

    void InternalExprOpIntNotGrabReleaseResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = TJC.exprGetValue(interp, 1, null);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            TJC.exprSetResult(interp, tmp0);
            TJC.exprReleaseValue(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This is the logic emitted for a unary
    // logical ! operator with +inline-expr.
    // Unlike InternalExprOpIntNotGrabReleaseResult
    // this implementation will reuse an ExprValue
    // saved on the stack and avoid having to
    // grab and release during each loop iteration.
    // This implementation should be the baseline
    // for optimized not operator implementations.

    void InternalExprOpIntNotStackValueResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            TJC.exprSetResult(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This method is like InternalExprOpIntNotStackValueResult
    // except that it invokes the Interp.setResult(int) method
    // directly instead of calling TJC.exprSetResult().
    // This optimization is valid because we know the result
    // of a unary not is always of type int. This optimization
    // avoids a method invocation, a branching operation,
    // and a call to ExprValue.getType() for each iteration
    // of the loop.

    void InternalExprOpIntNotStackValueIntResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            interp.setResult( tmp0.getIntValue() );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This method is like InternalExprOpIntNotStackValueIntResult
    // except that it invokes Interp.setResult(boolean) to
    // directly set the result to a boolean value.

    void InternalExprOpIntNotStackValueBooleanResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            interp.setResult( (tmp0.getIntValue() != 0) );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This method implements an optimization that looks up
    // the ref used for the boolean true and false objects
    // and then invokes Interp.setResult(TclObject) with
    // the object as the argument. Both the Interp.setResult(int)
    // and Interp.setResult(boolean) methods execute in
    // about the same amount of time for true/false values.
    // This logic does the true/false symbol lookup inline.

    void InternalExprOpIntNotStackValueBooleanLookupResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        interp.setResult(true);
        TclObject trueRef = interp.getResult();
        interp.setResult(false);
        TclObject falseRef = interp.getResult();

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
            interp.setResult( (tmp0.getIntValue() != 0) ? trueRef : falseRef );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This optimized logic will check the type of an
    // ExprValue operand and inline a call to a
    // specific optimized method for the ExprValue.
    // This should execute significantly faster
    // than the TJC.exprUnaryOperator() call found in
    // InternalExprOpIntNotStackValueResult().
    // Note that exprSetResult() can end up executing
    // very quickly when the same value is set
    // as the result twice.

    void InternalExprOpIntInlinedNotStackValueResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            // Use optimized impl when ExprValue is known to be an int.
            if ( tmp0.isIntType() ) {
                tmp0.optIntUnaryNot();
            } else {
                //TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
                throw new TclRuntimeError("else branch reached");
            }
            TJC.exprSetResult(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This implementation is like InternalExprOpIntInlinedNotStackValueResult
    // except that it invokes optIntUnaryNotNstr() which does not null
    // the string value. If this shaves a few ms off the execution time
    // then it is worthwhile to implement in compiled code.

    void InternalExprOpIntInlinedNotNstrStackValueResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            // Use optimized impl when ExprValue is known to be an int
            // that has no string value.
            if ( tmp0.isIntType() ) {
                tmp0.optIntUnaryNotNstr();
            } else {
                //TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
                throw new TclRuntimeError("else branch reached");
            }
            TJC.exprSetResult(interp, tmp0);
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // This implementation takes advantage of the inlined not
    // operator logic and the int result set logic. This method
    // should indicate how much faster the implementation can
    // be made compared to InternalExprOpIntNotStackValueResult.

    void InternalExprOpIntInlinedNotNstrStackValueIntResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(1);
            // Use optimized impl when ExprValue is known to be an int
            // that has no string value.
            if ( tmp0.isIntType() ) {
                tmp0.optIntUnaryNotNstr();
            } else {
                //TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
                throw new TclRuntimeError("else branch reached");
            }
            interp.setResult( tmp0.getIntValue() );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

    // Like InternalExprOpIntInlinedNotNstrStackValueIntResult
    // except makes use of interp.setResult(boolean).

    void InternalExprOpIntInlinedNotNstrStackValueBooleanResult(Interp interp)
        throws TclException
    {
        // expr {!1}

        ExprValue evs0 = TJC.exprGetValue(interp);

        for (int i=0; i < 5000; i++) {
            ExprValue tmp0 = evs0;
            tmp0.setIntValue(0);
            // Use optimized impl when ExprValue is known to be an int
            // that has no string value.
            if ( tmp0.isIntType() ) {
                tmp0.optIntUnaryNotNstr();
            } else {
                //TJC.exprUnaryOperator(interp, TJC.EXPR_OP_UNARY_NOT, tmp0);
                throw new TclRuntimeError("else branch reached");
            }
            interp.setResult( (tmp0.getIntValue() != 0) );
        }
        RESULT_INT = TclInteger.get(interp, interp.getResult());
    }

} // end class TJCBench

