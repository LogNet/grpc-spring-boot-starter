package org.lognet.springboot.rules;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.Statement;

import java.util.function.Predicate;

public class ExpectExceptionWithPredicate extends Statement {

    private final Predicate<Throwable> inspector;
    private final Statement next;

    public ExpectExceptionWithPredicate(Statement next, Predicate<Throwable> inspector) {
        this.next = next;
        this.inspector = inspector;
    }

    @Override
    public void evaluate() throws Throwable {
        boolean complete = false;
        try {
            next.evaluate();
            complete = true;
        } catch (AssumptionViolatedException e) {
            throw e;
        } catch (Throwable e) {
            if (!inspector.test(e)) {
                String message = "Unexpected exception, " + inspector.getClass().getName() + " returned false";
                throw new Exception(message, e);
            }
        }
        if (complete) {
            throw new AssertionError("Expected exception");
        }
    }
}
